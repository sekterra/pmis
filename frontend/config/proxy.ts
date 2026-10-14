import { createDevProxy } from '../src/core/dev/proxyRules';

const proxy = {
  dev: createDevProxy(process.env.DEV_BACKEND_URL, process.env.DEV_TENANT_CODE),
  test: {},
  pre: {},
};

export default proxy;
