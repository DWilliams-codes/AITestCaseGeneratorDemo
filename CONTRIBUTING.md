# Contributing to TestForge AI

Use small, reviewable changes that preserve the modular-monolith boundaries. Do not mix unrelated refactors with product work.

## Before opening a pull request

1. Run `mvn verify` from `backend` on Java 21.
2. Run `npm run format:check`, `npm run lint`, `npm run typecheck`, `npm run test:coverage`, `npm run build`, and `npm run audit:ci` from `frontend`.
3. Run relevant Playwright tests.
4. Update documentation when behavior or configuration changes.
5. Confirm that no credentials, tokens, customer data, or production requirement content is present.

Java source is formatted by Spotless. Frontend source is formatted by Prettier and checked by ESLint. Persistence entities must never be returned directly from APIs, ownership checks belong in backend services and repositories, and external-provider output must pass the application-owned validator before persistence.
