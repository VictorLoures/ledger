package dev.victorloures.ledger.dto;

import dev.victorloures.ledger.domain.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateJobRequest(
        @NotNull(message = "jobType é obrigatório")
        JobType jobType,

        @NotBlank(message = "payload é obrigatório")
        String payload,

        // Sem @Future de propósito: agendar "agora" ou num instante já
        // passado é um caso válido (significa "processar assim que possível"
        // — é como os próprios testes/exercícios anteriores criam jobs).
        @NotNull(message = "scheduledAt é obrigatório")
        OffsetDateTime scheduledAt
) {
}
