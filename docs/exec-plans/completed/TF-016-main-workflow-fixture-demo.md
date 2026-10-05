# TF-016: Main-workflow fixture demo and public demo account

## Objective and approved scope

Daniel prioritizes the real main application workflow and requests working demo credentials on the login page, security review/fixes, and later normal publication by the Lead. This plan materializes the Architect-approved bounded handoff before feature writes. The same Builder owns all overlapping changes and re-reads this file first. Preserve TF-015 and all prior user edits.

## Non-goals and authority

No paid AI calls, new credentials, new dependencies, Salesforce connector, archive or runner expansion. No Builder commit/push/deploy. The Lead owns independent repository security audit, publication and exact-SHA CI after local gates. Explicitly authorized fallback is an external deterministic fixture with truthful UI/provenance, not a live model.

## Inspected contracts and current findings

Existing DemoDataSeeder exposes a fixed public account but only checks seed-enabled, accepts any existing account with its email, and could seed into a live-provider local configuration. AuthService and GenerationService currently lack public-principal mode guards. The current external Node Responses stub binds all interfaces, has no body bound, and supplies synthetic usage. Docker E2E reaches it via DNS. Preserve owner authorization, CSRF, rate limits, validated generation, source/ambiguity/revision/review persistence and the OpenAI HTTP adapter.

## Acceptance criteria

1. Central DemoModePolicy has fixture-mode default false. Enable only with fixture and seed flags, explicit local profile without production/conflicting profiles, OpenAI HTTP adapter, exact http://127.0.0.1:8081/v1 URL, fixed public synthetic key sentinel, redirects disabled and backend loopback bind. Unsafe demo startup fails closed. External stub binds loopback, bounds request bodies and never proxies.
2. Seed reserved demo account only under that policy. Existing account must be enabled and match the documented disposable password; never reset or adopt a different account. Block reserved-principal registration/login/refresh outside safe mode, and independently guard generation before claim/provider to prevent old JWT paid-provider reachability. Preserve all owner and CSRF/rate controls.
3. Anonymous GET /api/v1/demo-info returns only enabled:false when unavailable/disabled, or public disposable credentials and fixture label when safe and ready. Login displays/fills server-returned credentials; failure hides them. Shell and generation notices explicitly label maintained fixture generation/no AI calls.
4. Persist provider external-demo-fixture, model testforge-review-fixture and explicit adapter provenance under validated fixture mode. Do not claim actual billed token usage. Keep normal OpenAI mode unchanged and no packaged canned provider.
5. One maintained sanitized Case review fixture under frontend/e2e/stub/fixtures uses exact title plus five exact criterion texts, with six cases: create, update, persisted reopen, blank Subject, read-only create, read-only update. Concrete setup, action/result steps, synthetic data references and AC mappings pass existing validation. First response exposes owner clarification. Source-supported proposed values are visibly proposed. Captured resolved answers change both priority/owner actions and assertions; test an alternate choice. Unsupported stories must never receive this fixture under false coverage. No autoapproval or execution.
6. Adapt Docker E2E namespace topology to the literal-loopback gate without security exceptions and preserve seven-job CI configuration. Local backend and stub never expose public demo credentials to a live provider or another owner's resources.

## Files/interfaces and compatibility

Add DemoModePolicy/demo-info controller and tests; narrow DemoProperties, DemoDataSeeder, AuthService/refresh rotation, GenerationService and OpenAI configuration/provider. Add frontend demo metadata UI/helper, exact maintained fixture and stub tests/browser flow. Update Docker E2E wiring, canonical API/product/security/testing docs and walkthrough. No applied migration edits anticipated. Existing normal provider configuration remains compatible except unsafe public-demo seeding now explicitly rejected.

## Implementation and rollback

