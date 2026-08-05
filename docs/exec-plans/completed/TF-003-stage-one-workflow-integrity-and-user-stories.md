# TF-003: Stage One workflow integrity and user stories

## Status

Completed — The Lead declared TF-003 implementation complete on 2026-08-04
after the exact repository wrapper and rebuilt-application regression passed on
the final diff, the final Architect returned `CONFORMS`, and the final Reviewer
returned `APPROVE` with no findings. The working tree remained on `main` and
unstaged/uncommitted throughout Builder delivery. Publication remains a separate
authorization and was not performed.

## Source request

Evolve the existing Requirement-centered Stage One workflow into a canonical
ADO/Jira-style User Story experience while preserving storage identity,
authorization, history, compatibility, and old-binary rollback. Make generation
sets, test-data references, review transitions, history, and authentication
session reset behavior deterministic without introducing duplicate concepts.

## Objective

Deliver one integrated Stage One workflow in which a Project owns User Stories;
each story preserves requirements/constraints, assumptions, and acceptance
criteria; successful generation runs form immutable historical generation sets;
active cases have referentially sound test data and controlled review
transitions; history is owner-isolated and reviewable; and authentication resets
cannot reinstall stale tokens.

## Non-goals

- No nested user-story table or physical rename of `requirements`.
- No enterprise boards, sprints, assignments, or future Add coverage workflow.
- No `generation_sets` table and no deletion of historical generation output.
- No generic replacement review workflow, new review statuses, or duplicate
  audit/history stores.
- No change to production prompt, JSON response schema, or model selection.
- No live provider call or generated automation execution.
- No edits to applied V1–V4 migrations; no destructive migration or rollback
  path removal.
- No staging, commit, push, PR, merge, deployment, branch-protection change, or
  external publication by the Builder.
- Do not stop or alter the running local demo processes or browser; final manual
  browser regression and later shutdown are Lead-owned.

## Approved decisions and invariants

1. The existing Requirement aggregate becomes the canonical User Story domain
   concept. Preserve the `requirements` table, UUIDs, work-item numbers, foreign
   keys, and owner predicates. Add only priority: `CRITICAL`, `HIGH`, `MEDIUM`,
   or `LOW`, defaulting to `MEDIUM`.
2. Add only forward migration V5 for `requirements.priority`: nullable bridge,
   deterministic `MEDIUM` backfill, and application null-as-`MEDIUM` behavior so
   the prior binary remains rollback-compatible. V1–V4 remain byte-identical.
3. Canonical API routes use `/user-stories`; thin `/requirements` adapters retain
   DTO parity for create/get/criteria/generate/regenerate/runs/cases/coverage,
   traceability, and export. Projects expose `userStoryCount` while retaining
   deprecated `requirementCount`.
4. Successful `generation_run` rows are generation sets. One shared
   `ActiveGenerationSetResolver` selects latest successful by `completedAt`
   then UUID; failure never supersedes. Default reads are active-only; a
   historical run selector is read-only. History returns `setNumber` and
   `ACTIVE`/`SUPERSEDED`. Regeneration requires `confirmSupersede` if the active
   set contains revisions or review evidence.
5. One shared test-data reference policy enforces stripped case-insensitive
   unique names and resolves every nonblank step reference to exactly one
   canonical name. Backend updates and generation validation reject atomically.
   UI uses dropdown plus None, propagates renames, and requires explicit clear
   or reassignment before referenced-data deletion.
6. `TestCaseEntity` owns valid review/edit transitions. Existing endpoints,
   statuses, tables, revisions, reviews, and audit events remain. Review and
   reopen bodies carry comment/reason plus expected version. Superseded sets and
   terminal cases reject edits/reviews; version mismatch is 409 and mutations
   flush before response.
7. Reuse revisions/reviews/audit events. Add paged normalized case revisions and
   bounded project audit filters with stable timestamp-plus-ID ordering. Every
   query remains owner-isolated and every rendered payload inert.
