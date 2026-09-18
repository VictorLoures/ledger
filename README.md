# Ledger

Plataforma de agendamento e notificações com processamento assíncrono confiável,
construída como projeto de estudo em Java + Spring + PostgreSQL + Docker (com uma
camada básica de visualização em React + TypeScript).

## Stack

- Java + Spring Boot
- PostgreSQL
- Docker + Docker Compose
- React + TypeScript (front básico)

## Estrutura de estudo

Este projeto é guiado por uma trilha de estudo em módulos — veja
[`trilha-estudo-backend.md`](trilha-estudo-backend.md).

O progresso e o contexto de trabalho com o Claude Code ficam em
[`CLAUDE.md`](CLAUDE.md), que deve ser atualizado ao final de cada módulo.

## Como rodar

Back-end + banco (aplica as migrations Flyway sozinho):

```bash
cp .env.example .env
docker compose up -d
```

API em `http://localhost:8080/api/v1/jobs`. Métricas/health em
`http://localhost:8080/actuator`.

Front-end (opcional, ver [`web/README.md`](web/README.md)):

```bash
cd web
npm install
npm run dev
```
