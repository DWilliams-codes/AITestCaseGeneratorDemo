# Dependency Risk Acceptance Register

## React Router RSC action CSRF advisory

| Field | Value |
| --- | --- |
| Advisory | `GHSA-qwww-vcr4-c8h2` |
| Dependency | `react-router` through `react-router-dom@7.18.2` |
| Severity | High |
| Status | Temporarily accepted; no patched stable release is published |
| Owner | TestForge AI maintainers |
| Accepted | 2026-07-30 |
| Expires | 2026-08-30 or immediately when a fixed stable version is available |

The advisory affects React Server Components mode by allowing an action to execute before an invalid-content-type request is rejected. TestForge AI is a client-only Vite single-page application. It does not enable React Server Components, framework actions, server actions, server-side rendering, or React Router's RSC request handlers. The Spring Boot API is a separate origin boundary with bearer authentication, rotating refresh cookies, exact credentialed CORS, and CSRF protection for cookie-authenticated mutations.

The current stable `7.18.2` release is retained because older 7.x versions contain multiple applicable router, redirect, denial-of-service, and deserialization advisories. CI uses `frontend/scripts/audit.mjs` to allow only this advisory, only for `react-router`, and only through the expiration date. Every other high or critical npm advisory fails the build.

Required follow-up: check for a patched stable React Router release on every dependency update and remove both the acceptance and allowlist entry as soon as one is available.
