import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { areaForbidden, setAccessContext } from '@/core/auth/context';
import { guardView } from '@/core/menu/guard';
import type { MyMenu } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import type { PermissionGrant, Role } from '@/services/mdm/rolesRecord';
import RolePage from './index';

const listRoles = vi.hoisted(() => vi.fn());
const getRole = vi.hoisted(() => vi.fn());
const createRole = vi.hoisted(() => vi.fn());
const updateRole = vi.hoisted(() => vi.fn());
const deleteRole = vi.hoisted(() => vi.fn());
const restoreRole = vi.hoisted(() => vi.fn());
const listRolePermissions = vi.hoisted(() => vi.fn());
const replaceRolePermissions = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
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
    formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) => defaultMessage ?? id,
  }),
  useAccess: () => ({ can: (menu: string, action: string) => accessBag.can(menu, action) }),
}));

vi.mock('@/services/mdm/roles', () => ({
  listRoles: (...args: unknown[]) => listRoles(...args),
  getRole: (...args: unknown[]) => getRole(...args),
  createRole: (...args: unknown[]) => createRole(...args),
  updateRole: (...args: unknown[]) => updateRole(...args),
  deleteRole: (...args: unknown[]) => deleteRole(...args),
  restoreRole: (...args: unknown[]) => restoreRole(...args),
  listRolePermissions: (...args: unknown[]) => listRolePermissions(...args),
  replaceRolePermissions: (...args: unknown[]) => replaceRolePermissions(...args),
}));

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: (...args: unknown[]) => fetchLanguages(...args),
}));

function role(patch: Partial<Role> = {}): Role {
  return {
    rolePk: 3,
    roleCode: 'VIEWER',
    roleName: '조회',
    sortOrder: 10,
    description: '메모',
    useYn: 'Y',
    userCount: 2,
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
    translations: [{ supportLanguageCode: 'ko-KR', text: '조회자' }],
    ...patch,
  };
}

function grant(menuCode: string, menuName: string, action: string, granted: boolean, permissionPk: number): PermissionGrant {
  return {
    permissionPk,
    permissionCode: `${menuCode}:${action}`,
    menuCode,
    menuName,
    actionCode: action,
    permissionName: action,
    granted,
  };
}

function menu(actions: string[]): MyMenu {
  return {
    menuCode: 'ROLE',
    parentMenuCode: 'SYSTEM',
    menuName: '역할',
    menuTypeCode: 'PAGE',
    routePath: null,
    iconName: null,
    levelNo: 2,
    sortOrder: 1,
    actions,
  };
}

const permissions = [
  grant('COMPANY', '회사', 'READ', false, 1),
  grant('COMPANY', '회사', 'CREATE', false, 2),
  grant('PERSON', '사람', 'READ', true, 3),
];

