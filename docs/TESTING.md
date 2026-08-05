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
tools and installed frontend dependencies before the backend Maven lifecycle and
frontend formatting, lint, type, unit-coverage, and build checks. They do not
install dependencies, start containers, or call a model provider.

A local wrapper pass is local evidence, not complete publication evidence. A
published change is supported only when the repository CI gates pass for the
exact published commit SHA. Use `$quality-gate` to keep local results, skipped
checks, and exact-SHA CI evidence distinct.

## Test layers

| Layer | Location | Responsibility |
| --- | --- | --- |
| Backend unit and integration | `backend/src/test` | Service rules, authorization, validation, persistence, migrations, provider protocol, and safe failures |
| Frontend unit and workflow | `frontend/src/**/*.test.*` | Auth state, API behavior, rendering, interaction, and accessibility assertions |
| Browser | `frontend/e2e` | Seeded Stage 1 workflow, application shell, keyboard focus, and axe checks |
| Manual-generation benchmark | `evals/manual-test-generation.jsonl` | Sanitized User Stories covering traceability, ambiguity, boundaries, data references, security, recovery, accessibility, and injection resistance |
| Automation roadmap benchmark | `evals/automation-generation.jsonl` | Non-blocking design fixtures for the unimplemented Stage 2 boundary |
| Harness | `scripts/validate-harness.py` | Exact agent/skill allowlists and metadata, fixture shape and enums, rubric invariants, workflow protection, and required documentation |

The backend uses Java 21 and Maven 3.9 or newer. `mvn verify` enforces tests,
Spotless, SpotBugs, and minimum 80% line / 70% branch coverage. The frontend
requires Node 22.12 or newer and locked dependencies; CI currently uses Node 24.

The default Playwright command excludes `@live-generation` tests:

```bash
cd frontend
npm run e2e
```

It requires the seeded application topology and is run separately by CI. Live
generation is never part of the repository harness or default CI.

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

The workspace and User Story slices add migration paths to the backend lifecycle: a fresh
H2 database applies V1–V5, a synthetic V3 database advances through V4 so
deterministic user/workspace/membership/project backfill can be inspected.
An explicit V4→V5 fixture verifies deterministic `MEDIUM` priority backfill and
the nullable rollback bridge. Active-set ordering, failed-run behavior,
supersession confirmation, test-data references, review/reopen transitions,
revision normalization, audit filters, and auth reset races require targeted
unit or integration coverage.
Testcontainers PostgreSQL verifies the actual timestamp type, nullable rollback
bridge, uniqueness and role constraints when Docker is available.

The API workflow asserts registration provisioning, caller-scoped workspace
listing, project `workspaceId`, and the critical negative case: an outsider who
is deliberately inserted as a member of the owner's workspace still receives
`404` for the owner's project, requirement, and test case. This denial must
remain until a later explicit content-policy release.
