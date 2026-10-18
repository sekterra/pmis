import { rawRequest } from '@/core/request/runtime';
import type { Me, PasswordChangeRequest, Session } from '@/services/mdm/data';
import type { PlatformMe } from './data';

export const PLATFORM_AUTH_BASE = '/api/platform/v1/auth';

export function platformAuthUrl(path: string): string {
  return `${PLATFORM_AUTH_BASE}${path}`;
}

export function login(body: { loginId: string; password: string }): Promise<Session> {
  return rawRequest<Session>(platformAuthUrl('/login'), {
    method: 'POST',
    data: body,
    skipAuth: true,
    skipAuthRetry: true,
  });
}

export function refresh(): Promise<Session> {
  return rawRequest<Session>(platformAuthUrl('/refresh'), {
    method: 'POST',
    skipAuth: true,
    skipAuthRetry: true,
    headers: { 'X-MDM-Request': '1' },
  });
}

export function logout(): Promise<void> {
  return rawRequest<void>(platformAuthUrl('/logout'), {
    method: 'POST',
    skipAuthRetry: true,
    headers: { 'X-MDM-Request': '1' },
  });
}

export function changePassword(body: PasswordChangeRequest): Promise<Session> {
  return rawRequest<Session>(platformAuthUrl('/password/change'), {
    method: 'POST',
    data: body,
  });
}

export async function me(): Promise<Me> {
  const profile = await rawRequest<PlatformMe>(platformAuthUrl('/me'), { method: 'GET' });
  return {
    userPk: profile.platformAdminPk,
    loginId: profile.loginId,
    displayName: profile.adminName,
    supportLanguageCode: '',
    timezoneId: '',
    roleCodes: [],
    passwordChangeRequired: profile.passwordChangeRequired,
  };
}
