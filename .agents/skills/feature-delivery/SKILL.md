---
name: feature-delivery
description: Deliver significant TestForge AI features, refactors, schema changes, and generation changes through a bounded architect-builder-reviewer workflow. Use when a repository change needs an ExecPlan, one authorized writer, independent review, deterministic verification, evaluation impact analysis, and completion evidence.
---

# Feature Delivery

## Overview

Use the repository as the system of record for scope, contracts, evidence, and
decisions. Keep one writer responsible for a feature from implementation through
review fixes.

## Workflow

1. Read `AGENTS.md` and the linked source-of-truth documents.
2. Ask the read-only architect to inspect real contracts and produce a concrete
   plan. Resolve material ambiguity before implementation.
3. Create `docs/exec-plans/active/TF-###-short-name.md` with acceptance criteria,
   exact files, interfaces, tests, evaluation impact, risks, and definition of
   done.
4. Assign one workspace-write builder. Do not split overlapping implementation
   across agents or modify files outside the approved scope.
5. Add tests with behavior changes. For prompt, schema, validator, or generated
   behavior changes, also use `$testforge-evaluation` and update the benchmark.
6. Run `./scripts/verify.sh` or `./scripts/verify.ps1`. Do not install missing
   tools or dependencies automatically, and never call a live provider.
7. Ask the read-only reviewer to inspect the plan, diff, contracts, security,
   tests, and evaluation evidence. Return blocking findings to the same builder.
8. Repeat verification and review until blockers are resolved.
9. Record actual commands, results, deviations, and residual risks. Move the
   plan to `completed/` only when its definition of done is met.

## Guardrails

- Preserve provider, API, persistence, and frontend contracts unless the plan
  explicitly changes them.
- Keep generated content untrusted and require application-owned validation.
- Use synthetic fixtures; exclude credentials, customer data, and hidden
  prompts.
- Keep Stage 2 automation work separate and non-blocking until implemented.
- Require human review before generated evidence or automation is approved.
