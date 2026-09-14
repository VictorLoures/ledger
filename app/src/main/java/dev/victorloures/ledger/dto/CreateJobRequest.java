package dev.victorloures.ledger.dto;

import dev.victorloures.ledger.domain.JobType;

import java.time.OffsetDateTime;

public record CreateJobRequest(
        JobType jobType,
        String payload,
        OffsetDateTime scheduledAt
) {
}
