import type { ApiError } from '@/core/request/errors';
import { toFormFields } from '@/core/request/errors';

const LOGIN_ID = /^[A-Za-z0-9._@-]{3,100}$/;
const FORM_FIELDS = new Set(['loginId', 'accountTypeCode', 'personPk', 'supportLanguageCode', 'timezoneId', 'description']);
const FALLBACK_ZONES = ['Asia/Seoul', 'UTC', 'America/New_York', 'Europe/London'];

export type ErrorText = (id: string, fallback: string) => string;
export type UserAction = 'lock' | 'unlock' | 'dormant' | 'wake' | 'reset' | 'delete' | 'restore';

const ERROR_TEXT: Record<string, [string, string]> = {
  'AUTH.FORBIDDEN': ['pages.access.user.errors.forbidden', '이 작업을 할 권한이 없습니다.'],
  'USER.DUPLICATE_LOGIN_ID': ['pages.access.user.errors.duplicate', '이미 있는 로그인 아이디입니다.'],
  'USER.PERSON_ALREADY_LINKED': ['pages.access.user.errors.personLinked', '이미 계정이 연결된 사람입니다.'],
  'USER.NOT_FOUND': ['pages.access.user.errors.notFound', '사용자를 찾을 수 없습니다.'],
  'USER.INACTIVE': ['pages.access.user.errors.inactive', '사용 중이 아닌 계정입니다.'],
  'USER.INVALID_STATUS_TRANSITION': ['pages.access.user.errors.status', '바꿀 수 없는 상태입니다.'],
  'USER.SELF_OPERATION_FORBIDDEN': ['pages.access.user.errors.self', '본인 계정은 변경할 수 없습니다.'],
  'USER.LAST_ADMIN': ['pages.access.user.errors.lastAdmin', '마지막 관리자 계정입니다.'],
  'USER.NO_PASSWORD_ACCOUNT': ['pages.access.user.errors.noPassword', '비밀번호가 없는 계정입니다.'],
  'ROLE.INACTIVE': ['pages.access.user.errors.roleInactive', '사용 중이 아닌 역할입니다.'],
  'ROLE.NOT_FOUND': ['pages.access.user.errors.roleNotFound', '역할을 찾을 수 없습니다.'],
  'ROLE.INVALID_DATE': ['pages.access.user.errors.roleDate', '시작일은 종료일보다 늦을 수 없습니다.'],
  'COMMON.VALIDATION_FAILED': ['pages.access.user.errors.validation', '입력값을 확인해 주세요.'],
};

export function validLoginId(value: string | undefined): boolean {
  const text = value?.trim() ?? '';
  return LOGIN_ID.test(text);
}

export function timeZoneOptions(): string[] {
  const lookup = Intl as typeof Intl & { supportedValuesOf?: (key: string) => string[] };
  if (typeof lookup.supportedValuesOf === 'function') {
    try {
      const zones = lookup.supportedValuesOf('timeZone');
      if (zones.length > 0) {
        return zones.includes('Asia/Seoul') ? zones : ['Asia/Seoul', ...zones];
      }
    } catch {
      return FALLBACK_ZONES;
    }
  }
  return FALLBACK_ZONES;
}

export function userActions(user: { userStatusCode: string; useYn: string }): UserAction[] {
  if (user.useYn !== 'Y' || user.userStatusCode === 'WITHDRAWN') {
    return ['restore'];
  }
  const actions: UserAction[] = [];
  if (user.userStatusCode === 'ACTIVE') {
    actions.push('lock', 'dormant');
  } else if (user.userStatusCode === 'LOCKED') {
    actions.push('unlock');
  } else if (user.userStatusCode === 'DORMANT') {
    actions.push('wake');
  }
  return [...actions, 'reset', 'delete'];
}

export function hiddenWhenSelf(action: UserAction): boolean {
  return action !== 'restore';
}

export function isSelf(currentUserPk: number | undefined, userPk: number): boolean {
  return currentUserPk != null && currentUserPk === userPk;
}

export function formatRoles(codes: string[] | null | undefined, names: ReadonlyMap<string, string>, more: (count: number) => string): string {
  const labels = (codes ?? []).map((code) => names.get(code) ?? code);
  if (labels.length === 0) {
    return '-';
  }
  if (labels.length <= 3) {
    return labels.join(', ');
  }
  return `${labels.slice(0, 3).join(', ')} ${more(labels.length - 3)}`;
}

export function invalidGrantDates(grants: { roleCode: string; validFromDate?: string | null; validToDate?: string | null }[]): string[] {
  return grants
    .filter((grant) => grant.validFromDate && grant.validToDate && grant.validToDate < grant.validFromDate)
    .map((grant) => grant.roleCode);
}

export function userErrorText(error: Pick<ApiError, 'message' | 'errorCode'>, text: ErrorText): string {
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

export function userFormErrors(error: ApiError, text: ErrorText): { name: string; errors: string[] }[] {
  const fromServer = toFormFields(error.fieldErrors).filter((item) => FORM_FIELDS.has(String(item.name)));
  if (fromServer.length > 0) {
    return fromServer;
  }
  if (error.errorCode === 'USER.DUPLICATE_LOGIN_ID') {
    return [{ name: 'loginId', errors: [userErrorText(error, text)] }];
  }
  if (error.errorCode === 'USER.PERSON_ALREADY_LINKED') {
    return [{ name: 'personPk', errors: [userErrorText(error, text)] }];
  }
  return [];
}

export function roleGrantRowErrors(error: Pick<ApiError, 'message' | 'fieldErrors'>, roleCodes: string[]): Record<string, string> {
  const message = error.message?.trim() ?? '';
  const rows: Record<string, string> = {};
  for (const item of error.fieldErrors ?? []) {
    if (roleCodes.includes(item.field)) {
      rows[item.field] = item.message?.trim() || message;
    }
  }
  return rows;
}
