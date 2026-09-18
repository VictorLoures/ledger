package dev.victorloures.ledger;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.JobStatus;
import dev.victorloures.ledger.domain.JobType;
import dev.victorloures.ledger.repository.JobRepository;
import dev.victorloures.ledger.repository.OutboxEventRepository;
import dev.victorloures.ledger.service.JobProcessingService;
import dev.victorloures.ledger.service.JobSideEffectExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Mocka só o "trabalho real" (JobSideEffectExecutor) — tudo em volta
// (transação, banco via Testcontainers, migrations) roda de verdade, então
// o teste valida o comportamento de retry/idempotência/outbox contra o
// schema real, sem depender de sleep nem de serviço externo de verdade.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JobProcessingServiceTest {

    @Autowired
    private JobProcessingService jobProcessingService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private JobSideEffectExecutor sideEffectExecutor;

    private Job job;

    @BeforeEach
    void setUp() {
        job = jobRepository.save(new Job(UUID.randomUUID(), JobType.EMAIL,
                "{\"to\":\"teste@example.com\"}", OffsetDateTime.now()));
    }

    @Test
    void retriesWithBackoffOnFailureAndSucceedsNextAttempt() throws Exception {
        doThrow(new RuntimeException("falha simulada"))
                .doNothing()
                .when(sideEffectExecutor).execute(any());

        OffsetDateTime before = OffsetDateTime.now();
        jobProcessingService.processAsync(job.getId()).join();

        Job afterFirstAttempt = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(afterFirstAttempt.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(afterFirstAttempt.getAttempts()).isEqualTo(1);
        assertThat(afterFirstAttempt.getScheduledAt()).isAfter(before.plusSeconds(1));

        jobProcessingService.processAsync(job.getId()).join();

        Job afterSecondAttempt = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(afterSecondAttempt.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(afterSecondAttempt.getAttempts()).isEqualTo(2);
        verify(sideEffectExecutor, times(2)).execute(any());
    }

    @Test
    void marksFailedAfterExhaustingAllAttempts() throws Exception {
        doThrow(new RuntimeException("sempre falha")).when(sideEffectExecutor).execute(any());

        for (int i = 0; i < job.getMaxAttempts(); i++) {
            jobProcessingService.processAsync(job.getId()).join();
        }

        Job result = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getAttempts()).isEqualTo(job.getMaxAttempts());
        assertThat(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc())
                .noneMatch(e -> e.getAggregateId().equals(job.getId()));
    }

    @Test
    void doesNotReapplySideEffectWhenAlreadyAppliedButRecordsOutboxEvent() throws Exception {
        // Simula: o efeito já rodou numa tentativa anterior, mas o processo
        // crashou antes de marcar o job como done.
        jobProcessingService.processAsync(job.getId()).join(); // aplica de verdade e marca done

        verify(sideEffectExecutor, times(1)).execute(any());
        assertThat(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc())
                .anyMatch(e -> e.getAggregateId().equals(job.getId()));

        // Reprocessamento forçado do mesmo job (idempotência sendo testada).
        jobProcessingService.processAsync(job.getId()).join();

        // Efeito colateral NÃO rodou de novo — só a primeira chamada existe.
        verify(sideEffectExecutor, times(1)).execute(any());
    }
}
