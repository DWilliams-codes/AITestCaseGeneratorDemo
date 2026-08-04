---
name: feature-delivery
description: Deliver significant TestForge AI features, refactors, schema changes, and generation changes through a bounded Lead-Architect-Builder-Reviewer workflow. Use when work needs evidence-backed reconnaissance, an ExecPlan, one writer, conformance review, independent approval, and completion evidence.
---

# Feature Delivery

## Workflow

1. Read `AGENTS.md` and linked sources. Use `$repository-audit` when repository
   facts are unknown or may have drifted.
2. The Lead defines the outcome. The read-only Architect inspects real contracts,
   risks, acceptance criteria, compatibility, rollback, tests, security, and
   evaluation impact and returns an approved plan handoff without writing it.
3. The Lead immediately assigns one workspace-write Builder and never writes
   overlapping feature files.
4. As its first repository write, the Builder materializes the handoff under
   `docs/exec-plans/active/TF-###-short-name.md`, re-reads it, and only then
   begins implementation. That Builder owns all later remediation.
5. Add tests and canonical documentation with behavior changes. Route generation
   impact through `$ai-generation-evals` and narrow fixture/candidate scoring
   through `$testforge-evaluation`.
6. Run deterministic local verification without installs, provider calls,
   generated automation execution, or unapproved external writes.
7. Obtain read-only Architect `CONFORMS` or `BLOCK` against the plan after
   Builder evidence, then independent Reviewer `APPROVE` or `BLOCK`.
8. Return blockers to the same Builder and repeat affected checks and reviews.
9. After Architect `CONFORMS` and Reviewer `APPROVE`, the Lead decides
   implementation completion. The same Builder records final local evidence and
   moves the plan from `active/` to `completed/`.
10. Only afterward, and only if separately authorized, a Lead/publisher stages,
    commits, and pushes the final candidate. All seven CI jobs must pass that
    exact SHA before the Lead decides publication or merge; no repository write
    is required for that decision.

## Output contract

Preserve approved scope, decisions, exact local commands/results, deviations,
evaluation impact, security/privacy evidence, reviewer verdicts, and residual
risks in the ExecPlan. Keep exact-SHA publication results in GitHub/PR/external
evidence or a later historical record, not as a self-referential prerequisite
inside the candidate commit.

## Guardrails

- Keep one writer; never split overlapping edits.
- Preserve API, persistence, provider, authorization, and frontend contracts
  unless the plan changes them explicitly.
- Use synthetic fixtures and application-owned validation; exclude secrets,
  customer data, production selectors, and hidden prompts/reasoning.
- Keep Stage 2 automation non-executing, roadmap-only, and non-blocking.
- Do not stage, commit, push, open a PR, merge, deploy, publish, install
  dependencies, or mutate external systems without explicit authority.
