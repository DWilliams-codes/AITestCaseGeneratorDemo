# Testing and verification

## Quality gates

Run a verification wrapper from the repository root:

```powershell
.\scripts\verify.ps1
```

```bash
./scripts/verify.sh
```

The wrappers run the deterministic repository harness first, then check required
tools and use read-only `npm ls --all --json` integrity inspection to reject an
installed frontend tree that differs from the reviewed manifest/lock before the
backend Maven lifecycle and frontend audit-policy, comments, formatting, lint,
type, unit-coverage, and build checks. They do not install dependencies, start
containers, or call a model provider.

A local wrapper pass is local evidence, not complete publication evidence. A
published change is supported only when the repository CI gates pass for the
exact published commit SHA. Use `$quality-gate` to keep local results, skipped
checks, and exact-SHA CI evidence distinct.

## Test layers

| Layer | Location | Responsibility |
| --- | --- | --- |
| Backend unit and integration | `backend/src/test` | Service rules, authorization, validation, persistence, migrations, provider protocol, safe failures, bridge reconciliation, and bounded query counts |
| Frontend unit and workflow | `frontend/src/**/*.test.*` | Auth state, API behavior, rendering, interaction, and accessibility assertions |
| Browser | `frontend/e2e` | Seeded deterministic generation/review/traceability/export, safe terminal failure, application shell, keyboard focus, and axe checks through a test-only external Responses stub |
| Manual-generation benchmark | `evals/manual-test-generation.jsonl` | Nine blocking sanitized User Stories covering traceability, direct-versus-supporting semantics, comprehensive bounds, ambiguity, data references, security, recovery, accessibility, injection resistance, and actionable pending-transaction approval |
| Automation roadmap benchmark | `evals/automation-generation.jsonl` | Non-blocking design fixtures for the unimplemented Stage 2 boundary |
| Harness | `scripts/validate-harness.py` | Exact agent/skill allowlists and metadata, fixture shape and enums, rubric invariants, workflow protection, required documentation, and the one IPv4-loopback frontend Compose publication |

The backend uses Java 21 and Maven 3.9 or newer. `mvn verify` enforces tests,
Spotless, SpotBugs, and minimum 80% line / 70% branch coverage. The frontend
requires Node 22.22 or newer and locked dependencies; CI currently uses Node 24.
The deterministic harness fails closed unless default Compose has exactly one
cleartext frontend publication at `127.0.0.1:${FRONTEND_PORT:-3000}:8080`;
non-loopback deployments require TLS and secure cookies.

The default Playwright command excludes `@live-generation` tests and uses the
Compose e2e override's test-only external Responses stub:

```bash
cd frontend
npm run e2e
```

It requires the seeded application topology and is run separately by CI. The
stub derives valid direct cases from supplied source keys and can emit one
deterministic malformed-output failure; it is not packaged in an application
image. Live generation is never part of the repository harness or default CI.
`npm run e2e:live` is an explicitly authorized, single-candidate path rather
than a convenience alias: it requires `TESTFORGE_LIVE_GENERATION_AUTHORIZED=true`,
the default `http://localhost:3000` Compose frontend, a nonblank root ignored
`.env` key entry (and refuses a process-environment key override), and a
selective inspection of the effective running backend.
That inspection fails closed unless the effective backend label has exactly one
canonical, case-insensitive path equal to this repository's
`docker-compose.yml` (no override, lookalike, or other-directory file), the
official `https://api.openai.com/v1` endpoint, pinned `gpt-5.6-sol` model, and
OpenAI provider. Any `docker-compose.e2e.yml`/Responses-stub source is refused
before a browser opens a generation page. Docker resolves portably from the
optional absolute `TESTFORGE_DOCKER_EXECUTABLE` path or `docker` on `PATH`, with
no shell invocation. The tagged suite also fails before setup unless the
launcher sets `TESTFORGE_LIVE_GENERATION_MODE=true`, preventing direct tagged
Playwright calls. The live describe has zero retries and disables trace,
screenshot, and video retention; its only retained artifact is the sanitized
application-owned Manual-009 evidence attachment described in the active plan.

