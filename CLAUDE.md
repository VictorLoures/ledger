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

**Módulo atual:** nenhum — trilha completa (Módulos 1-9)
**Status:** Módulos 6-9 implementados de forma autônoma (usuário pediu pra
seguir sem pausar pra perguntas — ver commits individuais de cada módulo
pra detalhes e "porquês"; dúvidas ficam pra revisão depois).

- [x] Módulo 1 — Modelagem de dados no PostgreSQL
- [x] Módulo 2 — Docker e Containerização
- [x] Módulo 3 — Spring Boot com boas práticas (+ Flyway, fora do escopo original)
- [x] Módulo 4 — Agendamento e execução assíncrona
- [x] Módulo 5 — Confiabilidade: retry, idempotência, outbox pattern
- [x] Módulo 6 — Concorrência no banco
- [x] Módulo 7 — Observabilidade e API profissional
- [x] Módulo 8 — SQL avançado
- [x] Módulo 9 — Front-end básico (visualização)

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
  trilha original — decisão do usuário. `db/schema.sql` foi convertido em
  `app/src/main/resources/db/migration/V1__init_schema.sql`, que o Flyway aplica
  sozinho na subida da aplicação — substituindo a aplicação manual de DDL usada
  nos Módulos 1-2.
- **Esqueleto Spring Boot** (`app/`): Maven, Java 21, Spring Boot 4.1.1 (nota: essa
  versão usa Jackson 3 sob o pacote `tools.jackson.*`, não mais `com.fasterxml.jackson.*`,
  e módulos de teste splitados como `spring-boot-starter-webmvc-test` em vez do antigo
  `spring-boot-starter-test` monolítico). Camadas controller/service/repository com
  constructor injection; DTOs (`record`) na borda da API, nunca a entidade JPA
  diretamente; `JobStatus`/`JobType` mapeados via `AttributeConverter` customizado
  (não `@Enumerated` — os dois juntos no mesmo campo fazem o JPA ignorar o converter,
  bug real que apareceu e foi corrigido); `ddl-auto: validate` (Flyway é quem aplica
  schema, Hibernate só confere o mapeamento); `open-in-view: false`.
- **`GlobalExceptionHandler` (`@RestControllerAdvice`)** confirmado por demonstração ao
  vivo (Exercício 3 do Módulo 3): sem ele, `JobNotFoundException` sobe como erro 500
  genérico e indistinguível de um bug real; com ele, vira 404 estruturado.
- **Docker da aplicação** (`app/Dockerfile`, multi-stage: `maven:3.9-eclipse-temurin-21`
  pra build, `eclipse-temurin:21-jre-alpine` pra rodar, usuário não-root) integrado ao
  `docker-compose.yml` com `depends_on: condition: service_healthy` no Postgres.
  Validado de ponta a ponta: `docker compose up` sozinho sobe banco vazio, Flyway aplica
  a V1, app responde em `:8080` — sem nenhum `psql` manual.
- **Módulo 4 (scanner + processamento assíncrono)**: `JobScannerService` com
  `@Scheduled(fixedDelay = 5000)` (não `fixedRate`, pra nunca sobrepor varreduras)
  busca jobs `pending` vencidos e delega a `JobProcessingService.processAsync(UUID)`
  — método `@Async("jobExecutor")`, pool dedicado (`AsyncConfig`, core=2/max=2/fila=2,
  pequeno de propósito). `processAsync` recebe o **id** do job, não a entidade (nunca
  passar entidade JPA entre threads/transações).
- **Bug de self-invocation demonstrado ao vivo**: com scan+processamento na mesma
  classe (`this.processAsync(...)`), a chamada não passa pelo proxy do Spring — nem
  `@Async` nem `@Transactional` funcionam. Provado por log (thread sempre
  `scheduling-1`, nunca `job-worker-*`) e por persistência (mudança de status nunca
  foi salva, já que `@Transactional` também foi ignorado). Corrigido movendo o método
  `@Async` pra um bean separado (`JobProcessingService`).
- **Esgotamento do pool tratado como backpressure natural**: quando o pool+fila enchem,
  `processAsync` lança `RejectedExecutionException` **antes** de `markRunning()`, então
  o job nunca muda de status — continua `pending` e é automaticamente re-tentado na
  próxima varredura. `JobScannerService.scan()` captura essa exceção e loga um `WARN`
  limpo em vez de deixar vazar o stacktrace do framework.
- **Módulo 5 (retry, idempotência, outbox)**: migration `V2__reliability.sql`
  adiciona `job_side_effects` (guarda de idempotência) e `outbox_events`.
  `JobProcessingService.processAsync` agora retorna `CompletableFuture<Void>`
  (não `void`) — necessário pra testar sem depender de sleep/timing, e é
  também a correção padrão pra "exceção @Async engolida". Falha → calcula
  backoff exponencial (`2s,4s,8s...` até 1min) e reagenda via
  `job.scheduleRetry(...)` (reaproveita o scanner do Módulo 4, sem
  infraestrutura nova); esgotou `max_attempts` → `markFailed()`. Efeito
  colateral e conclusão gravados na mesma transação que o evento outbox
  (`recordCompletionEvent`), evitando dual-write. `OutboxPublisherService`
  (`@Scheduled`) publica (simulado via log) eventos pendentes.
