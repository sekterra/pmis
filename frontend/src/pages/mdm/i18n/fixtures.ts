import type { SupportLanguage } from '@/services/mdm/data';
import type { MessageRecord, NamespaceRecord } from '@/services/mdm/i18nRecord';

export const languages: SupportLanguage[] = [
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
    languageName: '영어',
    nativeName: 'English',
    defaultLanguage: false,
    fallbackLanguageCode: 'ko-KR',
    textDirectionCode: 'LTR',
    dateFormat: 'yyyy-MM-dd',
    datetimeFormat: 'yyyy-MM-dd HH:mm',
    numberFormat: '#,##0.##',
  },
];

export const menuNamespace: NamespaceRecord = {
  namespacePk: 1,
  namespaceCode: 'menu',
  namespaceName: '메뉴',
  solutionCode: 'MDM',
  description: '메뉴 문구',
  useYn: 'Y',
};

export const oldNamespace: NamespaceRecord = {
  namespacePk: 2,
  namespaceCode: 'old',
  namespaceName: '이전',
  solutionCode: null,
  description: null,
  useYn: 'N',
};

export const saveMessage: MessageRecord = {
  messagePk: 5,
  namespaceCode: 'menu',
  messageKey: 'SAVE',
  messageTypeCode: 'LABEL',
  defaultMessage: '저장',
  paramDescription: null,
  useYn: 'Y',
  translations: [],
};

export const saveMessageDetail: MessageRecord = {
  ...saveMessage,
  translations: [
    {
      supportLanguageCode: 'ko-KR',
      messageText: '저장',
      translationStatusCode: 'DRAFT',
      machineTranslated: false,
      versionNo: 2,
    },
  ],
};

export function pageOf<T>(items: T[]) {
  return { items, total: items.length, page: 1, size: 20 };
}
