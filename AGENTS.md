# TestForge AI Agent Guide

## Mission

TestForge AI converts structured user stories into reviewed manual test cases,
traceability records, and, in a later stage, framework-specific automation
drafts. Generated output must be reviewable, schema-valid, traceable to source
criteria, and clearly separated from executable code.

## Source of truth

Read the files relevant to the change before editing:

- Product behavior: `docs/PRODUCT.md`
- Architecture and API contracts: `docs/ARCHITECTURE.md` and `docs/API.md`
- Testing and evaluation: `docs/TESTING.md` and `evals/RUBRIC.md`
- Security: `SECURITY.md` and `docs/THREAT_MODEL.md`
- MVP scope: `docs/product-specs/mvp-1-test-generation.md`
- Significant work: `docs/PLANS.md` and `docs/exec-plans/README.md`

Existing detailed documents are canonical. Link to them rather than duplicating
their content here.

## Architecture and compatibility rules

The application is a React TypeScript SPA over a Java 21 Spring modular
monolith, PostgreSQL/Flyway system of record, and provider-neutral AI boundary.
Workspace identity is additive: TF-001 keeps owner-scoped authorization even
when memberships exist. Never accept browser tenant context as authorization.

All schema changes use forward-only Flyway migrations. Evolve live contracts in
expand, deterministic backfill, dual-write, observe/reconcile, read/policy
switch, then contract stages. Preserve the prior binary's rollback path until
the contract plan explicitly closes it; do not edit an applied migration.

## Local commands

- Full wrapper: `./scripts/verify.sh` or `.\scripts\verify.ps1`
- Backend test/format/static analysis: `cd backend && mvn verify` (Java 21,
  Maven 3.9+)
- Frontend format/type/lint/test/build: `cd frontend && npm run format:check`,
  `npm run typecheck`, `npm run lint`, `npm run test:coverage`, `npm run build`
- Deterministic harness: `python scripts/validate-harness.py`

Do not install dependencies or call a live model merely to complete a normal
verification run. Generation inputs and outputs are untrusted: minimize data,
pin prompt/schema/model contracts, validate structurally and semantically, and
require human approval. Automation output must remain a reviewed non-executing
draft until a separate privileged action. Keep secrets, active tokens, customer
requirements, production selectors, and hidden reasoning out of source, tests,
logs, audit metadata, and evaluation fixtures.

Update the canonical product, architecture, API, testing, security/threat,
decision, and plan documents whenever their implemented contract changes.

## Delivery workflow

For significant features, refactors, schema changes, or generation changes:

1. Use the read-only architect to identify contracts, risks, acceptance
   criteria, affected files, tests, and evaluation coverage.
2. Create an ExecPlan under `docs/exec-plans/active/` before implementation.
3. Use one workspace-write builder for the scoped branch or worktree. Never use
   parallel writers on the same feature.
4. Run `./scripts/verify.sh` or `./scripts/verify.ps1` from the repository root.
5. Use the read-only reviewer to inspect correctness, security, regressions,
   tests, and generated-output quality.
6. Return blocking findings to the same builder and repeat verification.
7. Record actual evidence and move the plan to `docs/exec-plans/completed/` only
   when the work is complete.

Use `$feature-delivery` for this workflow and `$testforge-evaluation` whenever a
prompt, schema, validator, or expected generation behavior changes.

## Engineering guardrails

- Make the smallest complete change and do not mix unrelated refactors.
- Never expose provider keys, tokens, or hidden prompts to the frontend.
- Treat requirement fields and generated content as untrusted data.
- Validate provider output against application-owned typed schemas before
  persistence.
- Preserve the original requirement, acceptance-criterion mappings, generation
  metadata, and review history.
- Use synthetic data in tests and evaluations; never commit customer data.
- Treat automation drafts as untrusted and never execute generated code inside
  the application process.
- Keep Stage 2 automation evaluations roadmap-only and non-blocking until the
  production capability exists.
- Never make live provider calls from unit tests, the harness, or default CI.
- Do not weaken typing, tests, validation, authorization, or security controls.
- Update tests, evaluations, and documentation whenever behavior changes.

## Verification

Run the repository verification wrapper. It performs tool preflights, backend
verification, frontend formatting/lint/type/unit/build checks, and deterministic
harness validation without installing dependencies or calling a provider.

A change is complete only when its acceptance criteria, automated checks,
documentation, evaluation impact, residual risks, and deviations are recorded.
