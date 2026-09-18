package dev.victorloures.ledger;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.JobStatus;
import dev.victorloures.ledger.domain.JobType;
import dev.victorloures.ledger.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Módulo 6, Exercício 2: prova que dois "workers" concorrentes nunca
// reivindicam o mesmo job. Em vez de só disparar duas threads e torcer pra
// elas coincidirem no tempo (não-determinístico), uso TransactionTemplate
// pra forçar a sobreposição de verdade: a transação A fica com o lock
// aberto (segurando FOR UPDATE) até a transação B já ter terminado a dela.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JobConcurrencyTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void skipLockedPreventsTwoWorkersFromClaimingTheSameJob() throws Exception {
        List<UUID> jobIds = List.of(
                createDueJob(), createDueJob(), createDueJob(), createDueJob());

        CountDownLatch workerAHasLocked = new CountDownLatch(1);
        CountDownLatch workerBFinished = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Worker A: reivindica 2 jobs e SEGURA a transação aberta (não
        // comita) até B terminar — simulando A ainda "no meio" do claim
        // exatamente quando B tenta reivindicar também.
        Future<List<UUID>> resultA = pool.submit(() -> tx.execute(status -> {
            List<UUID> claimed = claim(2);
            workerAHasLocked.countDown();
            awaitQuietly(workerBFinished);
            return claimed;
        }));

        assertThat(workerAHasLocked.await(5, TimeUnit.SECONDS)).isTrue();

        // Worker B roda enquanto A ainda segura o lock nos 2 jobs dele —
        // SKIP LOCKED deve pular essas linhas e trazer só as 2 restantes.
        Future<List<UUID>> resultB = pool.submit(() -> tx.execute(status -> claim(2)));
        List<UUID> claimedByB = resultB.get(5, TimeUnit.SECONDS);
        workerBFinished.countDown();
        List<UUID> claimedByA = resultA.get(5, TimeUnit.SECONDS);

        pool.shutdown();

        assertThat(claimedByA).hasSize(2);
        assertThat(claimedByB).hasSize(2);
        assertThat(Collections.disjoint(claimedByA, claimedByB))
                .as("nenhum job pode ser reivindicado pelos dois workers")
                .isTrue();

        var allClaimed = new HashSet<UUID>();
        allClaimed.addAll(claimedByA);
        allClaimed.addAll(claimedByB);
        assertThat(allClaimed).containsExactlyInAnyOrderElementsOf(jobIds);
    }

    private List<UUID> claim(int limit) {
        List<Job> claimed = jobRepository.lockDueJobs(
                JobStatus.PENDING, OffsetDateTime.now(), PageRequest.of(0, limit));
        claimed.forEach(Job::markRunning);
        return claimed.stream().map(Job::getId).toList();
    }

    private UUID createDueJob() {
        Job job = jobRepository.save(new Job(UUID.randomUUID(), JobType.EMAIL,
                "{\"to\":\"teste@example.com\"}", OffsetDateTime.now()));
        return job.getId();
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
