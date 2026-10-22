import { describe, expect, it } from 'vitest';
import { localToday } from '@/core/codes/options';
import {
  assignmentListParams,
  peopleListParams,
  toAssignmentBody,
  toCreateBody,
  toEndBody,
  toUpdateBody,
} from './peopleQuery';
import type { Assignment, Person } from './peopleRecord';

function person(): Person {
  return {
    personPk: 7,
    personName: '홍길동',
    email: 'hong@example.com',
    mobilePhoneNo: '010',
    hasProfileImage: false,
    useYn: 'Y',
    primaryAssignment: null,
    officePhoneNo: '02',
    profileFilePk: null,
    description: '메모',
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
    translations: [],
    assignments: [],
  };
}

describe('people query', () => {
  it('omits a blank use filter and unknown sorts', () => {
    expect(
      peopleListParams({
        keyword: ' 홍 ',
        companyCode: 'ACME',
        deptPk: 3,
        employmentTypeCode: 'FULL',
        assignmentStatusCode: 'ACTIVE',
        useYn: 'ALL',
        sort: ['email,asc', 'unknown,asc'],
        page: 2,
        size: 10,
      }),
    ).toEqual({
      keyword: '홍',
      companyCode: 'ACME',
      deptPk: 3,
      employmentTypeCode: 'FULL',
      assignmentStatusCode: 'ACTIVE',
      sort: ['email,asc'],
      page: 2,
      size: 10,
    });
    expect(assignmentListParams({ useYn: 'Y', sort: ['startDate,desc', 'name,asc'] })).toEqual({
      useYn: 'Y',
      sort: ['startDate,desc'],
      page: 1,
      size: 20,
    });
  });

  it('builds create and update bodies with nulls and kept hidden fields', () => {
    expect(toCreateBody({ personName: ' 홍길동 ', email: ' ', mobilePhoneNo: '', names: { 'ko-KR': ' 길동 ', 'en-US': ' ' } })).toEqual({
      personName: '홍길동',
      email: null,
      mobilePhoneNo: null,
      translations: [{ supportLanguageCode: 'ko-KR', text: '길동' }],
    });
    const updated = toUpdateBody({ personName: '홍길동', email: 'hong@example.com', mobilePhoneNo: '010' }, person());
    expect(updated.officePhoneNo).toBe('02');
    expect(updated.description).toBe('메모');
    expect(updated).not.toHaveProperty('personPk');
  });

  it('sends null department and end defaults', () => {
    const current = { description: '소속 메모' } as Assignment;
    expect(
      toAssignmentBody(
        {
          companyCode: 'ACME',
          deptCode: ' ',
          employeeNo: '',
          employmentTypeCode: 'FULL',
          assignmentStatusCode: 'ACTIVE',
          primaryYn: false,
          deptHeadYn: true,
          startDate: '2026-10-22',
        },
        current,
        true,
      ),
    ).toEqual({
      companyCode: 'ACME',
      deptCode: null,
      employeeNo: null,
      employmentTypeCode: 'FULL',
      positionCode: null,
      jobTitleCode: null,
      assignmentStatusCode: 'ACTIVE',
      primaryYn: 'Y',
      deptHeadYn: 'N',
      startDate: '2026-10-22',
      endDate: null,
      description: '소속 메모',
    });
    expect(toEndBody({}, localToday())).toEqual({ endDate: localToday(), assignmentStatusCode: 'RESIGNED' });
  });
});
