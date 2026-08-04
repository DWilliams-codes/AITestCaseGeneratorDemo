# TF-001: Workspace tenancy foundation

## Status

Completed — implementation, architect/reviewer remediation, and supported
verification are complete on `codex/tf-001-ci-remediation` at commit
`f7549590372c438473c944053d8a26a5999fcf90`. All seven required gates passed in
[GitHub Actions run 30918173119, attempt 2](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/attempts/2).

## Objective

Add the expand-only workspace and membership foundation without changing the
existing Stage 1 owner-scoped authorization model. Registration provisions a
personal workspace, new projects reference that workspace while retaining
`owner_id`, and callers can list only workspaces where they are members. A
membership does not grant access to another user's projects, requirements, test
cases, generation runs, traceability, exports, or audit data in TF-001.

## Acceptance criteria

- Flyway V4 creates `workspaces` and `workspace_memberships` with UUID primary
  keys, bounded status/role values, audit/timestamp/version fields, a unique
  `(workspace_id, user_id)` membership, foreign keys, and query indexes.
- V4 adds nullable `projects.workspace_id`, deterministically creates one
  personal workspace and OWNER membership per existing user using the user's
  UUID for both new identifiers, and backfills projects from `owner_id`.
- `projects.workspace_id` remains nullable as the old-binary rollback bridge;
  all new application-created projects dual-write `owner_id` and `workspace_id`.
- Workspace entity, membership entity, enums, repositories, service, DTO, and
  controller provide `GET /api/v1/workspaces`, scoped to caller memberships and
  including the caller's role.
- Registration transactionally creates the user, personal workspace, OWNER
  membership, and token family or rolls the operation back.
- Project API responses and the frontend `Project` type expose `workspaceId`.
- Existing owner checks and inaccessible-resource `404` behavior remain
  authoritative; workspace membership does not enable content sharing.
- H2 migration tests cover fresh startup and deterministic V3-to-V4 legacy
  backfill. PostgreSQL integration checks cover V4 and constraints where the
  environment permits, including the nullable compatibility bridge.
- Service/API tests cover registration provisioning, membership-scoped listing,
  project workspace IDs, and outsider project/requirement/test-case denial.
- Every added Java constructor and method has intent-level Javadoc and the
  documentation coverage check passes.
- Architecture, assessment, migration, security, AI/automation, product, API,
  testing, ADR, roadmap, README, and agent guidance describe implemented versus
  future behavior without changing generation contracts.

## Explicit non-goals

- No shared project/content authorization, workspace invitation UI, membership
  mutation endpoint, or role-based content permission in this slice.
- No generation prompt, schema, provider, model, evaluation behavior, or live
  provider call.
- No contract migration that removes `projects.owner_id` or makes
  `projects.workspace_id` non-null.
- No version snapshots/restoration, retention engine, Copado generator, or
  automation execution; documentation may describe these only as target-state
  future slices.
- No local dependency installation or production deployment; Git publication is
  delivery activity, not TF-001 product scope.

## Source contracts to inspect before implementation

- V1–V3 Flyway migrations and H2/PostgreSQL migration tests.
- User, project, authentication, requirement, and test-case entities,
  repositories, services, DTOs, controllers, and authorization integration tests.
- Registration transaction boundaries, demo seeding, project creation, and
  frontend project response mapping.
- Existing product, architecture, API, testing, security, threat-model, ADR, and
  orchestration documentation.

## Planned implementation

1. Inspect source/database contracts and record compatibility assumptions.
2. Add V4 expand/backfill migration and migration verification.
3. Add workspace domain, persistence, application, DTO, controller, and tests.
4. Provision personal tenancy during registration and dual-write new projects.
5. Expose `workspaceId` through backend/frontend response types without changing
   owner-scoped access predicates.
6. Add substantive current-state assessment, gaps, migration risks, target
   architecture/domain/AI/automation/security documents, ADR 0009, and umbrella
   roadmap; minimally update canonical documentation and AGENTS.md.
7. Run deterministic harness, frontend checks, Java/Maven backend verification,
   H2 and PostgreSQL migration checks where available, and whitespace checks.
8. Record exact evidence, deviations, known failure states, and residual risks.
   Keep this plan active if backend/migration verification cannot run.

## Compatibility and rollback

