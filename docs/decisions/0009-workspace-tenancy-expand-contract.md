# ADR 0009: Introduce workspace tenancy through expand/backfill/contract

- Status: Accepted for the expand/backfill stage
- Date: 2026-08-03

## Context

Stage 1 stores `projects.owner_id` and uses owner-scoped repository predicates.
The roadmap needs collaboration roles without risking a flag-day schema and
authorization rewrite. Old and new binaries may overlap during deployment or
rollback, and existing users/projects need deterministic tenancy.

## Decision

Introduce `workspaces` and `workspace_memberships` in Flyway V4. Create one
personal workspace and OWNER membership per existing user, using the user UUID
for both new primary keys. Add nullable `projects.workspace_id`, backfill it
from `owner_id`, and dual-write both columns for new projects.

Registration provisions the personal workspace and OWNER membership in the same
transaction as user/session creation. `GET /api/v1/workspaces` lists only the
caller's memberships and reports that caller's role. TF-001 does not use
membership to authorize content: existing owner predicates and cross-owner
`404` responses remain unchanged.

Use expand/backfill/observe/contract releases. During rollback, deploy the old
binary and leave V4 data in place. Do not reverse Flyway or make workspace_id
non-null until old binaries are excluded and reconciliation is complete.

## Consequences

- Existing data receives stable, explainable tenancy with no identity mapping
  table or nondeterministic UUID generation.
- Mixed versions remain compatible, including old-binary null workspace writes.
- New APIs can expose workspace identity without prematurely enabling sharing.
- The schema temporarily has two ownership concepts and needs reconciliation.
- Shared access, role semantics, invitation lifecycle, and owner-column contract
  removal require later ADRs and independently reviewed security tests.

## Alternatives considered

- A flag-day replacement of `owner_id` was rejected because rollback would be
  unsafe and authorization changes too broad.
- A single global default workspace was rejected because it collapses tenant
  isolation.
- Random backfill IDs were rejected because they complicate reconciliation and
  mixed-version self-healing.
- Treating membership as immediate project access was rejected because roles,
  invitation lifecycle, and resource policy are not yet implemented.
