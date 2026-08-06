# ADR 0011: Keep PostgreSQL as the only runtime database

- Status: Accepted
- Date: 2026-08-05
- Supersedes: the isolated H2 demo choice in [ADR 0008](0008-postgresql-flyway.md)

## Context

PostgreSQL and Flyway are the system of record, and Testcontainers already
exercise PostgreSQL-specific migration and locking behavior. Shipping H2 in the
application artifact solely for a convenient demo adds a second runtime dialect
that can conceal PostgreSQL behavior and increases production dependency
surface.

Fast H2-backed Spring and migration tests still provide useful local feedback.
Removing them before equally fast, non-skipping PostgreSQL replacements exist
would weaken default `mvn verify` coverage.

## Decision

PostgreSQL is the only application runtime database. H2 is test-scoped and must
not appear in the packaged Boot runtime. The seeded demo uses the normal Compose
PostgreSQL topology with `TESTFORGE_DEMO_SEED_ENABLED=true`; the H2 application
demo profile is retired.

Default Maven verification retains fast H2 tests, while pessimistic locking,
PostgreSQL constraints, and real migration behavior use Testcontainers when
Docker is available. H2 tests will be removed only after equivalent PostgreSQL
tests are fast, deterministic, and non-skipping.

## Consequences

- Production and seeded-demo behavior share one SQL dialect and migration path.
- The deployable artifact has a smaller database-driver attack surface.
- The local seeded demo requires Docker/PostgreSQL instead of an embedded file.
- Some test-only dual-dialect maintenance remains temporarily and is explicit.

## Alternatives

- A separate Maven profile was rejected because default verification could omit
  database coverage.
- Immediate H2 removal was rejected because Docker availability would make core
  local tests conditional.
- Serving the SPA through Spring is unrelated and remains rejected under ADR
  0006; Nginx stays the same-origin edge.
