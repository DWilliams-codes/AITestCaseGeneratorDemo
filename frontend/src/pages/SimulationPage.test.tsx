import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter } from 'react-router';
import { axe } from 'vitest-axe';
import { App } from '../App';
import { appRoutes } from '../routes/routes';

/** Mounts the public synthetic route with the real shared theme and providers. */
function renderSimulation() {
  return render(
    <App router={createMemoryRouter(appRoutes, { initialEntries: ['/simulation'] })} />,
  );
}

describe('simulation walkthrough', () => {
  it('clarifies, reviews, approves, runs, repeats, and invalidates approval on edit', async () => {
    const actor = userEvent.setup();
    const { container } = renderSimulation();
    expect(screen.getByText('No Salesforce org connected')).toBeVisible();
    expect(screen.getAllByText('Unconnected')).toHaveLength(2);
    expect((await axe(container)).violations).toEqual([]);
    await actor.click(screen.getByRole('combobox', { name: 'Clarified owner' }));
    await actor.click(screen.getByRole('option', { name: 'Synthetic Agent' }));
    await actor.click(screen.getByRole('button', { name: 'Propose fixture steps' }));
    expect(screen.getByLabelText('Expected Owner (step-8)')).toHaveValue('Synthetic Agent');
    expect(screen.getByRole('button', { name: 'Approve execution contract' })).toBeDisabled();
    await actor.click(screen.getByRole('checkbox'));
    await actor.click(screen.getByRole('button', { name: 'Approve execution contract' }));
    await actor.click(screen.getByRole('button', { name: 'Run approved simulation' }));
    expect(screen.getByText('Outcome: PASS')).toBeVisible();
    expect(screen.getByText('Evidence: COMPLETE')).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'Start new isolated run' }));
    expect(screen.getAllByRole('button', { name: /^Run \d/ })).toHaveLength(2);
    await actor.click(screen.getByRole('button', { name: 'Return to review' }));
    await actor.clear(screen.getByLabelText('Expected Owner (step-8)'));
    await actor.type(screen.getByLabelText('Expected Owner (step-8)'), 'Synthetic Queue');
    await actor.click(screen.getByRole('tab', { name: '3. Run & evidence' }));
    expect(screen.getByRole('button', { name: 'Start new isolated run' })).toBeDisabled();
  }, 20000);
  it('keeps custom/blank criteria blocked and validates pasted source safely', async () => {
    const actor = userEvent.setup();
    renderSimulation();
    await actor.click(screen.getByRole('button', { name: 'Account create / update' }));
    await actor.clear(screen.getByLabelText(/Acceptance criteria/));
    await actor.click(screen.getByRole('button', { name: 'Propose fixture steps' }));
    expect(screen.getAllByText(/at least one nonempty criterion/).length).toBeGreaterThan(0);
    await actor.click(screen.getByRole('button', { name: 'Paste or import story JSON' }));
    await actor.type(screen.getByLabelText('Story JSON'), '{{bad');
    await actor.click(screen.getByRole('button', { name: 'Apply pasted JSON' }));
    expect(screen.getByText(/Use JSON with title/)).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'Show fixture JSON' }));
    await actor.click(screen.getByRole('button', { name: 'Apply pasted JSON' }));
    await actor.click(screen.getByRole('button', { name: 'Propose fixture steps' }));
    expect(screen.getByText('Review the execution contract')).toBeVisible();
  }, 20000);
});
