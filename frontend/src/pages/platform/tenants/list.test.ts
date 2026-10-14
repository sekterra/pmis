import { describe, expect, it, vi } from 'vitest';
import { toTableRequest } from '@/core/request/table';

describe('tenant list query', () => {
  it('sends keyword, status, page, size, and sort', async () => {
    const service = vi.fn(async () => ({
      items: [],
      total: 0,
      page: 2,
      size: 10,
    }));
    const request = toTableRequest(service);
    await request(
      { current: 2, pageSize: 10, keyword: 'acme', status: 'ACTIVE' },
      { tenantName: 'descend' },
    );
    expect(service).toHaveBeenCalledWith({
      keyword: 'acme',
      status: 'ACTIVE',
      page: 2,
      size: 10,
      sort: ['tenantName,desc'],
    });
  });

  it('sends only a server sort field for createDatetime', async () => {
    const service = vi.fn(async () => ({
      items: [],
      total: 0,
      page: 1,
      size: 20,
    }));
    await toTableRequest(service)({ current: 1, pageSize: 20 }, { createDatetime: 'descend' });
    expect(service).toHaveBeenCalledWith({
      page: 1,
      size: 20,
      sort: ['createDatetime,desc'],
    });
  });
});
