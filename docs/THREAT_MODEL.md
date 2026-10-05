# STRIDE Threat Model

## Assets and trust boundaries

Protected assets are credentials, refresh-token families, owner-scoped project data, requirements, generated cases, review decisions, audit evidence, database secrets, and provider keys. Trust boundaries exist between browser and API, API and database, API and external provider, and operator configuration and each runtime container.

| Asset | STRIDE threat | Attack path | Impact | Implemented mitigation | Residual risk / production action |
| --- | --- | --- | --- | --- | --- |
| User account | Spoofing | Password guessing, credential stuffing, or forged access token | Account takeover and unauthorized quality records | Argon2id, generic auth errors, minute-window rate limit, HS256 with a 32-byte minimum key, issuer/audience/time validation | Add gateway/IP reputation plus MFA or enterprise OIDC |
| Refresh-token family | Spoofing | Theft, replay, or concurrent reuse of a browser refresh cookie | Persistent account takeover or two usable successors | Random opaque tokens, SHA-256 hashes at rest, HttpOnly SameSite Secure cookies outside the explicit loopback-only local Compose exception, pessimistically locked rotation, committed family reuse detection/revocation before one generic 401 that expires the cookie | Monitor reuse events and shorten TTL by risk tier |
| Client address boundary | Spoofing / denial of service | Caller supplies forged or chained forwarding headers to evade or consume address buckets | Rate-limit bypass or denial of service against another client | Trust one sanitized literal only from configured socket-peer CIDRs; Nginx overwrites `X-Forwarded-For`; account/token digest adds a second bounded key | Configure real ingress CIDRs exactly and use a distributed gateway limiter before scale-out |
| Browser auth epoch | Spoofing / elevation | A refresh or CSRF request from a logged-out session completes after a new auth action | Old credentials are restored into browser memory | One epoch-based reset clears access/CSRF state and coalesced promises; stale completions cannot install tokens | Add abort signals where browser support and observability justify them |
| Owner-scoped records | Tampering / elevation | Substitute another user's project, requirement, case, run, export, or audit identifier | Cross-tenant read or mutation | Owner predicates in services and repositories, inaccessible objects return 404, automated IDOR tests | Add explicit organization membership and role policy before sharing |
| Workspace membership | Elevation / information disclosure | Treat membership or a caller-supplied workspace ID as implicit access to owner-scoped content | Cross-owner data exposure before role policy exists | Membership only scopes workspace listing; project creation derives personal workspace server-side; a shared-member integration test still requires 404 for project, requirement, and case | Define an explicit role/action policy, invitation lifecycle, inactive-member handling, and last-owner protection before shared reads |
| Requirement and case revisions | Tampering | Submit a stale edit over a newer version | Lost evidence or reviewer changes | Optimistic versions, `409 stale_version`, immutable case revisions | Add side-by-side diff and merge UX as concurrency grows |
| Generation-set history | Tampering | Edit source criteria during/after generation, edit/review a superseded case, or let a failed run become active | Historical evidence changes or active coverage becomes inconsistent | Immutable criterion snapshots and snapshot links with source version/provenance, captured criterion UUID for rename-compatible dual write, fail-atomic deletion outcome, one deterministic successful-run resolver, active-only mutations/default reads, explicit supersession confirmation | Pre-V6 history is irrecoverable and visibly labeled `LEGACY_RECONSTRUCTED`; later contraction requires reconciliation evidence |
| Test-data references | Tampering | Duplicate normalized names or dangling/ambiguous step references enter persistence | A manual case becomes non-reproducible or misleading | Shared pre-mutation semantic policy for provider output and edits; UI dropdown plus explicit rename/delete handling | Add database-level normalized-name constraints if parts gain independent write APIs |
| Generated evidence | Tampering | Compromised provider emits malicious, structurally valid-looking, coerced, poisoned, or generic criterion-placeholder output | Incorrect tests, unsafe data, or misleading coverage | Exact three-field wire parsing rejects provider-authored transport data and scalar/enum coercion; strict schema plus enum, size, separate contiguous actionable setup/readiness and individual evidence procedures, mapping, duplicate, generic-action, vague-language, and executable-content validation; one controlled retry | Maintain provider evals, anomaly metrics, and an emergency provider-disable switch |
| Audit evidence | Repudiation / tampering | Reviewer disputes a decision, an old binary writes a reopen reason into metadata, or a database operator modifies audit rows | Loss of accountability or sensitive free text in a broad event stream | Immutable review rows, revisions, correlated audit events, server timestamps, minimal allowlisted metadata; one bounded compatibility batch per read transfers legacy reopen reasons to controlled revisions, while a response guard redacts any still-pending row without claiming migration | Stream to append-only SIEM or WORM storage; protect database administrator access |
| Access token | Information disclosure | XSS or browser storage theft | Requests within the token lifetime | Access token exists in memory only; refresh cookie is inaccessible to JavaScript; CSP and output encoding | XSS can act during the short access-token lifetime; maintain CSP and dependency hygiene |
| Cleartext local Compose frontend | Information disclosure / tampering | A broad host-port mapping exposes local-profile credentials, cookies, and bearer traffic to a LAN peer | Credential, token, and owner-scoped data disclosure or modification | Exactly one IPv4-loopback frontend publication is enforced by the deterministic harness; backend and PostgreSQL remain unpublished | Host-local malware, Docker-network peers, and any non-loopback deployment remain outside this control; the latter requires TLS and secure cookies |
| Color-mode preference | Information disclosure | Browser storage is inspected or replaced with malformed content | Minor presentation disclosure or broken appearance | Only the allowlisted `testforge-color-mode` enum is persisted; invalid/unavailable storage falls back to system and never stores auth, identity, workspace, query, or generated data | Browser extensions can still observe this non-sensitive preference |
| Provider key and requirement content | Information disclosure | Browser exposure, excessive provider payload, provider retention, or provider compromise | Secret loss or disclosure of proprietary requirements | Key is server-side only; minimized payload excludes identity, UUIDs, correlation IDs, tokens, and hidden reasoning; `store:false` | Complete vendor DPA, residency, retention, and breach-response review; classify requirements before use |
| Application logs | Information disclosure | Request bodies, authorization headers, secrets, or provider payloads enter logs | Credential or proprietary-data disclosure | No body logging, generic provider/auth errors, allowlisted audit metadata, normalized correlation IDs | Verify proxy, APM, container, and cloud logging redaction |
| Database | Information disclosure / tampering | Stolen database credential, exposed port, backup theft, or excessive application-role privileges | Full tenant-data disclosure or audit manipulation | Container-private network, externalized credentials, parameterized JPA access, schema constraints, least-function application path | Use a managed private database, TLS, encryption at rest, restricted backup access, rotation, and a formally least-privileged role |
| Provider budget and availability | Denial of service | Authentication abuse, oversized input, repeated generation, or slow provider responses | Cost growth and service unavailability | Per-subject limits, body/collection bounds, provider connect/read/output caps, bounded DB pool, required idempotency key | Add distributed gateway limits, per-account quotas, cost alerts, circuit breaking, and queueing before scale-out |
| Cookie-authenticated endpoints | Elevation of privilege | Cross-site request forgery invokes refresh, logout, login, or registration | Session manipulation | SameSite cookies, double-submit CSRF cookie/header, exact credentialed CORS | Keep origins exact, enforce TLS, and regression-test proxy behavior |
| Generation policy | Elevation of privilege | Prompt injection in a story or criterion asks the model to reveal secrets or ignore policy | Policy bypass or data leakage | Source is delimited as untrusted data; versioned developer prompt; no tools; strict structured-output validation | Run a maintained injection corpus for every prompt, schema, model, or provider upgrade |
| Export consumers | Content injection | User-controlled cell starts with `=`, `+`, `-`, or `@`, or Markdown contains links, images, HTML, autolinks, references, code, protocols, or multiline structure | Spreadsheet formula execution or misleading/active rendered output | Approved-only export, CSV formula-prefix neutralization, conservative punctuation-level Markdown encoding, safe filename, hostile corpus test | Consumers must use patched viewers and preserve protective prefixes |
| Build and runtime supply chain | Tampering / information disclosure | Compromised action, package, base image, or transitive dependency | Build-secret theft or vulnerable deployment | Exact npm lock, fail-closed no-allowlist audit policy, explicit Maven versions, dependency/secret/container CI scans, every Action pinned to commit SHA, every image pinned to digest, non-root images | Generate SBOMs, sign artifacts, automate reviewed digest refresh, and review scanner advisories before public release |

