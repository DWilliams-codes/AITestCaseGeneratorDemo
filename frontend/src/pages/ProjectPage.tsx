import AddRoundedIcon from '@mui/icons-material/AddRounded';
import ArrowDownwardRoundedIcon from '@mui/icons-material/ArrowDownwardRounded';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import ArrowUpwardRoundedIcon from '@mui/icons-material/ArrowUpwardRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Breadcrumbs,
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
  IconButton,
  Link as MuiLink,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { ApiError, apiRequest } from '../api/client';
import type { PageResponse, Project, Requirement, RequirementSummary } from '../types/api';

const schema = z.object({
  title: z.string().trim().min(1, 'Enter a title.').max(200),
  userStory: z.string().trim().min(1, 'Enter the user story or requirement.').max(10000),
  businessRequirements: z.string().max(20000),
  assumptions: z.string().max(10000),
  sourceReference: z.string().max(1000),
  criteria: z
    .array(
      z.object({ value: z.string().trim().min(1, 'Describe the expected behavior.').max(4000) }),
    )
    .min(1)
    .max(50),
});
type Values = z.infer<typeof schema>;

/** Displays one owned project and manages creation and navigation for its user stories. */
export function ProjectPage() {
  const { projectId = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const project = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => apiRequest<Project>(`/api/v1/projects/${projectId}`),
    enabled: Boolean(projectId),
  });
  const requirements = useQuery({
    queryKey: ['requirements', projectId],
    queryFn: () =>
      apiRequest<PageResponse<RequirementSummary>>(`/api/v1/projects/${projectId}/requirements`),
    enabled: Boolean(projectId),
  });
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: '',
      userStory: '',
      businessRequirements: '',
      assumptions: '',
      sourceReference: '',
      criteria: [{ value: '' }],
    },
  });
  const fields = useFieldArray({ control: form.control, name: 'criteria' });
  const create = useMutation({
    mutationFn: (values: Values) =>
      apiRequest<Requirement>(`/api/v1/projects/${projectId}/requirements`, {
        method: 'POST',
        body: JSON.stringify({
          ...values,
          acceptanceCriteria: values.criteria.map((item) => item.value),
          criteria: undefined,
        }),
      }),
    onSuccess: (requirement) => {
      void queryClient.invalidateQueries({ queryKey: ['requirements', projectId] });
      setOpen(false);
      form.reset();
      navigate(`/requirements/${requirement.id}`);
    },
  });

  if (project.isLoading)
    return (
      <Box sx={{ py: 10, display: 'grid', placeItems: 'center' }}>
        <CircularProgress />
      </Box>
    );
  if (project.error || !project.data)
    return (
      <Alert severity="error">
        {project.error instanceof ApiError ? project.error.message : 'Project not found.'}
      </Alert>
    );

  return (
    <Stack spacing={4}>
      <Breadcrumbs>
        <MuiLink component={Link} to="/" underline="hover">
          Projects
        </MuiLink>
        <Typography color="text.primary">{project.data.name}</Typography>
      </Breadcrumbs>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' } }}
      >
        <Box>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <Typography component="h1" variant="h1">
              {project.data.name}
            </Typography>
            <Chip label={project.data.status} color="success" size="small" />
          </Stack>
          <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 760 }}>
            {project.data.description}
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setOpen(true)}>
          New requirement
        </Button>
      </Stack>
      <Box>
        <Typography component="h2" variant="h2">
          Requirements
        </Typography>
        <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
          Source stories and the test-design work attached to them.
        </Typography>
      </Box>
      {requirements.error && <Alert severity="error">Requirements could not be loaded.</Alert>}
      <Stack spacing={1.5}>
        {requirements.data?.items.map((requirement) => (
          <Card key={requirement.id}>
            <CardActionArea onClick={() => navigate(`/requirements/${requirement.id}`)}>
              <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2.5, p: 2.5 }}>
                <Box sx={{ flexGrow: 1 }}>
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Chip
                      size="small"
                      label={`User Story ${requirement.workItemNumber}`}
                      color="primary"
                      variant="outlined"
                    />
                    <Typography component="h3" sx={{ fontWeight: 700 }}>
                      {requirement.title}
                    </Typography>
                    <Chip
                      size="small"
                      label={requirement.status.replaceAll('_', ' ')}
                      color={
                        requirement.status === 'NEEDS_CLARIFICATION'
                          ? 'warning'
                          : requirement.status === 'GENERATED'
                            ? 'success'
                            : 'default'
                      }
                    />
                  </Stack>
                  <Typography color="text.secondary" variant="body2" sx={{ mt: 0.75 }}>
                    {requirement.acceptanceCriteriaCount} acceptance criteria • Updated{' '}
                    {new Date(requirement.updatedAt).toLocaleDateString()}
                  </Typography>
                </Box>
                <ArrowForwardRoundedIcon color="action" />
              </CardContent>
            </CardActionArea>
          </Card>
        ))}
      </Stack>
      {requirements.data?.items.length === 0 && (
        <Card>
          <CardContent sx={{ py: 8, textAlign: 'center' }}>
            <Typography variant="h2">No requirements yet</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Add a user story and at least one measurable acceptance criterion.
            </Typography>
            <Button sx={{ mt: 2 }} variant="contained" onClick={() => setOpen(true)}>
              Add requirement
            </Button>
          </CardContent>
        </Card>
      )}

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <Stack component="form" onSubmit={form.handleSubmit((values) => create.mutate(values))}>
          <DialogTitle>Add a requirement</DialogTitle>
          <DialogContent>
            <Stack spacing={2.25} sx={{ pt: 1 }}>
              {create.error && (
                <Alert severity="error">
                  {create.error instanceof ApiError
                    ? create.error.message
                    : 'Requirement could not be created.'}
                </Alert>
              )}
              <Alert severity="info">
                Use synthetic or non-production content. Do not submit secrets or personal data.
              </Alert>
              <TextField
                label="Requirement title"
                autoFocus
                slotProps={{ htmlInput: { maxLength: 200 } }}
                {...form.register('title')}
                error={Boolean(form.formState.errors.title)}
                helperText={form.formState.errors.title?.message}
              />
              <TextField
                label="User story or requirement"
                multiline
                minRows={4}
                slotProps={{ htmlInput: { maxLength: 10000 } }}
                {...form.register('userStory')}
                error={Boolean(form.formState.errors.userStory)}
                helperText={form.formState.errors.userStory?.message}
              />
              <TextField
                label="Business rules and constraints"
                multiline
                minRows={3}
                slotProps={{ htmlInput: { maxLength: 20000 } }}
                {...form.register('businessRequirements')}
              />
              <TextField
                label="Assumptions"
                multiline
                minRows={2}
                slotProps={{ htmlInput: { maxLength: 10000 } }}
                {...form.register('assumptions')}
              />
              <TextField
                label="Source reference"
                slotProps={{ htmlInput: { maxLength: 1000 } }}
                {...form.register('sourceReference')}
              />
              <Box>
                <Typography sx={{ fontWeight: 700, mb: 1 }}>Acceptance criteria</Typography>
                <Stack spacing={1.25}>
                  {fields.fields.map((field, index) => (
                    <Stack key={field.id} direction="row" spacing={1}>
                      <TextField
                        fullWidth
                        label={`AC-${index + 1}`}
                        slotProps={{ htmlInput: { maxLength: 4000 } }}
                        {...form.register(`criteria.${index}.value`)}
                        error={Boolean(form.formState.errors.criteria?.[index]?.value)}
                        helperText={form.formState.errors.criteria?.[index]?.value?.message}
                      />
                      <IconButton
                        aria-label={`Move acceptance criterion ${index + 1} up`}
                        disabled={index === 0}
                        onClick={() => fields.move(index, index - 1)}
                      >
                        <ArrowUpwardRoundedIcon />
                      </IconButton>
                      <IconButton
                        aria-label={`Move acceptance criterion ${index + 1} down`}
                        disabled={index === fields.fields.length - 1}
                        onClick={() => fields.move(index, index + 1)}
                      >
                        <ArrowDownwardRoundedIcon />
                      </IconButton>
                      <IconButton
                        aria-label={`Remove acceptance criterion ${index + 1}`}
                        disabled={fields.fields.length === 1}
                        onClick={() => fields.remove(index)}
                      >
                        <DeleteOutlineRoundedIcon />
                      </IconButton>
                    </Stack>
                  ))}
                </Stack>
                <Button
                  sx={{ mt: 1 }}
                  startIcon={<AddRoundedIcon />}
                  onClick={() => fields.append({ value: '' })}
                >
                  Add criterion
                </Button>
              </Box>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={create.isPending}>
              {create.isPending ? 'Saving…' : 'Save requirement'}
            </Button>
          </DialogActions>
        </Stack>
      </Dialog>
    </Stack>
  );
}
