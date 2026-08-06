# TF-006 Minimal Coherent Test Generation

## Status and ownership

- Status: Active
- Lead: coordinating root agent
- Architect: approved read-only handoff and post-build conformance
- Builder: `/root/minimal_coverage_builder`, sole workspace writer
- Reviewer: independent read-only final gate
- Objective: release prompt behavior `manual-test-v1` to `manual-test-v2` while
  preserving the existing result, schema, validator, provider, model, adapter,
  persistence, API, and frontend contracts.

## Release tuple and compatibility

| Member | TF-006 value |
| --- | --- |
| Prompt | `manual-test-v2` |
| Result contract | unchanged: `manual-test-result-v1` |
| Schema | unchanged: `manual-test-schema-v2` |
| Semantic validator | unchanged: `manual-test-validator-v2` |
| Provider/model/adapter | unchanged |
| Persistence/API/frontend | unchanged |
| Evaluation fixtures | format `1` to `2` |

Keep `test-generation-v1.txt` for rollback and historical runs. This is a
prompt-and-evaluation behavior change only; do not add a coverage-item schema,
database record, API field, DTO, frontend surface, dependency, migration, or
provider call.

## Approved implementation scope

- Add `backend/src/main/resources/prompts/test-generation-v2.txt` and point the
  existing release tuple at `manual-test-v2`.
- Preserve all v1 safety, untrusted-input handling, structural, and semantic
  output requirements. Direct the model to decompose each supplied acceptance
  criterion internally into atomic, independently testable coverage items; do
  not output reasoning or a coverage inventory.
- Require the smallest coherent suite that realizes every atomic item and maps
  every supplied acceptance criterion through existing `acceptanceCriteriaKeys`.
  Allow one realistic case to cover multiple criteria/items when it verifies
  them independently together. Do not default to one criterion per case or
  split a shared workflow without a material distinction in actor/authentication,
  state/precondition, input/boundary, positive/negative/failure/recovery/
  concurrency/accessibility path, expected result, or setup/verification.
- Update only `GenerationContractVersions.java`, the provider and named mocked
  tests, harness, manual-generation fixture/rubric, canonical product,
  architecture/testing, and AI-pipeline documentation, and this active plan.
- Add fixture-v2 evaluator-owned atomic obligations keyed solely to supplied
  acceptance criteria, with bounded minimum/maximum cases and at least one
  required multi-criterion consolidation scenario. The rubric hard-fails
  semantic duplicates that lack distinct risk, condition, or path.

## Acceptance criteria

1. Runtime provider request uses `manual-test-v2`; all other tuple members are
   demonstrably unchanged.
2. `test-generation-v2.txt` preserves v1 safety/validation constraints and
   expresses internal atomic decomposition, complete criterion mapping, coherent
   consolidation, and minimal nonredundancy without hidden output.
3. The existing validator accepts a valid direct multi-criterion case and no
   schema, persistence, API, or frontend contract changes are introduced.
4. Fixture format `2` validates bounded cases, supplied keys, atomic obligations,
   and a multi-criterion consolidation fixture; all automation fixtures remain
   roadmap-only and nonblocking.
5. The rubric rejects semantic duplicate cases without a distinct risk,
   condition, or path. No live provider candidate/scoring run occurs.
6. Deterministic harness and relevant mocked tests pass; evidence records the
   absent live candidate and remaining local/exact-SHA residuals.

## Verification and evidence

Run the deterministic harness plus focused mocked Java tests when the local
toolchain is available. Use `ai-generation-evals` with the narrow
`testforge-evaluation` handoff to inspect fixture IDs, supplied criterion keys,
tuple compatibility, and duplicate hard-failure coverage. Do not call a live
provider, install dependencies, execute generated automation, stage, commit,
push, publish, or move this plan until post-build Architect `CONFORMS`,
independent Reviewer `APPROVE`, and the Lead's completion decision.

## Implementation evidence

