import type { ClientRequest } from 'node:http';

type ApiProxy = {
  [key: string]: {
    target: string;
    changeOrigin: boolean;
    onProxyReq: (proxyReq: ClientRequest) => void;
  };
};

const backendUrl = process.env.DEV_BACKEND_URL;
const tenantCode = process.env.DEV_TENANT_CODE;

function devProxy(): ApiProxy {
  if (!backendUrl) {
    return {};
  }
  const onProxyReq = (proxyReq: ClientRequest) => {
    if (tenantCode) {
      proxyReq.setHeader('X-Tenant-Code', tenantCode);
    }
  };
  return {
    '/api/mdm': {
      target: backendUrl,
      changeOrigin: true,
      onProxyReq,
    },
    '/api/platform': {
      target: backendUrl,
      changeOrigin: true,
      onProxyReq,
    },
  };
}

const proxy: { dev: ApiProxy; test: ApiProxy; pre: ApiProxy } = {
  dev: devProxy(),
  test: {},
  pre: {},
};

export default proxy;

