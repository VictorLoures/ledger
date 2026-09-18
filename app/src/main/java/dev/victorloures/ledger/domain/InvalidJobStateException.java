package dev.victorloures.ledger.domain;

import java.util.UUID;

public class InvalidJobStateException extends RuntimeException {

    public InvalidJobStateException(UUID jobId, JobStatus currentStatus, String acao) {
        super("Não é possível %s o job %s no estado atual (%s)".formatted(acao, jobId, currentStatus));
    }
}
