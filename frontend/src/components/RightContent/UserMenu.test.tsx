import { fireEvent, render, screen } from '@testing-library/react';
import { App, ConfigProvider } from 'antd';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const push = vi.hoisted(() => vi.fn());
const replace = vi.hoisted(() => vi.fn());
const logoutSession = vi.hoisted(() => vi.fn(async () => undefined));
const setInitialState = vi.hoisted(() => vi.fn());
const userBag = vi.hoisted(() => ({
  current: {
    userPk: 1,
    loginId: 'admin@example.com',
    displayName: '관리자',
    supportLanguageCode: 'ko-KR',
    timezoneId: 'Asia/Seoul',
    roleCodes: ['TENANT_ADMIN'],
    passwordChangeRequired: false,
  } as Record<string, unknown>,
}));

vi.mock('@umijs/max', () => ({
  history: {
    push: (...args: unknown[]) => push(...args),
    replace: (...args: unknown[]) => replace(...args),
  },
  useIntl: () => ({
    formatMessage: ({ id, defaultMessage }: { id: string; defaultMessage?: string }) => {
      const map: Record<string, string> = {
        'pages.account.password': '비밀번호 변경',
        'pages.account.logout': '로그아웃',
      };
      return map[id] ?? defaultMessage ?? id;
    },
  }),
  useModel: () => ({
    initialState: { currentUser: userBag.current },
    setInitialState,
  }),
}));

vi.mock('@/core/auth/actions', () => ({
  logoutSession,
}));

vi.mock('@/core/auth/authApi', () => ({
  logout: vi.fn(),
}));

vi.mock('@/core/menu/runtime', () => ({
  getMenuSnapshot: () => ({ status: 'ready', items: [] }),
}));

import UserMenu from './UserMenu';

function renderMenu() {
  return render(
    <ConfigProvider>
      <App>
        <UserMenu>
          <span data-testid="avatar">avatar</span>
        </UserMenu>
      </App>
    </ConfigProvider>,
  );
}

describe('UserMenu with real Dropdown', () => {
  beforeEach(() => {
    push.mockReset();
    replace.mockReset();
    logoutSession.mockClear();
    setInitialState.mockReset();
    userBag.current = {
      userPk: 1,
      loginId: 'admin@example.com',
      displayName: '관리자',
      supportLanguageCode: 'ko-KR',
      timezoneId: 'Asia/Seoul',
      roleCodes: ['TENANT_ADMIN'],
      passwordChangeRequired: false,
    };
  });

  it('renders without Children.only error and shows password and logout', async () => {
    expect(() => renderMenu()).not.toThrow();
    expect(screen.getByText('관리자')).toBeTruthy();
    fireEvent.click(screen.getByText('관리자'));
    expect(await screen.findByText('비밀번호 변경')).toBeTruthy();
    expect(screen.getByText('로그아웃')).toBeTruthy();
  });

  it('hides password change during a support session', async () => {
    userBag.current = {
      ...userBag.current,
      displayName: '플랫폼 지원 (ops)',
      support: {
        supportAccessId: 1,
        operatorLoginId: 'ops',
        expireDatetime: '2099-01-01T00:00:00Z',
      },
    };
    renderMenu();
    fireEvent.click(screen.getByText('플랫폼 지원 (ops)'));
    expect(await screen.findByText('로그아웃')).toBeTruthy();
    expect(screen.queryByText('비밀번호 변경')).toBeNull();
  });
});
