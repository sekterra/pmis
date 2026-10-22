import type { ApiError } from '@/core/request/errors';
import { toFormFields } from '@/core/request/errors';
import type { ErrorText } from '@/pages/org/company/rules';
import { emptyToNull } from '@/services/mdm/companyQuery';
import type { Dept } from '@/services/mdm/deptRecord';

const ERROR_TEXT: Record<string, [string, string]> = {
  'AUTH.FORBIDDEN': ['pages.org.dept.errors.forbidden', '이 작업을 할 권한이 없습니다.'],
  'DEPT.DUPLICATE': ['pages.org.dept.errors.duplicate', '이미 있는 부서 코드입니다.'],
  'DEPT.PARENT_CYCLE': ['pages.org.dept.errors.parentCycle', '부서 계층이 순환합니다.'],
  'DEPT.PARENT_COMPANY_MISMATCH': ['pages.org.dept.errors.parentCompany', '다른 회사의 상위 부서입니다.'],
  'DEPT.PARENT_INACTIVE': ['pages.org.dept.errors.parentInactive', '상위 부서가 사용 중이 아닙니다.'],
  'DEPT.INVALID_DATE': ['pages.org.dept.errors.invalidDate', '시작일은 종료일보다 늦을 수 없습니다.'],
  'DEPT.HAS_CHILD': ['pages.org.dept.errors.hasChild', '사용 중인 하위 부서가 있습니다.'],
  'DEPT.HAS_ACTIVE_ASSIGNMENT': ['pages.org.dept.errors.hasAssignment', '현재 소속이 있습니다.'],
  'DEPT.NOT_FOUND': ['pages.org.dept.errors.notFound', '부서를 찾을 수 없습니다.'],
  'COMMON.VALIDATION_FAILED': ['pages.org.dept.errors.validation', '입력값을 확인해 주세요.'],
};

const FIELD_CODES: Record<string, string> = {
  'DEPT.DUPLICATE': 'deptCode',
  'DEPT.PARENT_CYCLE': 'parentDeptCode',
  'DEPT.PARENT_COMPANY_MISMATCH': 'parentDeptCode',
  'DEPT.PARENT_INACTIVE': 'parentDeptCode',
  'DEPT.INVALID_DATE': 'validToDate',
};

export function validDeptCode(value: string | undefined): boolean {
  const text = value?.trim() ?? '';
  return text.length > 0 && text.length <= 50;
}

export function datesOrdered(from: string | null | undefined, to: string | null | undefined): boolean {
  if (!from || !to) {
    return true;
  }
  return to >= from;
}

export function historyChange(
  current: Pick<Dept, 'deptName' | 'parentDeptCode'>,
  values: { deptName?: string; parentDeptCode?: string | null },
): { name: boolean; parent: boolean; any: boolean } {
  const name = (values.deptName ?? '').trim() !== current.deptName;
  const parent = emptyToNull(values.parentDeptCode) !== (current.parentDeptCode ?? null);
  return { name, parent, any: name || parent };
}

export function deptErrorText(error: Pick<ApiError, 'message' | 'errorCode'>, text: ErrorText): string {
  const server = error.message?.trim();
  if (server) {
    return server;
  }
  const entry = error.errorCode ? ERROR_TEXT[error.errorCode] : undefined;
  if (entry) {
    return text(entry[0], entry[1]);
  }
  return error.errorCode ?? '';
}

export function deptFormErrors(error: ApiError, text: ErrorText): { name: string; errors: string[] }[] {
  const fromServer = toFormFields(error.fieldErrors);
  if (fromServer.length > 0) {
    return fromServer;
  }
  const field = error.errorCode ? FIELD_CODES[error.errorCode] : undefined;
  if (!field) {
    return [];
  }
  return [{ name: field, errors: [deptErrorText(error, text)] }];
}

export function namesFromDept(dept: Dept | undefined): Record<string, string> {
  const names: Record<string, string> = {};
  for (const item of dept?.translations ?? []) {
    names[item.supportLanguageCode] = item.text;
  }
  return names;
}
