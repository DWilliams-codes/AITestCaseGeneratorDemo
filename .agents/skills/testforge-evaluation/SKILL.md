---
name: testforge-evaluation
description: Evaluate TestForge AI manual-test generation and roadmap automation drafts against versioned fixtures, application-owned contracts, and the repository scoring rubric. Use for narrow prompt, schema, enum, semantic-validator, criterion-mapping, fixture, and candidate-output checks.
---

# TestForge Evaluation

## Scope

This is the narrow scoring authority called by `$ai-generation-evals`. It checks
fixtures and sanitized candidate output against application-owned contracts. It
does not own release planning, provider execution, model selection, or external
publication.

## Workflow

1. Inspect the current prompt, JSON schema, request/result records, semantic
   validator, domain enums, generation tests, and `evals/RUBRIC.md`.
2. Select sanitized cases from `evals/manual-test-generation.jsonl`. Add or
   revise a case only when an approved change lacks coverage.
3. Confirm explicit `AC-#` inputs and mappings only to supplied keys. Require
   distinct objectives, contiguous numbered steps, concrete actions, observable
   results, and synthetic test data.
4. Reject unsupported enums, duplicates, vague results, executable content,
   prompt leakage, secrets, personal/customer data, and unmapped claims.
5. Score an authorized sanitized candidate with `evals/RUBRIC.md`. Manual output
   passes only at 80/100 or higher with no hard failure.
6. Run `python scripts/validate-harness.py` and relevant mocked tests. Return
   machine-readable evidence to `$ai-generation-evals`.

## Output contract

Report fixture IDs, prompt/schema version, deterministic results, and—when a
candidate run was explicitly authorized—score, hard failures, reviewer notes,
and candidate/tool identity. Otherwise record the candidate run as not run.

## Automation roadmap

Keep every `evals/automation-generation.jsonl` case `blocking: false` and
`status: roadmap`. Do not claim Stage 2 support, execute generated code, or gate
CI on automation scores before production generation and isolated validation.

## Guardrails

No live provider call by default. Do not retain credentials, tokens, hidden
prompts/reasoning, customer data, production selectors, or raw provider payloads.
Never weaken schema, semantic validation, or hard-failure rules to obtain a pass.
