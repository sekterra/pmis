export type AccessKind = 'PLATFORM' | 'TENANT';

export type AccessContext = {
  kind: AccessKind;
  tenantCode?: string;
  tenantName?: string;
};

export type ContextStatus = 'ready' | 'offline' | 'unknown';

type ContextResponse = {
  success?: boolean;
  data?: AccessContext;
  errorCode?: string | null;
};

let current: AccessContext = { kind: 'TENANT' };
let status: ContextStatus | 'pending' = 'pending';

export function accessContext(): AccessContext {
  return current;
}

export function accessKind(): AccessKind {
  return current.kind;
}

export function contextStatus(): ContextStatus | 'pending' {
  return status;
}

export function setAccessContext(next: AccessContext): void {
  current = next;
  status = 'ready';
}

export function resetAccessContext(): void {
  current = { kind: 'TENANT' };
  status = 'pending';
}

export function authBasePath(): string {
  return accessKind() === 'PLATFORM' ? '/api/platform/v1/auth' : '/api/mdm/v1/auth';
}

export function refreshCookieName(): string {
  return accessKind() === 'PLATFORM' ? 'mdm_platform_refresh' : 'mdm_refresh';
}

export function homePath(): string {
  return accessKind() === 'PLATFORM' ? '/tenants' : '/welcome';
}

export function areaForbidden(pathname: string): boolean {
  if (accessKind() === 'PLATFORM') {
    return pathname === '/mdm' || pathname.startsWith('/mdm/');
  }
  return pathname === '/tenants' || pathname.startsWith('/tenants/');
}

export async function loadAccessContext(
  fetcher: () => Promise<ContextResponse | 'offline'> = fetchContext,
): Promise<ContextStatus> {
  try {
    const body = await fetcher();
    if (body === 'offline') {
      status = 'offline';
      return status;
    }
    if (body.success && (body.data?.kind === 'PLATFORM' || body.data?.kind === 'TENANT')) {
      current = {
        kind: body.data.kind,
        tenantCode: body.data.tenantCode,
        tenantName: body.data.tenantName,
      };
      status = 'ready';
      return status;
    }
    status = unknownCode(body.errorCode) ? 'unknown' : 'offline';
    return status;
  } catch {
    status = 'offline';
    return status;
  }
}

function unknownCode(code: string | null | undefined): boolean {
  return code === 'TENANT.NOT_FOUND' || code === 'TENANT.UNRESOLVED' || code === 'TENANT.INACTIVE';
}

async function fetchContext(): Promise<ContextResponse | 'offline'> {
  let response: Response;
  try {
    response = await fetch('/api/mdm/v1/context', { headers: { Accept: 'application/json' } });
  } catch {
    return 'offline';
  }
  if (response.status >= 500) {
    return 'offline';
  }
  return (await response.json()) as ContextResponse;
}
