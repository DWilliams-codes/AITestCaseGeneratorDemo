---
name: repository-audit
description: Perform read-only TestForge repository reconnaissance and produce an evidence-backed inventory of instructions, stack, manifests, documentation, validation, git state, gaps, conflicts, and risks. Use before planning when repository facts are unknown, stale, or disputed.
---

# Repository Audit

## Workflow

1. Read applicable `AGENTS.md` files and identify the repository root.
2. Inspect tracked guidance, product and architecture documents, manifests,
   runtime/tool versions, agent profiles, skills, validation wrappers, CI,
   security controls, active plans, and relevant git state.
3. Compare actual files and commands with documented claims. Separate observed
   facts, missing evidence, conflicts, and clearly labeled inferences.
4. Report active instructions, stack and contracts, validation surface, tracked
   and untracked state, gaps, risks, and only questions that materially affect
   planning.
5. Hand the evidence to the Architect or Lead; re-audit only facts affected by a
   later change.

## Output contract

Return paths and commands supporting each material claim, the scope searched,
unresolved uncertainty, and a concise planning handoff. Do not present
reconnaissance as an approved design.

## Guardrails

- Remain read-only: no writes, formatting, staging, commits, pushes, PRs,
  installs, external mutations, or production access.
- Do not call a provider, execute generated automation, expose secrets, or copy
  customer data into evidence.
- Do not infer absence from a narrow search or expand into implementation.
