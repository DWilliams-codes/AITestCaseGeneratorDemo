import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

test('390px signed-in workspace keeps theme, dialogs, tables, and Stage 1 actions reachable', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  await page.getByLabel('Email address').fill('demo@testforge.local');
  await page.getByLabel('Password').fill('TestForge!Demo2026');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Projects' })).toBeVisible();

  await page.getByRole('combobox', { name: 'Theme' }).click();
  await page.getByRole('option', { name: 'Dark' }).click();
  await expect(page.locator('html')).toHaveCSS('color-scheme', 'dark');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  );

  await page.getByRole('button', { name: 'New project' }).click();
  const projectDialog = page.getByRole('dialog', { name: 'Create a project' });
  await expect(projectDialog).toBeVisible();
  expect((await projectDialog.boundingBox())?.width).toBe(390);
  await projectDialog.getByRole('button', { name: 'Cancel' }).click();

  await page.getByText('Commerce Returns Platform').click();
  await page.getByText('Submit an eligible product return').click();
  await expect(
    page.getByRole('heading', { level: 1, name: 'Submit an eligible product return' }),
  ).toBeVisible();
  await page.getByRole('tab', { name: 'Traceability' }).click();
  const traceability = page.getByLabel('Traceability matrix');
  await expect(traceability).toBeVisible();
  await traceability.focus();
  await expect(traceability).toBeFocused();
  expect(await traceability.evaluate((element) => element.scrollWidth > element.clientWidth)).toBe(
    true,
  );

  const results = await new AxeBuilder({ page }).analyze();
  expect(
    results.violations.filter(({ impact }) => ['serious', 'critical'].includes(impact ?? '')),
  ).toEqual([]);
});
