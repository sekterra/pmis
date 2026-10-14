import { type ApiError, toApiError } from '@/core/request/errors';
import type { Session } from '@/services/mdm/data';
import { accessTokenEpoch, clearAccessToken, getAccessToken, setAccessToken } from './token';

export function refreshRetryDelay(random: () => number = Math.random): number {
  return 300 + Math.floor(random() * 701);
}

let flight: Promise<string | null> | null = null;

export function resetRefreshFlight(): void {
  flight = null;
}

async function attempt(
  run: () => Promise<Session>,
  sleep: (ms: number) => Promise<void>,
  delayMs: () => number,
  onFailure: (error: ApiError) => void,
  retried: number,
): Promise<string | null> {
  const epoch = accessTokenEpoch();
  try {
    const session = await run();
    setAccessToken(session.accessToken);
    return session.accessToken;
  } catch (error) {
    const apiError = toApiError(error);
    if (apiError.errorCode === 'AUTH.REFRESH_IN_PROGRESS' && retried === 0) {
      await sleep(delayMs());
      return attempt(run, sleep, delayMs, onFailure, 1);
    }
    if (accessTokenEpoch() !== epoch) {
      return getAccessToken();
    }
    clearAccessToken();
    onFailure(apiError);
    return null;
  }
}

export function refreshAccess(
  run: () => Promise<Session>,
  options: {
    sleep: (ms: number) => Promise<void>;
    delayMs?: () => number;
    onFailure: (error: ApiError) => void;
  },
): Promise<string | null> {
  if (!flight) {
    flight = attempt(
      run,
      options.sleep,
      options.delayMs ?? refreshRetryDelay,
      options.onFailure,
      0,
    ).finally(() => {
      flight = null;
    });
  }
  return flight;
}

export async function runWithRefresh<T>(
  execute: (token: string | null) => Promise<T>,
  refresh: () => Promise<string | null>,
  token: string | null,
): Promise<T> {
  try {
    return await execute(token);
  } catch (error) {
    const apiError = toApiError(error);
    if (apiError.errorCode !== 'AUTH.TOKEN_EXPIRED' || apiError.retried) {
      throw apiError;
    }
    const next = await refresh();
    if (!next) {
      throw apiError;
    }
    try {
      return await execute(next);
    } catch (second) {
      const again = toApiError(second);
      again.retried = true;
      throw again;
    }
  }
}
