import { accessKind, authBasePath, homePath, refreshCookieName } from './context';

export type AppMode = 'tenant' | 'platform';

export function appMode(): AppMode {
  return accessKind() === 'PLATFORM' ? 'platform' : 'tenant';
}

export { authBasePath, homePath, refreshCookieName };
