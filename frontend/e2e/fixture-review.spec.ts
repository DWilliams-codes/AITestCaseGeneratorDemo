import { expect, test } from '@playwright/test';
import { readFileSync } from 'node:fs';

const source = JSON.parse(
  readFileSync(new URL('./stub/fixtures/case-review-story.json', import.meta.url), 'utf8'),
);

test('server-confirmed public demo credentials fill and sign into the labeled workspace', async ({
  page,
}) => {
  await page.goto('/login');
  await page.getByRole('button', { name: 'Fill demo credentials' }).click();
  await expect(page.getByLabel('Email address')).toHaveValue('demo@testforge.local');
  await expect(page.getByLabel('Password')).toHaveValue('TestForge!Demo2026');
  await expect(page.getByText(/no live AI call occurs/)).toBeVisible();
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Projects', exact: true })).toBeVisible();
  await expect(page.getByRole('alert').filter({ hasText: 'no live AI call' })).toBeVisible();
});

test('maintained Case drafts persist, consume clarification, and retain a human edit after reload', async ({
  page,
}) => {
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  const registration = await page.request.post('/api/v1/auth/register', {
    headers: { [csrf.headerName]: csrf.token },
    data: {
      email: `fixture-${crypto.randomUUID()}@example.test`,
      displayName: 'Synthetic Fixture Reviewer',
      password: 'TestForge!Fixture2026',
    },
  });
  expect(registration.status()).toBe(201);
  const session = await registration.json();
  const headers = { Authorization: `Bearer ${session.accessToken}`, [csrf.headerName]: csrf.token };
  const project = await page.request.post('/api/v1/projects', {
    headers,
    data: {
      name: `Fixture review ${crypto.randomUUID().slice(0, 8)}`,
      description: 'Isolated synthetic browser review fixture',
    },
  });
  expect(project.status()).toBe(201);
  const projectId = (await project.json()).id;
  const created = await page.request.post(`/api/v1/projects/${projectId}/user-stories`, {
    headers,
    data: source,
  });
  expect(created.status()).toBe(201);
  const storyId = (await created.json()).id;
  await page.goto(`/user-stories/${storyId}`);
  await page.getByRole('button', { name: 'Generate tests', exact: true }).click();
  await expect(
    page.getByRole('alert').filter({ hasText: 'Fixture generation completed' }),
  ).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Test cases (6)' })).toBeVisible();
  await page.getByRole('tab', { name: 'Ambiguities (1)' }).click();
  await page.getByRole('button', { name: 'Resolve', exact: true }).click();
  const resolution = page.getByRole('dialog', { name: 'Resolve ambiguity' });
  await resolution.getByLabel('Resolution').fill('Priority High; Owner Synthetic Agent');
  await resolution.getByRole('button', { name: 'Save resolution' }).click();
  await expect(page.getByText(/Saved answer: Priority High; Owner Synthetic Agent/)).toBeVisible();
  await page.getByRole('button', { name: 'Regenerate', exact: true }).click();
  await expect(
    page.getByRole('alert').filter({ hasText: 'Fixture generation completed' }),
  ).toBeVisible();
  await page.getByRole('tab', { name: 'Test cases (6)' }).click();
  await page
    .getByText('Move a Case to Working with the confirmed priority and owner', { exact: true })
    .click();
  await expect(page.getByText('Select High in Priority.', { exact: true })).toBeVisible();
  await expect(page.getByText('Owner displays Synthetic Agent.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Edit', exact: true }).first().click();
  const editor = page.getByRole('dialog', { name: /Edit TC-/ });
  await editor
    .getByRole('textbox', { name: 'Expected result', exact: true })
    .first()
    .fill('The same recorded Case identifier displays New before the routing update.');
  await editor.getByRole('button', { name: 'Save changes' }).click();
  await expect(editor).toBeHidden();
  await page.reload();
  await page
    .getByText('Move a Case to Working with the confirmed priority and owner', { exact: true })
    .click();
  await expect(
    page.getByText('The same recorded Case identifier displays New before the routing update.', {
      exact: true,
    }),
  ).toBeVisible();
  const casesResponse = await page.request.get(`/api/v1/user-stories/${storyId}/test-cases`, {
    headers,
  });
  expect(casesResponse.status()).toBe(200);
  const cases = await casesResponse.json();
  expect(cases).toHaveLength(6);
  expect(cases.every((item: { status: string }) => item.status !== 'APPROVED')).toBe(true);
  const runs = await (
    await page.request.get(`/api/v1/user-stories/${storyId}/generation-runs`, { headers })
  ).json();
  expect(
    runs.every(
      (run: {
        provider: string;
        model: string;
        inputTokens: number | null;
        outputTokens: number | null;
      }) =>
        run.provider === 'external-demo-fixture' &&
        run.model === 'testforge-review-fixture' &&
        run.inputTokens === null &&
        run.outputTokens === null,
    ),
  ).toBe(true);
});
