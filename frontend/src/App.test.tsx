import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { createMemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { App } from './App';
import { appRoutes } from './routes/routes';
import { server } from './test/server';

const demoUser = {
  id: '10000000-0000-0000-0000-000000000001',
  email: 'demo@testforge.local',
  displayName: 'Maya Chen',
  role: 'USER',
  createdAt: '2026-07-30T12:00:00Z',
};

function renderRoute(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  return render(<App router={router} />);
}

describe('TestForge application', () => {
  it('renders a functional sign-in form accessibly', async () => {
    const { container } = renderRoute('/login');

    expect(await screen.findByRole('heading', { level: 1, name: 'Welcome back' })).toBeVisible();
    expect(screen.getByLabelText('Email address')).toHaveValue('demo@testforge.local');
    expect(screen.getByLabelText('Password')).toHaveValue('TestForge!Demo2026');
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled();
    const accessibilityResults = await axe(container);
    expect(accessibilityResults.violations).toEqual([]);
  });

  it('signs in and displays the owned project workspace', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json({
          accessToken: 'signed-access-token',
          expiresInSeconds: 600,
          user: demoUser,
        }),
      ),
      http.get('/api/v1/projects', () =>
        HttpResponse.json({
          items: [
            {
              id: '20000000-0000-0000-0000-000000000001',
              name: 'Customer Returns Portal',
              description: 'Retail returns quality coverage.',
              status: 'ACTIVE',
              requirementCount: 1,
              createdAt: '2026-07-30T12:00:00Z',
              updatedAt: '2026-07-30T12:00:00Z',
              version: 0,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        }),
      ),
      http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 })),
    );
    const user = userEvent.setup();
    renderRoute('/login');
    await user.click(await screen.findByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Projects' })).toBeVisible();
    expect(await screen.findByRole('heading', { name: 'Customer Returns Portal' })).toBeVisible();
    expect(screen.getByText('Maya Chen')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Welcome back' })).toBeVisible();
  });

  it('restores a session and supports the new-project dialog', async () => {
    server.use(
      http.post('/api/v1/auth/refresh', () =>
        HttpResponse.json({ accessToken: 'restored-token', expiresInSeconds: 600, user: demoUser }),
      ),
      http.get('/api/v1/projects', () =>
        HttpResponse.json({
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          hasNext: false,
        }),
      ),
      http.post('/api/v1/projects', async ({ request }) => {
        const body = (await request.json()) as { name: string; description: string };
        return HttpResponse.json(
          {
            id: '20000000-0000-0000-0000-000000000002',
            name: body.name,
            description: body.description,
            status: 'ACTIVE',
            requirementCount: 0,
            createdAt: '2026-07-30T12:00:00Z',
            updatedAt: '2026-07-30T12:00:00Z',
            version: 0,
          },
          { status: 201 },
        );
      }),
      http.get('/api/v1/projects/20000000-0000-0000-0000-000000000002', () =>
        HttpResponse.json({
          id: '20000000-0000-0000-0000-000000000002',
          name: 'Payments Modernization',
          description: 'Synthetic payments project.',
          status: 'ACTIVE',
          requirementCount: 0,
          createdAt: '2026-07-30T12:00:00Z',
          updatedAt: '2026-07-30T12:00:00Z',
          version: 0,
        }),
      ),
      http.get('/api/v1/projects/20000000-0000-0000-0000-000000000002/requirements', () =>
        HttpResponse.json({
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          hasNext: false,
        }),
      ),
    );
    const user = userEvent.setup();
    renderRoute('/');

    expect(await screen.findByRole('heading', { level: 1, name: 'Projects' })).toBeVisible();
    expect(await screen.findByRole('heading', { name: 'Create your first project' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'New project' }));
    expect(screen.getByRole('heading', { name: 'Create a project' })).toBeVisible();
    await user.type(screen.getByLabelText('Project name'), 'Payments Modernization');
    await user.type(screen.getByLabelText('Description'), 'Synthetic payments project.');
    await user.click(screen.getByRole('button', { name: 'Create project' }));
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Payments Modernization' }),
    ).toBeVisible();
  });

  it('recovers the projects page after a temporary API outage', async () => {
    let projectRequests = 0;
    server.use(
      http.get('/api/v1/projects', () => {
        projectRequests += 1;
        if (projectRequests <= 2) {
          return HttpResponse.json(
            {
              title: 'Service Unavailable',
              detail: 'The project service is temporarily unavailable.',
              code: 'service_unavailable',
            },
            { status: 503 },
          );
        }
        return HttpResponse.json({
          items: [
            {
              id: '20000000-0000-0000-0000-000000000003',
              name: 'Recovered Project',
              description: 'Available after retry.',
              status: 'ACTIVE',
              requirementCount: 0,
              createdAt: '2026-07-30T12:00:00Z',
              updatedAt: '2026-07-30T12:00:00Z',
              version: 0,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
        });
      }),
    );
    const user = userEvent.setup();
    renderRoute('/');

    expect(
      await screen.findByText('The project service is temporarily unavailable.'),
    ).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('heading', { name: 'Recovered Project' })).toBeVisible();
    expect(projectRequests).toBe(3);
  });

  it('registers a new account without persisting an access token in browser storage', async () => {
    server.use(
      http.post('/api/v1/auth/register', () =>
        HttpResponse.json(
          {
            accessToken: 'new-account-token',
            expiresInSeconds: 600,
            user: { ...demoUser, email: 'analyst@testforge.local', displayName: 'Jordan Lee' },
          },
          { status: 201 },
        ),
      ),
      http.get('/api/v1/projects', () =>
        HttpResponse.json({
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          hasNext: false,
        }),
      ),
    );
    const actor = userEvent.setup();
    renderRoute('/login');
    await actor.click(await screen.findByRole('tab', { name: 'Create account' }));
    await actor.type(screen.getByLabelText('Display name'), 'Jordan Lee');
    const email = screen.getByLabelText('Email address');
    await actor.clear(email);
    await actor.type(email, 'analyst@testforge.local');
    await actor.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Projects' })).toBeVisible();
    expect(window.localStorage).toHaveLength(0);
  });

  it('provides a recovery path for unknown routes', async () => {
    renderRoute('/missing');
    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Return to projects' })).toHaveAttribute('href', '/');
    await waitFor(() =>
      expect(screen.queryByLabelText('Restoring session')).not.toBeInTheDocument(),
    );
  });
});
