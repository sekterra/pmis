import type { CodeGroup, CodeHistory, CommonCode } from '@/services/mdm/code';
import type { SupportLanguage } from '@/services/mdm/data';

export const fixedAt = '2026-04-01T08:00:00';

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
    languageName: 'English',
    nativeName: 'English',
    defaultLanguage: false,
    fallbackLanguageCode: 'ko-KR',
    textDirectionCode: 'LTR',
    dateFormat: 'yyyy-MM-dd',
    datetimeFormat: 'yyyy-MM-dd HH:mm',
    numberFormat: '#,##0.##',
  },
];

export const activeGroup: CodeGroup = {
  codeGroupPk: 1,
  codeGroupCode: 'STATUS',
  solutionCode: 'MDM',
  codeGroupName: '상태',
  description: null,
  hierarchy: false,
  codeValueLength: 4,
  extAttrSchema: null,
  sortOrder: 1,
  useYn: 'Y',
  translations: [],
};

export const inactiveGroup: CodeGroup = {
  ...activeGroup,
  codeGroupPk: 2,
  codeGroupCode: 'OLD',
  codeGroupName: '중지그룹',
  useYn: 'N',
};

export const hierarchyGroup: CodeGroup = {
  ...activeGroup,
  codeGroupPk: 3,
  codeGroupCode: 'TREE',
  codeGroupName: '계층',
  hierarchy: true,
  codeValueLength: 10,
};

export function code(
  partial: Partial<CommonCode> & Pick<CommonCode, 'codePk' | 'codeValue'>,
): CommonCode {
  return {
    groupCode: 'TREE',
    codeName: partial.codeValue,
    parentCodeValue: null,
    levelNo: null,
    codePath: null,
    sortOrder: 1,
    extAttr1: null,
    extAttr2: null,
    extAttr3: null,
    extAttr4: null,
    extAttr5: null,
    validFromDate: '2026-03-01',
    validToDate: '2026-03-31',
    useYn: 'Y',
    translations: [],
    ...partial,
  };
}

export const historyRow: CodeHistory = {
  historyPk: 9,
  codePk: 11,
  codeGroupCode: 'STATUS',
  codeValue: 'ON',
  changeTypeCode: 'U',
  beforeData: '{"codeName":"이전"}',
  afterData: '{"codeName":"이후"}',
  changeReason: '이름 수정',
  changeDatetime: fixedAt,
  changeUserPk: 7,
};

export function pageOf<T>(items: T[], total = items.length) {
  return { items, total, page: 1, size: 20 };
}
