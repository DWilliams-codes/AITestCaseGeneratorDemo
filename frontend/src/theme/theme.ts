import { alpha, createTheme } from '@mui/material/styles';

const primary = '#2457D6';

export const testForgeTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: primary,
      dark: '#183E9D',
      light: '#E9EFFE',
    },
    background: {
      default: '#F6F7F9',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#1E232B',
      secondary: '#5F6774',
    },
    divider: '#E2E5EA',
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
          borderColor: '#E2E5EA',
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
