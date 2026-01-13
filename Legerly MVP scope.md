# Ledgerly — MVP Scope, Responsibilities, and Transaction Model

## 1. Overview

Ledgerly is a **transaction acceptance and fulfillment‑intent ledger** designed to act as the authoritative system of record for transactional requests in a payment‑adjacent environment.

The system provides durable transaction recording, strict validation, deterministic state management, and a controlled interface for external systems to assert fulfillment outcomes. Ledgerly is intentionally positioned at the **front of the transaction lifecycle**, ensuring correctness, traceability, and integrity before any downstream execution occurs.

---

## 2. System Responsibilities

Ledgerly is responsible for the following system‑level functions:

- Accepting transaction creation requests from external clients
- Validating transaction structure, constraints, and invariants
- Enforcing idempotent transaction acceptance
- Persisting transaction intent durably in a relational data store
- Managing authoritative transaction state transitions
- Enforcing time‑based state rules and service‑level expectations
- Exposing query interfaces for transaction inspection
- Accepting externally asserted transaction outcomes via a controlled interface

Ledgerly operates as a **system of record** for transaction intent and outcome state.

---

## 3. Transaction Lifecycle

Transactions in Ledgerly follow a well‑defined lifecycle modeled as a state machine. Each state represents a clear semantic phase in the transaction’s progression.

### 3.1 Lifecycle States

| State     | Description |
|----------|------------|
| PENDING  | Transaction has been validated and durably accepted; fulfillment outcome is not yet known |
| SUCCESS  | A fulfillment outcome has been asserted indicating successful execution |
| FAILED   | A fulfillment outcome has been asserted indicating execution failure |
| EXPIRED  | No fulfillment outcome was asserted within the defined SLA window |

---

### 3.2 Lifecycle Flow

1. **Transaction Creation**
   - A client submits a transaction request to Ledgerly
   - Ledgerly validates the request and enforces idempotency

2. **Acceptance**
   - Upon successful validation, the transaction is persisted
   - The transaction enters the `PENDING` state
   - At this point, the transaction is considered successfully accepted by the system

3. **Outcome Assertion**
   - An external system reports the fulfillment result
   - Ledgerly validates and records the outcome
   - The transaction transitions to `SUCCESS` or `FAILED`

4. **Expiration Handling**
   - If no outcome is asserted within the SLA window
   - The transaction transitions to `EXPIRED`

Ledgerly does not infer or execute fulfillment actions; it records state transitions based on validated input.

---

## 4. Acceptance Semantics

A transaction is considered **successfully processed by Ledgerly** when:

- All validation rules pass
- Idempotency constraints are satisfied
- The transaction intent is durably persisted
- The transaction is visible for querying and downstream consumption

This acceptance represents Ledgerly’s terminal responsibility for the creation phase of the transaction.

---

## 5. Outcome Assertion Interface

Ledgerly exposes a secure interface for external systems to assert transaction outcomes.

### 5.1 Purpose

- Enable downstream fulfillment systems to report execution results
- Decouple transaction execution from transaction recording
- Preserve a single authoritative source of truth

### 5.2 Interface Characteristics

- Inbound, request‑driven
- Authenticated and authorized
- Idempotent
- State‑transition validated

### 5.3 Example Endpoint

```
POST /transactions/{transaction_id}/outcome
```

### 5.4 Example Payload

```json
{
  "status": "SUCCESS",
  "external_reference": "processor_ref_123",
  "reported_at": "2026-01-15T10:31:00Z",
  "metadata": {
    "source": "fulfillment_service"
  }
}
```

Ledgerly validates the request and applies the state transition atomically.

---

## 6. Time‑Based State Enforcement

Ledgerly enforces logical service‑level constraints on transaction progression.

- Transactions are expected to transition out of `PENDING` within a defined time window
- Transactions exceeding this window transition to `EXPIRED`

Expiration indicates absence of a reported fulfillment outcome within the expected timeframe.

---

## 7. Client Interfaces

Ledgerly exposes APIs for:

- Transaction creation
- Transaction status retrieval
- Transaction history and inspection

These interfaces are intended for:
- Merchant backends
- POS systems
- Booking and reservation systems
- Internal platform services

---

## 8. Data Model Principles

Ledgerly’s data model enforces:

- Strong relational integrity
- Primary and unique key constraints
- Indexed access paths for transactional queries
- Immutable transaction intent records
- Append‑only state transition history

---

## 9. Scope Summary

Ledgerly provides a complete, production‑aligned solution for transaction acceptance and state management. It establishes clear ownership of transaction correctness, durability, and lifecycle visibility while remaining decoupled from execution concerns.

This scope ensures the system remains focused, testable, and extensible, while accurately reflecting real‑world transactional architectures.

