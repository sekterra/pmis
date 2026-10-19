import { beforeEach, describe, expect, it, vi } from 'vitest';
import { login, logout, refresh } from '@/core/auth/authApi';
import {
  authBasePath,
  homePath,
  loadAccessContext,
  refreshCookieName,
  resetAccessContext,
  setAccessContext,
} from '@/core/auth/context';
import { tenantHeaderFor } from '@/core/request/tenantHeader';
import * as platformAuth from '@/services/platform/auth';
import * as tenantAuth from '@/services/mdm/auth';

vi.mock('@/services/mdm/auth', () => ({
  login: vi.fn(async () => ({ accessToken: 'tenant' })),
  refresh: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
  me: vi.fn(),
}));

vi.mock('@/services/platform/auth', () => ({
  login: vi.fn(async () => ({ accessToken: 'platform' })),
  refresh: vi.fn(),
  logout: vi.fn(),
  changePassword: vi.fn(),
  me: vi.fn(),
  platformAuthUrl: (path: string) => `/api/platform/v1/auth${path}`,
}));

describe('runtime access context', () => {
  beforeEach(() => {
    resetAccessContext();
  });

  it('uses the platform auth path after a platform context', async () => {
    const status = await loadAccessContext(async () => ({
      success: true,
      data: { kind: 'PLATFORM' },
    }));
    expect(status).toBe('ready');
    expect(authBasePath()).toBe('/api/platform/v1/auth');
    expect(refreshCookieName()).toBe('mdm_platform_refresh');
    expect(homePath()).toBe('/tenants');
    expect(platformAuth.platformAuthUrl('/login')).toBe('/api/platform/v1/auth/login');
    expect(tenantHeaderFor('/api/platform', 'acme')).toBeUndefined();
    expect(tenantHeaderFor('/api/mdm', 'acme')).toBe('acme');
    await expect(login({ loginId: 'op', password: 'secret' })).resolves.toEqual({
      accessToken: 'platform',
    });
    await refresh();
    await logout();
    expect(platformAuth.refresh).toHaveBeenCalledTimes(1);
    expect(platformAuth.logout).toHaveBeenCalledTimes(1);
    expect(tenantAuth.refresh).not.toHaveBeenCalled();
  });

  it('uses the tenant auth path and name after a tenant context', async () => {
    const status = await loadAccessContext(async () => ({
      success: true,
      data: { kind: 'TENANT', tenantCode: 'acme', tenantName: 'Acme' },
    }));
    expect(status).toBe('ready');
    expect(authBasePath()).toBe('/api/mdm/v1/auth');
    expect(refreshCookieName()).toBe('mdm_refresh');
    expect(homePath()).toBe('/welcome');
    await expect(login({ loginId: 'user', password: 'secret' })).resolves.toEqual({
      accessToken: 'tenant',
    });
    setAccessContext({ kind: 'TENANT' });
  });

  it('hides the site when the tenant cannot be resolved', async () => {
    await expect(
      loadAccessContext(async () => ({ success: false, errorCode: 'TENANT.NOT_FOUND' })),
    ).resolves.toBe('unknown');
    await expect(
      loadAccessContext(async () => ({ success: false, errorCode: 'TENANT.UNRESOLVED' })),
    ).resolves.toBe('unknown');
  });

  it('reports the server as unreachable on network and 5xx failures', async () => {
    await expect(loadAccessContext(async () => 'offline')).resolves.toBe('offline');
    await expect(loadAccessContext(async () => { throw new Error('down'); })).resolves.toBe('offline');
  });
});
