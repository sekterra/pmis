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
import type { Person } from '@/services/mdm/peopleRecord';
import PersonPage from './index';

const listPeople = vi.hoisted(() => vi.fn());
const getPerson = vi.hoisted(() => vi.fn());
const createPerson = vi.hoisted(() => vi.fn());
const updatePerson = vi.hoisted(() => vi.fn());
const deletePerson = vi.hoisted(() => vi.fn());
const restorePerson = vi.hoisted(() => vi.fn());
const listAssignments = vi.hoisted(() => vi.fn());
const createAssignment = vi.hoisted(() => vi.fn());
const updateAssignment = vi.hoisted(() => vi.fn());
const endAssignment = vi.hoisted(() => vi.fn());
const deleteAssignment = vi.hoisted(() => vi.fn());
const restoreAssignment = vi.hoisted(() => vi.fn());
const listCompanies = vi.hoisted(() => vi.fn());
const listDepartmentTree = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());
const listGroupCodes = vi.hoisted(() => vi.fn());
const uploadFile = vi.hoisted(() => vi.fn());
const assignProfileImage = vi.hoisted(() => vi.fn());
const clearProfileImage = vi.hoisted(() => vi.fn());
const fetchProfileImage = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
const replace = vi.hoisted(() => vi.fn());
const searchBag = vi.hoisted(() => ({ value: '' }));
const accessBag = vi.hoisted(() => ({
  can: (_menu: string, _action: string): boolean => true,
}));

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
  rawBlob: vi.fn(),
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
    formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) => defaultMessage ?? id,
  }),
  useAccess: () => ({ can: (menu: string, action: string) => accessBag.can(menu, action) }),
  useLocation: () => ({ pathname: '/org/person', search: searchBag.value }),
  history: { replace, push: vi.fn() },
}));

vi.mock('@/services/mdm/people', () => ({
  listPeople: (...args: unknown[]) => listPeople(...args),
  getPerson: (...args: unknown[]) => getPerson(...args),
  createPerson: (...args: unknown[]) => createPerson(...args),
  updatePerson: (...args: unknown[]) => updatePerson(...args),
  deletePerson: (...args: unknown[]) => deletePerson(...args),
  restorePerson: (...args: unknown[]) => restorePerson(...args),
  listAssignments: (...args: unknown[]) => listAssignments(...args),
  createAssignment: (...args: unknown[]) => createAssignment(...args),
  updateAssignment: (...args: unknown[]) => updateAssignment(...args),
  endAssignment: (...args: unknown[]) => endAssignment(...args),
  deleteAssignment: (...args: unknown[]) => deleteAssignment(...args),
  restoreAssignment: (...args: unknown[]) => restoreAssignment(...args),
}));

vi.mock('@/services/mdm/company', () => ({
  listCompanies: (...args: unknown[]) => listCompanies(...args),
}));

vi.mock('@/services/mdm/dept', () => ({
  listDepartmentTree: (...args: unknown[]) => listDepartmentTree(...args),
}));

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: (...args: unknown[]) => fetchLanguages(...args),
}));

vi.mock('@/services/mdm/codes', () => ({
  listGroupCodes: (...args: unknown[]) => listGroupCodes(...args),
}));

