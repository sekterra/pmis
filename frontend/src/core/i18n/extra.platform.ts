import enUS from '@/locales/platform/en-US';
import koKR from '@/locales/platform/ko-KR';

const catalogs: Record<string, Record<string, string>> = {
  'ko-KR': koKR,
  'en-US': enUS,
};

export function extraMessages(locale: string): Record<string, string> {
  return catalogs[locale] ?? catalogs['ko-KR'];
}
