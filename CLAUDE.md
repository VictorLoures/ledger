# Ledger — Contexto do projeto

## O que é

Plataforma de agendamento e notificações com processamento assíncrono confiável
(retry, idempotência, outbox pattern). Projeto de estudo — ver `docs/trilha-estudo-backend.md`
para a trilha completa de módulos e exercícios.

## Stack

- Back-end: Java + Spring Boot
- Banco: PostgreSQL
- Containerização: Docker + Docker Compose (aplicação e banco sobem via `docker compose up`)
- Front-end: React + TypeScript (básico, só visualização — não é o foco do estudo)

## Como trabalhar comigo neste projeto

- Antes de implementar qualquer coisa de um módulo novo, **explique o conceito e as
  alternativas primeiro**. Só implemente depois que eu confirmar que entendi.
- No módulo de Docker, meu conhecimento é zero — explique os conceitos básicos
  (imagem, container, rede, volume) antes de qualquer Dockerfile ou compose file.
- Nos módulos de back-end e SQL, revisar o "porquê" de cada decisão é mais importante
  que entregar código rápido. Não pule essa parte mesmo se eu não pedir explicitamente.
- Seja específico e direto nas implementações — evite gerar código especulativo ou
  features não pedidas, para não desperdiçar contexto.
- Ao final de cada módulo, atualize a seção "Progresso" abaixo neste arquivo.

## Progresso

**Módulo atual:** 2 — Docker e Containerização
**Status:** em andamento — Postgres containerizado (docker-compose) feito;
Dockerfile da aplicação e integração no compose ficam pendentes até existir
código Java (Módulo 3), depois voltamos aqui pra fechar os exercícios
restantes.

- [x] Módulo 1 — Modelagem de dados no PostgreSQL
- [ ] Módulo 2 — Docker e Containerização (parcial: só Postgres)
- [ ] Módulo 3 — Spring Boot com boas práticas
- [ ] Módulo 4 — Agendamento e execução assíncrona
- [ ] Módulo 5 — Confiabilidade: retry, idempotência, outbox pattern
- [ ] Módulo 6 — Concorrência no banco
- [ ] Módulo 7 — Observabilidade e API profissional
- [ ] Módulo 8 — SQL avançado
- [ ] Módulo 9 — Front-end básico (visualização)

## Decisões tomadas

- **Status de job**: `VARCHAR` + `CHECK`, não enum nativo do Postgres nem tabela de lookup.
  Motivo: evoluir um enum nativo exige `ALTER TYPE` (restrito dentro de transações);
  lookup table é overkill para poucos valores fixos sem atributos próprios.
- **Payload do job**: coluna `payload JSONB`, não colunas esparsas por tipo nem tabela por tipo.
  Motivo: reminder/email/webhook têm formatos de dado diferentes; JSONB evita colunas
  cheias de NULL e joins desnecessários — é o padrão usado em filas de job reais.
- **Timestamps**: sempre `TIMESTAMPTZ`, nunca `TIMESTAMP` sem timezone.
- **Schema inicial** (Módulo 1, `db/schema.sql`): duas tabelas —
  `jobs` (estado atual, PK `UUID`) e `job_executions` (histórico append-only de tentativas,
  PK `BIGSERIAL`, FK para `jobs`). Índices parciais em `jobs` para as buscas de fila
  (`pending` por `scheduled_at`, `running` travado por `updated_at`) e índice explícito
  na FK de `job_executions` (Postgres não indexa FK automaticamente).
- **Exercício 3 do Módulo 1 confirmado empiricamente** (`db/concurrency-exercise.md`):
  rodando as duas transações concorrentes de verdade, `READ COMMITTED` deixou o
  segundo `UPDATE` sobrescrever silenciosamente sem erro (bug de duplo-processamento);
  `REPEATABLE READ` acusou `ERROR: could not serialize access due to concurrent update`.
- **Postgres containerizado** (`docker-compose.yml`): imagem `postgres:16.4-alpine`
  (versão fixada, não `latest`), volume nomeado `ledger_pgdata` pra persistir dados
  entre restarts, variáveis de ambiente via `.env` (não versionado — `.env.example`
  documenta as chaves necessárias), healthcheck com `pg_isready` pra sinalizar quando
  o banco está pronto pra conexões (importante quando a app entrar no compose).
- **Migrations (Flyway) adicionadas ao escopo do Módulo 3**, mesmo não estando na
  trilha original — decisão do usuário. `db/schema.sql` será convertido em
  migrations versionadas (`V1__...sql`, etc.) quando o esqueleto Spring Boot existir,
  substituindo a aplicação manual de DDL usada nos Módulos 1-2.
