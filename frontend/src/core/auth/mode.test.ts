import { describe, expect, it } from 'vitest';
import { homePath, resolveAuthMode } from '@/core/auth/mode';
import { tenantHeaderFor } from '@/core/request/tenantHeader';
import { platformAuthUrl } from '@/services/platform/auth';

describe('platform auth routing', () => {
  it('uses the platform auth path and cookie and skips the tenant header', () => {
    const mode = resolveAuthMode('platform');
    expect(mode.authBasePath).toBe('/api/platform/v1/auth');
    expect(mode.refreshCookieName).toBe('mdm_platform_refresh');
    expect(platformAuthUrl('/login')).toBe('/api/platform/v1/auth/login');
    expect(homePath('platform')).toBe('/tenants');
    expect(tenantHeaderFor('/api/platform', 'acme')).toBeUndefined();
    expect(tenantHeaderFor('/api/mdm', 'acme')).toBe('acme');
  });
});
