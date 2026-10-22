import { describe, expect, it } from 'vitest';
import { deptHistoryParams, deptListParams, deptTreeParams, toCreateBody, toUpdateBody } from './deptQuery';
import type { Dept } from './deptRecord';

const current = {
  deptPk: 2,
  description: '기존 설명',
  deptName: '개발본부',
  parentDeptCode: 'HQ',
} as Dept;

describe('dept query', () => {
  it('builds tree, history, and list parameters', () => {
    expect(deptTreeParams({ companyCode: ' ACME ', asOfDate: '', includeClosed: false })).toEqual({
      companyCode: 'ACME',
    });
    expect(deptTreeParams({ companyCode: 'ACME', asOfDate: '2026-10-22', includeClosed: true })).toEqual({
      companyCode: 'ACME',
      asOfDate: '2026-10-22',
      includeClosed: true,
    });
    expect(deptListParams({ useYn: 'ALL', sort: ['unknown,asc', 'deptName,desc'], page: 1, size: 20 })).toEqual({
      page: 1,
      size: 20,
      sort: ['deptName,desc'],
    });
    expect(deptHistoryParams({ page: 2, sort: ['deptName,asc', 'effectiveDate,desc'] })).toEqual({
      page: 2,
      size: 20,
      sort: ['effectiveDate,desc'],
    });
  });

  it('omits the code and company on update and sends history only when asked', () => {
    const created = toCreateBody('ACME', {
      deptCode: ' DEV ',
      deptName: ' 개발 ',
      deptTypeCode: 'TEAM',
      parentDeptCode: '',
      sortOrder: null,
      validFromDate: '2026-10-22',
      validToDate: ' ',
      names: { 'ko-KR': '개발', 'en-US': ' ' },
    });
    expect(created).toMatchObject({
      companyCode: 'ACME',
      deptCode: 'DEV',
      parentDeptCode: null,
      validToDate: null,
      translations: [{ supportLanguageCode: 'ko-KR', text: '개발' }],
    });
    expect(created).not.toHaveProperty('description');
    const kept = toUpdateBody(
      { deptName: '개발본부', parentDeptCode: 'HQ', deptTypeCode: 'TEAM', names: {} },
      current,
      false,
    );
    expect(kept.effectiveDate).toBeUndefined();
    expect(kept.changeReason).toBeUndefined();
    expect(kept).not.toHaveProperty('deptCode');
    expect(kept).not.toHaveProperty('companyCode');
    expect(kept.description).toBe('기존 설명');
    const tracked = toUpdateBody(
      { deptName: '개발실', parentDeptCode: null, deptTypeCode: 'TEAM', effectiveDate: '2026-10-22', changeReason: '이동', names: {} },
      current,
      true,
    );
    expect(tracked.effectiveDate).toBe('2026-10-22');
    expect(tracked.changeReason).toBe('이동');
  });
});
