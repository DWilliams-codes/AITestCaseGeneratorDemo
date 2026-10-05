# TF-015: Salesforce simulation walkthrough and generation integrity

## Objective and authorization

Implement the Architect-approved TF-015 handoff for Daniel's request to finish TestForge AI for a truthful interview walkthrough. Preserve all prior uncommitted work in the existing React/TypeScript and Java 21 Spring checkout. The user expressly authorizes bounded synthetic simulation; older roadmap-only automation guidance continues to apply to live/generated-code execution.

## Non-goals

No provider calls, dependency installs, credential collection/resolution, real Salesforce org, external integration writes, production operations, deployment, commits, or push. No arbitrary generated code, network, or selector execution. Durable multiuser execution remains planned.

## Inspected contracts and resolved assumptions

Read AGENTS.md; feature-delivery, quality-gate, ai-generation-evals, security-review skills; planning contracts; PRODUCT, MVP specification, ARCHITECTURE, API, TESTING, SECURITY, THREAT_MODEL, and eval rubric. Existing Stage 1 owner authorization, release evidence, manual review and theme are preserved. Resolved answers currently do not enter provider input/hash or update source version; usage is retained only for valid final responses. Criterion DTO list copies can fail before null-element validation. Existing baseline has 39 frontend tests passing; backend 89 tests with four setup 429 failures, two duplicate setup fixture errors and five Docker-dependent skips, to repair without weakening production controls.

## Acceptance criteria

1. Add a public visibly synthetic `/simulation` route, shell/login navigation, professional existing light/dark theme, no connected org or AI request. Jira and Azure DevOps previews say Unconnected. Two supported fixtures cover record create/update and case create/status/owner.
2. Intake requires trimmed title, story/description and at least one nonblank AC; bounded JSON paste/file import has visible errors. Unsupported/custom AC semantics cannot claim runnable coverage: require supported fixture semantics and explicit typed step mapping; show blocking explanation.
3. Clarification changes concrete fixture data and assertions and invalidates approval. Review allows useful data/assertion edits before explicit approval.
4. Allow only CREATE_RECORD, UPDATE_RECORD, ASSERT_FIELD, ASSERT_RECORD_COUNT on allowed synthetic objects/fields. Approval copies immutable source, ordered AC, mapped steps, actions/assertions/data, clarification, environment/role, reusable variables/reference names, fixture/runner versions and canonical fingerprint. All changes invalidate approval; runner independently verifies it.
5. Runs reset isolated fixtures, deduplicate delivered actions, never retry uncertain writes; interruption is INDETERMINATE with partial evidence and an explicit fresh isolated run option. Keep lifecycle RUNNING/COMPLETED/INTERRUPTED, business outcome PASS/FAIL/NOT_EVALUATED/INDETERMINATE, evidence COMPLETE/PARTIAL distinct. Restricted role fails safely without mutation.
6. Per-step records include sequence/action/AC, safe before/after, assertion, time and failure codes; history/download include run ID, approval fingerprint and runner version. In-memory history visibly clears on refresh. Only synthetic variables and secret-reference names; reject/redact sensitive input, never credential values in evidence.
7. Append ordered bounded resolved clarification Q/A to captured provider assumptions as explicitly untrusted data, without overwriting original assumptions. Hash includes same captured input used for retries. Resolution locks owned requirement, preserves prior revision, advances version/time. UI shows saved answers and next-generation use.
8. Capture transport usage before structured parsing; sanitized exceptions retain available usage. Accumulate completed, rejected and failed attempts including malformed/retry/refusal. Unknown totals remain null. Malformed null/blank criteria produce controlled 4xx and no run/provider calls.
9. Preserve framework, existing authorization and live generation behavior. Fix baseline fixture isolation and duplicate setup assumptions only. Record tests, evaluation impact, privacy boundaries, deviations and residual risks.

## Files and compatibility

Add frontend/src/pages/SimulationPage.tsx; frontend/src/simulation/{contracts,fixtures,runner}.ts and tests; simulation Playwright coverage. Touch routes, shell/login navigation and RequirementPage answer display. Narrow backend RequirementService/RequirementEntity/RequirementDtos, GenerationTransactionService/GenerationService/GenerationRunEntity, provider exceptions/provider and targeted tests. Existing nullable token columns need no migration. Document PRODUCT/ARCHITECTURE/API/TESTING and security boundary plus walkthrough. No applied migration edits.

## Ordered implementation and rollback

1. Materialize this handoff as first repository write and re-read it.
2. Build tested typed simulation domain, fixtures, immutable approval and safe runner.
3. Add intake, review, environment, approval, run/history/evidence UI and tests.
4. Fix clarification capture/version and usage/malformed-input integrity with focused tests.
5. Repair baseline deterministic test fixtures, update canonical documentation and run full local gates.
6. Lead obtains Architect CONFORMS then independent Reviewer APPROVE. Builder remediates and records evidence. Move to completed only after Lead implementation-completion decision.

