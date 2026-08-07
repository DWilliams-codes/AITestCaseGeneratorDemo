# Stage 2 TestForge website capabilities — Architect-ready user-story backlog

**Status:** ROADMAP — WEBSITE FUNCTIONALITY ONLY — NOT AUTHORIZED

## Relationship to TF-007

This is a website-capability derivative of [TF-007](../exec-plans/active/TF-007-stage-two-agentic-test-execution.md), not a replacement for it, an ExecPlan, or implementation authorization. Numbered stories describe only what a TestForge user can see or do. Required contracts, infrastructure, governance, and release work are retained below as prerequisites, not numbered capabilities.

## Stage 1 baseline and Test Step reuse

Stage 1 provides owner-scoped User Stories, validated manual-test generation, human review, immutable traceability/history, and approved export; see [MVP 1](mvp-1-test-generation.md). The current TestCase already has ordered action/expected-result **Test Steps**, including concise initial-generation steps. Stage 2 reuses an owned, current, approved TestCase and its existing generated/manual Test Steps as immutable run input. It does not regenerate steps, create a parallel step model, or alter Stage 1 review/export behavior.

## Website-only scope and milestone map

- **M9:** eligibility, environment selection, run confirmation/start, lifecycle/cancel.
- **M9a:** step and result review, authorized evidence, immutable history, safe accessible recovery.
- **M9b:** visible visual-fallback provenance and explicit safety-pause response.
- **M10:** readiness, non-executing neutral-draft review, and non-executing Copado-draft export.
- **M11:** release prerequisite only; it is not a numbered website capability.

## Architecture guardrails and prerequisite ledger

All future implementation preserves Java 21 Spring, React TypeScript,
PostgreSQL/Flyway, provider-neutral boundaries, and backend ownership of state
transitions and persistence. The authorization chain remains authenticated owner
→ owned project → current approved TestCase → eligible environment → run/evidence;
inaccessible resources return `404`. Browser workspace/tenant context and an
arbitrary URL are never authority.

Any data change is additive and forward-only: expand, deterministic backfill
where needed, dual-write, observe/reconcile, read/policy switch, then later
contract; the prior binary remains viable during rollback and no down migration
is allowed. A durable asynchronous worker uses fenced leases, bounded
infrastructure-only retry/recovery, cancellation, idempotency, and an execution
kill switch; UI requests never hold browser work open.

Execution-agent decisions are minimized, provider-neutral, and validated by
application-owned structural/semantic contracts before action or persistence.
Browser operations are allowlisted semantic actions only; base origin, allowed/
blocked origins, schemes, redirects, turns/actions/duration/screenshots/tokens/
cost/retries and SSRF policy are bounded. Rendered page text, DOM labels,
uploads, errors, screenshots, and third-party content are untrusted data and
cannot override policy. Policy, injection, secret, off-origin, and limit events
fail closed. Visual computer use is a later constrained fallback, never default.

Evidence is accessed by opaque reference only, owner-scoped, bounded, redacted,
and classified; it exposes no paths, secrets, prompts, or raw browser/provider
state. No credential profile or credential request input is in scope; session
establishment and secret handling require separate approval. Deterministic
sanitized fixtures and fake agent/browser doubles precede any separately
authorized provider pilot. `ai-generation-evals` owns future release-tuple
impact; Terra may be evaluated first but no model/routing decision is implied.

M10 artifacts are non-executing. M11 remains a release prerequisite: managed
private data, TLS, secret management, backup/restore, redacted telemetry,
append-only audit forwarding, SLOs/incident response, SBOMs/signed artifacts,
DPA/residency/retention/egress review, and load/failover/isolation/injection/
dependency/container/accessibility/disaster-recovery gates. TestForge execution
does not target production environments in this roadmap.

## Website story template

Every numbered story is **Status: ROADMAP — NOT AUTHORIZED** and includes:

- **Persona narrative / outcome:**
- **User-visible location / interaction:**
- **Dependencies / architectural prerequisites:**
- **Acceptance intent:** Given/When/Then
- **UI states:**
- **Authorization / privacy:**
- **Accessibility intent:**
- **Deterministic validation intent:**
- **Compatibility / rollback:**
- **Non-goals:**
- **Architect-ready questions:**

## M9 website capabilities

