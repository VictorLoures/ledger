# Módulo 1, Exercício 3 — Simulando condição de corrida

Roteiro pra rodar quando o Postgres estiver no ar (a partir do Módulo 2, via
Docker). Precisa de **duas sessões `psql` abertas ao mesmo tempo** (dois
terminais), simulando dois workers concorrentes tentando pegar o mesmo job.

Setup (rodar uma vez, em qualquer uma das sessões):

```sql
INSERT INTO jobs (id, job_type, payload, scheduled_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'email',
        '{"to": "teste@example.com"}', now())
RETURNING id;
```

---

## Parte A — `READ COMMITTED` (default, mostra o bug)

| Sessão A (Worker A) | Sessão B (Worker B) |
|---|---|
| `BEGIN;` | |
| `SELECT status FROM jobs WHERE id = '111...';`<br>→ vê `pending` | |
| | `BEGIN;` |
| | `SELECT status FROM jobs WHERE id = '111...';`<br>→ também vê `pending` |
| `UPDATE jobs SET status = 'running', updated_at = now() WHERE id = '111...';` | |
| `COMMIT;` | |
| | `UPDATE jobs SET status = 'running', updated_at = now() WHERE id = '111...';`<br>→ **não trava, não dá erro** — sobrescreve em cima do commit de A |
| | `COMMIT;` |

**O que observar:** os dois workers leram `pending` e os dois conseguiram fazer
o `UPDATE` sem erro nenhum. Do ponto de vista de cada um, "eu peguei o job".
Resultado: o job seria processado duas vezes. Esse é o bug de duplo-processamento
que motiva o Módulo 6.

Resetar pro próximo teste:
```sql
UPDATE jobs SET status = 'pending' WHERE id = '11111111-1111-1111-1111-111111111111';
```

---

## Parte B — `REPEATABLE READ` (detecta o conflito)

| Sessão A | Sessão B |
|---|---|
| `BEGIN ISOLATION LEVEL REPEATABLE READ;` | |
| `SELECT status FROM jobs WHERE id = '111...';`<br>→ `pending` | |
| | `BEGIN ISOLATION LEVEL REPEATABLE READ;` |
| | `SELECT status FROM jobs WHERE id = '111...';`<br>→ `pending` |
| `UPDATE jobs SET status = 'running', updated_at = now() WHERE id = '111...';` | |
| `COMMIT;` | |
| | `UPDATE jobs SET status = 'running', updated_at = now() WHERE id = '111...';`<br>→ **trava esperando A, e ao continuar retorna erro**:<br>`ERROR: could not serialize access due to concurrent update` |
| | `ROLLBACK;` |

**O que observar:** dessa vez o Postgres percebe que a linha mudou desde o
snapshot que B tirou no início da transação, e recusa o `UPDATE` de B em vez
de deixar sobrescrever silenciosamente. B precisa tratar esse erro e tentar
de novo (retry na aplicação) — mas repare que isso ainda não é a solução ideal
pra fila de jobs: B só descobre o conflito depois de tentar, e fica preso
esperando A até ali. O Módulo 6 resolve isso de forma mais direta com
`SELECT ... FOR UPDATE SKIP LOCKED`, que faz B nem tentar pegar um job que já
está sendo processado, sem travar nem dar erro.

Resetar pro próximo teste:
```sql
UPDATE jobs SET status = 'pending' WHERE id = '11111111-1111-1111-1111-111111111111';
```
