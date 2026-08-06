import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { http, HttpResponse } from 'msw';
import { vi } from 'vitest';
import { App } from './App';
import { appRoutes } from './routes/routes';
import { server } from './test/server';

const user = {
  id: '10000000-0000-0000-0000-000000000001',
  email: 'demo@testforge.local',
  displayName: 'Maya Chen',
  role: 'USER',
  createdAt: '2026-07-30T12:00:00Z',
};
const project = {
  id: '20000000-0000-0000-0000-000000000001',
  workspaceId: user.id,
  name: 'Customer Returns Portal',
  description: 'Retail returns quality coverage.',
  status: 'ACTIVE',
  userStoryCount: 1,
  requirementCount: 1,
  createdAt: '2026-07-30T12:00:00Z',
  updatedAt: '2026-07-30T12:00:00Z',
  version: 0,
};
const requirement = {
  id: '30000000-0000-0000-0000-000000000001',
  workItemNumber: 1000,
  projectId: project.id,
  title: 'Submit an eligible product return',
  userStory: 'As a signed-in customer, I want to return an eligible item.',
  businessRequirements: 'Returns are accepted within 30 days and duplicates are blocked.',
  assumptions: 'A synthetic delivered order exists.',
  sourceReference: 'DEMO-RET-101',
  status: 'NEEDS_CLARIFICATION',
  priority: 'MEDIUM',
  acceptanceCriteriaCount: 1,
  acceptanceCriteria: [
    {
      id: '40000000-0000-0000-0000-000000000001',
      criterionKey: 'AC-1',
      description: 'One return request is created for an eligible item.',
      sortOrder: 0,
      createdAt: '2026-07-30T12:00:00Z',
      updatedAt: '2026-07-30T12:00:00Z',
    },
  ],
  ambiguities: [
    {
      id: '50000000-0000-0000-0000-000000000001',
      category: 'MISSING_PERMISSION_RULE',
      severity: 'MEDIUM',
      description: 'The exact support-agent permissions are not specified.',
      suggestedQuestion: 'May a support agent submit a return for the customer?',
      resolved: false,
      resolution: null,
      createdAt: '2026-07-30T12:00:00Z',
      resolvedAt: null,
      version: 0,
    },
  ],
  createdAt: '2026-07-30T12:00:00Z',
  updatedAt: '2026-07-30T12:00:00Z',
  version: 1,
};
const testCase = {
  id: '60000000-0000-0000-0000-000000000001',
  workItemNumber: 1001,
  requirementId: requirement.id,
  generationRunId: '70000000-0000-0000-0000-000000000001',
  testCaseKey: 'TC-1001',
  title: 'Create one eligible return request',
  objective: 'Verify that the order owner can submit an eligible return.',
  category: 'HAPPY_PATH',
  priority: 'HIGH',
  riskLevel: 'HIGH',
  automationCandidate: true,
  status: 'GENERATED',
  coverageIntent: 'ACCEPTANCE_CRITERIA',
  rationale: 'Provides direct evidence for AC-1.',
  finalExpectedOutcome: 'One return request is linked to the order item.',
  preconditions: [{ sortOrder: 0, description: 'A synthetic eligible order exists.' }],
  steps: [
    {
      stepNumber: 1,
      action: 'Submit the return form with synthetic valid data.',
      expectedResult: 'The request is accepted once.',
      testDataReference: 'validReturn',
    },
  ],
  testData: [
    {
      name: 'validReturn',
      description: 'Synthetic valid return data.',
      exampleValue: 'TF-DEMO-001',
      sensitivity: 'PUBLIC',
      generationStrategy: 'Create a unique demo identifier.',
    },
  ],
  acceptanceCriteriaKeys: ['AC-1'],
  reviews: [],
  createdAt: '2026-07-30T12:00:00Z',
  updatedAt: '2026-07-30T12:00:00Z',
  version: 0,
};

