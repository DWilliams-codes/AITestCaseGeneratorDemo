import type { TokenResponse } from '../types/api';

let accessToken: string | null = null;
let csrfToken: string | null = null;
let refreshPromise: Promise<TokenResponse | null> | null = null;

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

/** Lazily obtains and caches the CSRF token required for state-changing requests. */
async function ensureCsrf(): Promise<string> {
  if (csrfToken) return csrfToken;
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'include' });
  if (!response.ok) throw new ApiError(response.status, 'Could not initialize request security.');
  const data = (await response.json()) as { token: string };
  csrfToken = data.token;
  return data.token;
}

/** Coalesces concurrent refresh attempts and restores the short-lived in-memory access token. */
export async function refreshSession(): Promise<TokenResponse | null> {
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    try {
      const csrf = await ensureCsrf();
      const response = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': csrf },
        credentials: 'include',
      });
      if (!response.ok) {
        setAccessToken(null);
        return null;
      }
      const session = (await response.json()) as TokenResponse;
      setAccessToken(session.accessToken);
      return session;
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
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
export async function downloadExport(requirementId: string, format: string, retry = true) {
  const headers = new Headers();
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  const response = await fetch(
    `/api/v1/requirements/${requirementId}/export?format=${encodeURIComponent(format)}`,
    { headers, credentials: 'include' },
  );
  if (response.status === 401 && retry) {
    const refreshed = await refreshSession();
    if (refreshed) return downloadExport(requirementId, format, false);
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
