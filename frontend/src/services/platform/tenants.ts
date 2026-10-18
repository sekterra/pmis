import { rawRequest } from '@/core/request/runtime';
import type {
  CreatedTenant,
  CreateTenantRequest,
  DomainView,
  PageResult,
  ReissuedPassword,
  TenantDetail,
  TenantListQuery,
  TenantSummary,
} from './data';

const TENANTS = '/api/platform/v1/tenants';

function tenantUrl(tenantCode: string, suffix = ''): string {
  return `${TENANTS}/${encodeURIComponent(tenantCode)}${suffix}`;
}

export function listTenants(query: TenantListQuery): Promise<PageResult<TenantSummary>> {
  return rawRequest<PageResult<TenantSummary>>(TENANTS, { method: 'GET', params: query });
}

export function getTenant(tenantCode: string): Promise<TenantDetail> {
  return rawRequest<TenantDetail>(tenantUrl(tenantCode), { method: 'GET' });
}

export function createTenant(body: CreateTenantRequest): Promise<CreatedTenant> {
  return rawRequest<CreatedTenant>(TENANTS, { method: 'POST', data: body });
}

export function retryProvisioning(tenantCode: string): Promise<CreatedTenant> {
  return rawRequest<CreatedTenant>(tenantUrl(tenantCode, '/provisioning/retry'), { method: 'POST' });
}

export function reissueAdminPassword(tenantCode: string): Promise<ReissuedPassword> {
  return rawRequest<ReissuedPassword>(tenantUrl(tenantCode, '/admin-password/reissue'), {
    method: 'POST',
  });
}

export function suspendTenant(tenantCode: string, changeReason: string): Promise<void> {
  return rawRequest<void>(tenantUrl(tenantCode, '/suspend'), {
    method: 'POST',
    data: { changeReason },
  });
}

export function resumeTenant(tenantCode: string, changeReason: string): Promise<void> {
  return rawRequest<void>(tenantUrl(tenantCode, '/resume'), {
    method: 'POST',
    data: { changeReason },
  });
}

export function listDomains(tenantCode: string): Promise<DomainView[]> {
  return rawRequest<DomainView[]>(tenantUrl(tenantCode, '/domains'), { method: 'GET' });
}

export function addDomain(tenantCode: string, domainName: string): Promise<DomainView> {
  return rawRequest<DomainView>(tenantUrl(tenantCode, '/domains'), {
    method: 'POST',
    data: { domainName },
  });
}

export function deleteDomain(tenantCode: string, domainName: string): Promise<void> {
  return rawRequest<void>(
    `${tenantUrl(tenantCode, '/domains/')}${encodeURIComponent(domainName)}`,
    { method: 'DELETE' },
  );
}
