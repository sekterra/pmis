import type { NameTranslation } from './companyRecord';

export type Role = {
  rolePk: number;
  roleCode: string;
  roleName: string;
  sortOrder: number | null;
  description: string | null;
  useYn: string;
  userCount: number | null;
  createDatetime: string | null;
  createUserPk: number | null;
  updateDatetime: string | null;
  updateUserPk: number | null;
  translations?: NameTranslation[] | null;
};

export type RoleListQuery = {
  page?: number;
  size?: number;
  keyword?: string;
  useYn?: string;
  sort?: string[];
};

export type RoleCreateBody = {
  roleCode: string;
  roleName: string;
  sortOrder: number | null;
  description: string | null;
  translations: NameTranslation[];
};

export type RoleUpdateBody = {
  roleName: string;
  sortOrder: number | null;
  description: string | null;
  translations: NameTranslation[];
};

export type RoleFormValues = {
  roleCode?: string;
  roleName?: string;
  sortOrder?: number | null;
  description?: string;
  names?: Record<string, string | undefined>;
};

export type PermissionGrant = {
  permissionPk: number;
  permissionCode: string;
  menuCode: string;
  menuName: string;
  actionCode: string;
  permissionName: string;
  granted: boolean;
};

export type PermissionReplaceBody = {
  permissionCodes: string[];
};
