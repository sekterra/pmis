import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import { refreshAccess, resetRefreshFlight, runWithRefresh } from './refresh';
import { clearAccessToken, getAccessToken, setAccessToken } from './token';

function expired(): ApiError {
  const error = new ApiError('expired');
  error.errorCode = 'AUTH.TOKEN_EXPIRED';
  error.status = 401;
  return error;
}

describe('refresh single-flight', () => {
  afterEach(() => {
    resetRefreshFlight();
    clearAccessToken();
  });

  it('refreshes once for three concurrent expired requests and retries all', async () => {
    let refreshCalls = 0;
    const refresh = () =>
      refreshAccess(
        async () => {
          refreshCalls += 1;
          await new Promise((resolve) => setTimeout(resolve, 20));
          return {
            accessToken: 'new-token',
            tokenType: 'Bearer',
            expiresIn: 900,
            passwordChangeRequired: false,
          };
        },
        { sleep: async () => undefined, onFailure: () => undefined },
      );
    setAccessToken('old-token');
    const results = await Promise.all(
      [1, 2, 3].map(() => {
        let attempts = 0;
        return runWithRefresh(
          async (token) => {
            attempts += 1;
            if (attempts === 1) {
              throw expired();
            }
            return token;
          },
          refresh,
          getAccessToken(),
        );
      }),
    );
    expect(refreshCalls).toBe(1);
    expect(results).toEqual(['new-token', 'new-token', 'new-token']);
  });

  it('retries a 409 once and sends the user to login on refresh 401', async () => {
    const sleep = vi.fn(async () => undefined);
    const onFailure = vi.fn();
    let calls = 0;
    const token = await refreshAccess(
      async () => {
        calls += 1;
        if (calls === 1) {
          const error = new ApiError('busy');
          error.errorCode = 'AUTH.REFRESH_IN_PROGRESS';
          error.status = 409;
          throw error;
        }
        return {
          accessToken: 'after-409',
          tokenType: 'Bearer',
          expiresIn: 900,
          passwordChangeRequired: false,
        };
      },
      { sleep, onFailure },
    );
    expect(token).toBe('after-409');
    expect(sleep).toHaveBeenCalledTimes(1);
    expect(onFailure).not.toHaveBeenCalled();

    resetRefreshFlight();
    calls = 0;
    const failed = await refreshAccess(
      async () => {
        calls += 1;
        const error = new ApiError('reused');
        error.errorCode = 'AUTH.TOKEN_REUSED';
        error.status = 401;
        throw error;
      },
      { sleep, onFailure },
    );
    expect(failed).toBeNull();
    expect(calls).toBe(1);
    expect(onFailure).toHaveBeenCalledTimes(1);
    expect(getAccessToken()).toBeNull();
  });

  it('keeps an access token saved after a refresh attempt started', async () => {
    let release: () => void = () => undefined;
    const gate = new Promise<void>((resolve) => {
      release = () => resolve();
    });
    const onFailure = vi.fn();
    const pending = refreshAccess(
      async () => {
        await gate;
        const error = new ApiError('unauthorized');
        error.errorCode = 'AUTH.UNAUTHORIZED';
        error.status = 401;
        throw error;
      },
      { sleep: async () => undefined, onFailure },
    );
    setAccessToken('login-token');
    release();
    await expect(pending).resolves.toBe('login-token');
    expect(getAccessToken()).toBe('login-token');
    expect(onFailure).not.toHaveBeenCalled();
  });
});
