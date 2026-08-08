# TF-005 Codebase Review Remediation and Simplification

## Status and ownership

- Status: Active
- Baseline: clean synchronized `main`/`origin/main` at
  `7289bdff170dd50c4deee33506047a8bd6e4bcb8`
- Lead: coordinating root agent
- Architect: read-only approved handoff and post-build conformance
- Builder: `/root/tf005_builder`, sole workspace writer for this delivery
- Reviewer: independent read-only final gate
- Source request: simplify the codebase using the supplied assessment, then
  re-evaluate and remediate all 33 confirmed findings from
  `codebase-review-findings-2026-08-04/`.

Preserve every tracked review report, completed TF-000 through TF-004 plan, and
TF-004 comment improvement. Never edit Flyway V1 through V5, execute generated
automation, call a live model, stage, commit, push, open a PR, merge, deploy, or
mutate external systems as part of local implementation.

## Objective

Implement the minimum complete repository-grounded simplifications, then close
all 33 confirmed defects without weakening owner authorization, evidence
integrity, validation, security controls, tests, or CI. Preserve compatibility
through forward-only V6 expansion, dual writes, bounded legacy adapters, and
explicit legacy-reconstruction provenance.

## Architecture decisions

### S1 — H2 partial isolation

Move H2 to Maven test scope so it is absent from the production Boot artifact
and runtime dependency graph. Delete `application-demo.yml` and retire the H2
demo command. Compose/PostgreSQL with explicit
`TESTFORGE_DEMO_SEED_ENABLED=true` becomes the only seeded demo. Retain
`application-test.yml` and the fast H2 API/migration suites. Put
PostgreSQL-specific locking assertions in Testcontainers. Do not introduce a
separate opt-in Maven profile because default `mvn verify` must not silently
omit database tests. Complete removal of H2 test fixtures remains deferred
until equivalent fast, non-skipping PostgreSQL coverage exists.

### S2 — verification orchestration

Add `scripts/verify.py` as the sole cross-platform local orchestrator.
`verify.sh` and `verify.ps1` become thin interpreter/argument launchers with
exact exit propagation and support for `--harness-only` and `-HarnessOnly`.
Keep `validate-harness.py` as the separate semantic repository/evaluation
validator. Python owns tool/version checks and harness/backend/frontend order,
including `comments:check` and deterministic audit-policy tests. Retain the
seven existing CI job identities.

### S3 — Stage 2 surface

Delete the four unused `com.testforge.automation` Java types and their JaCoCo
exclusion. Preserve conceptual `AutomationDraftGenerator` roadmap documentation
and all three synthetic, nonblocking JSONL fixtures. Validate fixture-owned
shapes directly in the harness, not via Java. Stage 2 remains unimplemented,
non-executing, and nonblocking.

### S4 — Nginx retained

Reject/defer serving the SPA through Spring. Retain unprivileged Nginx under ADR
0006 for an independently buildable/deployable frontend, CSP and headers,
same-origin API proxying, and future separation. Fix trusted-edge and Compose
exposure findings within this topology. Reconsider consolidation only through a
later deployment ADR.

## Finding clusters and provenance limits

No confirmed finding is obsolete, duplicate, or false positive. Shared
implementation clusters are P1-001/P2-008 transaction coordination;
P1-002/P2-006 immutable criterion snapshots and aggregate versioning;
P1-003/P1-009 direct semantics; P1-004/P2-001 schema/validator defense;
P1-008/P2-004 trusted edge; P2-002/P3-004 auth routing;
P2-010/P2-014/P3-003 pagination; P1-005/P2-013 terminal browser states; and
P3-006/P3-007 verification hygiene. Exact pre-V6 criterion history is
irrecoverable and must be labeled `LEGACY_RECONSTRUCTED`. P2-005 requires
verified immutable pins and provenance; SBOM/update-bot work is deferred.
P2-012 does not require per-field visit tracking.

## Ordered implementation milestones

### Milestone 0 — plan and baseline

1. Materialize this active plan as the Builder's first repository write and
   re-read it before any implementation write.
2. Record base SHA/status and protected tracked artifacts.
3. Keep observed evidence separate from intended checks.

### Milestone 1 — verification, LF, H2, and Stage 2

- Add `.gitattributes` with LF normalization and binary exclusions.
- Add `scripts/verify.py` and `scripts/test_verify.py`; reduce both launchers to
  thin aliases with exact exit propagation.
- Include frontend `comments:check` and `test:audit-policy` in full local and CI
  verification without renaming the seven jobs.
- Put H2 in test scope, delete `application-demo.yml`, retain test H2 resources,
  and ensure the packaged runtime excludes H2.
- Delete the four production automation Java types and remove the coverage
  exclusion.
- Validate automation-roadmap shapes and `requiredDraftSections` directly from
  JSONL/constants; keep exactly three nonblocking roadmap fixtures.
- Add only the two missing intent comments at the client request function and
  `RequirementPage`'s normalized-data-name helper.
- Add ADR 0011 for the runtime PostgreSQL/test-database boundary, superseding
  only ADR 0008's demo choice without rewriting historical ADR 0008.

### Milestone 2 — forward-only V6 expansion

Create
`backend/src/main/resources/db/migration/V6__generation_evidence_and_revision_provenance.sql`.
Do not edit V1 through V5.

- Add nullable generation-run release tuple and source provenance columns:
  `provider_adapter_version`, `schema_version`, `validator_version`,
  `source_requirement_version`, and `source_snapshot_provenance`.
- Add `generation_criterion_snapshots` with an ID, run FK, retained source
  criterion UUID without cascading FK, key, description, sort order, source
  requirement version, `EXACT|LEGACY_RECONSTRUCTED` provenance, and unique
  run/key and run/order constraints.
- Add `snapshot_traceability_links` with snapshot, case, coverage type,
  confidence, creation time, and uniqueness. Keep legacy `traceability_links`
  unchanged and dual-write for prior-binary rollback.
- Add nullable `change_type` and `change_reason` to test-case revisions.
- Backfill knowable V5 evidence as `LEGACY_RECONSTRUCTED` with an explicit legacy
  release tuple. Allow V5-shaped old-column inserts after V6. Do not contract,
  null-harden, or remove legacy storage.
- Reconcile snapshot-null old-binary rows from then-current criteria as
  reconstructed in the new binary.
- Add snapshot/provenance entities and repositories; extend generation run and
  revision contracts. Test fresh H2/PostgreSQL V1-V6, V3/V4/V5-to-V6, seeded V5
  evidence backfill, old-column inserts, and nullable rollback compatibility.

### Milestone 3 — authentication, client address, and problems

- Extract refresh rotation to a separate transactional
  `RefreshTokenRotationService`; keep `AuthService.refresh` nontransactional and
  add pessimistic locked token lookup.
- Commit success rotation atomically. Commit replay marking and family
  revocation before `AuthService` returns a generic 401. Concurrent predecessor
  uses yield exactly one success, one rejection, one lineage, and no usable
  successor after replay.
- Add `TrustedClientAddressResolver`. Trust exactly one sanitized forwarded IP
  only when the socket peer is in configured trusted-proxy CIDRs; ignore
  malformed, multiple, or untrusted headers. Nginx overwrites `X-Forwarded-For`
  with `$remote_addr`. Disable Spring's generic forwarded-header strategy.
- Rate-limit both resolved client and a bounded digest of normalized account or
  token subject; never retain a raw email, password, or refresh token as a key.
- Timestamp every handled Problem Detail from the existing UTC clock.
- Emit current-session invalidation when refresh fails; have `AuthContext`
  clear user/loading state. Do not let resets for login/logout or stale refresh
  completions invalidate a newer session.
