import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import {
  completeLogin,
  completePasswordChange,
  invalidCredentialsMessage,
  cancelPasswordChange,
  logoutSession,
  recoverSession,
  signOutToLogin,
  startSession,
} from './actions';
import { applyRemoteLogout } from './broadcast';
import { getAccessToken, setAccessToken } from './token';

const session = {
  accessToken: 'access',
  tokenType: 'Bearer',
  expiresIn: 900,
  passwordChangeRequired: false,
};

const user = {
  userPk: 1,
  loginId: 'admin@example.com',
  displayName: '관리자',
  supportLanguageCode: 'ko-KR',
  timezoneId: 'Asia/Seoul',
  roleCodes: ['TENANT_ADMIN'],
  passwordChangeRequired: false,
};

describe('auth actions', () => {
  afterEach(() => {
    setAccessToken(null);
    vi.restoreAllMocks();
  });

  it('starts a session after the screen is allowed to paint', async () => {
    const started = await startSession({
      pathname: '/welcome',
      search: '',
      loadI18n: async () => undefined,
      recover: async () => user,
    });
    expect(started.user?.displayName).toBe('관리자');
    expect(started.next).toBeNull();

    const missing = await startSession({
      pathname: '/welcome',
      search: '',
      loadI18n: async () => undefined,
      recover: async () => null,
    });
    expect(missing.next).toContain('/user/login?redirect=');
  });

  it('recovers a session or stays logged out', async () => {
    const loadMe = vi.fn(async () => user);
    const recovered = await recoverSession({
      refresh: async () => session,
      loadMe,
      sleep: async () => undefined,
    });
    expect(recovered?.loginId).toBe('admin@example.com');
    expect(getAccessToken()).toBe('access');

    setAccessToken('stale');
    const missing = await recoverSession({
      refresh: async () => {
        const error = new ApiError('no session');
        error.status = 401;
        error.errorCode = 'AUTH.TOKEN_EXPIRED';
        throw error;
      },
      loadMe,
      sleep: async () => undefined,
    });
    expect(missing).toBeNull();
    expect(getAccessToken()).toBeNull();
  });

  it('branches login success and uses one failure message', async () => {
    const loadMe = vi.fn(async () => user);
    const ok = await completeLogin(
      async () => session,
      loadMe,
      { loginId: 'admin@example.com', password: 'secret' },
      '/welcome',
    );
    expect(ok.path).toBe('/welcome');
    expect(loadMe).toHaveBeenCalledTimes(1);

    const forced = await completeLogin(
      async () => ({
        ...session,
        accessToken: 'temp',
        passwordChangeRequired: true,
      }),
      loadMe,
      { loginId: 'admin@example.com', password: 'secret' },
      '/welcome',
    );
    expect(forced.path).toBe('/user/password');
    expect(getAccessToken()).toBe('temp');

    const error = new ApiError('로그인에 실패했습니다.');
    error.errorCode = 'AUTH.INVALID_CREDENTIALS';
    expect(invalidCredentialsMessage(error, 'fallback')).toBe(
      '로그인에 실패했습니다.',
    );
  });

  it('logs out locally when another tab broadcasts, without calling the server', () => {
    const server = vi.fn();
    setAccessToken('keep');
    applyRemoteLogout();
    expect(server).not.toHaveBeenCalled();
    expect(getAccessToken()).toBeNull();
  });

  it('clears the token and publishes logout after the server call', async () => {
    const channel = { postMessage: vi.fn() };
    setAccessToken('keep');
    await logoutSession(async () => undefined, channel);
    expect(getAccessToken()).toBeNull();
    expect(channel.postMessage).toHaveBeenCalledWith({ type: 'logout' });
  });

  it('clears local state when the logout request fails', async () => {
    const clearUser = vi.fn();
    const goLogin = vi.fn();
    setAccessToken('keep');
    await signOutToLogin(
      async () => {
        throw new Error('down');
      },
      clearUser,
      goLogin,
    );
    expect(getAccessToken()).toBeNull();
    expect(clearUser).toHaveBeenCalled();
    expect(goLogin).toHaveBeenCalled();
  });

  it('returns to the previous screen, or home when there is none', () => {
    const back = vi.fn();
    const replace = vi.fn();
    cancelPasswordChange('/welcome', { idx: 2, back, replace });
    expect(back).toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
    cancelPasswordChange('/welcome', { idx: 0, back, replace });
    expect(replace).toHaveBeenCalledWith('/welcome');
  });

  it('replaces the access token after a password change', async () => {
    setAccessToken('old');
    const profile = await completePasswordChange(
      async () => ({
        ...session,
        accessToken: 'replaced',
        passwordChangeRequired: false,
      }),
      async () => ({ ...user, passwordChangeRequired: false }),
      { currentPassword: 'old-secret', newPassword: 'new-secret' },
    );
    expect(getAccessToken()).toBe('replaced');
    expect(profile.passwordChangeRequired).toBe(false);
  });
});
