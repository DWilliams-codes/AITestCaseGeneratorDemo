# ADR 0007: Require application-owned structured AI output

- Status: Accepted
- Date: 2026-07-30

## Context

Valid JSON alone does not make probabilistic output safe, internally consistent, or traceable to requirements.

## Decision

Require a versioned strict JSON Schema at the provider boundary, then apply application semantic checks for enums, sizes, ordered steps, expected results, duplicate cases, accepted criterion keys, synthetic data, vague language, and executable content. Persist no output until it passes.

## Consequences

Provider output can be rejected even when the model considers it useful. This conservative boundary makes persisted cases deterministic enough for human review and future automation-draft translation.
