import { request } from '@umijs/max';
import { unwrapData } from '@/core/request/errors';
import type { Bundle, SupportLanguage } from './data';

function readEtag(headers: unknown): string | undefined {
  if (!headers || typeof headers !== 'object') {
    return undefined;
  }
  const bag = headers as {
    etag?: unknown;
    get?: (name: string) => unknown;
  };
  const fromGetter = bag.get?.('etag');
  if (typeof fromGetter === 'string') {
    return fromGetter;
  }
  return typeof bag.etag === 'string' ? bag.etag : undefined;
}

export async function fetchLanguages(): Promise<SupportLanguage[]> {
  const payload = await request('/api/mdm/v1/i18n/languages', {
    method: 'GET',
    skipErrorHandler: true,
    skipAuth: true,
  });
  return unwrapData<SupportLanguage[]>(payload);
}

export async function fetchBundle(
  namespace: string,
  lang: string,
  etag?: string,
): Promise<{ status: number; etag?: string | null; body?: Bundle }> {
  const response = (await request(`/api/mdm/v1/i18n/bundles/${namespace}`, {
    method: 'GET',
    skipErrorHandler: true,
    skipAuth: true,
    getResponse: true,
    params: { lang },
    headers: etag ? { 'If-None-Match': etag } : undefined,
    validateStatus: (status: number) => status === 304 || (status >= 200 && status < 300),
  })) as { status: number; headers?: unknown; data?: Bundle };
  const status = response.status as number;
  if (status === 304) {
    return { status, etag: readEtag(response.headers) ?? etag };
  }
  return {
    status,
    etag: readEtag(response.headers),
    body: response.data as Bundle,
  };
}
