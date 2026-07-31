import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router-dom';
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
  name: 'Customer Returns Portal',
  description: 'Retail returns quality coverage.',
  status: 'ACTIVE',
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

function authenticatedHandlers() {
  return [
    http.post('/api/v1/auth/refresh', () =>
      HttpResponse.json({ accessToken: 'workflow-token', expiresInSeconds: 600, user }),
    ),
    http.get(`/api/v1/projects/${project.id}`, () => HttpResponse.json(project)),
  ];
}

function renderRoute(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return render(<App router={router} />);
}

describe('project and requirement workflow', () => {
  it('shows project requirements and opens the structured requirement form', async () => {
    server.use(
      ...authenticatedHandlers(),
      http.get(`/api/v1/projects/${project.id}/requirements`, () =>
        HttpResponse.json({
          items: [
            {
              id: requirement.id,
              workItemNumber: requirement.workItemNumber,
              projectId: project.id,
              title: requirement.title,
              status: requirement.status,
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
    );
    const actor = userEvent.setup();
    renderRoute(`/projects/${project.id}`);

    expect(await screen.findByRole('heading', { level: 1, name: project.name })).toBeVisible();
    expect(await screen.findByRole('heading', { level: 3, name: requirement.title })).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'New requirement' }));
    expect(screen.getByRole('heading', { name: 'Add a requirement' })).toBeVisible();
    expect(screen.getByLabelText('AC-1')).toBeEnabled();
    await actor.click(screen.getByRole('button', { name: 'Add criterion' }));
    expect(screen.getByLabelText('AC-2')).toBeEnabled();
    await actor.click(screen.getByRole('button', { name: 'Move acceptance criterion 2 up' }));
    await actor.click(screen.getByRole('button', { name: 'Move acceptance criterion 1 down' }));
    await actor.click(screen.getByRole('button', { name: 'Remove acceptance criterion 2' }));
    expect(screen.queryByLabelText('AC-2')).not.toBeInTheDocument();
  });

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
      http.get(`/api/v1/requirements/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/requirements/${requirement.id}/test-cases`, () =>
        HttpResponse.json(unorderedCases),
      ),
      http.get(`/api/v1/requirements/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 1,
          approvedCriteria: 1,
          coveragePercent: 100,
          approvedCoveragePercent: 100,
        }),
      ),
      http.get(`/api/v1/requirements/${requirement.id}/traceability`, () =>
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
  });

  it('reviews generated evidence, edits its structure, and inspects traceability', async () => {
    let currentCase = testCase;
    const primaryCriterion = requirement.acceptanceCriteria[0]!;
    server.use(
      ...authenticatedHandlers(),
      http.get(`/api/v1/requirements/${requirement.id}`, () => HttpResponse.json(requirement)),
      http.get(`/api/v1/requirements/${requirement.id}/test-cases`, () =>
        HttpResponse.json([currentCase]),
      ),
      http.get(`/api/v1/requirements/${requirement.id}/coverage`, () =>
        HttpResponse.json({
          requirementId: requirement.id,
          totalCriteria: 1,
          coveredCriteria: 1,
          approvedCriteria: currentCase.status === 'APPROVED' ? 1 : 0,
          coveragePercent: 100,
          approvedCoveragePercent: currentCase.status === 'APPROVED' ? 100 : 0,
        }),
      ),
      http.get(`/api/v1/requirements/${requirement.id}/traceability`, () =>
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
      http.post(`/api/v1/test-cases/${testCase.id}/approve`, () => {
        currentCase = { ...currentCase, status: 'APPROVED' };
        return HttpResponse.json(currentCase);
      }),
      http.post(`/api/v1/requirements/${requirement.id}/regenerate`, () =>
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

    await actor.click(screen.getByRole('button', { name: 'Edit' }));
    const dirtyDialog = screen.getByRole('dialog', { name: `Edit ${testCase.testCaseKey}` });
    await actor.type(within(dirtyDialog).getByRole('textbox', { name: /Title/ }), ' updated');
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    await actor.click(within(dirtyDialog).getByRole('button', { name: 'Cancel' }));
    expect(confirm).toHaveBeenCalledWith('Discard the unsaved test-case changes?');
    confirm.mockRestore();

    await actor.click(screen.getByRole('button', { name: 'Approve' }));
    expect(await screen.findByText('APPROVED')).toBeVisible();
    await actor.click(screen.getByRole('tab', { name: 'Traceability' }));
    expect(screen.getByText('AC-1')).toBeVisible();
    await actor.click(screen.getByRole('tab', { name: 'Ambiguities (1)' }));
    expect(screen.getByText('MISSING PERMISSION RULE')).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'Regenerate' }));
    expect(await screen.findByText(/Generation completed/)).toBeVisible();
  }, 15_000);
});
