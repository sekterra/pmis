export type AppMode = 'tenant' | 'platform';

export type AuthModeConfig = {
  mode: AppMode;
  authBasePath: string;
  refreshCookieName: string;
};

export function resolveAuthMode(mode: string | undefined): AuthModeConfig {
  if (mode === 'platform') {
    return {
      mode: 'platform',
      authBasePath: '/api/platform/v1/auth',
      refreshCookieName: 'mdm_platform_refresh',
    };
  }
  return {
    mode: 'tenant',
    authBasePath: '/api/mdm/v1/auth',
    refreshCookieName: 'mdm_refresh',
  };
}

export const authMode = resolveAuthMode(process.env.APP_MODE);

export function homePath(mode: AppMode = authMode.mode): string {
  return mode === 'platform' ? '/tenants' : '/welcome';
}