Rollback is removal of additive simulation route/files plus reverting only this task's narrowly identified edits; preserve prior uncommitted changes. No schema/data migration rollback required.

## Verification and evaluation

Run harness first, offline Maven verify with Java21, frontend format/typecheck/lint/coverage/build, simulation desktop/mobile390 light/dark/keyboard/axe browser verification. Test fixture AC coverage, immutable approval mutations, repeated actions, interrupted writes, permission boundaries, safe errors/redaction, import validation, clarification input/hash/version/retry, usage sum/unknown and original authorization regression.

Release tuple at start: manual-test-v5, manual-test-result-v2, manual-test-schema-v3, manual-test-validator-v4, openai-responses-v4. Provider/model configuration unchanged; provider-input behavior adds resolved clarification within existing assumptions. Fixture scoring remains deterministic only; no live candidate or semantic score claimed. Record any adapter evidence version change if transport behavior changes.

## Risks and definition of done

Simulation is browser-local and explicitly not a security authority or Salesforce validation. No persistent execution history, connected sandbox or live AI generated simulation coverage claimed. Arbitrary source semantics cannot be inferred by deterministic fixtures. Approval/fingerprint protects app state integrity, not a malicious browser operator. Local completion requires observed checks, documentation, conformance and reviewer verdicts; publication needs separate authorization and CI.

## Evidence and status

- COMPLETE for the authorized local MVP scope. Builder materialized and re-read this plan before any feature write and preserved existing working-tree work.
- Architect post-build: CONFORMS. Independent Reviewer: APPROVE. Lead implementation-completion decision: COMPLETE. No blockers remain for the authorized local scope.

## Implementation checkpoint (2026-10-05)

- `/simulation` is available on the Lead's existing localhost:5173 Vite process.
  Both maintained workflows support source/import, concrete clarification,
  typed proposal, useful assertion/variable edits, human approval, isolated runs,
  failure exercises, retained approved contracts and JSON evidence download.
- Exact fixture obligations and mappings cannot be removed/replaced. Reviewer
  expectations may be edited to demonstrate a business failure; changing
  environment or clarification rebuilds the proposal with an explicit notice.
  Variables and unresolved reference names are reused without credential entry.
- Backend answers preserve source revision/version and original assumptions,
  enter captured provider input/hash/retry, and fail safely if changed in flight.
  Available usage is captured before parsing (including malformed null roots),
  accumulated on every attempt, and preserved on failures; unknown stays null.
- Current release tuple: manual-test-v5 / manual-test-result-v2 /
  manual-test-schema-v3 / manual-test-validator-v4 / openai-responses-v5.
  Only adapter evidence changed for transport accounting. Model/provider
  configuration is unchanged. Existing nine manual fixture IDs are unchanged;
  new deterministic tests cover the provider-input behavior. No live candidate
  evaluation or provider scoring was run or claimed.

## Observed local verification

- `python scripts/validate-harness.py`: PASS (3 profiles, 6 skills, 9 blocking
  manual fixtures, 3 nonblocking roadmap fixtures; 94-obligation oracle).
- `git diff --check`: PASS; existing CRLF normalization notices are not errors.
- Frontend `npm run format:check`, `typecheck`, `lint`, `comments:check`, `build`:
  PASS. Comments-only repairs addressed pre-existing missing intent comments.
- Frontend `npm run test:coverage`: 52 tests PASS in 9 files. Statements 84.15%,
  branches 78.96%, functions 77.90%, lines 85.45%; thresholds unchanged.
- `npm run test:audit-policy`: 16 PASS. `npm run test:live-preflight`: 6 PASS,
  using only local mocked preflight checks, with no provider call.
- Lead reports `e2e/simulation.spec.ts`: 6/6 PASS (1280/390 light/dark,
  keyboard, axe, approval invalidation, interrupted/repeat/download, dark login).
  Independent CUA verified clarification changes concrete data/assertions,
  current approval invalidation with old evidence intact, interrupted uncertain
  write, read-only denial, account duplicate delivery records=1, safe malformed
  and custom-AC imports, mobile overflow and downloadable approved contract.
- Final offline Java21/Maven3.9.16 `mvn -o verify`: BUILD SUCCESS at
  2026-10-05 13:05:42 Eastern, 102 tests, zero failures, zero errors, five
  Docker-dependent PostgreSQL test skips. This includes malformed-null and both
  late-expiry usage regressions. Spotless, SpotBugs (0 findings), JaCoCo gates
  PASS. Backend coverage: lines 92.43%, branches 75.20%, methods 89.78%.
  Log: task workspace `backend-tf015-verify.log`. No thresholds changed.

## Deviations and remediation

- Baseline login 429s came from repeatedly authenticating the same two fixture
  users. Suite fixture tokens now reuse successful authentication; production
  rate controls are unchanged. Two setup-row fixtures now explicitly replace
  generated rows; the export fixture explicitly supplies approved evidence.