8. One API-client reset clears access token, CSRF state, coalesced refresh/CSRF
   promises, and uses an epoch or equivalent so pre-reset requests cannot install
   later tokens. Invoke on logout success/failure, refresh failure/reset, and
   before a new authentication session where required.
9. Update tests, contracts, product/security/testing documentation, an ADR, and a
   sanitized manual-evaluation fixture for the semantic validator. Prompt,
   schema, model, and provider behavior remain unchanged.

## Acceptance criteria

### User Story domain and compatibility

- User Story create/get responses and project summaries expose priority with
  null mapped to `MEDIUM`; new writes use `MEDIUM` when omitted.
- V5 is the only migration change, works for fresh and V4→V5 databases, and
  preserves old-binary readability through a nullable column.
- Canonical `/user-stories` endpoints cover every approved operation; legacy
  `/requirements` endpoints remain thin parity adapters with no authorization
  divergence.
- Frontend uses canonical `/user-stories/:id`; legacy requirement URLs redirect
  without losing the target identifier.

### Generation sets

- All active-set consumers use one resolver and the same deterministic ordering.
- Failed runs do not supersede the active successful run; default case,
  coverage, traceability, review, and export reads are active-only.
- Historical successful runs remain read-only and report stable set number and
  state.
- Regeneration without `confirmSupersede` is rejected when the active set has
  revisions or review evidence; confirmed regeneration preserves all history.

### Test-data integrity and review

- Duplicate stripped/case-insensitive test-data names and dangling/ambiguous
  step references are rejected atomically for both generated and edited cases.
- Rename propagation and delete clear/reassign behavior are explicit in backend
  and UI tests.
- Only approved review transitions are offered and accepted. Reject/request
  changes requires comment; approve comment is optional. NEEDS_REVISION returns
  to IN_REVIEW only after an actual edit; APPROVED/REJECTED require explicit
  reopen with reason.
- Superseded or terminal cases reject edits/reviews. Expected-version conflicts
  return 409 and persisted versions are visible after flush.

### History, audit, and authentication

- Paged case revisions normalize legacy/current snapshots and the contextual UI
  combines revisions/reviews/audit with safe comparison rendering.
- Project audit filters are bounded, owner-isolated, and stably ordered by
  timestamp then ID.
- Auth reset covers logout success/failure, stale refresh completion, refresh
  failure, logout→login/register, and prevents an older request epoch from
  installing credentials.

### Evaluation and evidence

- A sanitized manual-generation fixture exercises the new semantic-validator
  data-reference rule; no live provider is called and prompt/schema/model remain
  unchanged.
- Backend, frontend, migration, authorization, active-set, review/history, auth,
  harness, and formatting/diff checks pass or local limitations are recorded
  exactly.
- The running demo and browser remain untouched; the Lead performs the final
  manual browser regression after Builder handoff.

## Expected interfaces and compatibility surface

- Persistence: `requirements.priority` through V5 only; `generation_run`, test
  case, revision, review, and audit tables reused.
- Backend domain/services: Requirement/UserStory aggregate and priority enum;
  shared active-generation-set resolver; shared test-data reference policy;
  entity-owned edit/review transitions; revision and audit query services.
- Backend web contracts: canonical `/user-stories` controller surface, legacy
  `/requirements` adapters, project count parity, run/set metadata,
  `confirmSupersede`, expected-version review/reopen bodies, paged revisions, and
  bounded audit filters.
- Frontend: canonical user-story routes and API client, active/historical set
  selector, integrity-safe test-data editor, controlled review/reopen/history UI,
  project audit timeline, and epoch-based auth reset.
- Evaluation/docs: one sanitized fixture plus canonical product, architecture,
  API, testing, security/threat, ADR, and this living evidence record.

## Exact affected-file inventory

Repository inspection completed after this plan was re-read. The implementation
is intentionally bounded to the following files; this inventory will be kept
current if verification proves that an additional seam is required.

