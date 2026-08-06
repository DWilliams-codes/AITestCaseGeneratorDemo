# Migration risks

## TF-001 workspace expand/backfill risks

| Risk | Detection | Mitigation / rollback |
| --- | --- | --- |
| Existing user lacks a personal workspace | Compare users to deterministic workspace and OWNER-membership IDs | V4 backfills from user UUID; new code self-heals before project creation |
| Existing project remains unassigned | Count non-null owners with null workspace after V4 | Deterministic `workspace_id = owner_id` backfill; keep column nullable during mixed-version operation |
| Old binary writes a null workspace | Reconciliation query and metric | New reads tolerate null; redeploy old binary without reversing V4; rerun idempotent reconciliation in a later migration/tool |
| Authorization broadens accidentally | Cross-owner API test with an explicit shared membership | Keep owner predicates authoritative in TF-001; block release on any non-404 result |
| H2 hides PostgreSQL behavior | Dialect-specific migration and constraint assertions | Run both H2 fresh/legacy tests and Testcontainers PostgreSQL verification |
| Registration partially provisions | Transactional integration test and orphan queries | User, workspace, membership, audit, and session share one Spring transaction |

## Staged migration order

1. Expand: add workspace tables and nullable `projects.workspace_id`.
2. Backfill: create deterministic personal tenancy and assign existing projects.
3. Dual-write: new registrations and projects populate both ownership models.
4. Observe: reconcile nulls, duplicates, orphan rows, latency, and denial tests.
5. Migrate authorization in a separate release behind an explicit role policy.
6. Contract only after old binaries are excluded and rollback no longer depends
   on `owner_id` or nullable workspace values.

Flyway migrations are never rolled back destructively in place. Application
rollback leaves V4 schema/data present and restores the previous binary. A
future contract migration needs a backup, restore rehearsal, reconciliation
report, release freeze, and a separately approved ExecPlan.

## Failure states

Startup must fail if Flyway cannot apply or Hibernate validation detects a
schema mismatch. A missing membership during new project creation is repaired
only for a valid existing user. Orphaned memberships are treated as invalid
server state, not silently omitted. No membership mutation is exposed in
TF-001, limiting the reachable inconsistency surface.

## TF-005 generation-evidence expansion — 2026-08-05

| Risk | Detection | Mitigation / rollback |
| --- | --- | --- |
| Prior run lacks exact source criteria | Snapshot reconciliation compares every successful run with snapshot rows | V6 backfills then-current criteria and marks them `LEGACY_RECONSTRUCTED`; never represent them as exact |
| Old binary writes only legacy links after V6 | Reconcile successful runs missing snapshot links/snapshots | New binary reconstructs visibly and continues dual-write; old binary ignores nullable columns/new tables |
| Criterion mutation races generation claim | Concurrent lock test and stale `If-Match` API test | Both operations lock the owning User Story; mutation stores a complete pre-change revision and advances aggregate version |
| New binary fails after provider response | Assert terminal run and child evidence are atomic | One short finalization transaction persists all cases/parts/links/audit/state or rolls back all of them |
| Rollback binary cannot write V6 schema | V5-shaped insert fixture against V6 | All new columns remain nullable, no prior column/table is removed, and legacy traceability remains writable |

V6 is expand/backfill/dual-write/read-switch only. Contracting legacy links,
making release fields non-null, or removing array compatibility routes requires
a later observed reconciliation release and a separately approved ExecPlan.
