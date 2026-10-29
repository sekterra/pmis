import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import { roleListParams } from './rolesQuery';
import type {
  PermissionGrant,
  PermissionReplaceBody,
  Role,
  RoleCreateBody,
  RoleListQuery,
  RoleUpdateBody,
} from './rolesRecord';

const ROLES = '/api/mdm/v1/roles';

function roleUrl(rolePk: number, suffix = ''): string {
  return `${ROLES}/${rolePk}${suffix}`;
}

export function listRoles(query: RoleListQuery): Promise<PageResult<Role>> {
  return rawRequest<PageResult<Role>>(ROLES, { method: 'GET', params: roleListParams(query) });
}

export function getRole(rolePk: number): Promise<Role> {
  return rawRequest<Role>(roleUrl(rolePk), { method: 'GET' });
}

export function createRole(body: RoleCreateBody): Promise<Role> {
  return rawRequest<Role>(ROLES, { method: 'POST', data: body });
}

export function updateRole(rolePk: number, body: RoleUpdateBody): Promise<Role> {
  return rawRequest<Role>(roleUrl(rolePk), { method: 'PUT', data: body });
}

export function deleteRole(rolePk: number): Promise<void> {
  return rawRequest<void>(roleUrl(rolePk), { method: 'DELETE' });
}

export function restoreRole(rolePk: number): Promise<Role> {
  return rawRequest<Role>(roleUrl(rolePk, '/restore'), { method: 'POST' });
}

export function listRolePermissions(rolePk: number, granted?: boolean): Promise<PermissionGrant[]> {
  return rawRequest<PermissionGrant[]>(roleUrl(rolePk, '/permissions'), {
    method: 'GET',
    params: granted === undefined ? undefined : { granted },
  });
}

export function replaceRolePermissions(rolePk: number, body: PermissionReplaceBody): Promise<PermissionGrant[]> {
  return rawRequest<PermissionGrant[]>(roleUrl(rolePk, '/permissions'), { method: 'PUT', data: body });
}
