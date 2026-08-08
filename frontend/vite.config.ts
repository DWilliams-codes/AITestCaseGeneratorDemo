import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
  preview: {
    port: 4173,
    strictPort: true,
  },
  build: {
    license: true,
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'react-vendor',
              test: /node_modules[\\/](?:react|react-dom|react-router)[\\/]/,
              priority: 40,
            },
            {
              name: 'ui-vendor',
              test: /node_modules[\\/](?:@mui|@emotion)[\\/]/,
              priority: 30,
            },
            {
              name: 'data-vendor',
              test: /node_modules[\\/](?:@tanstack|@hookform|react-hook-form|zod)[\\/]/,
              priority: 20,
            },
            {
              name: 'vendor',
              test: /node_modules[\\/]/,
              priority: 10,
            },
          ],
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.test.{ts,tsx}'],
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/main.tsx', 'src/test/**', 'src/**/*.test.{ts,tsx}', 'src/vite-env.d.ts'],
      thresholds: {
        lines: 75,
        branches: 65,
        functions: 75,
        statements: 75,
      },
    },
  },
});
