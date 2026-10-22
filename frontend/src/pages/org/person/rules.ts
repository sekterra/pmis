import type { ApiError } from '@/core/request/errors';
import { toFormFields } from '@/core/request/errors';
import type { ErrorText } from '@/pages/org/company/rules';
import { emptyToNull } from '@/services/mdm/companyQuery';
import type { Assignment } from '@/services/mdm/peopleRecord';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const ERROR_TEXT: Record<string, [string, string]> = {
  'AUTH.FORBIDDEN': ['pages.org.person.errors.forbidden', '이 작업을 할 권한이 없습니다.'],
  'PERSON.HAS_ACTIVE_ASSIGNMENT': ['pages.org.person.errors.hasAssignment', '현재 소속이 있습니다.'],
  'PERSON.HAS_ACCOUNT': ['pages.org.person.errors.hasAccount', '사용 중인 계정이 있습니다.'],
  'PERSON.INACTIVE': ['pages.org.person.errors.inactive', '사용 중지된 사람입니다.'],
  'PERSON.NOT_FOUND': ['pages.org.person.errors.notFound', '사람을 찾을 수 없습니다.'],
  'ASSIGNMENT.DUPLICATE': ['pages.org.person.errors.duplicate', '같은 회사·부서의 현재 소속이 있습니다.'],
  'ASSIGNMENT.EMPLOYEE_NO_DUPLICATE': ['pages.org.person.errors.employeeNo', '이미 있는 사원번호입니다.'],
  'ASSIGNMENT.DEPT_COMPANY_MISMATCH': ['pages.org.person.errors.deptCompany', '부서가 선택한 회사 소속이 아닙니다.'],
  'ASSIGNMENT.DEPT_INACTIVE': ['pages.org.person.errors.deptInactive', '부서가 사용 중이 아닙니다.'],
  'ASSIGNMENT.COMPANY_INACTIVE': ['pages.org.person.errors.companyInactive', '회사가 사용 중이 아닙니다.'],
  'ASSIGNMENT.INVALID_DATE': ['pages.org.person.errors.invalidDate', '종료일은 시작일보다 빠를 수 없습니다.'],
  'ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT': ['pages.org.person.errors.head', '부서장으로 두려면 부서가 필요합니다.'],
  'ASSIGNMENT.RESIGNED_REQUIRES_END': ['pages.org.person.errors.resigned', '퇴직 상태에는 종료일이 필요합니다.'],
  'ASSIGNMENT.INACTIVE': ['pages.org.person.errors.assignmentInactive', '사용 중지된 소속은 복구한 뒤 종료하세요.'],
  'ASSIGNMENT.ALREADY_ENDED': ['pages.org.person.errors.alreadyEnded', '이미 종료된 소속입니다.'],
  'ORG.INVALID_CODE_VALUE': ['pages.org.person.errors.invalidCode', '코드 값을 확인해 주세요.'],
  'COMMON.VALIDATION_FAILED': ['pages.org.person.errors.validation', '입력값을 확인해 주세요.'],
  'FILE.EXTENSION_NOT_ALLOWED': ['pages.org.person.errors.extension', '허용되지 않는 파일 형식입니다.'],
  'FILE.MIME_MISMATCH': ['pages.org.person.errors.mime', '파일 내용이 확장자와 다릅니다.'],
  'FILE.TOO_LARGE': ['pages.org.person.errors.tooLarge', '파일이 너무 큽니다.'],
  'FILE.NOT_IMAGE': ['pages.org.person.errors.notImage', '이미지 파일만 올릴 수 있습니다.'],
};

const FIELD_CODES: Record<string, string> = {
  'ASSIGNMENT.DUPLICATE': 'deptCode',
  'ASSIGNMENT.EMPLOYEE_NO_DUPLICATE': 'employeeNo',
  'ASSIGNMENT.DEPT_COMPANY_MISMATCH': 'deptCode',
  'ASSIGNMENT.DEPT_INACTIVE': 'deptCode',
  'ASSIGNMENT.COMPANY_INACTIVE': 'companyCode',
  'ASSIGNMENT.INVALID_DATE': 'endDate',
  'ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT': 'deptHeadYn',
  'ASSIGNMENT.RESIGNED_REQUIRES_END': 'endDate',
};

export function validEmail(value: string | undefined): boolean {
  const text = value?.trim() ?? '';
  if (!text) {
    return true;
  }
  return text.length <= 200 && EMAIL.test(text);
}

export function datesOrdered(from: string | null | undefined, to: string | null | undefined): boolean {
  if (!from || !to) {
    return true;
  }
  return to >= from;
}

export function resignedNeedsEnd(status: string | null | undefined, endDate: string | null | undefined): boolean {
  return status !== 'RESIGNED' || Boolean(emptyToNull(endDate));
}

export function headNeedsDept(deptHead: boolean | undefined, deptCode: string | null | undefined): boolean {
  return !deptHead || Boolean(emptyToNull(deptCode));
}

export function isFirstCurrentAssignment(rows: Pick<Assignment, 'useYn' | 'endDate'>[] | null | undefined): boolean {
  return !(rows ?? []).some((row) => row.useYn === 'Y' && !row.endDate);
}

export function personErrorText(error: Pick<ApiError, 'message' | 'errorCode'>, text: ErrorText): string {
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

export function assignmentFormErrors(error: ApiError, text: ErrorText): { name: string; errors: string[] }[] {
  const fromServer = toFormFields(error.fieldErrors);
  if (fromServer.length > 0) {
    return fromServer;
  }
  const field = error.errorCode ? FIELD_CODES[error.errorCode] : undefined;
  if (!field) {
    return [];
  }
  return [{ name: field, errors: [personErrorText(error, text)] }];
}
