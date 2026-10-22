import type { RequestConfig } from '@umijs/max';
import { request } from '@umijs/max';
import { getAccessToken } from '@/core/auth/token';
import { ApiError, presentError, toApiError, toBlobApiError, unwrapData } from './errors';
import { getFeedback } from './feedback';

type RetryRunner = () => Promise<string | null>;
type Navigate = () => void;

let refreshRunner: RetryRunner = async () => null;
let goLogin: Navigate = () => undefined;
let goPassword: Navigate = () => undefined;
let onAccountDisabled: (error: ApiError) => void = () => undefined;

export function registerRequestRuntime(handlers: {
  refresh: RetryRunner;
  goLogin: Navigate;
  goPassword: Navigate;
  onAccountDisabled: (error: ApiError) => void;
}): void {
  refreshRunner = handlers.refresh;
  goLogin = handlers.goLogin;
  goPassword = handlers.goPassword;
  onAccountDisabled = handlers.onAccountDisabled;
}

export function headersForBody(
  headers: Record<string, string> | undefined,
  data: unknown,
): Record<string, string> | undefined {
  if (!(typeof FormData !== 'undefined' && data instanceof FormData) || !headers) {
    return headers;
  }
  const next = { ...headers };
  delete next['Content-Type'];
  delete next['content-type'];
  return next;
}

function isPublicAuth(url: string): boolean {
  return /\/auth\/(login|refresh|jwks)(\?|$)/.test(url);
}

export function createRequestConfig(): RequestConfig {
  return {
    withCredentials: true,
    timeout: 15000,
    errorConfig: {
      errorThrower: (body) => {
        throw toApiError(body);
      },
      errorHandler: (error, opts) => {
        if (opts?.skipErrorHandler) {
          throw error;
        }
        const apiError = toApiError(error);
        if (apiError.errorCode === 'AUTH.TOKEN_EXPIRED') {
          throw apiError;
        }
        if (apiError.errorCode === 'AUTH.PASSWORD_CHANGE_REQUIRED') {
          goPassword();
          throw apiError;
        }
        if (apiError.errorCode === 'AUTH.ACCOUNT_DISABLED') {
          presentError(apiError, getFeedback(), goLogin);
          onAccountDisabled(apiError);
          throw apiError;
        }
        presentError(apiError, getFeedback(), goLogin);
        throw apiError;
      },
    },
    requestInterceptors: [
      (config: {
        url?: string;
        headers?: Record<string, string>;
        skipAuth?: boolean;
        data?: unknown;
      }) => {
        config.headers = headersForBody(config.headers, config.data);
        const url = config.url ?? '';
        const token = getAccessToken();
        if (token && !config.skipAuth && !isPublicAuth(url)) {
          config.headers = {
            ...config.headers,
            Authorization: `Bearer ${token}`,
          };
        }
        return config;
      },
    ] as RequestConfig['requestInterceptors'],
    responseInterceptors: [
      [
        (response: unknown) => response,
        async (error: {
          config?: {
            url?: string;
            _retried?: boolean;
            skipAuthRetry?: boolean;
            headers?: Record<string, string>;
          };
          response?: { status?: number; data?: { errorCode?: string } };
        }) => {
          const config = error.config;
          const code = error.response?.data?.errorCode;
          if (
            !config ||
            config._retried ||
            config.skipAuthRetry ||
            error.response?.status !== 401 ||
            code !== 'AUTH.TOKEN_EXPIRED'
          ) {
            throw error;
          }
          const token = await refreshRunner();
          if (!token) {
            throw error;
          }
          return request(config.url ?? '', {
            ...config,
            _retried: true,
            skipAuthRetry: true,
            headers: {
              ...config.headers,
              Authorization: `Bearer ${token}`,
            },
          });
        },
      ],
    ] as RequestConfig['responseInterceptors'],
  };
}

export async function rawRequest<T>(
  url: string,
  options: Record<string, unknown> = {},
): Promise<T> {
  try {
    const payload = await request(url, {
      ...options,
      skipErrorHandler: true,
    });
    return unwrapData<T>(payload, {
      wrapped: options.wrapped !== false,
    });
  } catch (error) {
    throw toApiError(error);
  }
}

export async function rawBlob(url: string): Promise<Blob> {
  try {
    const payload = await request(url, {
      responseType: 'blob',
      skipErrorHandler: true,
    });
    if (payload instanceof Blob) {
      return payload;
    }
    throw new ApiError('Request failed');
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw await toBlobApiError(error);
  }
}
