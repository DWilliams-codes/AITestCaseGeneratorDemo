import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ColorModeProvider } from './ColorModeContext';
import { useColorMode } from './useColorMode';

let systemListener: ((event: MediaQueryListEvent) => void) | undefined;
let systemDark = false;
let mediaQuery: MediaQueryList;

/** Exposes the provider state without coupling preference tests to application routes. */
function ColorModeProbe() {
  const { preference, resolvedMode, setPreference } = useColorMode();
  return (
    <>
      <output>{`${preference}:${resolvedMode}`}</output>
      <button onClick={() => setPreference('dark')}>Use dark</button>
      <button onClick={() => setPreference('light')}>Use light</button>
      <button onClick={() => setPreference('system')}>Use system</button>
    </>
  );
}

/** Renders a provider probe so preference lifecycle assertions stay route-independent. */
function renderProbe() {
  return render(
    <ColorModeProvider>
      <ColorModeProbe />
    </ColorModeProvider>,
  );
}

describe('ColorModeProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    systemDark = false;
    systemListener = undefined;
    mediaQuery = {
      matches: systemDark,
      media: '(prefers-color-scheme: dark)',
      addEventListener: (_: 'change', listener: (event: MediaQueryListEvent) => void) => {
        systemListener = listener;
      },
      removeEventListener: vi.fn(),
    } as unknown as MediaQueryList;
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn(() => mediaQuery),
    });
  });

  it('falls back safely from invalid storage and follows system changes', () => {
    localStorage.setItem('testforge-color-mode', 'invalid');
    renderProbe();

    expect(screen.getByText('system:light')).toBeVisible();
    systemDark = true;
    Object.defineProperty(mediaQuery, 'matches', { configurable: true, value: true });
    act(() => systemListener?.({ matches: true } as MediaQueryListEvent));
    expect(screen.getByText('system:dark')).toBeVisible();
  });

  it('persists explicit preferences, clears system mode, and synchronizes lifecycle changes', async () => {
    const actor = userEvent.setup();
    const mounted = renderProbe();
    await actor.click(screen.getByRole('button', { name: 'Use light' }));
    expect(localStorage.getItem('testforge-color-mode')).toBe('light');
    expect(screen.getByText('light:light')).toBeVisible();
    expect(document.documentElement.style.colorScheme).toBe('light');

    await actor.click(screen.getByRole('button', { name: 'Use dark' }));
    expect(localStorage.getItem('testforge-color-mode')).toBe('dark');
    expect(document.documentElement.style.colorScheme).toBe('dark');

    mounted.unmount();
    renderProbe();
    expect(screen.getByText('dark:dark')).toBeVisible();
    await actor.click(screen.getByRole('button', { name: 'Use system' }));
    expect(localStorage.getItem('testforge-color-mode')).toBeNull();
    expect(screen.getByText('system:light')).toBeVisible();
    Object.defineProperty(mediaQuery, 'matches', { configurable: true, value: true });
    act(() => systemListener?.({ matches: true } as MediaQueryListEvent));
    expect(screen.getByText('system:dark')).toBeVisible();

    localStorage.setItem('testforge-color-mode', 'light');
    act(() => window.dispatchEvent(new StorageEvent('storage', { key: 'testforge-color-mode' })));
    expect(screen.getByText('light:light')).toBeVisible();
  });
});
