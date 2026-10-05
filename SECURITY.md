# Security Policy

## Supported version

Until the first tagged release, security fixes apply to the current `main` branch.

## Reporting a vulnerability

Do not open a public issue. Use the repository owner's private security-reporting channel with the affected revision, reproduction steps, likely impact, and suggested mitigation. Never include active credentials, customer data, refresh cookies, authorization headers, or provider keys.

## Deployment requirements

- Terminate TLS before the application and keep `SECURE_COOKIES=true`.
- Cleartext local Compose is IPv4-loopback-only. Any LAN, shared, or public
  exposure requires a TLS deployment with secure cookies; do not broaden the
  local Compose port binding.
- Generate at least 32 random bytes for `JWT_ACCESS_TOKEN_SECRET`, Base64 encode them, and store all secrets in a managed secret store.
- Use a dedicated least-privilege PostgreSQL role, private database networking, backups, and tested restoration.
- Set exact `ALLOWED_ORIGINS`; never use wildcard credentialed CORS.
- Configure `TRUSTED_PROXY_CIDRS` only for actual ingress peers. Ignore caller
  forwarding headers at any untrusted socket boundary.
- Leave OpenAPI and demo seeding disabled unless the environment is isolated and intentional.
- Send requirement data to an external provider only after an approved data-processing review.
- Centralize logs while preserving the application's exclusion of credentials, tokens, and requirement bodies.

## Implemented controls

Authentication uses Argon2id, short-lived audience-bound HS256 JWT access tokens, rotating opaque refresh tokens hashed at rest, locked predecessor consumption, committed refresh-family replay revocation, cookie hardening, CSRF protection, generic authentication failures, and client-plus-bounded-subject rate limits. The SPA's epoch-based reset clears bearer, CSRF, and coalesced promise state so stale refresh/CSRF completions cannot restore an older session. Forwarded addresses are accepted only as one sanitized literal from a configured trusted socket peer, and Nginx overwrites inbound forwarding data. Authorization is owner-scoped and cross-owner access is deliberately indistinguishable from a missing object. Personal workspace memberships are now recorded, but membership does not grant shared-content access; owner predicates remain authoritative.

All JSON requests reject unknown properties and enforce field and collection bounds. Correlation IDs are parsed and normalized as UUIDs before being returned in a response header. Security headers deny framing and restrict browser capabilities. The Nginx frontend applies a restrictive CSP; inline styles remain allowed because Material UI's Emotion runtime injects styles.

Generation minimizes provider-bound data, marks User Story content as untrusted, stores no OpenAI Response object, commits no database transaction across the provider call, rejects provider-authored transport metadata and scalar/enum coercion, validates strict schema and business semantics (including direct coverage, actionable reproducible setup/readiness, individual evidence-bearing tester interactions, generic criterion-placeholder rejection, normalized test-data names/references, hard bounds, and every provider-authored text field), and retries only incomplete/malformed/empty or semantic output once. Complete evidence and dual traceability links persist atomically against immutable source snapshots; captured criterion identity preserves rename compatibility, while deletion fails before a generated graph write. Superseded generation sets are read-only and default exports are active-set approved cases only. Exports defend against CSV formula injection and conservatively encode all untrusted Markdown punctuation.

Audit events capture actor, project, resource, action, time, correlation ID, and
closed, bounded scalar metadata. They do not capture passwords, tokens,
provider keys, full requirements, generated case bodies, or reopen reasons.

The only client-persisted presentation value is the validated
`testforge-color-mode` preference (`system`, `light`, or `dark`). It contains no
identity, authentication, workspace, query, requirement, or generated-case
data; malformed or unavailable browser storage safely falls back to system.

## Residual risks

- Compromise of the browser runtime can act with the in-memory access token until its short expiry.
- A permitted external AI provider receives the minimized requirement fields configured for generation.
- Application-level rate limiting is per instance; horizontally scaled deployments need a shared gateway or distributed limiter.
- Audit storage shares the application database and is not yet an external append-only ledger.
- Material UI currently requires `'unsafe-inline'` for styles in the frontend CSP.
- Workspace roles have no shared-content permissions in TF-001. Invitation, role mutation, last-owner protection, and policy enforcement must be threat-modeled before collaboration is enabled.

These risks and mitigations are tracked in [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md). Dependency exceptions must include applicability, compensating controls, an owner, and an expiration in [docs/DEPENDENCY_RISK_ACCEPTANCE.md](docs/DEPENDENCY_RISK_ACCEPTANCE.md).
# Superseded generation-set purge

The purge endpoint is owner-scoped, confirmed, transactionally rechecked under
locks, and records only bounded set-number and purged-case-count audit metadata.
It never deletes human review or revision evidence.

## Bounded browser simulation

TF-015 is expressly authorized only as synthetic browser-local execution. It
has no provider/org/network execution capability and accepts no imported action
programs. Maintained operations, fields, synthetic values, exact fixture AC
semantics and complete obligations are allowlisted. Credential-looking input is
rejected and diagnostic exports redact recognized secret patterns; do not paste
real secrets or production data. The only reference is the unresolved name
`SALESFORCE_QA_CREDENTIAL`, never a credential value. These input safeguards are
not a general-purpose DLP guarantee.

Immutable approvals bind context and are rechecked in the runner; browser users
can still modify their own runtime. No multiuser security claim is made. A live
connector needs a separate server authorization boundary, authorized
nonproduction org, scoped secret resolution, audit/retention, isolated runners
and explicit write policy. Interrupted writes are indeterminate and not retried.

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
