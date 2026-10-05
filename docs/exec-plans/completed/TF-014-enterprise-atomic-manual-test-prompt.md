# TF-014: Enterprise atomic manual-test prompt

## Status

Active. Architect-approved handoff materialized by the assigned sole Builder
on 2026-08-13. Re-read before implementation.

## Objective

Introduce an immutable enterprise-grade atomic manual-test prompt that preserves
all v4 safeguards while making user-visible test paths concrete, reproducible,
and individually executable.

## Approved contract

- Add immutable prompt `manual-test-v5` using the user's authoritative wording
  and retaining every v4 safety, minimum-suite, acceptance-criterion mapping,
  ambiguity, and JSON-only clause.
- Interpret the numbered path precisely: preconditions are assumed-only;
  `setupSteps` are separately numbered, performed readiness actions; `steps`
  are evidence operations and observations. Do not compress a workflow or use
  generic criterion placeholders.
- Release tuple: prompt `manual-test-v5`; result `manual-test-result-v2`, schema
  `manual-test-schema-v3`, validator `manual-test-validator-v4`, provider
  adapter/model configuration, and fixture format remain unchanged.
- Retain the nine existing blocking manual fixtures; do not add fixtures.
  Update rubric hard failures and harness release history for v3/v4/v5.
- Update the approved prompt/version/provider/API/browser test surface and the
  fake-provider deterministic output so it uses atomic actions rather than
  combined workflow actions.

## Scope and exclusions

Change only the approved prompt, generation version binding, fixture/rubric/
harness evidence, fake provider, and focused provider/API/browser tests. Do not
change the JSON schema, semantic validator, result/API records or DTOs,
migrations, frontend production components/types, provider/model configuration,
or security documentation. Preserve completed TF-013 and dirty TF-009–012.

## Evaluation and safety

Fixture IDs remain the existing nine sanitized manual fixtures; no live
candidate is authorized or run. Provider-visible input remains minimized and
untrusted provider output continues through unchanged application-owned schema
and semantic validation. No provider keys, raw envelopes, hidden reasoning, or
generated automation are retained.

## Required evidence

Record tuple members and unchanged members, deterministic harness/rubric
history, mocked provider and API checks, fake output atomicity, browser absence
of generic actions, and frontend formatting/type/lint/coverage/build as
applicable. No live provider, installs, staging, commit, push, or publication.

## Review and completion

Builder evidence requires Architect `CONFORMS` and independent Reviewer
`APPROVE` before the Lead decides completion and this plan moves to completed.
Publication is separately authorized.

## Builder checkpoint — 2026-08-13

- First write and reread gate completed: this active plan was materialized and
  reread before TF-014 feature edits.
- Implemented prompt-only release change: immutable
  `test-generation-v5.txt`, `GenerationContractVersions.PROMPT =
  manual-test-v5`, and the OpenAI prompt resource binding now use v5. Result v2,
  schema v3, validator v4, adapter/model configuration, and fixture format 3
  are unchanged.
- Updated exactly the nine existing sanitized manual fixtures to prompt v5;
  no fixture was added. Harness retains v3/v4 prompt resources as historical
  artifacts, binds v5 as current, and checks the retained atomic-path clauses.
- Updated rubric hard failures for performed preconditions, combined actions,
  and compressed navigation/entry/submission/observation steps. Fake-provider
  outputs now use individually atomic interactions; provider/API/live-browser
  assertions use v5 and retain generic-action rejection checks.
- PASS — deterministic harness and `git diff --check`; frontend format,
  typecheck, lint, coverage, Workflow 9/9, and build. No live provider
  candidate was authorized or run.
- PASS — Lead-run safe Java 21 focused suite (fresh Surefire evidence): 23/23
  tests with zero failures or errors: `GenerationProviderValidationTest` 8,
  `OpenAiTestGenerationProviderTest` 8, `GenerationSchemaParityTest` 6, and
  the TF-014 StageOne tuple/workflow assertion 1. The local shell remains Java
  11/no Maven PATH, so Maven was delegated to the approved Java 21 environment.
- PENDING — default non-live stub-backed e2e must run only after the Lead
  rebuilds the synthetic stack, whose current images predate the v5 prompt
  resource. No Docker socket, Testcontainers, live provider, install, staging,
  commit, push, or publication was used.

## Final local evidence — 2026-08-13

- PASS — the Lead rebuilt fresh synthetic v5 images; all services were healthy
  and the backend Java 21 package compile succeeded. Default stub-backed
  browser e2e passed 6/6 in 5.6 seconds. It used no live provider.
- PASS — post-e2e deterministic harness and `git diff --check` passed. The
  latter reports only the repository's CRLF normalization warning for
  `scripts/validate-harness.py`.
- Live evidence: no v5 live candidate was run or authorized. Earlier v4 live
  evidence, if present in repository history, cannot substantiate this v5
  prompt release and is not claimed as v5 evidence.
- Residual risk: semantic validation remains intentionally validator v4; the
  v5 prompt is covered by synthetic fixtures, mocked provider/API tests, and
  stub-backed e2e only. Architect conformance and independent Reviewer approval
  remain required before Lead completion.

## Architect remediation checkpoint — 2026-08-13

- Added the authoritative v5 wording for realistic enterprise roles, synthetic
  data, permissions, statuses, approvals, and audit-relevant outcomes where
  applicable; each case must be independently executable without undocumented
  assumptions. The retained source-supported-policy and no-invented-policy rule
  remains explicit.