- Persistence/domain/API: create
  `backend/src/main/resources/db/migration/V5__add_user_story_priority.sql`,
  `backend/src/main/java/com/testforge/requirement/domain/UserStoryPriority.java`,
  `backend/src/main/java/com/testforge/generation/application/ActiveGenerationSetResolver.java`,
  `backend/src/main/java/com/testforge/generation/domain/GenerationSetState.java`,
  and
  `backend/src/main/java/com/testforge/testcase/validation/TestDataReferencePolicy.java`;
  update Requirement entity/DTO/service/controller/repository, Project DTO and
  service, generation run DTO/repository/service/controller, TestCase
  entity/DTO/service/controller/repositories/revision entity, Traceability
  service/controller, Export service/controller, Audit DTO/repository/controller,
  and `GenerationResultValidator` under their existing module paths.
- Backend verification: update
  `backend/src/test/java/com/testforge/StageOneApiIntegrationTest.java`,
  `backend/src/test/java/com/testforge/WorkspaceMigrationIntegrationTest.java`,
  `backend/src/test/java/com/testforge/PostgreSqlMigrationIntegrationTest.java`,
  `backend/src/test/java/com/testforge/PostgreSqlWorkspaceUpgradeIntegrationTest.java`,
  generation-validator tests, and the narrowly scoped
  `backend/src/test/java/com/testforge/testcase/domain/TestCaseEntityTest.java`
  state-machine suite.
- Frontend/API/UI: update `frontend/src/types/api.ts`,
  `frontend/src/api/client.ts`, `frontend/src/auth/AuthContext.tsx`,
  `frontend/src/routes/routes.tsx`, `frontend/src/pages/LoginPage.tsx`,
  `frontend/src/pages/ProjectDashboardPage.tsx`,
  `frontend/src/pages/ProjectPage.tsx`, and
  `frontend/src/pages/RequirementPage.tsx`; add
  `frontend/src/routes/LegacyRequirementRedirect.tsx` so the exported route
  table remains fast-refresh compatible. Preserve the existing page module while
  presenting it as User Story in routes and visible copy, avoiding a duplicate
  frontend domain implementation.
- Frontend verification: update `frontend/src/api/client.test.ts`,
  `frontend/src/auth/AuthContext.test.tsx`, `frontend/src/App.test.tsx`,
  `frontend/src/Workflow.test.tsx`,
  `frontend/e2e/application-shell.spec.ts`, and
  `frontend/e2e/stage-one-workflow.spec.ts`.
- Evaluation/contracts: update `evals/manual-test-generation.jsonl` and, only
  if its static contract inventory requires the new enum/fixture rule,
  `scripts/validate-harness.py`; create
  `docs/decisions/0010-stage-one-user-story-workflow-integrity.md`; update
  `docs/decisions/README.md`, `docs/PRODUCT.md`, `docs/ARCHITECTURE.md`,
  `docs/API.md`, `docs/TESTING.md`, `SECURITY.md`, `docs/THREAT_MODEL.md`,
  `docs/product-specs/mvp-1-test-generation.md`, and this living plan.

## Ordered milestones

1. Re-read this materialized plan; inspect canonical source contracts and record
   the exact affected-file inventory without writing.
2. Add priority enum/domain/DTO behavior and forward-only V5 migration with
   fresh and upgrade migration coverage.
3. Add canonical User Story routes, thin compatibility adapters, count parity,
   canonical frontend route, and legacy redirect.
4. Implement one active-generation-set resolver, run history metadata,
   active-only defaults, historical read selector, and guarded regeneration.
5. Implement one test-data reference policy across generation validation and
   edits, then the dropdown/rename/delete UI behavior.
6. Centralize entity review/edit transitions, optimistic version bodies and
   flushing, controlled reopen, valid-action UI, and export eligibility.
7. Add paged normalized revisions, bounded/stable owner-scoped audit queries,
   and contextual history/audit UI with inert rendering.
