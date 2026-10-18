import { describe, expect, it, vi } from 'vitest';
import { toTableRequest } from './table';

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
});
