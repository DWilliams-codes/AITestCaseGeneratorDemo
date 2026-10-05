# TF-012: Actionable manual-test steps

## Status

Active. Architect-approved handoff materialized by the assigned sole Builder on
2026-08-12; implementation has not begun until this plan is re-read.

## Objective

Strengthen the manual-test generation contract so generated cases contain a
complete reproducible path with discrete tester interactions and observable
evidence, rather than criterion-label placeholders or compressed workflows.

## Approved scope and non-goals

- Change only the prompt/semantic-validation/evaluation behavior described here.
- Release tuple: prompt `manual-test-v4`; result `manual-test-result-v2`
  unchanged; schema `manual-test-schema-v3` unchanged; semantic validator
  `manual-test-validator-v4`; provider adapter/model configuration unchanged;
  fixture format `3` unchanged.
- Add `test-generation-v4.txt`; retain historical prompt v3 unchanged.
- Do not change JSON schema, result records, DTOs, migrations, production
  frontend components/types, provider/model configuration, authorization, or
  automation execution boundaries.

## Acceptance criteria

1. The v4 prompt preserves v3 safety and requires each manual case to describe
   a complete reproducible path, individual tester interactions, actionable
   setup/state preparation, and evidence operations/observations in test steps.
   It prohibits criterion placeholders and whole-workflow compression.
2. The v4 semantic validator rejects generic actions including `Submit input for
   AC-2`, `Verify AC-2`, `Test the feature`, and `Perform the workflow`, while
   accepting concrete atomic one-step cases. Focused tests prove both outcomes.
3. Contract-version metadata and provider prompt binding select v4 without
   changing result/schema/adapter/model contracts; API assertions cover the
   updated tuple.
4. The fake provider and browser-test external Responses stub return actionable
   steps. Browser checks assert generic `Submit input for AC-#` output is absent.
5. The eight existing sanitized manual fixtures pin the v4 tuple. Add synthetic
   `Manual-009` for a pending-$500 requester path and update fixture count,
   tuple, and oracle rules in the rubric and deterministic harness.
6. Canonical product, architecture, testing, pipeline, security, and threat
   documentation state the actionable-step contract without exposing hidden
   prompts, secrets, or provider payloads.

## Contracts inspected

- `AGENTS.md`; `docs/PRODUCT.md`; `docs/product-specs/mvp-1-test-generation.md`;
  `docs/ARCHITECTURE.md`; `docs/API.md`; `docs/TESTING.md`; `evals/RUBRIC.md`;
  `SECURITY.md`; `docs/THREAT_MODEL.md`; `PLANS.md`; `docs/PLANS.md`; and
  `docs/exec-plans/README.md`.
- Generated content remains untrusted; exact schema and semantic validation
  precede persistence. Provider inputs stay minimized and generated automation
  remains non-executing.

## Implementation approach

1. Locate v3 prompt/version registry, semantic validation, provider test double,
   external browser stub, fixtures, harness and canonical documents.
2. Add the v4 prompt and bind the version registry/provider test contracts.
3. Narrow the semantic generic-action rejection and cover positive/negative
   behavior using deterministic tests.
4. Update synthetic evaluation fixtures and deterministic oracles, then update
   the named canonical documents.
5. Run harness first, focused backend tests, backend/frontend checks, default
   stub-backed browser tests, wrapper, and `git diff --check`; record observed
   results below. This deterministic validation makes no dependency install,
   live-provider call, generated-automation execution, staging, commit, push,
   or external mutation; the separately authorized live protocol is below.

## Compatibility, security, and rollback

No persisted/API/schema/provider-model/migration contract changes are planned.
Existing stored v3 outputs remain readable under unchanged result/schema types.
Rollback is a source revert to the v3 prompt binding and prior validator version;
no data backfill or dual-write is needed. The change tightens untrusted provider
output validation, does not expose prompts to the browser, and uses only
synthetic fixture data. Residual risk: deterministic mocked/stub evidence does
not establish live-provider adherence; an absent `OPENAI_API_KEY` leaves any
separately authorized sanitized candidate run to a later stage.

## Evaluation impact