V4 is expand/backfill only. Old binaries continue reading/writing owner-scoped
projects because `owner_id` is unchanged and `workspace_id` remains nullable.
New binaries dual-write both. Rollback means deploying the old binary while
leaving V4 schema/data in place; Flyway migrations are not reversed in place.
Later contract work may enforce non-null workspace ownership only after mixed-
version operation, backfill verification, and rollback windows are closed.

## Validation plan

- `python scripts/validate-harness.py`
- `mvn --batch-mode --no-transfer-progress verify` from `backend` on Java 21 and
  Maven 3.9+
- Targeted H2 legacy/fresh migration tests and PostgreSQL migration integration
  tests included in the Maven lifecycle
- `npm run format:check`, `lint`, `typecheck`, `test:coverage`, and `build` from
  `frontend`
- `git diff --check`

The completed supported-environment evidence is recorded below.

## Risks and known failure states

- An incomplete backfill could leave a new-binary project without workspace
  context; V4 must make the backfill deterministic and tests must inspect rows.
- Unique/FK/check differences between H2 and PostgreSQL can hide portability
  errors; both dialect paths need verification.
- Accidentally replacing owner predicates with membership checks would broaden
  access before role semantics are ready and is a blocking regression.
- Registration must remain atomic so a user cannot exist without their personal
  workspace/membership or vice versa.
- Mixed old/new binaries can create null `workspace_id` rows; application reads
  must tolerate the nullable bridge and a future reconciliation step must be
  documented.
- Target-state docs can be mistaken for implemented behavior; every future
  capability must be labeled explicitly.

## Definition of done

All additive contracts and required documents are implemented, every existing
owner isolation guarantee remains tested, H2 and PostgreSQL migration behavior
is evidenced to the extent the repository environment supports, full Java 21 /
Maven and frontend verification pass, no generation contract changes occur, and
independent architect/reviewer remediation has no remaining blocking finding.
The supported CI evidence below satisfies this definition of done.

## Implementation progress

- Added Flyway V4 with workspace/membership constraints, deterministic personal
  tenancy backfill, nullable project bridge, foreign keys, and indexes.
- Added workspace entities, lifecycle/role enums, repositories, service, DTO,
  and membership-scoped `GET /api/v1/workspaces` controller.
- Registration now provisions personal tenancy within its transaction. Project
  creation derives the personal workspace server-side and dual-writes it while
  retaining every owner-scoped repository predicate.
- Project responses and the frontend type expose nullable `workspaceId`.
- Added service tests, fresh/legacy migration tests, PostgreSQL constraint
  assertions, and an API denial scenario where a workspace member still gets
  `404` for another owner's project, requirement, and test case.
- Added the assessment, gap, migration, target architecture/domain/AI/
  automation/security, ADR, roadmap, and canonical documentation updates.
  The full `docs/plans/testforce-ai-mvp-execplan.md` is intentionally local-only
  and excluded from the TF-001 publication commit by request.

## Architect-review remediation

- Login and workspace listing now reconcile deterministic personal tenancy for
  users written by an old binary after V4. Reconciliation validates workspace
  ID/creator/status and membership ID/workspace/user/creator/status/OWNER role,
  creates missing rows idempotently, and fails closed on malformed or orphaned
  deterministic rows.
- Unit coverage now specifies old-binary listing reconciliation, idempotency,
  malformed workspace, malformed membership, and orphan membership behavior.
- A Spring integration test forces refresh-session persistence failure after
  user/workspace/membership creation and asserts the entire registration
  transaction leaves user, workspace, membership, and refresh-token counts
  unchanged.
- A dedicated Testcontainers test builds a two-user/two-project PostgreSQL V3
  fixture, migrates V4, asserts explicit applied versions `1,2,3,4`, verifies
  deterministic OWNER rows and per-owner project mapping, and rejects cross-user
  or orphan results. Existing H2/PostgreSQL tests now assert ordered versions
  instead of only a migration count.
- Reference documentation now inventories current structure/flow/data/API/UI/
  provider/tests/debt; maps each existing component to target migration actions;
  defines the complete target domain/lifecycles/invariants; separates six AI
  stages and validators; defines safe resource provenance/non-executing parsing
  and renderer/export controls; and gives the local roadmap explicit ordered
  milestones with dependencies, acceptance, rollback, tests/evals, and operations.
- Final review remediation adds the exact Requirement/RequirementContext fields
  and directive-defined Requirement, CoveragePlan, TestSuite, TestCase, and
  AutomationArtifact state sets with explicit current-to-target mappings. The
  local roadmap M2 and downstream milestones use those literal contracts.
