# Contributing to TestForge AI

Use small, reviewable changes that preserve the modular-monolith boundaries. Do not mix unrelated refactors with product work.

## Before opening a pull request

1. For significant work, create and maintain an ExecPlan as described in
   [docs/PLANS.md](docs/PLANS.md). Use one writer for overlapping implementation.
2. Run `./scripts/verify.sh` or `.\scripts\verify.ps1` from the repository root.
   The wrappers do not install missing dependencies or call a live provider.
3. Run `npm run audit:ci` from `frontend` and relevant deterministic Playwright
   tests when their prerequisites are available.
4. Update documentation and sanitized evaluations when behavior or generation
   expectations change.
5. Confirm that no credentials, tokens, customer data, or production requirement
   content is present.
6. Record verification, Evaluation impact, risks, and deviations in the pull
   request and ExecPlan.

Java source is formatted by Spotless. Frontend source is formatted by Prettier and checked by ESLint. Persistence entities must never be returned directly from APIs, ownership checks belong in backend services and repositories, and external-provider output must pass the application-owned validator before persistence.
