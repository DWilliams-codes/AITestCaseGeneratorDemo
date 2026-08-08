# API Guide

The API prefix is `/api/v1`. JSON errors use `application/problem+json` and contain a stable `code`. Authenticated routes require `Authorization: Bearer <access-token>`. Cookie-authenticated auth routes also require the CSRF cookie/header pair obtained from `GET /api/v1/auth/csrf`.

## Example authentication request

Initialize the CSRF cookie, then send its value in `X-XSRF-TOKEN`. The refresh token returned by login remains in an HttpOnly cookie; the access token belongs in memory and the bearer header only.

```bash
curl --cookie-jar cookies.txt http://localhost:8080/api/v1/auth/csrf
curl --cookie cookies.txt --cookie-jar cookies.txt \
  --header "Content-Type: application/json" \
  --header "X-XSRF-TOKEN: <value-of-XSRF-TOKEN-cookie>" \
  --data '{"email":"analyst@example.test","password":"a-long-unique-passphrase"}' \
  http://localhost:8080/api/v1/auth/login
```

Successful login returns an access token, expiry, and the current user:

```json
{
  "accessToken": "<short-lived-token>",
  "expiresInSeconds": 600,
  "user": {
    "id": "8d42d18d-606c-4fb1-ae87-f47b99919163",
    "email": "analyst@example.test",
    "displayName": "QA Analyst",
    "role": "USER",
    "createdAt": "2026-07-30T21:00:00Z"
  }
}
```

## Authentication

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/auth/csrf` | Initialize the readable CSRF cookie |
| `POST` | `/auth/register` | Create an account and token family |
| `POST` | `/auth/login` | Authenticate and create a token family |
| `POST` | `/auth/refresh` | Rotate the refresh token and issue access token |
| `POST` | `/auth/logout` | Revoke the current family and expire its cookie |
| `GET` | `/auth/me` | Return the authenticated user |

Passwords are 12–128 characters. Emails are normalized. Authentication failures do not disclose whether an account exists. Every rejected refresh returns the same `401 authentication_failed` detail and expires the refresh cookie, including missing, malformed, expired, disabled-user, and replayed-token cases.

## Projects and User Stories

| Method | Path | Purpose |
| --- | --- | --- |
| `GET`, `POST` | `/projects` | List or create owned projects |
| `GET`, `PATCH` | `/projects/{projectId}` | Read or update an owned project |
| `DELETE` | `/projects/{projectId}` | Archive a project |
| `GET`, `POST` | `/projects/{projectId}/user-stories` | List or create User Stories |
| `GET`, `PATCH` | `/user-stories/{userStoryId}` | Read or update a User Story |
| `POST` | `/user-stories/{userStoryId}/acceptance-criteria` | Add a criterion |
| `PATCH`, `DELETE` | `/acceptance-criteria/{criterionId}` | Update or remove a criterion |
| `POST` | `/ambiguities/{ambiguityId}/resolve` | Record an ambiguity resolution |

Collections are paged with `page` and `size` where applicable. Update bodies include `version`; stale writes return `409` with `code=stale_version`. Every acceptance-criterion mutation additionally requires the owning User Story's current version as one strong quoted numeric `If-Match` value (for example, `If-Match: "4"`). A successful mutation advances the User Story version and stores the complete ordered pre-change criteria; a stale header returns `409 stale_version`. A User Story must retain at least one criterion, may contain at most 50, and has priority `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW` (default `MEDIUM`). Project responses expose `userStoryCount` and retain deprecated `requirementCount`. Equivalent `/requirements` routes are compatibility adapters over the same services and DTOs.

## Generation and test cases

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/user-stories/{userStoryId}/generate-test-cases` | Generate initial cases |
| `POST` | `/user-stories/{userStoryId}/regenerate?confirmSupersede=false` | Generate a new immutable set |
| `GET` | `/user-stories/{userStoryId}/generation-runs` | List attempts with stable set number/state |
| `GET` | `/user-stories/{userStoryId}/generation-runs/page?page=&size=` | Canonical bounded attempt history plus stable `activeGenerationRunId` |
| `GET` | `/generation-runs/{runId}` | Inspect provider/run metadata |
| `GET` | `/user-stories/{userStoryId}/test-cases?generationRunId=` | List active or selected historical cases |
| `GET` | `/user-stories/{userStoryId}/test-cases/page?generationRunId=&page=&size=` | Canonical bounded active/historical cases |
| `GET`, `PATCH` | `/test-cases/{testCaseId}` | Read or edit a full structured case |
| `POST` | `/test-cases/{testCaseId}/approve` | Approve after human review |
| `POST` | `/test-cases/{testCaseId}/reject` | Reject after human review |
| `POST` | `/test-cases/{testCaseId}/request-changes` | Return the case for changes |
| `POST` | `/test-cases/{testCaseId}/reopen` | Reopen an approved/rejected active case with reason/version |
| `GET` | `/test-cases/{testCaseId}/revisions` | Page normalized structured revisions |
| `GET` | `/test-cases/{testCaseId}/reviews` | Page immutable review decisions |

Generation requests require a nonblank `Idempotency-Key` header. Reusing a key for the same actor and User Story returns the original run without another provider invocation; bridge-written rows are reconciled before that POST response. A claim commits `PENDING` evidence, captured source criterion identities, and exact source snapshots before provider work; only `COMPLETED` is successful. A criterion key rename during provider work retains legacy dual-write compatibility through the captured owned UUID. Deleting a captured criterion terminalizes the run as `FAILED` with `failureCode=source_criteria_changed` before any case or traceability link is written. `FAILED` and `REJECTED_BY_VALIDATION` expose only bounded safe failure data and never contain partial cases. Only the latest successful run by `completedAt` then UUID is active; failed attempts never supersede it. Regeneration returns `409 supersede_confirmation_required` when the active set has revision/review evidence unless confirmation is explicit. Review/reopen bodies include expected `version`; reject and request-changes comments and reopen reasons are required. Superseded sets and terminal cases are read-only until a valid reopen.

