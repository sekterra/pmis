import type { ApiError } from '@/core/request/errors';
import type { MessageText } from '@/services/mdm/i18nRecord';

const LOCAL_ERRORS: Record<string, string> = {
  'AUTH.FORBIDDEN': '이 작업을 할 권한이 없습니다.',
  'I18N.DUPLICATE': '이미 사용 중인 코드 또는 키입니다.',
  'I18N.LANGUAGE_NOT_FOUND': '지원하지 않는 언어입니다.',
  'I18N.NAMESPACE_NOT_FOUND': '네임스페이스를 찾을 수 없습니다.',
  'I18N.MESSAGE_NOT_FOUND': '메시지를 찾을 수 없습니다.',
  'COMMON.VALIDATION_FAILED': '입력값을 확인해 주세요.',
  'COMMON.INVALID_PAGE': '페이지 값이 올바르지 않습니다.',
  'COMMON.INVALID_SORT': '정렬 값이 올바르지 않습니다.',
};

export function i18nErrorText(error: Pick<ApiError, 'message' | 'errorCode'>): string {
  const server = error.message?.trim();
  if (server) {
    return server;
  }
  if (error.errorCode && LOCAL_ERRORS[error.errorCode]) {
    return LOCAL_ERRORS[error.errorCode];
  }
  return error.errorCode ?? '';
}

export function blankToNull(value?: string | null): string | null {
  const text = value?.trim() ?? '';
  return text ? text : null;
}

export function toMessageTranslations(
  rows: Array<{
    supportLanguageCode: string;
    messageText?: string;
    translationStatusCode?: string;
    machineTranslated?: boolean | null;
  }>,
): MessageText[] {
  return rows.flatMap((row) => {
    const messageText = row.messageText?.trim() ?? '';
    if (!messageText) {
      return [];
    }
    return [
      {
        supportLanguageCode: row.supportLanguageCode,
        messageText,
        translationStatusCode: blankToNull(row.translationStatusCode),
        machineTranslated: row.machineTranslated ?? null,
      },
    ];
  });
}

export function inUse(useYn?: string | null): boolean {
  return useYn === 'Y';
}
