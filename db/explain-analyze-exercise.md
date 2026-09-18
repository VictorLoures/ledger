# Módulo 8, Exercícios 2 e 3 — EXPLAIN ANALYZE e impacto de índice

Rodado de verdade contra o Postgres do `docker-compose`, com 200.001 jobs
sintéticos gerados só pra esse exercício (removidos depois — ver final).

## Exercício 2: achar a query lenta e ler o plano

A aplicação guarda o payload do job como `JSONB`. Uma busca plausível que
ainda não existe no código, mas que uma feature futura precisaria (ex.:
"achar todos os jobs de e-mail enviados pra um destinatário específico"):

```sql
SELECT id, status FROM jobs WHERE payload->>'to' = 'target@example.com';
```

Sem nenhum índice na expressão `payload->>'to'`, o plano é:

```
 Gather  (cost=1000.00..5763.38 rows=1000 width=21) (actual time=11.303..14.903 rows=1 loops=1)
   Workers Planned: 2
   Workers Launched: 2
   ->  Parallel Seq Scan on jobs  (cost=0.00..4663.38 rows=417 width=21) (actual time=8.811..8.813 rows=0 loops=3)
         Filter: ((payload ->> 'to'::text) = 'target@example.com'::text)
         Rows Removed by Filter: 66667
 Planning Time: 0.534 ms
 Execution Time: 14.970 ms
```

**Como ler isso:**
- `Parallel Seq Scan` = Postgres não tinha nenhuma estrutura pra pular direto
  pro dado; teve que ler a tabela inteira (dividida em 3 workers paralelos
  pra acelerar, mas ainda é força bruta).
- `Rows Removed by Filter: 66667` (por worker) = a esmagadora maioria do
  trabalho foi descartada — sinal claro de que falta um índice pra essa
  condição.
- `actual time=11.303..14.903` = tempo real de execução (não estimativa),
  a parte que mais importa no `EXPLAIN ANALYZE` (o `EXPLAIN` sozinho, sem
  `ANALYZE`, só mostra o *plano estimado*, sem rodar a query de verdade).

O problema: **índices "normais" indexam a coluna inteira**, não uma
expressão calculada em cima dela (`payload->>'to'` extrai um campo de dentro
do JSON). Índice comum na coluna `payload` não ajudaria essa busca.

## Exercício 3: medir o impacto de um índice — antes/depois

Solução: um **índice de expressão**, que indexa o *resultado* de
`payload->>'to'`, não a coluna bruta:

```sql
CREATE INDEX idx_jobs_payload_to ON jobs ((payload->>'to'));
```

Mesma query, depois do índice:

```
 Index Scan using idx_jobs_payload_to on jobs  (cost=0.42..8.44 rows=1 width=22) (actual time=0.039..0.039 rows=1 loops=1)
   Index Cond: ((payload ->> 'to'::text) = 'target@example.com'::text)
 Planning Time: 0.232 ms
 Execution Time: 0.055 ms
```

**Antes/depois:**

| | Sem índice | Com índice |
|---|---|---|
| Tipo de plano | Parallel Seq Scan (3 workers) | Index Scan |
| Execution Time | 14.970 ms | 0.055 ms |
| Linhas varridas | ~200.000 (tabela inteira) | 1 (direto pro resultado) |

**~270x mais rápido** nessa massa de dados — e a diferença só cresce
conforme a tabela cresce (seq scan é `O(n)`; index scan em uma B-tree é
`O(log n)`).

## Por que o índice NÃO ficou no schema do projeto

Esse índice foi criado, medido e **removido** depois do exercício — a
aplicação não tem hoje nenhuma feature que busca job por destinatário no
payload. Criar um índice sem uma query real que o use é puro custo (espaço
em disco + toda escrita na tabela fica mais lenta) sem benefício nenhum.
Fica documentado aqui como referência: se um dia existir essa busca, **este**
é o índice certo pra ela.

```sql
-- Limpeza rodada depois do exercício:
DROP INDEX idx_jobs_payload_to;
DELETE FROM jobs WHERE payload->>'to' LIKE 'user%@example.com' OR payload->>'to' = 'target@example.com';
```
