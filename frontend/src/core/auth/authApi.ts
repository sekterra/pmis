import * as tenantAuth from '@/services/mdm/auth';
import * as platformAuth from '@/services/platform/auth';
import { accessKind } from './context';

function api() {
  return accessKind() === 'PLATFORM' ? platformAuth : tenantAuth;
}

export function login(body: { loginId: string; password: string }) {
  return api().login(body);
}

export function refresh() {
  return api().refresh();
}

export function logout() {
  return api().logout();
}

export function changePassword(body: { currentPassword: string; newPassword: string }) {
  return api().changePassword(body);
}

export function me() {
  return api().me();
}
