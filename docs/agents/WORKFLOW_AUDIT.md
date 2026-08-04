# TestForge workflow package audit

This is the TF-002 baseline audit. Use `$repository-audit` for fresh evidence
when profiles, skills, stack, CI, or product contracts change.

## Actual repository and stack

TestForge is a React 19/TypeScript 6/Vite 8 SPA over a Java 21 Spring Boot 3.5.16
modular monolith. PostgreSQL with forward-only Flyway migrations is the system
of record. Provider access is application-owned and provider-neutral; structured
and semantic validation precede persistence. Stage 1 produces reviewed manual
tests. Stage 2 automation generation is unimplemented, non-executing, and
represented only by non-blocking roadmap fixtures.

Canonical sources are [AGENTS.md](../../AGENTS.md),
[product](../PRODUCT.md), [architecture](../ARCHITECTURE.md),
[API](../API.md), [testing](../TESTING.md), [security](../../SECURITY.md),
[threat model](../THREAT_MODEL.md), and [planning](../PLANS.md).

## Existing package inventory before TF-002

- `.codex/config.toml` enabled three concurrent profiles.
- `architect.toml` was read-only planning; `builder.toml` was the sole
  workspace-write implementation role; `reviewer.toml` was read-only final
  review.
- `$feature-delivery` described Architect → Builder → Reviewer delivery.
- `$testforge-evaluation` described current fixture, contract, and rubric checks.
- `scripts/validate-harness.py` validated those three profiles, two skills, six
  blocking manual fixtures, three non-blocking roadmap fixtures, source
  contracts, rubric invariants, and selected documentation.
- PowerShell/Bash wrappers, a PR template, completed TF-000/TF-001 plans, and
  seven existing CI jobs were already present.

## Existing-role capability audit

| Existing role | Strong coverage | Overlap or unsafe ambiguity | Gap before TF-002 | TF-002 treatment |
| --- | --- | --- | --- | --- |
| Lead | Implicit coordination | Completion/move authority was not cleanly separated | No durable handoff, first-write, or publication evidence contract | Retain; assign Builder immediately, never write overlapping feature files, and separate local completion from publication |
| Architect | Contracts, risks, implementation plan | Reconnaissance and final review could blur together; authorship of the initial plan file was ambiguous | No read-only handoff or post-build conformance verdict | Retain; narrow to plan handoff plus `CONFORMS`/`BLOCK` |
| Builder | Single implementation writer and local checks | Could appear authorized to move plans or publish independently | First-write plan materialization, external-write prohibitions, and Lead decision dependency incomplete | Retain; first write/re-read active plan and explicitly prohibit stage/commit/push/PR/merge/deploy/apps/connectors |
| Reviewer | Correctness/security/output review | Quality, security, and evaluation procedures bundled informally | No explicit final verdict or exact-SHA distinction | Retain; require `APPROVE`/`BLOCK` and use focused skills |

No role is removed or merged. Backend and frontend remain responsibilities of
the same Builder rather than new writers. Four bounded skills are added:
`$repository-audit`, `$ai-generation-evals`, `$quality-gate`, and
`$security-review`. Existing `$feature-delivery` is strengthened;
`$testforge-evaluation` is narrowed to application-contract fixture/scoring work.

## Contradictions, overlaps, and missing capabilities found

- Documentation previously described the harness after backend/frontend checks,
  while wrappers actually run the harness first.
- Local wrapper success was not clearly separated from publication evidence for
  an exact commit SHA.
- Architect post-build conformance was absent, and plan movement language could
  imply Builder or Reviewer completion authority.
- Initial plan ownership was contradictory: a read-only Architect could prepare
  a plan, but the workflow did not say the assigned Builder must materialize it
  as the first write before implementation.
- Exact-SHA CI could be read as a self-referential local completion prerequisite
  even though the candidate commit must exist before those results can exist.
- Generation engineering and narrow fixture scoring overlapped in one skill.
- Repository discovery, quality evidence, and security review had no reusable
  output contracts.
- Hardcoded profile/skill validation did not reject every unexpected discovery,
  renamed entry, or duplicate frontmatter name.