- Preserve pathname, search, and fragment through a shared safe internal-return
  helper that rejects scheme-relative, cross-origin, backslash/control,
  protocol, and malformed targets.
- Leave login fields empty and remove the unconditional demo notice. Seeded
  credentials remain explicit documentation/e2e data only.

### Milestone 4 — generation transaction, idempotency, validation, release tuple

Refactor `GenerationService` to a nontransactional orchestrator plus one
`GenerationTransactionService` owning short claim, complete, and failure
transactions. Do not add a queue, worker, or duplicate claim/finalizer services.

- Claim by locking the owned requirement row shared with criterion mutations;
  authorize, check project/supersession/idempotency, create one PENDING run with
  exact release metadata and criterion snapshots, and commit before provider
  work. Existing keys return the same run without provider invocation.
- Make provider calls outside any database transaction. Retry exactly once for
  semantic validation or retryable incomplete/empty/malformed structured
  output. Never retry refusal, auth/configuration, or transport failures.
- Finalize by locking/reloading PENDING state and atomically persisting validated
  ambiguities, cases/parts, both link forms, requirement state, audit evidence,
  and terminal run. Store only a bounded safe failure code/message. Reject late
  responses for already-terminal state.
- Define a stale-PENDING timeout greater than two provider-call bounds plus
  margin. Later get/claim operations terminalize stale claims as
  `FAILED/stale_generation_claim` and never invoke the same idempotency key.
- Keep prompt `manual-test-v1` and result contract `manual-test-result-v1`.
  Release `manual-test-schema-v2`, `manual-test-validator-v2`, and
  `openai-responses-v3`; provider remains `openai-responses`, default model
  `gpt-5.6-sol`. Add `GenerationContractVersions` and provider adapter-version
  reporting. Expose/store the tuple and source provenance.
- Retain schema v1 and add schema v2. Both schema and Java validator independently
  enforce persistence-safe limits: cases 1-25, steps 1-30, title 300,
  objective/rationale/final/precondition/action/expected 4000,
  preconditions/test data 30, data name 200, description 2000, example 1000,
  strategy 100, reference 1000, bounded source keys, and ambiguities 50/text
  4000. Reject runtime maxima above hard limits; parity-test deliberate
  provider/application duplication.
- Validate every provider-authored text for executable/unsafe and concrete-text
  rules. Require the union of `ACCEPTANCE_CRITERIA` mappings to equal every
  source criterion; supporting mappings do not satisfy direct coverage.
- Keep generation POST at 201 because it creates a run. Only `COMPLETED` may
  produce frontend success/review/export state; handle `PENDING`, `FAILED`, and
  `REJECTED_BY_VALIDATION` distinctly.

### Milestone 5 — criterion aggregate, immutable evidence, coverage/export/audit

- Add a locked owned-requirement repository operation shared by generation
  claims and criterion mutations.
- Require owning requirement version on every criterion mutation through one
  shared strict `If-Match` parser. Compare the aggregate version, save a complete
  ordered pre-change requirement revision, mutate, and mark criteria changed so
  version/updated time advance. Do not add criterion-level optimistic version.
- Read traceability, coverage, export, and case assembly from immutable
  snapshots. Expose reconstructed legacy provenance.
- Make primary covered/approved counts and percentages DIRECT-only and add
  explicit partial/supporting and approved breakdowns using distinct criterion
  counts.
- Use one conservative Markdown untrusted-text encoder for every insertion so
  links, images, HTML, autolinks, references, backticks, protocols, and
  multiline payloads remain inert. Preserve JSON and CSV behavior.
- Replace arbitrary audit maps with a closed bounded scalar `AuditMetadata`
  value/factory contract and convert every producer. Keep full reopen reason
  only in controlled revision fields; audit records only that a reason exists.
- Compare test data by normalized name and complete content, not request order,
  so pure reordering is a no-op.

### Milestone 6 — bounded collections and complete frontend workflow

Add canonical pages:

- `GET /api/v1/user-stories/{id}/generation-runs/page`
- `GET /api/v1/user-stories/{id}/test-cases/page`
- `GET /api/v1/test-cases/{id}/reviews`

Return paged generation runs, complete review-relevant test-case list items, and
reviews. Keep old generation-run/test-case arrays temporarily as deprecated,
capped compatibility adapters using the same service/query policy; cap embedded
legacy reviews. Batch-fetch child rows and snapshot links, compute active-set
metadata once, replace per-row aggregate count calls with grouped projections,
and share a `TestCaseResponseAssembler`. Prove bounded SQL query counts for page
sizes 1 and 20.

Add accessible shared pagination with URL-preserved filters. Make all projects,
stories, generation runs, cases, revisions, reviews, and audit records reachable
beyond 20 while preserving selected revisions. Display explicit retryable error
states for coverage, traceability, runs, and cases; never render failed evidence
as zero or empty. Add ambiguity resolution with stale-version refetch. Before a
review action, expose every structured case field. Avoid a broad
`RequirementPage` rewrite; extract only bounded dialogs and pagination pieces.

### Milestone 7 — dependencies, supply chain, Compose, default browser

- Extract a pure `audit-policy.mjs` evaluator from the npm process adapter. Fail
  closed on spawn errors/signals, unexpected exits, invalid JSON,
  `report.error`, unsupported report versions, or missing vulnerability/metadata
  sections. Test deterministically without registry calls.
- Upgrade and lock React Router to the verified fixed stable 8.3.0 planning
  baseline, remove the advisory allowlist, and preserve the resolved dependency
  record. Regenerate the lock only from reviewed registry metadata; never
  hand-edit integrity.
- Pin every action to a reviewed 40-character SHA with a version comment and
  every Docker/Compose/Testcontainers/e2e-stub image to `tag@sha256`, recording
  tag-to-digest provenance. Never guess. Defer SBOM/update-bot work.
- Publish only frontend from default Compose. Use separate edge and internal
  data networks so the frontend cannot reach the database; backend joins both
  and retains provider egress. Keep Nginx health, CSP, SPA fallback, same-origin
  API proxy, and overwritten client address.
- Add a Compose e2e override and a synthetic, test-only external Responses API
  stub that derives valid direct cases from input keys and deterministic safe
  terminal failures, contains no secrets, and is not packaged in the app.
- Move deterministic generation/review/traceability/export and terminal failure
  into default Playwright. Keep a separately named optional live-provider
  evaluation only. Explicitly fill seeded demo credentials in browser tests.

### Milestone 8 — canonical docs, evaluations, and closure

Update relevant root, environment, contribution, API, architecture, testing,
product, security, threat, AI pipeline, domain/security model, assessment,
migration-risk, decision index/ADR 0011, dependency-risk, and PR-template
contracts. Do not rewrite completed ExecPlans, review reports, or historical
evidence; add dated addenda where needed.

Expand manual generation to eight blocking synthetic fixtures with a case that
proves supporting links cannot satisfy direct coverage and a comprehensive
bounds/safety case. Record prompt/result/schema/validator versions and update
the harness/rubric. Preserve exactly three nonblocking Stage 2 roadmap fixtures.
No live candidate evaluation is authorized; Stage 2 scoring remains
nonblocking.

## Exact primary file and surface inventory

The approved surface includes root/ops (`.gitattributes`, Maven/Docker/Compose,
environment, CI, verification scripts, audit scripts); backend configuration,
V6, schemas, auth/security/common/generation/requirement/traceability/test-case/
audit/export/project modules; frontend API/auth/routes/pages/types/shared
pagination/e2e and synthetic stub assets; focused Java/TypeScript/browser tests;
and the canonical documentation/evaluation files named by the milestones.

