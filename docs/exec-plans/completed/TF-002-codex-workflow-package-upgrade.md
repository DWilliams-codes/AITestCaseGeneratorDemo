# TF-002: Codex workflow package upgrade

## Status

Completed — 2026-08-04 on `codex/workflow-package-upgrade`. After the final
focused Architect `CONFORMS`, the independent Reviewer returned `APPROVE` with
no blocking or advisory findings. The Lead decided local implementation is
complete, and the same sole Builder recorded final evidence and moved this plan
to `completed/`. Publication was not performed.

## Source request

Upgrade the supplied lead/architect/builder/reviewer bootstrap into a durable,
auditable TestForge repository workflow package. The completed TF-000 and TF-001
plans, repository scout audit, current seven-gate CI, and supplied workflow brief
are the baseline evidence.

## Objective

Make repository discovery, planning, delivery, evaluation, quality verification,
and security review explicit reusable capabilities without changing product
behavior. Keep TestForge as the operational name. “TestForce” appears only as an
unresolved naming mismatch in the protected local planning artifact.

## Non-goals

- No backend, frontend, API, persistence, Flyway, authentication, or runtime
  behavior change.
- No production prompt/schema/model/provider or evaluation-fixture change.
- No CI job rename, vulnerability-threshold change, dependency upgrade, or
  unrelated supply-chain/network change.
- No local dependency installation, live provider call, generated-code
  execution, production deployment, or external publication by the builder.
- No CLAUDE, Copilot, nested AGENTS override, or distributable plugin package in
  this slice.
- Do not create `prompts/IMPLEMENT_WORKFLOW_UPGRADE.md`: the supplied bootstrap
  prompt is captured by this plan, AGENTS, PLANS, agent docs, and skills, so a
  second prompt would be duplicative rather than durable.

## Audited baseline

- Root `AGENTS.md`; `.codex/config.toml` with three concurrent agent threads;
  read-only architect/reviewer and workspace-write builder profiles.
- Existing `$feature-delivery` and narrowly scoped `$testforge-evaluation`
  skills, each with `agents/openai.yaml` metadata.
- Completed TF-000 orchestration and TF-001 tenancy plans; no active feature plan
  before this file.
- Java 21/Spring Boot 3.5.16/PostgreSQL/Flyway backend and React 19/TypeScript
  6/Vite 8 frontend.
- Deterministic harness, six blocking manual fixtures, three non-blocking
  automation-roadmap fixtures, PowerShell/Bash wrappers, PR template, and seven
  named CI jobs.
- Local Python 3.14 and Node 24 are available; local Java is 11 and Maven,
  Docker, and Bash are unavailable. Supported publication evidence therefore
  comes from exact-SHA CI.
- `docs/plans/testforce-ai-mvp-execplan.md` is local and untracked. Its verified
  baseline SHA-256 is
  `5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`.

## Nine-capability target

| # | Capability | Accountable mapping | Outputs and handoff |
| --- | --- | --- | --- |
| 1 | Coordination and orchestration | Lead plus `$feature-delivery` | Approved scope, living plan, single-writer assignment, decision log, and publication/completion handoff |
| 2 | Repository reconnaissance | Architect or temporary read-only scout plus `$repository-audit` | Evidence-backed inventory of instructions, stack, manifests, docs, validation, git state, gaps, and risks handed to planning |
| 3 | Domain and architecture | Architect | Contracts, acceptance criteria, compatibility, security/privacy risks, validation design, rollback, and ordered builder handoff |
| 4 | Backend | Builder | Scoped Java/configuration implementation and backend evidence returned for conformance and review |
| 5 | Frontend | Builder | Scoped React/TypeScript implementation and frontend evidence returned for conformance and review |
| 6 | AI generation and evaluations | Builder as change owner, with Architect impact analysis and Reviewer evaluation review; `$ai-generation-evals` delegates scoring to `$testforge-evaluation` | Generation-impact statement, fixture selection, deterministic validation, authorized scoring evidence when applicable, and reviewer handoff |
| 7 | QA and reliability | Reviewer plus `$quality-gate`, with Builder running fixes and checks | Risk-based test matrix, deterministic local results, exact-SHA CI evidence, blocking findings, and remediation loop |
| 8 | Security | Reviewer accountable, Architect planning, and Builder remediation via `$security-review` | Threat/authorization/privacy/secrets/supply-chain findings, exact remediation, and verified closure evidence |
| 9 | Final integration review | Reviewer gives explicit APPROVE or BLOCK after post-build Architect conformance review; Lead makes the completion decision | Architect plan-conformance result, Reviewer final verdict, unresolved-risk record, and Lead completion/publication decision |