- 2026-08-05: The provider now loads `test-generation-v2.txt` and the release
  tuple records `manual-test-v2`; result `manual-test-result-v1`, schema
  `manual-test-schema-v2`, validator `manual-test-validator-v2`, and adapter
  `openai-responses-v3` remain unchanged. The v1 prompt remains present for
  rollback/history. No persistence, API, DTO, frontend, model, provider, or
  schema contract changed.
- 2026-08-05: The `ai-generation-evals` impact review and narrow
  `testforge-evaluation` handoff verified all eight sanitized manual fixture
  IDs use format `2`, bounded minimum/maximum case counts, and evaluator-owned
  atomic obligations keyed only to supplied `AC-#` values. Consolidation is
  required by multiple fixtures. The rubric hard-fails semantic duplicates
  without a distinct risk, condition, or path. No live-provider candidate or
  scoring run was authorized or performed.
- 2026-08-05: `python scripts/validate-harness.py` passed with 3 specialist
  profiles, 6 skills, 8 blocking manual fixtures, and 3 nonblocking automation
  roadmap fixtures. `git diff --check` passed with only pre-existing line-ending
  notices. Focused mocked Java tests were attempted but Maven could not resolve
  the absent Spring Boot 3.5.16 parent because sandbox policy denied Maven
  Central network access; no test class or provider request ran. Remaining
  gates are the available focused test rerun, post-build Architect conformance,
  independent Reviewer approval, and the Lead completion decision.
- 2026-08-06: The preceding Maven-blocked entry is superseded by observed root
  evidence. `GenerationProviderValidationTest`,
  `OpenAiTestGenerationProviderTest`, and `StageOneApiIntegrationTest` passed
  34 tests (7 + 6 + 21), with 0 failures and 0 errors. The full local wrapper
  passed: harness 3 specialist profiles/6 skills/8 blocking manual fixtures/3
  nonblocking roadmap fixtures; backend 77 tests/0 failures/0 errors, SpotBugs
  0 findings, and all JaCoCo thresholds met; frontend audit policy 16,
  comments, format, lint, typecheck, Vitest 34, coverage, and build all passed.
  No live provider call, candidate generation, or candidate scoring occurred.
  TF-006 made no schema, API, persistence, frontend, provider, or model change.
  The plan remains active pending post-build Architect conformance, independent
  Reviewer approval, and the Lead completion decision. Supported exact-SHA
  seven-job CI remains publication evidence; default Playwright execution and
  the POSIX launcher remain local residuals where not independently observed.
- 2026-08-06: Independent Reviewer returned `BLOCK` because fixture-v2 atomic
  obligations still combined independently testable actions/effects and the
  deterministic preflight did not yet prove evaluator evidence-matrix policy.
  The Architect approved same-Builder remediation limited exactly to this active
  plan, `evals/manual-test-generation.jsonl`, `evals/RUBRIC.md`,
  `scripts/validate-harness.py`, `docs/TESTING.md`, and
  `docs/assessment/current-codebase-assessment.md`. The release tuple remains
  `manual-test-v2`/`manual-test-result-v1`/`manual-test-schema-v2`/
  `manual-test-validator-v2`; evaluator obligations remain fixture-only and no
  provider, model, prompt, schema, API, persistence, or frontend contract may
  change. The remediation must preserve supplied criteria and justified direct
  multi-criterion consolidation while splitting compound obligations and adding
  deterministic policy self-tests. No candidate scoring or live provider call
  is authorized.
- 2026-08-06: Reviewer remediation evidence: all eight fixture-v2 records now
  use unique supplied-AC-keyed action/path and observable-effect obligations.
  Bounds remain coherent suites rather than obligation counts: 3..5 returns,
  permissions, and payments; 2..3 injection; 2..4 lockout; 4..6 accessibility;
  3..5 evidence metrics; and 6..10 output-bound validation. Consolidation stays
  required only where one direct workflow justifies it. The deterministic
  preflight now validates normalized obligation uniqueness, supplied-key
  representation, inclusive bounds, and sanitized evidence-matrix cases for
  valid/below/above count, missing/duplicate/unknown/supporting evidence, and
  missing/valid multi-AC proof. `python scripts/validate-harness.py` and
  `git diff --check` passed; no candidate scoring or live provider call ran.
