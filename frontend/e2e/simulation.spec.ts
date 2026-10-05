import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

for (const mode of ['light', 'dark'] as const) {
  for (const width of [1280, 390]) {
    test(`public Salesforce simulation ${mode} ${width}: clarification, approval and evidence`, async ({
      page,
    }) => {
      await page.setViewportSize({ width, height: 900 });
      await page.addInitScript(
        (preference) => localStorage.setItem('testforge-color-mode', preference),
        mode,
      );
      await page.goto('/simulation');
      await expect(page.getByText('No Salesforce org connected')).toBeVisible();
      await expect(page.getByText('Unconnected', { exact: true })).toHaveCount(2);
      await page.getByRole('combobox', { name: 'Clarified owner' }).click();
      await page.getByRole('option', { name: 'Synthetic Agent', exact: true }).click();
      await page.getByRole('button', { name: 'Propose fixture steps' }).focus();
      await page.keyboard.press('Enter');
      await expect(page.getByLabel('Expected Owner (step-8)')).toHaveValue('Synthetic Agent');
      await expect(page.getByRole('button', { name: 'Approve execution contract' })).toBeDisabled();
      await page.getByRole('checkbox').check();
      await page.getByRole('button', { name: 'Approve execution contract' }).click();
      await page.getByRole('button', { name: 'Run approved simulation' }).click();
      await expect(page.getByText('Outcome: PASS', { exact: true })).toBeVisible();
      await expect(page.getByText('Evidence: COMPLETE', { exact: true })).toBeVisible();
      expect(
        await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
      ).toBe(true);
      expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
      await page.getByRole('button', { name: 'Return to review' }).click();
      await page.getByLabel('Expected Owner (step-8)').fill('Synthetic Queue');
      await page.getByRole('tab', { name: '3. Run & evidence' }).click();
      await expect(page.getByRole('button', { name: 'Start new isolated run' })).toBeDisabled();
      await page.getByRole('button', { name: 'Approved source and contract' }).click();
      await expect(page.getByText(/"owner": "Synthetic Agent"/)).toBeVisible();
    });
  }
}

test('synthetic failure and repeat exercises preserve lifecycle and downloaded contract', async ({
  page,
}) => {
  await page.goto('/simulation');
  await page.getByRole('button', { name: 'Account create / update' }).click();
  await page.getByRole('button', { name: 'Propose fixture steps' }).click();
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Approve execution contract' }).click();
  await page.getByRole('combobox', { name: 'Simulation exercise' }).click();
  await page.getByRole('option', { name: 'Interrupt write acknowledgement' }).click();
  await page.getByRole('button', { name: 'Run approved simulation' }).click();
  await expect(page.getByText('Outcome: INDETERMINATE', { exact: true })).toBeVisible();
  await expect(page.getByText('Evidence: PARTIAL', { exact: true })).toBeVisible();
  await page.getByRole('combobox', { name: 'Simulation exercise' }).click();
  await page.getByRole('option', { name: 'Duplicate action delivery' }).click();
  await page.getByRole('button', { name: 'Start new isolated run' }).click();
  await expect(page.getByText('Outcome: PASS', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: /^Run \d/ })).toHaveCount(2);
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Download run JSON' }).click();
  expect((await download).suggestedFilename()).toMatch(/^testforge-simulation-.*\.json$/);
});

test('login dark hero remains accessible with simulation navigation', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('testforge-color-mode', 'dark'));
  await page.goto('/login');
  await expect(page.getByRole('link', { name: 'Explore the Salesforce simulation' })).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
});
