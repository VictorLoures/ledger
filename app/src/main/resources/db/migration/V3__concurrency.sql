-- Módulo 6 — Concorrência no banco

-- Lock otimista: toda UPDATE em jobs passa a checar se a versão não mudou
-- desde a leitura (WHERE id = ? AND version = ?), incrementando version no
-- mesmo UPDATE. Se 0 linhas forem afetadas, outra transação já mudou a
-- linha primeiro — o Hibernate traduz isso em OptimisticLockException.
-- Complementa (não substitui) o lock pessimista usado pra reivindicar jobs
-- da fila (FOR UPDATE SKIP LOCKED) — ver JobRepository.lockDueJobs.
ALTER TABLE jobs ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
