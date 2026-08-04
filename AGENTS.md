# TestForge AI Agent Guide

## Mission and source of truth

TestForge AI converts structured user stories into reviewed manual test cases,
traceability records, and later non-executing automation drafts. Generated
output must be schema-valid, traceable, reviewable, and treated as untrusted.

Read the contracts relevant to a change before editing:

- Product: `docs/PRODUCT.md` and `docs/product-specs/mvp-1-test-generation.md`
- Architecture and API: `docs/ARCHITECTURE.md` and `docs/API.md`
- Testing and evaluation: `docs/TESTING.md` and `evals/RUBRIC.md`
- Security: `SECURITY.md` and `docs/THREAT_MODEL.md`
- Planning: `PLANS.md`, `docs/PLANS.md`, and `docs/exec-plans/README.md`
- Workflow capabilities: `docs/agents/AGENT_ROSTER.md` and
  `docs/agents/WORKFLOW_AUDIT.md`

Existing detailed documents are canonical. Link to them instead of duplicating
their contracts.

## Capability routing

- Lead coordination and significant delivery: `$feature-delivery`.
- Read-only repository reconnaissance: `$repository-audit`.
- Architecture, contracts, acceptance criteria, and compatibility: the
  read-only Architect profile.
- Backend and frontend implementation: the single workspace-write Builder.
- Generation-change ownership and release evidence: `$ai-generation-evals`;
  use the narrower `$testforge-evaluation` for fixture and candidate scoring.
- Local checks and exact-SHA publication evidence: `$quality-gate`.
- Threat, authorization, privacy, secret, provider, and supply-chain review:
  `$security-review`.
- Final integration: post-build Architect conformance, independent Reviewer
  `APPROVE` or `BLOCK`, Lead implementation-completion decision, then optional
  separately authorized publication.

Do not use parallel writers on overlapping files. Architect and Reviewer are
read-only. The Architect returns an approved plan handoff without writing it;
the Lead immediately assigns one Builder and never writes overlapping feature
files. That Builder's first repository write materializes the active ExecPlan,
then the Builder re-reads it before implementation. The same Builder resolves
findings, records local evidence, and performs the active-to-completed move only
after the Lead decides implementation is complete.

## Architecture and compatibility rules

The application is a React TypeScript SPA over a Java 21 Spring modular
monolith, PostgreSQL/Flyway system of record, and provider-neutral AI boundary.
Workspace identity is additive: TF-001 keeps owner-scoped authorization even
when memberships exist. Never accept browser tenant context as authorization.

Use forward-only Flyway migrations. Evolve live contracts through expand,
deterministic backfill, dual-write, observe/reconcile, read/policy switch, then
contract stages. Preserve the prior binary's rollback path until the contract
plan closes it; never edit an applied migration.

## Delivery workflow

For a significant feature, refactor, schema, security, or generation change:

1. Use read-only reconnaissance and Architect analysis to identify contracts,
   risks, acceptance criteria, affected files, tests, and evaluation impact. The
   Architect returns an approved read-only plan handoff to the Lead.
2. The Lead immediately assigns one Builder as the sole writer and never writes
   overlapping feature files.
3. As its first repository write, the Builder materializes the handoff under
   `docs/exec-plans/active/`, re-reads it, and only then begins implementation.
4. Run deterministic local verification without installs or provider calls.
5. Obtain Architect `CONFORMS` or `BLOCK`, then independent Reviewer `APPROVE`
   or `BLOCK`.
6. Return blockers to the same Builder and repeat the affected checks/reviews.
7. After `CONFORMS` and `APPROVE`, the Lead decides implementation completion.
   The same Builder records final local evidence and moves the plan to
   `completed/`.
8. Only afterward, and only if separately authorized, a Lead/publisher stages,
   commits, and pushes the final candidate. All seven CI jobs must pass that
   exact SHA before the Lead decides publication or merge; that decision needs
   no repository write.

## Commands and evidence

- Full local wrapper: `./scripts/verify.sh` or `.\scripts\verify.ps1`
- Harness only: `python scripts/validate-harness.py` or
  `.\scripts\verify.ps1 -HarnessOnly`
- Backend: `cd backend && mvn verify` (Java 21, Maven 3.9+)
- Frontend: `npm run format:check`, `npm run typecheck`, `npm run lint`,
  `npm run test:coverage`, and `npm run build` from `frontend/`

The wrappers run the deterministic harness first. A local wrapper pass supports
implementation completion, not publication readiness. Exact-SHA results belong
in GitHub/PR/external evidence or a later historical record, never as a
self-referential prerequisite inside the candidate commit. Publication requires
all seven supported CI jobs to pass for the exact published commit SHA.

## Engineering guardrails

- Make the smallest complete change and update tests, evaluations, and canonical
  documentation when their contracts change.
- Never install dependencies or call a live model merely for normal validation.
- Never expose provider keys, tokens, or hidden prompts to the frontend.
- Minimize untrusted inputs and validate provider output with application-owned
  structural and semantic contracts before persistence.
- Preserve source requirements, criterion mappings, generation metadata, and
  review history.
- Use synthetic fixtures. Keep credentials, customer data, production
  selectors, provider payloads, and hidden reasoning out of source and evidence.
- Generated automation remains a reviewed, non-executing draft until a separate
  privileged action. Never execute it in the application or verification flow.
- Keep Stage 2 automation evaluation roadmap-only and non-blocking.
- Do not weaken typing, tests, validation, authorization, security controls, CI
  thresholds, or dependency/secret scanning.

A change is complete only when acceptance criteria, observed checks,
documentation, evaluation impact, residual risks, and deviations are recorded.
