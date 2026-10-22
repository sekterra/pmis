import type { ApiError } from '@/core/request/errors';
import { toFormFields } from '@/core/request/errors';
import type { Company } from '@/services/mdm/companyRecord';

const COMPANY_CODE = /^[A-Z0-9_-]{1,50}$/;

const LOCAL_ERRORS: Record<string, string> = {
  'AUTH.FORBIDDEN': '이 작업을 할 권한이 없습니다.',
  'COMPANY.DUPLICATE': '이미 있는 회사 코드입니다.',
  'COMPANY.DUPLICATE_BUSINESS_NO': '이미 있는 사업자등록번호입니다.',
  'COMPANY.PARENT_CYCLE': '회사 계층이 순환합니다.',
  'COMPANY.PARENT_INACTIVE': '상위 회사가 사용 중이 아닙니다.',
  'COMPANY.HAS_CHILD_COMPANY': '사용 중인 하위 회사가 있습니다.',
  'COMPANY.HAS_ACTIVE_DEPT': '사용 중인 부서가 있습니다.',
  'COMPANY.HAS_ACTIVE_ASSIGNMENT': '현재 소속이 있습니다.',
  'COMPANY.LAST_OWN': '마지막 자사는 사용 중지할 수 없습니다.',
  'COMPANY.NOT_FOUND': '회사를 찾을 수 없습니다.',
  'COMMON.VALIDATION_FAILED': '입력값을 확인해 주세요.',
};

const FIELD_CODES: Record<string, string> = {
  'COMPANY.DUPLICATE': 'companyCode',
  'COMPANY.DUPLICATE_BUSINESS_NO': 'businessRegistrationNo',
  'COMPANY.PARENT_CYCLE': 'parentCompanyCode',
};

export function validCompanyCode(value: string | undefined): boolean {
  return Boolean(value && COMPANY_CODE.test(value));
}

export function companyErrorText(error: Pick<ApiError, 'message' | 'errorCode'>): string {
  const server = error.message?.trim();
  if (server) {
    return server;
  }
  if (error.errorCode && LOCAL_ERRORS[error.errorCode]) {
    return LOCAL_ERRORS[error.errorCode];
  }
  return error.errorCode ?? '';
}

export function companyFormErrors(error: ApiError): { name: string; errors: string[] }[] {
  const fromServer = toFormFields(error.fieldErrors);
  if (fromServer.length > 0) {
    return fromServer;
  }
  const field = error.errorCode ? FIELD_CODES[error.errorCode] : undefined;
  if (!field) {
    return [];
  }
  return [{ name: field, errors: [companyErrorText(error)] }];
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
