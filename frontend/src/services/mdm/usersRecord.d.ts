import type { PageResult } from './data';

export type TenantUser = {
  userPk: number;
  loginId: string;
  accountTypeCode: string;
  userStatusCode: string;
  personPk: number | null;
  personName: string | null;
  lastLoginDatetime: string | null;
  mustChangePasswordYn: string;
  roleCodes: string[];
  useYn: string;
  supportLanguageCode?: string | null;
  timezoneId?: string | null;
  description?: string | null;
  loginFailCount?: number | null;
  lockedDatetime?: string | null;
  lockType?: string | null;
  passwordChangedDatetime?: string | null;
  passwordExpireDatetime?: string | null;
  createDatetime?: string | null;
  createUserPk?: number | null;
  updateDatetime?: string | null;
  updateUserPk?: number | null;
};

export type UserListQuery = {
  page?: number;
  size?: number;
  sort?: string[];
  keyword?: string;
  userStatusCode?: string;
  accountTypeCode?: string;
  roleCode?: string;
  useYn?: string;
};

export type UserFormValues = {
  loginId?: string;
  accountTypeCode?: string;
  personPk?: number | null;
  supportLanguageCode?: string;
  timezoneId?: string;
  description?: string;
  roleCodes?: string[];
};

export type UserCreateBody = {
  loginId: string;
  accountTypeCode: string | null;
  personPk: number | null;
  supportLanguageCode: string | null;
  timezoneId: string | null;
  description: string | null;
  roleCodes: string[] | null;
};

export type UserUpdateBody = {
  personPk: number | null;
  supportLanguageCode: string | null;
  timezoneId: string | null;
  description: string | null;
};

export type IssuedPassword = {
  userPk: number;
  loginId: string;
  temporaryPassword: string;
  passwordExpireDatetime: string | null;
  mustChangePasswordYn: string;
};

export type UserRoleGrant = {
  userRolePk: number;
  roleCode: string;
  roleName: string;
  validFromDate: string | null;
  validToDate: string | null;
  currentlyValid: boolean;
  useYn: string;
};

export type RoleGrantBody = {
  roleCode: string;
  validFromDate: string | null;
  validToDate: string | null;
};

export type UserStatusBody = {
  userStatusCode: string;
};

export type PageOf<T> = PageResult<T>;