### S2-US-001 — M9: See whether an approved Test Case is eligible to run

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need to see whether a Test Case can run before I attempt it.
**User-visible location / interaction:** Test Case execution panel eligibility summary.
**Dependencies / architectural prerequisites:** Existing Stage 1 case/review and TF-007 authorization/environment policy.
**Acceptance intent:** Given a case, when viewed, then eligibility appears only for owned/current/approved cases; Given historical, superseded, unapproved, or unsupported cases, when viewed, then a safe reason appears without leakage.
**UI states:** Eligible, ineligible with reason, unavailable, loading.
**Authorization / privacy:** Owner-scoped `404`; no cross-owner reason disclosure.
**Accessibility intent:** Status is programmatically named and announced.
**Deterministic validation intent:** Owned/current/approved and all ineligible-state fixtures.
**Compatibility / rollback:** Hide panel behind flag; preserve Stage 1 case view.
**Non-goals:** Starting a run or revealing environment internals.
**Architect-ready questions:** Which safe eligibility reasons and supported-case rules are approved?

### S2-US-002 — M9: Select an eligible non-production TestForge environment

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need to select an allowed pilot environment.
**User-visible location / interaction:** Execution panel environment selector.
**Dependencies / architectural prerequisites:** S2-US-001 and server environment catalog.
**Acceptance intent:** Given eligible case, when selecting environment, then only authorized enabled pilot environments appear with non-production label/policy summary; Given an arbitrary URL or browser tenant value, when supplied, then it is not accepted as authority.
**UI states:** Selectable, unavailable, policy-limited, loading.
**Authorization / privacy:** Catalog is server-authorized and excludes inaccessible environments.
**Accessibility intent:** Labelled selector, keyboard operation, descriptive policy text.
**Deterministic validation intent:** Catalog, owner isolation, disabled, and arbitrary-URL negatives.
**Compatibility / rollback:** Disable selector/flag; no Stage 1 environment field.
**Non-goals:** Editing origins or browsing arbitrary sites.
**Architect-ready questions:** What non-sensitive policy summary is safe to show?

### S2-US-003 — M9: Review exact run scope and explicitly authorize start

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need to understand and explicitly authorize exact run scope.
**User-visible location / interaction:** Start-run confirmation dialog.
**Dependencies / architectural prerequisites:** S2-US-002 and approved session/limits policy.
**Acceptance intent:** Given selected environment, when confirmation opens, then it shows environment, immutable case, ordered existing steps, scope/evidence/limits/cancel information; Given confirmation is absent, when start is attempted, then no run starts.
**UI states:** Ready, confirmation-required, policy-blocked, cancelled.
**Authorization / privacy:** Shows only owned approved case data; no credentials/secrets.
**Accessibility intent:** Focus-trapped dialog with clear confirm/cancel actions.
**Deterministic validation intent:** Required-confirmation, snapshot-preview, and policy-block tests.
**Compatibility / rollback:** Flag off confirmation; no Stage 1 approval mutation.
**Non-goals:** Editing steps, credentials, or hidden model settings.
**Architect-ready questions:** Which limits/evidence summary and session notice are approved?

### S2-US-004 — M9: Start one run and see queued state

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need one start action to create one queued run.
**User-visible location / interaction:** Confirmed start action and queued status card.
**Dependencies / architectural prerequisites:** S2-US-003, idempotent API, durable queue.
**Acceptance intent:** Given explicit authorization, when start is submitted, then one queued run is shown; Given retry/reload, when repeated, then no duplicate run is created and UI remains unblocked.
**UI states:** Submitting, queued, reconciled duplicate, failed safely.
**Authorization / privacy:** Start remains owner-scoped and does not expose queue internals.
**Accessibility intent:** Busy state and queued confirmation announced.
**Deterministic validation intent:** Idempotency, duplicate-click, reload, and API-failure fixtures.
**Compatibility / rollback:** Feature flag disables start; records remain readable.
**Non-goals:** Synchronous execution or browser work in request.
**Architect-ready questions:** What safe queued/reconciled wording is accepted?

### S2-US-005 — M9: Monitor lifecycle progress and cancel safely

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need accurate progress and safe cancellation.
**User-visible location / interaction:** Run status card/detail cancel action.
**Dependencies / architectural prerequisites:** S2-US-004, worker/lifecycle/poll/cancel contracts.
**Acceptance intent:** Given a run, when state changes, then QUEUED, STARTING, RUNNING, AWAITING_USER, COMPLETED, CANCELLED, or ERROR is shown; Given cancel is available, when requested, then accurate cancel state is shown.
**UI states:** Each lifecycle state, cancelling, cancellation unavailable, polling error.
**Authorization / privacy:** Owner-only status/cancel; inaccessible run is `404`.
**Accessibility intent:** Live status updates are concise and controls remain keyboard-safe.
**Deterministic validation intent:** Lifecycle transition, polling, cancellation-race, and inaccessible-ID tests.
**Compatibility / rollback:** Hide controls under flag; retain read-only history.
**Non-goals:** Worker leasing/retry administration UI.
**Architect-ready questions:** Which lifecycle transitions are user-visible and cancellable?