Run responses expose the reproducibility tuple (`provider`, `model`,
`promptVersion`, `providerAdapterVersion`, `resultContractVersion`,
`schemaVersion`, `validatorVersion`), `sourceRequirementVersion`, and
`sourceSnapshotProvenance`. Pre-V6 and bridge-written runs use
`resultContractVersion=legacy-unknown` rather than claiming unavailable
historical evidence. The canonical
paged routes should be used by new clients. The legacy array routes are
deprecated bounded adapters capped at 100 rows, and embedded legacy review
history is capped at 20 rows.

The SPA stores the selected generation set, workflow tab, case filters/sort,
and collection/history pages in URL query parameters. A failed coverage,
traceability, run, or case request is displayed as unavailable with a retry
action; clients must not translate a failed evidence request into zero coverage
or an empty collection. Generation history remains visible and paged even when
an attempt is pending, failed, rejected by validation, or produced no cases.
Generation action notices are typed: completed is success, pending is
informational, provider/source failure is error, and validation rejection is a
warning. Message wording is never used to infer severity.
Full case evidence—including rationale, coverage
intent, automation-candidate guidance, test data, and step data references—is
shown before a review decision is submitted.

Test-data names are stripped and unique case-insensitively. Every nonblank step
reference resolves to exactly one canonical name; the server rejects the whole
edit or generated result before partial persistence otherwise.

Requirement summaries and details include `workItemNumber`. Test-case responses include the same field and a derived `testCaseKey` such as `TC-1042`. Both resource types draw from one database sequence, so the numeric portion is globally unique and remains stable across edits and regenerations. UUID `id` fields remain the internal API identifiers.

```bash
curl --request POST \
  --header "Authorization: Bearer <access-token>" \
  --header "Idempotency-Key: 3aa87332-84a7-42f4-92e3-cbfad0cf18ed" \
  http://localhost:8080/api/v1/user-stories/<user-story-id>/generate-test-cases
```

## Traceability, export, and audit

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/user-stories/{userStoryId}/coverage?generationRunId=` | Active/selected criterion coverage |
| `GET` | `/user-stories/{userStoryId}/traceability?generationRunId=` | Active/selected evidence matrix |
| `GET` | `/user-stories/{userStoryId}/export?format=csv|json|markdown&generationRunId=` | Download approved cases |
| `GET` | `/projects/{projectId}/audit-events` | Page owned audit evidence with optional `entityType`, `entityId`, `actorId`, `action`, `from`, and `to` filters |

Export returns `400 no_approved_test_cases` until at least one case is approved. Responses include `Content-Disposition` with a safe filename.
An explicit historical export reconciles an eligible V5-shaped post-V6 run
before reading immutable snapshot evidence. Export is bounded to 100 approved
cases and batch-assembles their child records with a fixed query family.

Traceability rows are sourced from immutable generation criterion snapshots and
identify the snapshot, retained source criterion UUID, source requirement
version, and `EXACT` or `LEGACY_RECONSTRUCTED` provenance. Primary
`coveredCriteria`/`approvedCriteria` and percentages count `DIRECT` evidence
only. Partial and supporting evidence have separate counts and percentages and
cannot satisfy direct coverage.

Reopen reasons are stored in owner-isolated revision history, not broad audit
metadata. Compatibility reads reconcile at most one bounded legacy batch. Any
unreconciled legacy row is returned with `legacyReasonRedacted=true` and
`pendingReconciliation=true`; only a committed transfer is labeled
`legacyReasonMigrated=true`.

## Operations

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/api/v1/health` | Public, minimal |
| `GET` | `/actuator/health` and probe children | Public, no details |
| `GET` | `/v3/api-docs`, `/swagger-ui.html` | Public only when explicitly enabled |

## Pagination and rate limits

Paged collections accept zero-based `page` and bounded `size` query parameters. Responses include `items`, `page`, `size`, `totalElements`, `totalPages`, and `hasNext`.

Authentication and generation each use a one-minute in-process window. Their defaults are 10 attempts per minute and can be changed with `AUTH_ATTEMPTS_PER_MINUTE` and `GENERATION_ATTEMPTS_PER_MINUTE`. A rejected request returns `429` with `code=rate_limit_exceeded`. Production deployments should enforce an additional distributed limit at the gateway before horizontal scaling.

## Representative problem

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "This test case changed since it was loaded. Refresh and retry.",
  "instance": "/api/v1/test-cases/…",
  "code": "stale_version",
  "correlationId": "4a91eb76-32fb-4c57-a2d5-458cc98e2d7a",
  "timestamp": "2026-07-30T21:00:00Z"
}
```

OpenAPI is available for exact request/response schemas in an environment started with `OPENAPI_ENABLED=true`.
## Workspace foundation

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/workspaces` | List only workspaces represented by the authenticated caller's memberships, including `callerRole` and membership status |

Registration atomically provisions a personal workspace and OWNER membership.
Project responses now include `workspaceId`; it is nullable during the
expand/backfill compatibility window, although new application-created projects
populate it. A caller-supplied workspace ID is not accepted when creating a
project.

Workspace membership does not authorize project-derived data in TF-001. Project,
requirement, test-case, generation-run, traceability, export, and audit endpoints
continue to enforce the existing owner predicate and return `404` across owner
boundaries.
