export function tenantHeaderFor(
  apiPrefix: string,
  tenantCode: string | undefined,
): string | undefined {
  if (!tenantCode) {
    return undefined;
  }
  if (apiPrefix === '/api/platform' || apiPrefix.startsWith('/api/platform/')) {
    return undefined;
  }
  if (apiPrefix === '/api/mdm' || apiPrefix.startsWith('/api/mdm/')) {
    return tenantCode;
  }
  return undefined;
}
