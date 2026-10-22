import type { ApiError } from '@/core/request/errors';
import { toFormFields } from '@/core/request/errors';
import type { Company } from '@/services/mdm/companyRecord';

const COMPANY_CODE = /^[A-Z0-9_-]{1,50}$/;

export type ErrorText = (id: string, fallback: string) => string;

const ERROR_TEXT: Record<string, [string, string]> = {
  'AUTH.FORBIDDEN': ['pages.org.company.errors.forbidden', '이 작업을 할 권한이 없습니다.'],
  'COMPANY.DUPLICATE': ['pages.org.company.errors.duplicate', '이미 있는 회사 코드입니다.'],
  'COMPANY.DUPLICATE_BUSINESS_NO': ['pages.org.company.errors.duplicateBusinessNo', '이미 있는 사업자등록번호입니다.'],
  'COMPANY.PARENT_CYCLE': ['pages.org.company.errors.parentCycle', '회사 계층이 순환합니다.'],
  'COMPANY.PARENT_INACTIVE': ['pages.org.company.errors.parentInactive', '상위 회사가 사용 중이 아닙니다.'],
  'COMPANY.HAS_CHILD_COMPANY': ['pages.org.company.errors.hasChild', '사용 중인 하위 회사가 있습니다.'],
  'COMPANY.HAS_ACTIVE_DEPT': ['pages.org.company.errors.hasDept', '사용 중인 부서가 있습니다.'],
  'COMPANY.HAS_ACTIVE_ASSIGNMENT': ['pages.org.company.errors.hasAssignment', '현재 소속이 있습니다.'],
  'COMPANY.LAST_OWN': ['pages.org.company.errors.lastOwn', '마지막 자사는 사용 중지할 수 없습니다.'],
  'COMPANY.NOT_FOUND': ['pages.org.company.errors.notFound', '회사를 찾을 수 없습니다.'],
  'COMMON.VALIDATION_FAILED': ['pages.org.company.errors.validation', '입력값을 확인해 주세요.'],
};

const FIELD_CODES: Record<string, string> = {
  'COMPANY.DUPLICATE': 'companyCode',
  'COMPANY.DUPLICATE_BUSINESS_NO': 'businessRegistrationNo',
  'COMPANY.PARENT_CYCLE': 'parentCompanyCode',
};

export function validCompanyCode(value: string | undefined): boolean {
  return Boolean(value && COMPANY_CODE.test(value));
}

export function companyErrorText(error: Pick<ApiError, 'message' | 'errorCode'>, text: ErrorText): string {
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

export function companyFormErrors(error: ApiError, text: ErrorText): { name: string; errors: string[] }[] {
  const fromServer = toFormFields(error.fieldErrors);
  if (fromServer.length > 0) {
    return fromServer;
  }
  const field = error.errorCode ? FIELD_CODES[error.errorCode] : undefined;
  if (!field) {
    return [];
  }
  return [{ name: field, errors: [companyErrorText(error, text)] }];
}

export function parentChoices(rows: Company[], selfCode?: string): Company[] {
  return rows.filter((row) => row.useYn === 'Y' && row.companyCode !== selfCode);
}

export function namesFromTranslations(company: Company | undefined): Record<string, string> {
  const names: Record<string, string> = {};
  for (const item of company?.translations ?? []) {
    names[item.supportLanguageCode] = item.text;
  }
  return names;
}