- Read-only roles prohibited repository edits but needed stronger external-state
  boundaries. Builder needed explicit prohibitions against staging through
  deployment and connector/app mutation.

## Requested-package comparison

TF-002 implements the durable parts of the supplied package as root/repository
guidance, an operational root `PLANS.md`, a roster, this audit, three profiles,
six skills, PR/testing guidance, ignore protection, and deterministic validation.

Intentionally absent:

- No duplicate bootstrap prompt such as `prompts/IMPLEMENT_WORKFLOW_UPGRADE.md`;
  durable repository contracts supersede a one-shot prompt.
- No nested AGENTS override, CLAUDE instruction, or Copilot instruction.
- No distributable/marketplace plugin; repository-local skills are sufficient.
- No fourth implementation agent: backend/frontend/generation edits stay with
  one Builder.
- No CI job, runtime, API, migration, prompt/schema, fixture, rubric, dependency,
  Docker, README, or CONTRIBUTING change.

## Files added or changed and why

Added: root `PLANS.md` for operational lifecycle routing;
`docs/agents/AGENT_ROSTER.md` for roles, nine capabilities, and examples;
this audit for baseline evidence; and four new skill folders, each containing
only `SKILL.md` and `agents/openai.yaml`.

Changed: root `AGENTS.md` for routing and guardrails; the three specialist TOMLs
for explicit handoffs/permissions; the two existing skills for non-overlapping
scope; `docs/PLANS.md` for Architect handoff, Builder first write, local
completion, and later publication; `docs/TESTING.md` for harness-first and
exact-SHA evidence; the PR template for conformance/verdict/CI evidence;
`.gitignore` for the protected local roadmap; the deterministic harness for
exhaustive config/profile/skill discovery and workflow contracts; and the active
TF-002 plan for observed evidence.

## Validation and CI evidence

The local wrappers run the deterministic harness first, then supported backend
and frontend checks. Local checks, Architect `CONFORMS`, Reviewer `APPROVE`, and
a Lead decision support implementation completion; the same Builder then moves
the plan to completed. Only afterward may a separately authorized publisher
create the candidate. Publication readiness requires all seven existing CI jobs
for that exact SHA: Agent and evaluation harness, Backend verify, Frontend
verify, Dependency security, Repository secret scan, Container vulnerability
scan, and Docker end-to-end and accessibility. Those results belong in
GitHub/PR/external evidence or a later historical record, not inside the
candidate commit; the Lead publication/merge decision needs no repository write.

TF-002 does not rename or weaken those jobs. Normal validation installs nothing,
calls no provider, executes no generated automation, and changes no external
state.

## Routing examples

- Unknown repo state → `$repository-audit` → Architect plan.
- Cross-layer feature → `$feature-delivery` → Architect read-only handoff → Lead
  assigns one Builder → Builder first writes/re-reads the plan → implementation
  → Architect conformance → Reviewer verdict → Lead local-completion decision.
- Prompt/schema/provider/output change → `$ai-generation-evals` with
  `$testforge-evaluation` for fixtures and authorized scoring.
- Local or release verification → `$quality-gate`; exact-SHA CI remains separate.
- Authorization/data/provider/dependency review → `$security-review`; the same
  Builder fixes blockers.

## Remaining risks and next improvements

- Workflow documentation can drift; allowlist discovery and local-link checks
  reduce but cannot eliminate semantic drift.
- Local Java 11, absent Maven/Docker/Bash, or other workstation limits may leave
  checks to supported exact-SHA CI; skipped evidence must remain explicit.
- The protected local “TestForce” roadmap retains a naming mismatch while the
  product remains TestForge.
- Pre-existing supply-chain references use action/image tags rather than fully
  immutable digests in some locations. Pinning is future product-security work,
  not a TF-002 edit.
- The pre-existing Compose database host-port exposure remains a future
  product-security hardening item, not a workflow-package change.
- A later authorized slice may package these skills for distribution, add policy
  automation around exact-SHA CI, or harden supply-chain/Compose controls.
