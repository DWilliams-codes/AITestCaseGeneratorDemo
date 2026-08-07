# TF-007 — Stage 2 agentic TestForge test execution

**Status:** PROPOSED — IMPLEMENTATION NOT AUTHORIZED
**Owner:** Lead-assigned sole Builder
**Scope:** Planning and canonical roadmap documentation only. This ExecPlan does
not authorize schema, API, UI, provider, browser, worker, or deployment changes.

## Purpose and user-approved decisions

Plan a controlled Stage 2 capability that can actively test **TestForge itself**
as its first and only pilot target. The first deliverable is a basic,
approved-step click-through: the agent navigates the TestForge non-production
application, performs only the supplied reviewed steps, and records whether the
specified observable results occurred. It is not a general web-automation proxy
and it is not a replacement for the existing Stage 1 manual-test workflow.

The execution model remains entirely configuration-selected. Terra is the first
manually evaluated candidate; no model identifier, autonomous routing rule, or
Terra-to-Sol escalation is a Stage 2 contract until evaluation evidence supports
one. Existing `AutomationDraftGenerator` and Copado work stay intact as a
secondary, non-executing roadmap capability.

## Baseline and compatibility boundary

Preserve the current Java 21 Spring modular monolith, React TypeScript SPA,
PostgreSQL/Flyway system of record, owner-scoped `404` authorization, immutable
Stage 1 generation/review/traceability evidence, provider-neutral generation
boundary, and approved-only export. Do not change Stage 1 APIs or treat a
browser-supplied tenant/workspace context as authorization.

Stage 2 execution is distinct from generated automation: it interprets one
human-approved current TestCase through a small approved browser-tool interface
and captures an immutable run-bound snapshot before work begins. It does not
depend on M2–M8, the future general snapshot model, or workspace-sharing
evolution. The smallest pilot reuses current owner-scoped authorization. It does
not generate or execute arbitrary Playwright/JavaScript/program code. A future
persistent regression-script feature is separately scoped.

All planned persistence evolves through forward-only Flyway migrations using
expand, deterministic backfill where needed, dual-write, observe/reconcile,
read/policy switch, and only later contract. The prior binary must remain viable
against every expanded schema during its rollback window.

## Architecture and contracts to implement only after authorization

### Boundaries

Add provider-neutral boundaries with repository-conventional names equivalent to:

- `ExecutionOrchestrator`: creates, leases, resumes, cancels, and finalizes a
  run; the backend alone owns state transitions and persistence.
- `ExecutionAgentClient`: receives minimized approved-step context and returns
  validated structured observations/actions; it has no database, secret-store,
  shell, or arbitrary network authority.
- `BrowserDriver`: exposes a deliberately small browser action/observation
  surface. Semantic actions (`navigate`, `click`, `fill`, `select`, `press`,
  wait, URL/title/visible-text reads, screenshots, console/network summaries)
  are the default. No arbitrary JavaScript or generated program execution.
- `EvidenceStore`: stores bounded, non-public, authorization-scoped evidence by
  opaque reference, never a raw filesystem path.
- `TestRunRepository` and worker lease services: provide durable queue, fencing,
  cancellation, idempotency, retry policy, and terminal audit evidence.

Use browser semantics first. Visual computer interaction is a constrained
fallback only when an approved semantic action cannot operate a necessary
control (for example a canvas or an inaccessible custom widget), and each action
records `playwright_semantic` or `computer_visual`. Computer-use content and
all rendered page text are untrusted data, never instructions.

