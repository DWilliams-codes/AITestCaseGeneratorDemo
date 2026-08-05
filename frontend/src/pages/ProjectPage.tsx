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
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { ApiError, apiRequest } from '../api/client';
import type {
  AuditEvent,
  PageResponse,
  Project,
  Requirement,
  RequirementSummary,
} from '../types/api';

const schema = z.object({
  title: z.string().trim().min(1, 'Enter a title.').max(200),
  userStory: z.string().trim().min(1, 'Enter the user story.').max(10000),
  businessRequirements: z.string().max(20000),
  assumptions: z.string().max(10000),
  sourceReference: z.string().max(1000),
  priority: z.enum(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']),
  criteria: z
    .array(
      z.object({ value: z.string().trim().min(1, 'Describe the expected behavior.').max(4000) }),
    )
    .min(1)
    .max(50),
});
type Values = z.infer<typeof schema>;

type AuditFilters = {
  entityType: string;
  entityId: string;
  actorId: string;
  action: string;
  from: string;
  to: string;
};

const emptyAuditFilters: AuditFilters = {
  entityType: '',
  entityId: '',
  actorId: '',
  action: '',
  from: '',
  to: '',
};

/** Converts a local date-time control value into the instant expected by the audit API. */
function auditInstant(value: string) {
  return value ? new Date(value).toISOString() : '';
}

/** Presents legacy persistence entity names using the canonical product vocabulary. */
function auditEntityLabel(entityType: string) {
  const labels: Record<string, string> = {
    PROJECT: 'Project',
    REQUIREMENT: 'User story',
    ACCEPTANCE_CRITERION: 'Acceptance criterion',
    REQUIREMENT_AMBIGUITY: 'User story ambiguity',
    GENERATION_RUN: 'Generation run',
    TEST_CASE: 'Test case',
  };
  return labels[entityType] ?? entityType.replaceAll('_', ' ').toLowerCase();
}

/** Formats a stored audit action for compact human-readable display. */
function auditActionLabel(action: string) {
  const normalized = action.replaceAll('_', ' ').toLowerCase();
  return normalized ? normalized[0]!.toUpperCase() + normalized.slice(1) : action;
}

/** Displays one owned project and manages creation and navigation for its user stories. */
export function ProjectPage() {
  const { projectId = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [auditPage, setAuditPage] = useState(0);
  const [auditDraft, setAuditDraft] = useState<AuditFilters>(emptyAuditFilters);
  const [auditFilters, setAuditFilters] = useState<AuditFilters>(emptyAuditFilters);
  const project = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => apiRequest<Project>(`/api/v1/projects/${projectId}`),
    enabled: Boolean(projectId),
  });
  const userStories = useQuery({
    queryKey: ['user-stories', projectId],
    queryFn: () =>
      apiRequest<PageResponse<RequirementSummary>>(`/api/v1/projects/${projectId}/user-stories`),
    enabled: Boolean(projectId),
  });
  const auditEvents = useQuery({
    queryKey: ['audit-events', projectId, auditPage, auditFilters],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(auditPage), size: '20' });
      if (auditFilters.entityType) params.set('entityType', auditFilters.entityType);
      if (auditFilters.entityId.trim()) params.set('entityId', auditFilters.entityId.trim());
      if (auditFilters.actorId.trim()) params.set('actorId', auditFilters.actorId.trim());
      if (auditFilters.action.trim()) params.set('action', auditFilters.action.trim());
      if (auditFilters.from) params.set('from', auditInstant(auditFilters.from));
      if (auditFilters.to) params.set('to', auditInstant(auditFilters.to));
      return apiRequest<PageResponse<AuditEvent>>(
        `/api/v1/projects/${projectId}/audit-events?${params.toString()}`,
      );
    },
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
      priority: 'MEDIUM',
      criteria: [{ value: '' }],
    },
  });
  const fields = useFieldArray({ control: form.control, name: 'criteria' });
  const create = useMutation({
    mutationFn: (values: Values) =>
      apiRequest<Requirement>(`/api/v1/projects/${projectId}/user-stories`, {
        method: 'POST',
        body: JSON.stringify({
          ...values,
          acceptanceCriteria: values.criteria.map((item) => item.value),
          criteria: undefined,
        }),
      }),
    onSuccess: (requirement) => {
      void queryClient.invalidateQueries({ queryKey: ['user-stories', projectId] });
      setOpen(false);
      form.reset();
      navigate(`/user-stories/${requirement.id}`);
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
          New user story
        </Button>
      </Stack>
      <Box>
        <Typography component="h2" variant="h2">
          User stories
        </Typography>
        <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
          Source stories and the test-design work attached to them.
        </Typography>
      </Box>
      {userStories.error && <Alert severity="error">User stories could not be loaded.</Alert>}
      <Stack spacing={1.5}>
        {userStories.data?.items.map((requirement) => (
          <Card key={requirement.id}>
            <CardActionArea onClick={() => navigate(`/user-stories/${requirement.id}`)}>
              <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2.5, p: 2.5 }}>
                <Box sx={{ flexGrow: 1 }}>
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Chip
                      size="small"
                      label={`User Story ${requirement.workItemNumber}`}
                      color="primary"
                      variant="outlined"
                    />
                    <Chip size="small" label={requirement.priority} variant="outlined" />
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
      {userStories.data?.items.length === 0 && (
        <Card>
          <CardContent sx={{ py: 8, textAlign: 'center' }}>
            <Typography variant="h2">No user stories yet</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Add a user story and at least one measurable acceptance criterion.
            </Typography>
            <Button sx={{ mt: 2 }} variant="contained" onClick={() => setOpen(true)}>
              Add user story
            </Button>
          </CardContent>
        </Card>
      )}

      <Box>
        <Typography component="h2" variant="h2">
          Activity history
        </Typography>
        <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
          Filter the owner-scoped audit timeline. Results are newest first.
        </Typography>
      </Box>
      <Card>
        <CardContent>
          <Stack spacing={2.5}>
            <Box
              component="form"
              aria-label="Audit filters"
              onSubmit={(event) => {
                event.preventDefault();
                setAuditPage(0);
                setAuditFilters(auditDraft);
              }}
              sx={{
                display: 'grid',
                gridTemplateColumns: {
                  xs: '1fr',
                  md: 'repeat(3, minmax(0, 1fr))',
                },
                gap: 1.5,
              }}
            >
              <TextField
                select
                size="small"
                label="Event type"
                value={auditDraft.entityType}
                onChange={(event) =>
                  setAuditDraft((current) => ({ ...current, entityType: event.target.value }))
                }
              >
                <MenuItem value="">All event types</MenuItem>
                <MenuItem value="PROJECT">Project</MenuItem>
                <MenuItem value="REQUIREMENT">User story</MenuItem>
                <MenuItem value="ACCEPTANCE_CRITERION">Acceptance criterion</MenuItem>
                <MenuItem value="REQUIREMENT_AMBIGUITY">User story ambiguity</MenuItem>
                <MenuItem value="GENERATION_RUN">Generation run</MenuItem>
                <MenuItem value="TEST_CASE">Test case</MenuItem>
              </TextField>
              <TextField
                size="small"
                label="Entity ID"
                value={auditDraft.entityId}
                onChange={(event) =>
                  setAuditDraft((current) => ({ ...current, entityId: event.target.value }))
                }
                slotProps={{ htmlInput: { pattern: '[0-9a-fA-F-]{36}' } }}
              />
              <TextField
                size="small"
                label="Actor ID"
                value={auditDraft.actorId}
                onChange={(event) =>
                  setAuditDraft((current) => ({ ...current, actorId: event.target.value }))
                }
                slotProps={{ htmlInput: { pattern: '[0-9a-fA-F-]{36}' } }}
              />
              <TextField
                size="small"
                label="Action"
                value={auditDraft.action}
                onChange={(event) =>
                  setAuditDraft((current) => ({ ...current, action: event.target.value }))
                }
                slotProps={{ htmlInput: { maxLength: 100 } }}
              />
              <TextField
                size="small"
                type="datetime-local"
                label="From"
                value={auditDraft.from}
                onChange={(event) =>
                  setAuditDraft((current) => ({ ...current, from: event.target.value }))
                }
                slotProps={{ inputLabel: { shrink: true } }}
              />
              <TextField
                size="small"
                type="datetime-local"
                label="To"
                value={auditDraft.to}
                onChange={(event) =>
                  setAuditDraft((current) => ({ ...current, to: event.target.value }))
                }
                slotProps={{ inputLabel: { shrink: true } }}
              />
              <Stack direction="row" spacing={1} sx={{ gridColumn: { md: '1 / -1' } }}>
                <Button type="submit" variant="outlined">
                  Apply filters
                </Button>
                <Button
                  onClick={() => {
                    setAuditDraft(emptyAuditFilters);
                    setAuditFilters(emptyAuditFilters);
                    setAuditPage(0);
                  }}
                >
                  Clear filters
                </Button>
              </Stack>
            </Box>
            {auditEvents.error && (
              <Alert severity="error">
                {auditEvents.error instanceof ApiError
                  ? auditEvents.error.message
                  : 'Activity history could not be loaded.'}
              </Alert>
            )}
            <Stack spacing={1.5} aria-live="polite">
              {auditEvents.data?.items.map((event) => (
                <Box key={event.id} sx={{ borderBottom: 1, borderColor: 'divider', pb: 1.5 }}>
                  <Typography sx={{ fontWeight: 700 }}>
                    {auditEntityLabel(event.entityType)} — {auditActionLabel(event.action)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(event.timestamp).toLocaleString()}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                    Actor {event.actorId ?? 'system'} • Entity {event.entityId ?? 'project'}
                  </Typography>
                </Box>
              ))}
              {auditEvents.data?.items.length === 0 && (
                <Typography color="text.secondary">No activity matches these filters.</Typography>
              )}
            </Stack>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="caption" color="text.secondary">
                Page {(auditEvents.data?.page ?? auditPage) + 1} of{' '}
                {Math.max(auditEvents.data?.totalPages ?? 0, 1)} •{' '}
                {auditEvents.data?.totalElements ?? 0} events
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button
                  size="small"
                  disabled={auditPage === 0 || auditEvents.isFetching}
                  onClick={() => setAuditPage((page) => Math.max(page - 1, 0))}
                >
                  Previous page
                </Button>
                <Button
                  size="small"
                  disabled={!auditEvents.data?.hasNext || auditEvents.isFetching}
                  onClick={() => setAuditPage((page) => page + 1)}
                >
                  Next page
                </Button>
              </Stack>
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <Stack component="form" onSubmit={form.handleSubmit((values) => create.mutate(values))}>
          <DialogTitle>Add a user story</DialogTitle>
          <DialogContent>
            <Stack spacing={2.25} sx={{ pt: 1 }}>
              {create.error && (
                <Alert severity="error">
                  {create.error instanceof ApiError
                    ? create.error.message
                    : 'User story could not be created.'}
                </Alert>
              )}
              <Alert severity="info">
                Use synthetic or non-production content. Do not submit secrets or personal data.
              </Alert>
              <TextField
                label="User story title"
                autoFocus
                slotProps={{ htmlInput: { maxLength: 200 } }}
                {...form.register('title')}
                error={Boolean(form.formState.errors.title)}
                helperText={form.formState.errors.title?.message}
              />
              <TextField
                select
                label="Priority"
                defaultValue="MEDIUM"
                {...form.register('priority')}
              >
                {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((value) => (
                  <MenuItem key={value} value={value}>
                    {value}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="User story statement"
                multiline
                minRows={4}
                slotProps={{ htmlInput: { maxLength: 10000 } }}
                {...form.register('userStory')}
                error={Boolean(form.formState.errors.userStory)}
                helperText={form.formState.errors.userStory?.message}
              />
              <TextField
                label="Requirements and constraints"
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
              {create.isPending ? 'Saving…' : 'Save user story'}
            </Button>
          </DialogActions>
        </Stack>
      </Dialog>
    </Stack>
  );
}
