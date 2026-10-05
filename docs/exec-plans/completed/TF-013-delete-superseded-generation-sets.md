# TF-013: Delete superseded generation sets

## Status

Active. Architect-approved handoff materialized by the assigned sole Builder on
2026-08-12. This plan was re-read before implementation.

## Objective

Permit an owner to permanently purge the generated content of an eligible
superseded generation set while retaining a hidden generation-run tombstone for
stable set numbering and bounded audit evidence.

## Approved contract

- Deletion is owner-only, explicitly confirmed, and limited to a nondeleted,
  server-derived `COMPLETED` `SUPERSEDED` run with zero case reviews and zero
  revisions. It never deletes active, pending, failed, rejected, reviewed, or
  edited sets.
- `DELETE /api/v1/generation-runs/{runId}?confirm=true` returns `204`. Missing
  or false confirmation returns `409 generation_set_deletion_confirmation_required`;
  an active set returns `409 active_generation_set_cannot_be_deleted`; a
  noncompleted run returns `409 generation_attempt_not_deletable`; human
  evidence returns `409 generation_set_has_human_evidence`; inaccessible or
  already deleted runs return `404`.
- `GenerationRunResponse` adds the server-derived `deletable` boolean.
- V8 adds nullable `deleted_at` and `deleted_by` (foreign key) to generation
  runs plus a paired-null constraint. Do not edit V7 or any applied migration.
- In one transaction: lock the owner-scoped run, lock its owned requirement,
  re-resolve active status, lock bounded cases, recheck human evidence, delete
  the full case child graph via FK/bulk cases, delete criterion snapshots,
  tombstone the run, and write bounded audit metadata.
- The tombstone keeps its set number. Stable numbering counts tombstoned
  completed runs. Active resolution, standard reads/history/get, explicit case,
  coverage, traceability, and export must hide tombstones. Reusing a tombstone's
  same idempotency hash returns `409 generation_set_deleted` without a provider
  call.
- Audit metadata contains only `setNumber` and `purgedCaseCount`.
- UI offers deletion only when `deletable`. An accessible responsive confirmation
  identifies Set N, permanent purge, the stable reserved number, and the audit
  fact. Cancel is inert. Success clears selected URL/case parameters,
  invalidates requirement queries, presents an active fallback notice, repairs
  history pagination, and guards a selected tombstone as `404`.

## Approved implementation surface

- New V8 migration.
- `GenerationRunEntity`, generation-run repository, snapshot repository,
  test-case/review/revision repositories, `ActiveGenerationSetResolver`,
  `GenerationTransactionService`, `GenerationService`, generation controller,
  `GenerationRunResponse`, and `AuditMetadata`.
- Frontend API types and `RequirementPage` only; use the existing responsive
  dialog infrastructure.
- Tests: `GenerationServiceTest`, `StageOneApiIntegrationTest`, three migration
  suites, focused repository/domain tests when needed, `Workflow.test`, and
  the stage-one stub e2e two-set flow.
- Canonical documentation: PRODUCT, MVP specification, ARCHITECTURE, API,
  TESTING, SECURITY, THREAT_MODEL, and domain model.

## Non-goals and compatibility

- Do not change prompt, provider, model, schema/result contracts, evaluation
  tuple, fixtures, authentication model, or generated-automation boundary.
- Evaluation impact is none; the existing generation tuple and fixtures remain
  unchanged.
- Before use, rollback is source-compatible. After a purge, restore is
  backup-only; preferred remediation is roll-forward. Tombstones deliberately
  reserve historical set numbers.

## Required evidence

Prove full V7 graph purge; protection/error mapping; cross-owner rejection;
bounded audit metadata; stable number gap; active/read/history/export/case/
coverage/traceability hiding; idempotency reuse with zero provider calls;
PostgreSQL race safety; and the accessible responsive UI flow. Run deterministic
checks without installs, live-provider calls, generated automation execution,
staging, commit, push, or external mutation. Preserve dirty TF-009 through
TF-012 and unrelated work.

