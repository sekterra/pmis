import { describe, expect, it, vi } from 'vitest';
import { mergeKeywordParams, toTableRequest } from '@/core/request/table';
import { TENANT_LIST_SORT_FIELDS } from './rules';

describe('tenant list query', () => {
  it('sends keyword, status, page, size, and sort', async () => {
    const service = vi.fn(async () => ({
      items: [],
      total: 0,
      page: 2,
      size: 10,
    }));
    const request = toTableRequest(service, {
      mapParams: (params) => mergeKeywordParams(params),
    });
    await request(
      {
        current: 2,
        pageSize: 10,
        tenantCode: 'acme',
        tenantName: '에이',
        status: 'ACTIVE',
      },
      { tenantName: 'descend' },
    );
    expect(service).toHaveBeenCalledWith({
      keyword: 'acme 에이',
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
    await toTableRequest(service)(
      { current: 1, pageSize: 20 },
      { createDatetime: 'descend' },
    );
    expect(service).toHaveBeenCalledWith({
      page: 1,
      size: 20,
      sort: ['createDatetime,desc'],
    });
  });

  it('keeps the sort whitelist for the list page', () => {
    expect(TENANT_LIST_SORT_FIELDS).toEqual([
      'tenantCode',
      'tenantName',
      'tenantStatusCode',
      'createDatetime',
    ]);
  });
});
