import { useState } from 'react';
import { Link } from 'react-router';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  Container,
  Divider,
  FormControlLabel,
  MenuItem,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import PlayArrowRoundedIcon from '@mui/icons-material/PlayArrowRounded';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import { ThemeModeControl } from '../components/ThemeModeControl';
import {
  importStory,
  safeEvidenceJson,
  type Approval,
  type Environment,
  type FixtureId,
  type SimulationContract,
  type StorySource,
} from '../simulation/contracts';
import {
  defaultEnvironment,
  fixtureSources,
  proposeFixture,
  sourceIssues,
} from '../simulation/fixtures';
import {
  approvalMatches,
  approveContract,
  contractIssues,
  runSimulation,
  type RunRecord,
} from '../simulation/runner';

/** Demonstrates source-to-evidence controls locally with clearly labeled deterministic Salesforce fixtures. */
export function SimulationPage() {
  const [tab, setTab] = useState(0);
  const [fixtureId, setFixtureId] = useState<FixtureId>('case');
  const [source, setSource] = useState<StorySource>(structuredClone(fixtureSources.case));
  const [environment, setEnvironment] = useState<Environment>({
    ...structuredClone(defaultEnvironment),
    variables: { recordName: 'Synthetic Service Request' },
  });
  const [clarification, setClarification] = useState<SimulationContract['clarification']>({
    priority: 'High',
    owner: 'Synthetic Queue',
  });
  const [proposal, setProposal] = useState<SimulationContract | null>(null);
  const [approval, setApproval] = useState<Approval | null>(null);
  const [reviewed, setReviewed] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [importText, setImportText] = useState('');
  const [fault, setFault] = useState<'normal' | 'interrupt' | 'duplicate'>('normal');
  const [history, setHistory] = useState<RunRecord[]>([]);
  const [selectedRun, setSelectedRun] = useState<string | null>(null);
  const issues = proposal ? contractIssues(proposal) : sourceIssues(source, fixtureId);
  const approved = proposal !== null && approvalMatches(proposal, approval);
  const run = history.find((item) => item.id === selectedRun) ?? history[0];

  /** Clears the human decision whenever source, executable data or environment changes. */
  function invalidate() {
    if (approval)
      setNotice(
        'Approval invalidated. Review and approve the changed contract before another run.',
      );
    setApproval(null);
    setReviewed(false);
    setError('');
  }
  /** Loads a maintained source without changing reusable environment identity or role. */
  function loadFixture(id: FixtureId) {
    invalidate();
    setFixtureId(id);
    setSource(structuredClone(fixtureSources[id]));
    setProposal(null);
    setTab(0);
    setNotice('Supported fixture loaded. Review the source and clarify the intended data.');
  }
  /** Keeps a custom source as a draft until its criteria match a supported fixture. */
  function editSource(next: StorySource) {
    invalidate();
    setSource(next);
    setProposal(null);
  }
  /** Rebuilds concrete values and expected assertions when a clarification or environment changes. */
  function updateContext(
    nextEnvironment: Environment,
    nextClarification: SimulationContract['clarification'],
  ) {
    invalidate();
    setEnvironment(nextEnvironment);
    setClarification(nextClarification);
    if (proposal) {
      setProposal(proposeFixture(source, fixtureId, nextEnvironment, nextClarification));
      setNotice(
        'Proposal rebuilt for the changed context. Previous assertion edits and approval were cleared; review the new values and expected results.',
      );
    }
  }
  /** Parses a local bounded source file or pasted JSON without uploading any content. */
  function applyImport(value: string) {
    try {
      editSource(importStory(value));
      setNotice(
        'Source imported locally. Supported criterion semantics are checked before approval.',
      );
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Import could not be read.');
    }
  }
  /** Builds a deterministic proposal for review and never invokes an AI provider. */
  function propose() {
    invalidate();
    const next = proposeFixture(source, fixtureId, environment, clarification);
    setProposal(next);
    if (contractIssues(next).length) {
      setError(contractIssues(next).join(' '));
      return;
    }
    setNotice('Fixture proposal ready. Review each action, expected result and criterion mapping.');
    setTab(1);
  }
  /** Edits one assertion in the human review surface and revokes any prior approval. */
  function editExpected(stepId: string, value: string) {
    if (!proposal) return;
    invalidate();
    setProposal({
      ...proposal,
      steps: proposal.steps.map((step) =>
        step.id === stepId && step.action.type === 'ASSERT_FIELD'
          ? { ...step, action: { ...step.action, expected: value } }
          : step,
      ),
    });
  }
  /** Captures a human-approved immutable copy after application validation succeeds. */
  function approve() {
    if (!proposal || !reviewed) return;
    try {
      setApproval(approveContract(proposal));
      setNotice(
        'Approved contract captured. Runs use this exact source, data, assertions and environment.',
      );
      setTab(2);
    } catch {
      setError('Resolve the blocking validation issues before approval.');
    }
  }
  /** Starts a new isolated run; an interrupted prior run is never resumed or retried. */
  function execute() {
    if (!proposal || !approval) return;
    try {
      const result = runSimulation(proposal, approval, fault);
      setHistory((items) => [result, ...items].slice(0, 20));
      setSelectedRun(result.id);
      setError('');
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Simulation stopped safely.');
    }
  }
  /** Downloads sanitized evidence for one immutable run, with no credential values or source secrets. */
  function download() {
    if (!run) return;
    const blob = new Blob([safeEvidenceJson(run)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `testforge-simulation-${run.id}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Box
        component="header"
        sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}
      >
        <Container maxWidth="xl">
          <Stack
            direction="row"
            sx={{ minHeight: 76, alignItems: 'center', justifyContent: 'space-between', gap: 1 }}
          >
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
              <Box
                aria-hidden="true"
                sx={{
                  bgcolor: 'primary.main',
                  color: 'primary.contrastText',
                  borderRadius: 1,
                  p: 1,
                  fontWeight: 800,
                }}
              >
                TF
              </Box>
              <Box>
                <Typography sx={{ fontWeight: 750 }}>TestForge AI</Typography>
                <Typography variant="caption" color="text.secondary">
                  Salesforce QA walkthrough
                </Typography>
              </Box>
            </Stack>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <Button component={Link} to="/" sx={{ display: { xs: 'none', sm: 'inline-flex' } }}>
                Saved workspace
              </Button>
              <ThemeModeControl compact />
            </Stack>
          </Stack>
        </Container>
      </Box>
      <Container component="main" maxWidth="xl" sx={{ py: { xs: 3, md: 4 }, px: { xs: 2, md: 4 } }}>
        <Stack spacing={3}>
          <Box>
            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1, mb: 1.5 }}>
              <Chip color="primary" label="Synthetic simulation" size="small" />
              <Chip label="No Salesforce org connected" size="small" variant="outlined" />
            </Stack>
            <Typography variant="h1">From story to reviewed evidence</Typography>
            <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 840 }}>
              Clarify the requirement, review every proposed step, then approve a repeatable
              simulation. Human judgment stays at the center.
            </Typography>
          </Box>
          <Alert severity="info">
            Deterministic fixture demo. No AI request, Salesforce connection or external write
            occurs. This is separate from saved workspace data; refreshing clears proposals and run
            history. Use synthetic text only.
          </Alert>
          {error && (
            <Alert severity="error" onClose={() => setError('')}>
              {error}
            </Alert>
          )}
          {notice && (
            <Alert severity="success" role="status" onClose={() => setNotice('')}>
              {notice}
            </Alert>
          )}
          <Tabs
            value={tab}
            onChange={(_, value: number) => setTab(value)}
            variant="fullWidth"
            aria-label="Simulation workflow"
          >
            <Tab label="1. Source & clarify" />
            <Tab label="2. Review & approve" />
            <Tab label="3. Run & evidence" />
          </Tabs>
          {tab === 0 && (
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { md: 'minmax(0, 2fr) minmax(280px, 1fr)' },
                gap: 3,
              }}
            >
              <Card>
                <CardContent>
                  <Stack spacing={2.5}>
                    <Box>
                      <Typography variant="h2">Story intake</Typography>
                      <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
                        Start with one of two supported workflows or import a source for review.
                      </Typography>
                    </Box>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                      <Button
                        variant={fixtureId === 'record' ? 'contained' : 'outlined'}
                        onClick={() => loadFixture('record')}
                      >
                        Account create / update
                      </Button>
                      <Button
                        variant={fixtureId === 'case' ? 'contained' : 'outlined'}
                        onClick={() => loadFixture('case')}
                      >
                        Case management
                      </Button>
                    </Stack>
                    <TextField
                      label="Story title"
                      required
                      value={source.title}
                      onChange={(event) => editSource({ ...source, title: event.target.value })}
                      slotProps={{ htmlInput: { maxLength: 200 } }}
                    />
                    <TextField
                      label="User story / description"
                      required
                      multiline
                      minRows={3}
                      value={source.userStory}
                      onChange={(event) => editSource({ ...source, userStory: event.target.value })}
                      slotProps={{ htmlInput: { maxLength: 4000 } }}
                    />
                    <TextField
                      label="Acceptance criteria (one per line)"
                      required
                      multiline
                      minRows={4}
                      value={source.acceptanceCriteria.join('\n')}
                      onChange={(event) =>
                        editSource({
                          ...source,
                          acceptanceCriteria: event.target.value.split('\n'),
                        })
                      }
                      helperText="Each criterion is mapped to an observable assertion. Custom criteria remain a manual draft until supported."
                      slotProps={{ htmlInput: { maxLength: 12000 } }}
                    />
                    {issues.length > 0 && <Alert severity="warning">{issues.join(' ')}</Alert>}
                    <Button
                      variant="contained"
                      size="large"
                      startIcon={<FactCheckOutlinedIcon />}
                      onClick={propose}
                    >
                      Propose fixture steps
                    </Button>
                    <Accordion variant="outlined">
                      <AccordionSummary expandIcon={<ExpandMoreRoundedIcon />}>
                        Paste or import story JSON
                      </AccordionSummary>
                      <AccordionDetails>
                        <Stack spacing={2}>
                          <Typography variant="body2">
                            Local JSON only: title, userStory, acceptanceCriteria. Maximum 24,000
                            bytes. No credentials or production data.
                          </Typography>
                          <TextField
                            multiline
                            minRows={4}
                            label="Story JSON"
                            value={importText}
                            onChange={(event) => setImportText(event.target.value)}
                            slotProps={{ htmlInput: { maxLength: 24000 } }}
                          />
                          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                            <Button onClick={() => applyImport(importText)} variant="outlined">
                              Apply pasted JSON
                            </Button>
                            <Button component="label" variant="outlined">
                              Import JSON file
                              <input
                                aria-label="Import story JSON file"
                                type="file"
                                accept=".json,application/json"
                                hidden
                                onChange={(event) => {
                                  const file = event.target.files?.[0];
                                  if (!file) return;
                                  if (file.size > 24000) {
                                    setError('Import file must be at most 24,000 bytes.');
                                    return;
                                  }
                                  void file
                                    .text()
                                    .then(applyImport)
                                    .catch(() => setError('The local file could not be read.'));
                                  event.target.value = '';
                                }}
                              />
                            </Button>
                            <Button
                              onClick={() =>
                                setImportText(JSON.stringify(fixtureSources[fixtureId], null, 2))
                              }
                            >
                              Show fixture JSON
                            </Button>
                          </Stack>
                        </Stack>
                      </AccordionDetails>
                    </Accordion>
                  </Stack>
                </CardContent>
              </Card>
              <Stack spacing={3}>
                <Card>
                  <CardContent>
                    <Stack spacing={2}>
                      <Typography variant="h2">Clarify before generation</Typography>
                      {fixtureId === 'case' ? (
                        <>
                          <Typography variant="body2" color="text.secondary">
                            Which priority and owner should the case receive? Your answers become
                            both update values and expected results.
                          </Typography>
                          <TextField
                            select
                            label="Clarified priority"
                            value={clarification.priority}
                            onChange={(event) =>
                              updateContext(environment, {
                                ...clarification,
                                priority: event.target.value as 'High' | 'Medium',
                              })
                            }
                          >
                            <MenuItem value="High">High</MenuItem>
                            <MenuItem value="Medium">Medium</MenuItem>
                          </TextField>
                          <TextField
                            select
                            label="Clarified owner"
                            value={clarification.owner}
                            onChange={(event) =>
                              updateContext(environment, {
                                ...clarification,
                                owner: event.target.value as 'Synthetic Queue' | 'Synthetic Agent',
                              })
                            }
                          >
                            <MenuItem value="Synthetic Queue">Synthetic Queue</MenuItem>
                            <MenuItem value="Synthetic Agent">Synthetic Agent</MenuItem>
                          </TextField>
                        </>
                      ) : (
                        <Typography color="text.secondary">
                          This fixture specifies Technology as the target industry. No unresolved
                          decision is needed.
                        </Typography>
                      )}
                    </Stack>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent>
                    <Stack spacing={1.5}>
                      <Typography variant="h2">Work item previews</Typography>
                      <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
                        <Typography>Azure DevOps</Typography>
                        <Chip label="Unconnected" size="small" variant="outlined" />
                      </Stack>
                      <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
                        <Typography>Jira</Typography>
                        <Chip label="Unconnected" size="small" variant="outlined" />
                      </Stack>
                      <Typography variant="body2" color="text.secondary">
                        Paste or import a synthetic story above. There is no connector, sync or send
                        action.
                      </Typography>
                    </Stack>
                  </CardContent>
                </Card>
              </Stack>
            </Box>
          )}
          {tab === 1 && (
            <Stack spacing={3}>
              {!proposal ? (
                <Alert severity="info">
                  Propose fixture steps from the source tab to begin review.
                </Alert>
              ) : (
                <>
                  <Box
                    sx={{
                      display: 'grid',
                      gridTemplateColumns: { md: 'minmax(0, 2fr) minmax(280px, 1fr)' },
                      gap: 3,
                    }}
                  >
                    <Stack spacing={2}>
                      <Box>
                        <Typography variant="h2">Review the execution contract</Typography>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>
                          {proposal.steps.length} typed steps · 2 supported criteria · human
                          approval required
                        </Typography>
                      </Box>
                      {proposal.steps.map((step, index) => (
                        <Card key={step.id}>
                          <CardContent>
                            <Stack spacing={1.5}>
                              <Stack
                                direction="row"
                                sx={{ justifyContent: 'space-between', gap: 1 }}
                              >
                                <Typography sx={{ fontWeight: 650 }}>
                                  {index + 1}. {step.title}
                                </Typography>
                                <Chip label={step.criterionKeys.join(', ')} size="small" />
                              </Stack>
                              <Typography variant="caption" color="text.secondary">
                                {step.action.type} · {step.action.object}
                              </Typography>
                              {step.action.type === 'ASSERT_FIELD' ? (
                                <TextField
                                  size="small"
                                  label={`Expected ${step.action.field} (${step.id})`}
                                  value={step.action.expected}
                                  onChange={(event) => editExpected(step.id, event.target.value)}
                                  helperText="Reviewer-editable. Any change requires a new approval."
                                />
                              ) : (
                                <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                                  {step.action.type === 'ASSERT_RECORD_COUNT'
                                    ? `Expected record count: ${step.action.expected}`
                                    : Object.entries(step.action.values)
                                        .map(([key, value]) => `${key}: ${value}`)
                                        .join(' · ')}
                                </Typography>
                              )}
                            </Stack>
                          </CardContent>
                        </Card>
                      ))}
                    </Stack>
                    <Stack spacing={3}>
                      <Card>
                        <CardContent>
                          <Stack spacing={2}>
                            <Typography variant="h2">Reusable environment</Typography>
                            <Typography variant="body2" color="text.secondary">
                              Simulation only. Variables stay in memory across stories; the
                              credential reference is a name and is never resolved.
                            </Typography>
                            <TextField
                              select
                              label="Simulation environment"
                              value={environment.id}
                              onChange={(event) =>
                                updateContext(
                                  { ...environment, id: event.target.value as Environment['id'] },
                                  clarification,
                                )
                              }
                            >
                              <MenuItem value="SIM-QA">Synthetic QA</MenuItem>
                              <MenuItem value="SIM-UAT">Synthetic UAT</MenuItem>
                            </TextField>
                            <TextField
                              select
                              label="Execution role"
                              value={environment.role}
                              onChange={(event) =>
                                updateContext(
                                  {
                                    ...environment,
                                    role: event.target.value as Environment['role'],
                                  },
                                  clarification,
                                )
                              }
                            >
                              <MenuItem value="QA_EDITOR">QA editor · synthetic writes</MenuItem>
                              <MenuItem value="READ_ONLY">Read only · writes denied</MenuItem>
                            </TextField>
                            <TextField
                              label="Synthetic record name / subject"
                              value={environment.variables.recordName}
                              onChange={(event) =>
                                updateContext(
                                  { ...environment, variables: { recordName: event.target.value } },
                                  clarification,
                                )
                              }
                              helperText="Begin with Synthetic; letters, digits, spaces, dot and hyphen only."
                            />
                            <TextField
                              label="Secret reference name (unresolved)"
                              value="SALESFORCE_QA_CREDENTIAL"
                              slotProps={{ input: { readOnly: true } }}
                            />
                            <Typography variant="caption" color="text.secondary">
                              Fixture {proposal.fixtureVersion}
                              <br />
                              Runner {proposal.runnerVersion}
                            </Typography>
                          </Stack>
                        </CardContent>
                      </Card>
                      <Card>
                        <CardContent>
                          <Stack spacing={2}>
                            <Typography variant="h2">Human approval</Typography>
                            <Typography variant="body2" color="text.secondary">
                              Approval binds the source, criterion mappings, steps, data,
                              assertions, clarification, role, environment, variable values,
                              reference names and runner version.
                            </Typography>
                            {issues.length > 0 && (
                              <Alert severity="warning">{issues.join(' ')}</Alert>
                            )}
                            <FormControlLabel
                              control={
                                <Checkbox
                                  checked={reviewed}
                                  onChange={(event) => setReviewed(event.target.checked)}
                                />
                              }
                              label="I reviewed the source, actions and expected results."
                            />
                            <Button
                              variant="contained"
                              disabled={!reviewed || issues.length > 0}
                              onClick={approve}
                            >
                              Approve execution contract
                            </Button>
                          </Stack>
                        </CardContent>
                      </Card>
                    </Stack>
                  </Box>
                </>
              )}
            </Stack>
          )}
          {tab === 2 && (
            <Stack spacing={3}>
              <Card>
                <CardContent>
                  <Stack spacing={2}>
                    <Stack
                      direction={{ xs: 'column', sm: 'row' }}
                      sx={{ justifyContent: 'space-between', gap: 2 }}
                    >
                      <Box>
                        <Typography variant="h2">Run an approved simulation</Typography>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>
                          {approved
                            ? `Approved ${approval?.fingerprint} · ${environment.id} · ${environment.role}`
                            : 'A current approved contract is required.'}
                        </Typography>
                      </Box>
                      <Chip
                        label={approved ? 'Approved' : 'Needs review'}
                        color={approved ? 'success' : 'default'}
                      />
                    </Stack>
                    <Typography variant="body2">
                      Each run starts with a fresh synthetic fixture. No live record is created. An
                      interrupted write is never blindly retried.
                    </Typography>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                      <TextField
                        select
                        size="small"
                        label="Simulation exercise"
                        value={fault}
                        onChange={(event) => setFault(event.target.value as typeof fault)}
                        sx={{ minWidth: 250 }}
                      >
                        <MenuItem value="normal">Normal execution</MenuItem>
                        <MenuItem value="duplicate">Duplicate action delivery</MenuItem>
                        <MenuItem value="interrupt">Interrupt write acknowledgement</MenuItem>
                      </TextField>
                      <Button
                        variant="contained"
                        startIcon={<PlayArrowRoundedIcon />}
                        disabled={!approved}
                        onClick={execute}
                      >
                        {history.length ? 'Start new isolated run' : 'Run approved simulation'}
                      </Button>
                      <Button onClick={() => setTab(1)}>Return to review</Button>
                    </Stack>
                  </Stack>
                </CardContent>
              </Card>
              {run && (
                <Card>
                  <CardContent>
                    <Stack spacing={2.5}>
                      <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        sx={{ justifyContent: 'space-between', gap: 2 }}
                      >
                        <Box>
                          <Typography variant="h2">Execution evidence</Typography>
                          <Typography variant="caption" sx={{ overflowWrap: 'anywhere' }}>
                            Run {run.id}
                          </Typography>
                        </Box>
                        <Button variant="outlined" onClick={download}>
                          Download run JSON
                        </Button>
                      </Stack>
                      <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
                        <Chip label={`Lifecycle: ${run.lifecycle}`} variant="outlined" />
                        <Chip
                          label={`Outcome: ${run.outcome}`}
                          color={
                            run.outcome === 'PASS'
                              ? 'success'
                              : run.outcome === 'FAIL'
                                ? 'error'
                                : 'warning'
                          }
                        />
                        <Chip label={`Evidence: ${run.evidence}`} variant="outlined" />
                      </Stack>
                      {run.outcome === 'INDETERMINATE' && (
                        <Alert severity="warning">
                          Write acknowledgement was lost. This run is closed with partial evidence.
                          Do not retry that write; a new isolated simulation resets the fixture.
                        </Alert>
                      )}
                      <Typography variant="body2" color="text.secondary">
                        Approval {run.approvalFingerprint} · {run.runnerVersion}
                        <br />
                        Started {run.startedAt} · Finished {run.completedAt}
                      </Typography>
                      <Accordion variant="outlined">
                        <AccordionSummary expandIcon={<ExpandMoreRoundedIcon />}>
                          Approved source and contract
                        </AccordionSummary>
                        <AccordionDetails>
                          <Typography
                            component="pre"
                            variant="body2"
                            sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}
                          >
                            {safeEvidenceJson(run.approvedContract)}
                          </Typography>
                        </AccordionDetails>
                      </Accordion>
                      <Divider />
                      {run.steps.map((step) => (
                        <Accordion key={step.stepId} disableGutters variant="outlined">
                          <AccordionSummary expandIcon={<ExpandMoreRoundedIcon />}>
                            <Box>
                              <Typography sx={{ fontWeight: 600 }}>
                                {step.sequence}. {step.title}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                {step.criterionKeys.join(', ')} ·{' '}
                                {step.outcome === 'NOT_EVALUATED' && !step.failureCode
                                  ? 'Action recorded · no assertion'
                                  : step.outcome}{' '}
                                {step.failureCode ? `· ${step.failureCode}` : ''}
                              </Typography>
                            </Box>
                          </AccordionSummary>
                          <AccordionDetails>
                            <Stack spacing={1}>
                              <Typography>{step.observation}</Typography>
                              <Typography variant="caption">{step.startedAt}</Typography>
                              <Typography
                                component="pre"
                                variant="body2"
                                sx={{
                                  whiteSpace: 'pre-wrap',
                                  overflowWrap: 'anywhere',
                                  bgcolor: 'background.default',
                                  p: 2,
                                  borderRadius: 1,
                                }}
                              >
                                {safeEvidenceJson({
                                  action: step.action,
                                  before: step.before,
                                  after: step.after,
                                })}
                              </Typography>
                            </Stack>
                          </AccordionDetails>
                        </Accordion>
                      ))}
                    </Stack>
                  </CardContent>
                </Card>
              )}
              <Card>
                <CardContent>
                  <Stack spacing={2}>
                    <Typography variant="h2">Run history</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Last 20 runs in this page session. Refreshing clears history; download
                      evidence you want to keep. Durable shared records are planned.
                    </Typography>
                    {history.length === 0 ? (
                      <Typography>No simulation runs yet.</Typography>
                    ) : (
                      history.map((item, index) => (
                        <Button
                          key={item.id}
                          variant={run?.id === item.id ? 'outlined' : 'text'}
                          onClick={() => setSelectedRun(item.id)}
                          sx={{
                            justifyContent: 'flex-start',
                            textAlign: 'left',
                            overflowWrap: 'anywhere',
                          }}
                        >
                          Run {history.length - index} · {item.outcome} · {item.environment} ·{' '}
                          {item.id.slice(0, 8)}
                        </Button>
                      ))
                    )}
                  </Stack>
                </CardContent>
              </Card>
            </Stack>
          )}
          <Typography component="footer" variant="caption" color="text.secondary">
            Implemented: manual generation and review workspace. Simulated here: Salesforce fixture
            execution controls. Planned: authorized sandbox connector, broader actions and durable
            execution records.
          </Typography>
        </Stack>
      </Container>
    </Box>
  );
}
