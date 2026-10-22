import { describe, expect, it } from 'vitest';
import { ApiError } from '@/core/request/errors';
import type { Dept } from '@/services/mdm/deptRecord';
import { datesOrdered, deptErrorText, deptFormErrors, historyChange, validDeptCode } from './rules';

const current = { deptName: '개발본부', parentDeptCode: 'HQ' } as Dept;

describe('department rules', () => {
  it('checks the code length and the date order', () => {
    expect(validDeptCode('DEV-1')).toBe(true);
    expect(validDeptCode('')).toBe(false);
    expect(validDeptCode('x'.repeat(51))).toBe(false);
    expect(datesOrdered('2026-01-01', '2026-01-01')).toBe(true);
    expect(datesOrdered('2026-02-01', '2026-01-01')).toBe(false);
    expect(datesOrdered('2026-01-01', null)).toBe(true);
  });

  it('tracks a name or parent change and maps errors onto fields', () => {
    expect(historyChange(current, { deptName: '개발본부', parentDeptCode: 'HQ' }).any).toBe(false);
    expect(historyChange(current, { deptName: '개발실', parentDeptCode: 'HQ' })).toMatchObject({
      name: true,
      parent: false,
    });
    expect(historyChange(current, { deptName: '개발실', parentDeptCode: null })).toMatchObject({
      name: true,
      parent: true,
    });
    const duplicate = new ApiError('');
    duplicate.errorCode = 'DEPT.DUPLICATE';
    expect(deptFormErrors(duplicate, (id) => id)[0]).toEqual({
      name: 'deptCode',
      errors: ['pages.org.dept.errors.duplicate'],
    });
    const cycle = new ApiError('순환');
    cycle.errorCode = 'DEPT.PARENT_CYCLE';
    expect(deptFormErrors(cycle, (id) => id)[0]?.name).toBe('parentDeptCode');
    const date = new ApiError('');
    date.errorCode = 'DEPT.INVALID_DATE';
    expect(deptErrorText(date, (_id, fallback) => fallback)).toBe('시작일은 종료일보다 늦을 수 없습니다.');
    expect(deptFormErrors(date, (_id, fallback) => fallback)[0]?.name).toBe('validToDate');
  });
});