## Acceptance criteria

- Root and repository guidance route unambiguously among the nine capabilities
  and preserve architect/reviewer read-only and builder single-writer roles.
- The Architect returns an approved read-only plan handoff; the Lead immediately
  assigns one Builder and never writes overlapping feature files; the Builder's
  first repository write materializes and then re-reads the active ExecPlan
  before implementation.
- Root `PLANS.md` is a concise operational planning contract that links to, but
  does not duplicate, TestForge-specific `docs/PLANS.md` and ExecPlan lifecycle
  guidance.
- `docs/agents/` contains a capability roster and a factual baseline audit with
  permissions, triggers, handoffs, validation, and known gaps.
- Four new skills have valid `SKILL.md` frontmatter and minimal
  `agents/openai.yaml` interface metadata; their scopes do not overlap
  ambiguously.
- `$testforge-evaluation` remains the narrow authority for current prompt,
  schema, enum, validator, fixture, and rubric contract checks.
- `docs/TESTING.md` states that wrappers run the harness first and that a local
  wrapper pass is not full publication evidence; supported exact-SHA CI is.
- After the Builder finishes and records checks, a read-only Architect performs
  a post-build conformance review against this plan before the independent
  Reviewer issues the final APPROVE or BLOCK gate.
- `.codex/config.toml` contains only `[agents]` with exactly `enabled` and
  `max_concurrent_threads_per_session`; nested role declarations, any
  `config_file` indirection including escaping/external payloads, and duplicate
  TOML declarations are rejected by pure negative self-tests.
- The standard-library harness enumerates the actual `.codex/agents/*.toml`
  surface against the explicit allowlist `architect.toml`, `builder.toml`, and
  `reviewer.toml`, rejecting missing, renamed, duplicate, or unexpected entries.
- The harness enumerates the actual `.agents/skills/*/SKILL.md` surface against
  the explicit allowlist `feature-delivery`, `testforge-evaluation`,
  `repository-audit`, `ai-generation-evals`, `quality-gate`, and
  `security-review`, rejecting missing, renamed, duplicate, or unexpected
  entries. Negative parser self-tests prove those failure paths.
- Standard-library validation covers agent TOML, constrained skill frontmatter
  and interface metadata, new workflow docs, required local links, and
  protection metadata without network or provider calls. Official
  `quick_validate.py` runs separately for every skill when its existing runtime
  dependencies are available; it is not replaced by the repository parser.
- `.gitignore` contains exactly
  `/docs/plans/testforce-ai-mvp-execplan.md`; the protected file remains local,
  untracked, unstaged, and byte-identical to the baseline hash.
- Existing runtime contracts, fixtures, CI job names, and security thresholds are
  unchanged.
- Local completion requires Architect `CONFORMS`, Reviewer `APPROVE`, a Lead
  implementation-completion decision, and the same Builder's local evidence and
  plan move. Publication is later, separately authorized, and requires all seven
  CI jobs for the exact candidate SHA; its evidence stays outside the candidate
  commit and the Lead publication/merge decision requires no repository write.

## Planned changes

### Create

