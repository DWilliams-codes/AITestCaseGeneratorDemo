---
name: ai-generation-evals
description: Own TestForge generation-change impact analysis, release-tuple tracking, fixture coverage, deterministic validation, and authorized evaluation evidence. Use for prompt, schema, model/provider, output-contract, semantic-validation, or generation-behavior changes.
---

# AI Generation and Evaluation Delivery

## Workflow

1. Read `AGENTS.md`, the approved ExecPlan, `docs/TESTING.md`, current generation
   contracts, and `evals/RUBRIC.md`.
2. Record the affected release tuple: prompt version, application schema/result
   contract, semantic validator, model/provider configuration, and evaluation
   fixture version or IDs. State unchanged members explicitly.
3. Analyze provider-visible fields, data minimization, trust boundaries,
   compatibility, rollback, observability, and safe failure behavior.
4. Select or update sanitized fixtures. Invoke `$testforge-evaluation` for exact
   enum/schema/criterion mapping and authorized candidate scoring.
5. Run the deterministic harness and relevant mocked tests without a live
   provider. If a user separately authorizes a candidate run, retain only
   sanitized evidence.
6. Hand the impact statement and evidence to Architect conformance and Reviewer
   evaluation review.

## Output contract

Record fixture IDs, the release tuple, deterministic commands/results, and—only
for an authorized candidate—score, hard-failure status, reviewer notes, and
candidate/tool/commit identity. Record an absent candidate run as not run.

## Guardrails

- Do not store provider keys, hidden prompts/reasoning, customer requirements,
  personal data, production selectors, or raw provider payloads.
- Never execute generated automation or weaken application-owned validation.
- Keep Stage 2 automation fixtures `blocking: false` and roadmap-only.
- Do not make live provider calls, installs, or external writes by default.
