import { emptyToNull, toNameTranslations } from './companyQuery';
import type {
  Dept,
  DeptCreateBody,
  DeptFormValues,
  DeptListQuery,
  DeptTreeQuery,
  DeptUpdateBody,
} from './deptRecord';

const LIST_SORTS = new Set(['deptCode', 'deptName', 'levelNo', 'sortOrder', 'deptPk']);
const HISTORY_SORTS = new Set(['changeDatetime', 'effectiveDate', 'historyPk']);

function pageSize(input: { page?: number; size?: number }): { page: number; size: number } {
  return {
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
}

function allowedSort(sort: string[] | undefined, fields: Set<string>): string[] {
  return (sort ?? []).filter((item) => fields.has(item.split(',')[0] ?? ''));
}

export function deptListParams(input: DeptListQuery): Record<string, string | number | string[]> {
  const params: Record<string, string | number | string[]> = pageSize(input);
  const companyCode = input.companyCode?.trim();
  if (companyCode) {
    params.companyCode = companyCode;
  }
  const keyword = input.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  const deptTypeCode = input.deptTypeCode?.trim();
  if (deptTypeCode) {
    params.deptTypeCode = deptTypeCode;
  }
  const useYn = input.useYn?.trim().toUpperCase();
  if (useYn === 'Y' || useYn === 'N') {
    params.useYn = useYn;
  }
  if (input.parentDeptPk) {
    params.parentDeptPk = input.parentDeptPk;
  }
  const sort = allowedSort(input.sort, LIST_SORTS);
  if (sort.length > 0) {
    params.sort = sort;
  }
  return params;
}

export function deptTreeParams(input: DeptTreeQuery): Record<string, string | boolean> {
  const params: Record<string, string | boolean> = {};
  const companyCode = input.companyCode?.trim();
  if (companyCode) {
    params.companyCode = companyCode;
  }
  const asOfDate = input.asOfDate?.trim();
  if (asOfDate) {
    params.asOfDate = asOfDate;
  }
  if (input.includeClosed) {
    params.includeClosed = true;
  }
  return params;
}

export function deptHistoryParams(input: { page?: number; size?: number; sort?: string[] }): Record<string, number | string[]> {
  const params: Record<string, number | string[]> = pageSize(input);
  const sort = allowedSort(input.sort, HISTORY_SORTS);
  if (sort.length > 0) {
    params.sort = sort;
  }
  return params;
}

export function toCreateBody(companyCode: string, values: DeptFormValues): DeptCreateBody {
  return {
    companyCode,
    parentDeptCode: emptyToNull(values.parentDeptCode),
    deptCode: values.deptCode?.trim() ?? '',
    deptName: values.deptName?.trim() ?? '',
    deptTypeCode: values.deptTypeCode?.trim() ?? '',
    sortOrder: values.sortOrder ?? null,
    validFromDate: emptyToNull(values.validFromDate),
    validToDate: emptyToNull(values.validToDate),
    translations: toNameTranslations(values.names),
  };
}

export function toUpdateBody(values: DeptFormValues, current: Dept, trackHistory: boolean): DeptUpdateBody {
  const body: DeptUpdateBody = {
    parentDeptCode: emptyToNull(values.parentDeptCode),
    deptName: values.deptName?.trim() ?? '',
    deptTypeCode: values.deptTypeCode?.trim() ?? '',
    sortOrder: values.sortOrder ?? null,
    validFromDate: emptyToNull(values.validFromDate),
    validToDate: emptyToNull(values.validToDate),
    description: current.description,
    translations: toNameTranslations(values.names),
  };
  if (trackHistory) {
    body.effectiveDate = emptyToNull(values.effectiveDate);
    body.changeReason = emptyToNull(values.changeReason);
  }
  return body;
}
