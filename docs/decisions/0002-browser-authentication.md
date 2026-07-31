# ADR 0002: Use in-memory access tokens and rotating refresh cookies

- Status: Accepted
- Date: 2026-07-30

## Context

The SPA needs session continuity without placing long-lived bearer credentials in JavaScript-readable persistent storage.

## Decision

Issue short-lived audience-bound JWT access tokens to browser memory and opaque rotating refresh tokens through HttpOnly Secure SameSite cookies. Hash refresh tokens in PostgreSQL, track token families, revoke a family on predecessor reuse, and require CSRF protection on cookie-authenticated mutations.

## Consequences

Page reload requires a refresh bootstrap and XSS can act only with the current short access-token window. Horizontal deployments share token state through PostgreSQL. Production requires HTTPS and exact CORS origins.
