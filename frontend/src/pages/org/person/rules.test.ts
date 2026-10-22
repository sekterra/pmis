import { describe, expect, it } from 'vitest';
import { ApiError } from '@/core/request/errors';
import { isFirstCurrentAssignment, assignmentFormErrors, datesOrdered, headNeedsDept, resignedNeedsEnd } from './rules';

const text = (id: string, _fallback: string) => id;

describe('person rules', () => {
  it('requires an end date when resigned and a department for a head', () => {
    expect(datesOrdered('2026-10-22', '2026-10-21')).toBe(false);
    expect(datesOrdered('2026-10-22', undefined)).toBe(true);
    expect(resignedNeedsEnd('RESIGNED', ' ')).toBe(false);
    expect(resignedNeedsEnd('ACTIVE', undefined)).toBe(true);
    expect(headNeedsDept(true, '')).toBe(false);
    expect(headNeedsDept(false, '')).toBe(true);
    expect(isFirstCurrentAssignment([])).toBe(true);
    expect(isFirstCurrentAssignment([{ useYn: 'Y', endDate: null }])).toBe(false);
    expect(isFirstCurrentAssignment([{ useYn: 'Y', endDate: '2026-10-01' }])).toBe(true);
  });

  it('maps assignment errors onto fields', () => {
    const duplicate = new ApiError('');
    duplicate.errorCode = 'ASSIGNMENT.DUPLICATE';
    expect(assignmentFormErrors(duplicate, text)[0]?.name).toBe('deptCode');
    expect(assignmentFormErrors(duplicate, text)[0]?.errors[0]).toBe('pages.org.person.errors.duplicate');
    const employee = new ApiError('이미 있는 사원번호입니다.');
    employee.errorCode = 'ASSIGNMENT.EMPLOYEE_NO_DUPLICATE';
    expect(assignmentFormErrors(employee, text)).toEqual([{ name: 'employeeNo', errors: ['이미 있는 사원번호입니다.'] }]);
    const resigned = new ApiError('');
    resigned.errorCode = 'ASSIGNMENT.RESIGNED_REQUIRES_END';
    expect(assignmentFormErrors(resigned, text)[0]?.name).toBe('endDate');
    const head = new ApiError('');
    head.errorCode = 'ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT';
    expect(assignmentFormErrors(head, text)[0]?.name).toBe('deptHeadYn');
  });
});
