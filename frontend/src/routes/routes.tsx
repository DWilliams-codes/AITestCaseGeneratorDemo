import { createBrowserRouter, type RouteObject } from 'react-router-dom';
import { ApplicationShell } from '../layouts/ApplicationShell';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { ProjectDashboardPage } from '../pages/ProjectDashboardPage';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { ProjectPage } from '../pages/ProjectPage';
import { RequirementPage } from '../pages/RequirementPage';

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
          { path: 'requirements/:requirementId', element: <RequirementPage /> },
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
