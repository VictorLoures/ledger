# Ledger — Front-end (Módulo 9)

Tela básica de visualização — React + TypeScript + Vite. Não é o foco do
estudo (ver `CLAUDE.md`/`trilha-estudo-backend.md` na raiz do projeto).

## Rodando localmente

Precisa do back-end rodando via `docker compose up` na raiz do projeto
(porta `8080`) — a URL da API está fixa em `src/api.ts`.

```bash
npm install
npm run dev
```

Abre em `http://localhost:5173`. CORS pra essa origem já está liberado no
back-end (`WebConfig.java`).

## O que tem

- Listagem de jobs com filtro por status (client-side).
- Criar job (formulário simples).
- Cancelar / reagendar um job (regras de transição de estado espelham o
  back-end — `Job.cancel()` / `Job.reschedule()` em `domain/Job.java`).
- Tratamento básico de loading/erro, sem biblioteca externa de estado ou
  requisição (só `fetch` + `useState`/`useEffect`).
