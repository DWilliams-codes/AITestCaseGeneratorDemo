---
name: quality-gate
description: Produce TestForge APPROVE or BLOCK quality evidence by separating deterministic local checks from supported exact-SHA publication CI. Use for implementation verification, release readiness, review gates, and remediation loops.
---

# Quality Gate

## Workflow

1. Read `AGENTS.md`, the ExecPlan acceptance criteria, `docs/TESTING.md`, and the
   changed-file surface. Build a risk-based matrix of required harness, unit,
   integration, browser, security, and evaluation checks.
2. Run the deterministic harness first, then the planned local checks. Record
   exact commands, results, counts, environment constraints, and skipped checks.
3. Check `git diff --check`, exact changed/staged paths, and protected-file
   requirements without including unrelated user work.
4. Return local `APPROVE` only when implementation checks pass with no blocker;
   otherwise return `BLOCK` with the failing check, impact, evidence, and
   remediation. Send blockers to the same Builder and re-run affected checks.
5. After local completion, if publication is separately authorized, bind all
   seven required CI jobs to the exact candidate SHA. A local pass, prior SHA,
   queued job, or partial gate set is not publication readiness.
6. Return publication `APPROVE` only for that complete exact-SHA gate set. The
   Lead may then decide publication or merge without a repository write.

## Output contract

Separate local results, unavailable/skipped checks, findings, and residual risks
from exact-SHA CI. Local evidence supports implementation completion. Keep
exact-SHA results in GitHub/PR/external evidence or a later historical record,
not as a self-referential prerequisite inside the candidate commit. State
whether evidence supports local readiness, publication readiness, neither, or
both.

## Guardrails

- Do not install dependencies, alter checks, lower thresholds, or hide failures.
- Do not call providers, execute generated automation, stage, commit, push, open
  a PR, deploy, or mutate CI/external state without explicit authority.
- Do not require exact-SHA CI for local plan completion or conflate Reviewer
  verdict, implementation completion, publication readiness, and merge.