## Abuse cases tested

- Cross-owner project, requirement, and test-case identifiers return 404.
- A user with an explicit membership in the owner's workspace still cannot access owner-scoped content in TF-001.
- Registration without CSRF is forbidden.
- Unknown JSON properties are rejected.
- Concurrent refresh rotation yields one successor; predecessor replay revokes
  that successor and the complete family.
- Forged, chained, and malformed forwarded addresses cannot mint client buckets.
- Generated unsafe, vague, generic-placeholder, duplicate, unmapped, oversized, or malformed cases are rejected.
- CSV fields beginning with formula characters are neutralized.
- Missing approvals prevent export.
- Supporting-only evidence yields zero direct and approved-direct coverage.
- Hostile Markdown links/images/HTML/autolinks/references/code/protocols remain inert.
- Invalid/malformed correlation IDs are not reflected.
- The deterministic harness rejects a missing, broad, non-loopback, IPv6, or duplicate frontend Compose publication.

## Review triggers

Revisit this model before adding organization sharing, role-based collaboration, file uploads, SSO, automation execution, new provider tools, background generation, webhooks, public deployment, or a second application instance.
# Superseded generation-set tombstones

Deletion races and historical-number tampering are mitigated with owner/run and
requirement locks, evidence rechecks, full descendant cascades, and a hidden
tombstone that preserves the completed-set ordinal without retaining case content.

