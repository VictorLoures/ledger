-- Módulo 5 — Confiabilidade: idempotência e outbox pattern

-- Guarda de idempotência: o efeito colateral de um job só é "de fato aplicado"
-- uma vez, garantido pela PK (não por uma flag em memória, que não sobrevive
-- a um crash no meio do processamento).
CREATE TABLE job_side_effects (
    job_id     UUID PRIMARY KEY REFERENCES jobs(id),
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Outbox: evento gravado na MESMA transação que a mudança de negócio
-- (job -> done), evitando o problema de dual-write entre "salvar no banco"
-- e "publicar o evento".
CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(50) NOT NULL,
    payload        JSONB NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- O publicador só varre eventos ainda não publicados — parcial pelo mesmo
-- motivo dos índices de jobs no Módulo 1: é a única fatia que interessa.
CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (created_at)
    WHERE published_at IS NULL;
