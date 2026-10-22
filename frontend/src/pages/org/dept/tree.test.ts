import { describe, expect, it } from 'vitest';
import type { Dept } from '@/services/mdm/deptRecord';
import { ancestorKeys, defaultExpandedKeys, deptNodeLabel, forbiddenParentPks, nestDepts } from './tree';

function dept(patch: Partial<Dept> & Pick<Dept, 'deptPk' | 'deptCode' | 'deptName' | 'deptPath'>): Dept {
  return {
    companyPk: 1,
    companyCode: 'ACME',
    parentDeptPk: null,
    parentDeptCode: null,
    deptTypeCode: 'TEAM',
    levelNo: patch.deptPath.split('/').filter(Boolean).length,
    sortOrder: 1,
    validFromDate: '2026-01-01',
    validToDate: null,
    description: null,
    useYn: 'Y',
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
    ...patch,
  };
}

const rows = [
  dept({ deptPk: 1, deptCode: 'HQ', deptName: '본사', deptPath: '/1' }),
  dept({ deptPk: 2, deptCode: 'DEV', deptName: '개발본부', deptPath: '/1/2', parentDeptPk: 1, parentDeptCode: 'HQ' }),
  dept({ deptPk: 3, deptCode: 'DEV1', deptName: '개발1팀', deptPath: '/1/2/3', parentDeptPk: 2, parentDeptCode: 'DEV' }),
  dept({ deptPk: 4, deptCode: 'OLD', deptName: '옛팀', deptPath: '/1/4', parentDeptPk: 1, useYn: 'N' }),
  dept({ deptPk: 9, deptCode: 'ORPHAN', deptName: '고아', deptPath: '/8/9', parentDeptPk: 8 }),
];

describe('department tree', () => {
  it('nests by path, keeps order, and lifts a missing parent', () => {
    const tree = nestDepts(rows);
    expect(tree.map((node) => node.deptCode)).toEqual(['HQ', 'ORPHAN']);
    expect(tree[0]?.children.map((node) => node.deptCode)).toEqual(['DEV', 'OLD']);
    expect(tree[0]?.children[0]?.children.map((node) => node.deptCode)).toEqual(['DEV1']);
    expect(deptNodeLabel(rows[3], '종료')).toBe('옛팀 OLD 종료');
    expect(deptNodeLabel(rows[0], '종료')).toBe('본사 HQ');
  });

  it('expands the first level and the ancestors of the selection', () => {
    expect(defaultExpandedKeys(rows)).toEqual(['1']);
    expect(ancestorKeys(rows, 3)).toEqual(['1', '2']);
    expect(defaultExpandedKeys(rows, 3)).toEqual(['1', '2']);
    expect([...forbiddenParentPks(rows, 2)].sort()).toEqual([2, 3]);
  });
});
