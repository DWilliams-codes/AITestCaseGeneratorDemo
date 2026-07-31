import AddRoundedIcon from '@mui/icons-material/AddRounded';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { ApiError, apiRequest } from '../api/client';
import type { PageResponse, Project } from '../types/api';

const schema = z.object({
  name: z.string().trim().min(1, 'Enter a project name.').max(120),
  description: z.string().trim().max(2000),
});
type Values = z.infer<typeof schema>;

export function ProjectDashboardPage() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const projects = useQuery({
    queryKey: ['projects'],
    queryFn: () => apiRequest<PageResponse<Project>>('/api/v1/projects'),
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '' },
  });
  const create = useMutation({
    mutationFn: (values: Values) =>
      apiRequest<Project>('/api/v1/projects', { method: 'POST', body: JSON.stringify(values) }),
    onSuccess: (project) => {
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      reset();
      setOpen(false);
      navigate(`/projects/${project.id}`);
    },
  });

  return (
    <Stack spacing={4}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' } }}
      >
        <Box>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <Typography component="h1" variant="h1">
              Projects
            </Typography>
            <Chip label="Stage 1 MVP" size="small" color="success" variant="outlined" />
          </Stack>
          <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 680 }}>
            Organize source requirements, generate structured coverage, and keep every human
            decision traceable.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setOpen(true)}>
          New project
        </Button>
      </Stack>

      {projects.isLoading && (
        <Box sx={{ py: 10, display: 'grid', placeItems: 'center' }}>
          <CircularProgress aria-label="Loading projects" />
        </Box>
      )}
      {projects.error && (
        <Alert severity="error">
          {projects.error instanceof ApiError
            ? projects.error.message
            : 'Projects could not be loaded.'}
        </Alert>
      )}
      {projects.data?.items.length === 0 && (
        <Card>
          <CardContent
            sx={{ minHeight: 300, display: 'grid', placeItems: 'center', textAlign: 'center' }}
          >
            <Stack spacing={2} sx={{ alignItems: 'center', maxWidth: 440 }}>
              <Box
                sx={{
                  display: 'grid',
                  placeItems: 'center',
                  width: 60,
                  height: 60,
                  borderRadius: 2,
                  bgcolor: 'primary.light',
                  color: 'primary.main',
                }}
              >
                <FolderOutlinedIcon />
              </Box>
              <Typography component="h2" variant="h2">
                Create your first project
              </Typography>
              <Typography color="text.secondary">
                Group related requirements and give reviewers a clear boundary for ownership, audit
                history, and exports.
              </Typography>
              <Button variant="contained" onClick={() => setOpen(true)}>
                Create project
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            md: 'repeat(2, minmax(0, 1fr))',
            xl: 'repeat(3, minmax(0, 1fr))',
          },
          gap: 2.5,
        }}
      >
        {projects.data?.items.map((project) => (
          <Card key={project.id} sx={{ minHeight: 230 }}>
            <CardActionArea
              onClick={() => navigate(`/projects/${project.id}`)}
              sx={{ height: '100%', p: 0.5 }}
            >
              <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3 }}>
                <Stack
                  direction="row"
                  sx={{ justifyContent: 'space-between', alignItems: 'flex-start' }}
                >
                  <Box
                    sx={{
                      width: 44,
                      height: 44,
                      display: 'grid',
                      placeItems: 'center',
                      borderRadius: 1.5,
                      bgcolor: 'primary.light',
                      color: 'primary.main',
                    }}
                  >
                    <FolderOutlinedIcon />
                  </Box>
                  <Chip
                    label={project.status}
                    size="small"
                    color={project.status === 'ACTIVE' ? 'success' : 'default'}
                  />
                </Stack>
                <Typography component="h2" variant="h2" sx={{ mt: 3 }}>
                  {project.name}
                </Typography>
                <Typography
                  color="text.secondary"
                  variant="body2"
                  sx={{
                    mt: 1,
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden',
                  }}
                >
                  {project.description || 'No description provided.'}
                </Typography>
                <Stack
                  direction="row"
                  sx={{ justifyContent: 'space-between', alignItems: 'center', mt: 'auto', pt: 3 }}
                >
                  <Typography variant="caption" color="text.secondary">
                    {project.requirementCount} requirement
                    {project.requirementCount === 1 ? '' : 's'}
                  </Typography>
                  <ArrowForwardRoundedIcon color="primary" fontSize="small" />
                </Stack>
              </CardContent>
            </CardActionArea>
          </Card>
        ))}
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <Stack component="form" onSubmit={handleSubmit((values) => create.mutate(values))}>
          <DialogTitle>Create a project</DialogTitle>
          <DialogContent>
            <Stack spacing={2.5} sx={{ pt: 1 }}>
              {create.error && (
                <Alert severity="error">
                  {create.error instanceof ApiError
                    ? create.error.message
                    : 'Project could not be created.'}
                </Alert>
              )}
              <TextField
                autoFocus
                label="Project name"
                {...register('name')}
                error={Boolean(errors.name)}
                helperText={errors.name?.message}
              />
              <TextField
                label="Description"
                multiline
                minRows={4}
                {...register('description')}
                error={Boolean(errors.description)}
                helperText={
                  errors.description?.message ?? 'Describe the product area or quality objective.'
                }
              />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={create.isPending}>
              {create.isPending ? 'Creating…' : 'Create project'}
            </Button>
          </DialogActions>
        </Stack>
      </Dialog>
    </Stack>
  );
}
