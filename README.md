# Ledgerly Mini-RDBMS

Java/Spring Boot mini-RDBMS with REST API, REPL, and minimal React UI.

## Run (Docker)
```sh
docker-compose up --build
```
- Backend: http://localhost:8080
- Frontend: http://localhost:4173
- Data persisted in named volume `ledgerly-data`.
- Domain seeding (merchants/transactions/outcomes) enabled by default via `ledgerly.seed.domain-enabled=true`.

### REPL
```sh
docker-compose run backend java -jar /app/app.jar --spring.main.web-application-type=none --ledgerly.repl.enabled=true
```
Commands: `help`, `tables`, `describe <table>`, `insert <table> {"col":"val"}`, `select <table>`, `delete <table> col=val`, `quit`.
Domain commands: `merchant:create {json}`, `tx:create {json}`, `tx:get <id>`, `tx:list`, `tx:outcome <id> {json}`, `tx:expire`.

## API (examples)
List tables:
```sh
curl http://localhost:8080/tables
```

Query rows:
```sh
curl -X POST http://localhost:8080/tables/customers/query -H "Content-Type: application/json" -d '{}'
```

Insert row:
```sh
curl -X POST http://localhost:8080/tables/customers/rows -H "Content-Type: application/json" \
  -d '{"values":{"id":3,"name":"Carol","created_at":"2024-01-05T00:00:00Z"}}'
```

Join:
```sh
curl -X POST http://localhost:8080/tables/join -H "Content-Type: application/json" \
  -d '{"leftTable":"customers","rightTable":"orders","leftColumn":"id","rightColumn":"customer_id"}'
```

## Ledgerly Domain APIs (merchants, transactions, outcomes)
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

## Dev (backend)
```sh
cd backend
mvn spring-boot:run
```
Data dir default: `backend/data` (configurable via `LEDGERLY_DATA_DIR`).

## Dev (frontend)
```sh
cd frontend
npm install
npm run dev -- --host
```
Set `VITE_API_URL` to point to the backend (defaults to http://localhost:8080).***
