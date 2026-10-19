import { describe, expect, it } from 'vitest';
import { fallbackChoices, isNationCode, languageErrorText, normalizeNationCode, toLanguageBody, toNationBody } from './rules';
import { languageListParams, nationListParams } from '@/services/mdm/languageQuery';

describe('language rules', () => {
  it('normalizes and checks a nation code', () => {
    expect(normalizeNationCode(' kr ')).toBe('KR');
    expect(isNationCode('kr')).toBe(true);
    expect(isNationCode('K')).toBe(false);
    expect(isNationCode('KOR')).toBe(false);
  });

  it('builds a language body and drops blank optional fields', () => {
    expect(
      toLanguageBody({
        supportLanguageCode: ' ja-JP ',
        languageName: ' 일본어 ',
        nativeName: ' ',
        textDirectionCode: 'LTR',
        defaultLanguage: true,
        sortOrder: '3',
      }),
    ).toEqual({
      supportLanguageCode: 'ja-JP',
      languageName: '일본어',
      nativeName: null,
      isoLanguageCode: null,
      fallbackLanguageCode: null,
      textDirectionCode: 'LTR',
      dateFormat: null,
      datetimeFormat: null,
      numberFormat: null,
      defaultLanguage: true,
      sortOrder: 3,
    });
  });

  it('builds a nation body with an uppercased code', () => {
    expect(
      toNationBody({
        nationCode: 'kr',
        nationName: ' 한국 ',
        defaultLanguageCode: ' ko-KR ',
        phoneCode: '',
      }),
    ).toMatchObject({
      nationCode: 'KR',
      nationName: '한국',
      defaultLanguageCode: 'ko-KR',
      phoneCode: null,
    });
  });

  it('prefers the server error text and falls back to the local text', () => {
    expect(languageErrorText({ message: ' 서버 ', errorCode: 'I18N.DUPLICATE' })).toBe('서버');
    expect(languageErrorText({ message: ' ', errorCode: 'I18N.LANGUAGE_IN_USE' })).toContain('사용 중지');
  });

  it('omits an empty language filter and sends nation page sort', () => {
    expect(languageListParams(undefined)).toEqual({});
    expect(languageListParams('y')).toEqual({ useYn: 'Y' });
    expect(nationListParams({ page: 2, size: 20, sort: ['nationName,asc', 'keyword,desc'] })).toEqual({
      managed: true,
      page: 2,
      size: 20,
      sort: ['nationName,asc'],
    });
  });

  it('drops the current language and inactive languages from fallback choices', () => {
    const choices = fallbackChoices(
      [
        { supportLanguageCode: 'ko-KR', useYn: 'Y' },
        { supportLanguageCode: 'en-US', useYn: 'Y' },
        { supportLanguageCode: 'fr-FR', useYn: 'N' },
      ],
      'ko-KR',
    );
    expect(choices.map((item) => item.supportLanguageCode)).toEqual(['en-US']);
  });
});
