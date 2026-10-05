import { alpha, createTheme } from '@mui/material/styles';

/** Builds the shared accessible palette and component defaults for the chosen mode. */

/** Builds the shared accessible palette and component defaults for the chosen mode. */
export function createTestForgeTheme(mode: 'light' | 'dark') {
  const dark = mode === 'dark';
  const primary = dark ? '#8FAFFF' : '#2457D6';
  return createTheme({
    palette: {
      mode,
      primary: {
        main: primary,
        dark: dark ? '#314A93' : '#183E9D',
        light: dark ? '#DDE7FF' : '#E9EFFE',
        ...(dark ? { contrastText: '#101820' } : {}),
      },
      background: {
        default: dark ? '#101820' : '#F6F7F9',
        paper: dark ? '#17232D' : '#FFFFFF',
      },
      text: {
        primary: dark ? '#F4F7FA' : '#1E232B',
        secondary: dark ? '#B6C2CC' : '#5F6774',
      },
      divider: dark ? '#304250' : '#E2E5EA',
      success: {
        main: '#227A4B',
      },
      warning: {
        main: '#A34600',
        contrastText: '#FFFFFF',
      },
    },
    shape: {
      borderRadius: 8,
    },
    typography: {
      fontFamily: 'Inter, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
      h1: {
        fontSize: '2rem',
        fontWeight: 650,
        letterSpacing: '-0.025em',
      },
      h2: {
        fontSize: '1.25rem',
        fontWeight: 650,
      },
      button: {
        fontWeight: 600,
        textTransform: 'none',
      },
    },
    components: {
      MuiButton: {
        defaultProps: {
          disableElevation: true,
        },
        styleOverrides: {
          root: {
            minHeight: 40,
            borderRadius: 6,
          },
        },
      },
      MuiCard: {
        defaultProps: {
          variant: 'outlined',
        },
        styleOverrides: {
          root: {
            borderColor: dark ? '#304250' : '#E2E5EA',
            boxShadow: '0 1px 2px rgba(21, 28, 38, 0.04)',
          },
        },
      },
      MuiCssBaseline: {
        styleOverrides: {
          '*:focus-visible': {
            outline: `3px solid ${alpha(primary, 0.35)}`,
            outlineOffset: 2,
          },
          '@media (prefers-reduced-motion: reduce)': {
            '*, *::before, *::after': {
              animationDuration: '0.01ms !important',
              animationIterationCount: '1 !important',
              scrollBehavior: 'auto !important',
              transitionDuration: '0.01ms !important',
            },
          },
        },
      },
    },
  });
}

export const testForgeTheme = createTestForgeTheme('light');