`$ai-generation-evals` applies: prompt and semantic-validator behavior change.
The tuple above and fixture IDs will be recorded with local evidence. Narrow
`$testforge-evaluation` checks validate exact fixture keys/enums/mappings and
the deterministic harness. One separately authorized live candidate uses the
synthetic `manual-009-actionable-pending-transaction` fixture; it is currently
blocked only by absent local `OPENAI_API_KEY`, not by lack of authorization.

## Authorized live candidate protocol and evidence

The excluded `@live-generation` browser test submits only the Manual-009
synthetic requester/pending-$500 scenario through the production provider path.
It must run only after deterministic checks pass, with the default Compose stack
(never the Responses-stub override), a locally configured ignored key, and one
idempotency key. `npm run e2e:live` first requires the explicit
`TESTFORGE_LIVE_GENERATION_AUTHORIZED=true` flag and
`PLAYWRIGHT_BASE_URL=http://localhost:3000`; it validates a nonblank key entry
in the root ignored `.env`, rejects a process-environment key override, and
selectively checks the effective single running
`testforge-ai` backend. It refuses a Compose source containing
`docker-compose.e2e.yml`, requires the label to contain exactly the canonical
repository `docker-compose.yml` path (case-insensitive, with no lookalike,
other-directory, or multiple source), and checks only boolean runtime
conditions (OpenAI provider, exact official `https://api.openai.com/v1` base
URL, pinned `gpt-5.6-sol` model, and key presence), never the key value or
complete container environment. Docker resolves from an optional absolute
`TESTFORGE_DOCKER_EXECUTABLE` or portable `docker` PATH command through
`execFileSync`, never a shell. The live spec itself fails before setup unless
the launcher sets `TESTFORGE_LIVE_GENERATION_MODE=true`, so a direct tagged
Playwright command cannot click Generate. The live describe sets Playwright
retries to zero and disables trace, screenshot, and video retention. It may
use the application's existing maximum of two provider attempts and must never
execute generated automation.

The test records a sanitized Playwright attachment containing only fixture ID,
generation-run ID, provider/model, release tuple, terminal status, generated
case count, application-owned generated case keys/titles, setup and procedure
actions with expected results, criterion mappings, and booleans for
authentication/setup readiness, exact-$500 entry, visible PENDING state,
same-requester approval denial, retained PENDING state, and AC-1/2/3 mappings.
The evaluation report must add the atomic-obligation evidence matrix, rubric
score, hard-failure status, and concise reviewer notes. It must not retain a
provider key, authorization header, raw provider envelope, hidden
prompt/reasoning, customer requirement, or unredacted provider error body. No
repeat paid attempt follows a failure without separate authorization.
The generated run must persist and return `openai-responses` and `gpt-5.6-sol`;
the live spec asserts both before retaining the sanitized attachment.

## Verification evidence

Implemented the approved scope without changing schema, result records, DTOs,
migrations, frontend production components/types, provider/model configuration,
or historical `test-generation-v3.txt`.

Changed TF-012 paths:

- `backend/src/main/resources/prompts/test-generation-v4.txt`
- `backend/src/main/java/com/testforge/generation/application/GenerationContractVersions.java`
- `backend/src/main/java/com/testforge/generation/provider/OpenAiTestGenerationProvider.java`
- `backend/src/main/java/com/testforge/generation/validation/GenerationResultValidator.java`
- focused generation/provider/API tests, the synthetic Responses stub, and the
  Stage 1 browser workflow assertion
- `frontend/scripts/live-generation-preflight.mjs` and its deterministic Node
  tests; `frontend/package.json` binds `e2e:live` to this preflight
- `evals/manual-test-generation.jsonl`, `evals/RUBRIC.md`, and
  `scripts/validate-harness.py`
- `docs/PRODUCT.md`, `docs/product-specs/mvp-1-test-generation.md`,
  `docs/ARCHITECTURE.md`, `docs/TESTING.md`,
  `docs/architecture/ai-generation-pipeline.md`, `SECURITY.md`, and
  `docs/THREAT_MODEL.md`

Observed local checks:

