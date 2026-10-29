export const SUPPORT_ACCESS_STATUSES = [
  'ISSUED',
  'ACTIVE',
  'ENDED',
  'EXPIRED',
  'REVOKED',
] as const;

export type SupportAccessStatus = (typeof SUPPORT_ACCESS_STATUSES)[number];

export const PLATFORM_SORTS = new Set([
  'issuedDatetime',
  'startedDatetime',
  'tenantCode',
  'supportAccessPk',
]);

export const TENANT_SORTS = new Set([
  'issuedDatetime',
  'startedDatetime',
  'supportAccessPk',
]);

export function parseTicketFromHash(hash: string): string | null {
  const raw = hash.startsWith('#') ? hash.slice(1) : hash;
  const params = new URLSearchParams(raw.includes('=') ? raw : `ticket=${raw}`);
  const ticket = params.get('ticket')?.trim();
  return ticket ? ticket : null;
}

export function clearTicketFromLocation(
  location: { pathname: string; search: string; hash: string },
  replaceState: (url: string) => void = (url) => {
    if (typeof history !== 'undefined') {
      history.replaceState(null, '', url);
    }
  },
): string | null {
  const ticket = parseTicketFromHash(location.hash);
  if (location.hash) {
    replaceState(`${location.pathname}${location.search}`);
  }
  return ticket;
}

export function tenantCodeFromSearch(
  search: string,
  isDev: boolean,
): string | null {
  if (!isDev) {
    return null;
  }
  const value = new URLSearchParams(
    search.startsWith('?') ? search.slice(1) : search,
  )
    .get('tenant')
    ?.trim();
  return value || null;
}

export function remainingSeconds(
  expireDatetime: string | Date | null | undefined,
  now: Date = new Date(),
): number {
  if (!expireDatetime) {
    return 0;
  }
  const expire =
    expireDatetime instanceof Date
      ? expireDatetime.getTime()
      : Date.parse(expireDatetime);
  if (Number.isNaN(expire)) {
    return 0;
  }
  return Math.max(0, Math.floor((expire - now.getTime()) / 1000));
}

export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(safe / 60);
  const seconds = safe % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export function statusTagColor(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'processing';
    case 'ISSUED':
      return 'blue';
    case 'ENDED':
      return 'default';
    case 'EXPIRED':
      return 'warning';
    case 'REVOKED':
      return 'error';
    default:
      return 'default';
  }
}

export function issueReasonBody(
  reason: string | null | undefined,
): { reason: string } | Record<string, never> {
  const trimmed = reason?.trim() ?? '';
  if (!trimmed) {
    return {};
  }
  return { reason: trimmed };
}

export function listQueryParams(
  input: {
    tenantCode?: string;
    platformLoginId?: string;
    status?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
    sort?: string[];
  },
  allowedSorts: Set<string>,
): Record<string, string | number | string[]> {
  const params: Record<string, string | number | string[]> = {
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
  const tenantCode = input.tenantCode?.trim();
  if (tenantCode) {
    params.tenantCode = tenantCode;
  }
  const platformLoginId = input.platformLoginId?.trim();
  if (platformLoginId) {
    params.platformLoginId = platformLoginId;
  }
  const status = input.status?.trim();
  if (status) {
    params.status = status;
  }
  const from = input.from?.trim();
  if (from) {
    params.from = from;
  }
  const to = input.to?.trim();
  if (to) {
    params.to = to;
  }
  const sort = (input.sort ?? []).filter((item) =>
    allowedSorts.has(item.split(',')[0] ?? ''),
  );
  if (sort.length > 0) {
    params.sort = sort;
  }
  return params;
}