## TF-015 simulation boundary

Unknown operations/fields, removed fixture obligations, unmapped criteria and
stale approval context are rejected at runner entry. Duplicate deliveries reuse
evidence without another write. Read-only roles stop before mutation; uncertain
write acknowledgement stops with indeterminate outcome and partial evidence.
Imported content can supply only bounded source fields; it cannot introduce
code, selectors, URLs, credentials or new actions. History is transient and
downloaded records include the approved synthetic source contract. This is an
in-browser demonstration, not proof of live Salesforce authorization or durable
audit protection. Recognized credential text is rejected/redacted, with no
general DLP guarantee.

## Public disposable fixture boundary (TF-016)

The public demo email/password and `synthetic-e2e-only` sentinel are intentional
synthetic literals, not secrets or live-provider credentials. `DemoModePolicy`
fails startup unless both demo flags are true, the only active profile is
`local`, the HTTP adapter is selected, the provider base URL is exactly
`http://127.0.0.1:8081/v1`, the key is the public sentinel, and the backend binds
`127.0.0.1`. Both flags default false. DNS aliases, URL decorations, remote
endpoints, conflicting profiles, and real keys are rejected. Provider transport
never follows redirects and reads at most 2 MiB plus one overflow byte before
JSON parsing; non-success/oversize responses return sanitized failures.

The reserved public principal cannot register, log in, refresh, or start
generation outside this safe mode. The generation service checks persisted
identity before the claim/provider call, including an old JWT from a prior
mode. Existing ownership/CSRF/rate rules remain authoritative. Seeding refuses
a disabled account or different password; it never resets credentials or
adopts that account's content. Anonymous `/api/v1/demo-info` reveals only the
known public disposable account after verified startup; otherwise it returns
only `enabled:false`.

The maintained external Node fixture binds loopback, requires the public
sentinel, bounds request bodies to 256 KiB, and has no proxy or outbound
network implementation. It is not packaged in the application. Fixture runs
persist `external-demo-fixture` / `testforge-review-fixture` /
`external-demo-fixture-v1`; token counts stay unknown, with no billed usage
claim. Exact title and ordered AC text select the six-case review fixture.
Supported captured clarification answers change concrete draft actions and
observations. Drafts are unapproved and Salesforce execution is NOT RUN.

The Docker E2E override shares the frontend network namespace: Nginx exposes
only the existing host loopback port; backend listens on namespace loopback
8082 and stub on 8081. The anchor joins edge/data networks for PostgreSQL.
This local disposable topology is not a multiuser deployment or an authorized
Salesforce connection. Loopback processes and database access remain within
the trusted desktop boundary; use synthetic content only.


Negative controls cover URL/profile/flag/key/bind configurations, disabled
metadata, reserved login/register/refresh and stale-token generation denial,
existing-account mismatch, real transport redirects, provider exact/over-limit
responses, and stub authorization/body bounds. Normal owner404, CSRF and
validation tests remain enabled. No live AI evaluation is implied by these
tests. Docker execution/container scans require CI when Docker is unavailable
locally; local PostgreSQL browser evidence is distinct.
