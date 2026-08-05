import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useState } from 'react';
import { apiRequest, refreshSession, resetApiClient } from '../api/client';
import { server } from '../test/server';
import { AuthProvider } from './AuthContext';
import { useAuth } from './useAuth';

const oldUser = {
  id: '10000000-0000-0000-0000-000000000010',
  email: 'old-login@testforge.local',
  displayName: 'Old Login',
  role: 'USER',
  createdAt: '2026-08-04T12:00:00Z',
};

const registeredUser = {
  id: '10000000-0000-0000-0000-000000000020',
  email: 'new-registration@testforge.local',
  displayName: 'New Registration',
  role: 'USER',
  createdAt: '2026-08-04T12:01:00Z',
};

/** Exposes authentication operations and state for deterministic race testing. */
function AuthRaceHarness() {
  const { user, loading, login, register, logout } = useAuth();
  const [completion, setCompletion] = useState('idle');

  return (
    <div>
      <output aria-label="Session user">
        {loading ? 'loading' : (user?.email ?? 'signed out')}
      </output>
      <output aria-label="Last completion">{completion}</output>
      <button
        type="button"
        onClick={() => {
          void login(oldUser.email, 'TestForge!OldLogin2026').then(() =>
            setCompletion('login completed'),
          );
        }}
      >
        Begin login
      </button>
      <button
        type="button"
        onClick={() => {
          void register(
            registeredUser.email,
            registeredUser.displayName,
            'TestForge!NewRegistration2026',
          ).then(() => setCompletion('registration completed'));
        }}
      >
        Begin registration
      </button>
      <button
        type="button"
        onClick={() => {
          void logout().then(() => setCompletion('logout completed'));
        }}
      >
        Log out
      </button>
    </div>
  );
}

describe('AuthProvider authentication epochs', () => {
  afterEach(() => resetApiClient());

  it('keeps a newer login when the delayed mount-time refresh completes stale', async () => {
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
          accessToken: 'stale-refresh-token',
          expiresInSeconds: 600,
          user: oldUser,
        });
      }),
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json({
          accessToken: 'current-login-token',
          expiresInSeconds: 600,
          user: registeredUser,
        }),
      ),
      http.get('/api/v1/session-probe', ({ request }) => {
        authorizationHeaders.push(request.headers.get('Authorization'));
        return HttpResponse.json({ ok: true });
      }),
    );

    render(
      <AuthProvider>
        <AuthRaceHarness />
      </AuthProvider>,
    );
    expect(screen.getByLabelText('Session user')).toHaveTextContent('loading');
    await waitFor(() => expect(refreshStarted).toBe(true));
    const initialRefresh = refreshSession();

    fireEvent.click(screen.getByRole('button', { name: 'Begin login' }));
    await waitFor(() =>
      expect(screen.getByLabelText('Session user')).toHaveTextContent(registeredUser.email),
    );
    expect(screen.getByLabelText('Last completion')).toHaveTextContent('login completed');

    await act(async () => {
      releaseRefresh();
      await expect(initialRefresh).resolves.toBeNull();
    });
    expect(screen.getByLabelText('Session user')).toHaveTextContent(registeredUser.email);
    await apiRequest('/api/v1/session-probe');
    expect(authorizationHeaders).toEqual(['Bearer current-login-token']);
  });

  it('does not install a delayed login response after logout resets the session', async () => {
    let releaseLogin!: () => void;
    let loginStarted = false;
    const loginGate = new Promise<void>((resolve) => {
      releaseLogin = resolve;
    });
    const authorizationHeaders: Array<string | null> = [];
    server.use(
      http.post('/api/v1/auth/login', async () => {
        loginStarted = true;
        await loginGate;
        return HttpResponse.json({
          accessToken: 'stale-login-token',
          expiresInSeconds: 600,
          user: oldUser,
        });
      }),
      http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 })),
      http.get('/api/v1/session-probe', ({ request }) => {
        authorizationHeaders.push(request.headers.get('Authorization'));
        return HttpResponse.json({ ok: true });
      }),
    );

    render(
      <AuthProvider>
        <AuthRaceHarness />
      </AuthProvider>,
    );
    await waitFor(() =>
      expect(screen.getByLabelText('Session user')).toHaveTextContent('signed out'),
    );

    fireEvent.click(screen.getByRole('button', { name: 'Begin login' }));
    await waitFor(() => expect(loginStarted).toBe(true));
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }));
    expect(await screen.findByLabelText('Last completion')).toHaveTextContent('logout completed');

    act(() => releaseLogin());
    await waitFor(() =>
      expect(screen.getByLabelText('Last completion')).toHaveTextContent('login completed'),
    );
    expect(screen.getByLabelText('Session user')).toHaveTextContent('signed out');
    await apiRequest('/api/v1/session-probe');
    expect(authorizationHeaders).toEqual([null]);
  });

  it('keeps a newer registration when an older login response completes last', async () => {
    let releaseLogin!: () => void;
    let loginStarted = false;
    const loginGate = new Promise<void>((resolve) => {
      releaseLogin = resolve;
    });
    const authorizationHeaders: Array<string | null> = [];
    server.use(
      http.post('/api/v1/auth/login', async () => {
        loginStarted = true;
        await loginGate;
        return HttpResponse.json({
          accessToken: 'stale-login-token',
          expiresInSeconds: 600,
          user: oldUser,
        });
      }),
      http.post('/api/v1/auth/register', () =>
        HttpResponse.json(
          {
            accessToken: 'current-registration-token',
            expiresInSeconds: 600,
            user: registeredUser,
          },
          { status: 201 },
        ),
      ),
      http.get('/api/v1/session-probe', ({ request }) => {
        authorizationHeaders.push(request.headers.get('Authorization'));
        return HttpResponse.json({ ok: true });
      }),
    );

    render(
      <AuthProvider>
        <AuthRaceHarness />
      </AuthProvider>,
    );
    await waitFor(() =>
      expect(screen.getByLabelText('Session user')).toHaveTextContent('signed out'),
    );

    fireEvent.click(screen.getByRole('button', { name: 'Begin login' }));
    await waitFor(() => expect(loginStarted).toBe(true));
    fireEvent.click(screen.getByRole('button', { name: 'Begin registration' }));
    await waitFor(() =>
      expect(screen.getByLabelText('Session user')).toHaveTextContent(registeredUser.email),
    );

    act(() => releaseLogin());
    await waitFor(() =>
      expect(screen.getByLabelText('Last completion')).toHaveTextContent('login completed'),
    );
    expect(screen.getByLabelText('Session user')).toHaveTextContent(registeredUser.email);
    await apiRequest('/api/v1/session-probe');
    expect(authorizationHeaders).toEqual(['Bearer current-registration-token']);
  });
});
