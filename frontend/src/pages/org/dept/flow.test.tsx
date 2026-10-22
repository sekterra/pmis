import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { areaForbidden, setAccessContext } from '@/core/auth/context';
import { localToday } from '@/core/codes/options';
import { guardView } from '@/core/menu/guard';
import type { MyMenu } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import type { Company } from '@/services/mdm/companyRecord';
import type { Dept } from '@/services/mdm/deptRecord';
import DeptPage from './index';

const listCompanies = vi.hoisted(() => vi.fn());
const listDepartmentTree = vi.hoisted(() => vi.fn());
const getDepartment = vi.hoisted(() => vi.fn());
const createDepartment = vi.hoisted(() => vi.fn());
const updateDepartment = vi.hoisted(() => vi.fn());
const closeDepartment = vi.hoisted(() => vi.fn());
const restoreDepartment = vi.hoisted(() => vi.fn());
const listDepartmentHistory = vi.hoisted(() => vi.fn());
const listPeople = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());
const listGroupCodes = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
const replace = vi.hoisted(() => vi.fn());
const push = vi.hoisted(() => vi.fn());
const accessBag = vi.hoisted(() => ({
  can: (_menu: string, _action: string): boolean => true,
}));

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

vi.mock('@ant-design/pro-components', () => {
  const ReactMock = require('react') as typeof import('react');
  return {
    PageContainer: ({ children, title }: { children?: React.ReactNode; title?: string }) =>
      ReactMock.createElement('div', null, title, children),
  };
});

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) => {
      const labels: Record<string, string> = {
        'pages.org.dept.change.CREATE': '생성',
        'pages.org.dept.change.RENAME': '개명',
        'pages.org.dept.change.MOVE': '이동',
        'pages.org.dept.change.CLOSE': '종료',
        'pages.org.dept.change.REOPEN': '재개',
      };
      return defaultMessage || labels[id] || id;
    },
  }),
  useAccess: () => ({ can: (menu: string, action: string) => accessBag.can(menu, action) }),
  useLocation: () => ({ pathname: '/org/dept', search: '' }),
  history: { replace, push },
}));

vi.mock('@/services/mdm/company', () => ({
  listCompanies: (...args: unknown[]) => listCompanies(...args),
}));

vi.mock('@/services/mdm/dept', () => ({
  listDepartmentTree: (...args: unknown[]) => listDepartmentTree(...args),
  getDepartment: (...args: unknown[]) => getDepartment(...args),
  createDepartment: (...args: unknown[]) => createDepartment(...args),
  updateDepartment: (...args: unknown[]) => updateDepartment(...args),
  closeDepartment: (...args: unknown[]) => closeDepartment(...args),
  restoreDepartment: (...args: unknown[]) => restoreDepartment(...args),
  listDepartmentHistory: (...args: unknown[]) => listDepartmentHistory(...args),
}));

vi.mock('@/services/mdm/people', () => ({
  listPeople: (...args: unknown[]) => listPeople(...args),
}));

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: (...args: unknown[]) => fetchLanguages(...args),
}));

vi.mock('@/services/mdm/codes', () => ({
  listGroupCodes: (...args: unknown[]) => listGroupCodes(...args),
}));

function company(): Company {
  return {
    companyPk: 1,
    parentCompanyPk: null,
    parentCompanyCode: null,
    companyCode: 'ACME',
    companyName: '에이씨엠이',
    companyShortName: null,
    companyTypeCode: 'OWN',
    businessRegistrationNo: null,
    corporateRegistrationNo: null,
    representativeName: null,
    nationPk: null,
    nationCode: null,
    postalCode: null,
    address: null,
    addressDetail: null,
    phoneNo: null,
    faxNo: null,
    email: null,
    websiteUrl: null,
    sortOrder: 1,
    description: null,
    useYn: 'Y',
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
  };
}

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
    description: '메모',
    useYn: 'Y',
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
    translations: [{ supportLanguageCode: 'ko-KR', text: patch.deptName }],
    ...patch,
  };
}

