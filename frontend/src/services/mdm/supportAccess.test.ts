import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

import { exchangeSupportTicket, endSupportAccess } from './supportAccess';

describe('tenant support access services', () => {
  beforeEach(() => {
    rawRequest.mockReset();
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('exchanges a ticket with csrf and optional tenant header', async () => {
    rawRequest.mockResolvedValue({
      accessToken: 'token',
      expiresInSeconds: 1800,
      supportAccessId: 9,
    });
    await exchangeSupportTicket('ticket-value', 'acme');
    expect(rawRequest).toHaveBeenCalledWith(
      '/api/mdm/v1/auth/support-access/exchange',
      expect.objectContaining({
        method: 'POST',
        data: { ticket: 'ticket-value' },
        skipAuth: true,
        headers: {
          'X-MDM-Request': '1',
          'X-Tenant-Code': 'acme',
        },
      }),
    );
  });

  it('ends support access with csrf', async () => {
    rawRequest.mockResolvedValue(undefined);
    await endSupportAccess();
    expect(rawRequest).toHaveBeenCalledWith(
      '/api/mdm/v1/auth/support-access/end',
      expect.objectContaining({
        method: 'POST',
        headers: { 'X-MDM-Request': '1' },
      }),
    );
  });
});