- 2026-08-06: Post-remediation `scripts/verify.ps1` passed end to end. Harness
  evidence was 3 specialist profiles, 6 skills, 8 blocking manual fixtures, and
  3 nonblocking automation roadmap fixtures. Backend verification passed 77
  tests with 0 failures, 0 errors, and 0 skips; SpotBugs found 0 findings and
  all JaCoCo thresholds were met. Frontend audit-policy tests passed 16, and
  comments, format, lint, typecheck, Vitest 34, coverage, and build passed. No
  live provider call, candidate generation, or candidate scoring occurred. The
  Reviewer remediation changed only its approved six-file allowlist; tuple and
  runtime contracts remain unchanged. Local Playwright/POSIX execution and
  exact-SHA CI remain residual publication evidence, not local-plan completion
  prerequisites.
- 2026-08-06: This entry supersedes the earlier incomplete Reviewer-remediation
  note. Manual-008 restores every detailed numeric maximum, first-over boundary,
  unsafe-content category, and `mustCover` value; its evaluator obligations now
  cover every collection/text limit plus separate schema and semantic enforcement.
  The preflight now binds each obligation to an actual direct candidate case,
  mapped AC, numbered observable-result step, final outcome, and explanation;
  it rejects detached, supporting, unknown, wrong-AC, duplicate, and missing
  evidence and requires one same-case direct multi-AC consolidation with separate
  evidence per mapped key. Named sanitized self-tests cover valid min/max,
  below/above count, missing/duplicate/unknown/supporting/wrong-AC evidence,
  missing-step observable evidence, absent/supporting/detached consolidation,
  and valid direct multi-AC evidence. `python scripts/validate-harness.py` and
  `git diff --check` passed. No live provider or candidate scoring occurred.
- 2026-08-06: This is the superseding final remediation verification; earlier
  failed, blocked, or incomplete evidence remains historical only. The full
  local wrapper passed with harness 3 specialist profiles/6 skills/8 blocking
  manual fixtures/3 nonblocking roadmap fixtures; backend 77 tests/0 failures/
  0 errors, SpotBugs 0 findings, and all JaCoCo thresholds met; frontend audit
  policy 16 plus comments, format, lint, typecheck, Vitest 34, coverage, and
  build all passed. The Reviewer remediation changed exactly the approved
  six-file allowlist. No live provider call, candidate generation, or candidate
  scoring occurred. Default Playwright/POSIX execution and exact-SHA seven-job
  CI remain residual publication evidence; this active plan is not moved until
  post-build Architect conformance, independent Reviewer approval, and the Lead
  completion decision.
- 2026-08-06: This superseding correction addresses the final Architect BLOCK.
  Fixtures now separate the cited observable obligations (return rejection;
  title/body/owner/existence nondisclosure; prompt/secret/policy/reasoning
  disclosure; locked correct-password denial) and restore Manual-008's complete
  detailed bounds/security input. The candidate-bound preflight evaluates every
  qualifying direct multi-AC case, accepts any fully evidenced consolidation,
  and names isolated diagnostic self-tests. Harness and whitespace evidence is
  recorded after this correction; no live provider or candidate scoring ran.
- 2026-08-06: Final local wrapper verification after the exact last remediation
  passed. Harness reported 3 specialist profiles, 6 skills, 8 blocking manual
  fixtures, and 3 nonblocking automation-roadmap fixtures. Backend verification
  passed 77 tests with 0 failures, 0 errors, and 0 skips; SpotBugs reported 0
  findings and all JaCoCo thresholds were met. Frontend audit policy passed 16
  tests; comments, format, lint, typecheck, Vitest 34, coverage, and build also
  passed. No live provider call, candidate generation, or candidate scoring
  occurred. The transient
  `scripts/__pycache__/validate-harness.cpython-314.pyc` is absent. Local
  Playwright/POSIX execution and exact-SHA seven-job CI remain residual evidence
  gaps for publication; this plan remains active pending Architect conformance,
  independent Reviewer approval, and the Lead completion decision.
