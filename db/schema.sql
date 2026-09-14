-- Módulo 1 — Modelagem de dados no PostgreSQL
-- DDL puro (sem ORM). Ver CLAUDE.md > Decisões tomadas para o porquê de cada escolha.

-- =========================================================
-- jobs: estado atual de cada tarefa agendada
-- =========================================================
CREATE TABLE jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    job_type        VARCHAR(50) NOT NULL
                        CHECK (job_type IN ('reminder', 'email', 'webhook')),

    payload         JSONB NOT NULL,

    status          VARCHAR(20) NOT NULL DEFAULT 'pending'
                        CHECK (status IN ('pending', 'running', 'done', 'failed')),

    scheduled_at    TIMESTAMPTZ NOT NULL,

    attempts        INT NOT NULL DEFAULT 0
                        CHECK (attempts >= 0),
    max_attempts    INT NOT NULL DEFAULT 5
                        CHECK (max_attempts > 0),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Fila de trabalho: "me dê os jobs pendentes cujo horário já chegou, em ordem".
-- Parcial (WHERE status = 'pending') porque só nos interessa indexar a fatia
-- da tabela que o worker realmente varre — jobs 'done'/'failed' nunca entram
-- nessa busca, então não há motivo para inflar o índice com eles.
CREATE INDEX idx_jobs_pending_scheduled
    ON jobs (scheduled_at)
    WHERE status = 'pending';

-- Detectar jobs "travados": status = 'running' há tempo demais (worker morreu
-- no meio do processamento). Mesma lógica de índice parcial.
CREATE INDEX idx_jobs_running_updated
    ON jobs (updated_at)
    WHERE status = 'running';

-- =========================================================
-- job_executions: histórico append-only de cada tentativa de execução
-- =========================================================
CREATE TABLE job_executions (
    id              BIGSERIAL PRIMARY KEY,

    job_id          UUID NOT NULL REFERENCES jobs(id),
    attempt_number  INT NOT NULL
                        CHECK (attempt_number > 0),

    status          VARCHAR(20) NOT NULL
                        CHECK (status IN ('running', 'success', 'failed')),

    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    error_message   TEXT,

    UNIQUE (job_id, attempt_number)
);

-- Postgres NÃO cria índice automático em colunas de FK (diferente da PK).
-- Sem isso, buscar "histórico do job X" faria sequential scan em job_executions.
CREATE INDEX idx_job_executions_job_id
    ON job_executions (job_id);