No new Dependabot or SBOM work is included. No Spring SPA serving, total H2 test
removal, opt-in DB test profile, async generation, organization sharing,
authorization expansion, legacy contraction, exact recovery of already-lost
history, prompt/model change, live-provider evaluation, Stage 2 runtime,
generated automation execution, broad RequirementPage rewrite, unneeded
installs, secrets/customer data, or publication action is permitted.

## Acceptance criteria — all 33 findings

- P0-001: replay family revocation and reuse evidence commit before generic 401.
- P0-002: concurrent predecessor rotation produces one 200, one 401, one
  lineage, and no usable successor after replay.
- P1-001: a provider latch proves no Spring transaction and concurrent ordinary
  database work succeeds.
- P1-002: new-run traceability remains unchanged after source criterion edits or
  deletion; legacy reconstruction is explicit.
- P1-003: generation fails if DIRECT mapping union omits any source criterion.
- P1-004: schema and validator reject every oversize before persistence.
- P1-005: only completed generation is presented as success; pending and failure
  states remain distinct with no partial cases.
- P1-006: audit process, command, and schema failures fail closed in pure tests.
- P1-007: the complete Markdown payload corpus is inert.
- P1-008: spoofed forwarded headers cannot mint address buckets; trusted edge
  preserves real clients.
- P1-009: supporting/partial-only evidence yields zero direct and
  approved-direct coverage.
- P2-001: executable-content checks cover every provider-authored field.
- P2-002: current refresh failure clears protected state; stale failure cannot
  clear a newer session.
- P2-003: router is fixed/locked, the allowlist is gone, and compatibility tests
  pass.
- P2-004: default Compose publishes only frontend; data is internal and backend
  proxy-only.
- P2-005: every action/image reference is immutable with verified provenance.
- P2-006: stale criterion aggregate version returns 409; success increments
  requirement version and stores full pre-change criteria.
- P2-007: malformed/incomplete/empty/semantic output retries once; refusal and
  transport failures do not.
- P2-008: concurrent same-key generation produces one run/provider invocation.
- P2-009: reopen reason is controlled revision evidence, never arbitrary audit
  metadata.
- P2-010: 21-plus projects/stories are accessible with pages and totals.
- P2-011: the UI resolves ambiguity and handles stale refetch.
- P2-012: all structured evidence is visible before review actions.
- P2-013: default Playwright covers the complete deterministic workflow and a
  real safe failure without live provider access.
- P2-014: every collection is bounded, child histories are paged, and SQL count
  remains independent of page size.
- P2-015: failed evidence queries render retryable error state, never false zero.
- P3-001: every handled application problem has a server timestamp.
- P3-002: normal login has no prefilled credentials or demo banner.
- P3-003: histories beyond 20 are reachable.
- P3-004: safe query/fragment return is preserved and unsafe targets rejected.
- P3-005: reordered equivalent test data creates no change evidence.
- P3-006: LF policy is explicit and Prettier passes without weakening.
- P3-007: both missing comments are present and the checker gates local/CI work.

## Simplification acceptance

- S1: the packaged Boot runtime contains PostgreSQL but not H2; H2 tests/default
  Maven verification still run; the seeded demo uses PostgreSQL.
- S2: orchestration/order exists only in `verify.py`; both launchers support full
  and harness-only modes with exact failure propagation.
- S3: there is no production automation Java package or coverage exclusion;
  conceptual docs and exactly three roadmap fixtures validate without execution.
- S4: Nginx intentionally remains with same-origin Compose, Vite CORS, proxy
  headers/CSP, Playwright, and independent frontend build support.

## Security and privacy acceptance

Owner-scoped authorization and cross-owner 404 behavior remain unchanged.
Browser workspace input never authorizes. Raw refresh and idempotency tokens
remain hashed and raw provider bodies, keys, hidden prompt/reasoning, customer
data, and secrets remain outside UI/evidence. Audit metadata is closed and
bounded and excludes requirement bodies/reopen reasons. Provider output remains
untrusted through schema, semantic validation, and Markdown encoding. Snapshot
provenance remains visible. Paging and query caps reduce denial-of-service risk.
H2 is absent at runtime. Proxy trust is explicit and fail-safe. E2E provider
behavior is synthetic, test-only, and non-secret. Generated automation remains
unimplemented and never executes. Action/image pins must be verified, not
guessed.

## V6 compatibility and rollback

Use expand/backfill/dual-write/read-switch only. Old binaries ignore new
nullable columns/tables and continue legacy links. New binaries dual-write and
reconcile missing snapshots after old-binary rollback as
`LEGACY_RECONSTRUCTED`. V5-shaped inserts still succeed. No existing column or
table is removed and no new column is required. Old artifacts can roll back
against V6 without undoing the migration. Contracting legacy links and array
routes requires a later observe/reconcile release. Already-lost source history
cannot be recovered and must not be represented as exact.

## Deterministic validation matrix

Record only observed evidence:

1. `python scripts/validate-harness.py`
2. `python -m unittest scripts/test_verify.py` or the implemented discover path
3. Focused backend migration, PostgreSQL concurrency/transaction, validation,
   provider retry, traceability, export, audit, query-count, problem, and address
   tests
4. `cd backend && mvn --batch-mode --no-transfer-progress verify`
5. Frontend `test:audit-policy`, `comments:check`, `format:check`, `lint`,
   `typecheck`, `test:coverage`, and `build`
6. Packaged artifact/dependency assertion: PostgreSQL present, H2 absent, and no
   production `com.testforge.automation`
7. `docker compose config --quiet`
8. `docker compose -f docker-compose.yml -f docker-compose.e2e.yml config --quiet`
9. Where locally authorized/available: build/start/wait the e2e override, run
   default browser tests, inspect failures, and tear down volumes
10. Full PowerShell wrapper, both launcher harness-only aliases, and the POSIX
    launcher in a supported POSIX environment
11. `git diff --check`, status, and explicit preservation checks for reports,
    completed plans, TF-004 comments, and applied migrations

Normal verification performs no installation, registry audit, live-provider
call, or generated automation execution. Lock/digest resolution is
implementation work using reviewed official external metadata; if unavailable,
block rather than guess. Candidate AI scoring is not authorized and remains
not run.

## Review and completion protocol

After implementation and local evidence, the Lead requests read-only Architect
`CONFORMS` or `BLOCK`, followed by independent Reviewer `APPROVE` or `BLOCK`.
The same Builder resolves findings and repeats affected checks. Only after both
verdicts and a Lead implementation-completion decision may this Builder record
final evidence and move the plan to `completed/`.

Staging, commit, push, publication, and merge are separate publisher work. A
published exact SHA must pass all seven unchanged CI job identities: Agent and
evaluation harness; Backend verify; Frontend verify; Dependency security;
Repository secret scan; Container vulnerability scan; Docker end-to-end and
accessibility.

## Risks and deviations

- React Router major-version compatibility requires direct verification.
- V6 mixed-binary reconciliation cannot restore already lost criterion history.
- Trusted-proxy CIDRs require correct deployment configuration.
- Image digest resolution must cover the intended platforms.
- The deterministic e2e stub cannot establish live-provider fidelity.
- Dependency locks and immutable pins require authorized official metadata.
- This is an unusually broad integrated delivery; no partial milestone may be
  represented as closure of all 33 findings.

## Living progress and observed evidence

- 2026-08-05: Architect handoff received in four parts. Builder assignment,
  baseline branch, and one-writer boundary confirmed. This active plan was the
  Builder's first repository write. No implementation evidence recorded yet.
