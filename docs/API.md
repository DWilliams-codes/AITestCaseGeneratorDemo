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

## Projects and requirements

| Method | Path | Purpose |
| --- | --- | --- |
| `GET`, `POST` | `/projects` | List or create owned projects |
| `GET`, `PATCH` | `/projects/{projectId}` | Read or update an owned project |
| `DELETE` | `/projects/{projectId}` | Archive a project |
| `GET`, `POST` | `/projects/{projectId}/requirements` | List or create requirements |
| `GET`, `PATCH` | `/requirements/{requirementId}` | Read or update a requirement |
| `POST` | `/requirements/{requirementId}/acceptance-criteria` | Add a criterion |
| `PATCH`, `DELETE` | `/acceptance-criteria/{criterionId}` | Update or remove a criterion |
| `POST` | `/ambiguities/{ambiguityId}/resolve` | Record an ambiguity resolution |

Collections are paged with `page` and `size` where applicable. Update bodies include `version`; stale writes return `409` with `code=stale_version`. A requirement must retain at least one criterion and may contain at most 50.

## Generation and test cases

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/requirements/{requirementId}/generate-test-cases` | Generate initial cases |
| `POST` | `/requirements/{requirementId}/regenerate` | Generate an additional validated revision set |
| `GET` | `/generation-runs/{runId}` | Inspect provider/run metadata |
| `GET` | `/requirements/{requirementId}/test-cases` | List structured cases |
| `GET`, `PATCH` | `/test-cases/{testCaseId}` | Read or edit a full structured case |
| `POST` | `/test-cases/{testCaseId}/approve` | Approve after human review |
| `POST` | `/test-cases/{testCaseId}/reject` | Reject after human review |
| `POST` | `/test-cases/{testCaseId}/request-changes` | Return the case for changes |

Generation requests require a nonblank `Idempotency-Key` header. Reusing a key for the same actor and requirement returns the original run. Generation run statuses include completed, safe provider failure, and validation rejection. Editing a case creates a revision and moves it to review state.

Requirement summaries and details include `workItemNumber`. Test-case responses include the same field and a derived `testCaseKey` such as `TC-1042`. Both resource types draw from one database sequence, so the numeric portion is globally unique and remains stable across edits and regenerations. UUID `id` fields remain the internal API identifiers.

```bash
curl --request POST \
  --header "Authorization: Bearer <access-token>" \
  --header "Idempotency-Key: 3aa87332-84a7-42f4-92e3-cbfad0cf18ed" \
  http://localhost:8080/api/v1/requirements/<requirement-id>/generate-test-cases
```

## Traceability, export, and audit

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/requirements/{requirementId}/coverage` | Raw and approved criterion coverage |
| `GET` | `/requirements/{requirementId}/traceability` | Criterion-to-test evidence matrix |
| `GET` | `/requirements/{requirementId}/export?format=csv|json|markdown` | Download approved cases |
| `GET` | `/projects/{projectId}/audit-events` | Page through owned audit evidence |

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
