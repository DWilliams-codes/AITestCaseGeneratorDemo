# STRIDE Threat Model

## Assets and trust boundaries

Protected assets are credentials, refresh-token families, owner-scoped project data, requirements, generated cases, review decisions, audit evidence, database secrets, and provider keys. Trust boundaries exist between browser and API, API and database, API and external provider, and operator configuration and each runtime container.

| Asset | STRIDE threat | Attack path | Impact | Implemented mitigation | Residual risk / production action |
| --- | --- | --- | --- | --- | --- |
| User account | Spoofing | Password guessing, credential stuffing, or forged access token | Account takeover and unauthorized quality records | Argon2id, generic auth errors, minute-window rate limit, HS256 with a 32-byte minimum key, issuer/audience/time validation | Add gateway/IP reputation plus MFA or enterprise OIDC |
| Refresh-token family | Spoofing | Theft and replay of a browser refresh cookie | Persistent account takeover | Random opaque tokens, SHA-256 hashes at rest, HttpOnly SameSite Secure cookies, rotation, family reuse detection and revocation | Monitor reuse events and shorten TTL by risk tier |
| Owner-scoped records | Tampering / elevation | Substitute another user's project, requirement, case, run, export, or audit identifier | Cross-tenant read or mutation | Owner predicates in services and repositories, inaccessible objects return 404, automated IDOR tests | Add explicit organization membership and role policy before sharing |
| Workspace membership | Elevation / information disclosure | Treat membership or a caller-supplied workspace ID as implicit access to owner-scoped content | Cross-owner data exposure before role policy exists | Membership only scopes workspace listing; project creation derives personal workspace server-side; a shared-member integration test still requires 404 for project, requirement, and case | Define an explicit role/action policy, invitation lifecycle, inactive-member handling, and last-owner protection before shared reads |
| Requirement and case revisions | Tampering | Submit a stale edit over a newer version | Lost evidence or reviewer changes | Optimistic versions, `409 stale_version`, immutable case revisions | Add side-by-side diff and merge UX as concurrency grows |
| Generated evidence | Tampering | Compromised provider emits malicious, structurally valid-looking, or poisoned output | Incorrect tests, unsafe data, or misleading coverage | Strict schema plus enum, size, ordering, mapping, duplicate, vague-language, and executable-content validation; one controlled retry | Maintain provider evals, anomaly metrics, and an emergency provider-disable switch |
| Audit evidence | Repudiation / tampering | Reviewer disputes a decision or a database operator modifies audit rows | Loss of accountability | Immutable review rows, revisions, correlated audit events, server timestamps, minimal allowlisted metadata | Stream to append-only SIEM or WORM storage; protect database administrator access |
| Access token | Information disclosure | XSS or browser storage theft | Requests within the token lifetime | Access token exists in memory only; refresh cookie is inaccessible to JavaScript; CSP and output encoding | XSS can act during the short access-token lifetime; maintain CSP and dependency hygiene |
| Provider key and requirement content | Information disclosure | Browser exposure, excessive provider payload, provider retention, or provider compromise | Secret loss or disclosure of proprietary requirements | Key is server-side only; minimized payload excludes identity, UUIDs, correlation IDs, tokens, and hidden reasoning; `store:false` | Complete vendor DPA, residency, retention, and breach-response review; classify requirements before use |
| Application logs | Information disclosure | Request bodies, authorization headers, secrets, or provider payloads enter logs | Credential or proprietary-data disclosure | No body logging, generic provider/auth errors, allowlisted audit metadata, normalized correlation IDs | Verify proxy, APM, container, and cloud logging redaction |
| Database | Information disclosure / tampering | Stolen database credential, exposed port, backup theft, or excessive application-role privileges | Full tenant-data disclosure or audit manipulation | Container-private network, externalized credentials, parameterized JPA access, schema constraints, least-function application path | Use a managed private database, TLS, encryption at rest, restricted backup access, rotation, and a formally least-privileged role |
| Provider budget and availability | Denial of service | Authentication abuse, oversized input, repeated generation, or slow provider responses | Cost growth and service unavailability | Per-subject limits, body/collection bounds, provider connect/read/output caps, bounded DB pool, required idempotency key | Add distributed gateway limits, per-account quotas, cost alerts, circuit breaking, and queueing before scale-out |
| Cookie-authenticated endpoints | Elevation of privilege | Cross-site request forgery invokes refresh, logout, login, or registration | Session manipulation | SameSite cookies, double-submit CSRF cookie/header, exact credentialed CORS | Keep origins exact, enforce TLS, and regression-test proxy behavior |
| Generation policy | Elevation of privilege | Prompt injection in a story or criterion asks the model to reveal secrets or ignore policy | Policy bypass or data leakage | Source is delimited as untrusted data; versioned developer prompt; no tools; strict structured-output validation | Run a maintained injection corpus for every prompt, schema, model, or provider upgrade |
| Export consumers | Content injection | User-controlled cell starts with `=`, `+`, `-`, or `@`, or Markdown contains active markup | Spreadsheet formula execution or misleading rendered output | Approved-only export, CSV formula-prefix neutralization, Markdown and HTML escaping, safe filename | Consumers must use patched viewers and preserve protective prefixes |
| Build and runtime supply chain | Tampering / information disclosure | Compromised action, package, base image, or transitive dependency | Build-secret theft or vulnerable deployment | npm lockfile, explicit Maven versions, SpotBugs/ESLint, dependency/secret/container CI scans, SHA-pinned Trivy action, non-root images | Pin all action and image digests, generate SBOMs, sign artifacts, and review scanner advisories before public release |

## Abuse cases tested

- Cross-owner project, requirement, and test-case identifiers return 404.
- A user with an explicit membership in the owner's workspace still cannot access owner-scoped content in TF-001.
- Registration without CSRF is forbidden.
- Unknown JSON properties are rejected.
- Refresh rotation invalidates the predecessor and reuse is rejected.
- Generated unsafe, vague, duplicate, unmapped, oversized, or malformed cases are rejected.
- CSV fields beginning with formula characters are neutralized.
- Missing approvals prevent export.
- Invalid/malformed correlation IDs are not reflected.

## Review triggers

Revisit this model before adding organization sharing, role-based collaboration, file uploads, SSO, automation execution, new provider tools, background generation, webhooks, public deployment, or a second application instance.
