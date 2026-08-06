# MVP 1: Manual test generation

## Status and scope

Implemented as TestForge AI Stage 1. This specification covers creation of an
owned User Story, validated manual-test generation, review, traceability, and
approved export. Automation generation and execution are explicitly out of
scope.

## User Story input

An authenticated owner supplies:

- a title and user story;
- optional business requirements, assumptions, and source reference;
- priority `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW`, defaulting to `MEDIUM`;
- between 1 and 50 acceptance criteria, persisted with stable `AC-#` keys.

Request DTO size limits and exact API shapes are canonical in the backend source
and [API.md](../API.md). Submitted text is untrusted data, not model instruction.

## Generation contract

Generation requires a nonblank idempotency key. The application minimizes the
provider request to requirement fields and keyed acceptance criteria. The
versioned `manual-test-v2` developer prompt requires only application-owned
structured JSON. New runs record `manual-test-result-v1`,
`manual-test-schema-v2`, `manual-test-validator-v2`, and provider-adapter
evidence with exact source criterion snapshots before provider work.
The current OpenAI adapter evidence is `openai-responses-v3`; it rejects
provider-authored transport metadata and JSON scalar/enum coercion without
changing the prompt, result, schema, or semantic-validator contracts.

Each generated test case contains a distinct title and objective, category,
priority, risk, automation-candidate hint, coverage intent, preconditions,
synthetic test data, ordered action/result steps, final outcome, criterion keys,
and rationale. Before drafting, the prompt internally decomposes every supplied
criterion into atomic testable obligations, then requires the smallest coherent
suite that realizes them without emitting hidden reasoning or an obligation
inventory. A direct case maps to one or more supplied criteria when one
realistic workflow independently proves them together; it must not split shared
workflow/data merely to increase case count. Supporting exploratory coverage
maps only to supplied keys when a mapping is claimed.

The semantic validator rejects empty or oversized output, unsupported enums,
duplicate titles, missing or non-contiguous steps, blank or vague text,
executable content, unknown criterion keys, incomplete synthetic test data,
normalized duplicate data names, and dangling step data references.
The service permits one controlled retry only for incomplete, empty, malformed,
or semantically invalid structured output. It invokes the provider outside a
database transaction, then persists only the complete validated output in one
short finalization transaction and records safe run metadata/failure state.

## Acceptance criteria

1. An authenticated owner can create a requirement with at least one acceptance
   criterion and retrieve it without exposing another owner's data.
2. Reusing the same idempotency key for the same owner and User Story returns
   the reconciled original generation run rather than duplicating evidence or
   invoking the provider again.
3. Every direct generated case maps only to acceptance-criterion keys supplied
   with the source requirement.
4. Generated steps start at one, remain contiguous, and pair one concrete tester
   action with one independently observable expected result.
5. Unsafe, vague, duplicate, malformed, unmapped, or unsupported provider output
   is rejected before persistence; one controlled regeneration is allowed.
6. Provider and validation failures return safe run information without raw
   provider bodies, secrets, or partial cases.
7. A reviewer can edit an active case with optimistic concurrency and approve,
   reject, request changes, or explicitly reopen a terminal decision while
   retaining revision and audit evidence.
8. Traceability distinguishes raw generated coverage from approved coverage.
9. Export is unavailable until at least one case is approved and protects CSV
   and Markdown consumers from content injection.
10. Default automated tests and evaluations never require a live provider.
11. The latest successful run is the active generation set; failures do not
    supersede it, historical sets are read-only, and reviewed/revised evidence
    requires confirmation before regeneration.
12. Every source criterion has at least one DIRECT mapping. Supporting mappings
    are reported separately and cannot satisfy direct or approved-direct
    coverage.
13. Criterion edits/deletes after generation do not alter that run's
    traceability; the response exposes source version and exact/reconstructed
    provenance.
14. Every canonical history is bounded and paged; failed evidence requests are
    retryable errors rather than false zero/empty states.
15. The selected set, workflow tab, filters, sort, and pages survive navigation
    through URL state, and every structured case field is visible before review.
16. A criterion renamed while generation is in flight retains its legacy link
    by captured source identity; deletion safely fails the run without cases.
17. Generation notices expose success only for `COMPLETED`; pending, failed,
    and validation-rejected outcomes have distinct semantic severity.

## Quality bar

Generated output should cover every acceptance criterion directly, add only
source-supported exploratory coverage, expose missing decision-critical details
as ambiguities, avoid invented business policy, use synthetic test data, and be
reviewable without hidden reasoning. Permanent fixtures in `evals/` are scored
with the 100-point rubric; 80 with no hard failure is the passing threshold.

## Out of scope

- Organization sharing, SSO, MFA, and administration.
- Asynchronous or bulk generation.
- Automatic approval or export of unreviewed cases.
- Playwright, Copado, or other automation generation or execution.
- A live-provider CI quality gate.