- `PLANS.md`
- `docs/agents/AGENT_ROSTER.md`
- `docs/agents/WORKFLOW_AUDIT.md`
- `.agents/skills/repository-audit/SKILL.md`
- `.agents/skills/repository-audit/agents/openai.yaml`
- `.agents/skills/ai-generation-evals/SKILL.md`
- `.agents/skills/ai-generation-evals/agents/openai.yaml`
- `.agents/skills/quality-gate/SKILL.md`
- `.agents/skills/quality-gate/agents/openai.yaml`
- `.agents/skills/security-review/SKILL.md`
- `.agents/skills/security-review/agents/openai.yaml`

### Update

- `AGENTS.md`
- `.codex/agents/architect.toml`
- `.codex/agents/builder.toml`
- `.codex/agents/reviewer.toml`
- `.agents/skills/feature-delivery/SKILL.md`
- `.agents/skills/testforge-evaluation/SKILL.md`
- `docs/PLANS.md`
- `docs/TESTING.md`
- `scripts/validate-harness.py`
- `.github/pull_request_template.md`
- `.gitignore`
- This living plan, for actual evidence and lifecycle status only.

### Intentionally unchanged

- `.codex/config.toml` and existing skill `agents/openai.yaml` metadata.
- `.github/workflows/ci.yml`, including all seven job names and enforcement.
- `backend/**`, `frontend/**`, migrations, environment templates, Docker assets,
  runtime prompts/schema, `evals/**`, product/API/architecture contracts, README,
  and CONTRIBUTING.
- `docs/plans/testforce-ai-mvp-execplan.md`; never edit or stage it.

## Implementation sequence

1. The read-only Architect returns an approved plan handoff; the Lead immediately
   assigns one Builder. The Builder's first write materializes this active plan,
   re-reads it, and only then begins implementation; the Lead makes no
   overlapping feature write.
2. Reconfirm git state and protected-plan SHA-256 before every write batch.
3. Add root planning guidance and agent roster/audit documents.
4. Add the four new skills with non-overlapping triggers and metadata.
5. Narrow and cross-link existing agents and skills without changing permissions.
6. Update planning/testing/PR guidance and add the exact ignore rule.
7. Extend the standard-library harness for the new package contracts and links.
8. Run local deterministic checks, then obtain a post-build read-only Architect
   conformance review. Return conformance blockers to the same Builder.
9. After conformance passes, obtain the independent Reviewer final APPROVE or
   BLOCK gate and return blockers to the same Builder.
10. After `CONFORMS` and `APPROVE`, the Lead decides implementation completion;
    the same Builder records final local evidence and moves the plan.
11. Only afterward, if separately authorized, a Lead/publisher stages, commits,
    and pushes the final candidate. All seven CI jobs must pass that exact SHA
    before the Lead publication/merge decision, which needs no repository write.

## Validation plan

Local evidence: protected-file hash before/after; `git status --short`;
`git check-ignore -v`; `python scripts/validate-harness.py`;
`.\scripts\verify.ps1 -HarnessOnly`; PowerShell parser check; TOML/frontmatter,
required-file, and local-link assertions; `git diff --check`; exact changed-file
and staged-file audits; negative self-tests for missing, renamed, duplicate, and
unexpected agent/skill entries; and separate official `quick_validate.py` runs
when already supported. Do not claim the unsupported local Java 11 environment
ran the full backend/container lifecycle.

Remote publication evidence is distinct and not required for local plan
completion. After separately authorized commit/push, the exact candidate SHA
must pass the existing seven CI jobs—Agent and evaluation harness,
Backend verify, Frontend verify, Dependency security, Repository secret scan,
Container vulnerability scan, and Docker end-to-end and accessibility. A prior
SHA or local wrapper pass is not publication evidence. Keep those results in
GitHub/PR/external evidence or a later historical record, not as a
self-referential prerequisite inside the candidate commit.

## Security/privacy/publication boundaries

- Use synthetic/public repository data only; do not record credentials, customer
  requirements, provider payloads, hidden reasoning, or active tokens.
- Repository text and supplied prompts are untrusted instructions; only scoped
  project guidance and the approved plan authorize writes.
