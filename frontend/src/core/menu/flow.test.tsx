import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import { clearAccessToken, setAccessToken } from '@/core/auth/token';
import { applyRemoteLogout } from '@/core/auth/broadcast';
import RouteGuard from '@/components/RouteGuard';
import { activeGroup } from '@/pages/mdm/common-code/fixtures';
import CommonCodePage from '@/pages/mdm/common-code';
import { menuNamespace } from '@/pages/mdm/i18n/fixtures';
import I18nPage from '@/pages/mdm/i18n';
import LanguagePage from '@/pages/mdm/language';
import type { LanguageRecord } from '@/services/mdm/languageRecord';
import { resetAccessContext, setAccessContext } from '@/core/auth/context';
import { MenuFailureNotice, menuDataRender, noteAuthenticated } from './runtime';
import { buildSideMenu } from './buildSideMenu';
import { canAction } from './can';
import {
  getMenuSnapshot,
  noteLocaleChanged,
  noteLoggedOut,
  registerUnauthorized,
  resetMenuState,
} from './store';
import type { MyMenu } from './types';

const fetchMyMenus = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
const replace = vi.hoisted(() => vi.fn());
const accessBag = vi.hoisted(() => ({
  can: (_menuCode: string, _action: string): boolean => true,
}));
const guardBag = vi.hoisted(() => ({
  pathname: '/welcome',
  user: undefined as { passwordChangeRequired: boolean } | undefined,
  menu: undefined as { status: string; items: MyMenu[] } | undefined,
}));
const listCodeGroups = vi.hoisted(() => vi.fn());
const listNamespaces = vi.hoisted(() => vi.fn());
const listSupportLanguages = vi.hoisted(() => vi.fn());

vi.mock('@/services/mdm/menu', () => ({
  fetchMyMenus: (...args: unknown[]) => fetchMyMenus(...args),
  MY_MENUS_PATH: '/api/mdm/v1/menus/my',
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
  useAccess: () => ({ can: (menuCode: string, action: string) => accessBag.can(menuCode, action) }),
  useSearchParams: () => [new URLSearchParams(), vi.fn()],
  useModel: () => ({
    initialState: { currentUser: guardBag.user, menu: guardBag.menu, loading: false },
    loading: false,
  }),
  history: {
    get location() {
      return { pathname: guardBag.pathname, search: '', hash: '' };
    },
    replace: (...args: unknown[]) => replace(...args),
    listen: () => () => undefined,
  },
}));

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: vi.fn(async () => []),
}));

vi.mock('@/services/mdm/codes', () => ({
  listCodeGroups: (...args: unknown[]) => listCodeGroups(...args),
  listCommonCodes: vi.fn(async () => ({ items: [], total: 0, page: 1, size: 20 })),
  getCodeGroup: vi.fn(),
  createCodeGroup: vi.fn(),
  updateCodeGroup: vi.fn(),
  deleteCodeGroup: vi.fn(),
  restoreCodeGroup: vi.fn(),
  createCommonCode: vi.fn(),
  updateCommonCode: vi.fn(),
  deleteCommonCode: vi.fn(),
  restoreCommonCode: vi.fn(),
  listCodeHistory: vi.fn(),
}));

vi.mock('@/services/mdm/i18nAdmin', () => ({
  listNamespaces: (...args: unknown[]) => listNamespaces(...args),
  getNamespace: vi.fn(),
  createNamespace: vi.fn(),
  updateNamespace: vi.fn(),
  deleteNamespace: vi.fn(),
  restoreNamespace: vi.fn(),
  listMessages: vi.fn(async () => ({ items: [], total: 0, page: 1, size: 20 })),
  getMessage: vi.fn(),
  createMessage: vi.fn(),
  updateMessage: vi.fn(),
  deleteMessage: vi.fn(),
  restoreMessage: vi.fn(),
}));

