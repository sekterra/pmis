import type { ApiError } from '@/core/request/errors';
import type { LanguageBody, NationBody } from '@/services/mdm/languageRecord';

const LOCAL_ERRORS: Record<string, string> = {
  'AUTH.FORBIDDEN': '이 작업을 할 권한이 없습니다.',
  'I18N.DUPLICATE': '이미 사용 중인 코드입니다.',
  'I18N.LANGUAGE_NOT_FOUND': '지원하지 않는 언어입니다.',
  'I18N.LANGUAGE_IN_USE': '기본 언어이거나 다른 언어의 대체 언어라 사용 중지할 수 없습니다.',
  'I18N.DEFAULT_REQUIRED': '기본 언어가 하나여야 합니다.',
  'I18N.FALLBACK_CYCLE': '대체 언어가 순환됩니다.',
  'I18N.NATION_NOT_FOUND': '국가를 찾을 수 없습니다.',
  'COMMON.VALIDATION_FAILED': '입력값을 확인해 주세요.',
};

const NATION_CODE = /^[A-Z]{2}$/;
const LANGUAGE_CODE = /^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$/;

export function languageErrorText(error: Pick<ApiError, 'message' | 'errorCode'>): string {
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

export function normalizeNationCode(value?: string | null): string {
  return (value ?? '').trim().toUpperCase();
}

export function isNationCode(value?: string | null): boolean {
  return NATION_CODE.test(normalizeNationCode(value));
}

export function isLanguageCode(value?: string | null): boolean {
  return LANGUAGE_CODE.test((value ?? '').trim());
}

export function optionalSort(value?: number | string | null): number | null {
  if (value === undefined || value === null || value === '') {
    return null;
  }
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isInteger(parsed) ? parsed : null;
}

export type LanguageFormValues = {
  supportLanguageCode?: string;
  languageName?: string;
  nativeName?: string;
  isoLanguageCode?: string;
  fallbackLanguageCode?: string;
  textDirectionCode?: string;
  dateFormat?: string;
  datetimeFormat?: string;
  numberFormat?: string;
  defaultLanguage?: boolean;
  sortOrder?: number | string | null;
};

export function toLanguageBody(values: LanguageFormValues, lockedCode?: string): LanguageBody {
  return {
    supportLanguageCode: (lockedCode ?? values.supportLanguageCode ?? '').trim(),
    languageName: (values.languageName ?? '').trim(),
    nativeName: blankToNull(values.nativeName),
    isoLanguageCode: blankToNull(values.isoLanguageCode),
    fallbackLanguageCode: blankToNull(values.fallbackLanguageCode),
    textDirectionCode: values.textDirectionCode ?? '',
    dateFormat: blankToNull(values.dateFormat),
    datetimeFormat: blankToNull(values.datetimeFormat),
    numberFormat: blankToNull(values.numberFormat),
    defaultLanguage: values.defaultLanguage === true,
    sortOrder: optionalSort(values.sortOrder),
  };
}

export type NationFormValues = {
  nationCode?: string;
  isoAlpha3Code?: string;
  isoNumericCode?: string;
  nationName?: string;
  defaultLanguageCode?: string;
  phoneCode?: string;
  currencyCode?: string;
  timezoneId?: string;
  sortOrder?: number | string | null;
};

export function toNationBody(values: NationFormValues, lockedCode?: string): NationBody {
  return {
    nationCode: normalizeNationCode(lockedCode ?? values.nationCode),
    isoAlpha3Code: blankToNull(values.isoAlpha3Code),
    isoNumericCode: blankToNull(values.isoNumericCode),
    nationName: (values.nationName ?? '').trim(),
    defaultLanguageCode: (values.defaultLanguageCode ?? '').trim(),
    phoneCode: blankToNull(values.phoneCode),
    currencyCode: blankToNull(values.currencyCode),
    timezoneId: blankToNull(values.timezoneId),
    sortOrder: optionalSort(values.sortOrder),
  };
}

export function fallbackChoices<T extends { supportLanguageCode: string; useYn: string }>(
  languages: T[],
  selfCode?: string,
): T[] {
  return languages.filter(
    (language) => language.useYn === 'Y' && language.supportLanguageCode !== selfCode,
  );
}

export function inUse(useYn?: string | null): boolean {
  return useYn === 'Y';
}
