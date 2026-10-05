import { createContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';

export type ColorModePreference = 'system' | 'light' | 'dark';

const storageKey = 'testforge-color-mode';
// eslint-disable-next-line react-refresh/only-export-components
export const ColorModeContext = createContext<{
  preference: ColorModePreference;
  resolvedMode: 'light' | 'dark';
  setPreference(value: ColorModePreference): void;
} | null>(null);

/** Reads only an allowlisted presentation preference from local storage. */

/** Reads only an allowlisted presentation preference from local storage. */
function storedPreference(): ColorModePreference {
  try {
    const value = window.localStorage.getItem(storageKey);
    return value === 'light' || value === 'dark' || value === 'system' ? value : 'system';
  } catch {
    return 'system';
  }
}

/** Provides safe local-only color preference and system-mode resolution. */
export function ColorModeProvider({ children }: PropsWithChildren) {
  const [preference, setPreferenceState] = useState<ColorModePreference>(storedPreference);
  const [systemDark, setSystemDark] = useState(
    () => window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false,
  );
  const resolvedMode = preference === 'system' ? (systemDark ? 'dark' : 'light') : preference;

  useEffect(() => {
    const media = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (!media) return undefined;
    /** Synchronizes presentation state when the system or storage preference changes. */
    const update = () => setSystemDark(media.matches);
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, []);
  useEffect(() => {
    /** Synchronizes presentation state when the system or storage preference changes. */
    const update = (event: StorageEvent) => {
      if (event.key === storageKey) setPreferenceState(storedPreference());
    };
    window.addEventListener('storage', update);
    return () => window.removeEventListener('storage', update);
  }, []);
  useEffect(() => {
    document.documentElement.style.colorScheme = resolvedMode;
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', resolvedMode === 'dark' ? '#101820' : '#f6f7f9');
  }, [resolvedMode]);
  const value = useMemo(
    () => ({
      preference,
      resolvedMode,
      /** Stores only the chosen appearance enum and tolerates unavailable browser storage. */
      setPreference(next: ColorModePreference) {
        setPreferenceState(next);
        try {
          if (next === 'system') window.localStorage.removeItem(storageKey);
          else window.localStorage.setItem(storageKey, next);
        } catch {
          // Local preference is intentionally best-effort and never holds sensitive data.
        }
      },
    }),
    [preference, resolvedMode],
  );
  return <ColorModeContext.Provider value={value}>{children}</ColorModeContext.Provider>;
}
