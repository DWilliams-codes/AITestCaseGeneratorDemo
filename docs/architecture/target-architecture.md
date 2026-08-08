# Target architecture

## Direction

Retain the modular monolith as the system of record while making application
boundaries explicit enough to extract only when operational evidence demands
it. The target is not a microservice rewrite.

```mermaid
flowchart LR
  UI["React workspace UI"] --> API["Spring application API"]
  API --> Policy["Workspace policy boundary"]
  API --> Artifacts["Versioned artifact services"]
  API --> Jobs["Generation job orchestrator"]
  Jobs --> Provider["Provider adapters"]
  Jobs --> Validate["Schema and semantic validation"]
  API --> Execution["Future execution orchestration"]
  Execution --> Agent["Provider-neutral execution agent"]
  Execution --> Browser["Allowlisted browser driver"]
  Execution --> Evidence["Scoped evidence store"]
  Artifacts --> DB[("PostgreSQL + Flyway")]
  Policy --> DB
  Jobs --> DB
  Execution --> DB
  API --> Audit["Allowlisted audit/event outbox"]
  Audit --> Sink["Future append-only sink"]
```

## Implemented now

- One Spring deployable, React SPA, PostgreSQL/Flyway, provider boundary, audit,
  owner isolation, and structured manual-test lifecycle.
- TF-001 personal workspaces, membership roles, membership-scoped listing, and
  project dual-write. Membership does not authorize shared content.

## Future bounded components

- `WorkspacePolicy` answers actor/action/resource decisions using active
  membership and role. Controllers supply identity; repositories remain scoped.
- `ArtifactVersionStore` captures immutable source and generated snapshots.
- `GenerationJob` persists state before provider invocation and supports safe
  retries, cancellation, idempotency, and terminal failure evidence.
- The proposed Stage 2 `ExecutionOrchestrator` captures the current approved
  owner-scoped TestCase as an immutable run-bound snapshot before work, leases a
  worker, validates agent/browser action events, separates lifecycle from
  result, and records scoped evidence. Its first target is an isolated
  non-production TestForge environment, not a general browser proxy or a
  dependency on future workspace-sharing/general snapshot work.
- A provider-neutral execution-agent boundary receives minimized approved-step
  context; an allowlisted semantic browser driver performs constrained actions.
  Page content is untrusted, visual computer use is fallback-only, and model
  selection remains configuration-driven pending evaluation evidence.
- `AutomationDraftGenerator` emits a neutral intermediate representation; a
  Copado adapter serializes only reviewed drafts.
- An outbox forwards allowlisted domain/audit events without putting secrets or
  requirement bodies on the event bus.

## Deployment evolution

Scale the stateless API horizontally only after distributed rate limits and job
coordination exist. Keep database migrations under one release coordinator.
Provider workers may become a separate deployment when queue depth, latency, or
failure isolation justifies it; the domain and persistence contracts should not
assume that extraction in advance.

The active [TF-007 execution plan](../exec-plans/active/TF-007-stage-two-agentic-test-execution.md)
defines the proposed execution contracts, safety gates, and unresolved pilot
decisions. It does not authorize implementation.
