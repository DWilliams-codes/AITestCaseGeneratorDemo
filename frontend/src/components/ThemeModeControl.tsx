import Brightness4OutlinedIcon from '@mui/icons-material/Brightness4Outlined';
import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { useId } from 'react';
import { type ColorModePreference } from '../theme/ColorModeContext';
import { useColorMode } from '../theme/useColorMode';

/** Renders an explicit accessible local-only color-mode preference control. */
export function ThemeModeControl({ compact = false }: { compact?: boolean }) {
  const { preference, setPreference } = useColorMode();
  const labelId = useId();
  return (
    <FormControl size="small" sx={{ minWidth: compact ? 44 : 128 }}>
      {!compact && <InputLabel id={labelId}>Theme</InputLabel>}
      <Select
        aria-label={compact ? 'Theme' : undefined}
        labelId={compact ? undefined : labelId}
        label={compact ? undefined : 'Theme'}
        value={preference}
        onChange={(event) => setPreference(event.target.value as ColorModePreference)}
        renderValue={
          compact
            ? () => <Brightness4OutlinedIcon aria-hidden="true" fontSize="small" />
            : undefined
        }
        startAdornment={
          compact ? undefined : <Brightness4OutlinedIcon fontSize="small" sx={{ mr: 0.75 }} />
        }
      >
        <MenuItem value="system">System</MenuItem>
        <MenuItem value="light">Light</MenuItem>
        <MenuItem value="dark">Dark</MenuItem>
      </Select>
    </FormControl>
  );
}
