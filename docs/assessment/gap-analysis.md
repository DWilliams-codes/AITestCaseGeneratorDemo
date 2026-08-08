# Gap analysis

| Existing component/domain | Target responsibility | Migration action | Dependency / release gate |
| --- | --- | --- | --- |
| User/local authentication | Stable human identity with local and optional enterprise credentials | Keep user UUID; add identity-link and verified lifecycle additively | OIDC/MFA ADR, account-link and recovery threat model |
| Refresh token sessions | Browser-session family with reuse detection | Keep; add workspace-role/session re-evaluation events | Membership policy and distributed session/revocation strategy |
| Workspace/personal membership | Tenant and role context | Complete reconciliation, invitations, lifecycle, and last-owner rules | TF-001 verification, role/action policy, IDOR matrix |
| Project `owner_id` plus nullable `workspace_id` | Workspace-scoped project aggregate | Reconcile, dual-read/measure, switch policy one operation at a time, contract later | Mixed-version window closed; backup/restore rehearsal |
| Requirement | Versioned source requirement and source-resource references | Enhance fields and relationships without changing stable ID/work-item number | Source catalog, snapshot contract, migration fixtures |
| Requirement ambiguity | Immutable `RequirementAnalysis` findings and decisions | Backfill current ambiguity rows into analysis revisions; preserve resolutions | Analysis schema/prompt/validator and eval baseline |
| Acceptance criterion | Stable criterion within requirement revisions | Preserve criterion UUID/key; link snapshots and coverage items | Requirement snapshot design and key collision policy |
| No CoveragePlan | Reviewable coverage intent before case generation | Add plan aggregate and lifecycle; generate/approve before manual cases | RequirementAnalysis accepted; coverage prompt/schema/evals |
| No CoverageItem | Atomic risk/path/criterion coverage obligation | Add item and many-to-many criterion join | CoveragePlan, stable criterion snapshot references |
| GenerationRun | Durable stage/job evidence | Extend into explicit analysis/coverage/manual/automation stage runs or common job envelope | State-machine ADR, worker/idempotency/lease design |
| No TestEnvironment/TestRun/TestStepRun/TestEvidence/action event | Controlled execution of one approved current TestCase with separate lifecycle and result | Add additive owner-scoped execution records, immutable run-bound current-TestCase snapshots, worker leases, and bounded evidence | TF-007 isolated non-production TestForge pilot, SSRF/navigation policy, separate session-provisioning approval, evidence design, owner authorization matrix |
| No execution-agent/browser boundary | Provider-neutral bounded semantic test-step interpretation | Add minimized-context agent, validated structured result, allowlisted browser driver, and visual fallback only after semantic pilot evidence | Prompt-injection containment, model/tool evaluation tuple, human safety confirmation, kill switch |
| TestCase | Manual case under requirement | Preserve UUID/work-item number while reparenting through TestSuite | Suite backfill, traceability dual-write, API compatibility |
| Preconditions/steps/data | Ordered manual-test components | Keep normalized structure; snapshot canonical payloads | TestCase/TestSuite snapshot format and renderer limits |
| No TestSuite | Named/versioned collection of manual cases | Add default suite per requirement, backfill cases, expose later | Requirement/project tenancy and stable suite identity |
| TraceabilityLink | Criterion-to-case evidence | Dual-write through coverage-item and snapshot-aware joins, reconcile, switch reads | CoveragePlan/Item and suite/case migration complete |
| TestCaseReview | Human review decision | Bind decision to immutable snapshot and enforce reviewer policy | VersionSnapshot and workspace policy |
| Requirement/TestCase revisions | Partial history | Migrate/bridge to general immutable `VersionSnapshot` | Canonical serialization/hash and restore semantics |
| No ContentLock | Short-lived edit/generation coordination | Add expiring scoped locks with fencing token, not authorization | Durable jobs, optimistic-version behavior, clock policy |
| AuditEvent | Correlated application evidence | Keep allowlist; add outbox and immutable external forwarding | Event schema, retention classification, SIEM destination |
| Prompt/schema resources | Versioned generation release assets | Split by stage; retain immutable version/hash metadata | Analysis/coverage/manual domain contracts and evals |
| OpenAI provider adapter | One untrusted drafting provider | Keep behind stage-neutral adapters; add budgets/circuit/queue worker | Durable orchestration and provider governance |
| Semantic validator | Application-owned acceptance boundary | Split into stage validators and cross-stage invariants | Versioned schemas, fixture coverage, hard-failure rubric |
| React owner workflow | Workspace-aware quality workflow | Add selectors/editors incrementally after server policy/domain endpoints | API capability and negative authorization tests first |
| Export service | Safe approved manual-test renderers | Bind exports to approved snapshots; add neutral automation draft renderer separately | VersionSnapshot, suite model, content escaping tests |
| No SourceResource catalog | Paste/upload provenance and reusable source inputs | Add immutable blob/text metadata, digest, media type, parser status, extracted safe text | Upload limits, malware/content scanning, isolation policy |
| Automation extension records only | Reviewed automation readiness and neutral draft | Preserve as the separate, secondary non-executing M10 scope after controlled execution planning | Complete requirement→coverage→manual vertical slice and automation security/evaluation gates |
| No Copado artifact | Version-pinned non-executing vendor package | Serialize reviewed neutral draft; never execute/import implicitly | Copado schema contract, approved org maps, security review |
| H2/PostgreSQL/Flyway | Rehearsed expand/backfill/contract data evolution | Keep forward-only migrations and dialect tests | Java 21/Maven/Docker verification and rollback evidence |
| Process-local limits/operations | Multi-instance production control plane | Replace limiter, add durable jobs, telemetry, backups, outbox | SLO/capacity model and production landing-zone review |
| TestForge naming | One consistent product/operational namespace | Decide TestForce vs TestForge, then migrate atomically | Naming ADR, compatibility/redirect/search plan |

## Sequencing rule

Do not collapse these gaps into one migration. Land additive schema first,
dual-write, measure and reconcile, switch reads behind explicit policy, then
contract old columns only after the rollback window closes. Generation and
automation changes require separate evaluation evidence from tenancy changes.

The product dependency order for the future artifact roadmap is: requirement
enhancement → persisted RequirementAnalysis → CoveragePlan/CoverageItem and
criterion joins → TestSuite and manual-case restructuring → traceability
dual-write/migration → general VersionSnapshot and ContentLock → one complete
reviewed requirement-to-manual-test vertical slice → automation readiness/neutral
draft → Copado adapter. Separately, the smallest TF-007 pilot can perform
controlled approved-step execution against isolated TestForge using current
owner-scoped authorization and an immutable run-bound current-TestCase snapshot;
it does not depend on M2–M8, general snapshots, or workspace sharing.
Execution-environment safety and durable job orchestration are cross-cutting
gates for that pilot; tenancy policy and source-resource safety remain gates for
their own future dependent endpoints.

## Product naming gap

Planning material refers to “TestForce AI” while the implemented product is
“TestForge AI.” This is unresolved. New code and operational contracts continue
to use TestForge until a dedicated, end-to-end naming decision and migration is
approved.
