# Ledgerly Mini-RDBMS & Ledgerly Domain Demo

Comprehensive mini-RDBMS (Java/Spring Boot) with file-based persistence (WAL), REST API, REPL, and a minimal React UI. Includes a Ledgerly-flavored domain overlay (merchants, transactions, outcomes, expiration) to showcase a transaction lifecycle on top of the custom engine.

## Table of Contents
- Overview
- Features
- Architecture (with diagrams)
- Data Model & State Machine
- Persistence & Seeding
- Running the Stack
  - Docker Compose
  - Local backend
  - Local frontend
  - REPL
- API Reference
  - Core RDBMS APIs
  - Ledgerly Domain APIs
- Testing & Troubleshooting
- Notes for Reviewers

## Overview
This project demonstrates a self-contained mini-RDBMS built from first principles (no external DB), plus a Ledgerly-inspired domain: onboarding merchants, recording transactions, asserting outcomes, and handling expirations. It targets the challenge requirements while showing realistic transaction lifecycle flows.

## Features
- Core engine: schema management, PK/unique constraints, types (INT, STRING, TIMESTAMP), CRUD, filtered selects, simple inner join, in-memory indexes.
- Persistence: append-only WAL (JSONL), reload on startup; configurable data directory.
- Domain overlay: merchants, transactions (PENDING/SUCCESS/FAILED/EXPIRED), outcomes, manual expiration sweep; idempotent transaction creation, validated outcome assertion.
- Interfaces: REST API, REPL, and a minimal React UI.
- Dockerized: one-command bring-up with persisted volume; frontend auto-wired to backend.

## Architecture
### System (high level)
```mermaid
flowchart LR
  user[User] --> ui[ReactUI]
  ui --> api[SpringBootAPI]
  api --> domain[LedgerlyDomainService]
  api --> engine[MiniRDBMS Core]
  engine --> storage[WAL File Storage]
  domain --> engine
```

### Ledgerly domain flow (transaction lifecycle)
```mermaid
flowchart TD
  create[Create Transaction\nstate=PENDING] --> outcome[Assert Outcome?]
  outcome -->|SUCCESS| success[State=SUCCESS]
  outcome -->|FAILED| failed[State=FAILED]
  outcome -->|No outcome before expires_at| expired[State=EXPIRED]
```

## Data Model & State Machine
- Core demo tables (generic): `customers`, `orders` (seeded for basic CRUD/join demo).
- Ledgerly tables:
  - `merchants(id, name, status, created_at)`
  - `transactions(id, merchant_id, amount, currency, state, created_at, expires_at, metadata)`
  - `outcomes(tx_id, status, external_reference, reported_at, metadata)`
- States: PENDING → (SUCCESS | FAILED | EXPIRED). Outcome assertion allowed only from PENDING.

## Persistence & Seeding
- Persistence: append-only WAL at `data/ledgerly-wal.jsonl` (or mounted volume). Reloads on boot by replaying WAL.
- Seeding:
  - Generic seed (customers/orders).
  - Ledgerly seed (merchant + transactions + outcome) controlled by `ledgerly.seed.domain-enabled` (default true).
- Config: `ledgerly.data-dir` (default `data`), `ledgerly.seed.domain-enabled` (default true).

## Running the Stack

### Docker Compose (primary)
```sh
docker-compose up --build
```
- Backend: http://localhost:8080
- Frontend: http://localhost:4173
- Data persisted in volume `ledgerly-data`.
- Frontend build arg points to backend (`http://backend:8080`).

### REPL (via compose, recommended)
```sh
docker-compose run backend java -jar /app/app.jar --spring.main.web-application-type=none --ledgerly.repl.enabled=true
```
Interaction (prompt is `>`):
- Core commands:
  - `help` — list commands
  - `tables` — list table names
  - `describe <table>` — show schema
  - `insert <table> <json>` — insert a row (e.g., `insert customers {"id":4,"name":"Diana","created_at":"2024-02-01T00:00:00Z"}`)
  - `select <table>` — select all rows (prints JSON array)
  - `delete <table> col=val` — delete matching rows (e.g., `delete customers id=4`)
  - `quit` — exit
- Domain commands:
  - `merchant:create <json>` — create merchant (e.g., `merchant:create {"id":"m3","name":"Corner Shop","status":"ACTIVE"}`)
  - `tx:create <json>` — create transaction (e.g., `tx:create {"id":"t300","merchant_id":"m3","amount":1200,"currency":"USD"}`)
  - `tx:get <id>` — fetch transaction
  - `tx:list` — list transactions
  - `tx:outcome <id> <json>` — assert outcome (e.g., `tx:outcome t300 {"status":"SUCCESS","external_reference":"proc-22"}`)
  - `tx:expire` — expire pending past `expires_at`
- Notes: errors (e.g., constraint violations, missing merchant) are printed as `Error: <message>`; timestamps must be ISO-8601; amounts are numeric (cents).

### Local backend (without Docker)
```sh
cd backend
mvn spring-boot:run
```
Env: `LEDGERLY_DATA_DIR` to set data dir; `LEDGERLY_SEED_DOMAIN_ENABLED` to toggle domain seed.

### Local frontend (without Docker)
```sh
cd frontend
npm install
npm run dev -- --host
```
Env: `VITE_API_URL` (defaults to http://localhost:8080).

## API Reference

### Core RDBMS APIs
- List tables:
```sh
curl http://localhost:8080/tables
```
- Query rows:
```sh
curl -X POST http://localhost:8080/tables/customers/query -H "Content-Type: application/json" -d '{}'
```
- Insert row:
```sh
curl -X POST http://localhost:8080/tables/customers/rows -H "Content-Type: application/json" \
  -d '{"values":{"id":3,"name":"Carol","created_at":"2024-01-05T00:00:00Z"}}'
```
- Join:
```sh
curl -X POST http://localhost:8080/tables/join -H "Content-Type: application/json" \
  -d '{"leftTable":"customers","rightTable":"orders","leftColumn":"id","rightColumn":"customer_id"}'
```

### Ledgerly Domain APIs
- Create merchant:
```sh
curl -X POST http://localhost:8080/ledger/merchants -H "Content-Type: application/json" \
  -d '{"id":"m2","name":"Shop","status":"ACTIVE"}'
```
- Create transaction:
```sh
curl -X POST http://localhost:8080/ledger/transactions -H "Content-Type: application/json" \
  -d '{"id":"t200","merchantId":"m2","amount":999,"currency":"USD","expiresAt":"2026-01-15T00:00:00Z"}'
```
- List transactions:
```sh
curl "http://localhost:8080/ledger/transactions?merchant_id=m2&state=PENDING"
```
- Assert outcome:
```sh
curl -X POST http://localhost:8080/ledger/transactions/t200/outcome -H "Content-Type: application/json" \
  -d '{"status":"SUCCESS","externalReference":"proc-1"}'
```
- Expire pending:
```sh
curl -X POST http://localhost:8080/ledger/transactions/expire
```

## Testing & Troubleshooting
- Smoke test (core): list tables, select from seeded customers/orders, insert and re-select, run join.
- Smoke test (domain): create merchant, create transaction, list/filter, assert outcome, expire pending.
- Data persistence: ensure volume/directory is writable; WAL stored under `data/`.
- REPL: use for quick interactive checks without HTTP.
- If frontend can’t reach backend: confirm `VITE_API_URL` (local) or build arg (compose).
