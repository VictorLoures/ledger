package dev.victorloures.ledger.repository;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    // Mesma condição do Módulo 1 (db/queries.sql): pending E já vencido,
    // casando com o índice parcial idx_jobs_pending_scheduled.
    List<Job> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(JobStatus status, OffsetDateTime now);
}
