import { render, screen } from '@testing-library/react';
import { App, ConfigProvider, Layout } from 'antd';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ErrorBoundary from '@/components/ErrorBoundary';
import UserMenu from '@/components/RightContent/UserMenu';
import { resetAccessContext, setAccessContext } from '@/core/auth/context';
import { menuDataRender } from '@/core/menu/runtime';
import { resetMenuState } from '@/core/menu/store';
import koMenu from '@/locales/ko-KR/menu';
import koNetwork from '@/locales/ko-KR/network';

const userBag = vi.hoisted(() => ({
  displayName: '관리자',
}));

vi.mock('@umijs/max', () => ({
  history: {
    location: { pathname: '/welcome', search: '', hash: '' },
    push: vi.fn(),
    replace: vi.fn(),
    listen: () => () => undefined,
  },
  Link: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
  useIntl: () => ({
    formatMessage: ({
      id,
      defaultMessage,
    }: {
      id: string;
      defaultMessage?: string;
    }) => {
      const table = { ...koMenu, ...koNetwork } as Record<string, string>;
      return table[id] ?? defaultMessage ?? id;
    },
  }),
  getIntl: () => ({
    formatMessage: ({
      id,
      defaultMessage,
    }: {
      id: string;
      defaultMessage?: string;
    }) => {
      const table = { ...koMenu, ...koNetwork } as Record<string, string>;
      return table[id] ?? defaultMessage ?? id;
    },
  }),
  useModel: () => ({
    initialState: {
      currentUser: {
        userPk: 1,
        loginId: 'admin@example.com',
        displayName: userBag.displayName,
        supportLanguageCode: 'ko-KR',
        timezoneId: 'Asia/Seoul',
        roleCodes: ['TENANT_ADMIN'],
        passwordChangeRequired: false,
      },
    },
    setInitialState: vi.fn(),
  }),
}));

vi.mock('@/core/auth/actions', () => ({
  logoutSession: vi.fn(async () => undefined),
}));

vi.mock('@/core/auth/authApi', () => ({
  logout: vi.fn(),
}));

/** ProLayout 대신 헤더·사이드 사용자 영역만 같은 합성으로 검증한다(vitest에서 ProLayout 로케일 모듈 오류 회피). */
function LayoutShell({ children }: { children: React.ReactNode }) {
  const side = menuDataRender([{ path: '/welcome', name: 'welcome' }]) as {
    name?: string;
    path?: string;
  }[];
  return (
    <Layout style={{ minHeight: 200 }}>
      <Layout.Sider width={160}>
        <ul>
          {side.map((item) => (
            <li key={item.path}>{item.name}</li>
          ))}
        </ul>
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: '#fff' }}>
          <UserMenu>
            <span data-testid="avatar-child">avatar</span>
          </UserMenu>
        </Layout.Header>
        <Layout.Content>{children}</Layout.Content>
      </Layout>
    </Layout>
  );
}

function renderLayout() {
  return render(
    <ConfigProvider>
      <App>
        <ErrorBoundary>
          <LayoutShell>
            <div>본문</div>
          </LayoutShell>
        </ErrorBoundary>
      </App>
    </ConfigProvider>,
  );
}

describe('layout smoke with real Dropdown', () => {
  beforeEach(() => {
    resetAccessContext();
    resetMenuState();
    userBag.displayName = '관리자';
  });

  it('shows the user name in tenant mode without the error screen', () => {
    setAccessContext({ kind: 'TENANT' });
    expect(() => renderLayout()).not.toThrow();
    expect(screen.getByText('관리자')).toBeTruthy();
    expect(screen.queryByText('화면을 표시하지 못했습니다')).toBeNull();
    expect(screen.getByText('본문')).toBeTruthy();
  });

  it('shows the user name in platform mode without the error screen', () => {
    setAccessContext({ kind: 'PLATFORM' });
    userBag.displayName = '플랫폼 관리자';
    expect(() => renderLayout()).not.toThrow();
    expect(screen.getByText('플랫폼 관리자')).toBeTruthy();
    expect(screen.queryByText('화면을 표시하지 못했습니다')).toBeNull();
    expect(screen.getByText('테넌트 관리')).toBeTruthy();
  });
});