8. Implement epoch-based API-client reset and authentication race tests.
9. Update the sanitized evaluation fixture, deterministic harness expectations,
   ADR, and canonical product/API/architecture/testing/security documentation.
10. Run targeted checks continuously, then the full supported wrapper and
    migration/database evidence. Record unavailable Postgres/Docker/browser
    evidence honestly and prepare the Lead's manual browser handoff.
11. Obtain post-build Architect `CONFORMS` or `BLOCK`, then independent Reviewer
    `APPROVE` or `BLOCK`; return every blocker to this same Builder.

## Test and verification plan

- Backend unit/integration: priority/null compatibility, User Story/legacy route
  parity, owner denial, active-set ordering/failure behavior, guarded
  regeneration, test-data normalization/references, review transition matrix,
  optimistic conflicts/flush, normalized revisions, audit filters/order.
- Migration: fresh H2 V1–V5, V4→V5 deterministic backfill, and equivalent
  PostgreSQL/Testcontainers paths when Docker is available.
- Frontend unit/workflow: canonical/legacy routing, project counts, run selector,
  data rename/delete/reference controls, review/reopen actions, combined history,
  inert rendering, and auth reset/races.
- Evaluation: update one synthetic manual fixture for the semantic-validator
  rule; run deterministic harness and relevant generation validation tests with
  no provider call.
- Commands: targeted Maven/Vitest checks while iterating; `mvn verify` and all
  frontend format/type/lint/test/build commands; `python
  scripts/validate-harness.py`; `git diff --check`; repository wrapper when the
  supported Java/Maven/Node/Docker/Bash environment is available.
- Browser: prepare stable local routes/data and a regression checklist; do not
  stop or manipulate the running demo/browser. Lead owns final manual execution.

## Security and privacy

- Preserve owner-rooted authorization for every canonical and compatibility
  route; never derive authorization from browser workspace context.
- Treat story text, generated output, snapshots, review comments/reasons, audit
  filters, and rendered history as untrusted.
- Use typed/parameterized queries, bounded page/filter inputs, structured JSON,
  and inert UI rendering; do not log or fixture customer data, keys, tokens,
  hidden prompts, or raw provider payloads.
- Auth reset must make stale refresh/CSRF promises incapable of restoring prior
  session credentials.
- Generated automation stays non-executing and all fixtures remain synthetic.

## Risks and mitigations

- Compatibility drift between canonical and legacy routes: share services and
  DTOs, then assert parity and identical authorization failures.
- Active-set divergence: prohibit ad hoc latest-run queries and route every
  consumer through one resolver with deterministic tie-breaking tests.
- Historical corruption: never delete old sets and reject writes to superseded
  output.
- Migration rollback risk: nullable additive column, deterministic backfill,
  null-as-default application reads, and no change to V1–V4.
- Data-reference partial writes: validate normalized names/references before
  mutating managed state and cover transaction rollback.
- Review-state bypass: entity-owned transition methods plus API/UI negative tests
  and expected-version enforcement.
- Audit leakage or stored content execution: owner predicates, bounded filters,
  structured snapshots, and text-only rendering tests.
- Auth races: monotonic epoch and explicit clearing of cached/coalesced promises.
- Scope breadth: keep each redundancy seam single and shared; avoid enterprise
  workflow, duplicate tables, and opportunistic refactors.

## Rollback

Before publication, revert only TF-003 files. V5 is forward-only and must never
be edited after application; rollback uses the prior binary, which ignores the
nullable priority column, while the new binary treats null as `MEDIUM`. Revert
canonical routing/UI/service behavior without removing stored historical runs,
revisions, reviews, or audit evidence. After publication, use a normal revert
commit rather than history rewriting and keep V5 applied.

## Definition of done

All acceptance criteria and redundancy seams are implemented with targeted and
full deterministic evidence; exact files and compatibility effects are recorded;
fresh/upgrade migration, transition, authorization, active-set, history, auth,
and semantic-validator coverage pass; no live provider or generated automation
runs; documentation and ADR are current; Builder work does not manipulate the
running demo/browser; post-build Architect and Reviewer gates have no blockers;
and the Lead decides local implementation completion.

