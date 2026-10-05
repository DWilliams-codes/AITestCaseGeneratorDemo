# TF-017: Backend Alpine security updates

## Objective and approved handoff

Remediate five HIGH runtime-image findings observed in the exact-SHA container
scan for predecessor 529cbd85fae0a0d22ed81babc63f37745f10e4a7. The Architect
approved this narrow handoff and assigned the same sole Builder. This plan is
the first repository write for TF-017 and must be re-read before implementation.
The predecessor tree was clean. Preserve the running local demo.

## Scope and acceptance criteria

1. Extend the existing backend runtime `apk upgrade --no-cache` package list
   with `libcrypto3 libssl3 openssl sqlite-libs`, keeping current libexpat and
   p11-kit updates. Retain the pinned Temurin Alpine3.23 digest, build stage,
   curl health tooling, `USER testforge`, ports and entrypoint.
2. Use only the existing Alpine branch/repositories. No edge or mixed
   repositories, scanner suppression/threshold changes, ignore-unfixed change,
   base/toolchain jump or unrelated dependency update.
3. Document observed findings and known fixed-version thresholds: OpenSSL
   packages 3.5.8-r0 for CVE-2026-14456; sqlite-libs 3.53.4-r0 for
   CVE-2026-11822 and CVE-2026-11824. Do not claim rebuilt-image remediation
   until its exact-SHA scan passes. If fixed packages are unavailable in v3.23,
   BLOCK rather than silently changing repository branches.
4. Preserve all seven CI jobs, backend/frontend container scanning with zero
   fixable HIGH/CRITICAL, and the 14 Docker E2E tests. Local source conformance
   does not replace installed-package or container execution evidence.

## Files and compatibility

Only `backend/Dockerfile`, `docs/DEPENDENCY_RISK_ACCEPTANCE.md`, and this plan.
No Java, API, schema, UI, provider, credentials or running-process change. No
model call, dependency install on the desktop, or Builder commit/push/deploy.
The Lead owns separately authorized normal publication.

## Verification and review

Run the deterministic repository harness first and `git diff --check`; inspect
the exact changed-file surface and preserved digest/non-root/scanner controls.
Docker is unavailable locally, so image construction, installed fixed versions
and scans require new exact-SHA CI evidence. Application tests need not repeat
locally because no application source or dependency changes.

Return the patch and observed local evidence to the Architect for CONFORMS and
independent Reviewer for APPROVE. The Lead decides implementation completion;
only then does this same Builder move the plan to completed. All seven CI jobs
must pass the eventual candidate SHA before publication is reported successful.

## Rollback

Revert only this narrow patch if needed; there is no database rollback. The old
image remains non-publishable while its known container findings persist.

## Status

Completed for the authorized local source implementation scope. Architect CONFORMS,
independent Reviewer APPROVE and Lead completion authorization were received.
Publication and rebuilt-image remediation remain subject to the exact-SHA gates below.

- First repository write created this plan and it was re-read before edits.
- `python scripts/verify.py --harness-only`: PASS, including Compose loopback
  checks and all 94 manual-QA coverage obligations.
- `git diff --check`: PASS. The implementation changes only one runtime apk
  upgrade line; the other changed files are the canonical risk register and plan.
- Inspection confirms the pinned build/runtime digests, existing Alpine
  repositories, non-root user, curl health check and seven CI jobs are unchanged.
- No application tests were repeated for this Dockerfile-only implementation.
  Existing local demonstration processes remain untouched.
- Docker is unavailable locally. Fixed installed versions, rebuilt backend and
  frontend scans, Docker E2E and exact-SHA seven-job CI remain pending; this is
  a source-ready patch, not a claim of successful image remediation.


Formal review confirmed the package list covers the reported findings while
preserving pinned images, repository branch, non-root execution and scanner
policy. Lead owns the next authorized normal commit/push and CI review. No
successful image-remediation or eventual-candidate CI result is asserted here.
