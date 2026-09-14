package dev.victorloures.ledger.dto;

import dev.victorloures.ledger.domain.Job;
import dev.victorloures.ledger.domain.JobStatus;
import dev.victorloures.ledger.domain.JobType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        JobType jobType,
        String payload,
        JobStatus status,
        OffsetDateTime scheduledAt,
        int attempts
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getJobType(),
                job.getPayload(),
                job.getStatus(),
                job.getScheduledAt(),
                job.getAttempts()
        );
    }
}
