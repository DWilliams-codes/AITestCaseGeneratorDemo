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

The responsive workspace keeps generation-set selection, search, filters,
sorting, counts, and recovery controls visible when a server-side query returns
zero cases. It distinguishes an empty story from a filtered no-match and an
out-of-range shareable page. System, light, and dark appearance is a local
accessibility preference; it never changes identity, authorization, or data.

## Generation promise

Manual generation derives coverage from the supplied actor, behavior, business
rules, assumptions, boundaries, authorization and failure behavior, and
acceptance criteria. The `manual-test-v5` prompt internally decomposes each
criterion into testable obligations and produces the smallest coherent suite:
a direct case maps to one or more supplied `AC-#` keys when one realistic
workflow independently proves them together, while supporting exploratory cases
may cover relevant boundaries, negative paths, security, recovery, concurrency,
or accessibility. It requires a complete reproducible path: tester-performed
authentication and state preparation are actionable setup steps, while discrete
tester interactions and their observable evidence remain test steps. It rejects
criterion-label placeholders and does not expose hidden reasoning or an
obligation inventory. Cases use realistic enterprise roles, synthetic data,
permissions, statuses, approvals, and audit-relevant outcomes where applicable;
each remains independently executable without undocumented assumptions and does
not invent unsupported policy.

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

The proposed, not-yet-authorized Stage 2 first tests TestForge itself in an
isolated non-production environment. The current owner selects an approved test
case; the system captures it as an immutable run-bound snapshot and authorizes a
bounded semantic-browser click-through. It records action, observation,
assertion, and evidence without accepting page content as agent instruction.
This smallest pilot reuses owner-scoped authorization rather than depending on
future workspace-sharing or general snapshot work. The execution model is
configuration-selected. Terra is the first candidate for manual evaluation, and
no routing or escalation policy is decided until evaluation evidence exists.
`AutomationDraftGenerator` remains a separate, later non-executing extension
point; automation evaluation fixtures remain roadmap-only and non-blocking.

See [ARCHITECTURE.md](ARCHITECTURE.md), [the MVP specification](product-specs/mvp-1-test-generation.md),
and [the security policy](../SECURITY.md) for implementation and trust details.
The active [TF-007 execution plan](exec-plans/active/TF-007-stage-two-agentic-test-execution.md)
is the canonical Stage 2 proposal.
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
# Superseded-set deletion

Owners may permanently purge an explicitly confirmed, unreviewed and unedited
superseded completed generation set. The generated case graph is removed while
a hidden tombstone reserves its historical Set number for bounded audit evidence;
active, failed, pending, rejected, reviewed, and edited runs are never deletable.

## Salesforce simulation walkthrough (TF-015)

The public `/simulation` route is a separate, browser-local demonstration using
two maintained Salesforce-shaped fixtures: Account create/update and Case
creation/status/owner/priority. It makes no AI request, Salesforce connection or
external write. Source intake, bounded local JSON import, lightweight
clarification, reviewer edits, explicit approval and step evidence are usable.
Custom criteria remain a manual draft; only exact maintained fixture semantics
can run. Azure DevOps and Jira previews are explicitly Unconnected.

Clarification changes concrete fixture data and assertions. Reusable synthetic
environment variables and an unresolved credential-reference name are bound by
approval. Context changes rebuild the proposal and clearly clear reviewer edits
and approval. Every run starts from an isolated empty fixture; interrupted writes
are indeterminate and never retried. History holds the last 20 immutable runs in
memory, with self-contained JSON download; refresh clears it. This does not claim
live Salesforce coverage or durable multiuser execution.

In the saved manual-generation workspace, resolving an ambiguity now preserves
a prior source revision, advances source version, and supplies the ordered
question/answer to the next generation as untrusted data. Original assumptions
remain unchanged. See [the interview walkthrough](INTERVIEW_WALKTHROUGH.md).

## Main application fixture demonstration

An explicitly isolated local fixture mode demonstrates the real authenticated
story, ambiguity, generation, persistence and human review workflow. The login
page displays and fills server-confirmed public disposable credentials. A
persistent notice distinguishes maintained synthetic responses from live AI.
The six Case review drafts cover create, update/routing, persisted reopen,
blank Subject validation, and read-only create/update. Their exact source is
`frontend/e2e/stub/fixtures/case-review-story.json`. First generation asks the
owner to confirm routing; a supported saved answer changes priority/owner
actions and observations on regeneration. These are manual drafts awaiting
review, never automatically approved or executed against Salesforce.

The existing public `/simulation` remains a separate bounded demonstration of
typed execution/evidence controls. It does not execute saved workspace drafts.
Live AI requires a separately configured normal account/provider and explicit
authorization; live Salesforce, Jira/ADO sync and durable automated execution
remain planned.
