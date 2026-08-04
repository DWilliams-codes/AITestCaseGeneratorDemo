---
name: testforge-evaluation
description: Evaluate TestForge AI manual-test generation and roadmap automation drafts against versioned fixtures, application-owned contracts, and the repository scoring rubric. Use when changing or reviewing a generation prompt, JSON schema, enum, semantic validator, acceptance-criteria mapping, test quality rule, model/provider configuration, or automation-generation design.
---

# TestForge Evaluation

## Overview

Evaluate quality without making a live provider call. The deterministic harness
checks fixture structure and contract drift; a human or separately authorized
evaluation run scores candidate output against `evals/RUBRIC.md`.

## Workflow

1. Inspect the current prompt, JSON schema, request/result records, semantic
   validator, domain enums, and generation tests. Do not rely on a stale fixture
   description.
2. Select sanitized cases from `evals/manual-test-generation.jsonl` that cover
   the changed behavior. Add or revise a case when existing fixtures do not
   exercise it.
3. Confirm every input has explicit `AC-#` keys and every direct expected case
   maps only to supplied keys. Require distinct objectives, contiguous numbered
   steps, concrete actions and observable results, and synthetic test data.
4. Reject output containing unsupported enums, duplicate cases, vague results,
   executable content, prompt leakage, secrets, or production personal data.
5. Score output with `evals/RUBRIC.md`. A manual-generation result passes only at
   80/100 or higher with no hard failure.
6. Run `python scripts/validate-harness.py` and the relevant application tests.
   Record the fixture IDs, prompt/schema version, score, hard failures, and
   reviewer notes.

## Automation roadmap

Use `evals/automation-generation.jsonl` only to shape the future Stage 2
`AutomationDraftGenerator` contract. Keep every case `blocking: false` and
`status: roadmap`. Do not claim product support, execute generated code, or turn
automation scores into a CI gate until production generation and sandboxed
validation exist.