- Old-binary reconciliation now obtains a `PESSIMISTIC_WRITE` lock on the user
  row before deterministic checks/inserts. A two-caller Spring integration test
  synchronizes simultaneous requests and requires both to succeed with exactly
  one workspace and OWNER membership; there is no catch-and-ignore uniqueness
  path.
- Reviewer follow-up strengthened that concurrency test: a TransactionTemplate
  transaction explicitly acquires the same user-row lock, provisions tenancy,
  and holds the transaction open; only then does the second reconciliation
  start, and its Future must time out before the first transaction is released.
  Both calls must then complete and the exact-one assertions must hold.
- Registration rollback evidence now also asserts audit-event counts are
  unchanged after the forced late refresh-token persistence failure.

## Actual results — 2026-08-04

The canonical supported-environment evidence is
[GitHub Actions run 30918173119, attempt 2](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/attempts/2)
on `codex/tf-001-ci-remediation` at
`f7549590372c438473c944053d8a26a5999fcf90`. All seven gates passed.

| Gate | Result |
| --- | --- |
| [Backend verify](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/job/92028391289) | Passed on Java 21 and Maven: 31 of 31 tests passed; H2 fresh and V3→V4 migrations, PostgreSQL fresh and V3→V4 upgrade, pessimistic-lock concurrency, registration rollback, Spotless, SpotBugs, JaCoCo, and the Maven lifecycle succeeded |
| [Dependency security](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/job/92028342825) | Passed: OWASP Dependency-Check acquired the NIST annual and modified JSON 2.0 feeds, defragmented its database, emitted HTML and JSON reports, and completed the Maven build in 6:07; the complete database cache was saved and the frontend npm audit succeeded |
| [Frontend verify](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/job/92028344026) | Passed locked install, lint, formatting, type checking, unit coverage, and production build |
| [Agent and evaluation harness](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/job/92028344097) | Passed deterministic orchestration/evaluation validation, patch whitespace, and PowerShell wrapper parsing |
| [Repository secret scan](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/job/92028343516) | Passed the full-history Gitleaks scan |
| [Container vulnerability scan](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/job/92028344142) | Passed release-image builds and backend/frontend Trivy enforcement |
| [Docker end-to-end and accessibility](https://github.com/DWilliams-codes/AITestCaseGeneratorDemo/actions/runs/30918173119/job/92028344456) | Passed Compose validation, application build/start, deterministic Playwright checks, accessibility checks, and cleanup |

No dependencies were installed on the local workstation, no live generation or
provider call was made, and no generation or evaluation contract changed.

## Publication audit

The explicit publishable-file allowlist was staged successfully. The completed
index audit produced the following evidence:

- `git diff --cached --check` passed across 43 staged files.
- `git diff --cached --name-only` confirmed that
  `docs/plans/testforce-ai-mvp-execplan.md` is excluded from the staged patch.
- `git ls-files --others --exclude-standard` returned that local-only roadmap as
  the sole untracked file.

Before this plan-only path move, the final closure audit reconfirmed that
`docs/plans/testforce-ai-mvp-execplan.md` was the sole untracked file. It remains
excluded from the publishable TF-001 changes.

## Deviations and residual risks

- The local workstation still has Java 11 and lacks Maven and Docker, so it
  cannot reproduce the supported backend/container lifecycle. The canonical CI
  run above supplies that Java 21, Maven, PostgreSQL, container, and browser
  evidence without installing local dependencies.
- Dependency-security attempt 1 failed closed because the annual NIST feeds were
  available but `nvdcve-2.0-modified.json.gz` temporarily returned HTTP 404; no
  partial cache was saved. Attempt 2 succeeded after that exact gzip returned
  HTTP 200 and saved a complete cache. Future cold-cache scans still depend on
  NIST feed availability.
- `projects.workspace_id` intentionally remains nullable as the mixed-version
  rollback bridge. Reconciliation remains required until a future contract plan
  closes the rollback window and proves a deterministic non-null transition.
- Workspace membership remains listing-only in TF-001 and does not grant access
  to another owner's projects or related content.
- The intentionally untracked umbrella roadmap must remain excluded from
  publishable TF-001 changes.
- No generation prompt, schema, provider, model, or evaluation behavior changed,
  and no live provider call was made.
