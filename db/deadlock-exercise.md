# Módulo 6, Exercício 3 — Provocando e diagnosticando um deadlock

## Como um deadlock acontece

Um deadlock ocorre quando duas transações travam recursos **na ordem oposta**:
transação A trava a linha 1 e depois quer a linha 2; transação B trava a linha 2
e depois quer a linha 1. As duas ficam esperando uma pela outra pra sempre — um
ciclo de espera. O Postgres tem um detector de deadlock que roda periodicamente
(`deadlock_timeout`, default 1s): ao achar o ciclo, ele **aborta uma das duas
transações** (a "vítima", escolhida por critérios internos) com
`ERROR: deadlock detected`, liberando a outra pra continuar.

Isso é diferente da condição de corrida do Módulo 1 (duas transações lendo o
mesmo dado desatualizado) — aqui o problema é puramente de **ordem de
aquisição de locks**.

## Roteiro (duas sessões `psql` + uma de diagnóstico)

Setup:
```sql
INSERT INTO jobs (id, job_type, payload, scheduled_at) VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001', 'email', '{}', now() + interval '1 day'),
  ('aaaaaaaa-0000-0000-0000-000000000002', 'email', '{}', now() + interval '1 day');
```

**Sessão A** — trava o job `...0001` primeiro, depois tenta o `...0002`:
```sql
BEGIN;
UPDATE jobs SET status = 'running' WHERE id = 'aaaaaaaa-0000-0000-0000-000000000001'; -- t=0
SELECT pg_sleep(2);
UPDATE jobs SET status = 'running' WHERE id = 'aaaaaaaa-0000-0000-0000-000000000002'; -- t=2, bloqueia
COMMIT;
```

**Sessão B** — trava o job `...0002` primeiro, depois tenta o `...0001` (ordem
oposta de A, fechando o ciclo):
```sql
BEGIN;
SELECT pg_sleep(1);
UPDATE jobs SET status = 'running' WHERE id = 'aaaaaaaa-0000-0000-0000-000000000002'; -- t=1
SELECT pg_sleep(3);
UPDATE jobs SET status = 'running' WHERE id = 'aaaaaaaa-0000-0000-0000-000000000001'; -- t=4, bloqueia -> deadlock
COMMIT;
```

## O que aconteceu (rodado de verdade contra o Postgres do docker-compose)

Sessão A completou normalmente:
```
A: t=0 trava job 0001
UPDATE 1
A: t=2 tentando travar job 0002 (B já travou em t=1)
UPDATE 1
COMMIT
```

Sessão B foi a vítima escolhida pelo Postgres:
```
B: t=1 trava job 0002
UPDATE 1
B: t=4 tentando travar job 0001 (A já travou em t=0) -- fecha o ciclo
ERROR:  deadlock detected
DETAIL:  Process 2813 waits for ShareLock on transaction 792; blocked by process 2811.
Process 2811 waits for ShareLock on transaction 793; blocked by process 2813.
HINT:  See server log for query details.
CONTEXT:  while updating tuple (0,38) in relation "jobs"
ROLLBACK
```

O `DETAIL` já **é** o diagnóstico: o próprio Postgres identifica os dois
processos e o ciclo (`2813 espera 2811` e `2811 espera 2813`).

## Diagnosticando via `pg_locks` / `pg_stat_activity` (antes do Postgres resolver sozinho)

Uma terceira sessão, consultando durante a janela de espera (entre A bloquear
em t=2 e B fechar o ciclo em t=4), mostra a mesma cadeia circular:

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.query AS blocked_query,
    blocking.pid AS blocking_pid,
    blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks ON blocked_locks.pid = blocked.pid AND NOT blocked_locks.granted
JOIN pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.pid != blocked_locks.pid
    AND blocking_locks.granted
JOIN pg_stat_activity blocking ON blocking.pid = blocking_locks.pid;
```

Resultado real capturado (uma linha pra cada direção do ciclo):
```
 blocked_pid |                  blocked_query                   | blocking_pid |                  blocking_query
-------------+---------------------------------------------------+--------------+---------------------------------------------------
        2813 | UPDATE jobs ... WHERE id = '...0001'               |         2811 | UPDATE jobs ... WHERE id = '...0002'
        2811 | UPDATE jobs ... WHERE id = '...0002'               |         2813 | UPDATE jobs ... WHERE id = '...0001'
```

Em produção, essa é a query que se roda contra `pg_locks`/`pg_stat_activity`
**enquanto o sistema está travado** (antes do deadlock se resolver sozinho, ou
pra diagnosticar locks demorados que não chegam a virar deadlock).

## Por que isso não acontece no nosso `JobClaimService`

O `SELECT ... FOR UPDATE SKIP LOCKED` do Módulo 6 evita esse cenário por
construção: se uma linha já está travada, a transação **pula** ela em vez de
esperar (e nunca fica seguradando um lock enquanto pede outro na mesma ordem
que causaria um ciclo). Deadlock é um risco de transações que fazem múltiplos
`UPDATE`s manuais em ordens diferentes — o padrão certo pra evitar é sempre
adquirir locks múltiplos **na mesma ordem** (ex.: sempre por `id` crescente),
ou usar `SKIP LOCKED`/`NOWAIT` quando a aplicação permite pular em vez de
esperar.