## M9a website capabilities

### S2-US-006 — M9a: Review each step action, observation, assertion, and outcome

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As a reviewer, I need to inspect what occurred for every source step.
**User-visible location / interaction:** Ordered run-detail step timeline.
**Dependencies / architectural prerequisites:** S2-US-004 and step/evidence contracts.
**Acceptance intent:** Given a run, when step detail is viewed, then source order, intended/actual action, expected/met result, outcome, times, evidence, and method appear; Given missing/skipped work, when present, then it is explicit.
**UI states:** Pending, active, complete, failed, blocked, missing/skipped, unavailable.
**Authorization / privacy:** Only authorized run evidence metadata is shown.
**Accessibility intent:** Semantic ordered structure and non-color-only outcomes.
**Deterministic validation intent:** Source-order, missing/skipped, method, and redaction fixtures.
**Compatibility / rollback:** Run detail can be hidden; Stage 1 Test Steps unchanged.
**Non-goals:** Reordering/editing source steps or silent inference.
**Architect-ready questions:** What step outcome vocabulary is approved?

### S2-US-007 — M9a: Understand final result and failure classification

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As a reviewer, I need to understand the final result without confusing product and runner failures.
**User-visible location / interaction:** Run-detail result summary.
**Dependencies / architectural prerequisites:** S2-US-005 and S2-US-006.
**Acceptance intent:** Given terminal run, when summary is displayed, then lifecycle is separate from PASSED/FAILED/BLOCKED and infrastructure ERROR; Given incomplete work, when present, then it is never shown as pass.
**UI states:** Passed, failed, blocked, error, cancelled, incomplete/unavailable.
**Authorization / privacy:** Safe bounded reasons; no provider internals/secrets.
**Accessibility intent:** Clear text labels, headings, and announced severity.
**Deterministic validation intent:** Classification and no-false-pass fixtures.
**Compatibility / rollback:** Hide result summary without changing Stage 1 review.
**Non-goals:** Auto-approval or overwriting original run.
**Architect-ready questions:** Which safe reason codes/messages are user-visible?

### S2-US-008 — M9a: View authorized redacted evidence

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need to open permitted redacted evidence for a run.
**User-visible location / interaction:** Evidence link/drawer from step or run detail.
**Dependencies / architectural prerequisites:** S2-US-006 and authorization/classification/redaction/retention contracts.
**Acceptance intent:** Given permitted evidence, when opened, then screenshot/bounded metadata is retrieved by opaque reference with visible redaction; Given unavailable evidence, when retrieval fails, then retryable unavailable state appears without paths, secrets, prompts, or raw state.
**UI states:** Available, redacted, unavailable/retryable, forbidden/not found.
**Authorization / privacy:** Owner-only opaque reference; no filesystem path or raw browser/provider state.
**Accessibility intent:** Evidence alternatives and meaningful unavailable explanation.
**Deterministic validation intent:** IDOR, redaction, bounded metadata, unavailable-retry fixtures.
**Compatibility / rollback:** Disable evidence view; retain protected records.
**Non-goals:** Evidence download/share or retention administration.
**Architect-ready questions:** Which redaction markers and metadata are safe to display?

### S2-US-009 — M9a: Browse paged immutable run history

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need to browse past runs without historical evidence changing.
**User-visible location / interaction:** Test Case run-history list with page controls and URL state.
**Dependencies / architectural prerequisites:** S2-US-004, S2-US-007, and S2-US-008.
**Acceptance intent:** Given owned run history, when paged, then bounded pages and URL state are preserved; Given a past run, when opened, then immutable details appear and are not overwritten.
**UI states:** Loading, populated, empty, page boundary, unavailable/retryable.
**Authorization / privacy:** Owner-scoped history only; no false empty result on failed request.
**Accessibility intent:** Keyboard page controls and current-page semantics.
**Deterministic validation intent:** Paging, URL restoration, immutability, owner-404, failure fixtures.
**Compatibility / rollback:** Hide history behind flag; preserve existing Stage 1 history.
**Non-goals:** Editing/deleting/re-running historical evidence.
**Architect-ready questions:** What page bounds and sort semantics are appropriate?

