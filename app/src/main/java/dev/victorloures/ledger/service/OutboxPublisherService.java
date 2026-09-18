package dev.victorloures.ledger.service;

import dev.victorloures.ledger.domain.OutboxEvent;
import dev.victorloures.ledger.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPending() {
        var pending = outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : pending) {
            // "Publicar" aqui é simulado (log). Num sistema real, seria um
            // publish num broker (Kafka/RabbitMQ) ou uma chamada HTTP — o
            // ponto do outbox é justamente separar "gravar que precisa ser
            // publicado" de "publicar de fato", que pode ser tentado de novo
            // sem afetar a transação de negócio original.
            log.info("Publicando evento outbox #{}: {} (aggregate {})",
                    event.getId(), event.getEventType(), event.getAggregateId());
            event.markPublished();
        }
    }
}
