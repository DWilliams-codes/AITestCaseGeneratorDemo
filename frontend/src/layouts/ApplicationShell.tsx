import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import {
  Alert,
  AppBar,
  Avatar,
  Box,
  Button,
  Container,
  IconButton,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';
import { Link, Outlet, useNavigate } from 'react-router';
import { useAuth } from '../auth/useAuth';
import { useDemoInfo } from '../demo/useDemoInfo';
import { ThemeModeControl } from '../components/ThemeModeControl';

/** Renders authenticated navigation, identity controls, routed content, and review guidance. */
export function ApplicationShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const demo = useDemoInfo();
  const initials =
    user?.displayName
      .split(/\s+/)
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase() ?? 'QA';
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Box
        component="a"
        href="#main-content"
        sx={{
          position: 'fixed',
          left: 16,
          top: -80,
          zIndex: 2000,
          bgcolor: 'background.paper',
          color: 'primary.main',
          px: 2,
          py: 1,
          borderRadius: 1,
          boxShadow: 3,
          '&:focus': { top: 16 },
        }}
      >
        Skip to main content
      </Box>
      <AppBar
        position="sticky"
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: 1,
          borderColor: 'divider',
          backgroundColor: 'background.paper',
          backdropFilter: 'blur(16px)',
        }}
      >
        <Toolbar sx={{ minHeight: 72, px: { xs: 2, md: 4 } }}>
          <Stack
            component={Link}
            to="/"
            aria-label="TestForge AI home"
            direction="row"
            spacing={1.25}
            sx={{ flexGrow: 1, alignItems: 'center', color: 'inherit', textDecoration: 'none' }}
          >
            <Box
              aria-hidden="true"
              sx={{
                width: 34,
                height: 34,
                display: 'grid',
                placeItems: 'center',
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
                borderRadius: 1.25,
                fontWeight: 850,
                letterSpacing: '-0.08em',
              }}
            >
              TF
            </Box>
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 760, lineHeight: 1.1 }}>
                TestForge AI
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Quality engineering workspace
              </Typography>
            </Box>
          </Stack>
          <Button
            component={Link}
            to="/"
            color="inherit"
            startIcon={<DashboardOutlinedIcon />}
            sx={{ display: { xs: 'none', sm: 'inline-flex' }, mr: 1 }}
          >
            Projects
          </Button>
          <Button component={Link} to="/simulation" color="inherit">
            Simulation
          </Button>
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', pl: 2, borderLeft: 1, borderColor: 'divider' }}
          >
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <ThemeModeControl />
            </Box>
            <Box sx={{ display: { xs: 'block', sm: 'none' } }}>
              <ThemeModeControl compact />
            </Box>
            <Avatar
              sx={{
                width: 36,
                height: 36,
                bgcolor: 'primary.light',
                color: 'primary.dark',
                fontSize: '.85rem',
                fontWeight: 750,
              }}
            >
              {initials}
            </Avatar>
            <Box sx={{ display: { xs: 'none', md: 'block' }, mr: 1 }}>
              <Typography variant="body2" sx={{ fontWeight: 650 }}>
                {user?.displayName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.role}
              </Typography>
            </Box>
            <Button
              aria-label="Sign out"
              color="inherit"
              onClick={() => void logout().then(() => navigate('/login'))}
              startIcon={<LogoutRoundedIcon />}
              sx={{ display: { xs: 'none', sm: 'inline-flex' } }}
            >
              Sign out
            </Button>
            <IconButton
              aria-label="Sign out"
              onClick={() => void logout().then(() => navigate('/login'))}
              sx={{ display: { xs: 'inline-flex', sm: 'none' } }}
            >
              <LogoutRoundedIcon />
            </IconButton>
          </Stack>
        </Toolbar>
      </AppBar>
      <Box component="main" id="main-content" tabIndex={-1}>
        <Container maxWidth="xl" sx={{ px: { xs: 2, sm: 3, md: 5 }, py: { xs: 3, md: 5 } }}>
          {demo.data?.enabled && (
            <Alert severity="info" sx={{ mb: 3 }}>
              {demo.data.label} Public disposable workspace; synthetic content only. Human review is
              required.
            </Alert>
          )}
          <Outlet />
        </Container>
      </Box>
      <Stack
        component="footer"
        direction="row"
        spacing={1}
        sx={{ justifyContent: 'center', alignItems: 'center', py: 3, color: 'text.secondary' }}
      >
        <ShieldOutlinedIcon fontSize="small" />
        <Typography variant="caption">
          Human approval required before export or future automation.
        </Typography>
      </Stack>
    </Box>
  );
}