- 2026-08-05: Milestone 1 implementation added the Python verifier and four
  deterministic unit tests, reduced both platform launchers, introduced LF
  policy, test-scoped H2, removed the H2 demo resource and unused Stage 2 Java
  surface, moved roadmap-shape ownership into JSONL/harness constants, closed
  the two comment-check gaps, and recorded ADR 0011. Observed checks:
  `python -m unittest scripts/test_verify.py` passed 4/4;
  `python scripts/validate-harness.py` passed with 3 profiles, 6 skills, 6
  blocking manual fixtures, and 3 nonblocking roadmap fixtures; and
  `npm --prefix frontend run comments:check` passed. The newly wired
  `test:audit-policy` command is intentionally unavailable until Milestone 7.
- 2026-08-05: Milestones 2 through 7 were implemented as the approved bounded
  clusters: forward-only V6 evidence/revision expansion; locked refresh
  rotation and trusted-edge address resolution; nontransactional generation
  orchestration with transactional claim/finalization and one controlled
  retry; immutable criterion snapshots, DIRECT-only primary metrics, closed
  audit metadata, inert Markdown export, aggregate criterion concurrency, and
  order-insensitive test data; bounded canonical pages plus complete frontend
  review/error/ambiguity/history states; fail-closed dependency policy, fixed
  React Router lock, immutable workflow/image pins, internal Compose data
  networking, and a deterministic synthetic Responses stub/default browser
  workflow. V1 through V5 were not edited and the compatibility array routes
  remain capped adapters.
- 2026-08-05: Milestone 8 updated canonical product, API, architecture,
  testing, security, threat, migration, dependency, contribution, environment,
  and evaluation contracts. Manual-generation fixtures now total eight and
  pin `manual-test-v1`, `manual-test-result-v1`, `manual-test-schema-v2`, and
  `manual-test-validator-v2`; the three Stage 2 fixtures remain roadmap-only
  and nonblocking. No candidate scoring or live provider call was run.
- 2026-08-05: `python -m unittest scripts/test_verify.py` passed 4/4;
  `python scripts/validate-harness.py` and
  `.\scripts\verify.ps1 -HarnessOnly` passed with 3 profiles, 6 skills, 8
  blocking manual fixtures, and 3 roadmap fixtures. The POSIX alias was not
  run because Bash is not installed in this Windows environment.
- 2026-08-05: `.\scripts\verify.ps1` passed end to end using the existing Java
  21/Maven runtime. Backend `mvn verify` passed 50 tests with 0 failures/errors,
  2 Docker-only PostgreSQL tests skipped, clean Spotless/SpotBugs, and all
  JaCoCo thresholds met. Frontend audit-policy tests passed 10/10, comment and
  formatting checks passed, lint/typecheck passed, Vitest passed 31/31 across
  6 files with 83.22% statements, 66.89% branches, 76.44% functions, and
  84.22% lines, and the production build passed.
- 2026-08-05: Packaged-artifact inspection found one PostgreSQL driver, zero H2
  drivers, and zero production `com.testforge.automation` class files. Docker
  and Docker Compose are not installed, so PostgreSQL Testcontainers,
  Compose-config validation, and local Playwright e2e remain explicitly
  unobserved rather than represented as passing. Deterministic H2 fresh and
  V3/V4/V5-to-V6 upgrade coverage passed locally; exact PostgreSQL and browser
  execution remain publication-CI evidence.
- 2026-08-05: Retry regression testing exposed one same-exception
  self-suppression edge that misclassified an exhausted semantic retry as
  `FAILED`; the orchestrator now preserves `REJECTED_BY_VALIDATION` for that
  case. Static analysis then required moving the cross-property stale-timeout
  invariant from construction to post-construction validation; the invariant
  remains enforced and the repeated full gate passed. OpenAPI defaults were
  reconciled to fail closed (`false`) across Compose and the environment
  example, matching the documented contract.
- 2026-08-05: A final acceptance re-audit found that P2-014 was not yet fully
  closed: project/story summaries, generation metadata, and complete test-case
  assembly still performed per-row repository reads. The collection paths now
  use grouped count/set-number projections and a shared batch-loading
  `TestCaseResponseAssembler`; directly superseded unpaged/count repository
  methods were removed. `StageOneApiIntegrationTest` now compares Hibernate
  prepared-statement counts for page sizes 1 and 20 across projects, stories,
  runs, and cases. The focused final test passed 10/10 and demonstrated that
  collection query counts remain bounded independently of page size. The prior
  full-wrapper result predates this final remediation and will be superseded by
  a fresh complete run before review handoff.
- 2026-08-05: The fresh backend gate first exposed only a Spotless import-order
  violation in the new query-count test; the import was reordered without a
  formatter rewrite. The repeated Java 21/Maven 3.9.11 `mvn verify` passed 51
  tests with 0 failures/errors and 2 Docker-only PostgreSQL skips. Spotless and
  SpotBugs were clean and all JaCoCo thresholds passed. The final P2-014 test
  passed as part of the 10-test Stage One API integration suite.
- 2026-08-05: The complete `scripts/verify.ps1` wrapper passed after explicitly
  placing the repository's existing Java 21 and Maven runtimes on `PATH`; two
  earlier launcher attempts stopped before backend execution because Maven was
  absent from `PATH`, then because the ambient `java` was version 11. The
  successful run passed the harness (3 profiles, 6 skills, 8 blocking manual
  fixtures, 3 nonblocking roadmap fixtures), the full backend gate, frontend
  audit policy 10/10, comments, formatting, lint, typecheck, Vitest 31/31 with
  83.22% statements/66.89% branches/76.44% functions/84.22% lines, and the
  production build. `python -m unittest scripts/test_verify.py` passed 4/4 and
  the PowerShell harness-only alias passed. Bash remains unavailable, so the
  POSIX alias remains unobserved.
- 2026-08-05: Exact packaged-JAR paths confirmed one PostgreSQL JDBC driver,
  zero H2 drivers, and zero `com.testforge.automation` production classes. An
  initial broad substring inspection was discarded because it matched OAuth2
  library names; only exact `BOOT-INF/lib/{postgresql,h2}-` paths are evidence.
  The generated `backend/target` directory was then removed and a redacted
  Gitleaks directory scan passed with no leaks across 7.06 MB. No machine-local
  paths were found.
- 2026-08-05: Playwright discovery passed and lists four default, non-live
  Chromium tests covering shell/accessibility, generation-review-export,
  owner isolation, and safe provider failure. Their execution and both Compose
  configuration checks remain unobserved because Docker/Compose is unavailable.
  `git diff --check` passed; all 97 tracked diff files contain semantic changes,
  with zero whitespace-only tracked diffs. Review reports, completed plans, and
  applied migrations V1 through V5 have zero changes.
- 2026-08-05: Post-build Architect and security reviews returned `BLOCK` with
  source-level gaps. The same Builder will remediate only these findings:
  PostgreSQL Testcontainers coverage for refresh/generation locking; runtime
  `LEGACY_RECONSTRUCTED` evidence reconciliation after mixed-binary writes;
  fail-closed coverage/case/traceability UI; complete pre-review case evidence;
  explicit seeded-login data in default Playwright; UUID tie-breaking for equal
  generation completion times; one generic refresh failure surface with
  committed lineage/reuse/audit assertions; strict-schema supported-keyword
  preflight without unsupported `uniqueItems`; coherent fail-closed audit
  parsing; hard-bounded rate-limit key cardinality; expanded executable-output
  field-matrix validation; and literal-only, no-DNS proxy address parsing.
  Unrelated candidate files remain frozen. Focused checks and the complete
  deterministic wrapper will be repeated before a new conformance handoff.
