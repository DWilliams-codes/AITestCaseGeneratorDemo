# TF-008 — PR #9 dependency-security remediation

**Status:** ACTIVE — IMPLEMENTATION IN PROGRESS
**Owner:** Lead-assigned sole Builder
**Scope:** PR #9 dependency-security remediation only.

## Approved outcome

Remediate the reviewed frontend `nanoid` lockfile dependency and document a
narrow, temporary OWASP Dependency-Check suppression for
`CVE-2026-66299` in `tomcat-embed-core@10.1.57`. Preserve all existing
application, API, schema, authorization, generation, evaluation, CI threshold,
and scanning contracts.

## Approved changes

1. Generate a lockfile-only frontend update using:

   `npm update nanoid --package-lock-only --ignore-scripts --no-audit --no-fund`

   The approved current-registry deviation accepts the complete npm-generated
   result: `nanoid` `3.3.16` to patched supported legacy `3.3.18`, plus npm's
   optional-entry normalization only. Do not change `package.json`, add a
   direct dependency, add an override, or accept other dependency-version drift.
2. Add `backend/config/dependency-check-suppressions.xml` using OWASP
   suppression schema 1.4. It must contain exactly one temporary suppression,
   expiring `2026-08-22Z`, matching exact Maven PURL
   `pkg:maven/org.apache.tomcat.embed/tomcat-embed-core@10.1.57` and
   `CVE-2026-66299`. It must explain that only the Tomcat WebSocket chat example
   is affected and TestForge's embedded application does not ship examples. It
   must be removed on 10.1.58+ or by expiry.
3. Wire `${project.basedir}/config/dependency-check-suppressions.xml` and
   `<failBuildOnUnusedSuppressionRule>true</failBuildOnUnusedSuppressionRule>`
   into the existing Maven `security` profile without changing CVSS 7 or other
   scanning.
4. Record temporary acceptance in `docs/DEPENDENCY_RISK_ACCEPTANCE.md`:
   advisory, PURL, applicability, owner `TestForge AI maintainers`, accepted
   `2026-08-07`, expiry `2026-08-22T00:00:00Z`, Apache advisory/download,
   historical packaged-JAR observation, future exact-SHA CI publication gates,
   compensating controls, and fail-closed removal/expiry policy.

## Explicitly excluded

No workflow, application code, tests, schemas, APIs, authentication,
authorization, generation, evaluation, dependency-install refresh, provider,
browser, Docker, E2E, deployment, staging, commit, or publication work.

## Security and compatibility

The suppression is narrow to one exact Maven PURL and CVE, expires automatically,
and fails when unused. It is not a threshold reduction or broad selector.
Existing dependency scanning remains fail-closed at CVSS 7. The frontend change
is lockfile-only and must not alter the manifest or installed dependency tree
unless the approved command does so; validation records any stale-tree limitation.

## Verification plan

Run, after approved edits:

- `python scripts/validate-harness.py`
- `cd backend && mvn verify`
- `cd frontend && npm ls nanoid --all --json --package-lock-only`
- `cd frontend && npm run test:audit-policy`
- frontend `format:check`, `typecheck`, `lint`, `test:coverage`, and `build`
- `git diff --check`, scoped diff checks, manifest-unchanged check,
  suppression exactness/no-broad-selector check, and historical packaged-JAR
  observation.

## Completion gates

Remain active until Architect conformance, independent Reviewer approval, and
Lead implementation-completion decision. No publication action is authorized.

## Evidence record

Observed 2026-08-07:

- `python scripts/validate-harness.py` — passed: Manual-008 oracle self-tests
  passed with 81 obligations; harness reported 3 specialist profiles, 6 skills,
  8 blocking manual fixtures, and 3 non-blocking automation roadmap fixtures.
- Environment: Node `v24.12.0`, npm `11.6.2`.
- `npm update nanoid --package-lock-only --ignore-scripts --no-audit --no-fund`
  — initial sandboxed attempt failed with npm-cache `EACCES`; the approved rerun
  completed. After restoring the pre-TF-008 entry, the exact command was rerun
  and its complete generated result retained: `nanoid` changed `3.3.16` to
  `3.3.18`, and npm removed only optional lock entries
  `@emnapi/core@2.0.0-alpha.3` and `@emnapi/runtime@2.0.0-alpha.3`. No other
  dependency version changed; `package.json` is unchanged and no direct
  dependency or override was added.
- `npm ls nanoid --all --json --package-lock-only` — passed and resolves
  `postcss` to locked `nanoid@3.3.18`. The installed `node_modules` tree still
  reports `nanoid@3.3.16`; it was intentionally not refreshed.
