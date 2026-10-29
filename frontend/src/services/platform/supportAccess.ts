import {
  PLATFORM_SORTS,
  issueReasonBody,
  listQueryParams,
} from '@/core/supportAccess/rules';
import { rawRequest } from '@/core/request/runtime';
import type {
  IssuedSupportAccess,
  PageResult,
  SupportAccessActionItem,
  SupportAccessItem,
  SupportAccessListQuery,
} from './data';

const BASE = '/api/platform/v1';

export function startSupportAccess(
  tenantCode: string,
  reason?: string | null,
): Promise<IssuedSupportAccess> {
  return rawRequest<IssuedSupportAccess>(
    `${BASE}/tenants/${encodeURIComponent(tenantCode)}/support-access`,
    {
      method: 'POST',
      data: issueReasonBody(reason),
    },
  );
}

export function listPlatformSupportAccesses(
  query: SupportAccessListQuery,
): Promise<PageResult<SupportAccessItem>> {
  return rawRequest<PageResult<SupportAccessItem>>(`${BASE}/support-accesses`, {
    method: 'GET',
    params: listQueryParams(query, PLATFORM_SORTS),
  });
}

export function listPlatformSupportAccessActions(
  supportAccessId: number,
  page = 1,
  size = 50,
): Promise<PageResult<SupportAccessActionItem>> {
  return rawRequest<PageResult<SupportAccessActionItem>>(
    `${BASE}/support-accesses/${supportAccessId}/actions`,
    {
      method: 'GET',
      params: { page, size },
    },
  );
}
