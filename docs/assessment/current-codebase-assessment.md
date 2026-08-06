# Current codebase assessment

## Scope and evidence

This assessment describes the repository after local TF-005 implementation and
the evaluator-only TF-006 prompt release.
Evidence was taken from the Spring domain packages, Flyway V1–V6, React API types and workflow tests,
provider validation tests, export code, security tests, and the deterministic
evaluation harness. “Future” below means designed but not implemented.

## Repository and runtime inventory

| Area | Current implementation | Evidence / boundary |
| --- | --- | --- |
| Backend | Java 21 Spring Boot modular monolith under `backend/src/main/java/com/testforge` | Packages: `auth`, `user`, `workspace`, `project`, `requirement`, `generation`, `testcase`, `traceability`, `export`, `audit`, `security`, `common`, `config`; unused production automation types were removed |
| Frontend | React/TypeScript SPA under `frontend/src` | Auth context, application shell, project dashboard, project detail, requirement workspace, test-case review, API client, and shared API types |
| Persistence | PostgreSQL system of record, Hibernate validation, forward-only Flyway; H2 test scope only | V1–V5 prior schema; V6 additive release tuple, source snapshots, snapshot traceability, and revision provenance |
| AI resources | Versioned prompt and strict JSON Schema under backend resources | Runtime `test-generation-v2.txt` with retained v1 rollback/history prompt; runtime `test-generation-schema-v2.json` with retained schema v1, loaded by `OpenAiTestGenerationProvider` |
| Evaluation | Versioned JSONL fixtures and rubric under `evals`, deterministic validator under `scripts` | Eight blocking manual-test fixtures; three non-blocking automation roadmap fixtures |
| Delivery | Docker/Compose, GitHub Actions, verification wrappers, agent/skill guidance | No live provider call or dependency installation in the default harness |

## End-to-end implemented application flow

1. The browser obtains a CSRF cookie, registers or logs in, holds the JWT access
   token in memory, and receives a rotating refresh token in an HttpOnly cookie.
2. Registration writes `users`, the deterministic personal `workspaces` row,
   OWNER `workspace_memberships` row, audit evidence, and a refresh-token family
   in one transaction. Login/listing reconciles a valid old-binary user missing
   those deterministic rows and fails closed on conflicting personal rows.
3. The analyst creates an owner-scoped project and requirement, including
   keyed acceptance criteria. The API assigns UUID routing IDs and globally
   unique numeric work-item IDs to requirements and cases.
4. Requirement logic records ambiguity findings. Generation commits one
   idempotent `PENDING` claim and exact source snapshots, calls the configured
   `TestGenerationProvider` outside a database transaction, validates structured
   output, permits one classified retry, and atomically persists a complete set.
5. The analyst edits structured preconditions, synthetic data, ordered steps,
   expected results, and case metadata. Revisions and reviews preserve evidence.
6. Snapshot traceability joins cases to immutable generated criteria and
   calculates DIRECT-only primary coverage plus separate partial/supporting
   metrics. Only approved cases can be exported as CSV, JSON, or inert Markdown.
7. Audit queries remain project-owner scoped. TF-001 membership listing does not
   enable any shared project-derived read or write.

## Existing entities and database tables

The persistence model includes `users`, `refresh_token_sessions`, `workspaces`,
`workspace_memberships`, `projects`, `requirements`, `acceptance_criteria`,
`requirement_ambiguities`, `generation_runs`, `test_cases`,
`test_case_preconditions`, `test_steps`, `test_data_items`,
`traceability_links`, `generation_criterion_snapshots`,
`snapshot_traceability_links`, `test_case_reviews`, `requirement_revisions`,
`test_case_revisions`, and `audit_events`. The global
`work_item_number_seq` supplies stable external numbers for requirements and
test cases; UUIDs remain stable internal identifiers. Optimistic `version`
columns protect mutable project, requirement, ambiguity, case, workspace, and
membership records where modeled.

Important absences are not aliases hidden in current tables: there is no
persisted `RequirementAnalysis`, `CoveragePlan`, `CoverageItem`, `TestSuite`,
`ContentLock`, general `VersionSnapshot`, source-resource catalog, automation
readiness decision, platform-neutral automation draft, or Copado artifact.

## API and UI capability inventory

The API exposes authentication/session operations; caller workspace listing;
owned project CRUD/archive; requirement and acceptance-criterion management;
ambiguity resolution; generation/regeneration and run inspection; case list,
detail, edit, approve/reject/request-changes; coverage/traceability; approved
export; and project audit history. Pagination, strict unknown-field rejection,
optimistic versions, idempotency keys, correlation IDs, and RFC 7807 problems
are application contracts. Canonical generation-run, test-case, review,
revision, project, story, and audit collections are bounded pages; legacy arrays
are capped compatibility adapters.

The SPA implements the Stage 1 owner workflow and review surfaces. It does not
yet implement workspace selection/administration, source paste/upload catalog,
analysis or coverage-plan editors, suite organization, general history/restore,
content locks, retention controls, automation readiness review, or Copado draft
rendering. Those are target capabilities, not hidden routes.

## Prompt, schema, and provider integration

`GenerationService` is a nontransactional provider orchestrator;
`GenerationTransactionService` owns short claim/finalization/failure
transactions and records the pinned manual generation release tuple.
`TestGenerationProvider` is the neutral
boundary. `OpenAiTestGenerationProvider` loads the prompt and strict JSON Schema,
uses the Responses API with `json_schema` formatting and `store:false`, enforces
timeouts/output limits, parses usage, and maps safe provider failures. The
application validator independently rejects malformed enums, sizes, step
ordering, duplicate or vague content, unsafe executable language, incomplete
data, and invalid acceptance-criterion mappings. The fake provider is test-only
and runtime-boundary tests prevent it from shipping in production.