1. Materialize/re-read this plan first.
2. Implement central policy, fail-closed principal/provider guards, safe seeding/metadata and provenance.
3. Add maintained external fixture, bounded loopback stub and CI topology.
4. Add login/shell/generation labels and reviewer workflow browser tests.
5. Run deterministic tests and full local gates; record actual evidence and residuals.
6. Obtain Architect CONFORMS then independent Reviewer APPROVE; Lead decides completion. Same Builder alone moves this file after that decision. Publication belongs to Lead separately.

Rollback only these additive/scoped edits while preserving prior user work. Do not run the known public demo account against live-provider mode; its guards remain fail closed. No database schema rollback is needed.

## Verification/evaluation/security matrix

Harness first; policy URL/profile/flag/key/bind/redirect matrix; disabled and ready metadata; seeded account credential verification; off-mode reserved login/register/refresh and old JWT generation denial before provider; owner404 and CSRF; six-case schema/semantic validation and unapproved persistence/edit/reload; clarification alternate-value fixture test; normal provider regression. Full offline mvn verify and frontend format/type/lint/comments/coverage/build, stub/policy/preflight tests, browser main workflow and existing suites. No live model semantic scoring or provider charges. Release prompt/result/schema/validator remain v5/v2/v3/v4; fixture adapter has distinct provenance from normal openai-responses-v5.

## Risks and definition of done

Public credentials are disposable synthetic-only. Configuration validation and server-side principal guards are security boundaries; hiding UI credentials alone is insufficient. Exact fixture signature limits false coverage. Public metadata must never reveal real provider/environment/connection secrets. E2E Docker loopback topology needs explicit validation; local desktop browser evidence cannot be relabeled as Docker CI. Completion requires observed gates, docs, formal conformance/review and Lead decision.

## Final implementation status

**Complete for the authorized local MVP scope, 2026-10-05.** The same Builder
implemented and closed this plan after Architect **CONFORMS**, independent
Reviewer **APPROVE**, and the Lead's explicit completion decision. The Reviewer
also approved the final toolchain-only dependency declaration repair. No
Builder commit, push or deployment occurred. Publication and exact-SHA CI are
separate Lead-owned work.

The main app now has a working server-confirmed public demo login, real saved
story/clarification/generation/review persistence, six maintained Case review
drafts, and explicit fixture/no-AI labels. Native unapproved states are
`GENERATED` and `IN_REVIEW`; `DRAFT` is not claimed as a stored enum. No Case
was autoapproved or executed against Salesforce. The separate browser
simulation remains unchanged in scope.

## Final observed verification

| Check | Result and evidence |
| --- | --- |
| Backend `mvn verify`, final patched dependencies | PASS: 110 total tests; 105 executed passed, 5 Docker-dependent skipped; 0 failures/errors. Spotless PASS, SpotBugs 0 findings, JaCoCo gates PASS. Task `backend-tf016-verify.log`, finished 14:16:29 Eastern. |
| Backend coverage | Lines 92.41%, branches 75.14%, methods 89.94%; no thresholds changed. |
| Frontend final unit coverage | PASS: 54 tests / 9 files. Statements 84.14%, branches 78.84%, functions 78.08%, lines 85.51%. Task `frontend-tf016-coverage.log`. |
| Frontend static/build checks | Format, lint, typecheck, declared-function comments and production build PASS. Task `frontend-tf016-build.log`. |
| Fixture and guard tests | Maintained external fixture 5/5, audit policy 16/16, no-call live preflight 6/6 PASS. |
| Installed dependency integrity | `npm ls --all --json` PASS after narrow toolchain repair. Task `frontend-tf016-installed-tree.json`. |
| Clean lockfile reproduction | Isolated `npm ci --ignore-scripts --no-audit --no-fund` and `npm ls --all --json` PASS; 379 platform-applicable packages installed. Task `frontend-tf016-clean-install.log` / `frontend-tf016-clean-tree.json`. Live Vite was preserved. |
| Dependency vulnerability checks | Final npm audit PASS with no high/critical findings. Fresh OSV query of 142 resolved production/test Maven dependencies: zero findings, 18:17:23 UTC. Task `frontend-tf016-final-audit.log` / `backend-osv-audit.json`. |
| Browser on final patched backend | 14/14 PASS in 8.0 seconds, task `playwright-tf016-security-patched.log`. Standalone PostgreSQL 18.4 plus external loopback fixture; no live model. |
| Canonical saved main workflow | Lead observed 5 ACs, 6 cases, saved clarification/regeneration, human edit/save/reload, 5 GENERATED plus 1 IN_REVIEW, no approval, and approved-only export rejection. Mobile 390px had no horizontal overflow; accessibility checks passed. |
| Repository/secret controls | Harness and `git diff --check` PASS. Lead's redacted Gitleaks history/candidate checks and synthetic-data review passed; no secrets were printed. |

