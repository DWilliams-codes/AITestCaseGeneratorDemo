# TF-009: Test-case query and reproducible setup

## Status

Completed locally. Materialized by the assigned sole Builder from the approved
Architect handoff; moved to `completed/` after Architect `CONFORMS`, independent
Reviewer `APPROVE`, and the Lead's implementation-completion decision.

## Objective

Deliver both of the approved, backwards-compatible capabilities:

1. Server-side, URL-backed test-case search, filtering, and sorting before
   pagination.
2. A reproducible, structured per-test-case setup contract that is separate from
   preconditions and main test steps.

## Explicit non-goals

- Changes to authorization or workspace policy.
- Provider or model selection changes.
- Live provider evaluation.
- Automation generation or execution.
- Changes to legacy list-route behavior.
- Speculative search indexes.

## Approved contracts and implementation decisions

### Canonical test-case query

Extend the canonical
`/user-stories/{id}/test-cases/page` endpoint and its `/requirements` alias,
while retaining `generationRunId`, `page`, and `size`, with these optional query
parameters:

- `search`: trimmed, maximum 300 characters, literal case-insensitive substring
  over `testCaseKey`, title, objective, and immutable criterion key;
- `status`;
- `category`;
- `priority`;
- `sort`: `sequence-asc` (default), `sequence-desc`, `priority-desc`,
  `status-asc`, or `updated-desc`.

Invalid enum or sort values return a safe `400`. Query resolution always starts
from the owner-owned User Story/Requirement and selected successful generation
run. Search uses escaped `LIKE` semantics for literal `%`, `_`, and `\\`, and
parameterized values. The work-item number is the stable final tie-breaker.
The legacy array route remains unchanged.

### Reproducible setup steps

Add ordered `setupSteps` to each TestCase. A setup step uses the same
`number`/`action`/`expectedResult`/`testDataReference` shape as a test step, but
is distinct from preconditions and main test steps.

Provider v3 output requires a `setupSteps` array of at most 30 items; it may be
empty. The prompt distinguishes assumed preconditions, setup actions with an
observed readiness result, and test steps with test evidence. It forbids
duplicates and invented policy.

Add forward-only Flyway V7 table `test_case_setup_steps` with UUID primary key,
cascading TestCase foreign key, positive step number, action/result length 4000,
nullable reference length 1000, and unique `(test_case_id, step_number)`. No
backfill is needed. Old rows return `[]`. For PATCH compatibility, omitted
`setupSteps` means preserve and explicit `[]` means clear; the new SPA always
sends the field. Setup is visible/editable before test steps and included in
revisions plus JSON, Markdown, and CSV exports. Markdown receives a Setup
section. CSV preserves existing columns and positions, appends a phase column,
and emits `SETUP`/`TEST` with independent numbering. An old binary ignores this
additive table; rollback hides rows but never deletes them.

### Generation/evaluation tuple

Provider and model configuration remain unchanged. Bump prompt to v3, result
contract to v2, schema to v3, validator to v3, OpenAI adapter to v4, and manual
fixtures to format/version 3. Preserve v2 prompt/schema resources for historical
records. No live candidate is authorized.

## Affected implementation surface

- Backend controller, service, repository query, DTOs, and assembler.
- `TestDataReferencePolicy`, setup-step entity/repository, generation contract
  versions, `TestGenerationResult`, validator, transaction/provider mapping,
  and `ExportService`.
- Flyway V7 migration, v3 prompt/schema resources.
- Frontend `RequirementPage`, types and API client, deterministic MSW/e2e stub:
  server query/query key, page reset, local-filter removal, server totals, and
  setup rendering/editing/reference rename-delete behavior.
- Canonical product, API, architecture, testing, security, threat-model, README,
  evaluation rubric/manual fixtures/harness documentation and validation.

## Acceptance criteria

1. Search/filter/sort applies globally before pagination; the legacy/default
   route behavior remains compatible.
2. Query input is bounded and safe, including literal wildcard handling, invalid
   parameters, and owner-scoped authorization.
3. V3 output validates an independent, contiguous setup sequence and test-step
   sequence, validates setup references, and rejects invalid output atomically.
4. Legacy cases expose `setupSteps: []`; setup edits preserve omitted input,
   clear on explicit empty input, are visible/editable, revisioned, and exported.
5. Documentation, fixture/version tuple, schema parity, and deterministic
   evaluation coverage exactly reflect the updated contract.
6. No authorization, security, validation, typing, test, or CI threshold is
   weakened.

## Verification required

- Backend coverage for multi-page global query/filter/sort, literal wildcard
  search, invalid params, and authorization.
- Setup response/edit behavior (omit/preserve, explicit clear, references,
  revisions, atomic rejection), schema/provider/tuple/migration tests.
- Frontend request URL/query-key/page reset/server total/setup workflow tests.
- Deterministic browser coverage for setup and export.
- Deterministic harness and approved local verification commands; no dependency
  installation, provider call, generated automation execution, staging, commit,
  push, or external mutation.

## Risks and rollback

