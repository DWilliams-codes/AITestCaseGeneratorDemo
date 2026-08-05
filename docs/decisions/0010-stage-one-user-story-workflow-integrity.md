# 0010 — User Story workflow integrity on existing Stage One storage

Status: Accepted

## Context

Stage One originally exposed an internal `Requirement` name and returned every
generated case for a requirement. Regeneration, review, revision history, and
free-form test-data references therefore lacked one deterministic active-set
boundary. Renaming storage or adding parallel generation/history tables would
duplicate identity, complicate rollback, and risk authorization drift.

## Decision

Treat the existing Requirement aggregate as the canonical User Story concept
while retaining the `requirements` table, UUIDs, work-item numbers, foreign
keys, owner predicates, and legacy `/requirements` adapters. V5 adds only the
nullable `requirements.priority` bridge column and deterministically backfills
`MEDIUM`; the application treats null as `MEDIUM`.

Successful `generation_runs` are immutable generation sets. A single resolver
orders successful runs by completion time and UUID, designates the latest as
active, and numbers sets oldest first. Default reads and every mutation target
the active set; explicitly selected historical sets are read-only. A failed run
never supersedes successful evidence, and regeneration requires confirmation
before superseding a set with revision or review evidence.

One shared policy normalizes test-data names and references for provider output
and edited cases. Test-case entities enforce edit, review, and reopen state
transitions with optimistic versions. Existing revision, review, and audit
tables remain the history sources. The browser clears all in-memory auth and
CSRF state through one epoch-based reset so older asynchronous work cannot
restore credentials.

## Consequences

- Canonical User Story terminology and routes can evolve without a disruptive
  physical rename or authorization rewrite.
- Historical generated evidence is preserved and consistently visible, but it
  cannot be edited, reviewed, or accidentally exported by default.
- Review and test-data violations fail before partial mutation and return stable
  API errors; clients must send expected versions for review/reopen actions.
- The compatibility surface is intentionally temporary technical debt and must
  remain a thin adapter over the same services and DTOs.
- Active-set resolution reads successful runs to compute stable numbering. A
  later indexed projection may optimize this only if it preserves this ordering
  contract.

## Alternatives considered

- Rename `requirements` and its foreign keys: rejected because it adds migration
  risk with no product capability.
- Add `user_stories` or `generation_sets` tables: rejected because each would
  duplicate an existing aggregate or run identity.
- Mark old cases superseded in place or delete them: rejected because it mutates
  or destroys review evidence.
- Let controllers or UI code decide transitions/references independently:
  rejected because those policies would drift across entry points.

## Superseded records

None. This record extends ADRs 0002, 0004, 0007, 0008, and 0009.