## Post-implementation boundaries

- Final manual browser regression completed as recorded below.
- Local demo/browser shutdown after the Lead decides it is safe.
- Any staging, commit, push, PR, exact-SHA seven-job CI verification,
  publication, merge, deployment, branch-protection, or external-system action.

## Actual evidence

First-write evidence: this active plan was materialized from the approved
Architect handoff before any implementation edit.

- Pre-review backend Java 21/Maven 3.9.16 verification on 2026-08-04:
  `mvn verify -Dspring-boot.repackage.skip=true` passed compilation, 39 tests
  with zero failures/errors and two Docker-dependent PostgreSQL skips, Spotless,
  SpotBugs with zero findings, and JaCoCo at 72.63% branch coverage (329/453;
  required minimum 70%), 90.79% instruction coverage, and 90.87% line coverage.
  A focused Stage One integration rerun passed all 8 tests after adding the
  owner-scoped actor audit filter.
- Pre-review frontend verification passed `npm run format:check`, `npm run typecheck`,
  `npm run lint`, `npm run test:coverage`, and `npm run build`. Vitest passed all
  18 tests with 86.16% statements, 68.47% branches, 81.09% functions, and
  87.29% lines; all configured thresholds were met. The audit workflow has a
  bounded 20-second timeout and the larger generation/history workflow retains
  its bounded 30-second timeout because instrumentation takes longer while
  preserving all assertions.
- Lead manual regression findings were remediated before the full reruns:
  visible project/dashboard/login/workflow copy and counts now use canonical
  User Story terminology; project audit provides bounded paging plus entity,
  actor, action, and time filters; review/reopen decisions collect actual
  evidence and submit the selected version; revision history offers a selectable
  field-by-field comparison; and registration, criterion, and generation-count
  grammar is deterministic. Focused frontend tests passed 10/10 before the full
  18-test coverage run. The later packaged-runtime regression is recorded in the
  final evidence below.
- Deterministic evaluation evidence passed `python scripts/validate-harness.py`:
  3 specialist profiles, 6 skills, 6 blocking manual fixtures, and 3
  non-blocking automation-roadmap fixtures. No live provider was called; the
  prompt, provider schema, model selection, and runtime provider behavior were
  not changed.
- Migration evidence passed fresh H2 V1–V5 and V4→V5 priority-backfill tests.
  The equivalent PostgreSQL Testcontainers suites were discovered and skipped
  because Docker is unavailable in this environment.
- `git diff --check` passed with only Windows line-ending conversion notices.
  The working tree remains on `main`, is intentionally unstaged/uncommitted, and
  no publication action was taken. The implementation diff spans persistence,
  canonical/compatibility APIs, active-set and review/history services,
  frontend workflows/auth reset, tests/evaluation, ADR, and canonical docs.
- The Lead subsequently recycled the task-owned runtime, and the exact
  `.\scripts\verify.ps1` wrapper passed. The Lead's repeat manual browser
  regression also passed against the newly packaged runtime.
- Post-build Architect review returned `CONFORMS`. Independent Reviewer review
  returned `BLOCK` because `GenerationService.generate()` bypasses the
  protected-set confirmation guard and login/register completions are not
  epoch-conditional for both token and React user installation. The same sole
  Builder applied minimal remediation and the targeted/full evidence below
  passed. The plan remained active at that review stage.
- Reviewer remediation evidence on 2026-08-04 is fully deterministic and did
  not call a live provider or task-owned runtime. Focused
  `StageOneApiIntegrationTest` passed 8/8 and now covers initial generation,
  protected canonical and compatibility generate/regenerate aliases, confirmed
  supersession, a later failed run preserving the active successful set, and an
  idempotent retry returning before the new-operation confirmation guard.
  Focused auth/API-client tests passed 9/9 and include delayed logout-during-login
  plus overlapping login/register responses, asserting both React user state
  and the installed Authorization token.
