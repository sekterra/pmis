import { messageListParams, namespaceListParams } from '@/services/mdm/i18nQuery';
import { describe, expect, it } from 'vitest';
import { i18nErrorText, toMessageTranslations } from './rules';

describe('i18n rules', () => {
  it('drops languages with a blank translation and keeps status and machine flag', () => {
    expect(
      toMessageTranslations([
        {
          supportLanguageCode: 'ko-KR',
          messageText: ' 저장 ',
          translationStatusCode: ' DRAFT ',
          machineTranslated: false,
        },
        {
          supportLanguageCode: 'en-US',
          messageText: '   ',
          translationStatusCode: 'APPROVED',
          machineTranslated: true,
        },
      ]),
    ).toEqual([
      {
        supportLanguageCode: 'ko-KR',
        messageText: '저장',
        translationStatusCode: 'DRAFT',
        machineTranslated: false,
      },
    ]);
  });

  it('prefers the server message and falls back to a local message', () => {
    expect(i18nErrorText({ message: ' 서버 문구 ', errorCode: 'I18N.DUPLICATE' })).toBe('서버 문구');
    expect(i18nErrorText({ message: '   ', errorCode: 'I18N.DUPLICATE' })).toBe(
      '이미 사용 중인 코드 또는 키입니다.',
    );
    expect(i18nErrorText({ message: '', errorCode: 'I18N.LANGUAGE_NOT_FOUND' })).toBe(
      '지원하지 않는 언어입니다.',
    );
  });

  it('builds namespace and message queries', () => {
    expect(
      namespaceListParams({ page: 2, size: 20, keyword: ' 메뉴 ', useYn: 'n', sort: ['namespaceCode,asc'] }),
    ).toEqual({
      page: 2,
      size: 20,
      keyword: '메뉴',
      useYn: 'N',
      sort: ['namespaceCode,asc'],
    });
    expect(
      messageListParams({
        namespaceCode: ' menu ',
        keyword: '저장',
        messageTypeCode: ' LABEL ',
        translationStatusCode: ' DRAFT ',
        useYn: 'Y',
      }),
    ).toEqual({
      page: 1,
      size: 20,
      namespaceCode: 'menu',
      keyword: '저장',
      messageTypeCode: 'LABEL',
      translationStatusCode: 'DRAFT',
      useYn: 'Y',
    });
  });
});