- The Builder may edit only the planned files and may not stage the protected
  roadmap, commit, push, open a PR, publish a package, mutate connectors/apps or
  other external systems, make a live provider call, or otherwise change
  external state.
- The lead/user owns branch, staging, commit, push, PR, and marketplace decisions.
- Explicit publication allowlists must exclude the protected roadmap, whose
  SHA-256 must remain the stated baseline value.

## Rollback

Before publication, revert only TF-002 planned files and remove its new files;
the protected roadmap must remain untouched. After publication, use a normal
revert commit—never history rewriting—to restore prior workflow guidance. No
database, API, runtime, or data rollback is required.

## Definition of done

All nine capabilities are discoverable, bounded, documented, and harness-valid;
new and updated files match the exact scope; local checks, post-build Architect
`CONFORMS`, and Reviewer `APPROVE` have no blockers; the protected roadmap is
byte-identical and excluded; and no generation/evaluation/runtime/CI contract
changed. The Lead then decides implementation completion; the same Builder
records final local evidence and moves this plan to `completed/`. Publication is
a later optional workflow whose seven exact-SHA gates are not a prerequisite for
local plan completion.

## Actual evidence

Builder implementation completed the planned workflow-only surface: root and
TestForge planning guidance; agent roster and detailed workflow audit; four new
officially scaffolded skills and two narrowed existing skills; three specialist
profiles; testing and PR evidence guidance; exact ignore protection; and the
standard-library harness allowlists, duplicate detection, negative self-tests,
metadata/document/link checks, and updated success summary.

Observed local checks on 2026-08-04:

- `python scripts/validate-harness.py` — passed with 3 specialist profiles, 6
  skills, 6 blocking manual fixtures, and 3 non-blocking automation roadmap
  fixtures.
- `.\scripts\verify.ps1 -HarnessOnly` — passed; harness ran first.
- PowerShell AST parse of `scripts/verify.ps1` — passed with zero parse errors.
- Official `quick_validate.py` using the existing
  `%TEMP%\testforge-skill-validation-019fc9ea` runtime — all six skill folders
  passed separately.
- `git diff --check` — exited 0; Git reported only line-ending conversion
  warnings for existing tracked-file settings.
- Changed-file audit matched the TF-002 create/update allowlist; staged-file
  audit was empty. No backend, frontend, runtime, fixture, rubric, CI workflow,
  dependency, Docker, README, or CONTRIBUTING file changed.
- `git check-ignore -v docs/plans/testforce-ai-mvp-execplan.md` resolved to the
  exact `.gitignore` protection entry.
- Protected roadmap SHA-256 remained
  `5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`;
  `git ls-files -- docs/plans/testforce-ai-mvp-execplan.md` returned no path and
  the staged-file audit remained empty.

The full Java/frontend/container wrapper was not requested for this
workflow-only slice and remains unsupported by the audited local Java 11 / no
Maven, Docker, or Bash environment. No dependency was installed, no provider or
generated automation was run, and no stage, commit, push, PR, deployment,
connector/app mutation, or other external write occurred. Publication evidence
does not exist and must not be inferred from these local checks.

## Post-build Architect conformance — 2026-08-04

`CONFORMS` — no blockers. The read-only Architect found all 23 changed or
untracked paths matched the approved TF-002 allowlist and the staged-file count
was zero. Exact discovery covered the three allowlisted specialist profiles and
six allowlisted skills; pure negative self-tests covered missing, renamed,
unexpected, and duplicate-name cases.

The documented nine-capability ownership model, single-writer boundary,
Lead-decision/Builder-move lifecycle, and Architect-before-Reviewer gate order
conform to the approved plan. Skill scopes are distinct: repository audit,
delivery orchestration, generation-impact ownership, narrow fixture/candidate
scoring, quality evidence, and read-only security review do not overlap
ambiguously.