Analysis, coverage planning, manual generation, automation assessment, and
automation planning are not yet distinct durable services/prompts. Today,
ambiguity detection and manual-case drafting are coupled within the synchronous
Stage 1 generation transaction. Splitting them requires new versioned contracts
and evaluation baselines rather than renaming the existing prompt.

## Automated-test inventory

- MockMvc covers the full generation/review/traceability/export workflow,
  cross-owner 404s, CSRF/unknown fields, concurrent refresh reuse, optimistic conflicts,
  criteria and ambiguity behavior, workspace provisioning/listing, and the
  membership-does-not-share invariant.
- Workspace service tests cover deterministic provisioning, idempotency,
  old-binary reconciliation, caller role mapping, and malformed invariant
  failures. A Spring transaction test forces late refresh-token failure and
  asserts user/workspace/membership/session rollback.
- Migration coverage includes H2 fresh and V3/V4/V5-upgrade paths, PostgreSQL current
  constraints, and Testcontainers PostgreSQL upgrade fixtures through V6
  with two-user no-cross-mapping assertions.
- Provider/validator tests cover request protocol, strict parsing, transport and
  refusal failures, schema/semantic rejection, output bounds, and deterministic
  fixture quality. Runtime tests reject packaged canned output.
- Frontend Vitest/RTL/MSW tests cover authentication and the main workflow;
  Playwright covers the seeded browser path and accessibility checks.
- Documentation coverage parses every declared Java method/constructor for
  Javadoc; Spotless, SpotBugs, JaCoCo, ESLint, TypeScript, Prettier, build,
  advisory scanning, and the deterministic harness are release gates.

## Technical debt, security issues, and missing requirements

- Generation remains synchronous, but provider calls no longer hold database
  transactions and stale claims terminalize safely; process failure and multi-instance retry still need a
  durable job state machine. Rate limiting is process-local.
- Owner authorization is safe but blocks real collaboration. Workspace roles
  exist without shared-content semantics, invitations, last-owner protection,
  or inactive-member/session re-evaluation.
- Criterion evidence is immutable per generation run, but requirement ambiguity
  is not a versioned analysis artifact, and generated
  cases are not preceded by a reviewable coverage plan. There is no suite-level
  grouping or generalized traceability/version snapshot.
- Audit shares the transactional database and has no outbox/WORM forwarding.
  Retention, legal hold, purge, enterprise SSO/MFA, password recovery, email
  verification, and workspace administration remain unspecified/absent.
- Source paste/upload lacks an explicit provenance/hash catalog and parser
  isolation policy. Any future `.resource`, Python, library, selector, or
  automation import is therefore a security-review trigger.
- Production still needs distributed limits, managed secrets/private database,
  centralized redacted telemetry, recovery drills, SBOM/signing, and provider
  data-processing/residency approval.
- TestForce versus TestForge naming is unresolved. Partial renaming remains a
  correctness and operational-search risk.

## Keep

- The Java 21 Spring Boot modular monolith. Package boundaries already separate
  authentication, projects, requirements, generation, cases, traceability,
  export, audit, and the new workspace foundation without distributed-system
  overhead.
- PostgreSQL plus forward-only Flyway migrations. The normalized schema,
  foreign keys, check constraints, optimistic versions, and global work-item
  sequence provide stronger evidence than application-only validation.
- Owner-scoped repository predicates and inaccessible-resource `404` behavior.
  TF-001 deliberately preserves these checks even when two users share a
  workspace membership.
- The provider-neutral generation boundary, strict schema, semantic validator,
  controlled retry, test-only fake provider, and human approval gate.
- Typed React API contracts, MSW workflow tests, MockMvc integration tests, and
  the versioned evaluation fixtures/rubric.

## Refactor incrementally

- Ownership context must evolve from a user UUID to an explicit authorization
  context. TF-001 only adds workspace identity and project dual-write; shared
  content authorization requires a later policy slice with explicit role
  semantics and IDOR tests.
- Synchronous generation is adequate for the MVP but should move behind a
  durable job boundary before high-volume or multi-instance deployment.
- Audit evidence is useful but shares the application database. A production
  control plane should forward allowlisted events to append-only storage.
- The frontend assumes one owner workflow. A later workspace selector must be
  introduced only when server-side collaboration rules exist.

## Replace before scale

- Replace the process-local rate limiter with gateway or distributed limits for
  multi-instance operation.
- Replace Compose/demo operational assumptions with managed secrets, private
  PostgreSQL, tested backups, centralized telemetry, and signed artifacts.
- Replace ad hoc prompt/model changes with a release registry tied to fixture
  scores, semantic-validation versions, and rollback evidence.

## Remove or avoid

- Do not add client-authoritative tenant IDs, membership-based content access
  without a policy service, direct persistence-entity responses, live provider
  calls in deterministic tests, or executable automation emitted directly from
  unreviewed model output.
- Keep “TestForge” as the code, schema, environment-variable, and UI namespace
  until the product naming decision between TestForge and TestForce is resolved.
  A partial rename would create operational ambiguity.

## Readiness conclusion

The Stage 1 workflow is a credible foundation: its strongest assets are
server-side isolation, structured output validation, immutable generation
criterion evidence, durable traceability, and human review. The main
architectural gap is not technology choice; it is the absence of a complete
workspace policy, generalized cross-stage artifact snapshots, asynchronous
durable generation jobs, and an approved automation-draft contract.
