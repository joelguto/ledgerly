# Pesapal Junior Dev Challenge 2026 — Implementation Focus & Requirements

## 1. Challenge Objective

The goal of the Pesapal Junior Developer Challenge is to assess a candidate’s ability to **design, reason about, and implement a core system from first principles**.

The challenge intentionally avoids framework-heavy or domain-specific shortcuts and instead focuses on:
- Fundamental system design
- Data modeling
- Correctness and consistency
- Clear thinking and documentation

The expected output is a **working, inspectable system** that demonstrates technical depth and intentional design choices.

---

## 2. Core System to Be Implemented

Candidates are required to design and implement a **simple relational database management system (RDBMS)** with the following characteristics:

- User-defined tables
- A small but coherent set of column data types
- Persistent storage
- A structured query interface (SQL or SQL-like)
- An interactive REPL

The system must be functional, demonstrable, and understandable from its source code and documentation.

---

## 3. Mandatory Functional Areas (Primary Focus)

These areas are the **core of the challenge** and should receive the majority of implementation effort.

### 3.1 Table Definition & Schema Management

The system should support:
- Creating tables with named columns
- Declaring column data types (e.g. integer, string, timestamp)
- Defining primary keys
- Defining unique constraints

This demonstrates understanding of relational schema design and data integrity.

---

### 3.2 Data Manipulation (CRUD Operations)

The system must support:
- Inserting records
- Querying records
- Updating records
- Deleting records

Queries should allow:
- Filtering by conditions
- Selecting specific columns

Correct handling of data consistency and constraint enforcement is essential.

---

### 3.3 Query Processing & Joins

The system should demonstrate:
- Basic query parsing
- Execution planning at a simple level
- Support for joining tables on key relationships

This area is critical for showing understanding of relational data access patterns.

---

### 3.4 Indexing

The system should include:
- At least one basic indexing mechanism
- Indexed access paths for primary or unique keys

The goal is not performance optimization at scale, but demonstrating awareness of why and how indexes exist.

---

### 3.5 Persistence & Storage

The database must:
- Persist data beyond process lifetime
- Reload state on restart
- Maintain data integrity across restarts

The storage layer may be simple (e.g. file-backed), but must be explicit and reliable.

---

### 3.6 Interactive REPL

The system must provide an interactive mode that allows:
- Entering queries
- Viewing query results
- Observing errors and validation feedback

The REPL serves as the primary interface for demonstrating the database system.

---

## 4. Demonstration Application (Secondary Focus)

In addition to the database system, candidates must build a **trivial web application** that uses the RDBMS for data storage.

### Purpose of the Web App

The web application exists solely to:
- Demonstrate real usage of the database
- Exercise CRUD operations end-to-end
- Show integration with external application code

The application does not need to be visually complex or production-grade.

---

## 5. Engineering Qualities Being Evaluated

Pesapal explicitly evaluates:

- Ability to reason about system boundaries
- Correctness over feature count
- Clear data modeling
- Explicit tradeoffs and documented decisions
- Code readability and structure

The challenge is designed to reward **thoughtful, well-scoped solutions** rather than maximal implementations.

---

## 6. Use of Tools and AI

Candidates may use:
- Any programming language
- Any development tools
- AI assistance

However:
- All external help must be acknowledged
- Borrowed ideas or code must be credited

Transparency is valued more than originality.

---

## 7. What to Prioritize

Candidates should prioritize:

1. A working RDBMS core
2. Correct schema and data handling
3. Clear query semantics
4. Demonstrable joins and indexing
5. A usable REPL
6. Clear documentation explaining design choices

---

## 8. What Is Not Required

The challenge does **not** require:

- High-performance optimization
- Distributed systems
- Full SQL compliance
- Security hardening beyond basic correctness
- Production-scale deployment

Avoiding unnecessary complexity is encouraged.

---

## 9. Submission Expectations

A complete submission should include:

- Source code in a public repository
- Clear build and run instructions
- Documentation describing system design and scope
- A demonstration of the web application using the database

The repository should allow a reviewer to:
- Run the system locally
- Interact with the REPL
- Observe database functionality

---

## 10. Summary

This challenge is an evaluation of **fundamental engineering skill**, not framework familiarity.

A successful submission demonstrates:
- Strong grasp of relational data systems
- Clear and disciplined system design
- Correctness and clarity over breadth

Candidates are encouraged to implement a focused, well-reasoned system that clearly shows how they think as engineers.