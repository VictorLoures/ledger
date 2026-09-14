-- Módulo 1, Exercício 2 — queries manuais para os principais estados de consulta

-- =========================================================
-- 1. Jobs pendentes prontos para execução agora
-- =========================================================
-- "Prontos" = status pending E scheduled_at já chegou (não adianta pegar um
-- job agendado pra daqui a 1 hora). Ordena pelos mais atrasados primeiro.
-- LIMIT simula o "lote" que um worker pegaria numa varredura.
SELECT id, job_type, payload, scheduled_at
FROM jobs
WHERE status = 'pending'
  AND scheduled_at <= now()
ORDER BY scheduled_at
LIMIT 10;

-- Casa exatamente com o índice parcial idx_jobs_pending_scheduled
-- (mesma coluna no WHERE do índice, mesma coluna de ORDER BY).


-- =========================================================
-- 2. Jobs travados há mais de X minutos
-- =========================================================
-- "Travado" = ficou em 'running' e não foi atualizado (updated_at) há tempo
-- demais — sinal de que o worker que pegou o job morreu/travou no meio do
-- processamento, sem marcar done/failed. Parametrizamos X como intervalo.
SELECT id, job_type, updated_at,
       now() - updated_at AS tempo_travado
FROM jobs
WHERE status = 'running'
  AND updated_at < now() - INTERVAL '5 minutes'
ORDER BY updated_at;

-- Casa com o índice parcial idx_jobs_running_updated.


-- =========================================================
-- 3. Histórico de execuções de um job específico
-- =========================================================
-- Todas as tentativas já feitas para um job, da mais antiga pra mais recente,
-- pra reconstruir "o que aconteceu com esse job" (auditoria/debug).
SELECT attempt_number, status, started_at, finished_at, error_message
FROM job_executions
WHERE job_id = '00000000-0000-0000-0000-000000000000'  -- substituir pelo id real
ORDER BY attempt_number;

-- Usa o índice idx_job_executions_job_id na FK.