## Verification evidence

- PASS — `python -B scripts/validate-harness.py`, frontend format/type/lint,
  and `git diff --check` after the initial implementation.
- PASS — focused `GenerationServiceTest` additions assert that missing
  confirmation prevents all provider interaction and that a confirmed deletion
  delegates the stable set number without provider interaction; Java 21 Maven
  execution remains pending the Lead's Docker run.
- DEVIATION — the local frontend `npm run test:coverage` command exceeded the
  120-second command limit after Vitest started and produced no test result;
  no pass is claimed.
- PASS — frontend `npm run build` after the deletion UI/type change.
- Pending Java-21/PostgreSQL integration evidence: deletion HTTP/error matrix,
  full V7 descendant graph purge, audit metadata, tombstone visibility,
  idempotency/provider-zero, and concurrent deletion/review race.
- SAFE JAVA-21 RUN BLOCKED (remediated): `GenerationServiceTest` and
  `WorkspaceMigrationIntegrationTest` ran without Testcontainers or Docker
  socket and reported 9 tests, 0 failures, 6 errors. H2 2.3 rejected V8's
  combined `ALTER TABLE ... ADD COLUMN` syntax on all workspace migration paths;
  V8 now uses two portable statements. The two new service tests also triggered
  Mockito strict unnecessary shared setup stubs; only their unrelated shared
  stubs are now lenient. A fresh safe rerun is required.
- SAFE JAVA-21 RERUN BLOCKED (remediated): `GenerationServiceTest` passed, but
  all four `WorkspaceMigrationIntegrationTest` paths rejected H2 2.3's
  unsupported PostgreSQL partial-index predicate in V8. The visible-history
  index is now portable `(requirement_id, deleted_at, started_at DESC)` without
  a predicate; a fresh safe rerun is required.
- PASS — third safe Java-21 run, without Testcontainers or Docker socket:
  `GenerationServiceTest` plus `WorkspaceMigrationIntegrationTest` completed
  `BUILD SUCCESS` with 9 tests, zero failures, and zero errors. All H2 fresh
  and upgrade migration paths applied through V8.
- ADDED — H2 `StageOneApiIntegrationTest` deletion path covers active-set
  protection, missing confirmation, cross-owner access, confirmed deletion,
  V7 case and snapshot purge, paired tombstone state, hidden get/case/history/
  selected case/coverage/traceability/export reads, stable Set-2 gap, and
  deleted idempotency reuse with a zero provider-call spy assertion. Its safe
  Java-21 execution is pending.
- ADDED — frontend workflow history test reaches an eligible superseded Set 2,
  opens the responsive Delete Set 2 confirmation, and proves Cancel closes it
  without a delete request. Its Vitest execution remains pending because the
  local targeted process stalled without output.
- PASS — broader safe Java-21 process exceeded the tool timeout but persisted
  Surefire results: `GenerationServiceTest` 5/5, `WorkspaceMigrationIntegrationTest`
  4/4, and the TF-013 `StageOneApiIntegrationTest`
  `deletesEligibleSupersededGenerationSetWithStableNumberAndNoProviderReuse`
  passed in 0.262s. The 10 observed focused TF-013 tests had zero failures/errors;
  the later fresh focused run is the authoritative 11/11 count.
- RESIDUAL OUTSIDE TF-013 — the 26-test StageOne suite retained three failures
  and two errors: duplicate setup-step inserts in
  `cascadesSetupRowsWhenTheirOwningCaseIsDeleted` and
  `preservesOrClearsSetupStepsWithoutLeavingDanglingDataReferences`, plus three
  later setup paths blocked by auth rate-limit `429`. These are existing dirty
  TF-009/API-drift failures, not observed in the TF-013 deletion test; left
  untouched.

## Review and completion

