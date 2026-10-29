import { emptyToNull } from './companyQuery';
import type { RoleGrantBody, UserCreateBody, UserFormValues, UserListQuery, UserUpdateBody } from './usersRecord';

const SORTS = new Set(['loginId', 'userStatusCode', 'lastLoginDatetime', 'createDatetime', 'userPk']);

function textOrNull(value: string | null | undefined): string | null {
  return emptyToNull(value);
}

export function userListParams(input: UserListQuery): Record<string, string | number | string[]> {
  const params: Record<string, string | number | string[]> = {
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
  const keyword = input.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  const userStatusCode = input.userStatusCode?.trim();
  if (userStatusCode) {
    params.userStatusCode = userStatusCode;
  }
  const accountTypeCode = input.accountTypeCode?.trim();
  if (accountTypeCode) {
    params.accountTypeCode = accountTypeCode;
  }
  const roleCode = input.roleCode?.trim();
  if (roleCode) {
    params.roleCode = roleCode;
  }
  const useYn = input.useYn?.trim().toUpperCase();
  if (useYn === 'Y' || useYn === 'N') {
    params.useYn = useYn;
  }
  const sort = (input.sort ?? []).filter((item) => SORTS.has(item.split(',')[0] ?? ''));
  if (sort.length > 0) {
    params.sort = sort;
  }
  return params;
}

export function toCreateBody(values: UserFormValues): UserCreateBody {
  const roles = (values.roleCodes ?? []).map((code) => code.trim()).filter((code) => code.length > 0);
  return {
    loginId: values.loginId?.trim() ?? '',
    accountTypeCode: textOrNull(values.accountTypeCode),
    personPk: values.personPk ?? null,
    supportLanguageCode: textOrNull(values.supportLanguageCode),
    timezoneId: textOrNull(values.timezoneId),
    description: textOrNull(values.description),
    roleCodes: roles.length > 0 ? roles : null,
  };
}

export function toUpdateBody(values: UserFormValues): UserUpdateBody {
  return {
    personPk: values.personPk ?? null,
    supportLanguageCode: textOrNull(values.supportLanguageCode),
    timezoneId: textOrNull(values.timezoneId),
    description: textOrNull(values.description),
  };
}

export function toRoleGrantBody(grants: RoleGrantBody[]): RoleGrantBody[] {
  return grants.map((grant) => ({
    roleCode: grant.roleCode.trim(),
    validFromDate: textOrNull(grant.validFromDate),
    validToDate: textOrNull(grant.validToDate),
  }));
}

export function toStatusBody(userStatusCode: string): { userStatusCode: string } {
  return { userStatusCode };
}
