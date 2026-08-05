# TF-004: Critical implementation comments

## Status

Completed — The Lead declared TF-004 implementation complete on 2026-08-04 after
the final Architect returned `CONFORMS` and the independent Reviewer returned
`APPROVE` with no findings. The comment-only implementation and both wording
remediation loops preserved executable behavior. The canonical local wrapper's
unchanged frontend checkout-line-ending limitation remains recorded below and
does not represent a TF-004-authored change.

## Source request

Replace malformed or boilerplate comments at critical backend seams with concise
comments that explain lifecycle invariants, compatibility boundaries, security
ordering, and generation/provider safety. Make comment-only changes in the exact
approved inventory and preserve all executable behavior.

## Objective

Leave future maintainers a small set of durable, high-value comments explaining
why non-obvious ordering and compatibility rules exist. Every non-plan diff hunk
must change comments only; code, annotations, signatures, tests, migrations,
prompts, schemas, and runtime behavior must remain identical.

## Non-goals

- No executable code, signature, annotation, test assertion, prompt, schema,
  frontend, dependency, CI, or configuration change.
- No edit to V1–V5 migration SQL; all applied migrations remain byte-identical.
- No broad Javadoc cleanup, comment-style normalization, or unrelated refactor.
- No staging, commit, push, publication, pull-request mutation, deployment,
  branch-protection change, or external-system mutation.
- No edit to the protected local MVP plan or any running runtime/browser state.

## Approved comment invariants

1. Lifecycle comments explain state meaning, ownership, and valid ordering at
   the enum/entity seams without restating identifiers or method bodies.
2. Migration comments explain why null priority remains a rollback-compatible
   `MEDIUM` at the application boundary and why fresh-schema versus V4-to-V5
   PostgreSQL tests cover distinct contracts.
3. Authentication and security comments explain token hashing/rotation,
   revocation and persistence ordering, rate-limit trust boundaries, and the
   browser security configuration choices that must not be weakened.
4. Generation comments explain idempotency/supersession ordering, validation
   before persistence, provider-boundary trust, and schema-constrained output
   without exposing hidden prompts or duplicating implementation mechanics.
5. Comments remain concise, local to the critical seam, and encode why/order or
   an invariant. Boilerplate comments that only paraphrase a declaration are
   removed or replaced rather than expanded.

## Exact affected-file inventory

- Lifecycle:
  `backend/src/main/java/com/testforge/testcase/domain/TestCaseStatus.java`,
  `backend/src/main/java/com/testforge/testcase/domain/ReviewDecision.java`,
  `backend/src/main/java/com/testforge/testcase/domain/TestCaseEntity.java`, and
  `backend/src/main/java/com/testforge/generation/domain/GenerationStatus.java`.
- Migration compatibility:
  `backend/src/main/java/com/testforge/requirement/domain/RequirementEntity.java`,
  `backend/src/test/java/com/testforge/PostgreSqlMigrationIntegrationTest.java`,
  and
  `backend/src/test/java/com/testforge/PostgreSqlWorkspaceUpgradeIntegrationTest.java`.
- Authentication and security:
  `backend/src/main/java/com/testforge/auth/application/JwtService.java`,
  `backend/src/main/java/com/testforge/auth/application/AuthService.java`,
  `backend/src/main/java/com/testforge/auth/domain/RefreshTokenEntity.java`,
  `backend/src/main/java/com/testforge/security/RateLimitService.java`, and
  `backend/src/main/java/com/testforge/config/SecurityConfiguration.java`.
- Generation/provider:
  `backend/src/main/java/com/testforge/generation/application/GenerationService.java`
  and
  `backend/src/main/java/com/testforge/generation/provider/OpenAiTestGenerationProvider.java`.
- Planning evidence: this ExecPlan only.

## Ordered implementation

1. Re-read this materialized plan, then inspect the exact inventory and current
   diff so existing work is preserved.
2. Replace only malformed or boilerplate comments at the approved lifecycle,
   migration, authentication/security, and generation/provider seams.