vi.mock('@/services/mdm/languageAdmin', () => ({
  listSupportLanguages: (...args: unknown[]) => listSupportLanguages(...args),
  createSupportLanguage: vi.fn(),
  updateSupportLanguage: vi.fn(),
  deleteSupportLanguage: vi.fn(),
  restoreSupportLanguage: vi.fn(),
  listNations: vi.fn(async () => ({ items: [], total: 0, page: 1, size: 20 })),
  createNation: vi.fn(),
  updateNation: vi.fn(),
  deleteNation: vi.fn(),
  restoreNation: vi.fn(),
}));

function menu(partial: Partial<MyMenu> & Pick<MyMenu, 'menuCode' | 'menuName' | 'sortOrder'>): MyMenu {
  return {
    parentMenuCode: 'SYSTEM',
    menuTypeCode: 'PAGE',
    routePath: '/system/unused',
    iconName: null,
    levelNo: 2,
    actions: ['READ'],
    ...partial,
  };
}

const system = menu({
  menuCode: 'SYSTEM',
  parentMenuCode: null,
  menuName: '시스템',
  menuTypeCode: 'GROUP',
  routePath: '/system',
  levelNo: 1,
  sortOrder: 10,
});
const i18n = menu({ menuCode: 'I18N', menuName: '다국어', sortOrder: 10, iconName: 'global' });
const commonCode = menu({ menuCode: 'COMMON_CODE', menuName: '공통코드', sortOrder: 20 });
const company = menu({ menuCode: 'COMPANY', menuName: '회사', sortOrder: 30 });
const english: LanguageRecord = {
  supportLanguageCode: 'en',
  languageName: '영어',
  nativeName: 'English',
  isoLanguageCode: null,
  fallbackLanguageCode: null,
  textDirectionCode: 'LTR',
  dateFormat: null,
  datetimeFormat: null,
  numberFormat: null,
  defaultLanguage: false,
  sortOrder: 2,
  useYn: 'Y',
};
const user = { passwordChangeRequired: false };

