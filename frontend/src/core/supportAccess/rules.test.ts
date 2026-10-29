import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  clearTicketFromLocation,
  formatCountdown,
  issueReasonBody,
  listQueryParams,
  parseTicketFromHash,
  PLATFORM_SORTS,
  remainingSeconds,
  statusTagColor,
  tenantCodeFromSearch,
} from './rules';

describe('support access rules', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('parses a ticket and clears the hash from the location', () => {
    const replaceState = vi.fn();
    expect(parseTicketFromHash('#ticket=abc.def')).toBe('abc.def');
    expect(
      clearTicketFromLocation(
        {
          pathname: '/support-access',
          search: '?tenant=acme',
          hash: '#ticket=secret-ticket',
        },
        replaceState,
      ),
    ).toBe('secret-ticket');
    expect(replaceState).toHaveBeenCalledWith('/support-access?tenant=acme');
  });

  it('uses the tenant query only in development', () => {
    expect(tenantCodeFromSearch('?tenant=acme', true)).toBe('acme');
    expect(tenantCodeFromSearch('?tenant=acme', false)).toBeNull();
  });

  it('computes remaining time and countdown text with fake timers', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-29T00:00:00.000Z'));
    expect(remainingSeconds('2026-10-29T00:01:05.000Z')).toBe(65);
    expect(formatCountdown(65)).toBe('01:05');
    expect(formatCountdown(0)).toBe('00:00');
  });

  it('builds issue bodies and list queries without empty filters', () => {
    expect(issueReasonBody('  ')).toEqual({});
    expect(issueReasonBody(' 점검 ')).toEqual({ reason: '점검' });
    expect(
      listQueryParams(
        {
          tenantCode: ' ',
          platformLoginId: 'ops',
          status: 'ACTIVE',
          sort: ['issuedDatetime,desc', 'password,asc'],
        },
        PLATFORM_SORTS,
      ),
    ).toEqual({
      page: 1,
      size: 20,
      platformLoginId: 'ops',
      status: 'ACTIVE',
      sort: ['issuedDatetime,desc'],
    });
  });

  it('maps statuses to tag colors', () => {
    expect(statusTagColor('ACTIVE')).toBe('processing');
    expect(statusTagColor('EXPIRED')).toBe('warning');
    expect(statusTagColor('REVOKED')).toBe('error');
  });
});
