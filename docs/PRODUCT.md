# Product

## Purpose

TestForge AI helps a QA practitioner turn a structured user story into
review-ready manual test coverage with durable traceability and human approval.
It reduces first-draft effort without treating generated content as trusted or
complete.

## Stage 1 users and workflow

The current product serves an authenticated individual workspace owner acting as
a QA analyst or test lead:

1. Register or sign in.
2. Create and manage an owned project.
3. Create a prioritized User Story with business requirements, assumptions,
   source reference, and one or more keyed acceptance criteria.
4. Request manual-test generation with an idempotency key.
5. Review detected ambiguities and structured generated cases.
6. Edit an active-set case, inspect structured history, and approve, reject,
   request changes, or explicitly reopen a terminal decision.
7. Inspect criterion coverage and traceability.
8. Export approved cases as CSV, JSON, or Markdown and inspect audit events.

The SPA supports this workflow against the Java API. The API owns authorization,
validation, persistence, generation boundaries, review state, traceability,
export safety, and audit evidence. Exact endpoints and response contracts remain
canonical in [API.md](API.md).

## Generation promise

Manual generation derives coverage from the supplied actor, behavior, business
rules, assumptions, boundaries, authorization and failure behavior, and
acceptance criteria. The `manual-test-v2` prompt internally decomposes each
criterion into testable obligations and produces the smallest coherent suite:
a direct case maps to one or more supplied `AC-#` keys when one realistic
workflow independently proves them together, while supporting exploratory cases
may cover relevant boundaries, negative paths, security, recovery, concurrency,
or accessibility. It does not expose hidden reasoning or an obligation inventory.

Each successful generation run is an immutable generation set. The most recent
successful completion (completion time then UUID) is active; failed attempts do
not supersede it. Default cases, coverage, traceability, review, and export use
only that set, while explicitly selected historical sets remain read-only. Each
new run preserves the exact source User Story version and ordered criterion
snapshots; pre-V6 reconstructed evidence is visibly labeled rather than claimed
as exact. Primary coverage is DIRECT-only, with partial/supporting evidence
reported separately.
Pending, failed, and validation-rejected attempts remain visible in bounded
generation history even when no test-case set exists. Only completed generation
uses success notice semantics; the other outcomes remain informational,
warning, or error states.

Provider output is not accepted merely because it is valid JSON. It must satisfy
exact application-owned JSON types, the JSON schema, and semantic validator:
no provider-authored transport fields or scalar/enum coercion, supported enums,
unique case titles, bounded and contiguous steps, concrete text, valid criterion
mappings, stripped case-insensitive unique synthetic test-data names, resolvable
step data references, and no prohibited executable content. Incomplete, empty,
malformed, or semantically invalid output receives one
controlled regeneration attempt and otherwise becomes a safe run failure
without partial persistence. Provider refusal, configuration/authentication,
and transport failure do not retry. No database transaction spans provider
latency.

## Product quality principles

- Preserve the source User Story, every generation set, and review history.
- Make every action and expected result independently observable.
- Expose ambiguities instead of inventing decision-critical policy.
- Keep generated cases editable and require a human decision before export.
- Keep work items owner-scoped and return inaccessible resources as not found.
- Use synthetic data and minimize content sent to an external provider.
- Make quality measurable through permanent, sanitized evaluation fixtures.

## Current boundaries

Stage 1 does not provide organization sharing, enterprise SSO, administrative
UI, asynchronous generation, or a production cloud landing zone. It does not
generate or execute Playwright, Copado Robotic Testing, or other automation.
`AutomationDraftGenerator` is only a Stage 2 extension point; automation
evaluation fixtures are roadmap-only and non-blocking.

See [ARCHITECTURE.md](ARCHITECTURE.md), [the MVP specification](product-specs/mvp-1-test-generation.md),
and [the security policy](../SECURITY.md) for implementation and trust details.
## Workspace foundation and product direction

Each registered user receives a personal workspace and OWNER membership. New
projects carry both their existing `ownerId` contract and a `workspaceId`.
Callers can list only workspaces represented by their memberships. This is an
identity and migration foundation: membership does not grant access to another
user's projects, requirements, test cases, generation evidence, exports, or
audit records. Those resources remain owner-isolated and inaccessible IDs
continue to return `404`.

Workspace invitations, role-based collaboration, generalized artifact snapshots,
restoration/retention, queued generation, and automation drafts are future
roadmap capabilities. See the [gap analysis](assessment/gap-analysis.md) and
[target domain model](architecture/domain-model.md). Planning uses both
TestForce and TestForge names; the implemented product remains TestForge until
a separate naming decision is accepted.
