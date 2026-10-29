import { describe, expect, it } from 'vitest';
import { ApiError } from '@/core/request/errors';
import {
  formatRoles,
  invalidGrantDates,
  isSelf,
  timeZoneOptions,
  userActions,
  userFormErrors,
  validLoginId,
} from './rules';

const text = (_id: string, fallback: string) => fallback;

describe('user rules', () => {
  it('checks the login id pattern', () => {
    expect(validLoginId('ab')).toBe(false);
    expect(validLoginId('abc')).toBe(true);
    expect(validLoginId(' user.name@x-1 ')).toBe(true);
    expect(validLoginId('a b')).toBe(false);
    expect(validLoginId('가나다')).toBe(false);
    expect(validLoginId(`a${'b'.repeat(100)}`)).toBe(false);
  });

  it('maps status to actions and recognizes the signed-in user', () => {
    expect(userActions({ userStatusCode: 'ACTIVE', useYn: 'Y' })).toEqual(['lock', 'dormant', 'reset', 'delete']);
    expect(userActions({ userStatusCode: 'LOCKED', useYn: 'Y' })).toEqual(['unlock', 'reset', 'delete']);
    expect(userActions({ userStatusCode: 'DORMANT', useYn: 'Y' })).toEqual(['wake', 'reset', 'delete']);
    expect(userActions({ userStatusCode: 'WITHDRAWN', useYn: 'N' })).toEqual(['restore']);
    expect(userActions({ userStatusCode: 'ACTIVE', useYn: 'N' })).toEqual(['restore']);
    expect(isSelf(8, 8)).toBe(true);
    expect(isSelf(undefined, 8)).toBe(false);
  });

  it('rejects an end date before the start date and maps field errors', () => {
    expect(invalidGrantDates([{ roleCode: 'VIEWER', validFromDate: '2026-10-02', validToDate: '2026-10-01' }])).toEqual(['VIEWER']);
    expect(invalidGrantDates([{ roleCode: 'VIEWER', validFromDate: '2026-10-01', validToDate: null }])).toEqual([]);
    const duplicate = new ApiError('');
    duplicate.errorCode = 'USER.DUPLICATE_LOGIN_ID';
    expect(userFormErrors(duplicate, text)).toEqual([{ name: 'loginId', errors: ['이미 있는 로그인 아이디입니다.'] }]);
    const linked = new ApiError('');
    linked.errorCode = 'USER.PERSON_ALREADY_LINKED';
    linked.fieldErrors = [{ field: 'personPk', message: '이미 계정이 연결된 사람입니다.' }];
    expect(userFormErrors(linked, text)[0]?.name).toBe('personPk');
  });

  it('shows at most three role names and keeps Asia/Seoul available', () => {
    const names = new Map([['VIEWER', '조회']]);
    expect(formatRoles(['VIEWER', 'AUDITOR'], names, (count) => `외 ${count}`)).toBe('조회, AUDITOR');
    expect(formatRoles(['A', 'B', 'C', 'D'], new Map(), (count) => `외 ${count}`)).toBe('A, B, C 외 1');
    expect(timeZoneOptions()).toContain('Asia/Seoul');
  });
});