3. Prove every non-plan zero-context hunk is comment-only and that V1–V5 SQL is
   byte-identical to `HEAD`.
4. Run the full deterministic repository wrapper, formatting/diff checks, and
   status inventory without installs, live provider calls, or generated-code
   execution.
5. Record actual commands, results, skips, deviations, and residual risks here,
   then return the active plan and diff for Architect and Reviewer gates.

## Acceptance criteria

- Only files in the exact inventory plus this active plan change.
- Every non-plan hunk changes comment text only; executable tokens are unchanged.
- Comments explain why, ordering, trust boundaries, or compatibility invariants
  and do not merely narrate nearby code.
- V1–V5 migration SQL remains byte-identical to `HEAD`.
- `git diff --check` and the full `.\scripts\verify.ps1` wrapper pass, or any
  environment limitation is recorded exactly.
- No generation contract or evaluation behavior changes; no live model call is
  made and no evaluation fixture update is needed.
- Architect returns `CONFORMS`, independent Reviewer returns `APPROVE`, and the
  Lead decides implementation completion before this plan may move to
  `completed/`.

## Security, privacy, and generation impact

This is documentation-only implementation work. It must preserve owner-scoped
authorization, CSRF/CORS behavior, token secrecy and rotation, rate-limit
semantics, provider isolation, structural/semantic validation, idempotency,
generation history, and the non-executing automation boundary. Comments must not
contain secrets, active tokens, customer data, provider payloads, hidden prompts,
hidden reasoning, or production selectors.

## Risks and mitigations

- Comment/code drift: anchor each comment to an observed invariant and reject
  any wording not directly supported by the unchanged implementation.
- Accidental behavioral edit: inspect zero-context diffs and run the complete
  deterministic wrapper.
- Applied-migration mutation: run an exact migration-tree diff against `HEAD`.
- Scope creep: preserve the exact inventory and avoid broad Javadoc cleanup.

## Rollback

Before publication, revert only the comment hunks and this active plan. No data,
API, executable, or migration rollback is required because runtime behavior must
not change.

## Actual evidence

- First-write invariant satisfied: this active plan was created and re-read in
  full before any implementation comment changed.
- The exact 14-file Java/test inventory received only concise comment changes at
  lifecycle, migration compatibility, authentication/security, and
  generation/provider boundaries. `git diff -U0` classified all 93 changed
  non-plan source/test lines as Javadoc text or a Javadoc delimiter; no
  executable token, signature, annotation, assertion, prompt, schema, or
  frontend line changed.
- `mvn spotless:apply` with the existing portable Java 21/Maven 3.9.16 runtime
  passed and normalized only the 14 touched Java/test files. No dependency was
  installed.
- `python scripts/validate-harness.py` passed: 3 specialist profiles, 6 skills,
  6 blocking manual fixtures, and 3 non-blocking automation roadmap fixtures.
- `git diff --exit-code HEAD -- backend/src/main/resources/db/migration` exited
  zero: V1-V5 remain byte-identical. `git diff --check` also exited zero.
- The canonical `.\scripts\verify.ps1` run passed the harness and complete
  backend Maven lifecycle: compilation and packaging; 39 tests with zero
  failures/errors and two Docker-unavailable PostgreSQL skips; Spotless;
  SpotBugs with zero findings; and all JaCoCo thresholds. It then stopped at
  frontend `npm run format:check`, which reported line-ending differences in 15
  unchanged frontend files. None of those files appears in the working-tree
  inventory and TF-004 did not alter them.
- The remaining frontend gates were run individually and passed: `npm run lint`,
  `npm run typecheck`, `npm run test:coverage` (21/21 tests; 86.28% statements,
  69.29% branches, 81.37% functions, and 87.37% lines), and `npm run build`.
  Diagnostic `npm run format:check -- --end-of-line auto` also passed, isolating
  the wrapper blocker to checkout line endings without modifying frontend files.
- Final status inventory is limited to the 14 approved modified Java/test files
  plus this untracked active plan on `main`; nothing is staged, committed,
  pushed, published, or externally mutated.
