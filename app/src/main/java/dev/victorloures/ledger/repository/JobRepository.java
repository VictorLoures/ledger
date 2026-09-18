package dev.victorloures.ledger.repository;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.JobStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    // PESSIMISTIC_WRITE = "SELECT ... FOR UPDATE". O hint de timeout=-2 é a
    // forma documentada do Hibernate de pedir SKIP LOCKED especificamente
    // (LockOptions.SKIP_LOCKED) em vez do FOR UPDATE simples, que bloquearia
    // esperando o lock em vez de pular a linha já travada.
    //
    // Precisa rodar dentro de uma transação (o lock só existe enquanto ela
    // estiver aberta) — ver JobScannerService.claimDueJobs.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.scheduledAt <= :now ORDER BY j.scheduledAt ASC")
    List<Job> lockDueJobs(@Param("status") JobStatus status, @Param("now") OffsetDateTime now, Pageable pageable);
}