- The full post-remediation backend
  `mvn verify -Dspring-boot.repackage.skip=true` passed 39 tests with zero
  failures/errors and two Docker-dependent skips, Spotless, SpotBugs with zero
  findings, and JaCoCo at 73.73% branch coverage (334/453), 91.19% instruction
  coverage, and 91.28% line coverage. Full frontend format, typecheck, lint,
  coverage, and production build gates passed; Vitest passed 20/20 with 86.29%
  statements, 68.65% branches, 81.18% functions, and 87.39% lines. The full
  coverage run required the existing sort/filter workflow test to use the same
  bounded 20-second instrumented-test allowance as the audit workflow; its
  assertions were unchanged. The deterministic harness passed with 3 profiles,
  6 skills, 6 blocking manual fixtures, and 3 roadmap fixtures, and
  `git diff --check` passed with only Windows line-ending notices.
- Repeat Architect review returned `CONFORMS`. Reviewer repeat review returned
  `BLOCK` because AuthProvider's mount-time refresh callback treated a stale null
  result as a current signed-out result and could clear the React user installed
  by a newer login/register operation. The generation guard and explicit
  login/register epoch-installation findings remain remediated. The same sole
  Builder applied the narrow mount-restoration fix before the final passing
  read-only gates recorded below.
- Final auth-race remediation distinguishes current session-restoration outcomes
  from stale completions before changing mount-time React user/loading state;
  newer login/register completions also finish loading only after their epoch-
  conditional token install succeeds. Focused auth/API-client tests passed
  10/10, including a delayed initial refresh that completes after a newer login
  while the newer React user and Authorization header remain installed. The
  full frontend gates passed format, typecheck, lint, 21/21 coverage tests, and
  production build with 86.28% statements, 69.29% branches, 81.37% functions,
  and 87.37% lines. The unchanged backend non-repackage verification passed 39
  tests with zero failures/errors and two Docker-dependent skips, Spotless,
  SpotBugs, and all JaCoCo thresholds. The deterministic harness passed; final
  `git diff --check` passed with only Windows line-ending notices.
- Final closeout evidence on the final diff: the Lead's exact
  `.\scripts\verify.ps1` run passed the deterministic harness; backend
  compilation, Spring Boot packaging, Spotless, SpotBugs, and JaCoCo checks; 39
  backend tests with zero failures/errors and two local Docker-dependent skips;
  and frontend formatting, typecheck, lint, 21/21 coverage tests, and production
  build. Final reported coverage remained 73.73% backend branches (334/453),
  91.19% backend instructions, 91.28% backend lines, 86.28% frontend statements,
  69.29% frontend branches, 81.37% frontend functions, and 87.37% frontend lines.
- Against the rebuilt application, the Lead's immediate logout→login regression
  passed and the browser console remained empty. A manual protected-set
  generation request returned HTTP 409 with
  `supersede_confirmation_required`, confirming the shared supersession guard in
  the packaged runtime.
- Final read-only gates passed: Architect `CONFORMS`; Reviewer `APPROVE` with no
  findings. The Lead then declared implementation complete. No staging, commit,
  push, PR, merge, deployment, branch-protection change, or publication occurred
  during Builder closeout.

## Deviations

No product, persistence, authorization, generation-contract, or publication
scope deviation. Local Builder verification used the existing portable Java
21/Maven 3.9.16 runtime because the default host Java is 11 and Maven is not
globally installed. The earlier JAR-lock limitation was resolved by the
Lead-owned runtime recycle and exact wrapper pass. Docker-backed PostgreSQL
evidence remains unavailable locally.

## Residual risks

Docker-backed PostgreSQL migration evidence remains unavailable locally because
Docker is not available in this environment. Supported exact-SHA CI must supply
that PostgreSQL/Testcontainers evidence before publication.