- **Bug real pego pelos testes automatizados**: a primeira versão marcava o
  job em `job_side_effects` **antes** de rodar o efeito colateral — então uma
  tentativa que falhava já ficava "marcada como aplicada" e o retry nunca
  reexecutava de verdade. Corrigido invertendo a ordem: checa se já foi
  aplicado, executa, só marca depois do sucesso. `JobProcessingServiceTest`
  mocka `JobSideEffectExecutor` (não sleep/tempo real) pra validar retry,
  esgotamento de tentativas e idempotência de forma determinística.
- **Limitação conhecida, adiada pro Módulo 6**: o check-then-act em
  `job_side_effects` (existsById, depois insert) não é atômico sob
  processamento concorrente do mesmo job — só é seguro no cenário sequencial
  testado aqui. Prevenir dois workers pegando o mesmo job é o próprio
  objetivo do Módulo 6 (`FOR UPDATE SKIP LOCKED`).
- **Módulo 6 (concorrência)**: `JobRepository.lockDueJobs` usa
  `@Lock(PESSIMISTIC_WRITE)` + hint `jakarta.persistence.lock.timeout=-2`
  (forma documentada do Hibernate de pedir `SKIP LOCKED`, não só `FOR UPDATE`).
  Extraído pra bean próprio (`JobClaimService`) — **de novo** o bug de
  self-invocation do Módulo 4 apareceu de verdade aqui (`claimDueJobs()`
  chamado via `this.x()` dentro de `JobScannerService` → `@Transactional`
  ignorado → erro real em runtime `No active transaction`, pego rodando os
  testes). `JobConcurrencyTest` prova com `TransactionTemplate` (não
  threads torcendo pra coincidir no tempo) que duas transações concorrentes
  nunca reivindicam o mesmo job. Lock otimista (`@Version` em `Job`,
  migration V3) adicionado como defesa em camadas complementar, não
  substituto do lock pessimista da fila. Deadlock real provocado e
  diagnosticado contra o Postgres do compose — evidências e roteiro em
  `db/deadlock-exercise.md`.
- **Módulo 7 (observabilidade)**: `CorrelationIdFilter` gera/propaga
  `X-Correlation-Id` por requisição HTTP via MDC. Para o processamento
  assíncrono, `MdcTaskDecorator` (registrado no `jobExecutor`) copia o MDC da
  thread que chama `@Async` pra thread do pool — sem isso, MDC (baseado em
  ThreadLocal) simplesmente não atravessa a troca de thread; `jobId` é
  setado pelo `JobScannerService` job a job antes de disparar, então cada
  `job-worker-N` loga com o id do job certo. Confirmado ao vivo contra o
  compose: `X-Correlation-Id` ecoado na resposta, `jobId` aparecendo nos
  logs do worker. Métricas via Actuator + Micrometer (`jobs.completed`,
  `jobs.failed`, `jobs.retried`, `jobs.processing.duration`) expostas em
  `/actuator/metrics`. Validação via Bean Validation em `CreateJobRequest`
  (sem `@Future` em `scheduledAt` de propósito — agendar pro passado/agora
  significa "processar assim que possível", usado nos próprios
  testes/demos). API versionada em `/api/v1/jobs`. `GlobalExceptionHandler`
  ganhou handler de validação (400 com lista de campos) e um
  `@ExceptionHandler(Exception.class)` genérico — nenhum endpoint, nem os
  não previstos, deveria vazar stacktrace ou Whitelabel Error Page.
- **Módulo 8 (SQL avançado)**: só SQL/documentação, sem mudança de código
  Java. `db/reports.sql` — duas window functions rodadas contra dado real
  (jobs concluídos por hora com acumulado via `SUM() OVER`; taxa de falha
  por tipo via `COUNT(*) FILTER (...) OVER (PARTITION BY job_type)`, sem
  `GROUP BY`/self-join). `db/explain-analyze-exercise.md` — 200 mil jobs
  sintéticos gerados só pro exercício: busca por `payload->>'to'` sem índice
  rodou `Parallel Seq Scan`, 14.97ms, varrendo a tabela inteira; criando um
  **índice de expressão** (`(payload->>'to')` — índice comum não serve pra
  campo extraído de JSONB) virou `Index Scan`, 0.055ms (~270x). Índice e
  dados sintéticos foram removidos depois — a aplicação não tem hoje
  nenhuma busca por destinatário no payload, manter o índice seria custo
  sem benefício (fica documentado como referência se a feature existir).
- **Módulo 9 (front-end)**: precisou de duas ações novas na API que não
  existiam (`PATCH /api/v1/jobs/{id}/cancel`, `.../reschedule`) — adicionado
  status `CANCELLED` (migration V4, `Job.cancel()`/`Job.reschedule()` com
  guardas de transição de estado: cancelar só de `pending`/`failed`;
  reagendar não de `running`/`done`; violação vira `InvalidJobStateException`
  → 409). `reschedule` reseta `attempts` (é uma tentativa nova deliberada,
  diferente do retry automático que preserva o contador). `web/` — React +
  TS + Vite, só `fetch`/`useState` (sem lib de estado/requisição — projeto
  pequeno não justifica). Filtro de status é client-side (sem novo parâmetro
  na API). CORS liberado só pra `localhost:5173` (`WebConfig`). **Validado
  de ponta a ponta com Playwright real** (headless Chromium, sem sandbox):
  criar, listar, filtrar, cancelar e reagendar, zero erros de console —
  achou e confirmou a correção de um bug real de CORS (container não tinha
  sido rebuilded com o `WebConfig` novo) antes de fechar o módulo.
