# ADR 0005: Use Java 21 and Spring Boot for the API

- Status: Accepted
- Date: 2026-07-30

## Context

The product needs mature validation, security, transactional persistence, database migration, observability, and testing support in a portfolio-ready backend.

## Decision

Use Java 21 and Spring Boot 3 with Spring Web, Security, Data JPA, Validation, Actuator, Flyway, and Maven. Use records at immutable API/provider boundaries and explicit constructor injection and transactions.

## Consequences

The service benefits from a well-integrated ecosystem and strongly typed domain boundaries at the cost of a larger runtime than a minimal service. The build enforces Java 21 and Maven 3.9+.
