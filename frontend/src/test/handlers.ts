import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('/api/v1/auth/csrf', () =>
    HttpResponse.json({ headerName: 'X-XSRF-TOKEN', token: 'test-csrf-token' }),
  ),
  http.post('/api/v1/auth/refresh', () =>
    HttpResponse.json(
      { detail: 'No active session.', code: 'authentication_failed' },
      { status: 401 },
    ),
  ),
  http.get('/api/v1/health', () =>
    HttpResponse.json({
      status: 'UP',
      service: 'testforge-backend',
      timestamp: '2026-07-30T12:00:00Z',
    }),
  ),
];
