import type { TokenResponse } from '../types/api';

let accessToken: string | null = null;
let csrfToken: string | null = null;
interface SessionRefreshOutcome {
  session: TokenResponse | null;
  current: boolean;
}

let refreshPromise: Promise<SessionRefreshOutcome> | null = null;
let csrfPromise: Promise<string> | null = null;
let authEpoch = 0;
const sessionInvalidatedListeners = new Set<() => void>();

export class ApiError extends Error {
  /** Preserves normalized HTTP status, problem code, and field errors for the UI. */
  constructor(
    public readonly status: number,
    message: string,
    public readonly code?: string,
    public readonly errors?: Record<string, string>,
  ) {
    super(message);
  }
}

/** Replaces the in-memory bearer token without persisting credentials in browser storage. */
export function setAccessToken(value: string | null) {
  accessToken = value;
}

/** Clears every session-scoped cache and returns the new authentication epoch. */
export function resetApiClient() {
  authEpoch += 1;
  accessToken = null;
  csrfToken = null;
  refreshPromise = null;
  csrfPromise = null;
  return authEpoch;
}

/** Subscribes application auth state to current-session refresh failures. */
export function subscribeSessionInvalidated(listener: () => void) {
  sessionInvalidatedListeners.add(listener);
  return () => sessionInvalidatedListeners.delete(listener);
}

/** Clears and broadcasts only while the failing refresh still owns the active epoch. */
function invalidateSessionForEpoch(expectedEpoch: number) {
  if (expectedEpoch !== authEpoch) return false;
  resetApiClient();
  sessionInvalidatedListeners.forEach((listener) => listener());
  return true;
}

/** Installs a token only when its authentication attempt is still current. */
export function setAccessTokenForEpoch(value: string, expectedEpoch: number) {
  if (expectedEpoch !== authEpoch) return false;
  setAccessToken(value);
  return true;
}

/** Lazily obtains and caches the CSRF token required for state-changing requests. */
async function ensureCsrf(): Promise<string> {
  if (csrfToken) return csrfToken;
  if (csrfPromise) return csrfPromise;
  const requestEpoch = authEpoch;
  const pending = (async () => {
    const response = await fetch('/api/v1/auth/csrf', { credentials: 'include' });
    if (!response.ok) {
      throw new ApiError(response.status, 'Could not initialize request security.');
    }
    const data = (await response.json()) as { token: string };
    if (requestEpoch !== authEpoch) {
      throw new ApiError(401, 'The authentication session was reset.');
    }
    csrfToken = data.token;
    return data.token;
  })();
  csrfPromise = pending;
  try {
    return await pending;
  } finally {
    if (csrfPromise === pending) csrfPromise = null;
  }
}

/** Coalesces refresh work while distinguishing a current result from a stale completion. */
async function refreshSessionOutcome(): Promise<SessionRefreshOutcome> {
  if (refreshPromise) return refreshPromise;
  const requestEpoch = authEpoch;
  const pending = (async () => {
    try {
      const csrf = await ensureCsrf();
      const response = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': csrf },
        credentials: 'include',
      });
      if (!response.ok) {
        if (requestEpoch !== authEpoch) return { session: null, current: false };
        invalidateSessionForEpoch(requestEpoch);
        return { session: null, current: true };
      }
      const session = (await response.json()) as TokenResponse;
      if (requestEpoch !== authEpoch) return { session: null, current: false };
      setAccessToken(session.accessToken);
      return { session, current: true };
    } catch {
      if (requestEpoch !== authEpoch) return { session: null, current: false };
      invalidateSessionForEpoch(requestEpoch);
      return { session: null, current: true };
    }
  })();
  refreshPromise = pending;
  try {
    return await pending;
  } finally {
    if (refreshPromise === pending) refreshPromise = null;
  }
}

/** Restores a session and reports whether its completion still owns the auth epoch. */
export async function restoreSession(): Promise<SessionRefreshOutcome> {
  return refreshSessionOutcome();
}

/** Refreshes the access token, mapping stale completions to the existing null contract. */
export async function refreshSession(): Promise<TokenResponse | null> {
  const outcome = await refreshSessionOutcome();
  return outcome.current ? outcome.session : null;
}

/** Sends an authenticated API request and retries once after a successful token rotation. */
export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  retry = true,
): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    headers.set('X-XSRF-TOKEN', await ensureCsrf());
  }
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const response = await fetch(path, { ...init, headers, credentials: 'include' });
  if (response.status === 401 && retry && !path.startsWith('/api/v1/auth/')) {
    const refreshed = await refreshSession();
    if (refreshed) return apiRequest<T>(path, init, false);
  }
  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

/** Downloads a generated artifact and transparently recovers from one expired access token. */
export async function downloadExport(
  userStoryId: string,
  format: string,
  retry = true,
  generationRunId?: string,
) {
  const headers = new Headers();
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  const query = new URLSearchParams({ format });
  if (generationRunId) query.set('generationRunId', generationRunId);
  const response = await fetch(`/api/v1/user-stories/${userStoryId}/export?${query.toString()}`, {
    headers,
    credentials: 'include',
  });
  if (response.status === 401 && retry) {
    const refreshed = await refreshSession();
    if (refreshed) return downloadExport(userStoryId, format, false, generationRunId);
  }
  if (!response.ok) throw await toApiError(response);
  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const name = exportFilename(disposition, format);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = name;
  anchor.click();
  URL.revokeObjectURL(url);
}

/** Derives a safe export filename from response metadata with a conservative fallback. */
function exportFilename(disposition: string, format: string) {
  const extended = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (extended) {
    try {
      return decodeURIComponent(extended.trim());
    } catch {
      // Fall through to the conservative filename or application fallback.
    }
  }
  const basic = disposition.match(/filename="?([^";]+)"?/i)?.[1];
  return basic ?? `testforge-export.${format === 'markdown' ? 'md' : format}`;
}

/** Normalizes problem details and non-JSON failures into a stable UI error. */
async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as {
      detail?: string;
      title?: string;
      code?: string;
      errors?: Record<string, string>;
    };
    return new ApiError(
      response.status,
      body.detail ?? body.title ?? 'The request could not be completed.',
      body.code,
      body.errors,
    );
  } catch {
    return new ApiError(response.status, 'The request could not be completed.');
  }
}