Stage 1 workflow tests also cover server-authoritative test-case query results:
empty filtered results retain their controls and Clear action, an out-of-range
case page recovers to page zero, and the review metric is explicitly scoped to
the returned page. Form tests cover the 50-criterion and 30-item editor limits,
including reducing a legacy oversized case. Theme tests cover safe invalid
storage fallback, system/light/dark selection, and the non-sensitive storage
allowlist. The responsive Playwright check uses a 390px viewport and the
deterministic stub to exercise theme selection and horizontal/mobile layout.
The `docker-compose.e2e.yml` override raises only its disposable local
rate-limit budget so parallel synthetic browser accounts do not test an
unrelated abuse-control window; production defaults remain unchanged.

## AI evaluation policy

Before changing the prompt, response schema, generation result types, enums,
semantic validator, provider mapping, model/provider configuration, or output
expectations, use `$ai-generation-evals` to record impact and release evidence;
use `$testforge-evaluation` for the narrow fixture and scoring contract:

1. Inspect the current source contracts.
2. Add or update sanitized benchmark inputs that exercise the change.
3. Run deterministic schema/harness validation with no provider call.
4. When an explicitly authorized external evaluation is performed, retain only
   sanitized candidate output and score it using [RUBRIC.md](../evals/RUBRIC.md).
5. Require at least 80/100 and no hard failure for manual-generation output.

Manual fixtures use format `3` and pin `manual-test-v5`,
`manual-test-result-v2`, `manual-test-schema-v3`, and
`manual-test-validator-v4`. They contain evaluator-owned atomic obligations
keyed only to supplied criteria, bounded case counts, and a required
multi-criterion consolidation scenario. Supporting evidence is never accepted
as a substitute for direct coverage; semantic duplicates without a distinct
risk, condition, or path hard-fail scoring. The deterministic harness validates
contracts and fixture intent; no live candidate scoring is authorized for TF-006.
It is a deterministic preflight only: the rubric's evidence matrix and semantic
duplicate judgment remain human evaluator scoring, never provider output or a
runtime API/persistence/frontend contract. Manual-009 additionally requires an
actionable synthetic requester authentication path, exact `$500` entry, visible
`PENDING` state, blocked same-requester approval, and observable retained state;
criterion-key placeholder and whole-workflow-compression actions are forbidden.

Automation fixtures do not gate CI while automation generation remains a Stage
2 roadmap item. They may be structurally validated, but must stay
`blocking: false` and `status: roadmap`.

## Test data and failure handling

Use `example.test`, synthetic identifiers, and fictional values. Do not place
credentials, active tokens, customer requirements, personal data, or production
selectors in tests or evaluations. Mock provider transport in automated tests.
A provider failure or rejected output must produce a safe state and must not
persist partial generated evidence.

## Workspace migration and authorization verification

The workspace, User Story, and evidence slices add migration paths to the backend lifecycle: a fresh
H2 database applies V1–V6, synthetic V3/V4/V5 databases advance through V6 so
deterministic user/workspace/membership/project backfill can be inspected.
An explicit V4→V5 fixture verifies deterministic `MEDIUM` priority backfill and
V5→V6 fixtures verify release-tuple backfill, reconstructed snapshots,
dual-write compatibility, and old-column inserts. Active-set ordering, failed-run behavior,
supersession confirmation, test-data references, review/reopen transitions,
revision normalization, audit filters, and auth reset races require targeted
unit or integration coverage.
Testcontainers PostgreSQL verifies the actual timestamp type, nullable rollback
bridge, V6 migration constraints, uniqueness, lock behavior, and role constraints
when Docker is available. A local run without Docker must report these checks as
skipped; supported exact-SHA CI remains required for publication.

