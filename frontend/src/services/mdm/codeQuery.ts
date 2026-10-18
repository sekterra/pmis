import type { CodeListQuery } from './code';

export function listParams(
  input: CodeListQuery,
): Record<string, string | number | string[]> {
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
  const solutionCode = input.solutionCode?.trim();
  if (solutionCode) {
    params.solutionCode = solutionCode;
  }
  const groupCode = input.groupCode?.trim();
  if (groupCode) {
    params.groupCode = groupCode;
  }
  if (input.sort && input.sort.length > 0) {
    params.sort = input.sort;
  }
  return params;
}
