import { Box, CircularProgress } from '@mui/material';
import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './useAuth';
import { safeReturnLocation } from './returnLocation';

/** Gates protected routes until session restoration completes or redirects unauthenticated users. */
export function ProtectedRoute() {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <CircularProgress aria-label="Restoring session" />
      </Box>
    );
  }
  if (!user) {
    const from = safeReturnLocation(`${location.pathname}${location.search}${location.hash}`);
    return <Navigate to="/login" replace state={{ from }} />;
  }
  return <Outlet />;
}
