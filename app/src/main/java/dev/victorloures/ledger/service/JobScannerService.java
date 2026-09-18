package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.JobStatus;
import dev.victorloures.ledger.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.RejectedExecutionException;

@Service
public class JobScannerService {

    private static final Logger log = LoggerFactory.getLogger(JobScannerService.class);

    private final JobRepository jobRepository;
    private final JobProcessingService jobProcessingService;

    public JobScannerService(JobRepository jobRepository, JobProcessingService jobProcessingService) {
        this.jobRepository = jobRepository;
        this.jobProcessingService = jobProcessingService;
    }

    @Scheduled(fixedDelay = 5000)
    public void scan() {
        var dueJobs = jobRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                JobStatus.PENDING, OffsetDateTime.now());

        log.info("Scan (thread {}): {} job(s) pendente(s) prontos", Thread.currentThread().getName(), dueJobs.size());

        for (Job job : dueJobs) {
            try {
                jobProcessingService.processAsync(job.getId());
            } catch (RejectedExecutionException e) {
                // Pool + fila cheios: o job nunca chegou a rodar (rejeitado
                // antes de markRunning()), então continua "pending" e será
                // pego de novo sozinho na próxima varredura.
                log.warn("Pool de jobs cheio, job {} fica pending pra próxima varredura", job.getId());
            }
        }
    }
}
