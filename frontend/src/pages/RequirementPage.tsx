import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import ArrowDownwardRoundedIcon from '@mui/icons-material/ArrowDownwardRounded';
import ArrowUpwardRoundedIcon from '@mui/icons-material/ArrowUpwardRounded';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import ReplayRoundedIcon from '@mui/icons-material/ReplayRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  IconButton,
  LinearProgress,
  Link as MuiLink,
  MenuItem,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import { useMemo, useState } from 'react';
import { useFieldArray, useForm, useWatch } from 'react-hook-form';
import { Link, useParams } from 'react-router-dom';
import { ApiError, apiRequest, downloadExport } from '../api/client';
import type {
  AuditEvent,
  Coverage,
  GenerationRun,
  PageResponse,
  Project,
  Requirement,
  TestCase,
  TestCaseCategory,
  TestPriority,
  TestCaseStatus,
  TestCaseRevision,
  Traceability,
} from '../types/api';

const categories: TestCaseCategory[] = [
  'HAPPY_PATH',
  'NEGATIVE',
  'BOUNDARY',
  'VALIDATION',
  'PERMISSION',
  'DATA_INTEGRITY',
  'ERROR_HANDLING',
  'ACCESSIBILITY',
  'INTEGRATION',
  'SECURITY',
  'RECOVERY',
  'OTHER',
];
const priorities: TestPriority[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const statuses: TestCaseStatus[] = [
  'GENERATED',
  'IN_REVIEW',
  'APPROVED',
  'REJECTED',
  'NEEDS_REVISION',
];
const priorityRank: Record<TestPriority, number> = {
  CRITICAL: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
};
type TestCaseSort =
  'sequence-asc' | 'sequence-desc' | 'priority-desc' | 'status-asc' | 'updated-desc';
type ReviewAction = 'approve' | 'reject' | 'request-changes' | 'reopen';

/** Orders test cases by their global work-item number with stable creation and UUID tie-breakers. */
function compareByTestCaseNumber(left: TestCase, right: TestCase) {
  return (
    left.workItemNumber - right.workItemNumber ||
    left.createdAt.localeCompare(right.createdAt) ||
    left.id.localeCompare(right.id)
  );
}

/** Builds a grammatically correct completion notice for one or many generated cases. */
function generationCompletedNotice(run: GenerationRun, source: string) {
  const generated =
    run.generatedCaseCount === 1
      ? '1 manual test case was created'
      : `${run.generatedCaseCount} manual test cases were created`;
  return `Generation completed and passed the server-side quality gate. ${generated} from this story using ${source}.`;
}

/** Coordinates requirement details, generated coverage, review actions, traceability, and exports. */
export function RequirementPage() {
  const { userStoryId: requirementId = '' } = useParams();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState(0);
  const [editing, setEditing] = useState<TestCase | null>(null);
  const [reviewTarget, setReviewTarget] = useState<{
    testCase: TestCase;
    action: ReviewAction;
  } | null>(null);
  const [notice, setNotice] = useState('');
  const [caseSearch, setCaseSearch] = useState('');
  const [caseStatus, setCaseStatus] = useState<TestCaseStatus | 'ALL'>('ALL');
  const [caseCategory, setCaseCategory] = useState<TestCaseCategory | 'ALL'>('ALL');
  const [casePriority, setCasePriority] = useState<TestPriority | 'ALL'>('ALL');
  const [caseSort, setCaseSort] = useState<TestCaseSort>('sequence-asc');
  const [selectedRunId, setSelectedRunId] = useState('');
  const generationRunQuery = selectedRunId
    ? `?generationRunId=${encodeURIComponent(selectedRunId)}`
    : '';
  const requirement = useQuery({
    queryKey: ['requirement', requirementId],
    queryFn: () => apiRequest<Requirement>(`/api/v1/user-stories/${requirementId}`),
    enabled: Boolean(requirementId),
  });
  const project = useQuery({
    queryKey: ['project', requirement.data?.projectId],
    queryFn: () => apiRequest<Project>(`/api/v1/projects/${requirement.data?.projectId}`),
    enabled: Boolean(requirement.data?.projectId),
  });
  const cases = useQuery({
    queryKey: ['test-cases', requirementId, selectedRunId],
    queryFn: () =>
      apiRequest<TestCase[]>(
        `/api/v1/user-stories/${requirementId}/test-cases${generationRunQuery}`,
      ),
    enabled: Boolean(requirementId),
  });
  const coverage = useQuery({
    queryKey: ['coverage', requirementId, selectedRunId],
    queryFn: () =>
      apiRequest<Coverage>(`/api/v1/user-stories/${requirementId}/coverage${generationRunQuery}`),
    enabled: Boolean(requirementId),
  });
  const traceability = useQuery({
    queryKey: ['traceability', requirementId, selectedRunId],
    queryFn: () =>
      apiRequest<Traceability>(
        `/api/v1/user-stories/${requirementId}/traceability${generationRunQuery}`,
      ),
    enabled: Boolean(requirementId),
  });
  const runs = useQuery({
    queryKey: ['generation-runs', requirementId],
    queryFn: () =>
      apiRequest<GenerationRun[]>(`/api/v1/user-stories/${requirementId}/generation-runs`),
    enabled: Boolean(requirementId),
  });
  const visibleCases = useMemo(() => {
    const normalizedSearch = caseSearch.trim().toLocaleLowerCase();
    const filtered = (cases.data ?? []).filter((testCase) => {
      const searchableText = [
        testCase.testCaseKey,
        testCase.title,
        testCase.objective,
        ...testCase.acceptanceCriteriaKeys,
      ]
        .join(' ')
        .toLocaleLowerCase();
      return (
        (!normalizedSearch || searchableText.includes(normalizedSearch)) &&
        (caseStatus === 'ALL' || testCase.status === caseStatus) &&
        (caseCategory === 'ALL' || testCase.category === caseCategory) &&
        (casePriority === 'ALL' || testCase.priority === casePriority)
      );
    });
    return [...filtered].sort((left, right) => {
      switch (caseSort) {
        case 'sequence-desc':
          return compareByTestCaseNumber(right, left);
        case 'priority-desc':
          return (
            priorityRank[left.priority] - priorityRank[right.priority] ||
            compareByTestCaseNumber(left, right)
          );
        case 'status-asc':
          return left.status.localeCompare(right.status) || compareByTestCaseNumber(left, right);
        case 'updated-desc':
          return (
            right.updatedAt.localeCompare(left.updatedAt) || compareByTestCaseNumber(left, right)
          );
        default:
          return compareByTestCaseNumber(left, right);
      }
    });
  }, [caseCategory, casePriority, caseSearch, caseSort, caseStatus, cases.data]);
  const hasCaseFilters =
    Boolean(caseSearch) || caseStatus !== 'ALL' || caseCategory !== 'ALL' || casePriority !== 'ALL';
  /** Restores all test-case controls to the unfiltered review queue. */
  const clearCaseFilters = () => {
    setCaseSearch('');
    setCaseStatus('ALL');
    setCaseCategory('ALL');
    setCasePriority('ALL');
  };
  /** Invalidates every requirement-derived query after a generation, edit, or review transition. */
  const refreshAll = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['requirement', requirementId] }),
      queryClient.invalidateQueries({ queryKey: ['test-cases', requirementId] }),
      queryClient.invalidateQueries({ queryKey: ['coverage', requirementId] }),
      queryClient.invalidateQueries({ queryKey: ['traceability', requirementId] }),
      queryClient.invalidateQueries({ queryKey: ['generation-runs', requirementId] }),
    ]);
  };
  const generate = useMutation({
    mutationFn: async () => {
      const regenerating = runs.data?.some((run) => run.setState === 'ACTIVE') ?? false;
      const path = `/api/v1/user-stories/${requirementId}/${regenerating ? 'regenerate' : 'generate-test-cases'}`;
      const request = (confirmSupersede: boolean) =>
        apiRequest<GenerationRun>(
          `${path}${regenerating ? `?confirmSupersede=${String(confirmSupersede)}` : ''}`,
          { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() } },
        );
      try {
        return await request(false);
      } catch (error) {
        if (
          error instanceof ApiError &&
          error.code === 'supersede_confirmation_required' &&
          window.confirm(
            'The active set contains review or revision evidence. Preserve it and create a new active set?',
          )
        ) {
          return request(true);
        }
        throw error;
      }
    },
    onSuccess: async (run) => {
      const source =
        run.provider === 'openai-responses'
          ? `OpenAI model ${run.model}`
          : `the local story-driven engine ${run.model}`;
      setNotice(generationCompletedNotice(run, source));
      await refreshAll();
      setSelectedRunId('');
      setTab(1);
    },
  });
  const review = useMutation({
    mutationFn: ({
      testCase,
      action,
      text,
    }: {
      testCase: TestCase;
      action: ReviewAction;
      text: string;
    }) =>
      apiRequest(`/api/v1/test-cases/${testCase.id}/${action}`, {
        method: 'POST',
        body: JSON.stringify({
          ...(action === 'reopen' ? { reason: text.trim() } : { comments: text.trim() }),
          version: testCase.version,
        }),
      }),
    onSuccess: async () => {
      setReviewTarget(null);
      await refreshAll();
    },
  });

  if (requirement.isLoading)
    return (
      <Box sx={{ py: 10, display: 'grid', placeItems: 'center' }}>
        <CircularProgress aria-label="Loading user story" />
      </Box>
    );
  if (requirement.error || !requirement.data)
    return (
      <Alert severity="error">
        {requirement.error instanceof ApiError
          ? requirement.error.message
          : 'User story not found.'}
      </Alert>
    );
  const req = requirement.data;

  /** Downloads one export format and reports a user-facing success or failure notice. */
  const exportFile = async (format: string) => {
    setNotice('');
    try {
      await downloadExport(requirementId, format, true, selectedRunId || undefined);
      setNotice(`${format.toUpperCase()} export downloaded.`);
    } catch (error) {
      setNotice(error instanceof ApiError ? error.message : 'The export could not be completed.');
    }
  };

  return (
    <Stack spacing={3.5}>
      <Breadcrumbs>
        <MuiLink component={Link} to="/" underline="hover">
          Projects
        </MuiLink>
        {project.data && (
          <MuiLink component={Link} to={`/projects/${project.data.id}`} underline="hover">
            {project.data.name}
          </MuiLink>
        )}
        <Typography color="text.primary">{req.title}</Typography>
      </Breadcrumbs>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' } }}
      >
        <Box>
          <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
            <Typography component="h1" variant="h1">
              {req.title}
            </Typography>
            <Chip
              size="small"
              label={req.status.replaceAll('_', ' ')}
              color={
                req.status === 'NEEDS_CLARIFICATION'
                  ? 'warning'
                  : req.status === 'GENERATED'
                    ? 'success'
                    : 'default'
              }
            />
          </Stack>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            User Story {req.workItemNumber} • Source {req.sourceReference || 'not specified'} •
            Priority {req.priority} • Version {req.version}
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            startIcon={<DownloadRoundedIcon />}
            onClick={() => void exportFile('csv')}
          >
            CSV
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadRoundedIcon />}
            onClick={() => void exportFile('json')}
          >
            JSON
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadRoundedIcon />}
            onClick={() => void exportFile('markdown')}
          >
            Markdown
          </Button>
          <Button
            variant="contained"
            startIcon={cases.data?.length ? <ReplayRoundedIcon /> : <AutoAwesomeRoundedIcon />}
            onClick={() => generate.mutate()}
            disabled={generate.isPending}
          >
            {generate.isPending
              ? 'Generating…'
              : cases.data?.length
                ? 'Regenerate'
                : 'Generate tests'}
          </Button>
        </Stack>
      </Stack>
      {notice && (
        <Alert
          severity={
            notice.includes('could not') || notice.includes('Approve') ? 'warning' : 'success'
          }
          onClose={() => setNotice('')}
        >
          {notice}
        </Alert>
      )}
      {generate.error && (
        <Alert severity="error">
          {generate.error instanceof ApiError
            ? generate.error.message
            : 'Generation failed safely.'}
        </Alert>
      )}

      <Box
        sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}
      >
        <MetricCard
          label="Generated coverage"
          value={`${coverage.data?.coveragePercent ?? 0}%`}
          detail={`${coverage.data?.coveredCriteria ?? 0} of ${coverage.data?.totalCriteria ?? req.acceptanceCriteria.length} criteria`}
          color="primary.main"
        />
        <MetricCard
          label="Approved coverage"
          value={`${coverage.data?.approvedCoveragePercent ?? 0}%`}
          detail={
            (coverage.data?.approvedCriteria ?? 0) === 1
              ? '1 criterion has approved evidence'
              : `${coverage.data?.approvedCriteria ?? 0} criteria have approved evidence`
          }
          color="success.main"
        />
        <MetricCard
          label="Review queue"
          value={String(
            cases.data?.filter((item) => item.status !== 'APPROVED' && item.status !== 'REJECTED')
              .length ?? 0,
          )}
          detail={`${cases.data?.length ?? 0} total structured cases`}
          color="warning.main"
        />
      </Box>

      <Card>
        <Tabs
          value={tab}
          onChange={(_, value: number) => setTab(value)}
          variant="scrollable"
          scrollButtons="auto"
          aria-label="User Story workspace sections"
        >
          <Tab label="Story details" />
          <Tab label={`Test cases (${cases.data?.length ?? 0})`} />
          <Tab label="Traceability" />
          <Tab label={`Ambiguities (${req.ambiguities.filter((item) => !item.resolved).length})`} />
        </Tabs>
        <Divider />
        {tab === 0 && (
          <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
            <Stack spacing={3}>
              <Section title="User story" text={req.userStory} />
              <Section
                title="Requirements and constraints"
                text={req.businessRequirements || 'No additional business rules were supplied.'}
              />
              <Section
                title="Assumptions"
                text={req.assumptions || 'No assumptions were supplied.'}
              />
              <Box>
                <Typography component="h2" variant="h2" sx={{ mb: 1.5 }}>
                  Acceptance criteria
                </Typography>
                <Stack spacing={1.25}>
                  {req.acceptanceCriteria.map((criterion) => (
                    <Box
                      key={criterion.id}
                      sx={{
                        p: 2,
                        border: 1,
                        borderColor: 'divider',
                        borderRadius: 1.5,
                        display: 'flex',
                        gap: 2,
                      }}
                    >
                      <Chip
                        label={criterion.criterionKey}
                        size="small"
                        color="primary"
                        variant="outlined"
                      />
                      <Typography variant="body2">{criterion.description}</Typography>
                    </Box>
                  ))}
                </Stack>
              </Box>
            </Stack>
          </CardContent>
        )}
        {tab === 1 && (
          <Box sx={{ p: { xs: 1.5, md: 2.5 } }}>
            {cases.isLoading && <LinearProgress />}
            {cases.data?.length === 0 && (
              <Box sx={{ py: 8, textAlign: 'center' }}>
                <FactCheckOutlinedIcon color="disabled" sx={{ fontSize: 48 }} />
                <Typography variant="h2" sx={{ mt: 1 }}>
                  No test cases yet
                </Typography>
                <Typography color="text.secondary" sx={{ mt: 1 }}>
                  Generate a balanced, validated set from the user story source.
                </Typography>
              </Box>
            )}
            {(cases.data?.length ?? 0) > 0 && (
              <Stack
                component="section"
                aria-label="Test case controls"
                spacing={1.5}
                sx={{ mb: 2.5 }}
              >
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: {
                      xs: '1fr',
                      sm: 'minmax(240px, 2fr) repeat(2, minmax(150px, 1fr))',
                      lg: 'minmax(260px, 2fr) repeat(4, minmax(150px, 1fr))',
                    },
                    gap: 1.5,
                  }}
                >
                  <TextField
                    select
                    label="Generation set"
                    value={selectedRunId}
                    onChange={(event) => setSelectedRunId(event.target.value)}
                    size="small"
                  >
                    <MenuItem value="">Active set</MenuItem>
                    {runs.data
                      ?.filter((run) => run.status === 'COMPLETED')
                      .map((run) => (
                        <MenuItem key={run.id} value={run.id}>
                          Set {run.setNumber} — {run.setState}
                        </MenuItem>
                      ))}
                  </TextField>
                  <TextField
                    label="Search test cases"
                    value={caseSearch}
                    onChange={(event) => setCaseSearch(event.target.value)}
                    placeholder="ID, title, objective, or criterion"
                    size="small"
                  />
                  <TextField
                    select
                    label="Status"
                    value={caseStatus}
                    onChange={(event) =>
                      setCaseStatus(event.target.value as TestCaseStatus | 'ALL')
                    }
                    size="small"
                  >
                    <MenuItem value="ALL">All statuses</MenuItem>
                    {statuses.map((status) => (
                      <MenuItem key={status} value={status}>
                        {status.replaceAll('_', ' ')}
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField
                    select
                    label="Category"
                    value={caseCategory}
                    onChange={(event) =>
                      setCaseCategory(event.target.value as TestCaseCategory | 'ALL')
                    }
                    size="small"
                  >
                    <MenuItem value="ALL">All categories</MenuItem>
                    {categories.map((category) => (
                      <MenuItem key={category} value={category}>
                        {category.replaceAll('_', ' ')}
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField
                    select
                    label="Priority"
                    value={casePriority}
                    onChange={(event) =>
                      setCasePriority(event.target.value as TestPriority | 'ALL')
                    }
                    size="small"
                  >
                    <MenuItem value="ALL">All priorities</MenuItem>
                    {priorities.map((priority) => (
                      <MenuItem key={priority} value={priority}>
                        {priority}
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField
                    select
                    label="Sort by"
                    value={caseSort}
                    onChange={(event) => setCaseSort(event.target.value as TestCaseSort)}
                    size="small"
                  >
                    <MenuItem value="sequence-asc">Test case number (ascending)</MenuItem>
                    <MenuItem value="sequence-desc">Test case number (descending)</MenuItem>
                    <MenuItem value="priority-desc">Priority (highest first)</MenuItem>
                    <MenuItem value="status-asc">Status</MenuItem>
                    <MenuItem value="updated-desc">Recently updated</MenuItem>
                  </TextField>
                </Box>
                <Stack
                  direction="row"
                  spacing={1.5}
                  sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                >
                  <Typography variant="body2" color="text.secondary" aria-live="polite">
                    Showing {visibleCases.length} of {cases.data?.length ?? 0} test cases
                  </Typography>
                  {hasCaseFilters && (
                    <Button size="small" onClick={clearCaseFilters}>
                      Clear filters
                    </Button>
                  )}
                </Stack>
              </Stack>
            )}
            {(cases.data?.length ?? 0) > 0 && visibleCases.length === 0 && (
              <Alert severity="info">No test cases match the current filters.</Alert>
            )}
            {visibleCases.map((testCase) => (
              <TestCasePanel
                key={testCase.id}
                testCase={testCase}
                onEdit={() => setEditing(testCase)}
                onReview={(action) => {
                  review.reset();
                  setReviewTarget({ testCase, action });
                }}
                busy={review.isPending}
                readOnly={Boolean(selectedRunId)}
                projectId={req.projectId}
              />
            ))}
          </Box>
        )}
        {tab === 2 && (
          <CardContent sx={{ p: 0 }}>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Criterion</TableCell>
                    <TableCell>Acceptance criterion</TableCell>
                    <TableCell>Mapped evidence</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {traceability.data?.rows.map((row) => (
                    <TableRow key={row.acceptanceCriterionId}>
                      <TableCell sx={{ verticalAlign: 'top' }}>
                        <Chip label={row.criterionKey} size="small" />
                      </TableCell>
                      <TableCell sx={{ maxWidth: 480, verticalAlign: 'top' }}>
                        {row.description}
                      </TableCell>
                      <TableCell>
                        {row.testCases.length ? (
                          <Stack spacing={0.75}>
                            {row.testCases.map((item) => (
                              <Stack
                                key={item.id}
                                direction="row"
                                spacing={1}
                                sx={{ alignItems: 'center' }}
                              >
                                <MuiLink component="button" onClick={() => setTab(1)}>
                                  {item.testCaseKey}
                                </MuiLink>
                                <Typography variant="body2">{item.title}</Typography>
                                <Chip
                                  label={item.status}
                                  size="small"
                                  color={item.status === 'APPROVED' ? 'success' : 'default'}
                                />
                              </Stack>
                            ))}
                          </Stack>
                        ) : (
                          <Typography color="error">Not covered</Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        )}
        {tab === 3 && (
          <CardContent sx={{ p: { xs: 2.5, md: 4 } }}>
            <Stack spacing={2}>
              {req.ambiguities.length === 0 && (
                <Alert severity="success">No user story ambiguities were detected.</Alert>
              )}
              {req.ambiguities.map((item) => (
                <Card key={item.id} variant="outlined">
                  <CardContent>
                    <Stack direction="row" sx={{ justifyContent: 'space-between', gap: 2 }}>
                      <Box>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                          <WarningAmberRoundedIcon color={item.resolved ? 'disabled' : 'warning'} />
                          <Typography sx={{ fontWeight: 700 }}>
                            {item.category.replaceAll('_', ' ')}
                          </Typography>
                          <Chip
                            label={item.severity}
                            size="small"
                            color={
                              item.severity === 'HIGH' || item.severity === 'CRITICAL'
                                ? 'error'
                                : 'warning'
                            }
                          />
                        </Stack>
                        <Typography sx={{ mt: 1.5 }}>{item.description}</Typography>
                        <Typography color="text.secondary" variant="body2" sx={{ mt: 1 }}>
                          Question for the team: {item.suggestedQuestion}
                        </Typography>
                      </Box>
                      {item.resolved && (
                        <Chip
                          icon={<CheckCircleOutlineRoundedIcon />}
                          label="Resolved"
                          color="success"
                        />
                      )}
                    </Stack>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          </CardContent>
        )}
      </Card>
      {editing && (
        <EditTestCaseDialog
          testCase={editing}
          onClose={() => setEditing(null)}
          onSaved={async () => {
            setEditing(null);
            await refreshAll();
          }}
        />
      )}
      {reviewTarget && (
        <ReviewDecisionDialog
          testCase={reviewTarget.testCase}
          action={reviewTarget.action}
          busy={review.isPending}
          error={review.error}
          onClose={() => {
            if (!review.isPending) {
              review.reset();
              setReviewTarget(null);
            }
          }}
          onSubmit={(text) =>
            review.mutate({
              testCase: reviewTarget.testCase,
              action: reviewTarget.action,
              text,
            })
          }
        />
      )}
    </Stack>
  );
}

/** Renders one compact requirement health metric with supporting context. */
function MetricCard({
  label,
  value,
  detail,
  color,
}: {
  label: string;
  value: string;
  detail: string;
  color: string;
}) {
  return (
    <Card>
      <CardContent>
        <Typography color="text.secondary" variant="body2">
          {label}
        </Typography>
        <Typography sx={{ fontSize: '2rem', fontWeight: 760, color, mt: 0.5 }}>{value}</Typography>
        <Typography variant="caption" color="text.secondary">
          {detail}
        </Typography>
      </CardContent>
    </Card>
  );
}

/** Renders a titled block of preserved requirement prose. */
function Section({ title, text }: { title: string; text: string }) {
  return (
    <Box>
      <Typography component="h2" variant="h2" sx={{ mb: 1 }}>
        {title}
      </Typography>
      <Typography sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.75 }}>{text}</Typography>
    </Box>
  );
}

/** Collects decision-specific human evidence before changing a test-case review state. */
function ReviewDecisionDialog({
  testCase,
  action,
  busy,
  error,
  onClose,
  onSubmit,
}: {
  testCase: TestCase;
  action: ReviewAction;
  busy: boolean;
  error: Error | null;
  onClose(): void;
  onSubmit(text: string): void;
}) {
  const [text, setText] = useState('');
  const [validation, setValidation] = useState('');
  const required = action !== 'approve';
  const decisionLabel =
    action === 'request-changes'
      ? 'Request changes'
      : action === 'reopen'
        ? 'Reopen for review'
        : action === 'reject'
          ? 'Reject'
          : 'Approve';
  const inputLabel =
    action === 'reopen'
      ? 'Reopen reason'
      : action === 'approve'
        ? 'Approval comment (optional)'
        : 'Review comment';
  const submitLabel =
    action === 'request-changes'
      ? 'Request changes'
      : action === 'reopen'
        ? 'Reopen test case'
        : action === 'reject'
          ? 'Reject test case'
          : 'Confirm approval';

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <Stack
        component="form"
        onSubmit={(event) => {
          event.preventDefault();
          if (required && text.trim().length < 3) {
            setValidation(
              action === 'reopen'
                ? 'Enter a meaningful reason for reopening this test case.'
                : 'Enter a meaningful comment for this review decision.',
            );
            return;
          }
          setValidation('');
          onSubmit(text);
        }}
      >
        <DialogTitle>
          {decisionLabel} {testCase.testCaseKey}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <Alert severity={action === 'reject' ? 'warning' : 'info'}>
              {testCase.title} • Current state {testCase.status.replaceAll('_', ' ')}
            </Alert>
            {error && (
              <Alert severity="error">
                {error instanceof ApiError
                  ? error.message
                  : 'The review decision could not be saved.'}
              </Alert>
            )}
            <TextField
              autoFocus
              multiline
              minRows={4}
              label={inputLabel}
              value={text}
              onChange={(event) => {
                setText(event.target.value);
                if (validation) setValidation('');
              }}
              required={required}
              error={Boolean(validation)}
              helperText={
                validation ||
                (required
                  ? 'Required. Record the evidence behind this decision.'
                  : 'Optional. Add evidence that will help future reviewers.')
              }
              slotProps={{ htmlInput: { maxLength: 4000 } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            color={action === 'reject' ? 'error' : action === 'approve' ? 'success' : 'primary'}
            disabled={busy}
          >
            {busy ? 'Saving…' : submitLabel}
          </Button>
        </DialogActions>
      </Stack>
    </Dialog>
  );
}

/** Presents a generated test case, its evidence, and the available human review actions. */
function TestCasePanel({
  testCase,
  onEdit,
  onReview,
  busy,
  readOnly,
  projectId,
}: {
  testCase: TestCase;
  onEdit(): void;
  onReview(action: ReviewAction): void;
  busy: boolean;
  readOnly: boolean;
  projectId: string;
}) {
  const [historyOpen, setHistoryOpen] = useState(false);
  return (
    <Accordion disableGutters>
      <AccordionSummary expandIcon={<ExpandMoreRoundedIcon />}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={1.25}
          sx={{ alignItems: { xs: 'flex-start', md: 'center' }, width: '100%' }}
        >
          <Typography color="primary.main" sx={{ fontWeight: 750 }}>
            {testCase.testCaseKey}
          </Typography>
          <Typography sx={{ fontWeight: 650, flexGrow: 1 }}>{testCase.title}</Typography>
          <Chip label={testCase.category.replaceAll('_', ' ')} size="small" />
          <Chip
            label={testCase.status.replaceAll('_', ' ')}
            size="small"
            color={
              testCase.status === 'APPROVED'
                ? 'success'
                : testCase.status === 'REJECTED'
                  ? 'error'
                  : 'default'
            }
          />
        </Stack>
      </AccordionSummary>
      <AccordionDetails>
        <Stack spacing={2.5}>
          <Typography color="text.secondary">{testCase.objective}</Typography>
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
            <Chip label={`Priority ${testCase.priority}`} size="small" variant="outlined" />
            <Chip label={`Risk ${testCase.riskLevel}`} size="small" variant="outlined" />
            {testCase.acceptanceCriteriaKeys.map((key) => (
              <Chip key={key} label={key} size="small" color="primary" variant="outlined" />
            ))}
          </Stack>
          <Box>
            <Typography sx={{ fontWeight: 700, mb: 1 }}>Preconditions</Typography>
            <Stack component="ul" spacing={0.5} sx={{ m: 0, pl: 3 }}>
              {testCase.preconditions.map((item) => (
                <Typography component="li" variant="body2" key={item.sortOrder}>
                  {item.description}
                </Typography>
              ))}
            </Stack>
          </Box>
          <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 1 }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell width={60}>Step</TableCell>
                  <TableCell>Action</TableCell>
                  <TableCell>Expected result</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {testCase.steps.map((step) => (
                  <TableRow key={step.stepNumber}>
                    <TableCell>{step.stepNumber}</TableCell>
                    <TableCell>{step.action}</TableCell>
                    <TableCell>{step.expectedResult}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <Alert severity="success" icon={<FactCheckOutlinedIcon />}>
            <strong>Final outcome:</strong> {testCase.finalExpectedOutcome}
          </Alert>
          <Button
            onClick={() => setHistoryOpen((value) => !value)}
            sx={{ alignSelf: 'flex-start' }}
          >
            {historyOpen ? 'Hide history' : 'Show history'}
          </Button>
          {historyOpen && <TestCaseHistory testCase={testCase} projectId={projectId} />}
          {readOnly && <Alert severity="info">Historical generation sets are read-only.</Alert>}
          {!readOnly && (
            <Stack
              direction="row"
              spacing={1}
              sx={{ justifyContent: 'flex-end', flexWrap: 'wrap' }}
            >
              {(testCase.status === 'GENERATED' ||
                testCase.status === 'IN_REVIEW' ||
                testCase.status === 'NEEDS_REVISION') && (
                <Button startIcon={<EditOutlinedIcon />} onClick={onEdit}>
                  Edit
                </Button>
              )}
              {(testCase.status === 'GENERATED' || testCase.status === 'IN_REVIEW') && (
                <>
                  <Button
                    color="warning"
                    onClick={() => onReview('request-changes')}
                    disabled={busy}
                  >
                    Request changes
                  </Button>
                  <Button color="error" onClick={() => onReview('reject')} disabled={busy}>
                    Reject
                  </Button>
                  <Button
                    variant="contained"
                    color="success"
                    onClick={() => onReview('approve')}
                    disabled={busy}
                  >
                    Approve
                  </Button>
                </>
              )}
              {(testCase.status === 'APPROVED' || testCase.status === 'REJECTED') && (
                <Button onClick={() => onReview('reopen')} disabled={busy}>
                  Reopen for review
                </Button>
              )}
            </Stack>
          )}
        </Stack>
      </AccordionDetails>
    </Accordion>
  );
}

const comparisonFields = [
  ['Title', 'title'],
  ['Objective', 'objective'],
  ['Category', 'category'],
  ['Priority', 'priority'],
  ['Risk level', 'riskLevel'],
  ['Status', 'status'],
  ['Preconditions', 'preconditions'],
  ['Steps', 'steps'],
  ['Test data', 'testData'],
  ['Final outcome', 'finalExpectedOutcome'],
] as const;

/** Converts a normalized revision field into readable inert comparison text. */
function comparisonText(snapshot: Record<string, unknown>, field: string) {
  const value = snapshot[field];
  if (value == null) return 'Not recorded';
  if (!Array.isArray(value)) return typeof value === 'string' ? value : String(value);
  if (value.length === 0) return 'None';
  if (field === 'preconditions') {
    return value
      .map((item, index) => {
        const record =
          typeof item === 'object' && item !== null ? (item as Record<string, unknown>) : {};
        return `${index + 1}. ${String(record.description ?? item)}`;
      })
      .join('\n');
  }
  if (field === 'steps') {
    return value
      .map((item, index) => {
        const record =
          typeof item === 'object' && item !== null ? (item as Record<string, unknown>) : {};
        const reference = record.testDataReference
          ? ` [data: ${String(record.testDataReference)}]`
          : '';
        return `${String(record.stepNumber ?? index + 1)}. ${String(record.action ?? '')} → ${String(record.expectedResult ?? '')}${reference}`;
      })
      .join('\n');
  }
  if (field === 'testData') {
    return value
      .map((item) => {
        const record =
          typeof item === 'object' && item !== null ? (item as Record<string, unknown>) : {};
        return `${String(record.name ?? 'Unnamed')}: ${String(record.description ?? '')} (${String(record.exampleValue ?? 'no example')})`;
      })
      .join('\n');
  }
  return value.map((item) => String(item)).join('\n');
}

/** Presents structured revisions, review evidence, and matching audit events as inert text. */
function TestCaseHistory({ testCase, projectId }: { testCase: TestCase; projectId: string }) {
  const [selectedRevisionId, setSelectedRevisionId] = useState('');
  const revisions = useQuery({
    queryKey: ['test-case-revisions', testCase.id],
    queryFn: () =>
      apiRequest<PageResponse<TestCaseRevision>>(
        `/api/v1/test-cases/${testCase.id}/revisions?size=20`,
      ),
  });
  const audit = useQuery({
    queryKey: ['test-case-audit', projectId, testCase.id],
    queryFn: () =>
      apiRequest<PageResponse<AuditEvent>>(
        `/api/v1/projects/${projectId}/audit-events?entityType=TEST_CASE&entityId=${encodeURIComponent(testCase.id)}&size=20`,
      ),
  });
  const effectiveRevisionId = selectedRevisionId || revisions.data?.items[0]?.id || '';
  const selectedRevision = revisions.data?.items.find((item) => item.id === effectiveRevisionId);
  const currentSnapshot = testCase as unknown as Record<string, unknown>;
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack spacing={2}>
          <Box>
            <Typography sx={{ fontWeight: 750 }}>Revision timeline</Typography>
            {revisions.data?.items.map((revision) => (
              <Button
                key={revision.id}
                size="small"
                variant={effectiveRevisionId === revision.id ? 'contained' : 'text'}
                onClick={() => setSelectedRevisionId(revision.id)}
                aria-label={`Compare revision ${revision.revisionNumber}`}
                sx={{ mt: 1, mr: 1 }}
              >
                Revision {revision.revisionNumber} • {new Date(revision.changedAt).toLocaleString()}
              </Button>
            ))}
            {revisions.data?.items.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No saved revisions.
              </Typography>
            )}
          </Box>
          {selectedRevision && (
            <Box>
              <Typography sx={{ fontWeight: 750, mb: 1 }}>
                Revision {selectedRevision.revisionNumber} compared with current
              </Typography>
              <TableContainer sx={{ border: 1, borderColor: 'divider', borderRadius: 1 }}>
                <Table size="small" aria-label="Revision comparison">
                  <TableHead>
                    <TableRow>
                      <TableCell>Field</TableCell>
                      <TableCell>Selected revision</TableCell>
                      <TableCell>Current test case</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {comparisonFields.map(([label, field]) => (
                      <TableRow key={field}>
                        <TableCell sx={{ fontWeight: 700, verticalAlign: 'top' }}>
                          {label}
                        </TableCell>
                        <TableCell sx={{ whiteSpace: 'pre-wrap', verticalAlign: 'top' }}>
                          {comparisonText(selectedRevision.snapshot, field)}
                        </TableCell>
                        <TableCell sx={{ whiteSpace: 'pre-wrap', verticalAlign: 'top' }}>
                          {comparisonText(currentSnapshot, field)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
          <Box>
            <Typography sx={{ fontWeight: 750 }}>Reviews</Typography>
            {testCase.reviews.map((review) => (
              <Typography key={review.id} variant="body2" sx={{ mt: 0.75 }}>
                {review.decision.replaceAll('_', ' ')} • {review.comments || 'No comment'}
              </Typography>
            ))}
            {testCase.reviews.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No review decisions.
              </Typography>
            )}
          </Box>
          <Box>
            <Typography sx={{ fontWeight: 750 }}>Audit timeline</Typography>
            {audit.data?.items.map((event) => (
              <Typography key={event.id} variant="body2" sx={{ mt: 0.75 }}>
                {new Date(event.timestamp).toLocaleString()} • {event.action.replaceAll('_', ' ')}
              </Typography>
            ))}
            {audit.data?.items.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No matching audit events.
              </Typography>
            )}
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}

/** Edits structured test-case fields while guarding against accidental loss of local changes. */
function EditTestCaseDialog({
  testCase,
  onClose,
  onSaved,
}: {
  testCase: TestCase;
  onClose(): void;
  onSaved(): Promise<void>;
}) {
  const {
    register,
    control,
    handleSubmit,
    getValues,
    setValue,
    formState: { isDirty, isSubmitting },
  } = useForm<TestCase>({ defaultValues: testCase });
  const stepFields = useFieldArray({ control, name: 'steps' });
  const preconditionFields = useFieldArray({ control, name: 'preconditions' });
  const dataFields = useFieldArray({ control, name: 'testData' });
  const watchedData = useWatch({ control, name: 'testData' });
  const watchedSteps = useWatch({ control, name: 'steps' });
  const [error, setError] = useState('');
  const normalizedDataName = (value: string | null | undefined) =>
    (value ?? '').trim().toLocaleLowerCase();
  /** Propagates a data-item rename to every case-insensitive matching step reference. */
  const renameTestData = (index: number, nextName: string) => {
    const previousName = getValues(`testData.${index}.name`);
    setValue(`testData.${index}.name`, nextName, { shouldDirty: true });
    if (!normalizedDataName(previousName)) return;
    getValues('steps').forEach((step, stepIndex) => {
      if (normalizedDataName(step.testDataReference) === normalizedDataName(previousName)) {
        setValue(`steps.${stepIndex}.testDataReference`, nextName || null, {
          shouldDirty: true,
        });
      }
    });
  };
  /** Requires an explicit reference clear before deleting a referenced data item. */
  const removeTestData = (index: number) => {
    const name = getValues(`testData.${index}.name`);
    const referencedSteps = getValues('steps')
      .map((step, stepIndex) => ({ step, stepIndex }))
      .filter(
        ({ step }) =>
          normalizedDataName(name) &&
          normalizedDataName(step.testDataReference) === normalizedDataName(name),
      );
    if (
      referencedSteps.length > 0 &&
      !window.confirm(
        `Clear ${referencedSteps.length} step reference(s) before deleting this test-data item?`,
      )
    ) {
      return;
    }
    referencedSteps.forEach(({ stepIndex }) =>
      setValue(`steps.${stepIndex}.testDataReference`, null, { shouldDirty: true }),
    );
    dataFields.remove(index);
  };
  /** Closes immediately when clean or asks before discarding unsaved edits. */
  const requestClose = () => {
    if (!isDirty || window.confirm('Discard the unsaved test-case changes?')) onClose();
  };
  const submit = handleSubmit(async (values) => {
    setError('');
    try {
      await apiRequest(`/api/v1/test-cases/${testCase.id}`, {
        method: 'PATCH',
        body: JSON.stringify({
          title: values.title,
          objective: values.objective,
          category: values.category,
          priority: values.priority,
          riskLevel: values.riskLevel,
          automationCandidate: values.automationCandidate,
          rationale: values.rationale,
          finalExpectedOutcome: values.finalExpectedOutcome,
          preconditions: values.preconditions.map((item) => item.description),
          steps: values.steps.map((step, index) => ({ ...step, stepNumber: index + 1 })),
          testData: values.testData,
          version: values.version,
        }),
      });
      await onSaved();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'The test case could not be saved.');
    }
  });
  return (
    <Dialog open onClose={requestClose} fullWidth maxWidth="md">
      <Stack component="form" onSubmit={submit}>
        <DialogTitle>Edit {testCase.testCaseKey}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.25} sx={{ pt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="Title" {...register('title')} required />
            <TextField
              label="Objective"
              multiline
              minRows={2}
              {...register('objective')}
              required
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                select
                fullWidth
                label="Category"
                defaultValue={testCase.category}
                {...register('category')}
              >
                {categories.map((item) => (
                  <MenuItem key={item} value={item}>
                    {item.replaceAll('_', ' ')}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                fullWidth
                label="Priority"
                defaultValue={testCase.priority}
                {...register('priority')}
              >
                {priorities.map((item) => (
                  <MenuItem key={item} value={item}>
                    {item}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                fullWidth
                label="Risk"
                defaultValue={testCase.riskLevel}
                {...register('riskLevel')}
              >
                {priorities.map((item) => (
                  <MenuItem key={item} value={item}>
                    {item}
                  </MenuItem>
                ))}
              </TextField>
            </Stack>
            <FormControlLabel
              control={<Checkbox {...register('automationCandidate')} />}
              label="Suitable candidate for future automation analysis"
            />
            <TextField label="Rationale" multiline minRows={2} {...register('rationale')} />
            <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
              <Typography sx={{ fontWeight: 750 }}>Preconditions</Typography>
              <Button
                startIcon={<AddRoundedIcon />}
                onClick={() =>
                  preconditionFields.append({
                    sortOrder: preconditionFields.fields.length,
                    description: '',
                  })
                }
              >
                Add precondition
              </Button>
            </Stack>
            {preconditionFields.fields.map((field, index) => (
              <Stack key={field.id} direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
                <TextField
                  fullWidth
                  label={`Precondition ${index + 1}`}
                  {...register(`preconditions.${index}.description`)}
                  required
                />
                <IconButton
                  aria-label={`Move precondition ${index + 1} up`}
                  disabled={index === 0}
                  onClick={() => preconditionFields.move(index, index - 1)}
                >
                  <ArrowUpwardRoundedIcon />
                </IconButton>
                <IconButton
                  aria-label={`Move precondition ${index + 1} down`}
                  disabled={index === preconditionFields.fields.length - 1}
                  onClick={() => preconditionFields.move(index, index + 1)}
                >
                  <ArrowDownwardRoundedIcon />
                </IconButton>
                <IconButton
                  aria-label={`Remove precondition ${index + 1}`}
                  onClick={() => preconditionFields.remove(index)}
                >
                  <DeleteOutlineRoundedIcon />
                </IconButton>
              </Stack>
            ))}
            <Typography sx={{ fontWeight: 750 }}>Ordered steps</Typography>
            {stepFields.fields.map((field, index) => (
              <Box
                key={field.id}
                sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 1.5 }}
              >
                <Stack
                  direction="row"
                  sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                >
                  <Typography variant="caption" color="text.secondary">
                    Step {index + 1}
                  </Typography>
                  <Stack direction="row">
                    <IconButton
                      size="small"
                      aria-label={`Move step ${index + 1} up`}
                      disabled={index === 0}
                      onClick={() => stepFields.move(index, index - 1)}
                    >
                      <ArrowUpwardRoundedIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      aria-label={`Move step ${index + 1} down`}
                      disabled={index === stepFields.fields.length - 1}
                      onClick={() => stepFields.move(index, index + 1)}
                    >
                      <ArrowDownwardRoundedIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      aria-label={`Remove step ${index + 1}`}
                      disabled={stepFields.fields.length === 1}
                      onClick={() => stepFields.remove(index)}
                    >
                      <DeleteOutlineRoundedIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </Stack>
                <Stack spacing={1.5} sx={{ mt: 1 }}>
                  <TextField
                    label="Action"
                    multiline
                    {...register(`steps.${index}.action`)}
                    required
                  />
                  <TextField
                    label="Expected result"
                    multiline
                    {...register(`steps.${index}.expectedResult`)}
                    required
                  />
                  <TextField
                    select
                    label="Test data reference"
                    value={watchedSteps?.[index]?.testDataReference ?? ''}
                    onChange={(event) =>
                      setValue(`steps.${index}.testDataReference`, event.target.value || null, {
                        shouldDirty: true,
                      })
                    }
                  >
                    <MenuItem value="">None</MenuItem>
                    {(watchedData ?? [])
                      .map((item) => item.name.trim())
                      .filter(Boolean)
                      .map((name, dataIndex) => (
                        <MenuItem key={`${normalizedDataName(name)}-${dataIndex}`} value={name}>
                          {name}
                        </MenuItem>
                      ))}
                  </TextField>
                </Stack>
              </Box>
            ))}
            <Button
              startIcon={<AddRoundedIcon />}
              disabled={stepFields.fields.length >= 30}
              onClick={() =>
                stepFields.append({
                  stepNumber: stepFields.fields.length + 1,
                  action: '',
                  expectedResult: '',
                  testDataReference: null,
                })
              }
            >
              Add step
            </Button>
            <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
              <Typography sx={{ fontWeight: 750 }}>Synthetic test data</Typography>
              <Button
                startIcon={<AddRoundedIcon />}
                onClick={() =>
                  dataFields.append({
                    name: '',
                    description: '',
                    exampleValue: '',
                    sensitivity: 'PUBLIC',
                    generationStrategy: 'Create a unique synthetic value.',
                  })
                }
              >
                Add test data
              </Button>
            </Stack>
            {dataFields.fields.map((field, index) => (
              <Box
                key={field.id}
                sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 1.5 }}
              >
                <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
                  <Typography variant="caption" color="text.secondary">
                    Data item {index + 1}
                  </Typography>
                  <IconButton
                    size="small"
                    aria-label={`Remove test data item ${index + 1}`}
                    onClick={() => removeTestData(index)}
                  >
                    <DeleteOutlineRoundedIcon fontSize="small" />
                  </IconButton>
                </Stack>
                <Stack spacing={1.5} sx={{ mt: 1 }}>
                  <TextField
                    label="Name"
                    value={watchedData?.[index]?.name ?? ''}
                    {...register(`testData.${index}.name`)}
                    onChange={(event) => renameTestData(index, event.target.value)}
                    required
                  />
                  <TextField
                    label="Description"
                    {...register(`testData.${index}.description`)}
                    required
                  />
                  <TextField
                    label="Synthetic example"
                    {...register(`testData.${index}.exampleValue`)}
                    required
                  />
                  <TextField
                    select
                    label="Sensitivity"
                    defaultValue={field.sensitivity}
                    {...register(`testData.${index}.sensitivity`)}
                  >
                    {['PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED'].map((value) => (
                      <MenuItem key={value} value={value}>
                        {value}
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField
                    label="Generation strategy"
                    {...register(`testData.${index}.generationStrategy`)}
                    required
                  />
                </Stack>
              </Box>
            ))}
            <TextField
              label="Final expected outcome"
              multiline
              minRows={2}
              {...register('finalExpectedOutcome')}
              required
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={requestClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            {isSubmitting ? 'Saving…' : 'Save changes'}
          </Button>
        </DialogActions>
      </Stack>
    </Dialog>
  );
}
