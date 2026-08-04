# Architecture decision records

Decision records capture durable choices and their tradeoffs. Do not rewrite an
accepted record to hide a later change; add a superseding ADR and link both.

| ADR | Decision | Status |
| --- | --- | --- |
| [0001](0001-modular-monolith.md) | Modular monolith | Accepted |
| [0002](0002-browser-authentication.md) | Browser authentication | Accepted |
| [0003](0003-provider-neutral-generation.md) | Provider-neutral generation | Accepted |
| [0004](0004-human-approval.md) | Human approval and approved-only export | Accepted |
| [0005](0005-java-spring-backend.md) | Java and Spring backend | Accepted |
| [0006](0006-react-typescript-frontend.md) | React and TypeScript frontend | Accepted |
| [0007](0007-structured-ai-output.md) | Structured AI output | Accepted |
| [0008](0008-postgresql-flyway.md) | PostgreSQL and Flyway | Accepted |
| [0009](0009-workspace-tenancy-expand-contract.md) | Workspace tenancy through expand/backfill/contract | Accepted for expand/backfill |

Create a zero-padded sequential file for a new cross-cutting decision. Include
context, decision, consequences, alternatives, and any superseded record.
