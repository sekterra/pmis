import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import { toStatusBody, userListParams } from './usersQuery';
import type {
  IssuedPassword,
  RoleGrantBody,
  TenantUser,
  UserCreateBody,
  UserListQuery,
  UserRoleGrant,
  UserUpdateBody,
} from './usersRecord';

const USERS = '/api/mdm/v1/users';

function userUrl(userPk: number, suffix = ''): string {
  return `${USERS}/${userPk}${suffix}`;
}

export function listUsers(query: UserListQuery): Promise<PageResult<TenantUser>> {
  return rawRequest<PageResult<TenantUser>>(USERS, { method: 'GET', params: userListParams(query) });
}

export function getUser(userPk: number): Promise<TenantUser> {
  return rawRequest<TenantUser>(userUrl(userPk), { method: 'GET' });
}

export function createUser(body: UserCreateBody): Promise<IssuedPassword> {
  return rawRequest<IssuedPassword>(USERS, { method: 'POST', data: body });
}

export function updateUser(userPk: number, body: UserUpdateBody): Promise<TenantUser> {
  return rawRequest<TenantUser>(userUrl(userPk), { method: 'PUT', data: body });
}

export function deleteUser(userPk: number): Promise<void> {
  return rawRequest<void>(userUrl(userPk), { method: 'DELETE' });
}

export function restoreUser(userPk: number): Promise<TenantUser> {
  return rawRequest<TenantUser>(userUrl(userPk, '/restore'), { method: 'POST' });
}

export function changeUserStatus(userPk: number, userStatusCode: string): Promise<TenantUser> {
  return rawRequest<TenantUser>(userUrl(userPk, '/status'), { method: 'PUT', data: toStatusBody(userStatusCode) });
}

export function resetUserPassword(userPk: number): Promise<IssuedPassword> {
  return rawRequest<IssuedPassword>(userUrl(userPk, '/password/reset'), { method: 'POST' });
}

export function listUserRoles(userPk: number, useYn?: string): Promise<UserRoleGrant[]> {
  const flag = useYn?.trim().toUpperCase();
  return rawRequest<UserRoleGrant[]>(userUrl(userPk, '/roles'), {
    method: 'GET',
    params: flag === 'Y' || flag === 'N' ? { useYn: flag } : undefined,
  });
}

export function replaceUserRoles(userPk: number, body: RoleGrantBody[]): Promise<UserRoleGrant[]> {
  return rawRequest<UserRoleGrant[]>(userUrl(userPk, '/roles'), { method: 'PUT', data: body });
}
