# ADR 0001: Use a modular monolith for Stage 1

- Status: Accepted
- Date: 2026-07-30

## Context

TestForge AI needs clear boundaries for authentication, requirements, generation, review, traceability, export, and audit. The portfolio must demonstrate disciplined architecture while remaining straightforward to run locally and deploy as an early product.

## Decision

Build one Spring Boot deployable and organize it into domain-oriented modules under `com.testforge`. Modules expose explicit application and domain boundaries; controllers stay thin, persistence types do not cross the REST boundary, and cross-domain work is coordinated through application services.

## Consequences

The team gets one transactional boundary, one database migration stream, one security perimeter, and a simple Docker topology. Domain packages still make ownership and coupling visible and allow focused tests. Independent scaling and deployability are deferred. If operational evidence later justifies extraction, the provider adapter or another well-bounded module can move behind a network boundary without redesigning the core domain.
