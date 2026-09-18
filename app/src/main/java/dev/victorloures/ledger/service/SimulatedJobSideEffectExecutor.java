package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Simula o trabalho real (enviar e-mail, chamar webhook) até esses
// integrações existirem de fato. O que importa pro Módulo 5 é o que
// acontece AO REDOR dessa chamada (JobProcessingService), não ela em si.
@Component
public class SimulatedJobSideEffectExecutor implements JobSideEffectExecutor {

    private static final Logger log = LoggerFactory.getLogger(SimulatedJobSideEffectExecutor.class);

    @Override
    public void execute(Job job) throws InterruptedException {
        log.info("Executando efeito colateral do job {} ({})", job.getId(), job.getJobType());
        Thread.sleep(500);
    }
}
