import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createCompany, listCompanies, updateCompany } from './company';

const rawRequest = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

describe('company service', () => {
  beforeEach(() => {
    rawRequest.mockReset();
    rawRequest.mockResolvedValue({ items: [], total: 0, page: 1, size: 20 });
  });

  it('lists with page, size, and sort and omits an unrestricted use flag', async () => {
    await listCompanies({ page: 2, size: 20, keyword: 'acme', useYn: '', sort: ['companyCode,asc'] });
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/companies', {
      method: 'GET',
      params: { page: 2, size: 20, keyword: 'acme', sort: ['companyCode,asc'] },
    });
  });

  it('posts a create body and puts an update body without the code', async () => {
    await createCompany({
      companyCode: 'ACME',
      companyName: '에이씨엠이',
      companyShortName: null,
      companyTypeCode: 'OWN',
      businessRegistrationNo: null,
      nationCode: null,
      parentCompanyCode: null,
      sortOrder: null,
      translations: [],
    });
    await updateCompany(4, {
      companyName: '에이씨엠이',
      companyShortName: null,
      companyTypeCode: 'OWN',
      businessRegistrationNo: '123-45-67890',
      nationCode: 'KR',
      parentCompanyCode: null,
      sortOrder: 1,
      address: '서울',
      translations: [{ supportLanguageCode: 'en-US', text: 'Acme' }],
    });
    expect(rawRequest).toHaveBeenNthCalledWith(1, '/api/mdm/v1/companies', {
      method: 'POST',
      data: expect.not.objectContaining({ address: '서울' }),
    });
    const update = rawRequest.mock.calls[1]?.[1] as { data: Record<string, unknown> };
    expect(rawRequest.mock.calls[1]?.[0]).toBe('/api/mdm/v1/companies/4');
    expect(update.data.companyCode).toBeUndefined();
    expect(update.data.address).toBe('서울');
  });
});
