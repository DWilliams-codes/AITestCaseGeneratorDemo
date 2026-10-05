import { useQuery } from '@tanstack/react-query';
import { apiRequest } from '../api/client';

export type DemoInfo =
  { enabled: false } | { enabled: true; email: string; password: string; label: string };

/** Trusts only server-confirmed public fixture metadata; failures never reveal fallback credentials. */
export function useDemoInfo() {
  return useQuery({
    queryKey: ['demo-info'],
    queryFn: () => apiRequest<DemoInfo>('/api/v1/demo-info'),
    retry: false,
    staleTime: 30000,
  });
}
