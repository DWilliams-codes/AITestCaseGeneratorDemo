import { useContext } from 'react';
import { AuthContext } from './auth-context';

/** Returns the active authentication context and rejects use outside its provider. */
export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used within AuthProvider.');
  return value;
}
