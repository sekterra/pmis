import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import type { LanguageBody, LanguageRecord, NationBody, NationListQuery, NationRecord } from './languageRecord';
import { languageListParams, nationListParams } from './languageQuery';

const LANGUAGES = '/api/mdm/v1/support-languages';
const NATIONS = '/api/mdm/v1/nations';

function languageUrl(code: string, suffix = ''): string {
  return `${LANGUAGES}/${encodeURIComponent(code)}${suffix}`;
}

function nationUrl(code: string, suffix = ''): string {
  return `${NATIONS}/${encodeURIComponent(code)}${suffix}`;
}

export function listSupportLanguages(useYn?: string): Promise<LanguageRecord[]> {
  return rawRequest<LanguageRecord[]>(LANGUAGES, {
    method: 'GET',
    params: languageListParams(useYn),
  });
}

export function createSupportLanguage(body: LanguageBody): Promise<LanguageRecord> {
  return rawRequest<LanguageRecord>(LANGUAGES, { method: 'POST', data: body });
}

export function updateSupportLanguage(code: string, body: LanguageBody): Promise<LanguageRecord> {
  return rawRequest<LanguageRecord>(languageUrl(code), { method: 'PUT', data: body });
}

export function deleteSupportLanguage(code: string): Promise<void> {
  return rawRequest<void>(languageUrl(code), { method: 'DELETE' });
}

export function restoreSupportLanguage(code: string): Promise<LanguageRecord> {
  return rawRequest<LanguageRecord>(languageUrl(code, '/restore'), { method: 'POST' });
}

export function listNations(query: NationListQuery): Promise<PageResult<NationRecord>> {
  return rawRequest<PageResult<NationRecord>>(NATIONS, {
    method: 'GET',
    params: nationListParams(query),
  });
}

export function createNation(body: NationBody): Promise<NationRecord> {
  return rawRequest<NationRecord>(NATIONS, { method: 'POST', data: body });
}

export function updateNation(code: string, body: NationBody): Promise<NationRecord> {
  return rawRequest<NationRecord>(nationUrl(code), { method: 'PUT', data: body });
}

export function deleteNation(code: string): Promise<void> {
  return rawRequest<void>(nationUrl(code), { method: 'DELETE' });
}

export function restoreNation(code: string): Promise<NationRecord> {
  return rawRequest<NationRecord>(nationUrl(code, '/restore'), { method: 'POST' });
}
