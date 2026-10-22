import { beforeEach, describe, expect, it, vi } from 'vitest';
import { closeDepartment, listDepartmentHistory, listDepartmentTree } from './dept';
import { listPeople } from './people';

const rawRequest = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

describe('dept service', () => {
  beforeEach(() => {
    rawRequest.mockReset();
    rawRequest.mockResolvedValue({ items: [], total: 0, page: 1, size: 20 });
  });

  it('calls tree, history, and people with the query', async () => {
    await listDepartmentTree({ companyCode: 'ACME', asOfDate: '2026-10-22' });
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/departments/tree', {
      method: 'GET',
      params: { companyCode: 'ACME', asOfDate: '2026-10-22' },
    });
    await listDepartmentHistory(3, { page: 1 });
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/departments/3/history', {
      method: 'GET',
      params: { page: 1, size: 20 },
    });
    await listPeople({ deptPk: 3, page: 1, size: 20 });
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/people', {
      method: 'GET',
      params: { deptPk: 3, page: 1, size: 20 },
    });
    await closeDepartment(3);
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/departments/3', { method: 'DELETE' });
  });
});
