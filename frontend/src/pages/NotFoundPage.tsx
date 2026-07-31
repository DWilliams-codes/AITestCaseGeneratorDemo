import { Box, Button, Container, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <Box component="main" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3 }}>
      <Container maxWidth="sm">
        <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
          <Typography color="primary" sx={{ fontWeight: 700 }}>
            404
          </Typography>
          <Typography component="h1" variant="h1">
            Page not found
          </Typography>
          <Typography color="text.secondary">
            The page may have moved, or the address may be incomplete.
          </Typography>
          <Button component={Link} to="/" variant="contained">
            Return to projects
          </Button>
        </Stack>
      </Container>
    </Box>
  );
}
