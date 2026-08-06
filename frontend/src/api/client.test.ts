import { http, HttpResponse } from 'msw';
import {
  ApiError,
  apiRequest,
  downloadExport,
  refreshSession,
  resetApiClient,
  setAccessToken,
  subscribeSessionInvalidated,
} from './client';
import { server } from '../test/server';

describe('API client security and error behavior', () => {
  afterEach(() => resetApiClient());

  it('refreshes once after an expired access token and retries the request', async () => {
    let attempts = 0;
    server.use(
      http.get('/api/v1/secure-resource', () => {
        attempts += 1;
        return attempts === 1
          ? HttpResponse.json({ detail: 'Expired.' }, { status: 401 })
          : HttpResponse.json({ status: 'recovered' });
      }),
      http.post('/api/v1/auth/refresh', () =>
        HttpResponse.json({
          accessToken: 'replacement-token',
          expiresInSeconds: 600,
          user: {
            id: 'user-id',
            email: 'demo@testforge.local',
            displayName: 'Maya Chen',
            role: 'USER',
            createdAt: '2026-07-30T12:00:00Z',
          },
        }),
      ),
    );
    setAccessToken('expired-token');
    await expect(apiRequest<{ status: string }>('/api/v1/secure-resource')).resolves.toEqual({
      status: 'recovered',
    });
    expect(attempts).toBe(2);
  });

  it('maps problem details and field errors into a typed API error', async () => {
    server.use(
      http.post('/api/v1/invalid', () =>
        HttpResponse.json(
          {
            detail: 'One or more fields are invalid.',
            code: 'validation_failed',
            errors: { title: 'Enter a title.' },
          },
          { status: 400 },
        ),
      ),
    );
    const failure = await apiRequest('/api/v1/invalid', { method: 'POST', body: '{}' }).catch(
      (error: unknown) => error,
    );
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({
      status: 400,
      code: 'validation_failed',
      errors: { title: 'Enter a title.' },
    });
  });

  it('uses a safe generic error when an upstream response is not JSON', async () => {
    server.use(
      http.get(
        '/api/v1/broken-upstream',
        () => new HttpResponse('gateway returned an invalid document', { status: 502 }),
      ),
    );
    const failure = await apiRequest('/api/v1/broken-upstream').catch((error: unknown) => error);
    expect(failure).toMatchObject({ status: 502, message: 'The request could not be completed.' });
  });

  it('downloads an approved export using the server filename', async () => {
    let requestedRunId: string | null = null;
    server.use(
      http.get('/api/v1/user-stories/requirement-id/export', ({ request }) => {
        requestedRunId = new URL(request.url).searchParams.get('generationRunId');
        return new HttpResponse('testCaseKey,title\r\nTC-1,Safe title', {
          headers: {
            'Content-Type': 'text/csv',
            'Content-Disposition':
              'attachment; filename="=?UTF-8?Q?encoded-fallback.csv?="; filename*=UTF-8\'\'approved-cases.csv',
          },
        });
      }),
    );
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test-export');
    const revokeObjectUrl = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    await downloadExport('requirement-id', 'csv', true, 'historical-run-id');

    expect(requestedRunId).toBe('historical-run-id');
    expect(createObjectUrl).toHaveBeenCalledOnce();
    expect(click).toHaveBeenCalledOnce();
    expect((click.mock.instances[0] as HTMLAnchorElement).download).toBe('approved-cases.csv');
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:test-export');
  });

  it('refreshes an expired session once before downloading an export', async () => {
    let attempts = 0;
    const authorizationHeaders: Array<string | null> = [];
    server.use(
      http.get('/api/v1/user-stories/requirement-id/export', ({ request }) => {
        attempts += 1;
        authorizationHeaders.push(request.headers.get('Authorization'));
        return attempts === 1
          ? HttpResponse.json({ detail: 'Expired.' }, { status: 401 })
          : new HttpResponse('[]', {
              headers: {
                'Content-Type': 'application/json',
                'Content-Disposition': 'attachment; filename="approved-cases.json"',
              },
            });
      }),
      http.post('/api/v1/auth/refresh', () =>
        HttpResponse.json({
          accessToken: 'replacement-export-token',
          expiresInSeconds: 600,
          user: {
            id: 'user-id',
            email: 'demo@testforge.local',
            displayName: 'Maya Chen',
            role: 'USER',
            createdAt: '2026-07-30T12:00:00Z',
          },
        }),
      ),
    );
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:refreshed-export');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    setAccessToken('expired-export-token');

    await downloadExport('requirement-id', 'json');

    expect(attempts).toBe(2);
    expect(authorizationHeaders).toEqual([
      'Bearer expired-export-token',
      'Bearer replacement-export-token',
    ]);
  });

  it('coalesces concurrent CSRF initialization requests', async () => {
    let csrfRequests = 0;
    server.use(
      http.get('/api/v1/auth/csrf', () => {
        csrfRequests += 1;
        return HttpResponse.json({ token: 'coalesced-csrf' });
      }),
      http.post('/api/v1/write-one', () => HttpResponse.json({ ok: true })),
      http.post('/api/v1/write-two', () => HttpResponse.json({ ok: true })),
    );

    await Promise.all([
      apiRequest('/api/v1/write-one', { method: 'POST' }),
      apiRequest('/api/v1/write-two', { method: 'POST' }),
    ]);

    expect(csrfRequests).toBe(1);
  });

  it('prevents a refresh from an older reset epoch from reinstalling credentials', async () => {
    let releaseRefresh!: () => void;
    let refreshStarted = false;
    const refreshGate = new Promise<void>((resolve) => {
      releaseRefresh = resolve;
    });
    const authorizationHeaders: Array<string | null> = [];
    server.use(
      http.post('/api/v1/auth/refresh', async () => {
        refreshStarted = true;
        await refreshGate;
        return HttpResponse.json({
          accessToken: 'stale-token',
          expiresInSeconds: 600,
          user: {
            id: 'old-user',
            email: 'old@testforge.local',
            displayName: 'Old Session',
            role: 'USER',
            createdAt: '2026-07-30T12:00:00Z',
          },
        });
      }),
      http.get('/api/v1/session-probe', ({ request }) => {
        authorizationHeaders.push(request.headers.get('Authorization'));
        return HttpResponse.json({ ok: true });
      }),
    );

    const staleRefresh = refreshSession();
    await vi.waitFor(() => expect(refreshStarted).toBe(true));
    resetApiClient();
    releaseRefresh();

    await expect(staleRefresh).resolves.toBeNull();
    await apiRequest('/api/v1/session-probe');
    expect(authorizationHeaders).toEqual([null]);
  });

  it('broadcasts only a current refresh failure', async () => {
    let invalidations = 0;
    const unsubscribe = subscribeSessionInvalidated(() => {
      invalidations += 1;
    });
    server.use(
      http.post('/api/v1/auth/refresh', () =>
        HttpResponse.json({ detail: 'Expired.' }, { status: 401 }),
      ),
    );

    await expect(refreshSession()).resolves.toBeNull();
    expect(invalidations).toBe(1);
    resetApiClient();
    expect(invalidations).toBe(1);
    unsubscribe();
  });
});
