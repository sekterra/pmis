import type { ApiError } from '@/core/request/errors';
import { toApiError } from '@/core/request/errors';
import type {
  LoginRequest,
  Me,
  PasswordChangeRequest,
  Session,
} from '@/services/mdm/data';
import { AUTH_CHANNEL, type AuthBroadcast, publishLogout } from './broadcast';
import { nextPathAfterRecover } from './guard';
import { homePath } from './mode';
import { safeRedirect } from './redirect';
import { refreshAccess } from './refresh';
import { clearAccessToken, setAccessToken } from './token';

export async function completeLogin(
  login: (body: LoginRequest) => Promise<Session>,
  loadMe: () => Promise<Me>,
  body: LoginRequest,
  redirectQuery?: string | null,
  home = '/welcome',
): Promise<{ path: string; user?: Me }> {
  const session = await login(body);
  setAccessToken(session.accessToken);
  if (session.passwordChangeRequired) {
    return { path: '/user/password' };
  }
  const user = await loadMe();
  return { path: safeRedirect(redirectQuery, home), user };
}

export async function completePasswordChange(
  change: (body: PasswordChangeRequest) => Promise<Session>,
  loadMe: () => Promise<Me>,
  body: PasswordChangeRequest,
): Promise<Me> {
  const session = await change(body);
  setAccessToken(session.accessToken);
  return loadMe();
}

export async function logoutSession(
  callLogout: () => Promise<void>,
  channel?: { postMessage: (data: AuthBroadcast) => void },
): Promise<void> {
  try {
    await callLogout();
  } finally {
    clearAccessToken();
    publishLogout(channel);
  }
}

export async function signOutToLogin(
  callLogout: () => Promise<void>,
  clearUser: () => void,
  goLogin: () => void,
): Promise<void> {
  const channel =
    typeof BroadcastChannel === 'undefined'
      ? undefined
      : new BroadcastChannel(AUTH_CHANNEL);
  try {
    await logoutSession(callLogout, channel);
  } catch {
    // The server call can fail. Local logout still continues.
  } finally {
    channel?.close();
    clearUser();
    goLogin();
  }
}

export function cancelPasswordChange(
  home: string,
  navigation: {
    idx?: number;
    back: () => void;
    replace: (path: string) => void;
  },
): void {
  if (typeof navigation.idx === 'number' && navigation.idx > 0) {
    navigation.back();
    return;
  }
  navigation.replace(home);
}

export function recoverSession(input: {
  refresh: () => Promise<Session>;
  loadMe: () => Promise<Me>;
  sleep: (ms: number) => Promise<void>;
}): Promise<Me | null> {
  return refreshAccess(input.refresh, {
    sleep: input.sleep,
    onFailure: () => undefined,
  }).then(async (token) => {
    if (!token) {
      return null;
    }
    try {
      return await input.loadMe();
    } catch (error) {
      const apiError = toApiError(error);
      if (apiError.errorCode === 'AUTH.ACCOUNT_DISABLED') {
        clearAccessToken();
      }
      return null;
    }
  });
}

export async function startSession(input: {
  pathname: string;
  search: string;
  loadI18n: () => Promise<void>;
  recover: () => Promise<Me | null>;
}): Promise<{ user: Me | null; next: string | null }> {
  await input.loadI18n().catch(() => undefined);
  const user = await input.recover().catch(() => null);
  const redirectQuery = new URLSearchParams(input.search).get('redirect');
  return {
    user,
    next: nextPathAfterRecover({
      pathname: input.pathname,
      search: input.search,
      user,
      redirectQuery,
      home: homePath(),
    }),
  };
}

export function invalidCredentialsMessage(
  error: unknown,
  fallback: string,
): string {
  const apiError = toApiError(error) as ApiError;
  if (apiError.errorCode === 'AUTH.INVALID_CREDENTIALS') {
    return apiError.message || fallback;
  }
  return apiError.message || fallback;
}
