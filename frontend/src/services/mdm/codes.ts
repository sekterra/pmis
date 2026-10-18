import { rawRequest } from '@/core/request/runtime';
import type {
  CodeGroup,
  CodeGroupBody,
  CodeHistory,
  CodeListQuery,
  CommonCode,
  CommonCodeBody,
} from './code';
import { listParams } from './codeQuery';
import type { PageResult } from './data';

const GROUPS = '/api/mdm/v1/code-groups';
const CODES = '/api/mdm/v1/common-codes';

function groupUrl(groupCode: string, suffix = ''): string {
  return `${GROUPS}/${encodeURIComponent(groupCode)}${suffix}`;
}

function codeUrl(codePk: number, suffix = ''): string {
  return `${CODES}/${codePk}${suffix}`;
}

export function listCodeGroups(query: CodeListQuery): Promise<PageResult<CodeGroup>> {
  return rawRequest<PageResult<CodeGroup>>(GROUPS, {
    method: 'GET',
    params: listParams(query),
  });
}

export function getCodeGroup(groupCode: string): Promise<CodeGroup> {
  return rawRequest<CodeGroup>(groupUrl(groupCode), { method: 'GET' });
}

export function createCodeGroup(body: CodeGroupBody): Promise<CodeGroup> {
  return rawRequest<CodeGroup>(GROUPS, { method: 'POST', data: body });
}

export function updateCodeGroup(groupCode: string, body: CodeGroupBody): Promise<CodeGroup> {
  return rawRequest<CodeGroup>(groupUrl(groupCode), { method: 'PUT', data: body });
}

export function deleteCodeGroup(groupCode: string): Promise<void> {
  return rawRequest<void>(groupUrl(groupCode), { method: 'DELETE' });
}

export function restoreCodeGroup(groupCode: string): Promise<CodeGroup> {
  return rawRequest<CodeGroup>(groupUrl(groupCode, '/restore'), { method: 'POST' });
}

export function listCommonCodes(query: CodeListQuery): Promise<PageResult<CommonCode>> {
  return rawRequest<PageResult<CommonCode>>(CODES, {
    method: 'GET',
    params: listParams(query),
  });
}

export function createCommonCode(body: CommonCodeBody): Promise<CommonCode> {
  return rawRequest<CommonCode>(CODES, { method: 'POST', data: body });
}

export function updateCommonCode(codePk: number, body: CommonCodeBody): Promise<CommonCode> {
  return rawRequest<CommonCode>(codeUrl(codePk), { method: 'PUT', data: body });
}

export function deleteCommonCode(codePk: number, changeReason?: string): Promise<void> {
  return rawRequest<void>(codeUrl(codePk), {
    method: 'DELETE',
    params: changeReason?.trim() ? { changeReason: changeReason.trim() } : undefined,
  });
}

export function restoreCommonCode(codePk: number, changeReason?: string): Promise<CommonCode> {
  return rawRequest<CommonCode>(codeUrl(codePk, '/restore'), {
    method: 'POST',
    params: changeReason?.trim() ? { changeReason: changeReason.trim() } : undefined,
  });
}

export function listCodeHistory(
  codePk: number,
  query: CodeListQuery,
): Promise<PageResult<CodeHistory>> {
  return rawRequest<PageResult<CodeHistory>>(codeUrl(codePk, '/history'), {
    method: 'GET',
    params: listParams(query),
  });
}

export async function listGroupCodes(groupCode: string): Promise<CommonCode[]> {
  const first = await listCommonCodes({ groupCode, page: 1, size: 200 });
  const items = [...first.items];
  const size = first.size || 200;
  const pages = Math.ceil(first.total / size);
  for (let page = 2; page <= pages; page += 1) {
    const next = await listCommonCodes({ groupCode, page, size });
    items.push(...next.items);
  }
  return items;
}
