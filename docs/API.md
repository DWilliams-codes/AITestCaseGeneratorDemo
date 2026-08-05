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

Passwords are 12–128 characters. Emails are normalized. Authentication failures do not disclose whether an account exists.

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

Collections are paged with `page` and `size` where applicable. Update bodies include `version`; stale writes return `409` with `code=stale_version`. A User Story must retain at least one criterion, may contain at most 50, and has priority `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW` (default `MEDIUM`). Project responses expose `userStoryCount` and retain deprecated `requirementCount`. Equivalent `/requirements` routes are compatibility adapters over the same services and DTOs.

## Generation and test cases

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/user-stories/{userStoryId}/generate-test-cases` | Generate initial cases |
| `POST` | `/user-stories/{userStoryId}/regenerate?confirmSupersede=false` | Generate a new immutable set |
| `GET` | `/user-stories/{userStoryId}/generation-runs` | List attempts with stable set number/state |
| `GET` | `/generation-runs/{runId}` | Inspect provider/run metadata |
| `GET` | `/user-stories/{userStoryId}/test-cases?generationRunId=` | List active or selected historical cases |
| `GET`, `PATCH` | `/test-cases/{testCaseId}` | Read or edit a full structured case |
| `POST` | `/test-cases/{testCaseId}/approve` | Approve after human review |
| `POST` | `/test-cases/{testCaseId}/reject` | Reject after human review |
| `POST` | `/test-cases/{testCaseId}/request-changes` | Return the case for changes |
| `POST` | `/test-cases/{testCaseId}/reopen` | Reopen an approved/rejected active case with reason/version |
| `GET` | `/test-cases/{testCaseId}/revisions` | Page normalized structured revisions |

Generation requests require a nonblank `Idempotency-Key` header. Reusing a key for the same actor and User Story returns the original run. Only the latest successful run by `completedAt` then UUID is active; failed attempts never supersede it. Regeneration returns `409 supersede_confirmation_required` when the active set has revision/review evidence unless confirmation is explicit. Review/reopen bodies include expected `version`; reject and request-changes comments and reopen reasons are required. Superseded sets and terminal cases are read-only until a valid reopen.

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
