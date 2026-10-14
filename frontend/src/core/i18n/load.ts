import type { AppMode } from '@/core/auth/mode';
import type { Bundle, SupportLanguage } from '@/services/mdm/data';
import {
  applyBundleResponse,
  type BundleCache,
  FALLBACK_LANGUAGES,
  mergeMessages,
  PUBLIC_NAMESPACES,
  pickLocale,
} from './messages';

export type BundleFetch = {
  status: number;
  etag?: string | null;
  body?: Bundle;
};

export async function loadCatalogForMode(
  mode: AppMode,
  input: Parameters<typeof loadPublicCatalog>[0],
): Promise<Awaited<ReturnType<typeof loadPublicCatalog>>> {
  if (mode === 'platform') {
    const locale =
      input.savedLocale && input.localMessages[input.savedLocale]
        ? input.savedLocale
        : 'ko-KR';
    return {
      locale,
      languages: FALLBACK_LANGUAGES,
      messages: input.localMessages[locale] ?? {},
      cache: input.cache ?? {},
    };
  }
  return loadPublicCatalog(input);
}

export async function loadPublicCatalog(input: {
  fetchLanguages: () => Promise<SupportLanguage[]>;
  fetchBundle: (
    namespace: string,
    lang: string,
    etag?: string,
  ) => Promise<BundleFetch>;
  localMessages: Record<string, Record<string, string>>;
  savedLocale?: string | null;
  cache?: Record<string, BundleCache>;
}): Promise<{
  locale: string;
  languages: SupportLanguage[];
  messages: Record<string, string>;
  cache: Record<string, BundleCache>;
}> {
  try {
    const languages = await input.fetchLanguages();
    const locale = pickLocale(languages, input.savedLocale);
    const local =
      input.localMessages[locale] ?? input.localMessages['ko-KR'] ?? {};
    const cache = { ...(input.cache ?? {}) };
    let server: Record<string, string> = {};
    for (const namespace of PUBLIC_NAMESPACES) {
      const key = `${locale}:${namespace}`;
      const response = await input.fetchBundle(
        namespace,
        locale,
        cache[key]?.etag,
      );
      const next = applyBundleResponse(
        cache[key] ?? { messages: {} },
        response.status,
        response.body,
        response.etag,
      );
      cache[key] = next;
      server = { ...server, ...next.messages };
    }
    return {
      locale,
      languages: languages.length > 0 ? languages : FALLBACK_LANGUAGES,
      messages: mergeMessages(local, server),
      cache,
    };
  } catch {
    const locale =
      input.savedLocale && input.localMessages[input.savedLocale]
        ? input.savedLocale
        : 'ko-KR';
    return {
      locale,
      languages: FALLBACK_LANGUAGES,
      messages: input.localMessages[locale] ?? {},
      cache: input.cache ?? {},
    };
  }
}
