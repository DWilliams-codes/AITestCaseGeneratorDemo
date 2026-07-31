# ADR 0008: Use PostgreSQL with Flyway-owned migrations

- Status: Accepted
- Date: 2026-07-30

## Context

The workflow needs relational integrity, transactions, optimistic versions, auditable normalized data, and reproducible schema evolution.

## Decision

Use PostgreSQL as the production database and Flyway for every schema change. Configure Hibernate to validate the schema. Use H2 only for the isolated demo and fast API tests; use a Testcontainers PostgreSQL test for migrations and database-specific mappings when Docker is available.

## Consequences

Production and CI exercise the target database while the local demo remains easy to launch. Migration rollback is operational rather than automatic and requires backup/restore discipline.