Architect `CONFORMS` and independent Reviewer `APPROVE` were received before
the Lead's 2026-08-13 local implementation-completion decision. Publication is
separate authorization and has not been performed.

## Stop handoff — 2026-08-12

Implementation is intentionally paused at the user's direction. The Lead stopped
the synthetic Docker stack while preserving its volumes. No live provider call,
dependency installation, generated-automation execution, staging, commit, push,
or external mutation occurred.

### Implemented on the working tree

- Added forward-only `V8__add_generation_run_tombstones.sql`: nullable
  `deleted_at` and `deleted_by`, paired-null constraint, and a portable visible
  history index. The migration was revised from a combined `ADD COLUMN` and a
  PostgreSQL partial index to H2/PostgreSQL-compatible individual `ALTER`
  statements and an ordinary composite index.
- Added run tombstone state and server-derived `deletable`; owner-scoped,
  confirmed deletion locks and rechecks the run, requirement, and case graph,
  removes cases and criterion snapshots, records bounded purge metadata
  (`setNumber`, `purgedCaseCount` only), and retains a hidden tombstone that
  reserves its completed set number.
- Added `DELETE /api/v1/generation-runs/{runId}?confirm=true`, idempotency
  tombstone rejection without provider use, owner/active/noncompleted/human
  evidence protection codes, and tombstone exclusion from generation reads,
  selected case reads, selected case collections, coverage, traceability, and
  export.
- Added the typed frontend `deletable` flag and an accessible `Delete set`
  action with a confirmation dialog that explains permanent purge, reserved
  numbering, and bounded audit retention. Successful deletion clears selected
  generation/case/history URL state, invalidates requirement queries, and shows
  an active-set fallback notice.
- Added in-progress stale selected-set recovery: a selected generation-set
  `404` from cases, coverage, or traceability clears `generationRunId` and
  returns the browser to the active set. This latest UI change has not yet
  passed lint; see below.
- Updated canonical PRODUCT, MVP, ARCHITECTURE, API, TESTING, SECURITY,
  THREAT_MODEL, and domain-model documentation with the deletion contract,
  trust boundary, and backup-only post-purge recovery.

### Tests constructed

- `GenerationServiceTest`: missing confirmation prevents provider interaction;
  confirmed deletion delegates the stable set number with no provider use.
- `WorkspaceMigrationIntegrationTest`, `PostgreSqlMigrationIntegrationTest`,
  and `PostgreSqlWorkspaceUpgradeIntegrationTest`: migration chain now expects
  V1 through V8 and asserts the tombstone fields/constraint path.
- `StageOneApiIntegrationTest` deletion flow: active protection, confirmation,
  cross-owner `404`, confirmed delete, V7 case and snapshot purge, paired
  tombstone, hidden get/case/history/collection/coverage/traceability/export,
  stable Set-2 gap, and deleted idempotency reuse with zero provider calls.
  The pre-existing immutable-generation scenario was extended to create both a
  review and a revision and assert `generation_set_has_human_evidence`.
- `Workflow.test`: an eligible superseded Set 2 reaches the dialog and Cancel
  closes it without mutation. Its page-flow assertion was corrected so it does
  not click an already-disabled next-page control.
- `frontend/e2e/stage-one-workflow.spec.ts`: added a two-set browser flow that
  generates Set 1, regenerates Set 2, confirms `Delete set`, verifies Set 1 is
  hidden, Set 2 remains visible, and selected-set URL state is cleared. This
  spec has not yet been formatted or executed.

### Exact verification evidence and current blockers

- PASS — `python -B scripts/validate-harness.py`, frontend format/type/lint,
  frontend build, and `git diff --check` at the prior TF-013 implementation
  checkpoint (before the final stale-URL and browser-spec additions).
- PASS — first safe Java 21 run was remediated: it initially reported 9 tests,
  0 failures, 6 errors because H2 2.3 rejects combined V8 `ALTER TABLE ... ADD
  COLUMN` syntax and Mockito strict stubbing found unnecessary shared setup.
  V8 was split and only unrelated common stubs were made lenient.
