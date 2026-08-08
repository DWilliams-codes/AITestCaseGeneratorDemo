import { createBrowserRouter, type RouteObject } from 'react-router';
import { ApplicationShell } from '../layouts/ApplicationShell';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { ProjectDashboardPage } from '../pages/ProjectDashboardPage';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { ProjectPage } from '../pages/ProjectPage';
import { RequirementPage } from '../pages/RequirementPage';
import { LegacyRequirementRedirect } from './LegacyRequirementRedirect';

export const appRoutes: RouteObject[] = [
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/',
        element: <ApplicationShell />,
        children: [
          { index: true, element: <ProjectDashboardPage /> },
          { path: 'projects/:projectId', element: <ProjectPage /> },
          { path: 'user-stories/:userStoryId', element: <RequirementPage /> },
          { path: 'requirements/:requirementId', element: <LegacyRequirementRedirect /> },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
];

export const browserRouter = createBrowserRouter(appRoutes);
