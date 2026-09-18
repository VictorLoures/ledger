package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.Job;

// Separado do orquestrador (JobProcessingService) de propósito: retry,
// idempotência e outbox são preocupações de confiabilidade, independentes
// de qual é o trabalho real do job. Também é o ponto de extensão pra quando
// cada job_type (email/webhook/reminder) tiver uma execução de verdade.
public interface JobSideEffectExecutor {

    void execute(Job job) throws Exception;
}
