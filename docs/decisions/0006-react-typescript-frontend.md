# ADR 0006: Use a React and TypeScript single-page client

- Status: Accepted
- Date: 2026-07-30

## Context

The review workflow needs rich structured forms, server-state caching, accessible dialogs and tables, and a production static artifact.

## Decision

Use React, strict TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, and Material UI. Serve the production bundle from unprivileged Nginx and keep authentication access tokens only in memory.

## Consequences

The UI remains independently testable and deployable as static assets. API rules remain authoritative. Material UI's runtime styling currently requires a narrowly scoped CSP style exception.