/** Returns authenticated API handlers shared by user-story workflow component tests. */
function authenticatedHandlers() {
  return [
    http.post('/api/v1/auth/refresh', () =>
      HttpResponse.json({ accessToken: 'workflow-token', expiresInSeconds: 600, user }),
    ),
    http.get(`/api/v1/projects/${project.id}`, () => HttpResponse.json(project)),
    http.get(`/api/v1/user-stories/${requirement.id}/generation-runs/page`, () =>
      HttpResponse.json({
        items: [
          {
            id: testCase.generationRunId,
            requirementId: requirement.id,
            provider: 'requirement-rules',
            model: 'testforge-rules-v2',
            promptVersion: 'manual-test-v1',
            status: 'COMPLETED',
            generatedCaseCount: 1,
            failureCode: null,
            failureMessage: null,
            startedAt: '2026-07-30T12:00:00Z',
            completedAt: '2026-07-30T12:00:01Z',
            setNumber: 1,
            setState: 'ACTIVE',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
        activeGenerationRunId: testCase.generationRunId,
      }),
    ),
  ];
}

/** Mounts the complete application at a workflow route with an in-memory router. */
function renderRoute(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return { ...render(<App router={router} />), router };
}

describe('project and user-story workflow', () => {
  it('shows project user stories, audit controls, and the structured story form', async () => {
    const auditRequests: URLSearchParams[] = [];
    server.use(
      ...authenticatedHandlers(),
      http.get(`/api/v1/projects/${project.id}/user-stories`, () =>
        HttpResponse.json({
          items: [
            {
              id: requirement.id,
              workItemNumber: requirement.workItemNumber,
              projectId: project.id,
              title: requirement.title,
              status: requirement.status,
              priority: requirement.priority,
              acceptanceCriteriaCount: 1,
              updatedAt: requirement.updatedAt,
              version: 1,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/projects/${project.id}/audit-events`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        auditRequests.push(params);
        const page = Number(params.get('page') ?? 0);
        return HttpResponse.json({
          items: [
            {
              id: `audit-${page}`,
              actorId: user.id,
              projectId: project.id,
              entityType: page === 0 ? 'REQUIREMENT' : 'PROJECT',
              entityId: page === 0 ? requirement.id : project.id,
              action: 'CREATED',
              metadata: {},
              timestamp: '2026-07-30T12:04:00Z',
              correlationId: `audit-correlation-${page}`,
            },
          ],
          page,
          size: 20,
          totalElements: 21,
          totalPages: 2,
          hasNext: page === 0,
          activeGenerationRunId: null,
        });
      }),
    );
    const actor = userEvent.setup();
    const { router } = renderRoute(`/projects/${project.id}`);

    expect(await screen.findByRole('heading', { level: 1, name: project.name })).toBeVisible();
    expect(await screen.findByRole('heading', { level: 3, name: requirement.title })).toBeVisible();
    expect(await screen.findByText('User story — Created')).toBeVisible();
    await actor.click(screen.getByRole('combobox', { name: 'Event type' }));
    await actor.click(screen.getByRole('option', { name: 'User story' }));
    await actor.type(screen.getByRole('textbox', { name: 'Entity ID' }), requirement.id);
    await actor.type(screen.getByRole('textbox', { name: 'Actor ID' }), user.id);
    await actor.type(screen.getByRole('textbox', { name: 'Action' }), 'CREATED');
    await actor.click(screen.getByRole('button', { name: 'Apply filters' }));
    await waitFor(() => expect(auditRequests.at(-1)?.get('entityType')).toBe('REQUIREMENT'));
    expect(auditRequests.at(-1)?.get('entityId')).toBe(requirement.id);
    expect(auditRequests.at(-1)?.get('actorId')).toBe(user.id);
    expect(auditRequests.at(-1)?.get('action')).toBe('CREATED');
    await actor.click(screen.getByRole('button', { name: 'Next page' }));
    expect(await screen.findByText('Project — Created')).toBeVisible();
    expect(auditRequests.at(-1)?.get('page')).toBe('1');
    await actor.click(screen.getByRole('button', { name: 'Clear filters' }));
    await waitFor(() => {
      expect(router.state.location.search).toBe('');
      expect(screen.getByText('User story — Created')).toBeVisible();
    });
    await actor.click(screen.getByRole('button', { name: 'New user story' }));
    expect(screen.getByRole('heading', { name: 'Add a user story' })).toBeVisible();
    expect(screen.getByRole('textbox', { name: 'User story statement' })).toBeEnabled();
    expect(screen.getByRole('textbox', { name: 'Requirements and constraints' })).toBeEnabled();
    expect(screen.getByLabelText('AC-1')).toBeEnabled();
    await actor.click(screen.getByRole('button', { name: 'Add criterion' }));
    expect(screen.getByLabelText('AC-2')).toBeEnabled();
    await actor.click(screen.getByRole('button', { name: 'Move acceptance criterion 2 up' }));
    await actor.click(screen.getByRole('button', { name: 'Move acceptance criterion 1 down' }));
    await actor.click(screen.getByRole('button', { name: 'Remove acceptance criterion 2' }));
    expect(screen.queryByLabelText('AC-2')).not.toBeInTheDocument();
  }, 20_000);

  it('orders test cases naturally by default and supports sorting and filtering', async () => {
    const unorderedCases = [
      {
        ...testCase,
        id: '60000000-0000-0000-0000-000000000010',
        workItemNumber: 1010,
        testCaseKey: 'TC-1010',
        title: 'Audit restricted return access',
        category: 'SECURITY',
        priority: 'LOW',
        status: 'APPROVED',
        updatedAt: '2026-07-30T12:03:00Z',
      },
      {
        ...testCase,
        id: '60000000-0000-0000-0000-000000000002',
        workItemNumber: 1002,
        testCaseKey: 'TC-1002',
        title: 'Reject a return outside the boundary',
        objective: 'Verify the return-window boundary is enforced.',
        category: 'BOUNDARY',
        priority: 'CRITICAL',
        status: 'NEEDS_REVISION',
        updatedAt: '2026-07-30T12:02:00Z',
      },
      testCase,
    ];
    server.use(
      ...authenticatedHandlers(),
      http.get(`/api/v1/user-stories/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/user-stories/${requirement.id}/test-cases/page`, () =>
        HttpResponse.json({
          items: unorderedCases,
          page: 0,
          size: 20,
          totalElements: unorderedCases.length,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 1,
          approvedCriteria: 1,
          coveragePercent: 100,
          approvedCoveragePercent: 100,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/traceability`, () =>
        HttpResponse.json({ requirementId: requirement.id, rows: [] }),
      ),
    );
    const actor = userEvent.setup();
    renderRoute(`/requirements/${requirement.id}`);

    await actor.click(await screen.findByRole('tab', { name: 'Test cases (3)' }));
    await screen.findByText('TC-1010');
    expect(screen.getAllByText(/^TC-\d+$/).map((item) => item.textContent)).toEqual([
      'TC-1001',
      'TC-1002',
      'TC-1010',
    ]);
    expect(screen.getByText('Showing 3 of 3 test cases')).toBeVisible();

    await actor.type(screen.getByRole('textbox', { name: 'Search test cases' }), 'boundary');
    expect(screen.getByText('Showing 1 of 3 test cases')).toBeVisible();
    expect(screen.getByText('TC-1002')).toBeVisible();
    expect(screen.queryByText('TC-1001')).not.toBeInTheDocument();

    await actor.click(screen.getByRole('button', { name: 'Clear filters' }));
    await actor.click(screen.getByRole('combobox', { name: 'Status' }));
    await actor.click(screen.getByRole('option', { name: 'APPROVED' }));
    expect(screen.getByText('Showing 1 of 3 test cases')).toBeVisible();
    expect(screen.getByText('TC-1010')).toBeVisible();

    await actor.click(screen.getByRole('button', { name: 'Clear filters' }));
    await actor.click(screen.getByRole('combobox', { name: 'Sort by' }));
    await actor.click(screen.getByRole('option', { name: 'Priority (highest first)' }));
    expect(screen.getAllByText(/^TC-\d+$/).map((item) => item.textContent)).toEqual([
      'TC-1002',
      'TC-1001',
      'TC-1010',
    ]);

    await actor.click(screen.getByRole('combobox', { name: 'Category' }));
    await actor.click(screen.getByRole('option', { name: 'BOUNDARY' }));
    expect(screen.getByText('Showing 1 of 3 test cases')).toBeVisible();
    expect(screen.getByText('TC-1002')).toBeVisible();

    await actor.click(screen.getByRole('button', { name: 'Clear filters' }));
    await actor.click(screen.getByRole('combobox', { name: 'Priority' }));
    await actor.click(screen.getByRole('option', { name: 'CRITICAL' }));
    expect(screen.getByText('Showing 1 of 3 test cases')).toBeVisible();
    expect(screen.getByText('TC-1002')).toBeVisible();

    await actor.click(screen.getByRole('button', { name: 'Clear filters' }));
    await actor.click(screen.getByRole('combobox', { name: 'Sort by' }));
    await actor.click(screen.getByRole('option', { name: 'Recently updated' }));
    expect(screen.getAllByText(/^TC-\d+$/).map((item) => item.textContent)).toEqual([
      'TC-1010',
      'TC-1002',
      'TC-1001',
    ]);

    await actor.click(screen.getByRole('combobox', { name: 'Sort by' }));
    await actor.click(screen.getByRole('option', { name: 'Status' }));
    expect(screen.getAllByText(/^TC-\d+$/).map((item) => item.textContent)).toEqual([
      'TC-1010',
      'TC-1001',
      'TC-1002',
    ]);

    await actor.click(screen.getByRole('combobox', { name: 'Sort by' }));
    await actor.click(screen.getByRole('option', { name: 'Test case number (descending)' }));
    expect(screen.getAllByText(/^TC-\d+$/).map((item) => item.textContent)).toEqual([
      'TC-1010',
      'TC-1002',
      'TC-1001',
    ]);

    await actor.type(screen.getByRole('textbox', { name: 'Search test cases' }), 'no match');
    expect(screen.getByText('No test cases match the current filters.')).toBeVisible();
  }, 20_000);

  it('keeps every generation outcome and its paging reachable when no cases exist', async () => {
    const runRequests: number[] = [];
    /** Builds one complete generation-attempt response for history-state coverage. */
    const run = (
      id: string,
      status: 'PENDING' | 'FAILED' | 'REJECTED_BY_VALIDATION' | 'COMPLETED',
      failureMessage: string | null,
      setNumber = 0,
      setState: 'ACTIVE' | 'SUPERSEDED' | null = null,
    ) => ({
      id,
      requirementId: requirement.id,
      provider: 'requirement-rules',
      model: 'testforge-rules-v2',
      promptVersion: 'manual-test-v1',
      providerAdapterVersion: 'requirement-rules-adapter-v1',
      resultContractVersion: 'manual-test-result-v1',
      schemaVersion: 'manual-test-schema-v2',
      validatorVersion: 'manual-test-validator-v2',
      sourceRequirementVersion: 1,
      sourceSnapshotProvenance: 'EXACT',
      status,
      generatedCaseCount: status === 'COMPLETED' ? 1 : 0,
      failureCode: failureMessage ? 'safe_failure' : null,
      failureMessage,
      startedAt: '2026-08-05T12:00:00Z',
      completedAt: status === 'PENDING' ? null : '2026-08-05T12:00:01Z',
      setNumber,
      setState,
    });
    server.use(
      http.get(`/api/v1/user-stories/${requirement.id}/generation-runs/page`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        runRequests.push(page);
        return HttpResponse.json({
          items:
            page === 0
              ? [
                  run('run-pending', 'PENDING', null),
                  run('run-failed', 'FAILED', 'Provider request failed safely.'),
                  run(
                    'run-rejected',
                    'REJECTED_BY_VALIDATION',
                    'Generated output failed validation.',
                  ),
                ]
              : [run('run-completed', 'COMPLETED', null, 2, 'SUPERSEDED')],
          page,
          size: 20,
          totalElements: 21,
          totalPages: 2,
          hasNext: page === 0,
        });
      }),
      ...authenticatedHandlers(),
      http.get(`/api/v1/user-stories/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/user-stories/${requirement.id}/test-cases/page`, () =>
        HttpResponse.json({
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 0,
          approvedCriteria: 0,
          coveragePercent: 0,
          approvedCoveragePercent: 0,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/traceability`, () =>
        HttpResponse.json({ requirementId: requirement.id, rows: [] }),
      ),
    );
    const actor = userEvent.setup();
    renderRoute(`/requirements/${requirement.id}`);

    await actor.click(await screen.findByRole('tab', { name: 'Test cases (0)' }));
    expect(await screen.findByRole('heading', { name: 'Generation history' })).toBeVisible();
    expect(screen.getByText('PENDING')).toBeVisible();
    expect(screen.getByText('FAILED')).toBeVisible();
    expect(screen.getByText('REJECTED BY VALIDATION')).toBeVisible();
    expect(screen.getByText('Provider request failed safely.')).toBeVisible();
    expect(screen.getByText('Generated output failed validation.')).toBeVisible();
    expect(screen.getByText('No test cases yet')).toBeVisible();
    expect(screen.getByText('Page 1 of 2 • 21 items')).toBeVisible();

    await actor.click(screen.getByRole('button', { name: 'Next page' }));
    await waitFor(() => expect(runRequests.at(-1)).toBe(1));
    expect(await screen.findByText('Set 2')).toBeVisible();
    expect(screen.getByRole('button', { name: 'View set' })).toBeEnabled();
  });

  it('renders typed notice severity for every generation outcome', async () => {
    const outcomes = [
      {
        status: 'PENDING',
        failureMessage: null,
        text: 'Generation is still pending.',
        severityClass: 'MuiAlert-colorInfo',
      },
      {
        status: 'FAILED',
        failureMessage: 'Provider request failed safely.',
        text: 'Provider request failed safely.',
        severityClass: 'MuiAlert-colorError',
      },
      {
        status: 'REJECTED_BY_VALIDATION',
        failureMessage: 'Generated output failed validation.',
        text: 'Generated output failed validation.',
        severityClass: 'MuiAlert-colorWarning',
      },
      {
        status: 'COMPLETED',
        failureMessage: null,
        text: 'Generation completed and passed the server-side quality gate.',
        severityClass: 'MuiAlert-colorSuccess',
      },
    ] as const;
    let outcomeIndex = 0;
    /** Returns the next synthetic generation outcome through either compatible POST route. */
    const nextOutcome = () => {
      const outcome = outcomes[outcomeIndex++]!;
      return HttpResponse.json(
        {
          id: `notice-run-${outcomeIndex}`,
          requirementId: requirement.id,
          provider: 'requirement-rules',
          model: 'testforge-rules-v2',
          promptVersion: 'manual-test-v1',
          providerAdapterVersion: 'application-provider-v1',
          resultContractVersion: 'manual-test-result-v1',
          schemaVersion: 'manual-test-schema-v2',
          validatorVersion: 'manual-test-validator-v2',
          sourceRequirementVersion: 1,
          sourceSnapshotProvenance: 'EXACT',
          status: outcome.status,
          generatedCaseCount: outcome.status === 'COMPLETED' ? 1 : 0,
          failureCode: outcome.failureMessage ? 'safe_failure' : null,
          failureMessage: outcome.failureMessage,
          startedAt: '2026-08-05T12:00:00Z',
          completedAt: outcome.status === 'PENDING' ? null : '2026-08-05T12:00:01Z',
          setNumber: outcome.status === 'COMPLETED' ? 2 : 0,
          setState: outcome.status === 'COMPLETED' ? 'ACTIVE' : null,
        },
        { status: 201 },
      );
    };
    server.use(
      ...authenticatedHandlers(),
      http.get(`/api/v1/user-stories/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/user-stories/${requirement.id}/test-cases/page`, () =>
        HttpResponse.json({
          items: [testCase],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 1,
          approvedCriteria: 0,
          coveragePercent: 100,
          approvedCoveragePercent: 0,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/traceability`, () =>
        HttpResponse.json({ requirementId: requirement.id, rows: [] }),
      ),
      http.post(`/api/v1/user-stories/${requirement.id}/generate-test-cases`, nextOutcome),
      http.post(`/api/v1/user-stories/${requirement.id}/regenerate`, nextOutcome),
    );
    const actor = userEvent.setup();
    renderRoute(`/requirements/${requirement.id}`);

    for (const outcome of outcomes) {
      await actor.click(await screen.findByRole('button', { name: 'Regenerate' }));
      const alert = await screen.findByRole('alert');
      expect(alert).toHaveClass(outcome.severityClass);
      expect(within(alert).getByText(outcome.text, { exact: false })).toBeVisible();
      await actor.click(within(alert).getByRole('button', { name: 'Close' }));
      await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    }
  });

  it('regenerates from stable story state beyond the visible run page with one key', async () => {
    const regenerationRequests: { url: string; idempotencyKey: string | null }[] = [];
    let generateRequests = 0;
    let requestedRunPage = '';
    server.use(
      http.get(`/api/v1/user-stories/${requirement.id}/generation-runs/page`, ({ request }) => {
        requestedRunPage = new URL(request.url).searchParams.get('page') ?? '';
        return HttpResponse.json({
          items: [
            {
              id: 'failed-run-on-page-two',
              requirementId: requirement.id,
              provider: 'requirement-rules',
              model: 'testforge-rules-v2',
              promptVersion: 'manual-test-v1',
              status: 'FAILED',
              generatedCaseCount: 0,
              failureCode: 'provider_unavailable',
              failureMessage: 'Generation is temporarily unavailable.',
              startedAt: '2026-08-05T12:00:00Z',
              completedAt: '2026-08-05T12:00:01Z',
              setNumber: 0,
              setState: null,
            },
          ],
          page: 2,
          size: 20,
          totalElements: 41,
          totalPages: 3,
          hasNext: false,
          activeGenerationRunId: testCase.generationRunId,
        });
      }),
      ...authenticatedHandlers(),
      http.get(`/api/v1/user-stories/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/user-stories/${requirement.id}/test-cases/page`, () =>
        HttpResponse.json({
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 0,
          approvedCriteria: 0,
          coveragePercent: 0,
          approvedCoveragePercent: 0,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/traceability`, () =>
        HttpResponse.json({ requirementId: requirement.id, rows: [] }),
      ),
      http.post(`/api/v1/user-stories/${requirement.id}/generate-test-cases`, () => {
        generateRequests += 1;
        return HttpResponse.error();
      }),
      http.post(`/api/v1/user-stories/${requirement.id}/regenerate`, ({ request }) => {
        const url = new URL(request.url);
        regenerationRequests.push({
          url: `${url.pathname}${url.search}`,
          idempotencyKey: request.headers.get('Idempotency-Key'),
        });
        if (url.searchParams.get('confirmSupersede') !== 'true') {
          return HttpResponse.json(
            {
              status: 409,
              code: 'supersede_confirmation_required',
              detail: 'Confirm superseding the active generation set.',
            },
            { status: 409 },
          );
        }
        return HttpResponse.json(
          {
            id: 'confirmed-regeneration',
            requirementId: requirement.id,
            provider: 'requirement-rules',
            model: 'testforge-rules-v2',
            promptVersion: 'manual-test-v1',
            status: 'COMPLETED',
            generatedCaseCount: 1,
            failureCode: null,
            failureMessage: null,
            startedAt: '2026-08-05T12:00:00Z',
            completedAt: '2026-08-05T12:00:01Z',
            setNumber: 2,
            setState: 'ACTIVE',
          },
          { status: 201 },
        );
      }),
    );
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const actor = userEvent.setup();
    renderRoute(`/user-stories/${requirement.id}?runPage=2`);

    await actor.click(await screen.findByRole('button', { name: 'Regenerate' }));
    await waitFor(() => expect(regenerationRequests).toHaveLength(2));

    expect(requestedRunPage).toBe('2');
    expect(generateRequests).toBe(0);
    expect(regenerationRequests.map((request) => request.url)).toEqual([
      `/api/v1/user-stories/${requirement.id}/regenerate?confirmSupersede=false`,
      `/api/v1/user-stories/${requirement.id}/regenerate?confirmSupersede=true`,
    ]);
    expect(regenerationRequests[0]?.idempotencyKey).toBeTruthy();
    expect(regenerationRequests[1]?.idempotencyKey).toBe(regenerationRequests[0]?.idempotencyKey);
    confirm.mockRestore();
  });

  it('collects required human evidence before reopening or requesting changes', async () => {
    let currentCase = { ...testCase, status: 'APPROVED', version: 4 } as typeof testCase;
    let reopenRequests = 0;
    let changeRequests = 0;
    let reopenPayload: Record<string, unknown> | null = null;
    let changePayload: Record<string, unknown> | null = null;
    server.use(
      ...authenticatedHandlers(),
      http.get(`/api/v1/user-stories/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/user-stories/${requirement.id}/test-cases/page`, () =>
        HttpResponse.json({
          items: [currentCase],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 1,
          approvedCriteria: currentCase.status === 'APPROVED' ? 1 : 0,
          coveragePercent: 100,
          approvedCoveragePercent: currentCase.status === 'APPROVED' ? 100 : 0,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/traceability`, () =>
        HttpResponse.json({ requirementId: requirement.id, rows: [] }),
      ),
      http.post(`/api/v1/test-cases/${testCase.id}/reopen`, async ({ request }) => {
        reopenRequests += 1;
        reopenPayload = (await request.json()) as Record<string, unknown>;
        currentCase = { ...currentCase, status: 'IN_REVIEW', version: 5 };
        return HttpResponse.json(currentCase);
      }),
      http.post(`/api/v1/test-cases/${testCase.id}/request-changes`, async ({ request }) => {
        changeRequests += 1;
        changePayload = (await request.json()) as Record<string, unknown>;
        currentCase = { ...currentCase, status: 'NEEDS_REVISION', version: 6 };
        return HttpResponse.json(currentCase);
      }),
    );
    const actor = userEvent.setup();
    renderRoute(`/user-stories/${requirement.id}`);

    await actor.click(await screen.findByRole('tab', { name: 'Test cases (1)' }));
    await actor.click(await screen.findByText(testCase.title));
    await actor.click(screen.getByRole('button', { name: 'Reopen for review' }));
    const reopenDialog = screen.getByRole('dialog', {
      name: `Reopen for review ${testCase.testCaseKey}`,
    });
    const reopenReason = within(reopenDialog).getByRole('textbox', { name: 'Reopen reason' });
    await actor.type(reopenReason, '  ');
    await actor.click(within(reopenDialog).getByRole('button', { name: 'Reopen test case' }));
    expect(reopenRequests).toBe(0);
    expect(
      within(reopenDialog).getByText('Enter a meaningful reason for reopening this test case.'),
    ).toBeVisible();
    await actor.clear(reopenReason);
    await actor.type(reopenReason, 'New production-like evidence needs human review.');
    await actor.click(within(reopenDialog).getByRole('button', { name: 'Reopen test case' }));
    expect(await screen.findByText('IN REVIEW')).toBeVisible();
    expect(reopenPayload).toEqual({
      reason: 'New production-like evidence needs human review.',
      version: 4,
    });

    await actor.click(screen.getByRole('button', { name: 'Request changes' }));
    const changesDialog = screen.getByRole('dialog', {
      name: `Request changes ${testCase.testCaseKey}`,
    });
    const reviewComment = within(changesDialog).getByRole('textbox', { name: 'Review comment' });
    await actor.type(reviewComment, 'x');
    await actor.click(within(changesDialog).getByRole('button', { name: 'Request changes' }));
    expect(changeRequests).toBe(0);
    expect(
      within(changesDialog).getByText('Enter a meaningful comment for this review decision.'),
    ).toBeVisible();
    await actor.clear(reviewComment);
    await actor.type(reviewComment, 'Clarify the expected retry-state evidence.');
    await actor.click(within(changesDialog).getByRole('button', { name: 'Request changes' }));
    expect(await screen.findByText('NEEDS REVISION')).toBeVisible();
    expect(changePayload).toEqual({
      comments: 'Clarify the expected retry-state evidence.',
      version: 5,
    });
  }, 20_000);

  it('reviews generated evidence, edits its structure, and inspects traceability', async () => {
    let currentCase = testCase;
    let approvalPayload: Record<string, unknown> | null = null;
    const primaryCriterion = requirement.acceptanceCriteria[0]!;
    server.use(
      ...authenticatedHandlers(),
      http.get(`/api/v1/user-stories/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/user-stories/${requirement.id}/test-cases/page`, () =>
        HttpResponse.json({
          items: [currentCase],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 1,
          approvedCriteria: currentCase.status === 'APPROVED' ? 1 : 0,
          coveragePercent: 100,
          approvedCoveragePercent: currentCase.status === 'APPROVED' ? 100 : 0,
        }),
      ),
      http.get(`/api/v1/user-stories/${requirement.id}/traceability`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          rows: [
            {
              acceptanceCriterionId: primaryCriterion.id,
              criterionKey: 'AC-1',
              description: primaryCriterion.description,
              testCases: [
                {
                  id: currentCase.id,
                  testCaseKey: currentCase.testCaseKey,
                  title: currentCase.title,
                  status: currentCase.status,
                  coverageType: 'DIRECT',
                  confidence: 0.95,
                },
              ],
            },
          ],
        }),
      ),
      http.patch(`/api/v1/test-cases/${testCase.id}`, async ({ request }) => {
        const update = (await request.json()) as Record<string, unknown>;
        currentCase = {
          ...currentCase,
          ...update,
          preconditions: (update.preconditions as string[]).map((description, sortOrder) => ({
            sortOrder,
            description,
          })),
          steps: update.steps as typeof testCase.steps,
          testData: update.testData as typeof testCase.testData,
          status: 'IN_REVIEW',
          version: 1,
        } as typeof testCase;
        return HttpResponse.json(currentCase);
      }),
      http.post(`/api/v1/test-cases/${testCase.id}/approve`, async ({ request }) => {
        approvalPayload = (await request.json()) as Record<string, unknown>;
        currentCase = { ...currentCase, status: 'APPROVED', version: 2 };
        return HttpResponse.json(currentCase);
      }),
      http.get(`/api/v1/test-cases/${testCase.id}/revisions`, () =>
        HttpResponse.json({
          items: [
            {
              id: 'revision-1',
              revisionNumber: 1,
              snapshot: { ...testCase, schemaVersion: 1 },
              changedBy: user.id,
              changedAt: '2026-07-30T12:05:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/test-cases/${testCase.id}/reviews`, () =>
        HttpResponse.json({
          items: currentCase.reviews,
          page: 0,
          size: 20,
          totalElements: currentCase.reviews.length,
          totalPages: currentCase.reviews.length ? 1 : 0,
          hasNext: false,
        }),
      ),
      http.get(`/api/v1/projects/${project.id}/audit-events`, () =>
        HttpResponse.json({
          items: [
            {
              id: 'test-case-audit-1',
              actorId: user.id,
              projectId: project.id,
              entityType: 'TEST_CASE',
              entityId: testCase.id,
              action: 'UPDATED',
              metadata: {},
              timestamp: '2026-07-30T12:05:00Z',
              correlationId: 'test-case-history',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.post(`/api/v1/user-stories/${requirement.id}/regenerate`, () =>
        HttpResponse.json(
          {
            id: 'run-2',
            requirementId: requirement.id,
            provider: 'requirement-rules',
            model: 'testforge-rules-v2',
            promptVersion: 'manual-test-v1',
            status: 'COMPLETED',
            generatedCaseCount: 1,
            failureCode: null,
            failureMessage: null,
          },
          { status: 201 },
        ),
      ),
    );
    const actor = userEvent.setup();
    renderRoute(`/requirements/${requirement.id}`);

    expect(await screen.findByRole('heading', { level: 1, name: requirement.title })).toBeVisible();
    expect(await screen.findByText('100%')).toBeVisible();
    expect(screen.getByRole('tablist', { name: 'User Story workspace sections' })).toBeVisible();
    expect(screen.getByRole('tab', { name: 'Story details' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Requirements and constraints' })).toBeVisible();
    await actor.click(screen.getByRole('tab', { name: 'Test cases (1)' }));
    await actor.click(await screen.findByText(testCase.title));
    expect(await screen.findByText(testCase.finalExpectedOutcome, { exact: false })).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'Edit' }));
    const dialog = screen.getByRole('dialog', { name: `Edit ${testCase.testCaseKey}` });
    const title = within(dialog).getByRole('textbox', { name: /Title/ });
    await actor.clear(title);
    await actor.type(title, 'Create exactly one eligible return');
    await actor.click(
      within(dialog).getByRole('checkbox', {
        name: 'Suitable candidate for future automation analysis',
      }),
    );
    await actor.click(within(dialog).getByRole('button', { name: 'Add precondition' }));
    await waitFor(() => expect(within(dialog).getAllByLabelText(/^Precondition/)).toHaveLength(2));
    await actor.type(
      within(dialog).getAllByLabelText(/^Precondition/)[1]!,
      'A second setup condition.',
    );
    await actor.click(within(dialog).getByRole('button', { name: 'Move precondition 2 up' }));
    await actor.click(within(dialog).getByRole('button', { name: 'Remove precondition 1' }));
    await actor.click(within(dialog).getByRole('button', { name: 'Add step' }));
    await waitFor(() => expect(within(dialog).getAllByLabelText(/^Action/)).toHaveLength(2));
    await actor.type(within(dialog).getAllByLabelText(/^Action/)[1]!, 'Inspect the saved return.');
    await actor.type(
      within(dialog).getAllByLabelText(/^Expected result/)[1]!,
      'The return is linked once.',
    );
    await actor.click(within(dialog).getByRole('button', { name: 'Move step 2 up' }));
    await actor.click(within(dialog).getByRole('button', { name: 'Remove step 1' }));
    await actor.click(within(dialog).getByRole('button', { name: 'Add test data' }));
    await actor.click(within(dialog).getByRole('button', { name: 'Remove test data item 2' }));
    await actor.click(within(dialog).getByRole('button', { name: 'Save changes' }));
    expect(await screen.findByText('Create exactly one eligible return')).toBeVisible();

    await actor.click(screen.getByRole('button', { name: 'Show history' }));
    expect(await screen.findByText('Revision timeline')).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'Compare revision 1' }));
    const comparison = screen.getByRole('table', { name: 'Revision comparison' });
    expect(within(comparison).getByText('Create one eligible return request')).toBeVisible();
    expect(within(comparison).getByText('Create exactly one eligible return')).toBeVisible();
    expect(screen.getByText(/UPDATED/)).toBeVisible();

    await actor.click(screen.getByRole('button', { name: 'Edit' }));
    const dirtyDialog = screen.getByRole('dialog', { name: `Edit ${testCase.testCaseKey}` });
    await actor.type(within(dirtyDialog).getByRole('textbox', { name: /Title/ }), ' updated');
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    await actor.click(within(dirtyDialog).getByRole('button', { name: 'Cancel' }));
    expect(confirm).toHaveBeenCalledWith('Discard the unsaved test-case changes?');
    confirm.mockRestore();

    await actor.click(screen.getByRole('button', { name: 'Approve' }));
    const approvalDialog = screen.getByRole('dialog', { name: `Approve ${testCase.testCaseKey}` });
    expect(
      within(approvalDialog).getByRole('textbox', { name: 'Approval comment (optional)' }),
    ).toBeEnabled();
    await actor.click(within(approvalDialog).getByRole('button', { name: 'Confirm approval' }));
    expect((await screen.findAllByText('APPROVED')).length).toBeGreaterThan(0);
    expect(await screen.findByText('1 criterion has approved evidence')).toBeVisible();
    expect(approvalPayload).toEqual({ comments: '', version: 1 });
    await actor.click(screen.getByRole('tab', { name: 'Traceability' }));
    expect(screen.getByText('AC-1')).toBeVisible();
    await actor.click(screen.getByRole('tab', { name: 'Ambiguities (1)' }));
    expect(screen.getByText('MISSING PERMISSION RULE')).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'Regenerate' }));
    expect(await screen.findByText(/1 manual test case was created from this story/)).toBeVisible();
  }, 30_000);
});
