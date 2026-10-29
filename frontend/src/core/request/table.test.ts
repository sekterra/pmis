import { describe, expect, it, vi } from 'vitest';
import { ApiError } from './errors';
import { bindFeedback } from './feedback';
import {
  expandDateRangeParams,
  mergeKeywordParams,
  toSortQuery,
  toTableRequest,
} from './table';

describe('toTableRequest', () => {
  it('maps current, pageSize, and sorter to page, size, and sort', async () => {
    const service = vi.fn(async () => ({
      items: [{ id: 1 }],
      total: 8,
      page: 2,
      size: 10,
    }));
    const request = toTableRequest(service);
    const result = await request(
      { current: 2, pageSize: 10, keyword: 'pmis' },
      { name: 'descend', code: 'ascend' },
    );
    expect(service).toHaveBeenCalledWith({
      keyword: 'pmis',
      page: 2,
      size: 10,
      sort: ['name,desc', 'code,asc'],
    });
    expect(result).toEqual({ data: [{ id: 1 }], total: 8, success: true });
  });

  it('toasts once and returns an empty page on error by default', async () => {
    const error = vi.fn();
    bindFeedback({ warning: vi.fn(), error, notify: vi.fn() });
    const apiError = new ApiError('목록 실패');
    apiError.showType = 2;
    const service = vi.fn(async () => {
      throw apiError;
    });
    const result = await toTableRequest(service)({ current: 1, pageSize: 20 });
    expect(result).toEqual({ data: [], total: 0, success: false });
    expect(error).toHaveBeenCalledTimes(1);
    expect(error).toHaveBeenCalledWith('목록 실패');
  });

  it('rethrows when onError is throw', async () => {
    const service = vi.fn(async () => {
      throw new ApiError('boom');
    });
    await expect(
      toTableRequest(service, { onError: 'throw' })({ current: 1, pageSize: 20 }),
    ).rejects.toThrow('boom');
  });

  it('applies mapParams before calling the service', async () => {
    const service = vi.fn(async () => ({
      items: [],
      total: 0,
      page: 1,
      size: 20,
    }));
    await toTableRequest(service, {
      mapParams: (params) => mergeKeywordParams(params),
    })({ current: 1, pageSize: 20, tenantCode: 'acme', tenantName: '에이' });
    expect(service).toHaveBeenCalledWith({
      keyword: 'acme 에이',
      page: 1,
      size: 20,
    });
  });
});

describe('table param helpers', () => {
  it('builds sort query pairs', () => {
    expect(toSortQuery({ createDatetime: 'descend', name: null })).toEqual([
      'createDatetime,desc',
    ]);
  });

  it('expands a date range into from/to ISO strings', () => {
    const result = expandDateRangeParams({
      issuedRange: ['2026-10-01T00:00:00.000Z', '2026-10-02T00:00:00.000Z'],
      status: 'ACTIVE',
    });
    expect(result.issuedRange).toBeUndefined();
    expect(result.from).toBe('2026-10-01T00:00:00.000Z');
    expect(result.to).toBe('2026-10-02T00:00:00.000Z');
    expect(result.status).toBe('ACTIVE');
  });

  it('merges code and name into keyword', () => {
    expect(
      mergeKeywordParams({ tenantCode: ' a ', tenantName: 'b', status: 'ACTIVE' }),
    ).toEqual({ keyword: 'a b', status: 'ACTIVE' });
  });
});
