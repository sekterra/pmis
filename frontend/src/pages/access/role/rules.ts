import type { ApiError } from '@/core/request/errors';
import { toFormFields } from '@/core/request/errors';

const ROLE_CODE = /^[A-Z][A-Z0-9_]{1,49}$/;
const FORM_FIELDS = new Set(['roleCode', 'roleName', 'sortOrder', 'description']);

export type ErrorText = (id: string, fallback: string) => string;

const ERROR_TEXT: Record<string, [string, string]> = {
  'AUTH.FORBIDDEN': ['pages.access.role.errors.forbidden', '이 작업을 할 권한이 없습니다.'],
  'ROLE.DUPLICATE': ['pages.access.role.errors.duplicate', '이미 있는 역할 코드입니다.'],
  'ROLE.PROTECTED': ['pages.access.role.errors.protected', '테넌트 관리자 역할은 삭제하거나 권한을 바꿀 수 없습니다.'],
  'ROLE.IN_USE': ['pages.access.role.errors.inUse', '사용 중인 부여가 있습니다.'],
  'ROLE.READ_REQUIRED': ['pages.access.role.errors.readRequired', '같은 메뉴의 조회 권한이 필요합니다.'],
  'PERMISSION.NOT_FOUND': ['pages.access.role.errors.permissionNotFound', '권한을 찾을 수 없습니다.'],
  'ROLE.NOT_FOUND': ['pages.access.role.errors.notFound', '역할을 찾을 수 없습니다.'],
  'COMMON.VALIDATION_FAILED': ['pages.access.role.errors.validation', '입력값을 확인해 주세요.'],
};

export function validRoleCode(value: string | undefined): boolean {
  return Boolean(value && ROLE_CODE.test(value));
}

export function roleErrorText(error: Pick<ApiError, 'message' | 'errorCode'>, text: ErrorText): string {
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

export function roleFormErrors(error: ApiError, text: ErrorText): { name: string; errors: string[] }[] {
  const fromServer = toFormFields(error.fieldErrors).filter((item) => FORM_FIELDS.has(String(item.name)));
  if (fromServer.length > 0) {
    return fromServer;
  }
  if (error.errorCode === 'ROLE.DUPLICATE') {
    return [{ name: 'roleCode', errors: [roleErrorText(error, text)] }];
  }
  return [];
}

export function readRequiredMenus(error: Pick<ApiError, 'errorCode' | 'fieldErrors'>): string[] {
  if (error.errorCode !== 'ROLE.READ_REQUIRED') {
    return [];
  }
  return (error.fieldErrors ?? []).map((item) => item.field);
}