- PASS — second safe Java 21 run was remediated: `GenerationServiceTest`
  passed, while four H2 migration paths rejected V8's partial-index predicate.
  The index is now portable and non-partial.
- PASS — third safe Java 21 run:
  `GenerationServiceTest,WorkspaceMigrationIntegrationTest`, 9 tests, zero
  failures/errors, `BUILD SUCCESS`; all H2 fresh/upgrade paths applied through
  V8.
- PASS — broader safe Java 21 process exceeded the command timeout but
  persisted Surefire results: `GenerationServiceTest` 5/5 and
  `WorkspaceMigrationIntegrationTest` 4/4 passed; the TF-013 StageOne test
  `deletesEligibleSupersededGenerationSetWithStableNumberAndNoProviderReuse`
  passed in 0.262s. The focused TF-013 evidence has zero observed failures or
  errors.
- RESIDUAL OUTSIDE TF-013 — the 26-test StageOne suite has three failures and
  two errors from dirty TF-009/API drift: duplicate setup-step inserts in
  `cascadesSetupRowsWhenTheirOwningCaseIsDeleted` and
  `preservesOrClearsSetupStepsWithoutLeavingDanglingDataReferences`, and three
  later setup paths receive authentication rate-limit `429`. Do not change
  these without a new scoped assignment.
- ENVIRONMENT LIMITATION — PostgreSQL/Testcontainers race verification was not
  run. The attempted combined Java/Postgres approach required mounting the
  Docker socket into the Maven container, which grants broad daemon control and
  was correctly rejected. Do not work around that restriction; use only an
  existing safe repository-supported PostgreSQL mechanism if one is later
  authorized and available.
- INCOMPLETE — previous `npm run test:coverage` exceeded the 120-second limit
  after Vitest started and produced no result; an attempted targeted Vitest run
  also stalled without output. Neither is a pass claim.
- CURRENT LOCAL BLOCKER — after the last frontend edits,
  `npm run format:check; npm run typecheck; npm run lint` produced successful
  typecheck but failed overall: Prettier reported formatting in
  `e2e/stage-one-workflow.spec.ts`, and ESLint reported
  `react-hooks/set-state-in-effect` at `RequirementPage.tsx:295` for the
  synchronous `setNotice` in selected-tombstone recovery. No remediation was
  made after this result because the user stopped work.

### Ordered restart steps

1. Confirm with the Lead that this Builder remains the sole writer for TF-013
   and that no other writer changed overlapping frontend, backend, test, or
   documentation files while paused.
2. Repair the selected-tombstone recovery without synchronous state updates in
   an effect (for example, derive or queue the fallback notice through an
   existing mutation/error boundary), then format the new e2e spec. Preserve
   the authoritative 404 guard and active-set URL fallback.
3. Re-run frontend format, typecheck, lint, focused Workflow test, coverage,
   build, harness, and `git diff --check`; record exact commands/results,
   including any continued Vitest timeout.
4. Run the safe Java 21 focused suite again after the added human-evidence
   assertion: `GenerationServiceTest,WorkspaceMigrationIntegrationTest`, then
   the TF-013 StageOne deletion/protection tests. Keep the full StageOne
   TF-009/API residual inventory separate.
5. If the Lead safely rebuilds the synthetic stack, run the default stub-backed
   two-set Playwright deletion flow only; do not call a live provider. Record
   whether its current stub environment exposes the new backend image.
6. Seek a safe, already-supported PostgreSQL/Testcontainers mechanism for the
   required delete-versus-review race; otherwise retain the Docker-socket
   limitation as an explicit verification deviation.
7. Update this active plan with fresh evidence, then request Architect
   conformance and independent Reviewer approval. Do not move the plan to
   `completed/` until both approve and the Lead makes the implementation
   completion decision.

