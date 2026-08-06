import { Button, Stack, Typography } from '@mui/material';
import type { PageResponse } from '../types/api';

/** Provides the same accessible bounded-page navigation across collection views. */
export function PaginationControls({
  page,
  busy,
  onPageChange,
}: {
  page: Pick<PageResponse<unknown>, 'page' | 'totalPages' | 'totalElements' | 'hasNext'>;
  busy: boolean;
  onPageChange(page: number): void;
}) {
  return (
    <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
      <Typography variant="caption" color="text.secondary" aria-live="polite">
        Page {page.page + 1} of {Math.max(page.totalPages, 1)} • {page.totalElements} items
      </Typography>
      <Stack direction="row" spacing={1}>
        <Button
          size="small"
          disabled={page.page === 0 || busy}
          onClick={() => onPageChange(Math.max(page.page - 1, 0))}
        >
          Previous page
        </Button>
        <Button
          size="small"
          disabled={!page.hasNext || busy}
          onClick={() => onPageChange(page.page + 1)}
        >
          Next page
        </Button>
      </Stack>
    </Stack>
  );
}
