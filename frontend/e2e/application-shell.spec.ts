import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

test('seeded analyst workflow is navigable and has no serious accessibility violations', async ({
  page,
}) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Projects' })).toBeVisible();
  await expect(page.getByText('Commerce Returns Platform')).toBeVisible();

  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Skip to main content' })).toBeFocused();

  for (const currentPage of ['projects', 'project', 'requirement']) {
    const results = await new AxeBuilder({ page }).analyze();
    const seriousViolations = results.violations.filter(({ impact }) =>
      ['serious', 'critical'].includes(impact ?? ''),
    );
    expect(seriousViolations, `${currentPage} page accessibility`).toEqual([]);
    if (currentPage === 'projects') {
      await page.getByText('Commerce Returns Platform').click();
      await expect(
        page.getByRole('heading', { level: 1, name: 'Commerce Returns Platform' }),
      ).toBeVisible();
    }
    if (currentPage === 'project') {
      await page.getByText('Submit an eligible product return').click();
      await expect(
        page.getByRole('heading', { level: 1, name: 'Submit an eligible product return' }),
      ).toBeVisible();
      await expect(page.getByText(/User Story \d+/)).toBeVisible();
      await expect(page.getByRole('button', { name: 'Generate tests' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Test cases (0)' })).toBeVisible();
    }
  }
});
