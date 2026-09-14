package dev.victorloures.ledger.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID id) {
        super("Job não encontrado: " + id);
    }
}
