import { Dialog, type DialogProps, useMediaQuery, useTheme } from '@mui/material';

/** Keeps workflow dialogs reachable on narrow screens without changing their desktop layout. */
export function ResponsiveDialog({ fullScreen, ...props }: DialogProps) {
  const theme = useTheme();
  const compactViewport = useMediaQuery(theme.breakpoints.down('sm'));

  return <Dialog {...props} fullScreen={fullScreen ?? compactViewport} />;
}