- 2026-08-06: Independent Reviewer returned `BLOCK` for Manual-008 completeness:
  its fixture obligations did not separately enumerate every schema-derived
  bounded path and provider-authored text field. The Architect approved a final
  same-Builder remediation limited exactly to this active plan,
  `evals/manual-test-generation.jsonl`, and `scripts/validate-harness.py`.
  Manual-008 must retain its detailed synthetic input and `mustCover`, use the
  exact curated schema oracle, and contain the required 81 unique AC-keyed
  obligations; the harness must independently prove that contract. No schema,
  Java, prompt, provider, model, API, persistence, frontend, live-provider, or
  candidate-scoring change is authorized.
- 2026-08-06: Final Manual-008 completeness remediation passed deterministic
  verification. The fixture retains its detailed synthetic input and `mustCover`
  and now has exactly 81 unique normalized tuples: 54 AC-1/AC-2 exact and
  first-over obligations for all 27 curated schema bounds, 19 AC-3 named
  fenced-code text-field obligations, six AC-3 named taxonomy representatives,
  and two AC-4 independent-enforcement obligations. `python -B
  scripts/validate-harness.py` passed and printed its isolated oracle evidence:
  complete 81 plus missing exact/first-over, wrong max, both schema-path drift
  directions, missing fenced field, generic authored-field safety, missing
  taxonomy, and unexpected/duplicate diagnostics. `git diff --check` passed
  (line-ending notices only); scoped status names exactly this plan, the manual
  fixture, and the harness. No `__pycache__` directory remains. No live provider
  call, candidate generation, or candidate scoring occurred. This plan remains
  active pending Architect conformance, independent Reviewer approval, and the
  Lead completion decision; local Playwright/POSIX execution and exact-SHA CI
  remain publication residuals.
- 2026-08-06: Final full-wrapper verification after the Manual-008 candidate
  remediation passed. The oracle reported the complete 81-obligation set and
  named diagnostics for missing exact/first-over, wrong maximum, both schema
  path-drift directions, missing fenced field, generic authored-field safety,
  missing taxonomy, and unexpected/duplicate obligation. Harness passed with 3
  specialist profiles, 6 skills, 8 blocking manual fixtures, and 3 nonblocking
  automation-roadmap fixtures. Backend verification passed 77 tests with 0
  failures and 0 errors; SpotBugs reported 0 findings and JaCoCo thresholds
  passed. Frontend audit policy passed 16 tests, and comments, format, lint,
  typecheck, Vitest 34, coverage, and build passed. The final remediation is
  exactly this plan, `evals/manual-test-generation.jsonl`, and
  `scripts/validate-harness.py`; no cache remains. No live provider call,
  candidate generation, or candidate scoring occurred. Local Playwright/POSIX
  execution and exact-SHA seven-job CI remain publication residuals. The plan
  remains active pending Architect conformance, independent Reviewer approval,
  and the Lead completion decision.
- 2026-08-06: Post-build Architect returned `CONFORMS` for the approved TF-006
  scope, including the final Manual-008 oracle remediation. Independent Reviewer
  returned `APPROVE`. The Lead then decided TF-006 local implementation is
  complete. Final local evidence is the passing full wrapper: oracle 81 and all
  named diagnostics; harness 3 specialist profiles/6 skills/8 blocking manual
  fixtures/3 nonblocking roadmap fixtures; backend 77 tests/0 failures/0
  errors, SpotBugs 0 findings, and all JaCoCo thresholds; frontend audit policy
  16, comments, format, lint, typecheck, Vitest 34, coverage, and build. The
  final remediation scope is exactly this plan, the manual fixture, and the
  harness; no `__pycache__` remains. No live provider call, candidate generation,
  candidate scoring, generated-automation execution, staging, commit, push,
  publication, or service shutdown occurred. This supports local implementation
  completion only. Default Playwright/POSIX execution and exact-SHA seven-job CI
  remain residual publication evidence and are not prerequisites for moving this
  completed local plan.
