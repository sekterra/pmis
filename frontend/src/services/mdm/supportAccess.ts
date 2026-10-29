import {
  TENANT_SORTS,
  listQueryParams,
} from '@/core/supportAccess/rules';
import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import type {
  SupportAccessActionItem,
  SupportAccessItem,
  SupportAccessListQuery,
} from '@/services/platform/data';

const AUTH = '/api/mdm/v1/auth/support-access';
const LIST = '/api/mdm/v1/support-accesses';

export type ExchangedSupportAccess = {
  accessToken: string;
  expiresInSeconds: number;
  supportAccessId: number;
};

export function exchangeSupportTicket(
  ticket: string,
  tenantCode?: string | null,
): Promise<ExchangedSupportAccess> {
  const headers: Record<string, string> = { 'X-MDM-Request': '1' };
  const code = tenantCode?.trim();
  if (code) {
    headers['X-Tenant-Code'] = code;
  }
  return rawRequest<ExchangedSupportAccess>(`${AUTH}/exchange`, {
    method: 'POST',
    data: { ticket },
    headers,
    skipAuth: true,
    skipAuthRetry: true,
  });
}

export function endSupportAccess(): Promise<void> {
  return rawRequest<void>(`${AUTH}/end`, {
    method: 'POST',
    headers: { 'X-MDM-Request': '1' },
    skipAuthRetry: true,
  });
}

export function listTenantSupportAccesses(
  query: SupportAccessListQuery,
): Promise<PageResult<SupportAccessItem>> {
  return rawRequest<PageResult<SupportAccessItem>>(LIST, {
    method: 'GET',
    params: listQueryParams(query, TENANT_SORTS),
  });
}

export function listTenantSupportAccessActions(
  supportAccessId: number,
  page = 1,
  size = 50,
): Promise<PageResult<SupportAccessActionItem>> {
  return rawRequest<PageResult<SupportAccessActionItem>>(
    `${LIST}/${supportAccessId}/actions`,
    {
      method: 'GET',
      params: { page, size },
    },
  );
}
