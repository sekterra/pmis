import { emptyToNull, toNameTranslations } from './companyQuery';
import type { RoleCreateBody, RoleFormValues, RoleListQuery, RoleUpdateBody } from './rolesRecord';

const SORTS = new Set(['roleCode', 'roleName', 'sortOrder', 'rolePk']);

export function roleListParams(input: RoleListQuery): Record<string, string | number | string[]> {
  const params: Record<string, string | number | string[]> = {
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
  const keyword = input.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
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

function roleText(values: RoleFormValues): Pick<RoleCreateBody, 'roleName' | 'sortOrder' | 'description' | 'translations'> {
  return {
    roleName: values.roleName?.trim() ?? '',
    sortOrder: values.sortOrder ?? null,
    description: emptyToNull(values.description),
    translations: toNameTranslations(values.names),
  };
}

export function toCreateBody(values: RoleFormValues): RoleCreateBody {
  return {
    roleCode: values.roleCode?.trim() ?? '',
    ...roleText(values),
  };
}

export function toUpdateBody(values: RoleFormValues): RoleUpdateBody {
  return roleText(values);
}

export function namesFromRole(role: { translations?: { supportLanguageCode: string; text: string }[] | null } | undefined) {
  const names: Record<string, string> = {};
  for (const item of role?.translations ?? []) {
    names[item.supportLanguageCode] = item.text;
  }
  return names;
}
