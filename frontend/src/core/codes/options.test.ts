import { describe, expect, it } from 'vitest';
import type { CommonCode } from '@/services/mdm/code';
import { codeOptionLabel, isUsableCode, toCodeOptions } from './options';

function code(patch: Partial<CommonCode>): CommonCode {
  return {
    codePk: 1,
    groupCode: 'COMPANY_TYPE',
    codeValue: 'OWN',
    codeName: '자사',
    parentCodeValue: null,
    levelNo: null,
    codePath: null,
    sortOrder: 1,
    extAttr1: null,
    extAttr2: null,
    extAttr3: null,
    extAttr4: null,
    extAttr5: null,
    validFromDate: null,
    validToDate: null,
    useYn: 'Y',
    translations: [{ supportLanguageCode: 'en-US', name: 'Own', description: null }],
    ...patch,
  };
}

describe('code options', () => {
  it('keeps active codes inside the valid period and uses the current language name', () => {
    const rows = [
      code({}),
      code({ codePk: 2, codeValue: 'OLD', useYn: 'N' }),
      code({ codePk: 3, codeValue: 'FUTURE', validFromDate: '2099-01-01' }),
      code({ codePk: 4, codeValue: 'PAST', validToDate: '2000-01-01' }),
    ];
    expect(isUsableCode(rows[0], '2026-10-22')).toBe(true);
    expect(toCodeOptions(rows, 'en-US', '2026-10-22')).toEqual([{ value: 'OWN', label: 'Own' }]);
    expect(codeOptionLabel(rows[0], 'ko-KR')).toBe('자사');
    expect(toCodeOptions([], 'ko-KR', '2026-10-22')).toEqual([]);
  });
});