Current official OpenAI documentation, verified 2026-08-06, supports the
Responses API computer-tool/custom-harness patterns. Its safety guidance must
be applied: isolate the browser, allowlist sites and actions, regard page
content as untrusted, and require human confirmation for risky actions. Do not
build on deprecated `computer-use-preview`, and do not hard-code a model. See
[Computer use](https://developers.openai.com/api/docs/guides/tools-computer-use)
and [latest-model guidance](https://developers.openai.com/api/docs/guides/latest-model).

### Domain and lifecycle

Introduce additive, owner-scoped concepts for the smallest pilot. Workspace
sharing/content-policy evolution remains separate future work:

| Concept | Required responsibility |
| --- | --- |
| `TestEnvironment` | Explicit non-production target, base origin, active/execution flags, navigation policy, and owned-project scope. The initial slice permits only the TestForge pilot environment. |
| `TestRun` | Immutable run-bound snapshot of the approved current TestCase, environment, initiator, configured model/release tuple, execution lifecycle, separate final result, bounded usage/cost metrics, and safe terminal summary/reason. |
| `TestStepRun` | Ordered intended action, actual action/observation, expected result, assertion decision, evidence references, timestamps, execution method, and explicit step outcome. |
| `TestEvidence` | Authorized screenshot, browser-state, console, network-failure, and agent-observation metadata with retention/redaction classification. |
| `ExecutionActionEvent` | Append-only requested/allowed/blocked/performed action evidence, including policy decision and correlation; it prevents the model from silently mutating state. |

Keep lifecycle and outcome separate. A run may have lifecycle
`QUEUED`, `STARTING`, `RUNNING`, `AWAITING_USER`, `COMPLETED`, `CANCELLED`, or
`ERROR`; its completed test result is independently `PASSED`, `FAILED`, or
`BLOCKED`. State names must follow existing enum conventions when implemented.
`FAILED` means the application contradicted an evaluated expected result;
`BLOCKED` means an approved precondition/dependency prevented completion; and
`ERROR` means TestForge execution infrastructure failed. The agent cannot skip a
step silently, assign its own state transition, or call a click-only run a pass.

The structured agent response must be application-schema validated and include
the run ID, each source step ID/outcome, observation, actual result,
expected-result-met indicator, evidence IDs, and safe failure/blocked reason.
Application code validates referential integrity, action allowlists, sequence,
bounds, enum parity, and lifecycle transition before persistence. Deterministic
browser observations are preferred for assertions; model judgement is limited
to genuinely semantic comparisons.

### API, worker, and UI plan

Plan public TestForge-shaped APIs, without exposing provider wire objects:

- `POST /api/v1/test-cases/{testCaseId}/runs` accepts an authorized environment,
  requires an idempotency key, and returns `202` plus the queued run ID.
- `GET /api/v1/test-cases/{testCaseId}/runs?page=&size=` returns the owned,
  paged run history for that current TestCase.
- `GET /api/v1/test-runs/{runId}` returns scoped run metadata, lifecycle,
  result, step execution, usage, and evidence metadata.
- `POST /api/v1/test-runs/{runId}/cancel` requests safe cancellation.
- `GET /api/v1/test-runs/{runId}/evidence/{evidenceId}` retrieves evidence only
  after authenticated owner authorization of the containing run.

The first UI shows only approved cases eligible for the TestForge pilot,
explicit non-production environment selection, a start confirmation, polling
run state, per-step intended/observed/asserted outcome, evidence metadata, and
cancel controls. A `continue` endpoint is deliberately deferred to M9b, if a
current visual computer-use safety flow requires explicit re-authorization. The
worker must not hold a normal HTTP request open: persist a durable job before
work, acquire a fenced lease, use bounded retries only for infrastructure
failures, and recover expired leases. Application assertion failures never
auto-rerun or overwrite the original run.

### Security, authorization, and evidence gates

- Require authenticated owner → owned project/current approved TestCase →
  eligible environment → run/evidence authorization, with inaccessible IDs
  returning `404`. The pilot does not wait for workspace/content-policy delivery.
- At queue time, capture a human-approved current TestCase into an immutable
  run-bound snapshot. Authorization covers only its stated behavior in the
  selected environment; no unrelated exploration or privilege escalation is
  inferred.
- Enforce base origin, explicit allowed/blocked origins, scheme rules, redirect
  cap, max turns/actions/duration/screenshots/tokens/cost/retries, and an
  environment-specific SSRF policy. Localhost/private targets are allowed only
  by an explicit local-development environment policy; production execution is
  out of scope.
- Do not add `TestCredentialProfile` or a credential-profile request input to
  this initial plan. Authenticated-session provisioning and any secret-management
  integration require a separate explicit approval before implementation; raw
  secret values must never enter model context or persisted evidence.
- Treat rendered text, uploads, error messages, DOM labels, and third-party
  content as untrusted application data. The agent must ignore page attempts to
  override instructions, reveal prompts/secrets, navigate elsewhere, or run
  commands. It stops and records a blocked/safety state when policy would be
  crossed.
- Capture final, blocked, failure, and assertion-adjacent screenshots plus
  bounded console/network/URL evidence; minimize duplicates and classify/redact
  sensitive material before authorized retrieval.
- Apply an execution kill switch, budget limits, audit allowlists, retention,
  quarantine, and least-privilege worker/service identity before the pilot.

## Milestone plan

### M9 — Controlled execution foundation

Dependency: current owner-scoped authorization, an approved current TestCase,
an explicit isolated non-production TestForge environment, and the execution
safety controls in this milestone. Capture the immutable run-bound TestCase
snapshot at queue time. M2–M8, future general snapshots, workspace-sharing,
and future workspace/content policy are separate work, not pilot prerequisites.

Deliver the additive run/environment/step/evidence/action-event contracts,
configuration-selected provider-neutral agent/browser interfaces, async worker
with leases, narrow semantic browser tools, validated structured result, API/UI
run visibility, execution limits, cancellation, audit, and a kill switch.

Acceptance: an authorized owner can queue an immutable run-bound snapshot of
the approved current TestForge TestCase; the system executes only allowlisted
semantic actions on the approved origin; step events and final result are
durable, understandable, and owner-scoped; unsafe navigation, injection,
secret exposure, inaccessible-ID access, and policy-limit attempts fail closed.

Rollback: disable execution endpoints/worker through the feature flag and kill
switch; retain expanded records read-only; no Stage 1 manual-test, review,
traceability, generation, or export behavior changes and no down migration.

### M9a — Semantic TestForge click-through MVP

Dependency: M9 foundation and a human-approved, synthetic TestForge pilot test
case.

Deliver the smallest useful vertical slice: one manually evaluated configured
candidate (Terra first), semantic browser navigation/click/fill/press/wait/read
actions, deterministic expected-result checks where possible, and clear
pass/fail/blocked/error evidence. Exercise basic TestForge workflows only; do
not broaden to arbitrary applications or visual clicking by default.

Acceptance: an owner can select an approved basic TestForge case, explicitly
authorize the non-production run, see every step's action/observation/assertion
and final evidence, and distinguish a product failure from blocked dependency
or runner error. Candidate model output is manually evaluated against a new,
versioned, sanitized execution-specific fixture/rubric contract before any later
model/routing decision.

### M9b — Visual fallback and safety continuation

Dependency: stable M9a semantic evidence and safety baseline.

Add computer-use screenshot-driven fallback only for approved interaction gaps,
with per-action execution-method evidence, strict human-confirmation handling
for current provider safety conditions, resume/cancel semantics, and no silent
acknowledgement. Only this later milestone may add
`POST /api/v1/test-runs/{runId}/continue` if the current provider safety flow
requires it. Evaluate visual assertions separately from semantic click-through
and preserve all M9 containment limits.

Acceptance: a permitted visual-only control can be attempted only after its
semantic limitation is recorded; risky provider safety conditions enter
`AWAITING_USER`; decline/cancel is safe; prompt-injection and off-origin tests
remain fail-closed.

### M10 — Automation readiness, neutral draft, and Copado artifact

Dependency: approved immutable manual snapshots, approved source/application
catalog, and automation-specific security/evaluation gates. This preserves the
former M9/M9a/M9b scope as a separate, secondary non-executing capability.

- Add readiness decision, platform-neutral declarative draft, independent
  validator/review, then deterministic version-pinned Copado renderer and
  non-executing export. Keep import, deploy, scheduling, and execution external
  and separately privileged.
- Persist `AutomationArtifact` as exactly `DRAFT`, `VALIDATION_FAILED`,
  `VALIDATED`, `REVIEWED`, `APPROVED`, `CHANGES_REQUESTED`, or `ARCHIVED`.
  Validation reports failure; corrected content creates/revises `DRAFT`; only
  validated output becomes reviewed; only reviewed output becomes approved.
- Define neutral setup/action/assertion/cleanup IR, typed data, selector
  placeholders, reusable actions, suitability/gap evidence, traceability, and
  strict no-secret/no-executable-content validation.
- Pin a supported Copado schema/version and use organization-approved maps,
  reusable actions, naming/data profiles, and environment allowlists as explicit
  adapter input. Emit only an inspectable draft package.
- Tests/evals include approved sanitized automation fixtures, parser
  bombs/import/code/injection/secret/semantic-drift negatives, renderer
  contracts, and a synthetic non-production pilot. No live Copado tenant in
  default CI.

Acceptance: unapproved, incomplete, executable, secret-bearing, or fabricated
drafts cannot pass; exported drafts are digest-bound, reviewed, visibly
non-executing, never auto-recalled, and never executed by TestForge.

### M11 — Production readiness

Enterprise identity/lifecycle, distributed limits, private managed database,
TLS, secrets, encrypted backups/restore drills, redacted telemetry, append-only
audit forwarding, SLOs, incident response, SBOMs/signed artifacts, provider and
Copado DPA/residency/retention/egress reviews, and load/failover/tenant-isolation/
prompt-injection/dependency/container/accessibility/disaster-recovery gates.

Exit: operational ownership/runbooks and recovery objectives are accepted, and
security review has no unresolved release blockers. TestForge execution does
not target production environments as part of this MVP plan.

## Evaluation, validation, and test plan

Before live-provider pilot work, use deterministic fake agent/browser fixtures
and sanitized TestForge scenarios. Add application-owned schemas and semantic
validators plus unit, integration, API authorization, worker lease/retry,
idempotency, cancellation, migration, UI, accessibility, and browser tests.
Test the exact classifications of pass/fail/blocked/error and separation of
action, observation, and assertion.

Security negatives must cover prompt injection in page text, unapproved/off-
origin navigation, SSRF-sensitive targets, redirect exhaustion, secret/log/
evidence leakage, unauthorized run/evidence access, model-produced invalid
actions/results, action/turn/cost/time limits, expired leases, duplicate start,
cancel races, provider safety pauses, and browser/worker/evidence-store failure.
No default local or CI suite calls a live provider or performs generated
automation against a live application.

Use `ai-generation-evals` for provider/model/output-contract release impact.
Define a new future execution-specific rubric and fixture contract for the
sanitized TestForge pilot; its scoring and approval workflow must be accepted
before implementation. Existing manual-test and Copado evaluators, including
`testforge-evaluation`, remain unchanged unless separately expanded through an
approved plan. Maintain a release tuple containing model configuration, agent
prompt/policy, schema, semantic validator, browser policy, fixture version, and
evaluation report. A Terra candidate can be evaluated first, but its outcome is
evidence rather than a model-routing decision.

## Non-goals for the initial execution slice

- Arbitrary third-party or production application testing.
- Autonomous model routing, automatic Terra-to-Sol escalation, or a hard-coded
  execution model.
- Coordinate/visual clicking as the default browser mechanism.
- Arbitrary code, shell, JavaScript, network, secret, or database access by the
  agent.
- Automatic approval, unreviewed-case execution, indefinite loops, or a
  general-purpose browser automation service.
- Generated Playwright scripts, CI regression-script generation, Copado import,
  deployment, scheduling, or execution.
- Kafka, Kubernetes-specific orchestration, broad WebSocket/event-sourcing
  architecture, or any production deployment commitment.

## Unresolved pilot decisions requiring explicit approval before implementation

1. The exact isolated TestForge pilot environment/base origin and its permitted
   local/private-network policy.
2. Eligible approved manual cases, synthetic account/data setup, and which
   basic workflows may mutate pilot data.
3. Credential/session establishment and the approved secret-management/evidence
   redaction mechanism.
4. Retention, storage location, retrieval roles, and redaction policy for
   screenshots, console/network data, and browser state.
5. Initial action, redirect, duration, token/cost, screenshot, retry, and
   concurrency limits; cancellation/lease expiry policy; and kill-switch owner.
6. Current supported provider SDK/tool version and required safety-confirmation
   interaction at implementation time.
7. The sanitized fixture set, manual evaluation rubric, success threshold, and
   approval authority for the Terra candidate. No routing decision precedes this
   evidence.
8. The owner-scoped authorization matrix for the pilot run, list, cancel, and
   evidence routes; workspace sharing remains out of scope.

## Plan-only evidence

This plan intentionally makes no application behavior change. The authorized
documentation validation recorded after edits is: `python scripts/validate-harness.py`,
`git diff --check`, and link/text consistency checks. Results, deviations, and
residual planning risks are appended below after those checks run.

## Verification record

Observed 2026-08-06 (documentation-only change):

- `python scripts/validate-harness.py` — passed. Manual-008 oracle self-tests
  passed with 81 obligations; TestForge harness reported 3 specialist profiles,
  6 skills, 8 blocking manual fixtures, and 3 non-blocking automation roadmap
  fixtures.
- `git diff --check` — passed (exit 0). Git warned that the modified legacy
  CRLF working-copy files will be normalized to LF if Git later touches them;
  no whitespace error was reported.
- `rg -n "TF-007|M9a|M9b|M10|M11|Stage 2 roadmap|Controlled agentic test execution|Automation readiness" README.md docs\\PRODUCT.md docs\\architecture\\target-architecture.md docs\\assessment\\gap-analysis.md docs\\plans\\testforce-ai-mvp-execplan.md docs\\exec-plans\\active\\TF-007-stage-two-agentic-test-execution.md` — passed. It confirmed the
  active-plan references and M9/M9a/M9b/M10/M11 terminology are consistent in
every changed canonical document.

### Architect BLOCK remediation (2026-08-06)

- Removed M9/M9a dependence on M8, future general snapshots, and future
  workspace/content-policy delivery. The first pilot now reuses current
  owner-scoped authorization and captures an immutable run-bound snapshot of the
  approved current TestCase at queue time.
- Replaced the project-nested start API with the approved TestCase-nested start,
  list, run-detail, cancel, and owned-evidence routes. Removed
  `TestCredentialProfile` and credential-profile input from the initial plan;
  session provisioning remains an explicitly unresolved approval. The continue
  route is deferred to M9b visual fallback.
- Replaced the existing manual/Copado evaluator handoff with a new future
  execution-specific rubric and fixture contract. `ai-generation-evals` remains
  the provider/model release-impact owner; existing evaluators remain unchanged.

Remediation validation, observed 2026-08-06:

- `python scripts/validate-harness.py` — passed. Manual-008 oracle self-tests
  passed with 81 obligations; the TestForge harness reported 3 specialist
  profiles, 6 skills, 8 blocking manual fixtures, and 3 non-blocking automation
  roadmap fixtures.
- `git diff --check` — passed (exit 0), with only the existing CRLF-to-LF
  normalization warnings for the modified legacy working-copy files.
- Terminology/API consistency `rg` — passed. It confirmed the owner-scoped,
  run-bound pilot language; the approved start/list/detail/evidence routes; M9a,
  M9b, M10, and M11; and that `testforge-evaluation` is documented only as an
  unchanged evaluator rather than an execution-scoring route.

### Post-remediation plan review evidence (2026-08-06)

- Architect — **CONFORMS**: the remediated plan conforms to the approved
  owner-scoped pilot, API, deferred session-provisioning, and separate
  execution-evaluation decisions.
- Independent Reviewer — **APPROVE**: the plan-document change is approved.

These verdicts approve documentation-plan conformance only. TF-007 remains
**PROPOSED — IMPLEMENTATION NOT AUTHORIZED** in `active/`; explicit user review
and approval are still required before any Stage 2 implementation begins.

Changed files: this active ExecPlan; `README.md`; `docs/PRODUCT.md`;
`docs/architecture/target-architecture.md`; `docs/assessment/gap-analysis.md`;
and the locally ignored canonical umbrella plan
`docs/plans/testforce-ai-mvp-execplan.md`. No file was unignored, staged,
committed, or published.

Skipped by scope: backend/frontend builds, unit/integration/E2E browser tests,
provider calls, live application execution, model evaluation, dependency
installation, and deployment. No provider, browser, generated automation,
external system, staging, commit, push, or deployment action is authorized or
performed.

## Residual risks

Stage 2 execution inherently adds untrusted browser-content, SSRF, secret,
evidence-privacy, and agent-control risk. This proposal leaves it inactive until
the unresolved pilot decisions, current owner-authorization matrix, provider
safety verification, sanitized evaluations, security review, Architect
conformance, and independent Reviewer approval are complete. Future
workspace-sharing, general snapshots, and credential/session provisioning remain
separately authorized work rather than pilot dependencies.