The PostgreSQL suite also exercises refresh-family rotation/replay under real
row locking and same-key generation concurrency while a provider latch proves
that no database transaction spans the provider call. Fast integration tests
cover runtime post-V6 reconstruction for old-binary generation and reopen
writes, including explicit provenance, reason transfer, and audit redaction.
Schema parity tests probe every exact bound and max-plus-one value and apply the
unsafe-output corpus, including fenced code, to every provider-authored field.
Adapter tests also reject provider-authored root usage, string boolean/integer
coercion, fractional integers, and numeric enums. Provider-latch integration
tests prove that an in-flight criterion rename dual-writes by captured identity,
deletion fails with no case/link graph, and a same-key bridge POST reconciles
without another provider call. Frontend workflow tests assert typed semantic
notice severity for pending, failed, validation-rejected, and completed runs;
regeneration uses stable story state beyond the visible history page and reuses
one idempotency key for confirmation retry. Controller integration rejects
blank generation keys before provider invocation.
Integration coverage proves a rejected candidate leaves no partial case or
traceability graph. Pure audit-policy tests
cross-check top-level findings, severity metadata, dependency paths, report
status, and malformed/error process outcomes.

The API workflow asserts registration provisioning, caller-scoped workspace
listing, project `workspaceId`, and the critical negative case: an outsider who
is deliberately inserted as a member of the owner's workspace still receives
`404` for the owner's project, requirement, and test case. This denial must
remain until a later explicit content-policy release.
# Superseded generation-set deletion

Deletion tests cover owner scope, explicit confirmation, active and evidence
protection, stable tombstone numbering, full V7 descendant purge, and hidden
tombstones across history, case, coverage, traceability, and export reads.

## TF-015 deterministic simulation and integrity tests

`npm run test:coverage` includes simulation domain and UI checks for the two
fixtures, AC obligation preservation, immutable approval, context invalidation,
synthetic role boundaries, duplicate delivery, interrupted writes, assertion
failures, safe import and credential redaction. `e2e/simulation.spec.ts` covers
desktop/mobile390 light/dark, keyboard initiation, axe, retained approval
evidence, repeat/interruption and JSON download. It requires only the existing
frontend server: `PLAYWRIGHT_BASE_URL=http://localhost:5173` with
`npx --no-install playwright test e2e/simulation.spec.ts` (set the environment
variable using the local shell syntax). No AI provider or Salesforce is called.

Offline backend tests prove clarification persistence, version/hash/input and
retry capture, pending-generation clarification races, malformed criteria,
transport counts before parsing and sums/unknowns across failure paths. StageOne
workflow test authentication is reused within the suite so repeated fixture
setup does not consume the production login rate budget; production limits are
unchanged. Custom setup-row fixtures explicitly replace existing generated setup
rows, and export fixtures explicitly provide approved evidence.

These tests do not score a live model or prove Salesforce connector behavior.
Docker-dependent PostgreSQL integration tests remain skipped when Docker is
unavailable; separate local PostgreSQL browser tests may provide runtime evidence
without being substituted for those skipped migration tests.

## TF-016 isolated fixture checks

`npm run test:demo-fixture` tests exact maintained semantics, six drafts and
AC mappings, High/Synthetic Agent clarification changing actions/results,
unsupported answers, nullable usage and bounded/authenticated stub requests.
It is included in the local verification wrapper. The default browser suite
requires both fixture flags and the exact loopback namespace wiring in
`docker-compose.e2e.yml`; no live provider calls occur.

Backend tests cover the policy matrix, metadata readiness, refused account
adoption, reserved authentication/refresh and old-token generation blocking,
credential-bearing HTTP redirect refusal and the 2 MiB provider response
boundary. On this Windows host, the JDK's documented process-local
`jdk.net.unixdomain.tmpdir` fallback was needed for real loopback HTTP tests;
the path points to an absent temporary directory. No production code or test
skip was added for that host limitation.

Final counts and exact local/CI limitations are recorded in TF-016's ExecPlan.
Five Docker-dependent backend cases remain skipped where Docker is absent;
standalone PostgreSQL migration/API/browser coverage is separate evidence.
