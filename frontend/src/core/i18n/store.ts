import { useSyncExternalStore } from 'react';
import type { SupportLanguage } from '@/services/mdm/data';
import { FALLBACK_LANGUAGES } from './messages';

const LOCALE_KEY = 'pmis.locale';

export type I18nState = {
  locale: string;
  languages: SupportLanguage[];
  formats?: SupportLanguage;
};

let state: I18nState = {
  locale: 'ko-KR',
  languages: FALLBACK_LANGUAGES,
  formats: FALLBACK_LANGUAGES[0],
};

const listeners = new Set<() => void>();

function emit(): void {
  for (const listener of listeners) {
    listener();
  }
}

export function getI18nState(): I18nState {
  return state;
}

export function setI18nState(next: I18nState): void {
  state = next;
  emit();
}

export function subscribeI18n(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useI18nState(): I18nState {
  return useSyncExternalStore(subscribeI18n, getI18nState, getI18nState);
}

export function readSavedLocale(): string | null {
  try {
    return window.localStorage.getItem(LOCALE_KEY);
  } catch {
    return null;
  }
}

export function saveLocale(locale: string): void {
  try {
    window.localStorage.setItem(LOCALE_KEY, locale);
  } catch {
    // 저장을 못 해도 화면 언어 전환은 유지한다.
  }
}
