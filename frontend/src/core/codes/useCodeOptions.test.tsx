import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { CommonCode } from '@/services/mdm/code';
import { loadCodeGroup, resetCodeOptionsCache, useCodeOptions } from './useCodeOptions';

const listGroupCodes = vi.hoisted(() => vi.fn());

vi.mock('@/services/mdm/codes', () => ({
  listGroupCodes: (...args: unknown[]) => listGroupCodes(...args),
}));

const own: CommonCode = {
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
  translations: [],
};

describe('useCodeOptions', () => {
  beforeEach(() => {
    resetCodeOptionsCache();
    listGroupCodes.mockReset();
    listGroupCodes.mockResolvedValue([own]);
  });

  it('caches a group and returns an empty list when the group has no usable code', async () => {
    const first = await loadCodeGroup('COMPANY_TYPE');
    const second = await loadCodeGroup('COMPANY_TYPE');
    expect(first).toBe(second);
    expect(listGroupCodes).toHaveBeenCalledTimes(1);

    listGroupCodes.mockResolvedValue([{ ...own, useYn: 'N' }]);
    resetCodeOptionsCache();
    const hook = renderHook(() => useCodeOptions('COMPANY_TYPE'));
    await waitFor(() => expect(hook.result.current.loading).toBe(false));
    expect(hook.result.current.empty).toBe(true);
    expect(hook.result.current.options).toEqual([]);
  });
});