- 2026-08-05: The same Builder closed the review findings without adding a
  second implementation path. PostgreSQL-gated tests now cover real refresh
  family locking/replay persistence and same-key generation concurrency while
  a provider latch asserts that provider work is outside a transaction. Bounded
  runtime reconcilers repair V5-shaped post-V6 generation rows and legacy reopen
  evidence with explicit provenance and closed metadata. Refresh failures now
  share one generic response/cookie contract; forwarded addresses use a
  literal-only parser; limiter state has a hard cardinality cap; generation set
  ties include UUID ordering; CORS permits `If-Match`; strict-schema preflight,
  exact/max-plus-one parity, complete unsafe-field coverage, and coherent audit
  severity/status validation are deterministic. The frontend now fails closed
  for evidence errors, exposes every structured field before review, retains
  pages/filters/selected sets in the URL, and the default e2e login supplies
  explicit synthetic credentials. Superseded unbounded repository methods and
  a duplicate legacy bridge fetch/per-link existence query were removed.
- 2026-08-05: The first post-remediation backend lifecycle executed all tests
  successfully but stopped at seven Spotless differences. The configured
  formatter made only mechanical changes. The repeated Java 21/Maven 3.9.11
  offline `mvn verify` passed 63 tests with 0 failures/errors and 4
  Docker/Testcontainers skips; Spotless and SpotBugs were clean and all JaCoCo
  thresholds passed. Stage One passed 14/14, including mixed-binary generation,
  legacy reopen transfer/redaction, generic refresh failures, deterministic
  equal-time set ordering, and bounded query-count assertions.
- 2026-08-05: The first post-remediation frontend coverage run exposed a real
  query-state race: Clear Filters issued four independent navigations and could
  retain one stale filter. The shared URL updater now applies multi-control
  changes atomically, including generation-set/page transitions. The focused
  workflow suite then passed 4/4. The repeated complete frontend gate passed
  audit policy 12/12, all intent-comment checks, Prettier, ESLint, TypeScript,
  Vitest 31/31, and the production build. Coverage was 82.70% statements,
  68.73% branches, 75.39% functions, and 84.24% lines.
- 2026-08-05: `python -m unittest scripts/test_verify.py` passed 4/4,
  `python scripts/validate-harness.py` and the PowerShell harness-only alias
  passed with 3 profiles, 6 skills, 8 blocking manual fixtures, and 3
  nonblocking roadmap fixtures. The fresh full PowerShell wrapper passed end to
  end with the same backend/frontend evidence. Bash is unavailable, so the
  POSIX launcher remains unobserved.
- 2026-08-05: Packaged-artifact inspection found one PostgreSQL driver, zero H2
  drivers, and zero production automation classes. Playwright discovery listed
  four default non-live Chromium tests. Docker/Compose remains unavailable, so
  the four PostgreSQL Testcontainers tests, Compose configuration checks, and
  browser execution are explicitly skipped/unobserved pending supported
  exact-SHA CI. Gitleaks initially reported only the generated test artifact's
  synthetic access-token value; after removing `backend/target` and the Python
  bytecode cache, a redacted 7.20 MB source-tree scan passed with no leaks.
  `git diff --check` passed, no machine-specific path is present, and review
  reports, completed plans, and applied Flyway V1 through V5 have zero changes.
- 2026-08-05: Repeat Architect/security review identified remaining boundary
  coverage and read-safety gaps. The same Builder added exhaustive exact/max+1
  scalar and collection parity, fenced-code coverage across every
  provider-authored field, and an integration assertion that rejected output
  creates no case or traceability graph. Generation history now exposes
  `PENDING`, `FAILED`, and `REJECTED_BY_VALIDATION` attempts with reachable
  paging even when no cases exist. Project audit-filter reset uses one URL
  transition. The dependency gate rejects hidden nested high/critical
  advisories and incoherent package/advisory severity.
- 2026-08-05: Security remediation bounds legacy reopen reconciliation to one
  100-row batch per authorized read, applies a truthful response-only pending
  redaction to remaining rows, and reserves `legacyReasonMigrated` for committed
  revision transfer. H2 integration covers 101 legacy rows plus an interleaved
  bridge write. Refresh tests now assert family lineage, successor revocation,
  reuse flags, security-audit counts, and that a pre-authentication 429 does not
  expire a valid cookie. The in-process limiter now performs constant-time
  steady/rejected checks with one amortized minute rollover, and IPv4 tails are
  accepted only in the final IPv6 low-order slot.
- 2026-08-05: The first repeat focused backend run executed 27 tests and found
  only three test defects: an IPv4-mapped-address normalization expectation, a
  test query's nonexistent `issued_at` column, and a raw-reason count that also
  matched the closed `reasonRecorded` key. Correcting those assertions left
  production behavior unchanged; the repeat focused backend suite passed
  27/27. The first focused frontend workflow run passed 3/5; its two failures
  were test-harness observations (a cached page required direct URL assertion,
  and the custom history handler had lower precedence than the shared fixture).
  The corrected focused workflow suite passed 5/5, with TypeScript and lint
  clean and audit-policy tests 14/14.
- 2026-08-05: Fresh Java 21/Maven 3.9.11 `mvn verify` and the complete
  `scripts/verify.ps1` wrapper passed. Backend results were 66 tests with zero
  failures/errors and four Docker-only PostgreSQL skips; Spotless, SpotBugs,
  and all JaCoCo thresholds passed. Frontend results were audit policy 14/14,
  comments/Prettier/ESLint/TypeScript clean, Vitest 32/32, and a successful
  production build. Coverage was 83.23% statements, 70.91% branches, 76.56%
  functions, and 84.82% lines. The harness passed with three profiles, six
  skills, eight blocking manual fixtures, and three nonblocking roadmap
  fixtures; verifier unit tests passed 4/4; `git diff --check` passed; the known
  NVD key prefix is absent; V1-V5, review reports, and completed plans remain
  unchanged. Playwright discovery still lists four default tests, but Docker,
  PostgreSQL Testcontainers execution, Compose checks, and browser execution
  remain unobserved locally and require supported exact-SHA CI. The active plan
  remains active pending Architect conformance, security/Reviewer approval, and
  the Lead's implementation-completion decision.
- 2026-08-05: A further Architect/security pass returned `BLOCK` for six narrow
  evidence and simplification gaps. Direct exports now reconcile eligible
  bridge-written runs before snapshot reads and use the shared whole-page
  assembler; integration coverage preserves criterion mappings and proves the
  prepared-statement count does not grow between one and twenty approved cases.
  The schema-pointer matrix now covers every application-owned collection and
  scalar limit, all twelve `MAX_TEXT` paths, and the step-number ceiling. The
  dependency audit blocks empty `via` evidence and object advisories without a
  supported severity. Minute sampling now occurs under the limiter lock and a
  delayed stale-minute request cannot roll the window backward. Superseded
  singleton/unpaged repository declarations were removed, and the API,
  architecture, and testing documents record the resulting export contract.
- 2026-08-05: Focused remediation evidence passed: backend generation-schema,
  limiter, and Stage One integration tests passed 24/24; audit-policy tests
  passed 16/16. The first full backend run found only four missing Javadocs on
  the new controlled-clock test helper; after adding those comments, the final
  Java 21/Maven 3.9.11 `mvn verify` passed 67 tests with zero failures/errors and
  four Docker-only PostgreSQL skips. Spotless, SpotBugs, packaging, and all
  JaCoCo thresholds passed. The complete PowerShell wrapper passed the harness,
  the same backend gate, frontend comments/Prettier/ESLint/TypeScript, 16/16
  audit-policy tests, Vitest 32/32 with 83.23% statements, 70.91% branches,
  76.56% functions, and 84.82% lines, plus the production build. Verifier unit
  tests passed 4/4 and the PowerShell harness-only alias passed. Exact packaged
  paths contain one PostgreSQL driver, zero H2 drivers, and zero production
  automation classes. `git diff --check`, the secret-prefix scan, and
  preservation checks for V1-V5, review reports, and completed plans passed.
  Docker/Compose is not installed and the available Git Bash environment lacks
  `python3`, so Compose, PostgreSQL/Testcontainers, browser execution, and the
  POSIX launcher remain explicitly unobserved pending supported exact-SHA CI.
  The active plan remains active for fresh Architect conformance, independent
  Reviewer approval, and the Lead's implementation-completion decision.
