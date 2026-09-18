package dev.victorloures.ledger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@Service
public class JobScannerService {

    private static final Logger log = LoggerFactory.getLogger(JobScannerService.class);

    private final JobClaimService jobClaimService;
    private final JobProcessingService jobProcessingService;

    public JobScannerService(JobClaimService jobClaimService, JobProcessingService jobProcessingService) {
        this.jobClaimService = jobClaimService;
        this.jobProcessingService = jobProcessingService;
    }

    @Scheduled(fixedDelay = 5000)
    public void scan() {
        // Chamada a outro bean (não this.x()): passa pelo proxy do Spring,
        // o @Transactional de claimDueJobs() funciona de verdade.
        List<UUID> claimedIds = jobClaimService.claimDueJobs();
        log.info("Scan (thread {}): {} job(s) reivindicado(s)", Thread.currentThread().getName(), claimedIds.size());

        for (UUID jobId : claimedIds) {
            try {
                jobProcessingService.processAsync(jobId);
            } catch (RejectedExecutionException e) {
                // Pool + fila cheios. Diferente do Módulo 4: aqui o job JÁ
                // foi reivindicado (status = running, commitado). Só
                // logamos — o Módulo 7 (observabilidade) seria o lugar de
                // alertar sobre isso de verdade.
                log.warn("Pool de jobs cheio, job {} já reivindicado ficará parado até haver vaga", jobId);
            }
        }
    }
}
