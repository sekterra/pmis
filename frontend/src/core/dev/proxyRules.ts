import type { ClientRequest } from 'node:http';
import { tenantHeaderFor } from '../request/tenantHeader.ts';

export const MISSING_BACKEND_MESSAGE =
  '백엔드 주소가 없어 /api 요청이 프록시되지 않습니다. frontend/.env.local에 DEV_BACKEND_URL을 설정하세요';

export type ApiProxy = {
  [key: string]: {
    target: string;
    changeOrigin: boolean;
    onProxyReq: (proxyReq: ClientRequest) => void;
  };
};

export function backendMissingNotice(
  backendUrl: string | undefined,
): string | undefined {
  if (backendUrl?.trim()) {
    return undefined;
  }
  return MISSING_BACKEND_MESSAGE;
}

export function createDevProxy(
  backendUrl: string | undefined,
  tenantCode: string | undefined,
): ApiProxy {
  const target = backendUrl?.trim();
  if (!target) {
    return {};
  }
  const onProxyReq = (prefix: string) => (proxyReq: ClientRequest) => {
    const value = tenantHeaderFor(prefix, tenantCode);
    if (value) {
      proxyReq.setHeader('X-Tenant-Code', value);
    }
  };
  return {
    '/api/mdm': {
      target,
      changeOrigin: true,
      onProxyReq: onProxyReq('/api/mdm'),
    },
    '/api/platform': {
      target,
      changeOrigin: true,
      onProxyReq: onProxyReq('/api/platform'),
    },
  };
}
