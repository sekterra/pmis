import { describe, expect, it } from 'vitest';
import { ApiError } from '@/core/request/errors';
import { readRequiredMenus, roleFormErrors, validRoleCode } from './rules';

const text = (_id: string, fallback: string) => fallback;

describe('role rules', () => {
  it('checks the role code shape', () => {
    expect(validRoleCode('A')).toBe(false);
    expect(validRoleCode('AB')).toBe(true);
    expect(validRoleCode('A_1')).toBe(true);
    expect(validRoleCode('ab')).toBe(false);
    expect(validRoleCode(`A${'B'.repeat(49)}`)).toBe(true);
    expect(validRoleCode(`A${'B'.repeat(50)}`)).toBe(false);
  });

  it('maps duplicate to the code field and read-required to menu rows', () => {
    const duplicate = new ApiError('');
    duplicate.errorCode = 'ROLE.DUPLICATE';
    expect(roleFormErrors(duplicate, text)).toEqual([{ name: 'roleCode', errors: ['이미 있는 역할 코드입니다.'] }]);
    const read = new ApiError('조회가 필요합니다.');
    read.errorCode = 'ROLE.READ_REQUIRED';
    read.fieldErrors = [{ field: 'COMPANY', message: '조회가 필요합니다.' }];
    expect(readRequiredMenus(read)).toEqual(['COMPANY']);
    expect(roleFormErrors(read, text)).toEqual([]);
  });
});
