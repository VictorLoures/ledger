-- Módulo 9 — front-end precisa de uma ação real de "cancelar job", que não
-- existia em nenhum módulo anterior. Adiciona 'cancelled' como estado
-- terminal válido, ao lado de done/failed.
ALTER TABLE jobs DROP CONSTRAINT jobs_status_check;
ALTER TABLE jobs ADD CONSTRAINT jobs_status_check
    CHECK (status IN ('pending', 'running', 'done', 'failed', 'cancelled'));
