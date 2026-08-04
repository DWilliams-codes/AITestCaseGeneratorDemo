# Security Policy

## Supported version

Until the first tagged release, security fixes apply to the current `main` branch.

## Reporting a vulnerability

Do not open a public issue. Use the repository owner's private security-reporting channel with the affected revision, reproduction steps, likely impact, and suggested mitigation. Never include active credentials, customer data, refresh cookies, authorization headers, or provider keys.

## Deployment requirements

- Terminate TLS before the application and keep `SECURE_COOKIES=true`.
- Generate at least 32 random bytes for `JWT_ACCESS_TOKEN_SECRET`, Base64 encode them, and store all secrets in a managed secret store.
- Use a dedicated least-privilege PostgreSQL role, private database networking, backups, and tested restoration.
- Set exact `ALLOWED_ORIGINS`; never use wildcard credentialed CORS.
- Leave OpenAPI and demo seeding disabled unless the environment is isolated and intentional.
- Send requirement data to an external provider only after an approved data-processing review.
- Centralize logs while preserving the application's exclusion of credentials, tokens, and requirement bodies.

## Implemented controls

Authentication uses Argon2id, short-lived audience-bound HS256 JWT access tokens, rotating opaque refresh tokens hashed at rest, refresh-token family reuse detection, cookie hardening, CSRF protection, generic authentication failures, and rate limits. Authorization is owner-scoped and cross-owner access is deliberately indistinguishable from a missing object. Personal workspace memberships are now recorded, but membership does not grant shared-content access; owner predicates remain authoritative.

All JSON requests reject unknown properties and enforce field and collection bounds. Correlation IDs are parsed and normalized as UUIDs before being returned in a response header. Security headers deny framing and restrict browser capabilities. The Nginx frontend applies a restrictive CSP; inline styles remain allowed because Material UI's Emotion runtime injects styles.

Generation minimizes provider-bound data, marks requirement content as untrusted, stores no OpenAI Response object, validates strict schema and business semantics, rejects vague or executable output, allows one controlled retry, and persists only validated structured data. Exports require approval and defend against CSV formula injection and Markdown/HTML control injection.

Audit events capture actor, project, resource, action, time, correlation ID, and non-sensitive metadata. They do not capture passwords, tokens, provider keys, full requirements, or generated case bodies.

## Residual risks

- Compromise of the browser runtime can act with the in-memory access token until its short expiry.
- A permitted external AI provider receives the minimized requirement fields configured for generation.
- Application-level rate limiting is per instance; horizontally scaled deployments need a shared gateway or distributed limiter.
- Audit storage shares the application database and is not yet an external append-only ledger.
- Material UI currently requires `'unsafe-inline'` for styles in the frontend CSP.
- Workspace roles have no shared-content permissions in TF-001. Invitation, role mutation, last-owner protection, and policy enforcement must be threat-modeled before collaboration is enabled.

These risks and mitigations are tracked in [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md). Dependency exceptions must include applicability, compensating controls, an owner, and an expiration in [docs/DEPENDENCY_RISK_ACCEPTANCE.md](docs/DEPENDENCY_RISK_ACCEPTANCE.md).
