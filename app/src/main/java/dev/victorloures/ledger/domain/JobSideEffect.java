package dev.victorloures.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// Existe só como âncora de mapeamento pro Spring Data (JpaRepository precisa
// de uma entidade) — a escrita de verdade acontece via query nativa
// INSERT ... ON CONFLICT DO NOTHING em JobSideEffectRepository.
@Entity
@Table(name = "job_side_effects")
public class JobSideEffect {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private OffsetDateTime appliedAt;

    protected JobSideEffect() {
        // exigido pelo JPA
    }
}
