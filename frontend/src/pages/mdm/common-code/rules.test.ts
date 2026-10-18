import { describe, expect, it } from 'vitest';
import { listParams } from '@/services/mdm/codeQuery';
import type { CommonCode } from '@/services/mdm/code';
import {
  codeErrorText,
  historyPairs,
  languagesMissingName,
  parentChoices,
  toCodeTree,
  toTranslations,
} from './rules';

function code(partial: Partial<CommonCode> & Pick<CommonCode, 'codePk' | 'codeValue'>): CommonCode {
  return {
    groupCode: 'GROUP',
    codeName: partial.codeValue,
    parentCodeValue: null,
    levelNo: null,
    codePath: null,
    sortOrder: null,
    extAttr1: null,
    extAttr2: null,
    extAttr3: null,
    extAttr4: null,
    extAttr5: null,
    validFromDate: null,
    validToDate: null,
    useYn: 'Y',
    translations: [],
    ...partial,
  };
}

describe('common code rules', () => {
  it('builds a tree and keeps rows whose parent is missing at the root', () => {
    const tree = toCodeTree([
      code({ codePk: 1, codeValue: 'ROOT' }),
      code({ codePk: 2, codeValue: 'CHILD', parentCodeValue: 'ROOT' }),
      code({ codePk: 3, codeValue: 'ORPHAN', parentCodeValue: 'GONE' }),
    ]);
    expect(tree.map((item) => item.codeValue)).toEqual(['ROOT', 'ORPHAN']);
    expect(tree[0]?.children?.map((item) => item.codeValue)).toEqual(['CHILD']);
    expect(tree[1]?.children).toBeUndefined();
  });

  it('omits languages without a name and keeps a description only with a name', () => {
    expect(
      toTranslations([
        { supportLanguageCode: 'ko-KR', name: ' 그룹 ', description: ' 설명 ' },
        { supportLanguageCode: 'en-US', name: '  ', description: '' },
        { supportLanguageCode: 'ja-JP', name: '', description: 'メモ' },
      ]),
    ).toEqual([
      { supportLanguageCode: 'ko-KR', name: '그룹', description: '설명' },
    ]);
    expect(
      languagesMissingName([{ supportLanguageCode: 'ja-JP', name: '', description: 'メモ' }]),
    ).toEqual(['ja-JP']);
  });

  it('uses the server message first and a local message when it is empty', () => {
    expect(codeErrorText({ message: '이미 있는 코드입니다.', errorCode: 'CODE.DUPLICATE' })).toBe(
      '이미 있는 코드입니다.',
    );
    expect(codeErrorText({ message: '  ', errorCode: 'CODE.GROUP_IN_USE' })).toContain('사용 중');
    expect(codeErrorText({ message: '', errorCode: 'CODE.INVALID_POLICY_VALUE' })).toContain('정책');
    expect(codeErrorText({ message: '', errorCode: 'AUTH.FORBIDDEN' })).toContain('권한');
  });

  it('builds page, size, and sort query params', () => {
    expect(
      listParams({
        page: 2,
        size: 20,
        sort: ['codeGroupName,desc'],
        keyword: ' 계층 ',
        useYn: 'y',
        solutionCode: ' MDM ',
        groupCode: ' GROUP ',
      }),
    ).toEqual({
      page: 2,
      size: 20,
      sort: ['codeGroupName,desc'],
      keyword: '계층',
      useYn: 'Y',
      solutionCode: 'MDM',
      groupCode: 'GROUP',
    });
    expect(listParams({})).toEqual({ page: 1, size: 20 });
  });

  it('excludes the code itself and its descendants from parent choices', () => {
    const rows = [
      code({ codePk: 1, codeValue: 'ROOT' }),
      code({ codePk: 2, codeValue: 'CHILD', parentCodeValue: 'ROOT' }),
      code({ codePk: 3, codeValue: 'OTHER', useYn: 'N' }),
    ];
    expect(parentChoices(rows, 'ROOT').map((item) => item.codeValue)).toEqual([]);
    expect(parentChoices(rows, 'CHILD').map((item) => item.codeValue)).toEqual(['ROOT']);
  });

  it('compares history json as key-value pairs', () => {
    expect(historyPairs('{"name":"이전"}', '{"name":"이후","extra":1}')).toEqual([
      { key: 'name', before: '이전', after: '이후' },
      { key: 'extra', before: '', after: '1' },
    ]);
  });
});
