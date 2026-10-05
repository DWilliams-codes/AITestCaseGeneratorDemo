# TF-018: Spring dependency release blocker

## Objective and approved handoff

Record refreshed OWASP evidence and the unresolved Spring compatibility release
blocker. Architect approved an evidence-only handoff and Lead authorized it.
Baseline `13be562ef4dd7835ff4a512511a1370e7b0ab497` was clean. This plan is the
first repository write and must be re-read before editing the risk register.

## Scope and acceptance criteria

1. Record the failed refreshed OWASP check and affected Framework 6.2.19 and
   Security 6.5.11 findings in `docs/DEPENDENCY_RISK_ACCEPTANCE.md`. The report
   identifies affected versions; product-level CPE matches alone do not prove
   every packaged JAR is exploitable.
2. Explain the official affected/fixed version ranges and same-line enterprise
   patch availability. No free supported same-line patch was available in the
   observed Maven Central metadata. Do not buy access, suppress findings,
   weaken the CVSS 7 threshold or force Spring 7 under Boot 3.
3. Record observed servlet/JSON/JWT reachability context without declaring all
   findings unexploitable. A coordinated Boot 4 migration is future separately
   approved and tested work, not an implementation authorized by this plan.
4. Keep release BLOCK explicit. Local functional demo remains unchanged; no
   security-green claim. MEDIUM Data JPA and Swagger UI embedded DOMPurify findings
   remain report context, not the current >=7 release blockers.

## Files and protected boundaries

Only this plan and `docs/DEPENDENCY_RISK_ACCEPTANCE.md` may change. POM, Java,
UI, runtime, configuration, CI, suppressions and running processes remain
untouched. No dependency installation, provider call, runtime restart, CI
interruption or Builder publication.

## Evidence and verification

Source evidence: task workspace `owasp-final-check.log` and
`backend/target/dependency-check-report.json`; official Spring advisories and
Maven Central version availability were verified by the read-only Architect.
Run `python scripts/verify.py --harness-only` and `git diff --check` for this
documentation-only patch. No feature tests are required for unchanged code.

Obtain Architect CONFORMS and independent Reviewer APPROVE, then Lead's
triage-completion decision. Only then move this same plan to completed. A
completed triage does not resolve or accept the release blocker. Lead may separately publish the reviewed candidate under its latest explicit
authorization; new exact-SHA CI, not predecessor status, governs release evidence.

## Rollback and remaining work

Documentation may be reverted independently; there is no database or binary
rollback. That does not remove the underlying known dependency findings.
Future migration must explicitly assess data binding and ContentDisposition
export behavior, then pass compatibility, security and exact-SHA CI gates.

## Status

Completed as evidence-only triage following Architect CONFORMS, independent
Reviewer APPROVE and Lead completion decision. The Spring release BLOCK is
unresolved and is not accepted or waived by this completed plan.

## Observed local evidence

Implementation is complete within the approved file scope. The deterministic
harness passed all 94 coverage obligations and its Compose boundary checks.
`git diff --check` passed. TF-018 itself changes only this plan and its risk
register section. Concurrent TF-019 changes are separately planned and reviewed
in its own plan and manifest; they do not expand TF-018's evidence-only scope.
Formal source review and Lead completion received; release BLOCK remains.

Architect formal CONFORMS received for TF-018 evidence-only triage. Independent
Reviewer APPROVE and Lead completion were subsequently received. Release BLOCK persists.

Reviewer-requested precision distinguishes CVE-2026-47883 from -59314 and
records fixed UUID-based export filenames and JSON-body DTO binding. This
source reachability context does not clear all Spring findings or change gates.

Final handoff: local deterministic harness and whitespace checks passed. Lead
owns separately authorized normal commit/push and new exact-SHA terminal CI
review. No source completion or local browser result asserts a security-green
release. Running demonstration services were left unchanged.