## Resume evidence — 2026-08-13

- The Lead confirmed no other writer changed TF-013 production, test, or
  documentation files during the stop. TF-013 resumed as the same sole writer;
  TF-014 generation-contract work remains deliberately untouched.
- REMEDIATED — selected tombstone recovery in `RequirementPage` no longer calls
  React state synchronously from an effect. The effect only repairs the URL;
  the fallback notice is derived from a dismissible URL flag. Selected case,
  coverage, and traceability reads also do not retry an authoritative selected
  set `404`, so recovery is prompt rather than transport-retry delayed.
- ADDED — `Workflow.test` proves a canonical selected-set tombstone `404`
  clears `generationRunId`, sets the recovery notice flag, and returns to an
  active-set page. It also waits for the responsive dialog transition after
  Cancel before asserting the history controls.
- PASS — `npm run test -- src/Workflow.test.tsx`: 1 file, 9 tests passed,
  51.59 seconds.
- PASS — `npm run test:coverage`: 7 files, 38 tests passed, 86.11 seconds;
  statements 83.70%, branches 73.63%, functions 77.13%, lines 85.29%.
- PASS — `npm run format:check`, `npm run typecheck`, `npm run lint`, and
  `npm run build` after the recovery remediation.
- PASS — `python -B scripts/validate-harness.py` and `git diff --check` after
  the recovery remediation. The harness reported 3 specialist profiles, 6
  skills, 9 blocking manual fixtures, and 3 non-blocking automation fixtures.
- REMAINING — safe Java 21 focused rerun must include the newly added reviewed
  and revised human-evidence deletion protection; default stub-backed Playwright
  two-set deletion flow awaits a safely rebuilt synthetic stack. PostgreSQL
  delete-versus-review race remains environment-limited: do not mount Docker
  socket into Maven/Testcontainers.
- PASS — the Lead's safe Java 21 focused command exceeded its tool timeout but
  completed. Fresh Surefire reports show `GenerationServiceTest` 5/5,
  `WorkspaceMigrationIntegrationTest` 4/4, and the two TF-013 StageOne deletion
  and reviewed/revised-evidence protection methods 2/2: 11 tests total, zero
  failures and zero errors. It used no Testcontainers and no Docker socket.
- PENDING — the Lead is rebuilding the synthetic stub stack and will run the
  default non-live Playwright suite against this snapshot. Builder feature
  writes are paused until that result is available.
- PASS — after a safe rebuild with the current TF-013 images, all synthetic
  services were healthy and the default non-live stub-backed Playwright suite
  passed 6/6 in 5.7 seconds. This includes
  `owner deletes an eligible superseded set through the confirmed browser flow`.
  No live provider was contacted.

### Local readiness

The deterministic TF-013 implementation checks currently support Architect
conformance review: frontend format/type/lint/build, Workflow 9/9, frontend
coverage 38/38, deterministic harness, diff check, safe Java 21 focused tests
11/11, and default stub-backed e2e 6/6 all passed. This is not publication
readiness and no exact-SHA CI evidence exists. The only remaining verification
deviation is the PostgreSQL/Testcontainers delete-versus-review race: a Docker
socket mount was correctly rejected as overly privileged, and no safe existing
repository mechanism was available. The behavior is otherwise protected by the
transactional owner/run/requirement/case locks and H2-focused tests; retain this
limitation for Architect and Reviewer review.

### Final local evidence and residual risks

- FINAL LOCAL EVIDENCE — frontend format/type/lint/build; Workflow 9/9;
  frontend coverage 38/38; deterministic harness; diff check; safe Java 21
  focused tests 11/11; and default non-live stub-backed e2e 6/6 passed. No
  live provider was called, no dependencies were installed, and no generated
  automation was executed.
