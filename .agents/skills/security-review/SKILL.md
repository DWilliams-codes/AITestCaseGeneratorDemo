---
name: security-review
description: Perform a read-only TestForge threat and security review across authorization, data handling, provider boundaries, secrets, generated content, dependencies, and supply chain. Use during planning, implementation review, or release gating.
---

# Security Review

## Workflow

1. Read `AGENTS.md`, `SECURITY.md`, `docs/THREAT_MODEL.md`, the ExecPlan,
   relevant architecture/API contracts, and the full changed-file surface.
2. Trace identity, authorization, workspace/owner scope, untrusted requirement
   and provider data, validation, persistence, logs, audit metadata, and errors.
   Never accept browser tenant context as authorization.
3. Inspect secrets, prompt injection/data exfiltration, output validation,
   automation non-execution, dependencies, CI permissions, provenance, container
   scanning, and rollback.
4. Confirm fixtures and evidence are synthetic and exclude keys, tokens,
   customer requirements, production selectors, and hidden reasoning.
5. Return findings to the Builder and verify remediation read-only before the
   Reviewer verdict.

## Output contract

Order findings by severity. Include the affected path, evidence, plausible
impact, exact remediation, and verification method. Separate confirmed findings,
missing evidence, and residual risk.

## Guardrails

- Do not modify files, dependencies, credentials, CI, production, connectors,
  apps, or other external state.
- Do not disclose sensitive payloads, call a provider, execute generated
  automation, or weaken controls.
- Scanner success alone does not prove authorization or data boundaries.
