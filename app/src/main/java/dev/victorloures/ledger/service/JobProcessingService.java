package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.OutboxEvent;
import dev.victorloures.ledger.repository.JobRepository;
import dev.victorloures.ledger.repository.JobSideEffectRepository;
import dev.victorloures.ledger.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class JobProcessingService {

    private static final Logger log = LoggerFactory.getLogger(JobProcessingService.class);

    // Backoff exponencial com teto: 2s, 4s, 8s, 16s, 32s... nunca passa de 1 min.
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_DELAY = Duration.ofMinutes(1);

    private final JobRepository jobRepository;
    private final JobSideEffectRepository jobSideEffectRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JobSideEffectExecutor sideEffectExecutor;

    public JobProcessingService(JobRepository jobRepository,
                                 JobSideEffectRepository jobSideEffectRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 JobSideEffectExecutor sideEffectExecutor) {
        this.jobRepository = jobRepository;
        this.jobSideEffectRepository = jobSideEffectRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.sideEffectExecutor = sideEffectExecutor;
    }

    // Retorna CompletableFuture<Void> em vez de void por dois motivos: (1) só
    // assim dá pra esperar a conclusão num teste sem depender de sleep/timing;
    // (2) é a correção padrão pra armadilha "exceção @Async engolida" do
    // Módulo 4 — embora aqui toda exceção já seja tratada dentro do método.
    @Async("jobExecutor")
    @Transactional
    public CompletableFuture<Void> processAsync(UUID jobId) {
        // O job já chega aqui com status = running e attempts incrementado:
        // JobScannerService.claimDueJobs() faz isso na mesma transação que
        // reivindica o lock (FOR UPDATE SKIP LOCKED), pra nenhum outro
        // worker conseguir pegar o mesmo job no meio do caminho.
        Job job = jobRepository.findById(jobId).orElseThrow();

        try {
            runSideEffectIdempotently(job);
            job.markDone();
            recordCompletionEvent(job);
            log.info("Job {} concluído (tentativa {})", jobId, job.getAttempts());
        } catch (Exception e) {
            handleFailure(job, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    // Checa ANTES, executa, e só marca como aplicado DEPOIS do sucesso.
    // Se marcássemos antes de executar, uma tentativa que falha ficaria
    // registrada como "já aplicada" e o retry nunca tentaria de verdade de
    // novo — foi exatamente o bug que os testes pegaram na primeira versão.
    private void runSideEffectIdempotently(Job job) throws Exception {
        if (jobSideEffectRepository.existsById(job.getId())) {
            log.info("Efeito colateral do job {} já tinha sido aplicado — pulando reexecução", job.getId());
            return;
        }
        sideEffectExecutor.execute(job);
        jobSideEffectRepository.markAppliedIfAbsent(job.getId());
    }

    private void recordCompletionEvent(Job job) {
        // Mesma transação do markDone(): outbox e mudança de negócio
        // commitam juntos, ou nenhum dos dois commita.
        outboxEventRepository.save(new OutboxEvent(
                "job", job.getId(), "job.completed",
                "{\"jobId\":\"" + job.getId() + "\",\"jobType\":\"" + job.getJobType() + "\"}"));
    }

    private void handleFailure(Job job, Exception e) {
        if (job.hasAttemptsLeft()) {
            Duration delay = backoffFor(job.getAttempts());
            job.scheduleRetry(OffsetDateTime.now().plus(delay));
            log.warn("Job {} falhou (tentativa {}/{}): {} — nova tentativa em {}",
                    job.getId(), job.getAttempts(), job.getMaxAttempts(), e.getMessage(), delay);
        } else {
            job.markFailed();
            log.error("Job {} esgotou as {} tentativas — marcado como failed: {}",
                    job.getId(), job.getMaxAttempts(), e.getMessage());
        }
    }

    private Duration backoffFor(int attempt) {
        long seconds = BASE_DELAY.getSeconds() * (1L << (attempt - 1));
        return Duration.ofSeconds(Math.min(seconds, MAX_DELAY.getSeconds()));
    }
}