- `npm run test:audit-policy` — passed, 16/16 tests.
- `npm run format:check`, `npm run typecheck`, and `npm run lint` — passed.
- `npm run test:coverage` — passed, 6 files and 34 tests; statements 84.21%,
  branches 72.12%, functions 76.95%, lines 85.91%. The combined frontend
  command timed out after coverage while entering build; `npm run build` was
  rerun separately and passed.
- `mvn verify` — skipped: `mvn` is not available on PATH. Consequently the
  Maven security profile and a freshly packaged JAR were not verified locally.
  Existing `backend/target/testforge-backend.jar` (2026-08-06) was inspected
  with `jar tf`; it contains `BOOT-INF/lib/tomcat-embed-core-10.1.57.jar` and no
  `websocket chat` or `examples/` resources. TF-008 does not change packaging
  source. This is an existing-artifact observation, not fresh candidate or CI
  evidence.
- Static suppression and scope validation — passed: one schema-1.4 suppression,
  exact PURL/CVE/expiry, no broad selectors, CVSS 7 retained, fail-on-unused
  retained, required risk-register fields present, `package.json` unchanged,
  and the complete npm-generated lockfile result is `nanoid` `3.3.16` to
  `3.3.18` plus removal only of optional `@emnapi/core@2.0.0-alpha.3` and
  `@emnapi/runtime@2.0.0-alpha.3` lock nodes.
- `git diff --check` — passed. Scoped diff and status confirm only the five
  authorized TF-008 files changed; unrelated existing worktree changes were
  preserved.

Skipped by scope: provider/browser/Docker/E2E activity, generated automation,
dependency installation to refresh `node_modules`, live evaluation, deployment,
staging, commit, push, and publication. Local Maven verification and fresh
candidate packaging are recorded local gaps. The seven exact-SHA CI jobs are
publication gates after implementation completion, not completion prerequisites.

## Residual risks and review gates

The temporary suppression expires on 2026-08-22Z and must be removed on Tomcat
10.1.58 or later. Local Maven verification and fresh candidate packaging are
blocked by the missing Maven executable. Existing CI closes the Maven verify,
OWASP security-profile, candidate container-build, and container-scan gaps. The
seven exact-SHA CI jobs are publication gates after implementation completion,
not completion prerequisites. This plan remains active pending Architect
conformance, independent Reviewer approval, and Lead completion decision.

### Post-BLOCK remediation evidence (2026-08-07)

- Restored the pre-TF-008 `nanoid@3.3.16` lock entry and reran exactly
  `npm update nanoid --package-lock-only --ignore-scripts --no-audit --no-fund`
  with Node `v24.12.0` and npm `11.6.2`. The complete generated result is
  retained: `nanoid@3.3.18` and removal of only the optional
  `@emnapi/core@2.0.0-alpha.3` and `@emnapi/runtime@2.0.0-alpha.3` lock entries.
  `package.json` remains unchanged; the scoped diff confirms no other added
  dependency version.
- `npm ls nanoid --all --json --package-lock-only`, `npm run test:audit-policy`
  (16/16), `npm run format:check`, `npm run typecheck`, `npm run lint`,
  `npm run test:coverage` (6 files, 34 tests), and `npm run build` — passed.
- Repeated harness, exact suppression/POM/risk-register static checks, and
  `git diff --check` — passed.
- The existing 2026-08-06 JAR observation remains explicitly historical. Maven
  verification and fresh candidate packaging are local gaps; existing CI closes
  Maven verify, OWASP security-profile, candidate container-build, and
  container-scan gaps. The seven exact-SHA CI jobs remain publication gates
  after implementation completion, not completion prerequisites.

## Final local completion evidence (2026-08-07)

The Lead decided implementation complete after post-build Architect
**CONFORMS** and independent Reviewer **APPROVE** verdicts.

- Final local evidence: harness, generated-lockfile inspection, package-manifest
  scope, audit-policy, frontend format/typecheck/lint/coverage/build, suppression
  exactness, risk-register evidence, focused documentation checks, and
  `git diff --check` passed as recorded above.
- The completed scope is limited to the approved lockfile update, exact temporary
  OWASP suppression, Maven security-profile wiring, dependency-risk acceptance,
  and this historical ExecPlan. No application, API, schema, authorization,
  generation, evaluation, workflow, or test-contract change was made.
- Local residual gaps: Maven is unavailable on PATH, so local `mvn verify` and a
  fresh candidate package were unavailable; `node_modules` remains intentionally
  stale at `nanoid@3.3.16` because this was a lockfile-only update.
- Publication residual risk: all seven exact-SHA CI jobs remain publication gates
  after implementation completion. Existing CI must close the Maven verify,
  OWASP security-profile, candidate container-build, and container-scan gaps
  before any separately authorized publication decision.

No staging, commit, push, PR mutation, deployment, or publication was performed.
