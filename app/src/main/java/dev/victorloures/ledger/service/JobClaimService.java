package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.JobStatus;
import dev.victorloures.ledger.repository.JobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class JobClaimService {

    // Tamanho do lote reivindicado por varredura — limita quanto trabalho um
    // scan tenta empurrar pro pool de uma vez (complementa o próprio limite
    // do pool, que já rejeita o excedente).
    private static final int BATCH_SIZE = 10;

    private final JobRepository jobRepository;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // Bean separado de JobScannerService DE PROPÓSITO: se esse método
    // vivesse na mesma classe que o chama, a chamada seria self-invocation
    // (this.claimDueJobs()) e o @Transactional seria silenciosamente
    // ignorado — exatamente o bug do Módulo 4, e que apareceu de verdade
    // aqui até esse método ser extraído (log real: "No active transaction").
    //
    // A transação precisa existir: o SELECT ... FOR UPDATE SKIP LOCKED só
    // bloqueia as linhas enquanto ela estiver aberta.
    @Transactional
    public List<UUID> claimDueJobs() {
        List<Job> claimed = jobRepository.lockDueJobs(
                JobStatus.PENDING, OffsetDateTime.now(), PageRequest.of(0, BATCH_SIZE));

        claimed.forEach(Job::markRunning);

        return claimed.stream().map(Job::getId).toList();
    }
}