The deterministic harness, harness-only PowerShell wrapper, PowerShell AST
parse, and `git diff --check` passed. No excluded runtime, evaluation-fixture,
rubric, CI workflow, dependency, Docker, README, or CONTRIBUTING path changed;
no machine-specific path or secret shape was introduced. The protected roadmap
remained byte-identical at SHA-256
`5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`,
ignored, untracked, and unstaged.

Conformance residuals are exact-SHA CI evidence deferred and unavailable in the
local environment, the TestForce/TestForge naming mismatch, pre-existing
supply-chain action/image pinning and Compose database host-port exposure, and
future documentation drift. That conformance result was followed by the
Reviewer `BLOCK` below and must be repeated after remediation.

## Independent Reviewer review and remediation — 2026-08-04

`BLOCK`. Quality/workflow findings were: `.codex/config.toml` discovery did not
prove that nested `[agents.<name>]` declarations, external or escaping
`config_file` payloads, and duplicate TOML declarations were rejected; initial
ExecPlan authorship contradicted the read-only Architect/single-writer boundary;
and local implementation completion was incorrectly coupled to exact-SHA
publication evidence that can exist only after the candidate commit.

Security review found no new product/runtime secret, authorization, provider,
or data-exposure change in TF-002. Pre-existing action/image pinning and Compose
database host-port exposure remain future product-security residuals. Evaluation
impact is none: no production prompt, schema, validator, provider behavior,
fixture, or rubric changed, and existing deterministic fixture validation
remains the applicable evidence.

The same Builder remediated the blockers by requiring `[agents]` to contain
exactly `enabled` and `max_concurrent_threads_per_session`; adding pure negative
tests for nested roles, escaping/external `config_file`, and duplicate TOML;
making the Architect handoff → Lead assignment → Builder first write/re-read
sequence explicit; and separating local plan completion from later optional
publication across all workflow contracts.

Remediation results: `python scripts/validate-harness.py` and
`.\scripts\verify.ps1 -HarnessOnly` passed with exact three-profile/six-skill
discovery and all negative cases; PowerShell AST parsing passed; all six official
`quick_validate.py` calls passed; and `git diff --check` exited 0 with only the
pre-existing line-ending warnings. The explicit changed-path allowlist passed at
23 paths, staged count was zero, and no product runtime, evaluation, CI workflow,
dependency, or Docker path was present. The protected roadmap remained ignored,
untracked, unstaged, and byte-identical at SHA-256
`5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`.
Remediation then proceeded to the repeated Architect review below. TF-002 stays
active pending the final Reviewer `APPROVE` or `BLOCK` re-review.

## Repeated post-remediation Architect conformance — 2026-08-04

`CONFORMS` — no blockers. The remediated harness rejects every nested
`[agents.<name>]` role declaration, including external or escaping `config_file`
payloads, and rejects duplicate TOML declarations while preserving exact
three-profile and six-skill discovery.

The workflow now consistently requires an approved read-only Architect handoff,
immediate Lead assignment of one Builder, and the Builder's active ExecPlan as
the first repository write followed by a re-read before implementation. Local
completion is also consistently separated from optional later publication:
Architect `CONFORMS` plus Reviewer `APPROVE` precede the Lead implementation
completion decision and Builder plan move; exact-SHA CI follows only after a
separately authorized candidate and stays outside that candidate commit.

The changed-path audit matched the exact 23-path allowlist and staged count was
zero. The deterministic harness, harness-only PowerShell wrapper, PowerShell AST
parse, all six official skill validators, and `git diff --check` passed. No
excluded product/runtime, evaluation, CI workflow, dependency, or Docker change,
machine-specific path, or secret shape was introduced. The protected roadmap
remained ignored, untracked, unstaged, and byte-identical at SHA-256
`5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`.

Residuals are unchanged: exact-SHA publication CI is deferred and unavailable
locally, the TestForce/TestForge naming mismatch remains, pre-existing
supply-chain pinning and Compose database host-port exposure remain future
product-security work, and workflow documentation can drift. At that checkpoint,
TF-002 remained active pending final Reviewer re-review.

