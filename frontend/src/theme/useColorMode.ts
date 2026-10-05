import { useContext } from 'react';
import { ColorModeContext } from './ColorModeContext';

/** Returns the current explicit preference and resolved application mode. */
export function useColorMode() {
  const context = useContext(ColorModeContext);
  if (!context) throw new Error('useColorMode must be used within ColorModeProvider.');
  return context;
}