- PASS — `python -B scripts/validate-harness.py`: 3 specialist profiles, 6
  skills, 9 blocking manual fixtures, and 3 non-blocking roadmap fixtures;
  Manual-008's 94-obligation oracle and Manual-009 actionable-path oracle pass.
- PASS — `./scripts/verify.ps1 -HarnessOnly`: same deterministic harness.
- PASS — frontend `npm run format:check`, `npm run typecheck`, and `npm run lint`.
- PASS — frontend `npm run test:coverage`: 7 files and 38 tests; 84.66%
  statements, 74.71% branches, and 86.44% lines.
- PASS — frontend `npm run build`.
- PASS — `git diff --check`; Git emits a non-blocking CRLF-to-LF warning for
  `scripts/validate-harness.py`.
- SKIPPED/UNAVAILABLE — focused backend tests and `backend/mvn verify`: `mvn`
  is absent and host Java is 11 while the project requires Java 21. The full
  wrapper consequently stops after its passing harness with `Required command
  'mvn' was not found`; no installation was attempted.
- BLOCKED OUTSIDE TF-012 SCOPE — the Lead's corrected Docker Java-21
  `mvn -B verify` invocation completed nonzero because
  `WorkspaceMigrationIntegrationTest.migratesFreshDatabaseThroughWorkspaceFoundation`
  expects migration versions V1 through V6, while the pre-existing TF-009
  `V7__add_test_case_setup_steps.sql` is present. The first Docker mount attempt
  failed from path quoting before Maven; the corrected run reached the lifecycle
  and logged passing tests through
  `WorkspaceReconciliationConcurrencyIntegrationTest`. This migration-test
  drift is not changed by TF-012 and is left for its owning scope.
- PASS — default stub-backed `npm run e2e` with
  `PLAYWRIGHT_BASE_URL=http://localhost:3000`: 5 Playwright specs passed after
  the Lead rebuilt the full synthetic stack with the remediated TF-012 images.
  The final rerun completed in 5.6 seconds and exercised the updated actionable
  stub path. The first
  unconfigured invocation targeted the local Vite default (`127.0.0.1:5173`)
  and failed before generation; it is not the accepted stack result. Two
  TF-012 browser assertions were corrected during the rerun: repeated setup
  text is selected with `.first()`, and the empty first setup data-reference is
  no longer asserted to contain test data.
- PASS — after the second Architect remediation, frontend format, typecheck,
  lint, harness, and `git diff --check` pass again. The excluded live test now
  waits for the initial `/generate-test-cases` route exactly and attaches only
  sanitized application-owned Manual-009 case/step/result/mapping evidence and
  per-obligation booleans when separately run with a local key.
- PASS — `npm run test:live-preflight`: five deterministic tests prove explicit
  authorization/root-key requirements, official default-compose recognition,
  effective Responses-stub refusal before runtime execution, and narrow
  label/boolean runtime inspection. Harness guardrails bind the launcher,
  no-retry/no-artifact live describe, and persisted provider/model assertions.
- PASS — rerun after the security remediation: frontend format/type/lint,
  38-test coverage, build, deterministic harness, and `git diff --check`.
  The frontend coverage command took 98.22 seconds and preserved 84.66%
  statements / 74.71% branches. The active backend's selective Compose-source
  label contains both `docker-compose.yml` and `docker-compose.e2e.yml`, so it
  is intentionally ineligible for a live candidate. A direct live-launcher
  invocation was not continued because the execution guard correctly rejected
  a command that could otherwise reach a paid provider; no provider request
  occurred.
- PASS — unflagged `npm run e2e:live` fails closed before Playwright with
  `Live generation requires TESTFORGE_LIVE_GENERATION_AUTHORIZED=true.` No
  provider request, browser launch, or generated automation execution occurs.
- PASS — final security hardening: `npm run test:live-preflight` now has 6
  deterministic tests covering exact canonical Compose-label matching and
  negatives for an attacker lookalike, `.yml.bak`, another directory with the
  same filename, and multi-source e2e override; portable Docker executable
  resolution; effective stub refusal; and the launcher-mode/live-artifact
  controls. The launcher no longer embeds a workstation path: it accepts only
  an absolute `TESTFORGE_DOCKER_EXECUTABLE` named `docker`/`docker.exe`, else
  executes portable `docker` through `execFileSync` without a shell.