- RESIDUAL — the PostgreSQL delete-versus-review race is repository-supported
  and freshly Java 21 compiled, but its Testcontainers execution is locally
  unrun because Docker-socket mounting is prohibited. It remains pending on a
  supported CI/Testcontainers environment; this does not claim exact-SHA CI or
  publication readiness.
- RESIDUAL OUTSIDE TF-013 — the broader dirty StageOne suite retains TF-009/API
  drift failures: duplicate setup-step inserts and later authentication
  rate-limit `429` setup paths. They are not changed by this plan.
- NO PUBLICATION — no staging, commit, push, PR, deployment, exact-SHA CI, or
  provider call occurred under TF-013 authority.

## Architect conformance remediation — 2026-08-13

- Restored the exact tombstone idempotency conflict to
  `generation_set_deleted`; the API documentation now identifies it as the
  explicit exception to normal idempotency replay and preserves the
  provider-zero rule.
- Extended the H2 StageOne deletion scenario with repeat-delete `404`, active
  and failed-run `deletable`/noncompleted protection assertions, clean
  superseded eligibility, and a bounded `PURGED` audit assertion. It checks the
  exact owner, project, generation-run entity scope and only
  `{setNumber,purgedCaseCount}` metadata, without case bodies.
- Replaced the initial non-probative PostgreSQL concurrency test with an
  orchestrated real-review transaction. The review invokes and flushes the real
  service path while the old set is active, pauses before commit, a successor is
  created, and delete is started and time-bounded while it waits on the case
  lock. Releasing the review commits human evidence; delete then returns
  `409 generation_set_has_human_evidence`, leaves the run untombstoned, and
  retains the case/review evidence. The test is
  `@Testcontainers(disabledWithoutDocker = true)` alongside the existing
  PostgreSQL suite and is intended for supported CI; it remains locally unrun
  because Docker-socket mounting is prohibited.
- Strengthened the PostgreSQL lock proof: before the paused review is released,
  the test now bounded-polls `pg_stat_activity` joined to `pg_locks` for an
  active backend waiting on an ungranted lock while executing the
  `testforge.test_cases` lock query in the current database. The brief future
  timeout is retained only as a hang guard; the PostgreSQL lock observation is
  the serialization proof. This remains CI/Testcontainers pending rather than
  a local Docker-socket run.
- PASS — after the database-lock observation change, the Lead's final safe Java
  21 compile/focused command completed after its tool timeout. Fresh Surefire
  reports remain 11/11 with zero failures and zero errors; the freshly compiled
  `PostgreSqlMigrationIntegrationTest.class` timestamp is 02:59:02.
- Corrected the obsolete ``15 focused`` claim: the earlier observed subtotal was
  10 and the fresh safe Java 21 focused result is the authoritative 11/11.
- PASS — after the remediation, `python -B scripts/validate-harness.py` and
  `git diff --check` passed from the repository root. Local backend compilation
  remains unavailable on this workstation (`java -version` is 11.0.29 and Maven
  is not on PATH); the safe Java 21 focused rerun has been requested from the
  Lead. No local Docker/Testcontainers workaround was attempted.
- PASS — the Lead's remediated safe Java 21 focused command exceeded the tool
  boundary but completed. Fresh Surefire reports: `GenerationServiceTest` 5/5,
  `WorkspaceMigrationIntegrationTest` 4/4, and the two TF-013 StageOne methods
  2/2, for 11/11 total with zero failures and zero errors. The PostgreSQL
  Testcontainers concurrency test remains intentionally unrun locally under
  the no-Docker-socket restriction.
- PASS — after the PostgreSQL race-test choreography replacement, the Lead's
  safe Java 21 compile/focused run again exceeded the tool boundary but
  completed with fresh Surefire 11/11, zero failures and zero errors. The
  compiled `PostgreSqlMigrationIntegrationTest.class` was freshly produced at
  02:52:52, confirming the revised Testcontainers concurrency test compiles.
  It is still unrun locally by design because no Docker socket is permitted.
