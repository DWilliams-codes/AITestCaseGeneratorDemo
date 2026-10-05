# TF-010: MVP defects and adaptive UI

## Status

Locally complete on 2026-08-10. The Architect returned final `CONFORMS`, the
independent Reviewer returned final `APPROVE` with no blocking findings, and
the Lead decided implementation completion. This plan is ready to move to
`completed/`; preserve all TF-009 edits.

## Plan/base and objective

Architect observed `main` at `80dfe21`; TF-009 is an uncommitted dirty
candidate, including its completed plan. Preserve it. Fix every evidenced MVP
defect and deliver one cohesive responsive MUI system/light/dark UI across the
auth shell and Stage 1 pages.

## Approved scope

1. Fix PostgreSQL omitted/blank search `lower(bytea)` failure in canonical
   case-page queries.
2. Keep controls and Clear filters visible; eliminate contradictory empty/no-
   match states for filtered zero results.
3. Block the criterion-51 UI path despite API maximum 50.
4. Block precondition/test-data 31 UI paths despite API maximum 30.
5. Relabel Review queue wording/detail as page-scoped; do not add aggregation.
6. Recover truthfully when shareable `casePage` is out of range despite a
   nonzero total.
7. Add responsive MUI light/dark/system preference UI, semantic theming, and
   targeted mobile/dialog/table coverage.

## Non-goals and compatibility

No sidebar/IA rewrite, charts, font/image dependency, aggregation endpoint,
route/query-key rename, auth/workspace change, migration, provider/model/
prompt/schema/result/validator/eval fixture/candidate change, automation, or
dependency install. Preserve Stage 1 and TF-009 setup/export/query/owner-404
contracts. URLs, API/query params, JSON/PATCH/export/audit/auth remain
unchanged. Blank search means no filter; no V8. The theme is local-only and
deleting its key restores system mode. The generation tuple remains exactly
`manual-test-v3`/`manual-test-result-v2`/schema v3/validator v3/
`openai-responses-v4`, fixture version 3; record no evaluation impact.

## Backend design

Keep the nullable public API contract. Replace repository search input with an
always-non-null escaped LIKE pattern. `escapedSearchPattern` returns `%` for
null/blank; otherwise `%` + stripped input with `\\`, `%`, and `_` escaped +
`%`. Both normal and priority JPQL queries use
`lower(column) like lower(:searchPattern) escape '\\'`, with no null guard or
`concat`. Values remain parameterized. Preserve global filtering, stable ties,
priority CASE, and owner/run resolution.

## Results, limits, and adaptive UI

Controls, generation selector, search, filters, sort, result count, and Clear
filters remain visible whenever usable, including zero items. Show exactly one
truthful loading, error, unfiltered-empty, filtered-no-match, or out-of-range
state. Filtered zero has prominent Clear filters; errors retry and never become
empty. If `totalElements > 0` but the page is empty/out of range, atomically
return `casePage` to 0 with truthful transient recovery and no loop. Preserve
URL/server authority. Relabel the metric, for example, “Open cases on this
page.”

Project criteria show `n of 50`; Add disables at 50 and re-enables on removal;
keep Zod max. The editor shows `n of 30` for preconditions, setup/test steps,
and test data; each Add disables at 30. Legacy over-limit values remain
reducible, show an alert, and block Save until valid.

Use a calm compact quality-operations workspace with semantic light/dark MUI
tokens, restrained blue/teal, readable cards, consistent loading/empty/error,
and hover elevation only for interactive surfaces. A resolved-mode theme factory
must cover surfaces/dividers/text/actions/focus/status/table/dialog/input/card/
tab/appbar and remove hard-coded light shell/login colors. Preserve
focus-visible, reduced motion, contrast, and text/icon state cues.

Add `ColorMode` context/provider for `system|light|dark`, resolved via
`matchMedia`, validated failure-safe localStorage, storage-event sync,
`document.colorScheme`, and theme-color. Persist only
`testforge-color-mode`, never auth/identity/domain/search/generated data.
Expose an accessible explicit `ThemeModeControl` on Login and the authenticated
shell. Preference survives navigation/reload; invalid storage falls back to
system. Use meta color-scheme and CSP-safe pre-render setup only—no inline
executable script or CSP weakening. MUI-only. An optional reusable
`ResponsiveDialog` may cover all five dialogs. On mobile, action rows stack,
tabs scroll, dialogs become full-screen, and dense tables are horizontally
scrollable/focusable/labeled with min widths; no ~390px shell overflow.

