import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import { deptHistoryParams, deptListParams, deptTreeParams } from './deptQuery';
import type { Dept, DeptCreateBody, DeptHistory, DeptListQuery, DeptTreeQuery, DeptUpdateBody } from './deptRecord';

const DEPARTMENTS = '/api/mdm/v1/departments';

function deptUrl(deptPk: number, suffix = ''): string {
  return `${DEPARTMENTS}/${deptPk}${suffix}`;
}

export function listDepartments(query: DeptListQuery): Promise<PageResult<Dept>> {
  return rawRequest<PageResult<Dept>>(DEPARTMENTS, {
    method: 'GET',
    params: deptListParams(query),
  });
}

export function listDepartmentTree(query: DeptTreeQuery): Promise<Dept[]> {
  return rawRequest<Dept[]>(`${DEPARTMENTS}/tree`, {
    method: 'GET',
    params: deptTreeParams(query),
  });
}

export function getDepartment(deptPk: number): Promise<Dept> {
  return rawRequest<Dept>(deptUrl(deptPk), { method: 'GET' });
}

export function createDepartment(body: DeptCreateBody): Promise<Dept> {
  return rawRequest<Dept>(DEPARTMENTS, { method: 'POST', data: body });
}

export function updateDepartment(deptPk: number, body: DeptUpdateBody): Promise<Dept> {
  return rawRequest<Dept>(deptUrl(deptPk), { method: 'PUT', data: body });
}

export function closeDepartment(deptPk: number): Promise<void> {
  return rawRequest<void>(deptUrl(deptPk), { method: 'DELETE' });
}

export function restoreDepartment(deptPk: number): Promise<Dept> {
  return rawRequest<Dept>(deptUrl(deptPk, '/restore'), { method: 'POST' });
}

export function listDepartmentHistory(
  deptPk: number,
  query: { page?: number; size?: number; sort?: string[] },
): Promise<PageResult<DeptHistory>> {
  return rawRequest<PageResult<DeptHistory>>(deptUrl(deptPk, '/history'), {
    method: 'GET',
    params: deptHistoryParams(query),
  });
}