const tree = [
  dept({ deptPk: 1, deptCode: 'HQ', deptName: '본사', deptPath: '/1' }),
  dept({ deptPk: 2, deptCode: 'DEV', deptName: '개발본부', deptPath: '/1/2', parentDeptPk: 1, parentDeptCode: 'HQ' }),
  dept({ deptPk: 4, deptCode: 'OLD', deptName: '옛팀', deptPath: '/1/4', parentDeptPk: 1, parentDeptCode: 'HQ', useYn: 'N' }),
];

function menu(actions: string[]): MyMenu {
  return {
    menuCode: 'DEPT',
    parentMenuCode: 'ORG',
    menuName: '부서',
    menuTypeCode: 'PAGE',
    routePath: '/org/unused',
    iconName: null,
    levelNo: 2,
    sortOrder: 20,
    actions,
  };
}

describe('department page', () => {
  beforeEach(() => {
    accessBag.can = () => true;
    replace.mockReset();
    push.mockReset();
    listCompanies.mockReset();
    listDepartmentTree.mockReset();
    getDepartment.mockReset();
    createDepartment.mockReset();
    updateDepartment.mockReset();
    closeDepartment.mockReset();
    restoreDepartment.mockReset();
    listDepartmentHistory.mockReset();
    listPeople.mockReset();
    fetchLanguages.mockReset();
    listGroupCodes.mockReset();
    listCompanies.mockResolvedValue({ items: [company()], total: 1, page: 1, size: 200 });
    listDepartmentTree.mockResolvedValue(tree);
    getDepartment.mockImplementation(async (deptPk: number) => tree.find((item) => item.deptPk === deptPk) ?? tree[1]);
    createDepartment.mockResolvedValue(tree[1]);
    updateDepartment.mockResolvedValue(tree[1]);
    closeDepartment.mockResolvedValue(undefined);
    restoreDepartment.mockResolvedValue(tree[2]);
    listDepartmentHistory.mockResolvedValue({
      items: [
        {
          historyPk: 9,
          deptPk: 2,
          changeTypeCode: 'RENAME',
          effectiveDate: '2026-10-22',
          beforeDeptName: '개발',
          afterDeptName: '개발본부',
          beforeParentDeptPk: 1,
          afterParentDeptPk: 1,
          changeReason: null,
          changeDatetime: '2026-10-22T00:00:00',
          changeUserPk: null,
        },
      ],
      total: 1,
      page: 1,
      size: 20,
    });
    listPeople.mockResolvedValue({
      items: [{ personPk: 7, personName: '홍길동', primaryAssignment: { positionCode: 'MGR', assignmentStatusCode: 'ACTIVE' } }],
      total: 1,
      page: 1,
      size: 20,
    });
    fetchLanguages.mockResolvedValue([{ supportLanguageCode: 'ko-KR', languageName: '한국어' }]);
    listGroupCodes.mockImplementation(async (group: string) => {
      const value = group === 'POSITION' ? 'MGR' : group === 'ASSIGNMENT_STATUS' ? 'ACTIVE' : 'TEAM';
      const name = group === 'POSITION' ? '과장' : group === 'ASSIGNMENT_STATUS' ? '재직' : '팀';
      return [
        {
          codePk: 1,
          groupCode: group,
          codeValue: value,
          codeName: name,
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
        },
      ];
    });
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    cleanup();
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
  });

  async function openDevelopment() {
    render(
      <App>
        <DeptPage />
      </App>,
    );
    await screen.findByText('본사 HQ');
    fireEvent.click(await screen.findByText('개발본부 DEV'));
    await waitFor(() => expect(getDepartment).toHaveBeenCalledWith(2));
  }

  it('loads a company tree and opens history and people once', async () => {
    await openDevelopment();
    expect(listCompanies).toHaveBeenCalledWith({ useYn: 'Y', page: 1, size: 200 });
    expect(listDepartmentTree).toHaveBeenCalledWith({
      companyCode: 'ACME',
      asOfDate: localToday(),
      includeClosed: false,
    });
    expect(replace).toHaveBeenCalledWith({ pathname: '/org/dept', search: '?company=ACME' });
    expect(replace).toHaveBeenCalledWith({ pathname: '/org/dept', search: '?company=ACME&dept=2' });

    fireEvent.click(screen.getByRole('tab', { name: '변경 이력' }));
    await screen.findByText('개명');
    expect(listDepartmentHistory).toHaveBeenCalledTimes(1);
    expect(screen.getByText('1 → 1')).toBeTruthy();
    fireEvent.click(screen.getByRole('tab', { name: '기본 정보' }));
    fireEvent.click(screen.getByRole('tab', { name: '변경 이력' }));
    expect(listDepartmentHistory).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole('tab', { name: '소속 인원' }));
    await screen.findByText('홍길동');
    expect(listPeople).toHaveBeenCalledWith({ deptPk: 2, page: 1, size: 20 });
    expect(screen.getByText('주 소속 기준입니다.')).toBeTruthy();
    fireEvent.click(screen.getByText('홍길동'));
    expect(push).toHaveBeenCalledWith('/org/person?personPk=7');
  });

  it('keeps the newer department when an older history response arrives late', async () => {
    let resolveFirst: (value: unknown) => void = () => undefined;
    listDepartmentHistory.mockReset();
    listDepartmentHistory.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveFirst = resolve;
        }),
    );
    listDepartmentHistory.mockResolvedValueOnce({
      items: [
        {
          historyPk: 2,
          deptPk: 1,
          changeTypeCode: 'CREATE',
          effectiveDate: '2026-01-01',
          beforeDeptName: '본사',
          afterDeptName: '본사후',
          beforeParentDeptPk: null,
          afterParentDeptPk: null,
          changeReason: null,
          changeDatetime: '2026-01-01T00:00:00',
          changeUserPk: null,
        },
      ],
      total: 1,
      page: 1,
      size: 20,
    });
    await openDevelopment();
    fireEvent.click(screen.getByRole('tab', { name: '변경 이력' }));
    await waitFor(() => expect(listDepartmentHistory).toHaveBeenCalledWith(2, { page: 1, size: 20 }));
    fireEvent.click(screen.getByText('본사 HQ'));
    resolveFirst({
      items: [
        {
          historyPk: 1,
          deptPk: 2,
          changeTypeCode: 'RENAME',
          effectiveDate: '2026-02-01',
          beforeDeptName: '늦은',
          afterDeptName: '늦은개발',
          beforeParentDeptPk: 1,
          afterParentDeptPk: 1,
          changeReason: null,
          changeDatetime: '2026-02-01T00:00:00',
          changeUserPk: null,
        },
      ],
      total: 1,
      page: 1,
      size: 20,
    });
    fireEvent.click(screen.getByRole('tab', { name: '변경 이력' }));
    expect(await screen.findByText('본사 → 본사후')).toBeTruthy();
    expect(screen.queryByText('늦은 → 늦은개발')).toBeNull();
  });

  it('adds a child with the parent filled and sends an effective date when the name changes', async () => {
    await openDevelopment();
    fireEvent.click(screen.getByRole('button', { name: '하위 부서 추가' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText('회사')).toHaveValue('ACME');
    fireEvent.change(within(dialog).getByLabelText('부서 코드'), { target: { value: 'DEV1' } });
    fireEvent.change(within(dialog).getByLabelText('부서명'), { target: { value: '개발1팀' } });
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    fireEvent.click(await screen.findByTitle('팀'));
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(createDepartment).toHaveBeenCalled());
    expect(createDepartment.mock.calls[0]?.[0]).toMatchObject({
      companyCode: 'ACME',
      deptCode: 'DEV1',
      parentDeptCode: 'DEV',
      deptName: '개발1팀',
    });
    expect(createDepartment.mock.calls[0]?.[0].effectiveDate).toBeUndefined();

    fireEvent.click(screen.getByRole('button', { name: '수정' }));
    const editor = await screen.findByRole('dialog');
    expect(within(editor).getByLabelText('부서 코드')).toBeDisabled();
    fireEvent.change(within(editor).getByLabelText('부서명'), { target: { value: '개발실' } });
    expect(await within(editor).findByLabelText('적용일')).toBeTruthy();
    fireEvent.click(within(editor).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(updateDepartment).toHaveBeenCalled());
    const updated = updateDepartment.mock.calls[0]?.[1] as Record<string, unknown>;
    expect(updated.deptCode).toBeUndefined();
    expect(updated.companyCode).toBeUndefined();
    expect(updated.description).toBe('메모');
    expect(updated.effectiveDate).toBe(localToday());
  }, 30000);

  it('closes a department and reopens a closed one', async () => {
    await openDevelopment();
    fireEvent.click(screen.getByRole('button', { name: '종료' }));
    const hint = await screen.findByText('종료일이 오늘로 설정되고 이력이 남습니다.');
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '종료 확인' }));
    await waitFor(() => expect(closeDepartment).toHaveBeenCalledWith(2));

    fireEvent.click(await screen.findByText('옛팀 OLD 종료'));
    fireEvent.click(screen.getByRole('button', { name: '재개' }));
    await waitFor(() => expect(restoreDepartment).toHaveBeenCalledWith(4));
  }, 30000);

  it('shows a duplicate on the code field, a server close message, and hides buttons', async () => {
    createDepartment.mockRejectedValueOnce(
      Object.assign(new ApiError('이미 있는 부서 코드입니다.'), { errorCode: 'DEPT.DUPLICATE' }),
    );
    render(
      <App>
        <DeptPage />
      </App>,
    );
    await screen.findByText('본사 HQ');
    fireEvent.click(screen.getByRole('button', { name: '최상위 부서' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('부서 코드'), { target: { value: 'HQ' } });
    fireEvent.change(within(dialog).getByLabelText('부서명'), { target: { value: '본사' } });
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    fireEvent.click(await screen.findByTitle('팀'));
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await within(dialog).findByText('이미 있는 부서 코드입니다.')).toBeTruthy();

    cleanup();
    const rejected = new ApiError('사용 중인 하위 부서가 있습니다.');
    rejected.errorCode = 'DEPT.HAS_CHILD';
    closeDepartment.mockRejectedValueOnce(rejected);
    render(
      <App>
        <DeptPage />
      </App>,
    );
    await screen.findByText('본사 HQ');
    fireEvent.click(await screen.findByText('개발본부 DEV'));
    fireEvent.click(await screen.findByRole('button', { name: '종료' }));
    const hint = await screen.findByText('종료일이 오늘로 설정되고 이력이 남습니다.');
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '종료 확인' }));
    expect(await screen.findByText('사용 중인 하위 부서가 있습니다.')).toBeTruthy();

    cleanup();
    accessBag.can = (menu, action) => menu === 'DEPT' && action === 'READ';
    render(
      <App>
        <DeptPage />
      </App>,
    );
    await screen.findByText('본사 HQ');
    fireEvent.click(await screen.findByText('개발본부 DEV'));
    expect(screen.queryByRole('button', { name: '최상위 부서' })).toBeNull();
    expect(screen.queryByRole('button', { name: '하위 부서 추가' })).toBeNull();
    expect(screen.queryByRole('button', { name: '수정' })).toBeNull();
    expect(screen.queryByRole('button', { name: '종료' })).toBeNull();
    fireEvent.click(screen.getByRole('tab', { name: '소속 인원' }));
    expect(screen.getByText('조회 권한이 없습니다.')).toBeTruthy();
    expect(listPeople).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  it('blocks a direct visit without read and keeps the department route out of the platform area', () => {
    expect(guardView('/org/dept', { status: 'ready', items: [menu(['READ'])] })).toBe('allow');
    expect(guardView('/org/dept', { status: 'ready', items: [menu(['CREATE'])] })).toBe('forbidden');
    setAccessContext({ kind: 'PLATFORM' });
    expect(areaForbidden('/org/dept')).toBe(true);
    setAccessContext({ kind: 'TENANT' });
    expect(areaForbidden('/org/dept')).toBe(false);
  });
});
