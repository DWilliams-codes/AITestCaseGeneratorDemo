import { useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { apiRequest, refreshSession, setAccessToken } from '../api/client';
import type { TokenResponse, User } from '../types/api';
import { AuthContext, type AuthContextValue } from './auth-context';

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void refreshSession().then((session) => {
      if (active) {
        setUser(session?.user ?? null);
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
      async login(email, password) {
        const session = await apiRequest<TokenResponse>('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password }),
        });
        setAccessToken(session.accessToken);
        setUser(session.user);
      },
      async register(email, displayName, password) {
        const session = await apiRequest<TokenResponse>('/api/v1/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, displayName, password }),
        });
        setAccessToken(session.accessToken);
        setUser(session.user);
      },
      async logout() {
        try {
          await apiRequest<void>('/api/v1/auth/logout', { method: 'POST' });
        } finally {
          setAccessToken(null);
          setUser(null);
        }
      },
    }),
    [loading, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