describe('role page', () => {
  beforeEach(() => {
    accessBag.can = () => true;
    rawRequest.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    fetchMock.mockRejectedValue(new Error('network'));
    vi.stubGlobal('fetch', fetchMock);
    listRoles.mockReset();
    getRole.mockReset();
    createRole.mockReset();
    updateRole.mockReset();
    deleteRole.mockReset();
    restoreRole.mockReset();
    listRolePermissions.mockReset();
    replaceRolePermissions.mockReset();
    fetchLanguages.mockReset();
    listRoles.mockResolvedValue({
      items: [role(), role({ rolePk: 1, roleCode: 'TENANT_ADMIN', roleName: '관리자', userCount: 1 }), role({ rolePk: 4, roleCode: 'OLD', roleName: '예전', useYn: 'N', userCount: 0 })],
      total: 3,
      page: 1,
      size: 20,
    });
    getRole.mockImplementation(async (rolePk: number) => role(rolePk === 1 ? { rolePk: 1, roleCode: 'TENANT_ADMIN', roleName: '관리자' } : { rolePk }));
    createRole.mockImplementation(async (body: { roleCode: string; roleName: string }) => role({ rolePk: 9, roleCode: body.roleCode, roleName: body.roleName }));
    updateRole.mockImplementation(async () => role());
    deleteRole.mockResolvedValue(undefined);
    restoreRole.mockResolvedValue(role({ rolePk: 4, useYn: 'Y' }));
    listRolePermissions.mockResolvedValue(permissions);
    replaceRolePermissions.mockResolvedValue(permissions);
    fetchLanguages.mockResolvedValue([{ supportLanguageCode: 'ko-KR', languageName: '한국어', defaultYn: 'Y', useYn: 'Y' }]);
  });

  afterEach(() => {
    setAccessContext({ kind: 'TENANT' });
    cleanup();
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
  });

  async function renderPage() {
    render(
      <App>
        <RolePage />
      </App>,
    );
    await screen.findByText('VIEWER');
  }

  it('searches, creates a role, edits permissions, and restores', async () => {
    await renderPage();
    expect(listRoles).toHaveBeenCalledWith({ page: 1, size: 20, keyword: undefined, useYn: undefined, sort: [] });
    expect(listRolePermissions).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText('검색어'), { target: { value: '조회' } });
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() => expect(listRoles).toHaveBeenLastCalledWith({ page: 1, size: 20, keyword: '조회', useYn: undefined, sort: [] }));

    fireEvent.click(screen.getByRole('button', { name: '역할 등록' }));
    const creator = await screen.findByRole('dialog');
    expect(within(creator).getByRole('tab', { name: '권한' })).toHaveAttribute('aria-disabled', 'true');
    expect(within(creator).getByText('저장한 뒤에 권한을 설정할 수 있습니다.')).toBeTruthy();
    fireEvent.change(within(creator).getByLabelText('역할 코드'), { target: { value: 'ab' } });
    fireEvent.change(within(creator).getByLabelText('역할명'), { target: { value: '조회 전용' } });
    fireEvent.click(within(creator).getByRole('button', { name: '저장' }));
    expect(createRole).not.toHaveBeenCalled();
    fireEvent.change(within(creator).getByLabelText('역할 코드'), { target: { value: 'VIEWER2' } });
    fireEvent.change(within(creator).getByLabelText('설명'), { target: { value: '  ' } });
    fireEvent.click(within(creator).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(createRole).toHaveBeenCalled());
    const created = createRole.mock.calls[0]?.[0] as Record<string, unknown>;
    expect(created.roleCode).toBe('VIEWER2');
    expect(created.description).toBeNull();
    expect(await screen.findByRole('tab', { name: '권한', selected: true })).toBeTruthy();
    await waitFor(() => expect(listRolePermissions).toHaveBeenCalledTimes(1));
    expect(listRolePermissions).toHaveBeenCalledWith(9);

    const readBox = screen.getByRole('checkbox', { name: '회사 조회' });
    const createBox = screen.getByRole('checkbox', { name: '회사 등록' });
    expect(screen.getByRole('button', { name: '권한 저장' })).toBeDisabled();
    fireEvent.click(createBox);
    expect(createBox).toBeChecked();
    expect(readBox).toBeChecked();
    fireEvent.change(screen.getByLabelText('메뉴 이름'), { target: { value: '사람' } });
    expect(screen.queryByRole('checkbox', { name: '회사 조회' })).toBeNull();
    fireEvent.change(screen.getByLabelText('메뉴 이름'), { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: '권한 저장' }));
    await waitFor(() => expect(replaceRolePermissions).toHaveBeenCalledWith(9, { permissionCodes: ['COMPANY:READ', 'COMPANY:CREATE', 'PERSON:READ'] }));
    expect(await screen.findByText('권한은 즉시 반영되지만 화면 메뉴는 다음 로그인 또는 새로고침 때 바뀝니다.')).toBeTruthy();
    await waitFor(() => expect(listRolePermissions).toHaveBeenCalledTimes(2));

    fireEvent.click(screen.getByRole('checkbox', { name: '회사 등록' }));
    fireEvent.click(screen.getByRole('tab', { name: '기본 정보' }));
    const discard = await screen.findByText('저장하지 않은 권한 변경이 있습니다.');
    const popup = discard.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '버리기' }));
    expect(await screen.findByRole('tab', { name: '기본 정보', selected: true })).toBeTruthy();
    fireEvent.click(screen.getByRole('tab', { name: '권한' }));
    expect(listRolePermissions).toHaveBeenCalledTimes(2);
    expect(screen.getByRole('checkbox', { name: '회사 등록' })).not.toBeChecked();

    fireEvent.click(screen.getAllByRole('button', { name: '복구' })[0]);
    await waitFor(() => expect(restoreRole).toHaveBeenCalledWith(4));
  });

  it('loads permissions once per role and keeps the admin role read only', async () => {
    await renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[0]);
    const dialog = await screen.findByRole('dialog');
    await waitFor(() => expect(getRole).toHaveBeenCalledWith(3));
    expect(within(dialog).getByLabelText('역할 코드')).toBeDisabled();
    expect(listRolePermissions).not.toHaveBeenCalled();
    fireEvent.click(within(dialog).getByRole('tab', { name: '권한' }));
    await waitFor(() => expect(listRolePermissions).toHaveBeenCalledTimes(1));
    fireEvent.click(within(dialog).getByRole('tab', { name: '기본 정보' }));
    fireEvent.click(within(dialog).getByRole('tab', { name: '권한' }));
    expect(listRolePermissions).toHaveBeenCalledTimes(1);
    fireEvent.click(within(dialog).getByRole('button', { name: '취소' }));

    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[1]);
    const admin = await screen.findByRole('dialog');
    expect(within(admin).getByText('테넌트 관리자 역할은 삭제할 수 없습니다. 이름과 설명은 수정할 수 있습니다.')).toBeTruthy();
    expect(within(admin).getByLabelText('역할명')).toBeEnabled();
    fireEvent.click(within(admin).getByRole('tab', { name: '권한' }));
    expect(await screen.findByText('테넌트 관리자 역할의 권한은 바꿀 수 없습니다.')).toBeTruthy();
    expect(screen.getByRole('checkbox', { name: '회사 조회' })).toBeDisabled();
    expect(within(admin).queryByRole('button', { name: '삭제' })).toBeNull();
    expect(screen.getByText('보호 역할')).toBeTruthy();
  });

  it('shows server rejection, duplicate fields, and read-only actions', async () => {
    deleteRole.mockRejectedValue(Object.assign(new ApiError('부여가 남아 있습니다.'), { errorCode: 'ROLE.IN_USE' }));
    await renderPage();
    fireEvent.click(screen.getByRole('button', { name: '삭제' }));
    const hint = await screen.findByText('삭제하면 사용 중지됩니다. 데이터는 지워지지 않습니다.');
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '삭제 확인' }));
    expect(await screen.findByText('부여가 남아 있습니다.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    createRole.mockRejectedValue(Object.assign(new ApiError(''), { errorCode: 'ROLE.DUPLICATE' }));
    fireEvent.click(screen.getByRole('button', { name: '역할 등록' }));
    const creator = (await screen.findAllByRole('dialog')).at(-1);
    if (!(creator instanceof HTMLElement)) {
      throw new Error('drawer missing');
    }
    fireEvent.change(within(creator).getByLabelText('역할 코드'), { target: { value: 'VIEWER' } });
    fireEvent.change(within(creator).getByLabelText('역할명'), { target: { value: '조회' } });
    fireEvent.click(within(creator).getByRole('button', { name: '저장' }));
    expect(await within(creator).findByText('이미 있는 역할 코드입니다.')).toBeTruthy();

    cleanup();
    accessBag.can = (_menu, action) => action === 'READ';
    render(
      <App>
        <RolePage />
      </App>,
    );
    await screen.findByText('VIEWER');
    expect(screen.queryByRole('button', { name: '역할 등록' })).toBeNull();
    expect(screen.queryByRole('button', { name: '삭제' })).toBeNull();
    expect(screen.queryByRole('button', { name: '복구' })).toBeNull();
    expect(guardView('/access/role', { status: 'ready', items: [menu(['READ'])] })).toBe('allow');
    expect(guardView('/access/role', { status: 'ready', items: [menu(['CREATE'])] })).toBe('forbidden');
    setAccessContext({ kind: 'PLATFORM' });
    expect(areaForbidden('/access/role')).toBe(true);
    setAccessContext({ kind: 'TENANT' });
    expect(areaForbidden('/access/role')).toBe(false);
  });

  it('shows a menu-row error when read permission is missing', async () => {
    replaceRolePermissions.mockRejectedValue(
      Object.assign(new ApiError('조회가 필요합니다.'), {
        errorCode: 'ROLE.READ_REQUIRED',
        fieldErrors: [{ field: 'COMPANY', message: '조회가 필요합니다.' }],
      }),
    );
    await renderPage();
    fireEvent.click(screen.getAllByRole('button', { name: '상세' })[0]);
    const dialog = await screen.findByRole('dialog');
    fireEvent.click(within(dialog).getByRole('tab', { name: '권한' }));
    fireEvent.click(await screen.findByRole('checkbox', { name: '회사 등록' }));
    const savePermissions = screen.getByRole('button', { name: '권한 저장' });
    await waitFor(() => expect(savePermissions).toBeEnabled());
    fireEvent.click(savePermissions);
    await waitFor(() => expect(replaceRolePermissions).toHaveBeenCalled());
    expect(await screen.findByText('조회가 필요합니다.')).toBeTruthy();
  });
});
