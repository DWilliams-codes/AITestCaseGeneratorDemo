# ADR 0004: Require human approval before export

- Status: Accepted
- Date: 2026-07-30

## Context

Generated test cases are draft evidence. Allowing unreviewed material into execution or reporting would obscure accountability and amplify model errors.

## Decision

Keep generated and edited cases in explicit workflow states. Record approve, reject, and request-changes decisions with actor, comments, and time. Preserve edit revisions. Export only approved cases while traceability reports both raw and approved coverage.

## Consequences

The workflow has a deliberate human checkpoint and auditable responsibility. Users cannot export immediately after generation; the UI must make review status and approved coverage visible.
