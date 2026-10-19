import { addLocale, setLocale } from '@umijs/max';
import enUS from 'antd/locale/en_US';
import koKR from 'antd/locale/ko_KR';
import dayjs from 'dayjs';
import 'dayjs/locale/en';
import 'dayjs/locale/ko';
import { appMode } from '@/core/auth/mode';
import { noteLocaleChanged } from '@/core/menu/runtime';
import { extraMessages as tenantExtra } from '@/core/i18n/extra';
import { extraMessages as platformExtra } from '@/core/i18n/extra.platform';
import { withTimeout } from '@/core/request/timeout';
import enMessages from '@/locales/en-US';
import koMessages from '@/locales/ko-KR';
import { fetchBundle, fetchLanguages } from '@/services/mdm/i18n';
import { loadCatalogForMode } from './load';
import type { BundleCache } from './messages';
import { readSavedLocale, saveLocale, setI18nState } from './store';

const localMessages: Record<string, Record<string, string>> = {
  'ko-KR': koMessages,
  'en-US': enMessages,
};

let cache: Record<string, BundleCache> = {};

function applyLocale(locale: string, messages: Record<string, string>): void {
  const antdLocale = locale === 'en-US' ? enUS : koKR;
  const dayjsLocale = locale.startsWith('en') ? 'en' : 'ko';
  dayjs.locale(dayjsLocale);
  addLocale(locale, messages, {
    momentLocale: dayjsLocale,
    antd: antdLocale,
  });
  setLocale(locale, false);
}

const STARTUP_TIMEOUT_MS = 8000;

export async function loadPublicI18n(preferred?: string): Promise<void> {
  await withTimeout(applyPublicI18n(preferred), STARTUP_TIMEOUT_MS);
}

async function applyPublicI18n(preferred?: string): Promise<void> {
  const saved = preferred ?? readSavedLocale();
  const extraMessages = appMode() === 'platform' ? platformExtra : tenantExtra;
  const loaded = await loadCatalogForMode(appMode(), {
    fetchLanguages,
    fetchBundle,
    localMessages,
    savedLocale: saved,
    cache,
  });
  cache = loaded.cache;
  const formats = loaded.languages.find(
    (item) => item.supportLanguageCode === loaded.locale,
  );
  setI18nState({
    locale: loaded.locale,
    languages: loaded.languages,
    formats,
  });
  applyLocale(loaded.locale, {
    ...loaded.messages,
    ...extraMessages(loaded.locale),
  });
}

export async function changeLocale(locale: string): Promise<void> {
  saveLocale(locale);
  await loadPublicI18n(locale);
  await noteLocaleChanged();
}