- Exact TF-014 interface inventory: new immutable v5 prompt; prompt constant
  and OpenAI resource binding; existing nine fixture prompt tuples; harness and
  rubric; deterministic fake provider; provider, StageOne, and browser tests;
  PRODUCT, MVP, ARCHITECTURE, and pipeline documents. No schema, semantic
  validator, result/API DTO, migration, frontend production component/type,
  provider/model configuration, or security document is changed.
- Compatibility and rollback: v4 remains immutable at
  `test-generation-v4.txt` and remains readable as historical prompt evidence;
  persisted generation runs truthfully retain their original release tuple,
  while new runs record v5. This prompt-resource rebinding is source-compatible:
  rollback selects the retained v4 resource/constant without schema, API, or
  migration rollback. No historical run is reinterpreted.
- Fake and browser stub setup now decomposes authentication into open, enter
  identifier, enter credential, select, and inspect interactions. Fake evidence
  decomposes record/confirmation, validation/blocking, and retry/duplicate
  observations. Focused assertions cover every generated setup and evidence
  step across happy, validation, recovery, and accessibility output; browser
  assertions reject former compressed strings.
- PASS — after remediation, frontend format check, typecheck, lint, Workflow
  test 9/9, and production build passed. Harness passed (nine blocking manual
  fixtures and three automation roadmap fixtures) and `git diff --check` passed
  with only the pre-existing CRLF normalization warning. Safe Java 21 focused
  tests and a fresh synthetic rebuild/default e2e remain pending because fake
  provider and stub output changed.
- PASS — Lead-run remediated safe Java 21 focused suite: 23/23 tests, BUILD
  SUCCESS, zero failures/errors (`GenerationProviderValidationTest` 8,
  `OpenAiTestGenerationProviderTest` 8, `GenerationSchemaParityTest` 6, and
  StageOne tuple/workflow 1). Surefire emitted only a fork-shutdown warning.
  Fresh synthetic-stack rebuild and default non-live e2e remain pending because
  the deterministic stub changed.
- Browser remediation: the first fresh-stack default e2e was 5/6; its only
  TF-014 failure was an obsolete total-action-field assertion (five versus the
  new nine atomic setup-plus-procedure fields). The spec now scopes its edit to
  the added `Setup step 6` card and asserts concrete open/enter/select/inspect
  actions rather than global field counts. Formatting passed. A direct rerun
  subsequently timed out while waiting for registration `POST /api/v1/auth/register`
  before reaching the changed assertion; this is a synthetic-stack/environment
  failure, not evidence of a TF-014 browser assertion failure. The ignored
  generated `frontend/test-results` directory was removed. A healthy-stack
  default e2e retry remains pending.
- PASS — after the Lead confirmed the synthetic stack healthy, the targeted
  complete-workflow browser test passed 1/1 in 4.6 seconds and the full default
  non-live e2e suite passed 6/6 in 5.0 seconds. The earlier stale-count failure
  is remediated; the intervening registration timeout is environmental only.
  This fresh-stack pass is the authoritative browser evidence. No live provider
  candidate was run.
- Final narrow remediation: the stub now directs the tester to one primary
  workflow input and observes only that input's displayed synthetic value;
  plural-control and combined validation wording is explicitly absent in the
  browser assertion. Frontend format check, deterministic harness, and
  `git diff --check` passed (only the known CRLF normalization warning). A
  fresh synthetic rebuild plus targeted and default e2e are pending because the
  stub source changed. No provider call was made.
- PASS — following the final stub-correction rebuild, the targeted complete
  workflow passed 1/1 in 5.3 seconds and the full default non-live e2e suite
  passed 6/6 in 5.0 seconds. All services were healthy. This is the final
  authoritative browser evidence; no live provider candidate was run.

## Completion decision — 2026-08-13

- Architect verdict: `CONFORMS`. Independent Reviewer verdict: `APPROVE`. The
  Lead has decided TF-014 locally complete.
- Release/evaluation impact: new runs bind immutable prompt `manual-test-v5`;
  result `manual-test-result-v2`, schema `manual-test-schema-v3`, semantic
  validator `manual-test-validator-v4`, adapter/model configuration, and
  fixture format 3 remain unchanged. The nine existing sanitized manual
  fixtures were retupled; the harness preserves v3/v4 prompt history and v5
  current-contract clauses; the rubric adds atomic-path hard failures. No
  fixture was added and no live v5 candidate was authorized or run.
- Exact local evidence: deterministic harness passed (nine blocking manual and
  three non-blocking automation fixtures); frontend format/type/lint, Workflow
  9/9, coverage, and build passed; safe Java 21 focused suite passed 23/23,
  zero failures/errors (a Surefire fork-shutdown warning only); final fresh
  default stub-backed e2e passed 6/6 in 5.0 seconds and its targeted workflow
  passed 1/1 in 5.3 seconds; `git diff --check` passed with only the existing
  CRLF normalization warning.
- Local readiness is not publication readiness. No install, Docker socket,
  Testcontainers, live provider, staging, commit, push, publication, or
  exact-SHA CI was used. Any supported exact-SHA publication checks are
  separately authorized publisher work.
- Rollback is source-compatible: retain immutable `test-generation-v4.txt` and
  rebind the prompt constant/resource to v4 if required; persisted runs retain
  their truthful original tuple. No schema, API, DTO, migration, validator,
  provider/model, or frontend-production rollback is necessary.