### S2-US-010 — M9a: Recover from safe accessible execution errors

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need safe, accessible recovery guidance for execution states.
**User-visible location / interaction:** Execution panel, run detail, and error/retry affordances.
**Dependencies / architectural prerequisites:** S2-US-001–009.
**Acceptance intent:** Given loading, empty, `404`, policy, evidence, cancel, or infrastructure state, when shown, then semantic severity and safe next action appear; Given retry/navigation, when used by keyboard, then focus is safe and no false zero/success/sensitive detail appears.
**UI states:** Loading, empty, not found, policy-blocked, evidence unavailable, cancelling/cancelled, infrastructure error, retrying.
**Authorization / privacy:** Generic non-leaking errors and owner-scoped `404`.
**Accessibility intent:** Focus restoration, keyboard retry/navigation, announced severity.
**Deterministic validation intent:** All named state, severity, retry, focus, and no-leak fixtures.
**Compatibility / rollback:** Reuse established error patterns; flag off execution affordances.
**Non-goals:** Exposing raw provider/browser diagnostics.
**Architect-ready questions:** Which errors are retryable and what safe wording applies?

## M9b website capabilities

### S2-US-011 — M9b: See when and why visual fallback was used

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As a reviewer, I need to see whether visual fallback was used and why.
**User-visible location / interaction:** Step timeline execution-method badge and detail.
**Dependencies / architectural prerequisites:** Stable S2-US-006–010 and recorded semantic gap.
**Acceptance intent:** Given visual fallback, when shown, then `playwright_semantic` or `computer_visual`, recorded gap, reason, and permitted evidence are clear; Given no gap, when viewed, then visual fallback is never shown as default.
**UI states:** Semantic, visual fallback, gap recorded, evidence unavailable.
**Authorization / privacy:** Safe reason/evidence only; preserve redaction.
**Accessibility intent:** Method is textual, not icon/color only.
**Deterministic validation intent:** Method/gap/default-denial/redaction fixtures.
**Compatibility / rollback:** Disable visual display independently.
**Non-goals:** Initiating arbitrary visual control.
**Architect-ready questions:** What user-safe gap language is approved?

### S2-US-012 — M9b: Respond explicitly to an execution safety pause

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As an owner, I need to explicitly continue, decline, or cancel a safety pause.
**User-visible location / interaction:** AWAITING_USER run detail prompt.
**Dependencies / architectural prerequisites:** S2-US-011 and approved confirmation/continuation policy.
**Acceptance intent:** Given AWAITING_USER, when shown, then reason and owner-only continue/decline/cancel actions appear; Given expired, duplicate, declined, or unauthorized action, when submitted, then it resolves safely with no silent acknowledgement.
**UI states:** Awaiting decision, continuing, declined, cancelled, expired, unauthorized, unavailable.
**Authorization / privacy:** Owner-only confirmation and auditable safe reason.
**Accessibility intent:** Focused decision prompt and keyboard-safe explicit choices.
**Deterministic validation intent:** Expired/duplicate/declined/unauthorized/no-silent-ack fixtures.
**Compatibility / rollback:** Continue remains deferred unless approved; disable action/retain audit.
**Non-goals:** Generic approval or model authorization.
**Architect-ready questions:** Is continuation required, who can act, and what expiry applies?

## M10 non-executing website capabilities

### S2-US-013 — M10: Review automation readiness and unsupported gaps

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As a QA analyst, I need to review readiness and unsupported gaps without claiming execution.
**User-visible location / interaction:** Approved TestCase automation-readiness panel.
**Dependencies / architectural prerequisites:** Approved immutable snapshot and M10 catalogs/gates; independent of M9 success.
**Acceptance intent:** Given approved snapshot, when readiness is viewed, then suitability, gaps, and traceability appear; Given unsupported condition, when present, then it is not claimed as automatable and no execution occurs.
**UI states:** Ready, not ready, gaps found, unavailable.
**Authorization / privacy:** Owner-scoped approved snapshot only; no secret data.
**Accessibility intent:** Readable findings structure and clear status text.
**Deterministic validation intent:** Suitable/unsupported/missing-catalog/traceability fixtures.
**Compatibility / rollback:** Hide panel; Stage 1 unchanged.
**Non-goals:** Automation execution or browser launch.
**Architect-ready questions:** Which readiness categories and wording are approved?

