import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { areaForbidden, setAccessContext } from '@/core/auth/context';
import { resetCodeOptionsCache } from '@/core/codes/useCodeOptions';
import { guardView } from '@/core/menu/guard';
import type { MyMenu } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import type { CommonCode } from '@/services/mdm/code';
import type { TenantUser, UserRoleGrant } from '@/services/mdm/usersRecord';
import UserPage from './index';

const listUsers = vi.hoisted(() => vi.fn());
const getUser = vi.hoisted(() => vi.fn());
const createUser = vi.hoisted(() => vi.fn());
const updateUser = vi.hoisted(() => vi.fn());
const deleteUser = vi.hoisted(() => vi.fn());
const restoreUser = vi.hoisted(() => vi.fn());
const changeUserStatus = vi.hoisted(() => vi.fn());
const resetUserPassword = vi.hoisted(() => vi.fn());
const listUserRoles = vi.hoisted(() => vi.fn());
const replaceUserRoles = vi.hoisted(() => vi.fn());
const listRoles = vi.hoisted(() => vi.fn());
const listPeople = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());
const listGroupCodes = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
const accessBag = vi.hoisted(() => ({
  can: (_menu: string, _action: string): boolean => true,
}));
const sessionBag = vi.hoisted(() => ({ userPk: 1 }));

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
    formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) => defaultMessage ?? id,
  }),
  useAccess: () => ({ can: (menu: string, action: string) => accessBag.can(menu, action) }),
  useModel: () => ({ initialState: { currentUser: { userPk: sessionBag.userPk, loginId: 'me' } } }),
}));

vi.mock('@/services/mdm/users', () => ({
  listUsers: (...args: unknown[]) => listUsers(...args),
  getUser: (...args: unknown[]) => getUser(...args),
  createUser: (...args: unknown[]) => createUser(...args),
  updateUser: (...args: unknown[]) => updateUser(...args),
  deleteUser: (...args: unknown[]) => deleteUser(...args),
  restoreUser: (...args: unknown[]) => restoreUser(...args),
  changeUserStatus: (...args: unknown[]) => changeUserStatus(...args),
  resetUserPassword: (...args: unknown[]) => resetUserPassword(...args),
  listUserRoles: (...args: unknown[]) => listUserRoles(...args),
  replaceUserRoles: (...args: unknown[]) => replaceUserRoles(...args),
}));

