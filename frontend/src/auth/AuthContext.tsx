import { useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { apiRequest, resetApiClient, restoreSession, setAccessTokenForEpoch } from '../api/client';
import type { TokenResponse, User } from '../types/api';
import { AuthContext, type AuthContextValue } from './auth-context';

/** Restores and exposes the authenticated user while keeping bearer tokens in memory only. */
export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void restoreSession().then((outcome) => {
      if (active && outcome.current) {
        setUser(outcome.session?.user ?? null);
        setLoading(false);
      }
    });
    return () => {
      active = false;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      /** Authenticates existing credentials and installs the returned in-memory session. */
      async login(email, password) {
        const requestEpoch = resetApiClient();
        const session = await apiRequest<TokenResponse>('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password }),
        });
        if (setAccessTokenForEpoch(session.accessToken, requestEpoch)) {
          setUser(session.user);
          setLoading(false);
        }
      },
      /** Registers a user and installs the initial authenticated session. */
      async register(email, displayName, password) {
        const requestEpoch = resetApiClient();
        const session = await apiRequest<TokenResponse>('/api/v1/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, displayName, password }),
        });
        if (setAccessTokenForEpoch(session.accessToken, requestEpoch)) {
          setUser(session.user);
          setLoading(false);
        }
      },
      /** Revokes refresh state and clears all client-side authentication state. */
      async logout() {
        try {
          await apiRequest<void>('/api/v1/auth/logout', { method: 'POST' });
        } finally {
          resetApiClient();
          setUser(null);
        }
      },
    }),
    [loading, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
