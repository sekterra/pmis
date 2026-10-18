import { authMode } from '@/core/auth/mode';
import { rawRequest } from '@/core/request/runtime';
import type { LoginRequest, Me, PasswordChangeRequest, Session } from './data';

function authUrl(path: string): string {
  return `${authMode.authBasePath}${path}`;
}

export function login(body: LoginRequest): Promise<Session> {
  return rawRequest<Session>(authUrl('/login'), {
    method: 'POST',
    data: body,
    skipAuth: true,
    skipAuthRetry: true,
  });
}

export function refresh(): Promise<Session> {
  return rawRequest<Session>(authUrl('/refresh'), {
    method: 'POST',
    skipAuth: true,
    skipAuthRetry: true,
    headers: { 'X-MDM-Request': '1' },
  });
}

export function logout(): Promise<void> {
  return rawRequest<void>(authUrl('/logout'), {
    method: 'POST',
    skipAuthRetry: true,
    headers: { 'X-MDM-Request': '1' },
  });
}

export function changePassword(body: PasswordChangeRequest): Promise<Session> {
  return rawRequest<Session>(authUrl('/password/change'), {
    method: 'POST',
    data: body,
  });
}

export function me(): Promise<Me> {
  return rawRequest<Me>(authUrl('/me'), { method: 'GET' });
}