vi.mock('@/services/mdm/files', () => ({
  uploadFile: (...args: unknown[]) => uploadFile(...args),
  assignProfileImage: (...args: unknown[]) => assignProfileImage(...args),
  clearProfileImage: (...args: unknown[]) => clearProfileImage(...args),
  fetchProfileImage: (...args: unknown[]) => fetchProfileImage(...args),
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

function person(patch: Partial<Person> = {}): Person {
  return {
    personPk: 7,
    personName: '홍길동',
    email: 'hong@example.com',
    mobilePhoneNo: '010-0000-0000',
    hasProfileImage: false,
    useYn: 'Y',
    primaryAssignment: {
      companyCode: 'ACME',
      companyName: '에이씨엠이',
      deptCode: 'DEV',
      deptName: '개발본부',
      positionCode: 'MGR',
      jobTitleCode: null,
      employeeNo: 'E0001',
      assignmentStatusCode: 'ACTIVE',
    },
    officePhoneNo: '02',
    profileFilePk: null,
    description: '메모',
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
    translations: [],
    assignments: [],
    ...patch,
  };
}

function menu(actions: string[]): MyMenu {
  return {
    menuCode: 'PERSON',
    parentMenuCode: 'ORG',
    menuName: '사람',
    menuTypeCode: 'PAGE',
    routePath: '/org/unused',
    iconName: null,
    levelNo: 2,
    sortOrder: 30,
    actions,
  };
}

function code(group: string, value: string, name: string) {
  return {
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
  };
}

describe('person page', () => {
  beforeEach(() => {
    accessBag.can = () => true;
    searchBag.value = '';
    replace.mockReset();
    listPeople.mockReset();
    getPerson.mockReset();
    createPerson.mockReset();
    updatePerson.mockReset();
    deletePerson.mockReset();
    restorePerson.mockReset();
    listAssignments.mockReset();
    createAssignment.mockReset();
    updateAssignment.mockReset();
    endAssignment.mockReset();
    deleteAssignment.mockReset();
    restoreAssignment.mockReset();
    listCompanies.mockReset();
    listDepartmentTree.mockReset();
    fetchLanguages.mockReset();
    listGroupCodes.mockReset();
    uploadFile.mockReset();
    assignProfileImage.mockReset();
    clearProfileImage.mockReset();
    fetchProfileImage.mockReset();
    listPeople.mockResolvedValue({ items: [person()], total: 1, page: 1, size: 20 });
    getPerson.mockResolvedValue(person());
    createPerson.mockResolvedValue(person({ assignments: [] }));
    updatePerson.mockImplementation(async () => person());
    deletePerson.mockResolvedValue(undefined);
    restorePerson.mockResolvedValue(person());
    listAssignments.mockResolvedValue({ items: [], total: 0, page: 1, size: 20 });
    createAssignment.mockResolvedValue({ assignmentPk: 9 });
    endAssignment.mockResolvedValue({ assignmentPk: 9 });
    restoreAssignment.mockResolvedValue({ assignmentPk: 8 });
    listCompanies.mockResolvedValue({ items: [company()], total: 1, page: 1, size: 200 });
    listDepartmentTree.mockResolvedValue([]);
    fetchLanguages.mockResolvedValue([{ supportLanguageCode: 'ko-KR', languageName: '한국어' }]);
    listGroupCodes.mockImplementation(async (group: string) => {
      if (group === 'EMPLOYMENT_TYPE') {
        return [code(group, 'FULL', '정규직')];
      }
      if (group === 'ASSIGNMENT_STATUS') {
        return [code(group, 'ACTIVE', '재직')];
      }
      if (group === 'POSITION') {
        return [code(group, 'MGR', '과장')];
      }
      return [];
    });
    fetchProfileImage.mockRejectedValue(Object.assign(new ApiError(''), { errorCode: 'FILE.NOT_FOUND' }));
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    cleanup();
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
  });

  async function renderPage() {
    render(
      <App>
        <PersonPage />
      </App>,
    );
    await screen.findByText('홍길동');
  }

  it('searches the list and opens a person from a row', async () => {
    await renderPage();
    expect(listPeople).toHaveBeenCalledWith({ page: 1, size: 20 });
    expect(screen.getByText('에이씨엠이')).toBeTruthy();
    expect(screen.getByText('E0001')).toBeTruthy();
    expect(screen.getByText('과장')).toBeTruthy();
    expect(screen.getByText('사진 없음')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('검색어'), { target: { value: '홍' } });
    fireEvent.mouseDown(screen.getByLabelText('회사'));
    fireEvent.click(await screen.findByTitle('ACME 에이씨엠이'));
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() =>
      expect(listPeople).toHaveBeenCalledWith({ keyword: '홍', companyCode: 'ACME', page: 1, size: 20 }),
    );
    fireEvent.click(screen.getByText('홍길동'));
    await waitFor(() => expect(getPerson).toHaveBeenCalledWith(7));
    expect(replace).toHaveBeenCalledWith({ pathname: '/org/person', search: '?personPk=7' });
  });

  it('creates a person and adds the first assignment as primary', async () => {
    await renderPage();
    fireEvent.click(screen.getByRole('button', { name: '사람 등록' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('저장한 뒤에 소속과 프로필 이미지를 등록할 수 있습니다.')).toBeTruthy();
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '홍길동' } });
    fireEvent.change(within(dialog).getByLabelText('이메일'), { target: { value: 'hong@example.com' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(createPerson).toHaveBeenCalled());
    const created = createPerson.mock.calls[0][0] as { personName: string; email: string; officePhoneNo?: string };
    expect(created.personName).toBe('홍길동');
    expect(created.email).toBe('hong@example.com');
    expect(created.officePhoneNo).toBeUndefined();
    expect(await screen.findByRole('tab', { name: '소속', selected: true })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: '소속 추가' }));
    const formHint = await screen.findByText('첫 소속은 주 소속이 됩니다.');
    const formNode = formHint.closest('.ant-modal');
    if (!(formNode instanceof HTMLElement)) {
      throw new Error('assignment form missing');
    }
    const form = formNode;
    expect(within(form).getByText('첫 소속은 주 소속이 됩니다.')).toBeTruthy();
    expect(within(form).getByRole('checkbox', { name: '주 소속' })).toBeDisabled();
    fireEvent.mouseDown(within(form).getByLabelText('회사'));
    fireEvent.click(await screen.findByTitle('ACME 에이씨엠이'));
    fireEvent.mouseDown(within(form).getByLabelText('고용 유형'));
    fireEvent.click(await screen.findByTitle('정규직'));
    fireEvent.mouseDown(within(form).getByLabelText('재직 상태'));
    fireEvent.click(await screen.findByTitle('재직'));
    fireEvent.click(within(form).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(createAssignment).toHaveBeenCalled());
    const body = createAssignment.mock.calls[0][1] as { primaryYn: string; deptCode: string | null; companyCode: string };
    expect(body.companyCode).toBe('ACME');
    expect(body.deptCode).toBeNull();
    expect(body.primaryYn).toBe('Y');
  }, 30000);

  it('warns before changing the primary assignment, then ends and restores', async () => {
    getPerson.mockResolvedValue(
      person({
        assignments: [
          {
            assignmentPk: 9,
            personPk: 7,
            companyPk: 1,
            companyCode: 'ACME',
            companyName: '에이씨엠이',
            deptPk: null,
            deptCode: null,
            deptName: null,
            employeeNo: 'E0001',
            employmentTypeCode: 'FULL',
            positionCode: 'MGR',
            jobTitleCode: null,
            assignmentStatusCode: 'ACTIVE',
            primaryYn: 'Y',
            deptHeadYn: 'N',
            startDate: '2026-01-01',
            endDate: null,
            description: '소속 메모',
            useYn: 'Y',
            createDatetime: null,
            createUserPk: null,
            updateDatetime: null,
            updateUserPk: null,
          },
        ],
      }),
    );
    listAssignments.mockResolvedValue({
      items: [
        {
          assignmentPk: 9,
          personPk: 7,
          companyCode: 'ACME',
          companyName: '에이씨엠이',
          deptCode: null,
          deptName: null,
          employeeNo: 'E0001',
          employmentTypeCode: 'FULL',
          positionCode: 'MGR',
          jobTitleCode: null,
          assignmentStatusCode: 'ACTIVE',
          primaryYn: 'Y',
          deptHeadYn: 'N',
          startDate: '2026-01-01',
          endDate: null,
          description: '소속 메모',
          useYn: 'Y',
        },
        {
          assignmentPk: 8,
          personPk: 7,
          companyCode: 'ACME',
          companyName: '에이씨엠이',
          deptCode: null,
          deptName: null,
          employeeNo: null,
          employmentTypeCode: 'FULL',
          positionCode: null,
          jobTitleCode: null,
          assignmentStatusCode: 'ACTIVE',
          primaryYn: 'N',
          deptHeadYn: 'N',
          startDate: '2025-01-01',
          endDate: '2025-12-31',
          description: null,
          useYn: 'N',
        },
      ],
      total: 2,
      page: 1,
      size: 20,
    });
    await renderPage();
    fireEvent.click(screen.getByRole('button', { name: '상세' }));
    await screen.findByRole('dialog');
    fireEvent.click(screen.getByRole('tab', { name: '소속' }));
    await screen.findByText('2026-01-01 ~ 없음');
    fireEvent.click(screen.getByRole('button', { name: '소속 추가' }));
    expect(await screen.findByText('주 소속으로 바꾸면 기존 주 소속이 해제됩니다.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '취소' }));
    fireEvent.click(screen.getByRole('button', { name: '종료' }));
    expect(await screen.findByText('소속을 종료할까요?')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '종료 확인' }));
    await waitFor(() =>
      expect(endAssignment).toHaveBeenCalledWith(9, { endDate: localToday(), assignmentStatusCode: 'RESIGNED' }),
    );
    fireEvent.click(screen.getByRole('button', { name: '복구' }));
    await waitFor(() => expect(restoreAssignment).toHaveBeenCalledWith(8));
  }, 30000);

  it('shows a server delete message, hides actions, and treats a missing photo as empty', async () => {
    const rejected = new ApiError('사용 중인 계정이 있습니다.');
    rejected.errorCode = 'PERSON.HAS_ACCOUNT';
    deletePerson.mockRejectedValueOnce(rejected);
    await renderPage();
    fireEvent.click(screen.getByRole('button', { name: '상세' }));
    fireEvent.click(await screen.findByRole('button', { name: '삭제' }));
    const hint = await screen.findByText('삭제하면 사용 중지됩니다. 데이터는 지워지지 않습니다.');
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '삭제 확인' }));
    expect(await screen.findByText('사용 중인 계정이 있습니다.')).toBeTruthy();

    cleanup();
    accessBag.can = (menu, action) => menu === 'PERSON' && action === 'READ';
    await renderPage();
    expect(screen.queryByRole('button', { name: '사람 등록' })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: '상세' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).queryByRole('button', { name: '삭제' })).toBeNull();
    fireEvent.click(within(dialog).getByRole('tab', { name: '소속' }));
    expect(within(dialog).queryByRole('button', { name: '소속 추가' })).toBeNull();

    cleanup();
    accessBag.can = (menu, action) => menu === 'PERSON' && (action === 'READ' || action === 'UPDATE');
    getPerson.mockResolvedValue(person({ hasProfileImage: true }));
    await renderPage();
    fireEvent.click(screen.getByRole('button', { name: '상세' }));
    const photoDialog = await screen.findByRole('dialog');
    expect(within(photoDialog).getByText('파일을 올릴 권한이 없습니다.')).toBeTruthy();
    expect(within(photoDialog).queryByLabelText('이미지 올리기')).toBeNull();
    expect(await within(photoDialog).findByText('사진 없음')).toBeTruthy();
    expect(fetchProfileImage).toHaveBeenCalledWith(7);
  });

  it('opens the drawer from a person query and blocks the route outside the tenant area', async () => {
    searchBag.value = '?personPk=7';
    render(
      <App>
        <PersonPage />
      </App>,
    );
    expect(await screen.findByRole('dialog')).toBeTruthy();
    await waitFor(() => expect(getPerson).toHaveBeenCalledWith(7));
    expect(guardView('/org/person', { status: 'ready', items: [menu(['READ'])] })).toBe('allow');
    expect(guardView('/org/person', { status: 'ready', items: [menu(['CREATE'])] })).toBe('forbidden');
    setAccessContext({ kind: 'PLATFORM' });
    expect(areaForbidden('/org/person')).toBe(true);
    setAccessContext({ kind: 'TENANT' });
    expect(areaForbidden('/org/person')).toBe(false);
  });
});