- 2026-08-05: The dedicated generation/evaluation gate then returned `BLOCK`
  for two release-integrity gaps: `manual-test-result-v1` was declared but not
  persisted/exposed with each run, and structured output did not fail closed
  for unknown properties, missing/null `automationCandidate`, or the domain's
  `CRITICAL` ambiguity severity. The same Builder remediated only those gaps.
  Forward-only V6 now adds nullable `result_contract_version`, deterministically
  backfills pre-V6 rows as `legacy-unknown`, and preserves V5-shaped post-V6
  inserts as null until runtime reconciliation. New claims persist the exact
  application-owned result version, and backend/API/frontend release tuples
  expose it. The provider uses a strict per-contract Jackson reader; semantic
  validation rejects missing/null automation intent before final persistence;
  schema v2 intentionally includes the already-canonical `CRITICAL` severity;
  and harness parity now compares that enum with the Java domain. Focused unit,
  parity, adapter, migration, and API tests passed 40/40, including unknown
  fields, null primitive values, absent/null automation intent, exact/legacy
  release evidence, upgrade compatibility, and no partial case/traceability
  persistence after validation rejection. Candidate scoring and live-provider
  calls were not run because they were neither required nor authorized.
- 2026-08-05: The final Java 21/Maven 3.9.11 PowerShell wrapper passed end to
  end after the generation remediation. The deterministic harness passed with
  three profiles, six skills, eight blocking manual fixtures, and three
  nonblocking roadmap fixtures. Backend `mvn verify` passed 71 tests with zero
  failures/errors and four Docker-only PostgreSQL skips; Spotless and SpotBugs
  were clean, all JaCoCo thresholds passed, and packaging succeeded. Frontend
  audit-policy tests passed 16/16; intent comments, Prettier, ESLint, and
  TypeScript were clean; Vitest passed 32/32 across six files with 83.23%
  statements, 70.91% branches, 76.56% functions, and 84.82% lines; and the
  production build passed. Verifier unit tests passed 4/4. Exact packaged-JAR
  inspection found one PostgreSQL driver, zero H2 drivers, and zero production
  automation classes. `git diff --check` passed, no Python bytecode cache was
  present, the secret-pattern scan matched only removed demo/password material,
  and applied migrations V1 through V5 remain untouched. Docker/Compose,
  PostgreSQL Testcontainers execution, browser execution, and the POSIX
  launcher remain explicitly unobserved locally and require supported exact-SHA
  CI. The candidate is frozen for a fresh generation/evaluation decision,
  Architect conformance, and independent Reviewer approval before the Lead's
  implementation-completion decision.
- 2026-08-05: The two final read-only gates returned `BLOCK` and the Lead
  routed four consolidated findings to the same sole Builder. Existing-key
  generation POSTs must reconcile V5-shaped evidence and return the refreshed
  run without a provider call. The provider boundary must reject root-authored
  values, scalar/numeric coercion, fractional integers, and numeric enums; this
  behavior change advances only the provider-adapter version. In-flight source
  criterion renames must dual-write legacy links by captured criterion identity,
  while source deletion must fail atomically with an explicit safe source-change
  outcome and no cases. Frontend notices must carry typed outcome/severity so
  only `COMPLETED` is success and pending/failed/validation-rejected states are
  semantically distinct. No new service, migration, persistent role, prompt,
  result, schema, validator, model, or provider change is approved. The Builder
  will add focused integration/provider/UI evidence, run generation-impact and
  complete deterministic checks, remove any superseded remediation helper, and
  freeze the tree for fresh read-only review.
- 2026-08-05: The same sole Builder closed those four findings without adding
  a parallel implementation path. Existing-key generation POSTs now invoke the
  shared `LegacyGenerationEvidenceReconciler` and return refreshed V5-shaped
  evidence without calling the provider. Claims capture criterion snapshot and
  source UUID together in one immutable map; an in-flight key rename therefore
  dual-writes the legacy link by identity, while deletion produces the safe
  terminal `FAILED/source_criteria_changed` outcome before any case or link is
  written. The provider now deserializes only its exact three-field wire result
  through a contract-local strict reader that rejects unknown root fields,
  scalar coercion, float-to-integer coercion, and numeric enums. Only the
  adapter advanced, from `openai-responses-v2` to `openai-responses-v3`.
  Frontend notices now carry typed severity: `PENDING` is informational,
  `FAILED` is error, `REJECTED_BY_VALIDATION` is warning, and only `COMPLETED`
  is success.
- 2026-08-05: Focused backend verification passed 40/40 across generation
  service, provider validation, schema parity, strict OpenAI adapter, and Stage
  One API integration tests. The 20-test integration suite includes same-key
  V5 reconciliation with zero provider calls and provider-latch rename/delete
  mutation cases. Its first run found only the test's invalid synthetic key
  `AC-RENAMED`; changing the fixture to valid key `AC-9` left production code
  unchanged. Focused frontend formatting, TypeScript, ESLint, and the workflow
  suite passed; the workflow suite passed 6/6 after its first run corrected
  assertions from obsolete MUI `standard*` classes to the rendered `color*`
  severity classes. Harness validation passed with three profiles, six skills,
  eight blocking manual fixtures, and three nonblocking roadmap fixtures;
  verifier unit tests passed 4/4.
- 2026-08-05: Generation-impact review confirmed the release tuple is prompt
  `manual-test-v1`, result `manual-test-result-v1`, schema
  `manual-test-schema-v2`, validator `manual-test-validator-v2`, provider
  `openai-responses`, default model `gpt-5.6-sol`, and adapter
  `openai-responses-v3`. The eight blocking manual fixture IDs and three
  nonblocking automation-roadmap fixtures are unchanged. Provider-visible
  source fields remain title, story, business requirements, assumptions, and
  keyed criteria; UUID and correlation metadata remain excluded; provider
  storage remains disabled. Candidate scoring, live-provider calls, and
  generated automation execution were not run because they were not authorized
  and no prompt, result, schema, validator, model, provider, or fixture contract
  changed.
- 2026-08-05: The final Java 21/Maven 3.9.11 PowerShell wrapper passed end to
  end. Backend `mvn verify` passed 75 tests with zero failures/errors and four
  Docker-only PostgreSQL skips; Spotless and SpotBugs were clean, all JaCoCo
  thresholds passed, and packaging succeeded. Frontend audit-policy tests
  passed 16/16; comments, Prettier, ESLint, and TypeScript were clean; Vitest
  passed 33/33 across six files with 83.90% statements, 71.48% branches, 77.04%
  functions, and 85.55% lines; and the production build passed. Two launcher
  attempts stopped before backend execution because Maven and then Java 21 were
  absent from the process `PATH`; the successful invocation adjusted only its
  process environment and did not persist machine paths.
- 2026-08-05: Final integrity and redundancy checks passed. `git diff --check`
  is clean apart from informational line-ending normalization notices. Exact
  packaged-JAR inspection found one PostgreSQL driver, zero H2 drivers, and
  zero production automation classes. The known NVD-key prefix, machine-local
  paths, stale adapter-v2 references, message-string severity inference,
  superseded parallel captured-criterion maps, and production automation
  execution references are absent. Applied Flyway V1 through V5, review
  reports, and completed plans remain unchanged; no Python bytecode cache is
  present. Docker/Compose, the four PostgreSQL Testcontainers tests, browser
  execution, and the POSIX launcher remain unobserved locally and require
  supported exact-SHA CI. The candidate is frozen for a fresh generation/eval
  decision, Architect conformance, independent Reviewer approval, and then the
  Lead's implementation-completion decision.
