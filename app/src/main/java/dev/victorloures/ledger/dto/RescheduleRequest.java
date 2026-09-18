package dev.victorloures.ledger.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record RescheduleRequest(
        @NotNull(message = "scheduledAt é obrigatório")
        OffsetDateTime scheduledAt
) {
}
