# TF-011: Loopback-only cleartext Compose frontend

## Status

Completed locally on 2026-08-12 after Architect `CONFORMS`, independent Reviewer
`APPROVE`, and the Lead's implementation-completion decision.

## Outcome

Ensure the local Compose frontend's cleartext HTTP publication is reachable only
through IPv4 loopback by changing its port mapping to:

```text
127.0.0.1:${FRONTEND_PORT:-3000}:8080
```

Non-loopback deployment remains a separately secured configuration requiring TLS
and secure cookies. This work does not change the application local profile,
cookie behavior, nginx configuration, or internal Compose networking.

## Approved scope

1. Update the frontend `docker-compose.yml` port mapping to the exact loopback
   binding above.
2. Add deterministic, dependency-free harness validation that fails closed unless
   Compose declares exactly one frontend published loopback mapping with the
   required IPv4 host IP, environment-backed host port expression, and container
   port `8080`.
3. Update only the scoped canonical statements in `README.md`, `SECURITY.md`,
   `docs/ARCHITECTURE.md`, `docs/THREAT_MODEL.md`, and `docs/TESTING.md` to state
   that cleartext local Compose is loopback-only and non-loopback access requires
   TLS and secure cookies.

## Constraints and non-goals

- Do not add YAML dependencies.
- Do not alter app local profiles, cookies, nginx, or internal Compose networking.
- Do not install dependencies, call providers, execute generated automation, or
  mutate external systems.
- No generation, schema, provider, output-contract, or semantic-validation
  change is in scope; `ai-generation-evals` is not applicable.

## Acceptance criteria

- `docker-compose.yml` publishes the frontend only as
  `127.0.0.1:${FRONTEND_PORT:-3000}:8080`.
- The deterministic harness rejects missing, malformed, duplicate, wildcard,
  IPv6, or non-loopback frontend published mappings and accepts the approved
  mapping.
- Scoped canonical docs explain the loopback-only cleartext invariant and the
  TLS/secure-cookie requirement for non-loopback access.
- Required local checks are recorded with exact results, skipped checks,
  deviations, changed files, and residual risks.

## Verification plan

Run in order without dependency installation or provider calls:

1. `python scripts/validate-harness.py`
2. `python -m unittest scripts/test_verify.py`
3. `.\scripts\verify.ps1` when viable in the local environment
4. `git diff --check`
5. `docker compose config` when Docker Compose is available

## Changed files

- `docker-compose.yml`: binds the sole frontend publication to
  `127.0.0.1:${FRONTEND_PORT:-3000}:8080`.
- `scripts/validate-harness.py`: adds a dependency-free, fail-closed source
  validator and in-memory negative-branch self-tests for the frontend mapping.
- `README.md`, `SECURITY.md`, `docs/ARCHITECTURE.md`, `docs/THREAT_MODEL.md`,
  and `docs/TESTING.md`: state the cleartext-local loopback invariant and the
  TLS/secure-cookie requirement for every non-loopback deployment.
- This active ExecPlan: records scope and observed evidence.

## Evidence

Observed on 2026-08-12, without dependency installation, provider calls,
generated automation execution, or external mutation:

| Command | Result |
| --- | --- |
| `python -m py_compile scripts/validate-harness.py` | Passed. |
| `python scripts/validate-harness.py` | Passed. The new self-tests confirmed broad, non-loopback, IPv6, duplicate, and missing frontend mappings fail closed; existing harness completed with 3 specialist profiles, 6 skills, 8 blocking manual fixtures, and 3 non-blocking automation roadmap fixtures. |
| `python -m unittest scripts/test_verify.py` | Passed: 5 tests. |
| `.\scripts\verify.ps1` | Harness phase passed; wrapper then exited 1 because required command `mvn` is absent. Backend and frontend wrapper phases were not run. |
| `git diff --check` | Passed. |
| `docker compose --env-file .env.example -f docker-compose.yml config --quiet` and E2E override config | Skipped: `docker compose` is unavailable in this environment. Resolved JSON assertions and runtime E2E were therefore not attempted. |
| `python -B scripts/validate-harness.py` | Passed after cleanup. |
| `python -B -m unittest scripts/test_verify.py` | Passed after cleanup: 5 tests. |
| `git diff --check` and `git status --short -- scripts/__pycache__` | Passed after cleanup; no `scripts/__pycache__` artifact remains. |

The negative-mapping self-test initially exposed an implementation parser error
while the approved in-memory fixture was being developed. It was corrected
before the final passing harness run above.

No `ai-generation-evals` or `testforge-evaluation` work applies: TF-011 changes
only Compose host publication, a deterministic source validator, and security
documentation. No generation contract, fixture, model/provider, prompt, schema,
semantic validator, or candidate output changed.

## Deviations and preserved overlap

Before TF-011 implementation, `README.md`, `SECURITY.md`,
`docs/ARCHITECTURE.md`, `docs/THREAT_MODEL.md`, `docs/TESTING.md`, and
`scripts/validate-harness.py` already contained unrelated user-owned changes.
The Lead confirmed they were stable for integration; their existing content was
preserved. No approved scope was expanded. Full wrapper and Compose semantic
validation remain unavailable only because Maven and Docker Compose are absent.
After verification, Python created the untracked generated directory
`scripts/__pycache__/`. Builder cleanup attempts were interrupted by the local
environment, so the Lead completed the approved exact-target cleanup. Focused
no-bytecode verification then passed and confirmed the directory is absent.

## Residual risks

Docker enforces host-port publication at runtime; the harness statically protects
only the repository default. Host-local malware, privileged local users,
Docker-network peers, and cleartext traffic inside the existing scoped container
networks are outside this change. A deployment requiring LAN, shared, or public
reachability must use a separately configured TLS ingress and secure cookies.
Exact-SHA CI and runtime browser evidence are not local-plan evidence and remain
separately authorized publication work.

## Review gates

Architect `CONFORMS`: the approved scope, security invariant, deterministic
validator, documentation, preserved contracts, and recorded local evidence
conform to TF-011.

Independent Reviewer `APPROVE`: no remaining implementation blocker was found.

The Lead decided implementation completion after both verdicts. Local evidence
supports implementation completion only; Maven and Docker Compose remain
unavailable locally, so full wrapper phases and resolved-Compose/runtime checks
are explicitly unknown rather than passed. Exact-SHA CI, publication, and merge
remain separately authorized work and are not prerequisites for this completed
local ExecPlan.