- 2026-08-05: Fresh generation/evaluation and security reviews both returned
  `BLOCK` on the same documentation omission: two release-tuple definitions in
  `docs/architecture/ai-generation-pipeline.md` did not name the independently
  versioned result contract. The same sole Builder made the smallest canonical
  correction, adding result-contract version to the per-stage tuple and the
  evaluation-triggering generation tuple, consistent with
  `manual-test-result-v1`, the API, rubric, and all eight fixtures. No source,
  configuration, fixture, or executable contract changed. Harness validation
  passed with three profiles, six skills, eight blocking manual fixtures, and
  three nonblocking roadmap fixtures; verifier unit tests passed 4/4; and
  `git diff --check` passed with only informational line-ending notices. Final
  searches found no stale five-member tuple wording, adapter-v2 reference, or
  duplicate tuple explanation. Because this remediation changed documentation
  only, the freshly observed full-wrapper result (backend 75 tests and frontend
  33 tests, all deterministic gates green apart from four Docker-only skips)
  remains applicable and was not rerun. The candidate is frozen again for fresh
  read-only verdicts.
- 2026-08-05: The independent Reviewer returned `BLOCK` on four final gaps and
  the Lead routed them to the same sole Builder: the verifier did not fail
  closed when installed npm dependencies diverged from the reviewed manifest
  and lock; regeneration depended on the visible run page and allocated a new
  idempotency key for confirmation retry; orphan legacy reopen audit rows could
  be labeled migrated without source-linked revision evidence; and generation
  endpoint headers allowed whitespace-only idempotency keys past controller
  validation. The existing `node_modules` resolves React Router 7.18.2 while
  the manifest/lock require 8.3.0. No dependency installation is authorized;
  frontend runtime evidence remains preliminary until the Lead synchronizes the
  reviewed lock. The Builder will remediate only these four paths, add focused
  deterministic coverage, and freeze for repeat review.
- 2026-08-05: The same sole Builder closed the four Reviewer paths without a
  second parser, endpoint, reconciliation service, or generation role. The
  canonical verifier now runs read-only
  `npm ls --all --json --loglevel=silent` before backend/frontend verification and propagates a
  nonzero tree-integrity result. The existing run-page contract now exposes the
  resolver-owned `activeGenerationRunId` independently of visible items;
  regeneration uses that identity to choose `/regenerate` and reuses one
  operation key for the confirmed retry.
  Legacy reopen reconciliation preserves orphan audit rows for operator repair,
  flushes a source-linked revision before closed metadata replacement, and
  reports only rows it actually reconciled so startup cannot loop on an orphan.
  Both generation endpoints now combine `@NotBlank` with the existing size
  bound; method-level constraint failures use the existing Problem Detail
  surface and never reach the provider.
- 2026-08-05: Focused correction evidence passed. Verifier unit tests passed
  5/5, including stale-tree/nonzero behavior. `StageOneApiIntegrationTest`
  passed 21/21 and covers page-independent active identity, preserved orphan
  reason evidence, no false migrated label, no orphan revision, whitespace-only
  keys returning 400 on both routes, and zero provider calls.
  `DocumentationCoverageTest` passed 1/1 and Spotless passed. The frontend
  workflow suite passed 7/7, including an active set absent
  from `runPage=2`, `/regenerate?confirmSupersede=false` then `true`, and an
  identical nonblank idempotency key; Prettier, TypeScript, and ESLint passed.
  Harness and final integrity checks are recorded below after the plan write.
- 2026-08-05: The first focused frontend runs exposed only test-handler
  precedence and entry through the deprecated alias rather than the requested
  canonical `runPage=2` URL; the corrected test enters the canonical User Story
  route and production regeneration behavior was unchanged. The first focused
  backend run executed all 21 tests but surfaced
  the missing method-constraint Problem Detail mapping; adding the bounded 400
  handler closed the actual controller-boundary gap. The first Spotless check
  requested only annotation line wrapping; the canonical formatting was
  applied. All affected checks then passed.
- 2026-08-05: Installed dependency integrity remains deliberately unresolved in
  the workspace because installation is not authorized. Read-only `npm ls`
  fails closed with `ELSPROBLEMS`: installed `react-router@7.18.2` is invalid
  against exact manifest/lock `8.3.0`, and stale `react-router-dom@7.18.2` is
  extraneous. The Lead must synchronize `node_modules` from the reviewed lock
  before rerunning the complete wrapper. Therefore all frontend runtime results
  observed with the stale tree are preliminary, the prior full-wrapper 75/33
  result is superseded as completion evidence, and no new complete wrapper pass
  is claimed. No install, live provider, generated automation, staging, commit,
  push, PR, merge, or protected-file change occurred.
- 2026-08-05: Final post-remediation integrity evidence passed. Harness
  validation again reported three specialist profiles, six skills, eight
  blocking manual fixtures, and three nonblocking roadmap fixtures; verifier
  unit tests passed 5/5. `git diff --check` exited zero with only informational
  working-tree line-ending notices. Searches confirmed the page-limited active
  run scan and inline retry key are absent, the stable active-run identity and
  two `@NotBlank` generation headers are present, no stale adapter-v2 contract
  reference remains outside this historical plan, and no known NVD key or
  machine-local path was added. Applied Flyway V1 through V5, review reports,
  and completed plans remain unchanged, and no Python bytecode cache is
  present. The candidate is frozen for Lead-managed dependency synchronization,
  a complete wrapper rerun, Architect conformance, and independent Reviewer
  approval.

## Architect-approved additive model-routing handoff

The Lead authorizes the same sole Builder to add a bounded Codex model-routing
overlay within TF-005. This overlay applies only after an existing workflow has
authorized a role; it creates no agent, capability, gate, or permission. It does
not claim that this already-running parent thread changes models.

- Keep `.codex/config.toml` on exactly three concurrent agents. Route the
  primary and default subagent model to `gpt-5.6-terra` at medium reasoning.
- Add only `gpt-5.6-sol` with high reasoning to the existing Architect profile.
  Add only `gpt-5.6-terra` with high reasoning to the existing Builder profile.
  Give the existing Reviewer high reasoning without a persistent model pin, so
  normal review inherits Terra while an explicitly temporary security or
  high-risk generation review may be launched on Sol high.
- Preserve every existing instruction/profile block and its authorization,
  sandbox, ownership, and output contract. Do not create explorer,
  implementer, security-reviewer, documentation, Luna, or other role profiles.
- Add a concise overlay to `AGENTS.md`, an additive mapping table to
  `docs/agents/AGENT_ROSTER.md`, and an additive audit note to
  `docs/agents/WORKFLOW_AUDIT.md`. Preserve the existing role and capability
  tables.
- Extend `scripts/validate-harness.py` and its working-tree self-tests to
  require the exact supported global/profile mappings, exactly three role
  files, concurrency three, Reviewer model inheritance, and rejection of Luna,
  generic model slugs, persistent max/ultra effort, extra roles, or a Reviewer
  model pin. Preserve all current repository, workflow, and generation checks.
- Do not touch application provider/model configuration, prompts, fixtures,
  migrations, or application code. Do not install dependencies or publish.

Acceptance evidence is `python scripts/validate-harness.py`,
`.\scripts\verify.ps1 -HarnessOnly`, `python -m unittest scripts.test_verify`,
and `git diff --check`, plus a zero-deletion/subsequence proof for pre-existing
instruction and configuration content and an exact inventory of three files in
`.codex/agents/`. The stale `node_modules` dependency-tree blocker and resulting
full-wrapper residual remain unchanged.

