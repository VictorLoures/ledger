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
**Status:** ainda não iniciado

- [x] Módulo 1 — Modelagem de dados no PostgreSQL
- [ ] Módulo 2 — Docker e Containerização
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
