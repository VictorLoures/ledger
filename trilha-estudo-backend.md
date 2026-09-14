# Trilha de Estudo — Java/Spring, PostgreSQL, Docker e uso eficiente do Claude Code

## Objetivo

Aprofundar conhecimento em back-end (Java + Spring), banco de dados (PostgreSQL) e
containerização (Docker), partindo de um nível intermediário em Spring, básico em SQL
e zero em Docker, através da construção de um projeto real: uma **plataforma de
agendamento e notificações com processamento assíncrono**. O front-end (React + TS)
entra apenas como camada básica de visualização, sem ser o foco. Um eixo transversal
cobre como usar o Claude Code de forma eficiente (economia de tokens, quando delegar
vs. quando entender).

## Projeto: Ledger

Serviço que permite agendar tarefas (lembretes, envio de e-mail, chamadas a webhooks)
e processá-las de forma assíncrona e confiável, com retry, idempotência e outbox pattern.

---

## Módulo 1 — Modelagem de dados no PostgreSQL

**Objetivo:** entender profundamente o schema antes de escrever qualquer linha de Java.

- Modelar tabelas de `jobs`/`tarefas`, estados (pending, running, done, failed), histórico de execução
- Tipos de dados corretos (timestamps com timezone, enums vs. tabelas de lookup)
- Chaves primárias, estrangeiras, constraints (`CHECK`, `UNIQUE`)
- Índices: quando criar, tipos (B-tree, parcial, composto)
- Transações: `BEGIN`/`COMMIT`/`ROLLBACK`, níveis de isolamento (`READ COMMITTED` vs `REPEATABLE READ`)

**Exercícios:**
1. Desenhar o schema completo (DDL puro, sem ORM) e justificar cada índice
2. Escrever queries manuais para os principais estados de consulta (jobs pendentes, jobs travados há X minutos, histórico de um job)
3. Simular uma condição de corrida manualmente (duas transações concorrentes) e observar o comportamento em cada nível de isolamento

---

## Módulo 2 — Docker e Containerização (conhecimento zero)

**Objetivo:** sair do zero e entender containerização do jeito que é usada em ambientes reais,
containerizando o próprio Postgres e, mais adiante, a aplicação.

- Conceitos: o que é uma imagem, o que é um container, diferença pra máquina virtual, por que isso resolve "na minha máquina funciona"
- `Dockerfile`: instruções básicas, build de uma imagem, multi-stage build (build da aplicação Java em um estágio, imagem final enxuta em outro)
- `docker-compose`: orquestrar múltiplos serviços (aplicação + PostgreSQL) juntos, variáveis de ambiente, volumes (persistência de dados entre restarts), redes internas entre containers
- Boas práticas de produção: `.dockerignore`, imagens slim/alpine, não rodar processo como root, healthchecks, tags de versão (nunca `latest` em produção)
- Dev vs prod: compose para ambiente local, considerações que mudam num deploy real

**Exercícios:**
1. Escrever um `Dockerfile` multi-stage para a aplicação Spring Boot e buildar a imagem manualmente
2. Criar um `docker-compose.yml` que sobe PostgreSQL com volume persistente e variáveis de ambiente
3. Adicionar a aplicação ao compose, com rede compartilhada com o banco, e healthcheck no Postgres para a aplicação só subir quando o banco estiver pronto
4. Rodar o projeto inteiro do zero com `docker compose up`, sem ter Java ou Postgres instalados na máquina

> Esse módulo é pré-requisito prático pros seguintes: a partir daqui, PostgreSQL e a aplicação sobem via `docker compose up` durante todo o resto da trilha.

---

## Módulo 3 — Spring Boot com boas práticas

**Objetivo:** estrutura profissional, não só "fazer funcionar".

- Separação de camadas (controller / service / repository) e por que isso importa
- Injeção de dependência: constructor injection vs. field injection, e por quê
- Configuração via `application.yml`, profiles (dev/prod)
- Tratamento de erros centralizado (`@ControllerAdvice`)
- Testes: JUnit 5, Mockito, e testes de integração com Testcontainers (Postgres real em container — aqui o Módulo 2 se conecta direto)

**Exercícios:**
1. Criar o esqueleto do projeto com as camadas bem separadas
2. Escrever testes de integração que sobem um Postgres real via Testcontainers
3. Refatorar um endpoint mal feito (proposital) aplicando tratamento de erro adequado

---

## Módulo 4 — Agendamento e execução assíncrona

**Objetivo:** dominar as ferramentas de concorrência do Spring e seus riscos.

