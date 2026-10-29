import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

import { startSupportAccess, listPlatformSupportAccesses } from './supportAccess';

describe('platform support access services', () => {
  beforeEach(() => {
    rawRequest.mockReset();
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('omits a blank reason and keeps a trimmed reason', async () => {
    rawRequest.mockResolvedValue({
      supportAccessId: 1,
      accessUrl: 'http://localhost/support-access#ticket=x',
      ticketExpireDatetime: '2026-10-29T00:00:00Z',
    });
    await startSupportAccess('acme', '  ');
    expect(rawRequest).toHaveBeenCalledWith(
      '/api/platform/v1/tenants/acme/support-access',
      expect.objectContaining({ method: 'POST', data: {} }),
    );
    await startSupportAccess('acme', ' 점검 ');
    expect(rawRequest).toHaveBeenLastCalledWith(
      '/api/platform/v1/tenants/acme/support-access',
      expect.objectContaining({ data: { reason: '점검' } }),
    );
  });

  it('drops empty list filters and unknown sort fields', async () => {
    rawRequest.mockResolvedValue({ items: [], total: 0, page: 1, size: 20 });
    await listPlatformSupportAccesses({
      tenantCode: ' ',
      platformLoginId: 'ops',
      status: 'ACTIVE',
      sort: ['issuedDatetime,desc', 'secret,asc'],
      page: 2,
      size: 10,
    });
    expect(rawRequest).toHaveBeenCalledWith(
      '/api/platform/v1/support-accesses',
      expect.objectContaining({
        params: {
          page: 2,
          size: 10,
          platformLoginId: 'ops',
          status: 'ACTIVE',
          sort: ['issuedDatetime,desc'],
        },
      }),
    );
  });
});