- PASS — direct `npx playwright test --grep @live-generation` against the
  synthetic stack fails at the live suite's `beforeAll` with `Live generation
  must be launched through npm run e2e:live.` at 0ms, before the test body can
  register, create a story, or click Generate. The local failure artifact was
  removed immediately; no provider request occurred. The subsequent ordinary
  stub-backed `npm run e2e` remains 5/5 passing after the spec change.

Complete Java-21 Maven inventory from the prior corrected Docker `mvn -B
verify` run (all reported non-passing cases, before this Builder's source
remediation):

- TF-012 remediated: `DocumentationCoverageTest` (missing Javadocs on
  `validateSteps`/`validateTestData`), `GenerationSchemaParityTest`
  (`withDataReference` left the new setup reference undeclared), and
  `OpenAiTestGenerationProviderTest` (two assertions: stale
  `json_schema.schema` request path and incorrect adapter-boundary assertion
  for absent setup).
- Not attributed to TF-012 and left untouched: `StageOneApiIntegrationTest`
  has two setup-row unique-key errors
  (`cascadesSetupRowsWhenTheirOwningCaseIsDeleted`,
  `preservesOrClearsSetupStepsWithoutLeavingDanglingDataReferences`) and three
  `429` failures
  (`dualWritesLegacyTraceabilityByIdentityAfterInFlightCriterionRename`,
  `failsWithoutCasesAfterInFlightCriterionDeletion`,
  `rejectsBlankGenerationIdempotencyKeysBeforeProviderInvocation`).
- Confirmed pre-existing TF-009 residual: `WorkspaceMigrationIntegrationTest`
  expects V1–V6 while the workspace contains the TF-009 V7 setup-step
  migration. This is the sixth non-passing suite failure in that run.

The focused Java-21 rerun then passed `DocumentationCoverageTest` and all 8
`OpenAiTestGenerationProviderTest` cases, while `GenerationSchemaParityTest`
initially exposed a test-fixture-only reference mismatch: its generic scalar
loop replaced a data reference with undeclared `x` text. The Builder corrected
that fixture so its dedicated exact-boundary candidate gives both setup and
procedure the same canonical declared, whitespace-padded reference. The Lead's
fresh Java-21 Docker focused command completed `BUILD SUCCESS`: 15 tests total,
`DocumentationCoverageTest` 1/1, `GenerationSchemaParityTest` 6/6, and
`OpenAiTestGenerationProviderTest` 8/8, with zero failures and zero errors.
Host Maven/Java remains unavailable, but the focused TF-012 backend evidence is
now complete.

Security and evaluation evidence: no provider call, generated-automation
execution, dependency installation, staging, commit, push, or external write
occurred. `OPENAI_API_KEY` was absent; the separately authorized live candidate
and rubric score remain not run. The release tuple is `manual-test-v4` / `manual-test-result-v2` /
`manual-test-schema-v3` / `manual-test-validator-v4` /
unchanged `openai-responses-v4`, model/provider configuration, and fixture
format 3. The fixture set contains the existing eight updated fixtures plus
`manual-009-actionable-pending-transaction`.

Residual risk and deviation: mocked/unit, harness, and frontend evidence cannot
prove live-provider adherence. Host backend lifecycle remains unavailable
without Maven/Java 21; the Lead reports that the refreshed Docker image compiled
and packaged under Java 21 with Dockerfile tests skipped. The default e2e
workflow now passes against that freshly built synthetic stack. Post-Architect
remediation format, typecheck, lint, harness, and diff checks pass; the live
candidate remains deliberately excluded and not run because its key is absent.
The secure launcher adds a defense-in-depth runtime-compose check, but a live
candidate still needs a separately reviewed default-stack rebuild and its
sanitized report/rubric score.

## Review and completion

Builder evidence is pending Architect `CONFORMS` and independent Reviewer
`APPROVE`/`BLOCK`. The Lead alone decides implementation completion. Only after
that decision may this same Builder record final evidence and move this plan to
`completed/`; publication and exact-SHA CI are separately authorized work.