## Production surface

- Backend: `TestCaseService.java`, `TestCaseRepository.java`.
- Frontend: `index.html`, `theme/theme.ts`, new
  `theme/ColorModeContext.tsx`, new `components/ThemeModeControl.tsx`, optional
  `ResponsiveDialog.tsx`, `AppProviders.tsx`, `ApplicationShell.tsx`,
  `PaginationControls.tsx`, `LoginPage.tsx`, `ProjectDashboardPage.tsx`,
  `ProjectPage.tsx`, `RequirementPage.tsx`, `NotFoundPage.tsx`.
- Tests: `StageOneApiIntegrationTest.java`, frontend setup/App/Workflow/color
  tests as needed, `application-shell.spec.ts`, `stage-one-workflow.spec.ts`,
  and new `responsive-theme.spec.ts` at 390px.
- Docs: README, PRODUCT, ARCHITECTURE, API, TESTING, SECURITY, THREAT_MODEL,
  and this active plan. Do not rewrite completed TF-009.

## Security, accessibility, and tests

Owner guard is unchanged. Search remains bounded, escaped, and parameterized;
test null/blank/metacharacters. Theme storage is non-sensitive; update brittle
localStorage-empty coverage to ensure no auth/secret keys while allowing the
theme key. No external assets or analytics. Preserve CSP, reduced motion, skip
link, focus, labels, touch targets, inert text, and axe checks.

Test PostgreSQL omitted normal and blank priority searches; retain wildcard,
filter, page, historical, alias, and owner coverage. Test zero results/Clear,
out-of-range reset, and page metric; 49→50/remove and 29→30/remove/save;
system/light/dark persistence/invalid storage; and mobile dialogs/tables/shell.
At 390px ensure no overflow and reachable key paths. Browser coverage must use
the deterministic stub only.

## Ordered implementation and acceptance

1. Materialize/re-read this plan and inventory status.
2. Add failing real-Postgres omitted/blank tests and implement both query paths.
3. Add color provider/theme/control tests and implementation.
4. Apply coherent theme, shell/login/pages, then empty/out-of-range/page-metric
   behavior and tests.
5. Apply UI boundary guards and tests.
6. Add accessible mobile dialogs/tables/shell and Playwright coverage.
7. Update docs, format, checks, and evidence.

Acceptance requires PostgreSQL 200 for omitted/blank normal and priority query
variants while all existing literal/filter/sort/page/history/alias/404 behavior
holds; visible recoverable zero-result controls; no criterion-51 or item-31
append path; reducible legacy oversized values; explicit page-scoped metric;
persistent safe system/light/dark preferences; semantic responsive design;
390px usable layout; accessibility behaviors and axe serious/critical pass; and
no forbidden contract changes.

## Validation, rollback, and evidence

Run `python scripts/validate-harness.py`; Maven verify with Java 21/Maven 3.9
when available; frontend format/typecheck/lint/coverage/build/audit; diff/path
audit; and supported disposable Docker e2e topology only when available. Do
not call a live provider. Record unavailable checks honestly; exact-SHA CI is
later publication-only evidence.

Rollback is code-only; the theme key is harmless. Any partial rollback retains
the query fix. Risks include Hibernate/Postgres typing, page-reset loops,
pre-mount flash, dark contrast, storage leakage, mobile locators, and action
discoverability; mitigate with the approved focused tests and semantic design.

## Evidence log

Implemented without changing any generation tuple, provider, schema, fixture,
or evaluation contract. The backend now sends an always non-null escaped LIKE
pattern for normal and priority test-case pages; blank/omitted search is `%`.
Stage One integration coverage exercises both canonical page paths in addition
to the retained literal/filter/sort/paging/history/owner checks.

The SPA now retains server-query controls and an actionable filtered zero state,
shows a distinct out-of-range recovery notice before resetting `casePage`, and
labels the review metric as page-scoped. Criterion and case-editor collection
adds stop at 50 and 30 respectively; an oversized legacy case is still
reducible but cannot be saved. `ColorModeProvider`, `ThemeModeControl`, a
semantic light/dark MUI factory, and `ResponsiveDialog` provide local
system/light/dark behavior, safe storage fallback, mobile dialogs, responsive
pagination, and focusable traceability overflow. The sole stored key is
`testforge-color-mode`; selecting System deletes that key so future resolution
again follows the host preference.

