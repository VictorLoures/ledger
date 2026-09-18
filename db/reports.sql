-- Módulo 8, Exercício 1 — relatórios usando window functions

-- =========================================================
-- 1. Jobs concluídos por hora, com total acumulado
-- =========================================================
-- Window function (SUM() OVER) calcula o acumulado SEM precisar de subquery
-- nem de self-join — cada linha "vê" a soma de todas as linhas anteriores
-- na mesma ordenação, mantendo o detalhe por hora na mesma consulta.
SELECT
    date_trunc('hour', updated_at)                                    AS hora,
    count(*) FILTER (WHERE status = 'done')                           AS concluidos_na_hora,
    sum(count(*) FILTER (WHERE status = 'done')) OVER (
        ORDER BY date_trunc('hour', updated_at)
    )                                                                  AS acumulado
FROM jobs
GROUP BY date_trunc('hour', updated_at)
ORDER BY hora;


-- =========================================================
-- 2. Taxa de falha por tipo de job
-- =========================================================
-- COUNT(*) OVER (PARTITION BY job_type) dá o total do grupo em cada linha
-- sem GROUP BY — permite combinar "total do tipo" com "falhas desse tipo"
-- numa única passada, e ainda comparar o tipo com a taxa geral do sistema.
SELECT DISTINCT
    job_type,
    count(*) OVER (PARTITION BY job_type)                              AS total_por_tipo,
    count(*) FILTER (WHERE status = 'failed') OVER (PARTITION BY job_type)
        AS falhas_por_tipo,
    round(
        100.0 * count(*) FILTER (WHERE status = 'failed') OVER (PARTITION BY job_type)
        / count(*) OVER (PARTITION BY job_type),
        2
    )                                                                   AS taxa_falha_pct,
    round(100.0 * count(*) FILTER (WHERE status = 'failed') OVER () / count(*) OVER (), 2)
        AS taxa_falha_geral_pct
FROM jobs
ORDER BY taxa_falha_pct DESC;


-- =========================================================
-- 3. Ranking dos jobs com mais tentativas, por tipo
-- =========================================================
-- RANK() OVER (PARTITION BY ... ORDER BY ...) — "top N por grupo" é o caso
-- clássico que window function resolve bem e GROUP BY sozinho não resolve
-- (GROUP BY perde o detalhe da linha; aqui queremos manter o job inteiro).
SELECT job_type, id, attempts, status, rnk
FROM (
    SELECT
        job_type, id, attempts, status,
        RANK() OVER (PARTITION BY job_type ORDER BY attempts DESC) AS rnk
    FROM jobs
) ranked
WHERE rnk <= 3
ORDER BY job_type, rnk;