- Architect review returned `BLOCK` on three wording-accuracy findings: the
  review-transition comment used the ambiguous phrase "negative evidence," the
  lifecycle enum did not state the NEEDS_REVISION-to-IN_REVIEW direction, and
  the CORS comment incorrectly implied headers were operator-configured. The
  same sole Builder replaced only those three comments with the exact rationale,
  directional transition, and configured-origin/fixed-header invariants.
  `mvn spotless:apply spotless:check` passed and changed only those three touched
  Java files; focused `mvn test -Dtest=TestCaseEntityTest` passed 2/2 tests. Full
  `mvn verify -Dspring-boot.repackage.skip=true` passed 39 tests with zero
  failures/errors and two Docker-unavailable PostgreSQL skips, Spotless,
  SpotBugs with zero findings, and all JaCoCo thresholds. The deterministic
  harness passed again. Final `git diff -U0` retained the 14-file Javadoc-only
  proof, the V1-V5 migration-tree comparison and `git diff --check` exited zero,
  and status remained limited to the approved inventory plus this active plan.
  The unchanged frontend EOL baseline was not touched or needlessly rerun.
  Repeat Architect and independent Reviewer read-only gates remain pending.
- Independent Reviewer review returned `BLOCK` on one remaining wording finding:
  `configuredFlyway` was described only as stopping at historical versions even
  though its nullable target also selects the latest migration. The same sole
  Builder restored the optional-target semantics in that helper Javadoc without
  changing Flyway configuration or test behavior. Focused remediation checks
  passed: the deterministic harness; `mvn spotless:apply spotless:check`, which
  changed only the touched PostgreSQL upgrade test; and focused
  `mvn test -Dtest=PostgreSqlWorkspaceUpgradeIntegrationTest`, which compiled
  and completed with one discovered test, zero failures/errors, and one honest
  Docker-unavailable skip. Final `git diff -U0` still classifies all 93 changed
  non-plan lines as Javadoc only; V1-V5 remain byte-identical; `git diff
  --check` exited zero; and the TF-004-authored status remains limited to the
  approved inventory plus this active plan. Repeat read-only review remains
  pending.
- Final read-only gates passed after the Reviewer remediation: Architect
  `CONFORMS`; independent Reviewer `APPROVE` with no findings. The Lead then
  declared TF-004 implementation complete, and the same sole Builder moved this
  evidence record from `active/` to `completed/`. No staging, commit, push,
  publication, deployment, branch-protection change, or external-system
  mutation occurred during closeout.
- Final closeout checks passed: `git diff --check` and the V1-V5 migration-tree
  diff exited zero; the active path no longer exists and this completed path
  does; and the ignored protected local MVP plan retained its recorded SHA-256
  `5B8479A30D7179AC301DD68F6A4E0720305CC9EF7AC0486807E7EF1BB414732F`.
  Status remained the 14 approved comment-only Java/test modifications, this
  completed plan, and the separately preserved unrelated untracked review-
  findings directory.

## Deviations

The Architect handoff named four auth/security classes without their intermediate
package directories. Read-only path discovery resolved the existing canonical
locations and this plan records them; the named-class scope did not change. The
default host Java is 11 and Maven is not global, so verification used the
existing portable Java 21/Maven 3.9.16 runtime. The canonical wrapper's frontend
format gate remains blocked by unchanged working-copy line endings; TF-004 did
not modify or normalize out-of-scope frontend files. During the final remediation
status check, an unrelated untracked `codebase-review-findings-2026-08-04/`
directory appeared concurrently; TF-004 did not create or modify it, and it
remains preserved and excluded from this plan's scope.

## Residual risks

The source/comment delta is locally compiled, tested, statically analyzed, and
format-clean. The full wrapper cannot be reported as passing until the existing
frontend checkout line-ending condition is resolved outside TF-004 scope.
Docker-backed PostgreSQL tests remain skipped locally. No read-only review
finding remains; exact-SHA publication evidence is separate and was not
requested during implementation closeout.
