import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { useMemo, useState, type PropsWithChildren } from 'react';
import { ColorModeProvider } from '../theme/ColorModeContext';
import { createTestForgeTheme } from '../theme/theme';
import { useColorMode } from '../theme/useColorMode';
import { AuthProvider } from '../auth/AuthContext';

/** Creates isolated query, theme, and authentication state for the application tree. */
export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
            staleTime: 30_000,
          },
          mutations: {
            retry: false,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ColorModeProvider>
        <ThemedAuth>{children}</ThemedAuth>
      </ColorModeProvider>
    </QueryClientProvider>
  );
}

/** Applies the resolved color mode around authenticated application content. */

/** Applies the resolved color mode around authenticated application content. */
function ThemedAuth({ children }: PropsWithChildren) {
  const { resolvedMode } = useColorMode();
  const theme = useMemo(() => createTestForgeTheme(resolvedMode), [resolvedMode]);
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>{children}</AuthProvider>
    </ThemeProvider>
  );
}