Observed final checks on 2026-08-10 local time:

- `frontend`: `npm run format:check`, `npm run typecheck`, `npm run lint`,
  `npm run test:coverage`, and `npm run build` passed after the final
  remediation. Coverage: 84.66% statements, 74.71% branches, 78.24%
  functions, and 86.44% lines (38 tests in 7 files passed). The 30-item editor
  test runs in 62.25 seconds when focused and remains below its explicit
  90-second timeout. The focused ColorMode lifecycle test passed 2/2 and
  explicitly covers Light persistence/resolution, Dark remount, System key
  deletion plus `matchMedia` resolution, invalid storage, and storage-event
  synchronization.
- `frontend`: elevated `npm run audit:ci` passed with `No high or critical npm
  advisories were found.` Earlier registry/internal-error attempts are resolved
  by the permitted registry access; no audit finding remains.
- `python scripts/validate-harness.py` passed: 94 obligations, 8 blocking
  manual fixtures, and 3 roadmap fixtures. `git diff --check` passed; the sole
  message was Git's pre-existing CRLF warning for `scripts/validate-harness.py`.
  The changed-path audit found only the preserved TF-009 candidate and TF-010's
  approved backend, frontend, e2e, documentation, and plan surface. Nothing
  was staged, committed, or published.
- Disposable local Docker verification passed after one accessibility remediation:
  Docker Desktop was invoked by its explicit local path with
  `docker compose -f docker-compose.yml -f docker-compose.e2e.yml up --build --wait`.
  It compiled Java 21 backend and test sources through Maven package (the
  production Dockerfile intentionally uses `-DskipTests`) and brought
  PostgreSQL, backend, frontend, and the synthetic Responses stub healthy.
  With `PLAYWRIGHT_BASE_URL=http://localhost:3000`, `npm run e2e` passed all
  5 tests, including 390px theme/dialog/table/axe, setup/export, owner
  isolation, and safe provider failure. The first e2e run identified dark-theme
  primary contrast and the XS brand-link accessible-name defects; the resolved
  dark primary token and link label were rebuilt and the full five-test e2e
  suite then passed. The e2e override alone raises its disposable rate budget
  for parallel synthetic accounts; production limits remain unchanged. The
  exact disposable containers and networks were torn down without deleting the
  named database volume.
- Architect remediation added a deterministic browser assertion for the actual
  Docker PostgreSQL priority path: it selects `priority-desc` with no `search`
  parameter and asserts HTTP 200, a nonempty page, and CRITICAL→HIGH→MEDIUM→LOW
  priority rank order. Its first execution correctly exposed PostgreSQL 42P10
  (`SELECT DISTINCT` cannot order by the CASE expression). The priority query
  now uses a correlated criterion `exists` predicate instead of joins plus
  `DISTINCT`; the normal query remains unchanged. Rebuilt Docker compilation
  passed (main and test sources compile; Dockerfile skips tests), and the full
  five-test e2e suite passed in 6.4 seconds after the fix.
- Host `mvn`/Maven wrapper and Docker CLI PATH were unavailable. Docker Desktop
  was invoked by its explicit local path. An isolated read-only-source Maven
  3.9.11/Java 21 container could not start offline verification because the
  Spring Boot parent was absent; a disposable cached Dockerfile build-stage
  image then reached Surefire but lacked
  `org.apache.maven.surefire:surefire-junit-platform:3.5.6`. No dependency was
  downloaded, no test was executed, and the temporary image was removed.
  Backend `mvn verify` (including Spotless, SpotBugs, and test execution)
  remains unobserved. Exact-SHA CI, publication, and live-provider checks were
  not run.

Residual risks: real PostgreSQL behavior was exercised by the disposable runtime
and new test sources compile, but Maven verification and its quality plugins
remain pending supported CI/toolchain evidence because the offline verification
image lacks Surefire's JUnit Platform provider. Dependency auditing passed with
no high or critical advisories. Browser tests ran only against the local
synthetic stub; no external provider was called.

## Final disposition

The Lead has decided TF-010 implementation is locally complete after Architect
`CONFORMS` and independent Reviewer `APPROVE`. The recorded deterministic local
evidence supports local implementation completion, not publication readiness:
exact-SHA CI, staging, commit, push, publication, and merge remain separately
authorized publisher work. The residual Maven limitation is recorded above and
does not alter the Lead's local-completion decision.