- `@Scheduled`: cron expressions, fixedRate vs fixedDelay
- `@Async`: configuração de thread pools, `CompletableFuture`
- Armadilhas comuns: `@Async` não funciona chamando de dentro da mesma classe, pool esgotado, exceções engolidas

**Exercícios:**
1. Implementar o scheduler que varre jobs pendentes
2. Configurar um thread pool dedicado e demonstrar o que acontece quando ele esgota
3. Provocar e depois corrigir um bug clássico de `@Async` (self-invocation)

---

## Módulo 5 — Confiabilidade: retry, idempotência, outbox pattern

**Objetivo:** o que separa um sistema de brinquedo de um sistema de produção.

- Retry com backoff (Resilience4j ou implementação manual)
- Idempotência: como garantir que reprocessar um job não duplica efeito
- Outbox pattern: por que gravar o "evento a ser publicado" na mesma transação do dado de negócio

**Exercícios:**
1. Implementar retry com backoff exponencial e limite de tentativas
2. Adicionar uma chave de idempotência e testar reprocessamento forçado
3. Implementar outbox pattern simplificado (tabela de eventos + processo que os publica)

---

## Módulo 6 — Concorrência no banco

**Objetivo:** SQL aplicado ao problema real do projeto — evitar que dois workers peguem o mesmo job.

- `SELECT ... FOR UPDATE SKIP LOCKED` para fila de processamento
- Lock otimista (coluna de versão) vs. lock pessimista
- Deadlocks: como acontecem e como diagnosticar

**Exercícios:**
1. Implementar a fila de jobs usando `FOR UPDATE SKIP LOCKED`
2. Simular dois workers concorrentes e confirmar que nenhum job é processado duas vezes
3. Provocar um deadlock proposital e diagnosticar com `pg_locks`

---

## Módulo 7 — Observabilidade e API profissional

**Objetivo:** o que faz um serviço parecer produzido por alguém sênior.

- Logs estruturados (contexto de request, correlação de job)
- Métricas básicas (contadores de sucesso/falha, tempo de processamento)
- Validação de entrada, versionamento de API, respostas de erro consistentes

**Exercícios:**
1. Adicionar logging estruturado com ID de correlação por job
2. Expor métricas básicas (via Actuator, por exemplo)
3. Revisar todos os endpoints aplicando validação e formato de erro padronizado

---

## Módulo 8 — SQL avançado

**Objetivo:** ir além do CRUD — usar o banco do próprio projeto como laboratório.

- Window functions (para relatórios: jobs por hora, taxa de falha por tipo)
- `EXPLAIN ANALYZE`: ler plano de execução e identificar problema
- Otimização de índice baseada em queries reais do projeto

**Exercícios:**
1. Escrever uma query de relatório usando window functions
2. Pegar a query mais lenta do sistema, rodar `EXPLAIN ANALYZE`, e otimizar
3. Medir o impacto de um índice antes/depois

---

## Módulo 9 — Front-end básico (visualização)

**Objetivo:** fechar o ciclo, sem aprofundar.

- Tela simples em React + TS para listar jobs, status e permitir reagendar/cancelar
- Consumo da API, tratamento básico de loading/erro

**Exercícios:**
1. Tela de listagem com filtro por status
2. Ação de cancelar/reagendar um job

---

## Eixo transversal — Uso eficiente do Claude Code

Aplicar ao longo de todos os módulos, não como módulo isolado:

- **Quando delegar vs. quando pedir explicação:** para módulos de fundamentos (1, 2, 6, 8), pedir para o Claude Code explicar o raciocínio antes de aceitar o código, em vez de só aceitar o resultado
- **Prompts econômicos:** ser específico no pedido para evitar iterações desnecessárias que consomem contexto
- **Gerenciamento de contexto:** usar um `CLAUDE.md` no projeto com o estado atual (módulo em andamento, decisões já tomadas) como âncora contra perda de contexto em compactações
- **Checkpoints:** rodar `/compact` proativamente entre módulos, não esperar o limite
- **Revisão crítica:** para cada entrega do agente nos módulos de back-end/SQL/Docker, revisar linha a linha justificando o "porquê", não só validar que compila/roda/sobe

---

## Como usar esta trilha

Sugestão de fluxo por módulo: (1) ler o objetivo e conceitos, (2) tentar o exercício sozinho ou com o Claude Code em modo "explicativo", (3) implementar no projeto, (4) revisar o código gerado criticamente antes de avançar.