### S2-US-014 — M10: Inspect and review neutral non-executing draft

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As a reviewer, I need to inspect a safe neutral draft and lifecycle state.
**User-visible location / interaction:** Automation artifact detail/revision/review view.
**Dependencies / architectural prerequisites:** S2-US-013 and validated IR/lifecycle contracts.
**Acceptance intent:** Given eligible draft, when inspected, then setup/action/assertion/cleanup, data, placeholders, reusable actions, findings, traceability, revisions, and exact states `DRAFT`, `VALIDATION_FAILED`, `VALIDATED`, `REVIEWED`, `APPROVED`, `CHANGES_REQUESTED`, `ARCHIVED` appear; Given invalid transition/action, when attempted, then only valid actions are offered.
**UI states:** All exact lifecycle states, loading, unavailable, action blocked.
**Authorization / privacy:** Owner-scoped review; no executable/secret content.
**Accessibility intent:** Structured sections, revision comparison, keyboard review actions.
**Deterministic validation intent:** State/transition, revision, secret/executable redaction, authorization fixtures.
**Compatibility / rollback:** Disable draft view; retain immutable history.
**Non-goals:** Running artifact or external connector action.
**Architect-ready questions:** Which lifecycle actions/comments are user-visible?

### S2-US-015 — M10: Export approved non-executing Copado draft

**Status:** ROADMAP — NOT AUTHORIZED
**Persona narrative / outcome:** As a reviewer, I need to export only an approved draft with clear non-execution status.
**User-visible location / interaction:** Approved artifact export action/result.
**Dependencies / architectural prerequisites:** S2-US-014 and deterministic renderer/adapter contracts.
**Acceptance intent:** Given approved eligible draft, when exported, then schema/version/digest/source and `NON_EXECUTING_DRAFT` are visible; Given invalid state/input, when requested, then actionable safe failure appears with no import, deploy, schedule, recall, or execution.
**UI states:** Export available, preparing, complete, validation failed, unauthorized, unavailable.
**Authorization / privacy:** Owner-scoped export; no secret adapter input.
**Accessibility intent:** Export result/status and failures are textual and announced.
**Deterministic validation intent:** Approved-only, digest/version, renderer-failure, non-execution, authorization fixtures.
**Compatibility / rollback:** Disable export; retain artifact/revision history.
**Non-goals:** Copado import/deploy/schedule/recall/execute.
**Architect-ready questions:** What safe failure detail and export metadata are approved?

## Removed or deferred non-website categories

The following are retained as prerequisite-ledger work and must not be created as
numbered website stories: pilot charter/governance; evaluation governance; schema,
migrations, lifecycle records, repositories; standalone APIs; queues/fenced
leases; execution-agent validator; browser-driver policy; evidence-store and
retention operations; and M11 operations. They require separate approved
architecture, security, delivery, and release work.

## Compatibility and rollback

A feature flag and execution kill switch disable new execution interactions
without changing Stage 1 generation, Test Steps, review, traceability, history,
or approved export. Expanded records remain read-only during rollback. No
downward Flyway migration, raw evidence exposure, autonomous rerun, or production
execution target is authorized.

## Definition of Ready

A website story is ready only after its prerequisites are accepted; Architect
confirmation documents UI acceptance, server authority, owner `404` behavior,
environment/allowlist/session policy, limits/kill switch, redaction/retention,
forward-only rollback, deterministic validation/evaluation, and security review.
It also needs a named sole Builder and active ExecPlan. Otherwise it remains
roadmap-only.

## Canonical links

- [TF-007](../exec-plans/active/TF-007-stage-two-agentic-test-execution.md)
- [Product](../PRODUCT.md) and [MVP 1](mvp-1-test-generation.md)
- [Architecture](../ARCHITECTURE.md), [domain model](../architecture/domain-model.md), [security model](../architecture/security-model.md), and [Copado generation architecture](../architecture/copado-generation-architecture.md)
- [Target architecture](../architecture/target-architecture.md), [API](../API.md), and [gap analysis](../assessment/gap-analysis.md)
- [Testing](../TESTING.md), [rubric](../../evals/RUBRIC.md), [Security](../../SECURITY.md), and [threat model](../THREAT_MODEL.md)
- [Planning](../PLANS.md), [repository plans](../../PLANS.md), and [ExecPlan lifecycle](../exec-plans/README.md)

## Authorization boundary

This document authorizes **no implementation**. It does not authorize schema,
backend, frontend, API, provider, browser, worker, Docker, test environment,
evaluation, deployment, credentials, or external-system changes. Such work needs
explicit user authority, future approved Architect handoff, active ExecPlan,
sole Builder assignment, and required reviews.