vi.mock('@/services/mdm/roles', () => ({
  listRoles: (...args: unknown[]) => listRoles(...args),
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

function code(group: string, value: string, name: string): CommonCode {
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

function account(patch: Partial<TenantUser> = {}): TenantUser {
  return {
    userPk: 3,
    loginId: 'hong',
    accountTypeCode: 'NORMAL',
    userStatusCode: 'ACTIVE',
    personPk: 7,
    personName: '홍길동',
    lastLoginDatetime: '2026-10-22T01:00:00',
    mustChangePasswordYn: 'N',
    roleCodes: ['VIEWER', 'TENANT_ADMIN', 'AUDITOR', 'EXTRA'],
    useYn: 'Y',
    supportLanguageCode: 'ko-KR',
    timezoneId: 'Asia/Seoul',
    description: '메모',
    loginFailCount: 1,
    lockedDatetime: null,
    lockType: 'ADMIN',
    passwordChangedDatetime: '2026-10-01T00:00:00',
    passwordExpireDatetime: '2026-11-01T00:00:00',
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
    ...patch,
  };
}

function grant(): UserRoleGrant {
  return {
    userRolePk: 1,
    roleCode: 'VIEWER',
    roleName: '조회',
    validFromDate: null,
    validToDate: null,
    currentlyValid: true,
    useYn: 'Y',
  };
}

function menu(actions: string[]): MyMenu {
  return {
    menuCode: 'USER',
    parentMenuCode: 'SYSTEM',
    menuName: '사용자',
    menuTypeCode: 'PAGE',
    routePath: null,
    iconName: null,
    levelNo: 2,
    sortOrder: 1,
    actions,
  };
}

function storageText(): string {
  const values: string[] = [];
  for (let index = 0; index < localStorage.length; index += 1) {
    values.push(localStorage.getItem(localStorage.key(index) ?? '') ?? '');
  }
  for (let index = 0; index < sessionStorage.length; index += 1) {
    values.push(sessionStorage.getItem(sessionStorage.key(index) ?? '') ?? '');
  }
  return values.join('\n');
}

describe('user page', () => {
  const logs: unknown[][] = [];

  beforeEach(() => {
    accessBag.can = () => true;
    sessionBag.userPk = 1;
    resetCodeOptionsCache();
    rawRequest.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    fetchMock.mockRejectedValue(new Error('network'));
    vi.stubGlobal('fetch', fetchMock);
    logs.length = 0;
    for (const method of ['log', 'info', 'debug', 'warn', 'error'] as const) {
      vi.spyOn(console, method).mockImplementation((...args: unknown[]) => {
        logs.push(args);
      });
    }
    listUsers.mockReset();
    getUser.mockReset();
    createUser.mockReset();
    updateUser.mockReset();
    deleteUser.mockReset();
    restoreUser.mockReset();
    changeUserStatus.mockReset();
    resetUserPassword.mockReset();
    listUserRoles.mockReset();
    replaceUserRoles.mockReset();
    listRoles.mockReset();
    listPeople.mockReset();
    fetchLanguages.mockReset();
    listGroupCodes.mockReset();
    listUsers.mockResolvedValue({
      items: [account(), account({ userPk: 4, loginId: 'old', useYn: 'N', userStatusCode: 'WITHDRAWN', roleCodes: [] })],
      total: 2,
      page: 1,
      size: 20,
    });
    getUser.mockImplementation(async (userPk: number) => account({ userPk, loginId: userPk === 9 ? 'hong.kim' : userPk === 4 ? 'old' : 'hong' }));
    createUser.mockResolvedValue({
      userPk: 9,
      loginId: 'hong.kim',
      temporaryPassword: 'shown-once',
      passwordExpireDatetime: '2026-11-01T00:00:00',
      mustChangePasswordYn: 'Y',
    });
    updateUser.mockImplementation(async () => account());
    deleteUser.mockResolvedValue(undefined);
    restoreUser.mockResolvedValue(account({ userPk: 4, useYn: 'Y' }));
    changeUserStatus.mockResolvedValue(account({ userStatusCode: 'LOCKED' }));
    resetUserPassword.mockResolvedValue({
      userPk: 3,
      loginId: 'hong',
      temporaryPassword: 'shown-once',
      passwordExpireDatetime: '2026-12-01T00:00:00',
      mustChangePasswordYn: 'Y',
    });
    listUserRoles.mockResolvedValue([grant()]);
    replaceUserRoles.mockResolvedValue([grant()]);
    listRoles.mockResolvedValue({
      items: [
        { rolePk: 2, roleCode: 'VIEWER', roleName: '조회' },
        { rolePk: 1, roleCode: 'TENANT_ADMIN', roleName: '관리자' },
        { rolePk: 5, roleCode: 'AUDITOR', roleName: '감사' },
      ],
      total: 3,
      page: 1,
      size: 200,
    });
    listPeople.mockResolvedValue({ items: [{ personPk: 7, personName: '홍길동', email: 'hong@example.com' }], total: 1, page: 1, size: 20 });
    fetchLanguages.mockResolvedValue([{ supportLanguageCode: 'ko-KR', languageName: '한국어' }]);
    listGroupCodes.mockImplementation(async (group: string) =>
      group === 'USER_STATUS'
        ? [code(group, 'ACTIVE', '정상'), code(group, 'LOCKED', '잠금'), code(group, 'DORMANT', '휴면'), code(group, 'WITHDRAWN', '탈퇴')]
        : [code(group, 'NORMAL', '일반'), code(group, 'SYSTEM', '시스템')],
    );
  });

  afterEach(() => {
    setAccessContext({ kind: 'TENANT' });
    cleanup();
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  async function renderPage() {
    render(
      <App>
        <UserPage />
      </App>,
    );
    await screen.findByText('hong');
  }

  it('searches, creates a user, and clears the temporary password', async () => {
    await renderPage();
    expect(listUsers).toHaveBeenCalledWith({
      page: 1,
      size: 20,
      keyword: undefined,
      userStatusCode: undefined,
      accountTypeCode: undefined,
      roleCode: undefined,
      useYn: undefined,
      sort: [],
    });
    expect(listRoles).toHaveBeenCalledWith({ useYn: 'Y', page: 1, size: 200 });
    expect(screen.getByText('조회, 관리자, 감사 외 1')).toBeTruthy();
    expect(listUserRoles).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText('로그인 아이디·사람'), { target: { value: 'hong' } });
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() =>
      expect(listUsers).toHaveBeenLastCalledWith({
        page: 1,
        size: 20,
        keyword: 'hong',
        userStatusCode: undefined,
        accountTypeCode: undefined,
        roleCode: undefined,
        useYn: undefined,
        sort: [],
      }),
    );

    fireEvent.click(screen.getByRole('button', { name: '사용자 등록' }));
    const creator = await screen.findByRole('dialog');
    expect(within(creator).getByRole('tab', { name: '역할' })).toHaveAttribute('aria-disabled', 'true');
    fireEvent.change(within(creator).getByLabelText('로그인 아이디'), { target: { value: 'ab' } });
    fireEvent.click(within(creator).getByRole('button', { name: '저장' }));
    expect(await within(creator).findByText('영문, 숫자, ._@- 만 3~100자입니다.')).toBeTruthy();
    expect(createUser).not.toHaveBeenCalled();
    fireEvent.change(within(creator).getByLabelText('로그인 아이디'), { target: { value: ' Hong.Kim ' } });
    fireEvent.change(within(creator).getByLabelText('설명'), { target: { value: '  ' } });
    fireEvent.click(within(creator).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(createUser).toHaveBeenCalled());
    expect(createUser.mock.calls[0]?.[0]).toMatchObject({
      loginId: 'Hong.Kim',
      accountTypeCode: 'NORMAL',
      personPk: null,
      supportLanguageCode: 'ko-KR',
      timezoneId: 'Asia/Seoul',
      description: null,
      roleCodes: null,
    });
    expect(await screen.findByText('shown-once')).toBeTruthy();
    expect(screen.getByText(/2026-11-01T00:00:00/)).toBeTruthy();
    fireEvent.keyDown(document, { key: 'Escape' });
    const mask = document.querySelector('.ant-modal-mask');
    if (mask) {
      fireEvent.click(mask);
    }
    expect(screen.getByText('shown-once')).toBeTruthy();
    vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValue(new Error('denied'));
    fireEvent.click(screen.getByRole('button', { name: '복사' }));
    expect(await screen.findByText('복사하지 못했습니다. 비밀번호를 선택해서 복사하세요.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '닫기' }));
    await waitFor(() => expect(screen.queryByText('shown-once')).toBeNull());
    expect(storageText()).not.toContain('shown-once');
    expect(JSON.stringify(logs)).not.toContain('shown-once');
    await waitFor(() => expect(getUser).toHaveBeenCalledWith(9));
    expect(screen.getByDisplayValue('hong.kim')).toBeDisabled();
  });

  it('changes status, resets a password, and saves role grants', async () => {
    await renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[0]);
    const dialog = await screen.findByRole('dialog');
    await waitFor(() => expect(getUser).toHaveBeenCalledWith(3));
    expect(within(dialog).getByText(/관리자 잠금/)).toBeTruthy();
    expect(listUserRoles).not.toHaveBeenCalled();

    fireEvent.click(within(dialog).getByRole('button', { name: '잠금' }));
    const lockHint = await screen.findByText('로그인 중인 세션이 종료됩니다.');
    const lockPopup = lockHint.closest('.ant-modal');
    if (!(lockPopup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(lockPopup).getByRole('button', { name: '잠금' }));
    await waitFor(() => expect(changeUserStatus).toHaveBeenCalledWith(3, 'LOCKED'));

    fireEvent.click(screen.getByRole('button', { name: '비밀번호 초기화' }));
    const resetHint = await screen.findByText('새 임시 비밀번호가 발급되고 세션이 종료됩니다.');
    const resetPopup = resetHint.closest('.ant-modal');
    if (!(resetPopup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(resetPopup).getByRole('button', { name: '비밀번호 초기화' }));
    expect(await screen.findByText('shown-once')).toBeTruthy();
    await waitFor(() => expect(resetUserPassword).toHaveBeenCalledWith(3));
    fireEvent.click(screen.getByRole('button', { name: '닫기' }));
    await waitFor(() => expect(screen.queryByText('shown-once')).toBeNull());

    fireEvent.click(within(dialog).getByRole('button', { name: '삭제' }));
    const deleteHint = await screen.findByText('탈퇴 처리되고 세션이 종료됩니다.');
    const deletePopup = deleteHint.closest('.ant-modal');
    if (!(deletePopup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(deletePopup).getByRole('button', { name: '삭제' }));
    await waitFor(() => expect(deleteUser).toHaveBeenCalledWith(3));

    fireEvent.click(screen.getByRole('button', { name: '복구' }));
    const restoreHint = await screen.findByText('정상 상태로 돌아오고 비밀번호 변경이 필요합니다.');
    const restorePopup = restoreHint.closest('.ant-modal');
    if (!(restorePopup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(restorePopup).getByRole('button', { name: '복구' }));
    await waitFor(() => expect(restoreUser).toHaveBeenCalledWith(4));

    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[0]);
    const again = await waitFor(() => {
      const found = screen.getAllByRole('dialog').find((item) => item.className.includes('ant-drawer'));
      if (!(found instanceof HTMLElement) || !within(found).queryByText(/관리자 잠금/)) {
        throw new Error('user not loaded');
      }
      return found;
    });
    fireEvent.click(within(again).getByRole('tab', { name: '역할' }));
    await waitFor(() => expect(listUserRoles).toHaveBeenCalledTimes(1));
    expect(listUserRoles).toHaveBeenCalledWith(3, undefined);
    fireEvent.click(within(again).getByRole('tab', { name: '기본 정보' }));
    fireEvent.click(within(again).getByRole('tab', { name: '역할' }));
    expect(listUserRoles).toHaveBeenCalledTimes(1);
    fireEvent.click(within(again).getByRole('button', { name: '역할 편집' }));
    const editor = (await screen.findAllByRole('dialog')).at(-1);
    if (!(editor instanceof HTMLElement)) {
      throw new Error('editor missing');
    }
    expect(within(editor).getByText('선택에서 뺀 역할은 철회됩니다.')).toBeTruthy();
    fireEvent.click(within(editor).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(replaceUserRoles).toHaveBeenCalledWith(3, [{ roleCode: 'VIEWER', validFromDate: null, validToDate: null }]));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: '역할 편집' })).toBeNull());

    replaceUserRoles.mockRejectedValueOnce(Object.assign(new ApiError('마지막 관리자입니다.'), { errorCode: 'USER.LAST_ADMIN' }));
    fireEvent.click(screen.getByRole('button', { name: '역할 편집' }));
    const editorAgain = (await screen.findAllByRole('dialog')).at(-1);
    if (!(editorAgain instanceof HTMLElement)) {
      throw new Error('editor missing');
    }
    fireEvent.click(within(editorAgain).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('마지막 관리자입니다.')).toBeTruthy();
  });

  it('hides actions by permission and protects the signed-in user', async () => {
    cleanup();
    accessBag.can = (menu, action) => menu === 'USER' && action === 'READ';
    render(
      <App>
        <UserPage />
      </App>,
    );
    await screen.findByText('hong');
    expect(screen.queryByRole('button', { name: '사용자 등록' })).toBeNull();
    expect(screen.queryByRole('button', { name: '삭제' })).toBeNull();
    expect(screen.queryByRole('button', { name: '복구' })).toBeNull();
    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[0]);
    const reader = await screen.findByRole('dialog');
    expect(within(reader).queryByRole('button', { name: '잠금' })).toBeNull();
    fireEvent.click(within(reader).getByRole('tab', { name: '역할' }));
    expect(within(reader).getByText('역할 조회 권한이 없어 역할 코드를 그대로 보여 주고 편집은 숨깁니다.')).toBeTruthy();

    cleanup();
    accessBag.can = (menu) => menu === 'USER';
    render(
      <App>
        <UserPage />
      </App>,
    );
    expect(await screen.findByText('VIEWER, TENANT_ADMIN, AUDITOR 외 1')).toBeTruthy();
    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[0]);
    const coded = await screen.findByRole('dialog');
    fireEvent.click(within(coded).getByRole('tab', { name: '역할' }));
    expect(within(coded).queryByRole('button', { name: '역할 편집' })).toBeNull();
    expect(await within(coded).findByText('VIEWER')).toBeTruthy();

    cleanup();
    accessBag.can = () => true;
    sessionBag.userPk = 3;
    render(
      <App>
        <UserPage />
      </App>,
    );
    await screen.findByText('hong');
    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[0]);
    const self = await screen.findByRole('dialog');
    expect(await within(self).findByText('본인 계정은 변경할 수 없습니다.')).toBeTruthy();
    expect(within(self).queryByRole('button', { name: '잠금' })).toBeNull();
    expect(within(self).queryByRole('button', { name: '삭제' })).toBeNull();
    fireEvent.click(within(self).getByRole('tab', { name: '역할' }));
    expect(within(self).queryByRole('button', { name: '역할 편집' })).toBeNull();

    expect(guardView('/access/user', { status: 'ready', items: [menu(['READ'])] })).toBe('allow');
    expect(guardView('/access/user', { status: 'ready', items: [menu(['CREATE'])] })).toBe('forbidden');
    setAccessContext({ kind: 'PLATFORM' });
    expect(areaForbidden('/access/user')).toBe(true);
    setAccessContext({ kind: 'TENANT' });
    expect(areaForbidden('/access/user')).toBe(false);
  });
});
