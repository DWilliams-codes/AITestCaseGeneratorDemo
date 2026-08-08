import { RouterProvider } from 'react-router';
import type { ComponentProps } from 'react';
import { AppProviders } from './app/AppProviders';
import { browserRouter } from './routes/routes';

interface AppProps {
  router?: ComponentProps<typeof RouterProvider>['router'];
}

/** Composes global providers with the selected production or test router. */
export function App({ router = browserRouter }: AppProps) {
  return (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  );
}