The main risks are pagination truth drifting into local client filtering,
incorrect literal-wildcard escaping, loss of PATCH compatibility, setup/test
step conflation, and unvalidated test-data references. Mitigate with server-side
owner-scoped resolution, parameterized escaped search, omission semantics,
separate ordered contracts, shared semantic/reference validation, and targeted
tests. V7 is additive: an old binary ignores setup rows and rollback hides them
without deleting data. No search index is introduced; large-data performance is
observed rather than speculatively optimized.

## Evidence log

Implemented additive V7 setup-step persistence, server-side canonical page
query controls, v3 prompt/schema/result/validator/adapter tuple, setup-aware
response/PATCH/revision/export/UI paths, v3 sanitized fixtures, harness parity,
and API documentation. Provider/model selection is unchanged; no live candidate
was run and no provider payload, customer data, credential, or generated
automation was retained or executed.

Architect-BLOCK remediation added business-ranked `priority-desc` ordering,
omitted-setup reference validation, v2-preserving v3 prompt instructions,
revision comparison rendering, required setup output in the deterministic e2e
stub, v3 fixture setup obligations, rubric hard failure language, V7 migration
expectations, and setup workflow coverage.

Final remediation added backend source coverage for pre-page literal `%`, `_`,
and `\\` search; business priority order; invalid sort and owner isolation;
setup response, omitted-PATCH preserve, explicit clear, dangling reference
rejection, revision/export evidence, and atomic invalid input; generated v3
setup persistence; V7 constraints, cascade, and V6-to-V7 empty-table behavior.
Schema parity and OpenAI strict-wire tests now assert schema v3 setup bounds,
required setup output, malformed setup fields, semantic setup safety,
contiguity, and reference integrity. The retained five-argument page method is
read-only transactional, and all newly introduced declarations have Javadocs.

Final Architect remediation strengthened the StageOne page contract test with
matching and nonmatching persisted rows, page-two evidence after literal search
filtering, combined status/category/priority filters, all remaining allowlisted
sort modes, invalid enum input, and oversized search rejection. Changed Java
was manually normalized around the controller call, setup entity factory,
repository signature, provider expectations, setup test chain, and affected
generation/response layouts; Maven/Spotless remains unavailable locally.

Final narrow formatting remediation additionally normalized the setup-step
repository derived query, v2 compatibility constructor delegation, export setup
CSV/Markdown formatting, five-argument page delegation, setup/test reference
collection formatting, and long reference comparisons. This was a formatting-
only pass with no contract or behavioral change.

Reviewer remediation retained the valid Markdown `### Setup` heading and its
substring assertion. The deterministic browser workflow now asserts generated
setup rendering, setup edit/add/reference selection, edited readiness display,
CSV `phase`/`SETUP` content, and Markdown `### Setup` content. `npm run e2e`
was attempted without installing anything, but all browser specs could not
reach `/api/v1/auth/csrf` because the local backend proxy target refused the
connection; this is a local topology skip, not a product assertion result.

Architect follow-up made the browser edit assertion deterministic: after adding
setup step two, the spec asserts the expected action/readiness/reference field
counts and fills/selects `nth(1)` for the second setup step. This prevents the
test from accidentally modifying the main procedure step.

## Final local completion evidence

The post-build Architect verdict is `CONFORMS`; the independent Reviewer verdict
is `APPROVE`; the Lead decided TF-009 implementation locally complete. The
observed deterministic evidence is: harness PASS (94 obligations, eight
blocking manual fixtures, and three non-blocking roadmap fixtures); frontend
format, typecheck, lint, coverage, and production build PASS (34 tests;
83.70% statements, 72.21% branches, 77.09% functions, and 85.44% lines); and
`git diff --check` PASS. The full wrapper was attempted: its harness passed,
then it stopped because Maven is absent and no repository Maven wrapper exists.

Residual risks and deliberate exclusions: Maven/Java 21 backend verification
and Java Spotless were unobserved; browser e2e was attempted but the local
backend proxy refused `/api/v1/auth/csrf`; no exact-SHA CI, publication,
staging, commit, push, provider call, live candidate, or generated-automation
execution occurred. These are recorded local limits, not substitutions for
later publication evidence.

Observed checks:

- `python scripts/validate-harness.py`: PASS; 8 blocking manual fixtures and 3
  non-blocking automation-roadmap fixtures validated (candidate run: not run).
- `frontend: npm run typecheck`, `npm run lint`, `npm run format:check`, and
  `npm run test`: PASS (6 files, 34 tests).
- `frontend: npm run test:coverage`: PASS (34 tests; 83.70% statements,
  72.21% branches, 77.09% functions, 85.44% lines).
- `frontend: npm run build`: PASS.
- `git diff --check`: PASS.
- `scripts/verify.ps1`: harness PASS, then expected BLOCKED because `mvn` is
  absent; the repository supplies no Maven wrapper. Maven compile/verify and
  Java Spotless could not run. No dependency installation was used.
- Browser/e2e: attempted with `npm run e2e`; skipped after local backend proxy
  connection refusal to `/api/v1/auth/csrf` (no supported topology available).

Residual risks: backend query/migration behavior still requires Maven-backed
integration verification, including wildcard escaping, authorization, V7
constraints, and the expanded API/provider suite; browser/e2e verification is
blocked by the unavailable local topology. The plan is locally complete; later
exact-SHA CI or publication remains separately authorized work.
