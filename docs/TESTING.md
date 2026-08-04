# Testing and verification

## Quality gates

Run a verification wrapper from the repository root:

```powershell
.\scripts\verify.ps1
```

```bash
./scripts/verify.sh
```

The wrappers check required tools and installed frontend dependencies, then run
the backend Maven lifecycle, frontend formatting, lint, type, unit-coverage, and
build checks, followed by the deterministic repository harness. They do not
install dependencies, start containers, or call a model provider.

## Test layers

| Layer | Location | Responsibility |
| --- | --- | --- |
| Backend unit and integration | `backend/src/test` | Service rules, authorization, validation, persistence, migrations, provider protocol, and safe failures |
| Frontend unit and workflow | `frontend/src/**/*.test.*` | Auth state, API behavior, rendering, interaction, and accessibility assertions |
| Browser | `frontend/e2e` | Seeded Stage 1 workflow, application shell, keyboard focus, and axe checks |
| Manual-generation benchmark | `evals/manual-test-generation.jsonl` | Sanitized requirements covering traceability, ambiguity, boundaries, security, recovery, accessibility, and injection resistance |
| Automation roadmap benchmark | `evals/automation-generation.jsonl` | Non-blocking design fixtures for the unimplemented Stage 2 boundary |
| Harness | `scripts/validate-harness.py` | Agent/skill configuration, fixture shape and enums, rubric invariants, and required documentation |

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
semantic validator, provider mapping, or output expectations:

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
