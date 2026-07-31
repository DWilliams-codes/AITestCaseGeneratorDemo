import { http, HttpResponse } from 'msw';
import { ApiError, apiRequest, downloadExport, setAccessToken } from './client';
import { server } from '../test/server';

describe('API client security and error behavior', () => {
  afterEach(() => setAccessToken(null));

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
    server.use(
      http.get(
        '/api/v1/requirements/requirement-id/export',
        () =>
          new HttpResponse('testCaseKey,title\r\nTC-1,Safe title', {
            headers: {
              'Content-Type': 'text/csv',
              'Content-Disposition':
                'attachment; filename="=?UTF-8?Q?encoded-fallback.csv?="; filename*=UTF-8\'\'approved-cases.csv',
            },
          }),
      ),
    );
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test-export');
    const revokeObjectUrl = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    await downloadExport('requirement-id', 'csv');

    expect(createObjectUrl).toHaveBeenCalledOnce();
    expect(click).toHaveBeenCalledOnce();
    expect((click.mock.instances[0] as HTMLAnchorElement).download).toBe('approved-cases.csv');
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:test-export');
  });
});