### Model-routing implementation evidence

- 2026-08-05: The additive overlay is implemented without another role or
  workflow path. `.codex/config.toml` routes the primary and default temporary
  subagent to `gpt-5.6-terra` at medium reasoning and retains enabled agents at
  maximum concurrency three. Architect is Sol high, Builder is Terra high, and
  Reviewer has high reasoning with no model pin. The root guide, roster, and
  workflow audit document authorization-only routing, unchanged gates, and the
  temporary Sol-high Reviewer exception.
- 2026-08-05: The harness requires the exact global keys and values and the
  exact three profile mappings. Its self-tests accept the supported mapping and
  reject missing, renamed, or extra roles; Luna and generic model slugs;
  persistent max/ultra reasoning; concurrency other than three; nested or
  escaping role configuration; duplicate TOML; and a Reviewer model pin. The
  prior repository, skill, documentation, source-contract, manual-evaluation,
  and roadmap-evaluation checks remain in the same validation flow.
- 2026-08-05: All approved focused checks passed. `python
  scripts/validate-harness.py` passed with three profiles, six skills, eight
  blocking manual fixtures, and three nonblocking roadmap fixtures.
  `.\scripts\verify.ps1 -HarnessOnly` passed. `python -m unittest
  scripts.test_verify` passed 5/5. `git diff --check` exited zero with only
  informational line-ending notices. The exact unittest command created two
  Python bytecode files under `scripts/__pycache__`; only those generated files
  and the empty generated directory were removed, restoring the pre-check
  no-bytecode state.
- 2026-08-05: Zero-deletion evidence passed. Git numstat against the pre-existing
  tracked content reports zero deleted lines for `.codex/config.toml`, all
  three profiles, `AGENTS.md`, `AGENT_ROSTER.md`, and `WORKFLOW_AUDIT.md`; the
  only counts are 5, 2, 2, 1, 10, 17, and 16 added lines respectively.
  `.codex/agents/` contains exactly `architect.toml`, `builder.toml`, and
  `reviewer.toml`. No application provider/model configuration, prompt,
  fixture, migration, or application source file changed in this routing
  slice.
- 2026-08-05: No dependency install, live-provider call, generated automation,
  staging, commit, push, PR, or publication occurred. The stale installed
  React Router tree remains unchanged, so the complete wrapper is still blocked
  pending Lead-managed synchronization of `node_modules` from the reviewed
  lock. This routing candidate is frozen for Architect conformance and
  independent Reviewer approval together with the rest of TF-005.
- 2026-08-05: Post-build Architect conformance returned `BLOCK` on two narrow
  durable-documentation omissions. Authorized repository exploration must route
  to the built-in read-only explorer with `$repository-audit` on the
  Terra-medium default without creating a persistent explorer profile. Max
  reasoning must never be persisted and may be chosen manually only for
  exceptional work. The same Builder will add only these concepts to the root
  guide, roster, workflow audit, and existing harness assertions/self-tests,
  then repeat the approved routing checks.
- 2026-08-05: The Architect documentation BLOCK is remediated without adding a
  role, route, or repeated instruction block. The root guide, roster, and
  workflow audit now state that authorized repository exploration uses the
  built-in read-only explorer with `$repository-audit` on the Terra-medium
  default and never a persistent explorer profile. They also state that Max
  reasoning is never persisted and may be selected manually only for
  exceptional work. The existing harness requires all six normalized policy
  concepts in each document and its self-tests prove removal of any one concept
  is detected; every prior config, profile, workflow, source-contract, skill,
  documentation, and evaluation check remains active.
- 2026-08-05: Repeat focused evidence passed. `python
  scripts/validate-harness.py` and `.\scripts\verify.ps1 -HarnessOnly` passed
  with three profiles, six skills, eight blocking manual fixtures, and three
  nonblocking roadmap fixtures. `python -m unittest scripts.test_verify` passed
  5/5. `git diff --check` exited zero with only informational line-ending
  notices. The exact profile inventory remains `architect.toml`, `builder.toml`,
  and `reviewer.toml`. Zero-deletion numstat remains clean: config 5/0,
  Architect 2/0, Builder 2/0, Reviewer 1/0, root guide 14/0, roster 21/0, and
  workflow audit 20/0 added/deleted. The unittest-created bytecode artifacts
  were again removed from the known `scripts/__pycache__` path, leaving no
  generated bytecode.
- 2026-08-05: This documentation-only correction did not touch application,
  provider, prompt, fixture, migration, or dependency files. No install or
  publication action occurred. The stale installed React Router tree and
  full-wrapper residual remain unchanged. The candidate is frozen again for
  fresh Architect conformance and independent Reviewer approval.
- 2026-08-05: Fresh post-remediation Architect conformance returned
  `CONFORMS`, and the independent Reviewer returned `APPROVE`, for the additive
  model-routing slice. The Lead therefore decides that this slice is
  implementation-complete. Broader TF-005 remains active because the stale
  `node_modules` tree still blocks a current complete-wrapper result; this plan
  is intentionally not moved to `completed/`.

### Final local evidence addendum — 2026-08-05

- The stale dependency-tree blocker is superseded. An Architect-approved narrow
  integrity exception restored the exact `node_modules/@emnapi/core` and
  `node_modules/@emnapi/runtime` lock records byte-for-byte from `HEAD` after
  package-manager regeneration did not retain the platform-unselected optional
  records. This provenance preserves the reviewed resolved URLs and integrity
  values without invention or recalculation; `frontend/package.json` was
  unchanged. The Lead-authorized `npm ci` then completed with 379 packages and
  zero vulnerabilities, and `npm ls --all --json --loglevel=silent` completed
  cleanly.
- Source-conformance evidence: the exact pinned PostgreSQL `DockerImageName`
  now has one package-private test definition in `PostgreSqlTestImage`, declared
  compatible with `postgres`; both PostgreSQL test classes consume that shared
  immutable name. This fixes the Testcontainers compatibility root cause
  without changing production code, Compose, the image digest, migrations, or
  configuration. The focused PostgreSQL suites passed 4/4.
- The complete local PowerShell wrapper passed: harness validation reported 3
  specialist profiles, 6 skills, 8 blocking manual fixtures, and 3 nonblocking
  roadmap fixtures; backend verification passed 76 tests with 0 failures and 0
  errors, 0 SpotBugs findings, and all JaCoCo thresholds met. Frontend
  audit-policy tests passed 16/16, Vitest passed 34 tests, and format, lint,
  typecheck, and build passed. `docker compose config --quiet` and `docker
  compose -f docker-compose.yml -f docker-compose.e2e.yml config --quiet` both
  exited 0. No live-provider scoring or generated-automation execution occurred.
- Residuals and remaining gates: default Playwright execution and the POSIX
  launcher remain unobserved and require supported exact-SHA CI. TF-005 remains
  active pending fresh post-build Architect conformance, independent Reviewer
  `APPROVE`, and the Lead's implementation-completion decision; only then may
  this Builder record final local evidence and move the plan to `completed/`.

### Completion record — 2026-08-05

- Fresh post-build Architect review returned `CONFORMS`; the independent
  Reviewer returned `APPROVE` with no findings. The Lead decided TF-005 is
  implementation-complete locally, so this full historical plan is moved to
  `docs/exec-plans/completed/`.
- Default Playwright execution and the POSIX launcher remain unobserved. They
  and all supported exact-SHA CI jobs are publication evidence, not a
  prerequisite for this local plan completion. No live-provider scoring,
  generated-automation execution, staging, commit, push, PR, deployment, or
  publication occurred.
