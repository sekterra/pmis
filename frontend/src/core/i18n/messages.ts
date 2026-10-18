import type { Bundle, SupportLanguage } from '@/services/mdm/data';

export type BundleCache = {
  etag?: string;
  messages: Record<string, string>;
};

export const FALLBACK_LANGUAGES: SupportLanguage[] = [
  {
    supportLanguageCode: 'ko-KR',
    languageName: '한국어',
    nativeName: '한국어',
    defaultLanguage: true,
    fallbackLanguageCode: null,
    textDirectionCode: 'LTR',
    dateFormat: 'yyyy-MM-dd',
    datetimeFormat: 'yyyy-MM-dd HH:mm',
    numberFormat: '#,##0.##',
  },
  {
    supportLanguageCode: 'en-US',
    languageName: 'English',
    nativeName: 'English',
    defaultLanguage: false,
    fallbackLanguageCode: null,
    textDirectionCode: 'LTR',
    dateFormat: 'yyyy-MM-dd',
    datetimeFormat: 'yyyy-MM-dd HH:mm',
    numberFormat: '#,##0.##',
  },
];

export function resolveText(
  key: string,
  server: Record<string, string>,
  local: Record<string, string>,
  defaultMessage?: string,
): string {
  if (Object.hasOwn(server, key)) {
    return server[key];
  }
  if (Object.hasOwn(local, key)) {
    return local[key];
  }
  return defaultMessage ?? key;
}

export function mergeMessages(
  local: Record<string, string>,
  server: Record<string, string>,
): Record<string, string> {
  return { ...local, ...server };
}

export function applyBundleResponse(
  previous: BundleCache,
  status: number,
  body?: Bundle,
  etag?: string | null,
): BundleCache {
  if (status === 304) {
    return previous;
  }
  return {
    etag: etag ?? undefined,
    messages: body?.messages ?? {},
  };
}

export function pickLocale(
  languages: SupportLanguage[],
  saved?: string | null,
): string {
  if (saved && languages.some((item) => item.supportLanguageCode === saved)) {
    return saved;
  }
  return (
    languages.find((item) => item.defaultLanguage)?.supportLanguageCode ??
    'ko-KR'
  );
}

export const PUBLIC_NAMESPACES = ['common', 'error', 'auth'] as const;
