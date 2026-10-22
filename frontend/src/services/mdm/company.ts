import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import type { Company, CompanyCreateBody, CompanyListQuery, CompanyUpdateBody } from './companyRecord';
import { companyListParams } from './companyQuery';

const COMPANIES = '/api/mdm/v1/companies';

function companyUrl(companyPk: number, suffix = ''): string {
  return `${COMPANIES}/${companyPk}${suffix}`;
}

export function listCompanies(query: CompanyListQuery): Promise<PageResult<Company>> {
  return rawRequest<PageResult<Company>>(COMPANIES, {
    method: 'GET',
    params: companyListParams(query),
  });
}

export function getCompany(companyPk: number): Promise<Company> {
  return rawRequest<Company>(companyUrl(companyPk), { method: 'GET' });
}

export function createCompany(body: CompanyCreateBody): Promise<Company> {
  return rawRequest<Company>(COMPANIES, { method: 'POST', data: body });
}

export function updateCompany(companyPk: number, body: CompanyUpdateBody): Promise<Company> {
  return rawRequest<Company>(companyUrl(companyPk), { method: 'PUT', data: body });
}

export function deleteCompany(companyPk: number): Promise<void> {
  return rawRequest<void>(companyUrl(companyPk), { method: 'DELETE' });
}

export function restoreCompany(companyPk: number): Promise<Company> {
  return rawRequest<Company>(companyUrl(companyPk, '/restore'), { method: 'POST' });
}
