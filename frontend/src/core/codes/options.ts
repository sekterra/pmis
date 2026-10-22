import type { CommonCode } from '@/services/mdm/code';

export function localToday(now = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function isUsableCode(code: CommonCode, today: string): boolean {
  if (code.useYn !== 'Y') {
    return false;
  }
  if (code.validFromDate && code.validFromDate > today) {
    return false;
  }
  if (code.validToDate && code.validToDate < today) {
    return false;
  }
  return true;
}

export function codeOptionLabel(code: CommonCode, locale: string): string {
  const translated = code.translations
    ?.find((item) => item.supportLanguageCode === locale)
    ?.name?.trim();
  return translated || code.codeName;
}

export function toCodeOptions(codes: CommonCode[], locale: string, today = localToday()) {
  return codes.filter((code) => isUsableCode(code, today)).map((code) => ({
    value: code.codeValue,
    label: codeOptionLabel(code, locale),
  }));
}