The first browser pass exposed a demo-label heading-order issue and an editor
accessible-name/test-locator issue. Both were corrected before the green
frozen rerun. A registration state reset during an active LoginPage HMR edit
did not recur after freeze; no authentication logic was weakened. Browser
discovery now selects only `*.spec.ts`, keeping Node fixture tests separate.

## Security remediations and dependency repair

The previously unguarded public demo principal is now isolated by complete
server-side startup validation and independent pre-claim generation checks.
Reserved registration/login/refresh fail outside safe mode, including old-JWT
provider attempts. Existing account mismatch fails rather than resetting its
password or adopting its content. Metadata reveals only the deliberate public
account after readiness. Fixture runs have truthful provenance and unknown
token counts. The external stub binds loopback, checks its public sentinel,
bounds bodies to 256 KiB and has no proxy implementation.

The independent review found unbounded provider JSON buffering and then
verified that Spring response cleanup also drained unread bytes. The final
adapter owns a cancellable JDK input stream through `exchange(..., false)`;
try-with-resources closes it before status/size rejection. Redirects are
disabled, a total response deadline applies, and the body cap is 2 MiB plus
one overflow byte before parsing. Real loopback 32 MiB streaming tests prove
early disconnect for successful and rejected status codes, alongside exact-cap
and normal parsing controls and real redirect refusal. Errors retain no raw
transport cause. Deadline behavior was also reviewed against installed
Spring bytecode; a separate slow-drip regression was not added.

User-approved existing-dependency patches: Tomcat 10.1.60, Jackson BOM 2.21.7,
Bouncy Castle 1.85, test-only Commons Compress 1.26.0, brace-expansion 5.0.12
and undici 7.29.1. Both expired Tomcat suppressions were removed. No acceptance
was renewed and no audit threshold was weakened.

Npm's optional dependency resolution omitted the already-required Rolldown
WASM `@emnapi/core` and `@emnapi/runtime` 2.0.0-alpha.3 packages while retaining
their dependent package. Reinstall/update and an isolated reproduction showed
that temporary root hints were removed again. The Lead explicitly approved
two exact devDependency declarations for these existing toolchain transitives;
the Reviewer approved the narrow repair. The npm-generated lock now reproduces
cleanly with no new runtime library or unrelated version change.

## Remaining limits and publication evidence

Docker is unavailable on this desktop: five Docker-dependent backend tests,
the Docker network-namespace topology and container scans were not executed
locally. The reviewed topology and standalone PostgreSQL/API/browser evidence
must not be represented as Docker runtime evidence. Fresh OWASP feed analysis
and all seven repository CI jobs for the eventual exact commit SHA remain
separate publication gates; this plan does not claim their completion.

The registered desktop Codex Security scan launcher was unavailable. The Lead
performed independent local repository, dependency, redacted-secret and
synthetic-data checks, with specialist code review. No live AI semantic
evaluation, paid provider call, Salesforce org operation, Jira/ADO connection,
or durable multiuser execution archive is claimed.

The backend jar verified at completion has SHA-256
`C025D1AC7922D4CAE61ACF2F784F7A4D539E32DC7E332FDE7A5EDD8AF79EAAAC`.
It packages patched Jackson/Tomcat/Bouncy Castle artifacts; Commons Compress
remains test-only and is absent from the runtime jar.
