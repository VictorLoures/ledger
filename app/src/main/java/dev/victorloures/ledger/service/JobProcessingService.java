package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class JobProcessingService {

    private static final Logger log = LoggerFactory.getLogger(JobProcessingService.class);

    private final JobRepository jobRepository;

    public JobProcessingService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // Bean separado do scanner: a chamada agora vem de fora, passa pelo proxy
    // do Spring, e o @Async funciona de verdade.
    @Async("jobExecutor")
    @Transactional
    public void processAsync(UUID jobId) {
        log.info("Processando job {} na thread {}", jobId, Thread.currentThread().getName());
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.markRunning();

        try {
            Thread.sleep(2000); // simula trabalho (chamada de webhook, envio de e-mail, etc.)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        job.markDone();
        log.info("Job {} concluído", jobId);
    }
}