describe('server menu', () => {
  beforeEach(() => {
    resetMenuState();
    resetAccessContext();
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    clearAccessToken();
    accessBag.can = () => true;
    guardBag.pathname = '/welcome';
    guardBag.user = undefined;
    guardBag.menu = undefined;
    fetchMyMenus.mockResolvedValue([]);
    listCodeGroups.mockResolvedValue({ items: [activeGroup], total: 1, page: 1, size: 20 });
    listNamespaces.mockResolvedValue({ items: [menuNamespace], total: 1, page: 1, size: 20 });
    listSupportLanguages.mockResolvedValue([english]);
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    cleanup();
    expect(fetchMock).not.toHaveBeenCalled();
    expect(rawRequest).not.toHaveBeenCalled();
  });

  it('nests known screens, drops unknown menus, and hides an empty group', () => {
    const tree = buildSideMenu([company, commonCode, system, i18n], (key) =>
      key === 'menu.language' ? '언어·국가' : key,
    );
    expect(tree.map((item) => item.name)).toEqual(['시스템']);
    expect(tree[0]?.children?.map((item) => [item.name, item.path])).toEqual([
      ['다국어', '/mdm/i18n'],
      ['언어·국가', '/mdm/language'],
      ['공통코드', '/mdm/common-code'],
      ['회사', '/org/company'],
    ]);
    const unknown = menu({ menuCode: 'NOT_A_SCREEN', menuName: '없는메뉴', sortOrder: 40 });
    expect(buildSideMenu([system, unknown], (key) => key)).toEqual([]);
  });

  it('uses the local name for a secondary screen and refreshes the server name', () => {
    const label = (key: string) => (key === 'menu.language' ? '언어·국가' : key);
    const first = buildSideMenu([system, i18n], label);
    const renamed = buildSideMenu([system, { ...i18n, menuName: 'Languages' }], label);
    expect(first[0]?.children?.[0]?.name).toBe('다국어');
    expect(first[0]?.children?.[1]?.name).toBe('언어·국가');
    expect(renamed[0]?.children?.[0]?.name).toBe('Languages');
    expect(renamed[0]?.children?.[1]?.name).toBe('언어·국가');
  });

  it('keeps every action false until the menu is ready', () => {
    const ready = { status: 'ready' as const, items: [{ ...i18n, actions: ['READ', 'UPDATE'] }] };
    expect(canAction(ready, 'I18N', 'UPDATE')).toBe(true);
    expect(canAction(ready, 'I18N', 'DELETE')).toBe(false);
    expect(canAction(ready, 'COMMON_CODE', 'READ')).toBe(false);
    expect(canAction({ status: 'loading', items: ready.items }, 'I18N', 'READ')).toBe(false);
    expect(canAction({ status: 'error', items: [] }, 'I18N', 'READ')).toBe(false);
    expect(canAction(undefined, 'I18N', 'READ')).toBe(false);
  });

  it('loads once after login and session recovery, again after a language change, and not during a forced password change', async () => {
    await noteAuthenticated(user);
    await noteAuthenticated(user);
    expect(fetchMyMenus).toHaveBeenCalledTimes(2);
    fetchMyMenus.mockClear();
    await noteLocaleChanged();
    expect(fetchMyMenus).toHaveBeenCalledTimes(1);
    fetchMyMenus.mockClear();
    await noteAuthenticated({ passwordChangeRequired: true });
    expect(fetchMyMenus).not.toHaveBeenCalled();
    expect(getMenuSnapshot().items).toEqual([]);
  });

  it('loads after a successful password change and clears the menu on logout', async () => {
    await noteAuthenticated({ passwordChangeRequired: true });
    expect(fetchMyMenus).not.toHaveBeenCalled();
    await noteAuthenticated(user);
    expect(fetchMyMenus).toHaveBeenCalledTimes(1);
    expect(getMenuSnapshot().status).toBe('ready');
    noteLoggedOut();
    expect(getMenuSnapshot()).toEqual({ status: 'idle', items: [] });
    applyRemoteLogout();
    expect(getMenuSnapshot().status).toBe('idle');
  });

  it('shares one request when loads overlap', async () => {
    let release: (items: MyMenu[]) => void = () => undefined;
    fetchMyMenus.mockReturnValue(new Promise((resolve) => {
      release = resolve;
    }));
    const first = noteAuthenticated(user);
    const second = noteLocaleChanged();
    await Promise.resolve();
    expect(fetchMyMenus).toHaveBeenCalledTimes(1);
    expect(first).toBe(second);
    release([i18n]);
    await Promise.all([first, second]);
    expect(getMenuSnapshot().items).toEqual([i18n]);
  });

  it('shows a retry after a load failure and recovers without treating 401 as that failure', async () => {
    fetchMyMenus.mockRejectedValueOnce(new Error('down'));
    await noteAuthenticated(user);
    guardBag.pathname = '/mdm/i18n';
    guardBag.user = user;
    guardBag.menu = getMenuSnapshot();
    render(
      <RouteGuard>
        <div>본문</div>
      </RouteGuard>,
    );
    expect(screen.queryByText('본문')).toBeNull();
    render(<MenuFailureNotice />);
    expect(screen.getAllByText('메뉴를 불러오지 못했습니다.').length).toBeGreaterThan(0);
    expect(canAction(getMenuSnapshot(), 'I18N', 'READ')).toBe(false);
    fetchMyMenus.mockResolvedValueOnce([i18n]);
    fireEvent.click(screen.getAllByRole('button', { name: '다시 시도' })[0]);
    await waitFor(() => expect(getMenuSnapshot().status).toBe('ready'));

    const onUnauthorized = vi.fn();
    registerUnauthorized(onUnauthorized);
    const unauthorized = new ApiError('unauthorized');
    unauthorized.status = 401;
    unauthorized.errorCode = 'AUTH.UNAUTHORIZED';
    fetchMyMenus.mockRejectedValueOnce(unauthorized);
    await noteLocaleChanged();
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(getMenuSnapshot().status).toBe('idle');
  });

  it('does not store the access token or the menu in browser storage', async () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem');
    setAccessToken('access-token');
    fetchMyMenus.mockResolvedValueOnce([system, i18n, commonCode]);
    await noteAuthenticated(user);
    const keys = setItem.mock.calls.map((call) => String(call[0]));
    expect(keys.some((key) => /token|menu/i.test(key))).toBe(false);
    expect(keys.join(' ')).not.toContain('access-token');
    setItem.mockRestore();
  });

  it('does not call menus in platform mode', async () => {
    setAccessContext({ kind: 'PLATFORM' });
    await noteAuthenticated(user);
    expect(fetchMyMenus).not.toHaveBeenCalled();
    const rendered = menuDataRender([{ path: '/welcome', name: 'welcome' }]);
    expect(rendered).toHaveLength(1);
    expect(rendered[0]).toMatchObject({ path: '/tenants', name: '테넌트 관리' });
  });

  it('sends a logged-in user to 403 when the path belongs to the other area', () => {
    setAccessContext({ kind: 'PLATFORM' });
    guardBag.pathname = '/mdm/language';
    guardBag.user = user;
    guardBag.menu = { status: 'ready', items: [] };
    render(
      <RouteGuard>
        <div>본문</div>
      </RouteGuard>,
    );
    expect(replace).toHaveBeenCalledWith('/exception/403');
    cleanup();
    resetAccessContext();
    guardBag.pathname = '/tenants/new';
    render(
      <RouteGuard>
        <div>본문</div>
      </RouteGuard>,
    );
    expect(replace).toHaveBeenCalledWith('/exception/403');
  });

  it('sends a direct visit without read access to the 403 page and skips unrelated paths', () => {
    guardBag.pathname = '/mdm/i18n';
    guardBag.user = user;
    guardBag.menu = { status: 'loading', items: [] };
    const { rerender } = render(
      <RouteGuard>
        <div>본문</div>
      </RouteGuard>,
    );
    expect(screen.queryByText('본문')).toBeNull();
    expect(document.querySelector('.ant-spin')).toBeTruthy();

    guardBag.menu = { status: 'ready', items: [{ ...i18n, actions: ['CREATE'] }] };
    rerender(
      <RouteGuard>
        <div>본문</div>
      </RouteGuard>,
    );
    expect(screen.queryByText('본문')).toBeNull();
    expect(replace).toHaveBeenCalledWith('/exception/403');

    guardBag.menu = { status: 'ready', items: [i18n] };
    rerender(
      <RouteGuard>
        <div>본문</div>
      </RouteGuard>,
    );
    expect(screen.getByText('본문')).toBeTruthy();
    cleanup();

    guardBag.pathname = '/welcome';
    guardBag.menu = { status: 'error', items: [] };
    render(
      <RouteGuard>
        <div>환영</div>
      </RouteGuard>,
    );
    expect(screen.getByText('환영')).toBeTruthy();
  });

  it('hides write buttons for a read-only user and shows them when every action is granted', async () => {
    accessBag.can = (_menu, action) => action === 'READ';
    render(
      <App>
        <CommonCodePage />
        <I18nPage />
        <LanguagePage />
      </App>,
    );
    await screen.findByText('상태');
    expect(screen.queryByRole('button', { name: '그룹 등록' })).toBeNull();
    expect(screen.queryByRole('button', { name: '네임스페이스 등록' })).toBeNull();
    expect(screen.queryByRole('button', { name: '언어 등록' })).toBeNull();
    expect(screen.queryByRole('button', { name: '수정' })).toBeNull();
    expect(screen.queryByRole('button', { name: '사용 중지' })).toBeNull();

    cleanup();
    accessBag.can = () => true;
    render(
      <App>
        <CommonCodePage />
        <I18nPage />
        <LanguagePage />
      </App>,
    );
    expect(await screen.findByRole('button', { name: '그룹 등록' })).toBeTruthy();
    await screen.findByText('상태');
    await screen.findByText('메뉴');
    await screen.findByText('영어');
    expect(screen.getByRole('button', { name: '네임스페이스 등록' })).toBeTruthy();
    expect(screen.getByRole('button', { name: '언어 등록' })).toBeTruthy();
    expect(screen.getAllByRole('button', { name: '수정' }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: '사용 중지' }).length).toBeGreaterThan(0);
  });
});