## Second Reviewer review and remediation — 2026-08-04

`BLOCK` — one P2 test-evidence finding. The escape predicate already recognized
POSIX absolute paths and Windows drive-absolute paths, so the Reviewer did not
demonstrate a bypass. However, the pure negative self-tests proved only the
existing `../outside.toml` traversal payload and did not explicitly exercise
`/outside.toml` or `C:/outside.toml`.

The same Builder added two in-memory nested-role TOML cases, one for each
absolute-path form, and independently asserted that both produce the
`external or escaping config_file` diagnostic. The existing traversal case was
preserved unchanged.

Post-fix results: `python scripts/validate-harness.py` and
`.\scripts\verify.ps1 -HarnessOnly` passed; PowerShell AST parsing passed; all
six official `quick_validate.py` calls passed; `git diff --check` exited 0 with
only the pre-existing line-ending warnings; and the exact changed-path allowlist
passed at 23 paths with zero staged paths. The protected roadmap remained
ignored, untracked, unstaged, and byte-identical at SHA-256
`5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`.
No product/runtime, evaluation, CI workflow, dependency, Docker, or external
state changed.

## Final focused Architect conformance — 2026-08-04

`CONFORMS` — no blockers. Pure in-memory negative coverage now proves the
external/escaping diagnostic for `../outside.toml`, `/outside.toml`, and
`C:/outside.toml`. The actual `.codex/config.toml` `[agents]` mapping remains
exactly `enabled` and `max_concurrent_threads_per_session`, with only the three
standalone specialist TOMLs and six allowlisted skills accepted.

The deterministic harness and `git diff --check` passed; the changed-path audit
remained exactly 23 paths with zero staged paths. The protected roadmap remained
ignored, untracked, unstaged, and byte-identical at SHA-256
`5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`.
This result advanced TF-002 to the final independent Reviewer re-review.

## Final independent Reviewer re-review — 2026-08-04

`APPROVE` — no blocking or advisory findings. Security is approved for this
workflow-only change: no secret, authorization, provider, customer-data,
generated-execution, dependency, or external-state regression was found. Local
implementation quality is approved based on the exact discovery controls,
negative self-tests, documentation/lifecycle consistency, and deterministic
evidence. Publication remains pending and separately authorized; no candidate
SHA or exact-SHA CI evidence exists. Evaluation impact is none because no
production prompt, schema, validator, model/provider behavior, fixture, or
rubric changed.

Accepted residuals are deferred exact-SHA publication CI, the TestForce/TestForge
naming mismatch, pre-existing action/image pinning and Compose database
host-port exposure, local toolchain limits, and possible future documentation
drift. None blocks local implementation completion.

## Lead implementation-completion decision — 2026-08-04

`COMPLETE` for local implementation on `codex/workflow-package-upgrade`. Final
local evidence passed: `python scripts/validate-harness.py`,
`.\scripts\verify.ps1 -HarnessOnly`, PowerShell AST parsing, all six official
`quick_validate.py` calls, and `git diff --check`. The changed-path surface was
the exact 23-path allowlist, the staged count was zero, and the protected roadmap
remained ignored, untracked, unstaged, and byte-identical at SHA-256
`5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`.

The same sole Builder recorded this final local evidence and moved the plan from
`active/` to `completed/`. No stage, commit, push, PR, merge, deployment,
provider call, connector/app mutation, or other publication action occurred.
All seven exact-SHA CI jobs are deferred until a separately authorized candidate
exists and are not a prerequisite for this local completion decision.

## Deviations

None. The four new skills were created with the standard initializer before
their `SKILL.md` templates were customized, and no resource directories were
added.

## Residual risks

Accepted at local completion: documentation drift between capabilities and
future Codex behavior, local-toolchain limits and deferred exact-SHA publication
CI, the unresolved TestForce/TestForge planning-name mismatch, pre-existing
supply-chain action/image pinning, and pre-existing Compose database host-port
exposure. The final two are future product-security work, not TF-002 edits.
