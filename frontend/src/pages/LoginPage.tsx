import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/useAuth';
import { useDemoInfo } from '../demo/useDemoInfo';
import { safeReturnLocation } from '../auth/returnLocation';
import { ThemeModeControl } from '../components/ThemeModeControl';

const schema = z.object({
  email: z.email('Enter a valid email address.'),
  displayName: z.string().max(120).optional(),
  password: z.string().min(12, 'Use at least 12 characters.').max(128),
});
type FormValues = z.infer<typeof schema>;

/** Presents sign-in and registration flows with validation and recoverable API errors. */
export function LoginPage() {
  const { user, login, register: registerAccount } = useAuth();
  const demo = useDemoInfo();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [serverError, setServerError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: '',
      displayName: '',
      password: '',
    },
  });

  if (user) return <Navigate to="/" replace />;

  const submit = handleSubmit(async (values) => {
    setServerError('');
    try {
      if (mode === 'login') await login(values.email, values.password);
      else await registerAccount(values.email, values.displayName ?? '', values.password);
      const destination = safeReturnLocation((location.state as { from?: unknown } | null)?.from);
      navigate(destination, { replace: true });
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : 'Sign in could not be completed.');
    }
  });

  return (
    <Box
      component="main"
      sx={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: { md: '1fr 1fr' } }}
    >
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'space-between',
          p: 7,
          color: '#FFFFFF',
          bgcolor: 'primary.dark',
        }}
      >
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              display: 'grid',
              placeItems: 'center',
              bgcolor: 'secondary.light',
              color: 'secondary.contrastText',
              borderRadius: 1.5,
              fontWeight: 900,
            }}
          >
            TF
          </Box>
          <Typography component="p" variant="h6" sx={{ fontWeight: 750 }}>
            TestForge AI
          </Typography>
        </Stack>
        <Box sx={{ maxWidth: 560 }}>
          <AutoAwesomeRoundedIcon sx={{ fontSize: 40, color: 'secondary.light', mb: 2 }} />
          <Typography
            component="h1"
            sx={{
              fontSize: '3.25rem',
              lineHeight: 1.05,
              fontWeight: 760,
              letterSpacing: '-.045em',
            }}
          >
            From user stories to review-ready test coverage.
          </Typography>
          <Typography sx={{ mt: 3, fontSize: '1.125rem', color: '#FFFFFF', maxWidth: 500 }}>
            Structure the source, expose ambiguity, generate balanced manual tests, and keep human
            approval in control.
          </Typography>
        </Box>
        <Typography variant="caption" sx={{ color: '#FFFFFF' }}>
          Provider-neutral | Traceable | Secure by design
        </Typography>
      </Box>

      <Box
        sx={{ display: 'grid', placeItems: 'center', bgcolor: 'background.default', px: 2, py: 6 }}
      >
        <Container maxWidth="xs">
          <Stack spacing={2.5}>
            <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
              <ThemeModeControl />
            </Stack>
            <Box sx={{ display: { md: 'none' }, textAlign: 'center' }}>
              <LockOutlinedIcon color="primary" />
              <Typography component="p" variant="h5" sx={{ fontWeight: 750 }}>
                TestForge AI
              </Typography>
            </Box>
            <Box>
              <Typography component="h1" variant="h1">
                {mode === 'login' ? 'Welcome back' : 'Create your account'}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 1 }}>
                {mode === 'login'
                  ? 'Sign in to your quality engineering workspace.'
                  : 'Start a secure quality engineering workspace.'}
              </Typography>
            </Box>
            {demo.data?.enabled && (
              <Alert severity="info">
                <Stack spacing={1}>
                  <Typography component="p" variant="subtitle2">
                    Local fixture demo
                  </Typography>
                  <Typography variant="body2">
                    Public disposable account. Generation uses maintained synthetic responses; no
                    live AI call occurs. Use synthetic content only.
                  </Typography>
                  <Typography variant="body2">Email: {demo.data.email}</Typography>
                  <Typography variant="body2">Password: {demo.data.password}</Typography>
                  <Button
                    variant="outlined"
                    onClick={() => {
                      if (!demo.data?.enabled) return;
                      setMode('login');
                      setServerError('');
                      setValue('email', demo.data.email);
                      setValue('password', demo.data.password);
                    }}
                  >
                    Fill demo credentials
                  </Button>
                </Stack>
              </Alert>
            )}
            <Card>
              <Tabs
                value={mode}
                onChange={(_, value: 'login' | 'register') => {
                  setMode(value);
                  setServerError('');
                }}
                variant="fullWidth"
                aria-label="Account action"
              >
                <Tab value="login" label="Sign in" />
                <Tab value="register" label="Create account" />
              </Tabs>
              <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
                <Stack component="form" onSubmit={submit} spacing={2.25} noValidate>
                  {serverError && <Alert severity="error">{serverError}</Alert>}
                  {mode === 'register' && (
                    <TextField
                      label="Display name"
                      autoComplete="name"
                      {...register('displayName')}
                      error={Boolean(errors.displayName)}
                      helperText={errors.displayName?.message}
                    />
                  )}
                  <TextField
                    label="Email address"
                    type="email"
                    autoComplete="email"
                    {...register('email')}
                    error={Boolean(errors.email)}
                    helperText={errors.email?.message}
                  />
                  <TextField
                    label="Password"
                    type="password"
                    autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                    {...register('password')}
                    error={Boolean(errors.password)}
                    helperText={errors.password?.message}
                  />
                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                    {isSubmitting ? 'Working...' : mode === 'login' ? 'Sign in' : 'Create account'}
                  </Button>
                </Stack>
              </CardContent>
            </Card>
            <Button component={Link} to="/simulation" variant="outlined">
              Explore the Salesforce simulation
            </Button>
            <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center' }}>
              Refresh tokens stay in protected cookies. Access tokens are kept only in memory.
            </Typography>
          </Stack>
        </Container>
      </Box>
    </Box>
  );
}