- Required Spotless reformatted 25 already-modified Java files, including files
  with prior user edits; no intentional pre-existing behavior was reverted.
- One existing generated JaCoCo file was corrupt (unknown block type 64).
  Removing only `backend/target/jacoco.exec` and rerunning verify produced a
  clean coverage report and passing unchanged coverage gates.
- A read-only browser review found pre-existing dark login hero contrast 2.157:1.
  White hero text now passes the dark-login axe browser check; buttons/theme
  behavior remain intact.
- Lead's full API-dependent browser suite initially hit a Windows Java Unix
  selector startup failure after PostgreSQL migrations/JPA succeeded; the Lead
  corrected the process-local runtime configuration before retrying, with no
  application source workaround. Six simulation browser tests already pass.

## Completed gates and retained boundaries

Architect CONFORMS, independent Reviewer APPROVE and the Lead decision that
the authorized local MVP implementation is COMPLETE have all been received.
Full local PostgreSQL/API browser evidence and the final-jar repeat are
recorded below. The five Docker integration tests remain honestly skipped. No dependencies installed, live provider/Salesforce operation, secret
collection, commit, push, external message, deployment or publication occurred.
Simulation remains transient, synthetic, limited to the maintained fixtures and
not a browser-adversary security authority. Broader/live/durable execution and
real Jira/Azure DevOps integrations remain planned.

## Architect usage finding remediation

The Architect identified late provider usage lost after GET/list expires a
pending claim. The original claimed run now fills previously unknown usage
dimensions under its row lock only for FAILED `stale_generation_claim`. The
original failure, completion timestamp, no-case outcome and closed lifecycle
remain unchanged. Already-known totals are retained, never added again, making
duplicate completion/failure reconciliation idempotent. A provider latch/race
test covers expiry during work and late completion; a late-failure regression
covers the same exact terminal-state guard and duplicate totals.

Lead reports full Playwright PASS 12/12 in 13.2 seconds against portable
PostgreSQL 18.4 and a loopback-only synthetic Responses stub: six simulation and
six existing authenticated API/UI tests. All eight Flyway migrations and JPA
validation succeeded on a fresh PostgreSQL schema. This is separate evidence,
not a replacement or relabeling of the five skipped Docker tests. Initial
Windows Java AF_UNIX startup failed; a process-local built-in TCP fallback
resolved it with no product-code workaround. The Lead will stop the synthetic
backend/stub/database after final verification, leaving Vite and the explicitly
labeled simulation available. The final-jar rerun passed as recorded below.

## Final acceptance and handoff (2026-10-05)

- Architect verdict: **CONFORMS**, including review of the malformed-null and
  expired-claim late-usage reconciliation fixes. No remaining conformance blocker.
- Independent Reviewer verdict: **APPROVE**. No remaining review blocker.
- Lead decision: **implementation COMPLETE for the authorized local MVP scope**.
  The Lead instructed the same Builder to record these verdicts and move this
  plan from active to completed. Publication remains separately unauthorized.
- Final-jar Playwright repeat: **12/12 PASS in 13.8 seconds**, verified in task
  workspace `playwright-final-jar.log`. The final jar was copied outside the
  checkout before execution; health was UP. Portable PostgreSQL 18.4, all eight
  Flyway migrations, JPA validation, and the loopback-only synthetic Responses
  stub supported this run. This is distinct from the five skipped Docker tests.
- Final backend: **102 tests, 0 failures, 0 errors, 5 skips**; offline verify,
  Spotless, SpotBugs and unchanged JaCoCo gates PASS. Final frontend: **52 tests
  PASS**, format/type/lint/comments/build PASS; audit-policy 16 PASS and
  deterministic preflight 6 PASS. No live AI evaluation occurred.
- Independent browser screenshots in the task workspace:
  `testforge-final-case-evidence.jpg`, `testforge-mobile-dark.jpg`,
  `testforge-interrupted-run.jpg`. Actual downloaded synthetic evidence:
  `account-duplicate-run-evidence.json`. Exact task file manifest:
  `TF015-file-manifest.json` (updated to this completed plan path).
- Interview entry point: **http://127.0.0.1:5173/simulation**. The Lead retains
  the single Vite server and live Chrome simulation tab. The Lead will stop the
  temporary QA backend, Responses stub and PostgreSQL cluster; that test runtime
  is not a real connected workspace or a live AI provider and will not be left
  presented as one. See `docs/INTERVIEW_WALKTHROUGH.md` for the concise walkthrough.
- Preserved limitations: synthetic maintained fixtures only, transient last-20
  run history, no live org or credentials supplied, no durable multiuser runner,
  no real Jira/Azure DevOps connection, no live provider semantic score, and no
  deployment, commit, push or publication. These are explicit product boundaries,
  not unresolved blockers to the approved local demonstration scope.
