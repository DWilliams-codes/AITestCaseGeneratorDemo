# TF-019: Frontend Alpine security updates

## Objective and approved handoff

Close ten HIGH frontend runtime-image findings observed for predecessor
`13be562ef4dd7835ff4a512511a1370e7b0ab497`. Architect approved this narrow
handoff and Lead assigned the same sole Builder. This plan is TF-019's first
repository write and is re-read before implementation. TF-018 documentation
is preserved as concurrent, separately reviewed evidence-only work.

## Scope and acceptance criteria

1. Add only `libuuid` and `pcre2` to the frontend runtime's existing
   `apk upgrade --no-cache` list, preserving all existing packages.
2. Preserve pinned Node/Nginx digests, Alpine v3.23 repositories, USER 101,
   nginx configuration, CI/scanner thresholds, application code and lockfiles.
   No mixed repository, suppression, scanner bypass or paid service.
3. Record six libuuid findings with highest fixed threshold 2.41.6-r1 and four
   pcre2 findings with highest fixed threshold 10.49-r0. Architect verified
   both versions on the existing v3.23/main/x86_64 repository. Do not present
   a source patch as proof of installed image versions or successful scanning.
4. Keep predecessor backend-image PASS separate from pending rebuilt frontend
   proof and the unresolved TF-018 Spring release BLOCK. Both rebuilt images
   require zero fixable HIGH/CRITICAL; do not claim all seven CI jobs passed.

## Files and protected boundaries

Only `frontend/Dockerfile`, `frontend/e2e/fixture-review.spec.ts`, the additive
TF-019 section in
`docs/DEPENDENCY_RISK_ACCEPTANCE.md`, and this plan. No UI, Java, lockfile,
runtime or local demo changes. Builder does not commit, push, restart services
or interrupt CI. Lead owns separately authorized normal publication.

## Evidence and verification

Predecessor findings are recorded in task-workspace `github-container-final.log`.
Run deterministic harness and `git diff --check`; inspect the four-file
TF-019 delta separately from TF-018. The approved test-only synchronization
amendment also requires targeted fixture repetition and the full 14-test suite,
plus formatting, type and lint checks. No application behavior changes.
Docker is unavailable locally; rebuilt image
versions, Trivy, Docker E2E and new exact-SHA CI require remote evidence.

Prior Docker E2E job succeeded with 13 initial passes and one retry pass;
that is not 14 first-attempt passes. Do not silently waive or weaken its test.

Obtain Architect CONFORMS and independent Reviewer APPROVE, then Lead's
implementation-completion decision before moving this same plan to completed.
Spring findings remain an independent unresolved release blocker.

## Rollback

Revert the two package additions or test synchronization independently if needed.
The predecessor frontend image has known findings and is not release-ready.
There is no database or binary rollback and no repository-branch fallback.

## Status

Completed for the authorized local source and regression scope following
Architect CONFORMS, independent Reviewer APPROVE and Lead completion decision.
Rebuilt-image proof remains pending; TF-018 release BLOCK persists.

## Observed local evidence

Implementation is complete within the approved file scope. The deterministic
harness passed all 94 coverage obligations and its Compose boundary checks.
`git diff --check` passed. No source/runtime/CI/scanner change is made outside
the approved two-package Dockerfile addition and test-only synchronization.
Formal source review and Lead completion received;
release BLOCK remains.

## Approved test synchronization amendment

Lead and Architect authorized this narrow amendment after reading the failed
predecessor E2E attempt. The test clicked Save changes and immediately reloaded,
without waiting for the asynchronous PATCH to finish. The editor closes only
after its awaited request succeeds. Amend/re-read this plan before changing
the test to await `expect(editor).toBeHidden()` between Save and reload.

Retain the exact edited-text assertion after reload, six-case/unapproved and
provenance checks, retry/timeouts and all gates. No sleeps, forced UI state,
broad response match, weaker assertion or application code change. Verify the
focused fixture repeatedly and the full 14-test suite once. Existing demo
services must be reused, not restarted or duplicated.

## Final local regression results

- Focused persisted-review fixture: 5/5 consecutive repetitions PASS in 18.7s,
  no retries, existing loopback synthetic services. Evidence:
  task-workspace `playwright-tf019-focused.log`.
- Full local browser suite: 14/14 first-attempt PASS in 7.7s. Evidence:
  task-workspace `playwright-tf019-full.log`.
- `npm run format:check`, `npm run lint`, `npm run typecheck`: PASS. The edited
  test was formatted after the initial formatting check flagged it; the final
  test diff is exactly one added successful-save wait.
- No application source, dependency lock, runtime, scanner or CI settings
  changed. Existing local services were reused. No live AI/Salesforce call.
- Docker rebuild/installed package proof remain unavailable locally and need
  new exact-SHA CI. Spring findings remain release BLOCK despite these passes.

Final handoff: local deterministic harness and whitespace checks passed. Lead
owns separately authorized normal commit/push and new exact-SHA terminal CI
review. No source completion or local browser result asserts a security-green
release. Running demonstration services were left unchanged.
