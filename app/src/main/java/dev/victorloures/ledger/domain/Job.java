package dev.victorloures.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Lock otimista: defesa em camadas, complementar ao lock pessimista do
    // claim da fila (ver JobRepository.lockDueJobs). Protege qualquer OUTRO
    // caminho de escrita que venha a existir (ex.: cancelar via API) contra
    // sobrescrever silenciosamente uma mudança concorrente.
    @Version
    @Column(nullable = false)
    private long version;

    protected Job() {
        // exigido pelo JPA
    }

    public Job(UUID id, JobType jobType, String payload, OffsetDateTime scheduledAt) {
        this.id = id;
        this.jobType = jobType;
        this.payload = payload;
        this.scheduledAt = scheduledAt;
    }

    public UUID getId() {
        return id;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getPayload() {
        return payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.attempts++;
    }

    public void markDone() {
        this.status = JobStatus.DONE;
    }

    public void markFailed() {
        this.status = JobStatus.FAILED;
    }

    public boolean hasAttemptsLeft() {
        return attempts < maxAttempts;
    }

    // Volta pra pending com scheduled_at no futuro: reaproveita o scanner do
    // Módulo 4 como mecanismo de retry, sem infraestrutura nova.
    public void scheduleRetry(OffsetDateTime nextAttemptAt) {
        this.status = JobStatus.PENDING;
        this.scheduledAt = nextAttemptAt;
    }
}
