# Agent and capability roster

This roster maps TestForge's nine delivery capabilities to roles and reusable
skills. Canonical engineering rules remain in [AGENTS.md](../../AGENTS.md).

## Existing role matrix

| Role | Responsibilities | Required inputs | Edit surface | Expected output | Overlaps and gaps | Decision |
| --- | --- | --- | --- | --- | --- | --- |
| Lead | Own scope, immediate Builder assignment, implementation completion, separate publication authorization, and merge decision | User request, audit, Architect handoff, Builder evidence, conformance and review verdicts, optional exact-SHA CI | Never writes overlapping feature files; delegates repository writes to Builder | Approved scope, assignment, local completion decision, later publication/merge decision | Coordinates every role but does not materialize the plan or replace technical review | Keep; separate local completion from publication |
| Architect | Pre-build contract/risk/acceptance analysis and post-build plan conformance | Repository evidence, source contracts, request, later active ExecPlan and Builder evidence | Read-only | Approved plan handoff without repository write; later `CONFORMS` or `BLOCK` | May perform reconnaissance, but `$repository-audit` standardizes it; must not become Reviewer | Keep; narrow to handoff and conformance |
| Builder | First write materializes active plan, then implementation, tests/docs/evals, checks, remediation, and local plan completion record | Architect handoff, Lead assignment, source contracts, active ExecPlan after re-read, review blockers | Workspace-write only within approved plan; sole overlapping writer | Active plan, complete diff, exact local evidence, deviations, residual risk, completed plan after Lead decision | Owns backend/frontend/generation implementation; does not self-approve or publish | Keep; strengthen single-writer, first-write, and external-write boundaries |
| Reviewer | Independent correctness, reliability, security, compatibility, and generated-output gate | Approved plan, full diff, Builder evidence, Architect conformance | Read-only | `APPROVE` or `BLOCK` with severity, evidence, remediation | Uses quality/security/evaluation skills but does not implement or decide completion | Keep; narrow to final independent gate |

## Nine-capability map

| # | Capability | Accountable mapping | Permission and output | Handoff |
| --- | --- | --- | --- | --- |
| 1 | Coordination and orchestration | Lead plus `$feature-delivery` | Decision authority; approved scope, immediate single-writer assignment after Architect handoff, local completion and separate publication decisions | Architect, Builder, Reviewer, optional publisher |
| 2 | Repository reconnaissance | Architect or temporary scout plus `$repository-audit` | Read-only inventory of instructions, stack, manifests, docs, validation, git state, gaps, risks | Architect planning |
| 3 | Domain and architecture | Architect | Read-only contracts, acceptance criteria, compatibility, security/privacy analysis, rollback, then conformance | Builder, then Reviewer |
| 4 | Backend | Builder | Approved Java/config/persistence edits and backend evidence | Architect conformance |
| 5 | Frontend | Builder | Approved React/TypeScript edits and frontend evidence | Architect conformance |
| 6 | AI generation and evaluations | Builder with `$ai-generation-evals`; Architect analysis; Reviewer evaluation review; `$testforge-evaluation` for narrow scoring | Impact statement, release tuple, fixture coverage, deterministic results, authorized scores if applicable | Architect and Reviewer |
| 7 | QA and reliability | Reviewer plus `$quality-gate`; Builder runs fixes/checks | Local and exact-SHA evidence, risk matrix, `APPROVE` or `BLOCK` findings | Same Builder, then Lead |
| 8 | Security | Reviewer plus `$security-review`; Architect plans; Builder remediates | Threat/auth/privacy/secrets/provider/supply-chain findings and verified closure | Same Builder, then Reviewer |
| 9 | Final integration review | Architect, Reviewer, Lead | Architect `CONFORMS`/`BLOCK`, Reviewer `APPROVE`/`BLOCK`, unresolved risks, Lead implementation-completion decision | Builder records/moves plan; only later may an authorized publisher create exact-SHA evidence for Lead publication/merge decision |

## Routing examples

- “What is in this unfamiliar repo?” → `$repository-audit`, then Architect if a
  change is requested.
- “Add a backend endpoint and UI workflow” → `$feature-delivery` → Architect
  read-only handoff → Lead assigns one Builder → Builder first writes/re-reads
  the active plan → both layers → Architect conformance → Reviewer.
- “Change the generation prompt/schema or provider configuration” →
  `$ai-generation-evals`; invoke `$testforge-evaluation` for fixtures/scoring.
- “Are local checks enough to ship?” → `$quality-gate`; publication also needs
  all required CI jobs bound to the exact published SHA.
- “Review authorization, provider data, or dependencies” → `$security-review`;
  remediation returns to the same Builder.
- “Everything looks done” → Architect conformance first, Reviewer verdict second,
  Lead implementation-completion decision third, then Builder records/moves the
  plan. A separately authorized publisher may later create a candidate; seven
  exact-SHA CI jobs precede the Lead publication/merge decision.

## Invariants

Architect and Reviewer are read-only. The Lead never writes overlapping feature
files. One assigned Builder's first write creates the active plan and that
Builder owns every implementation/remediation edit. `CONFORMS` plus `APPROVE`
precedes the Lead implementation-completion decision and Builder plan move.
Publication is later and optional; its seven exact-SHA CI results live outside
the candidate commit and precede a no-write Lead publication/merge decision.
Normal checks never install dependencies, call a live provider, execute
generated automation, or mutate external systems.
