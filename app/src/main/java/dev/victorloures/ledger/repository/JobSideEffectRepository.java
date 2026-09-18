package dev.victorloures.ledger.repository;

import dev.victorloures.ledger.domain.JobSideEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface JobSideEffectRepository extends JpaRepository<JobSideEffect, UUID> {

    // Retorna 1 se essa foi a primeira vez (linha inserida) ou 0 se o efeito
    // já tinha sido aplicado antes — o próprio banco decide, via PK, sem
    // condição de corrida entre "checar" e "aplicar".
    @Modifying
    @Query(value = "INSERT INTO job_side_effects (job_id) VALUES (:jobId) ON CONFLICT (job_id) DO NOTHING", nativeQuery = true)
    int markAppliedIfAbsent(UUID jobId);
}
