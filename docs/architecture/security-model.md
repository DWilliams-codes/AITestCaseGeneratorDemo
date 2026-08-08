# Target security model

## Principles

- Authenticate the user, authorize every resource action server-side, and keep
  the browser-provided workspace identifier non-authoritative.
- Deny by default and return `404` for inaccessible object identifiers.
- Treat requirement text, model output, imported automation, selectors, and
  exported cells as untrusted content at every boundary.
- Make privileged state transitions explicit, versioned, auditable, and safe to
  replay or reject.

## Tenancy evolution

TF-001 implements membership identity but not shared-content permissions.
`owner_id` predicates remain authoritative. A later `WorkspacePolicy` must map
active roles to explicit actions and must scope repository queries by both
workspace and resource. OWNER does not imply platform administration; ADMIN is
workspace-local; author/reviewer separation must be enforced for approvals
where policy requires it; STAKEHOLDER is read-only only after that behavior is
implemented and tested.

Membership creation, role change, suspension, and removal require actor/target
checks, last-owner protection, optimistic concurrency, audit events, session
re-evaluation, and IDOR tests. Invitation tokens must be random, hashed,
single-use, short-lived, and bound to intended workspace/email.

## Data and AI controls

Classify requirements before provider use, minimize provider payloads, keep
keys server-side, set provider retention controls, and log metadata rather than
bodies. Pin generation release tuples and reject any output that fails schema
or semantic validation. Automation drafts require a second policy boundary and
cannot execute from a provider response.

Snapshots, approvals, exports, and automation drafts link immutable revisions.
Retention and legal hold must preserve referential/audit evidence. Deletion is
a privileged workflow with quarantine and purge evidence, not a cascade exposed
directly through a user endpoint.

## Source resources, parsers, renderers, and exports

Paste/upload is a content-ingestion trust boundary. Authorize workspace/project
before reading bytes; enforce request and decompressed-size limits; normalize but
do not trust filenames; detect media type independently; compute SHA-256; scan;
and store immutable provenance plus parser/version/status. Hash equality supports
deduplication and evidence but does not make content safe. Raw and extracted
content inherit the stricter data classification and retention policy.

Parsers operate on an allowlist with bounded CPU, memory, time, recursion, and
output. They have no provider key, database credential, network, application
classpath, package manager, or writable shared filesystem. Copado `.resource`
files are parsed as non-executing data: do not run keywords, imports, variables,
listeners, dynamic expressions, or referenced paths. Reject normal ingestion of
Python, bytecode, JavaScript, shell, archives, native code, macros, and unknown
executable libraries. When investigation is required, quarantine and inspect in
a disposable offline sandbox that returns metadata only.

AI sees only approved extracted snapshots, delimited as untrusted source, with
provenance IDs rather than storage paths. Provider output cannot select a parser,
library, plugin, filesystem path, environment, or execution mode.

Renderers are pure allowlisted serializers from validated domain snapshots.
They do not execute templates or load plugins and cannot add actions absent from
the source neutral draft. Reparse and independently validate rendered artifacts,
bind a manifest/digest and source IDs, escape formulas/markup/control characters,
use safe filenames/content types, and label automation packages non-executing.
Import/deploy/run is a separately authorized external operation with review,
environment allowlist, least-privilege credentials, confirmation, and audit.

## Operational controls

Use managed secrets, TLS, private database networking, least-privilege roles,
encrypted and tested backups, distributed rate limits, centralized redacted
logs, append-only audit forwarding, dependency/SBOM scanning, signed artifacts,
and incident runbooks. Provider and automation integrations require outbound
allowlists, budgets, timeouts, circuit breakers, and kill switches.

## Required negative tests

Test cross-workspace reads and writes for every identifier path; membership
without content permission; inactive memberships; stale role changes; last-
owner removal; forged workspace IDs; prompt injection; malicious structured
output; secret/selector leakage; unsafe export content; and replayed invitations,
jobs, approvals, or automation actions.
Also test spoofed media types, malicious filenames, parser bombs, oversized
expansion, `.resource` dynamic imports, embedded Python/unknown libraries,
quarantine bypass, provenance/hash mismatch, renderer injection, semantic drift,
and any attempt for export to trigger execution or network activity.

## Implemented Stage 1 evidence boundary — 2026-08-05

New manual-generation runs capture exact criterion snapshots, the source User
Story version, and a pinned prompt/result/schema/validator/provider-adapter
tuple before provider invocation. Completion dual-writes legacy and snapshot
traceability; reads, coverage, and export use the immutable snapshot form.
Pre-V6 material is retained but explicitly marked `LEGACY_RECONSTRUCTED`.
Supporting evidence never satisfies direct coverage.

Mixed-version reads run bounded, locked, idempotent reconciliation for missing
post-V6 generation evidence. Legacy reopen events are separately reconciled by
copying the prior reason into an owner-scoped immutable case revision with
source-event provenance, then replacing the event metadata with the closed
allowlisted form. Each read performs at most one bounded bridge batch; rows
still awaiting committed transfer are response-redacted and explicitly marked
pending rather than migrated. The application never represents reconstructed snapshots or
revision content as exact historical source.

Refresh rotation is a locked, atomic predecessor transition. Replay evidence
and family revocation commit before the generic 401 and expired cookie are
returned. Address-derived limits ignore forwarding data unless one valid IP
literal arrives from a trusted socket peer; parsing performs no hostname
resolution. Audit metadata is a closed bounded value contract, and reopen
reasons live only in controlled revision evidence. Default Compose publishes
only Nginx and keeps API/database on private networks.
