import type { NationListQuery } from './languageRecord';

const NATION_SORTS = new Set(['nationCode', 'nationName', 'sortOrder', 'nationPk']);

export function languageListParams(useYn?: string): Record<string, string> {
  const flag = useYn?.trim().toUpperCase();
  if (flag === 'Y' || flag === 'N') {
    return { useYn: flag };
  }
  return {};
}

export function nationListParams(
  input: NationListQuery,
): Record<string, string | number | boolean | string[]> {
  const params: Record<string, string | number | boolean | string[]> = {
    managed: true,
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
  const sort = (input.sort ?? []).filter((item) => NATION_SORTS.has(item.split(',')[0] ?? ''));
  if (sort.length > 0) {
    params.sort = sort;
  }
  return params;
}
