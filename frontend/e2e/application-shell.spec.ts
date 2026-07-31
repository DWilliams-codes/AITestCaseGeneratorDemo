import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

test('seeded analyst workflow is navigable and has no serious accessibility violations', async ({
  page,
}) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Projects' })).toBeVisible();
  await expect(page.getByText('Customer Returns Portal')).toBeVisible();

  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Skip to main content' })).toBeFocused();

  for (const currentPage of ['projects', 'project', 'requirement']) {
    const results = await new AxeBuilder({ page }).analyze();
    const seriousViolations = results.violations.filter(({ impact }) =>
      ['serious', 'critical'].includes(impact ?? ''),
    );
    expect(seriousViolations, `${currentPage} page accessibility`).toEqual([]);
    if (currentPage === 'projects') {
      await page.getByText('Customer Returns Portal').click();
      await expect(
        page.getByRole('heading', { level: 1, name: 'Customer Returns Portal' }),
      ).toBeVisible();
    }
    if (currentPage === 'project') {
      await page.getByText('Submit an eligible product return').click();
      await expect(
        page.getByRole('heading', { level: 1, name: 'Submit an eligible product return' }),
      ).toBeVisible();
      await expect(page.getByText('75%')).toBeVisible();
    }
  }

  await page.getByRole('tab', { name: /Test cases/ }).click();
  await expect(page.getByText('TC-1', { exact: true })).toBeVisible();
  await expect(page.getByLabel('Sort by')).toHaveText('Test case number (ascending)');
  await expect(page.getByText(/^TC-\d+$/).allTextContents()).resolves.toEqual([
    'TC-1',
    'TC-2',
    'TC-3',
    'TC-4',
    'TC-5',
    'TC-6',
    'TC-7',
  ]);
  await page.getByRole('textbox', { name: 'Search test cases' }).fill('keyboard');
  await expect(page.getByText('Showing 1 of 7 test cases')).toBeVisible();
  await expect(page.getByText('TC-7', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Clear filters' }).click();
  await expect(page.getByText('Showing 7 of 7 test cases')).toBeVisible();
  const testCaseResults = await new AxeBuilder({ page }).analyze();
  expect(
    testCaseResults.violations.filter(({ impact }) =>
      ['serious', 'critical'].includes(impact ?? ''),
    ),
    'test case review accessibility',
  ).toEqual([]);
  await page.getByRole('tab', { name: 'Traceability' }).click();
  await expect(page.getByRole('columnheader', { name: 'Mapped evidence' })).toBeVisible();
});
