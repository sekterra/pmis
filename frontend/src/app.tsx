import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { history, Link, useModel } from '@umijs/max';
import { App } from 'antd';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import React, { useEffect } from 'react';
import {
  ErrorBoundary,
  FeedbackBridge,
  Footer,
  LanguageSelect,
  OfflineBanner,
  RouteGuard,
  UserMenu,
} from '@/components';
import BrandMark from '@/components/BrandMark';
import { recoverSession, startSession } from '@/core/auth/actions';
import * as authApi from '@/core/auth/authApi';
import { subscribeAuthBroadcast } from '@/core/auth/broadcast';
import { loadAccessContext } from '@/core/auth/context';
import { homePath } from '@/core/auth/mode';
import { refreshAccess } from '@/core/auth/refresh';
import { clearAccessToken } from '@/core/auth/token';
import { loadPublicI18n } from '@/core/i18n/apply';
import {
  MenuFailureNotice,
  getMenuSnapshot,
  menuDataRender,
  noteAuthenticated,
  noteLoggedOut,
  registerUnauthorized,
  subscribeMenu,
} from '@/core/menu/runtime';
import {
  createRequestConfig,
  registerRequestRuntime,
} from '@/core/request/runtime';
import type { Me } from '@/services/mdm/data';
import defaultSettings from '../config/defaultSettings';

dayjs.extend(relativeTime);

const loginPath = '/user/login';

function leaveToLogin(): void {
  noteLoggedOut();
  const { pathname, search, hash } = history.location;
  if (pathname !== loginPath) {
    history.replace(`${loginPath}?redirect=${encodeURIComponent(pathname + search + hash)}`);
  }
}

registerUnauthorized(leaveToLogin);

registerRequestRuntime({
  refresh: () =>
    refreshAccess(authApi.refresh, {
      sleep: (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
      onFailure: leaveToLogin,
    }),
  goLogin: leaveToLogin,
  goPassword: () => {
    if (history.location.pathname !== '/user/password') {
      history.replace('/user/password');
    }
  },
  onAccountDisabled: () => {
    void authApi
      .logout()
      .catch(() => undefined)
      .finally(() => {
        clearAccessToken();
        noteLoggedOut();
        history.replace(loginPath);
      });
  },
});

export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  currentUser?: Me;
  loading?: boolean;
  fetchUserInfo?: () => Promise<Me | undefined>;
}> {
  const fetchUserInfo = async () => {
    try {
      return await authApi.me();
    } catch {
      return undefined;
    }
  };
  return {
    fetchUserInfo,
    loading: true,
    settings: defaultSettings as Partial<LayoutSettings>,
  };
}

function SessionBootstrap() {
  const { setInitialState } = useModel('@@initialState');
  useEffect(() => {
    let cancelled = false;
    void (async () => {
      const context = await loadAccessContext();
      if (cancelled) {
        return;
      }
      if (context !== 'ready') {
        setInitialState((current) => ({
          ...current,
          loading: false,
          contextStatus: context,
        }));
        return;
      }
      const { user, next } = await startSession({
        pathname: history.location.pathname,
        search: history.location.search,
        loadI18n: loadPublicI18n,
        recover: () =>
          recoverSession({
            refresh: authApi.refresh,
            loadMe: authApi.me,
            sleep: (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
          }),
      });
      if (cancelled) {
        return;
      }
      await noteAuthenticated(user);
      if (cancelled) {
        return;
      }
      if (next) {
        history.replace(next);
      }
      setInitialState((current) => ({
        ...current,
        currentUser: user ?? undefined,
        loading: false,
        contextStatus: 'ready',
        menu: getMenuSnapshot(),
      }));
    })();
    return () => {
      cancelled = true;
    };
  }, [setInitialState]);
  return null;
}

export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
    menuDataRender: (menuData) => menuDataRender(menuData ?? []),
    menuContentRender: (_props, defaultDom) => (
      <>
        <MenuFailureNotice />
        {defaultDom}
      </>
    ),
    menuItemRender: (item, dom) => {
      if (item.path) {
        return (
          <Link to={item.path} prefetch>
            {dom}
          </Link>
        );
      }
      return dom;
    },
    actionsRender: () => [<LanguageSelect key="lang" />],
    avatarProps: {
      title: initialState?.currentUser?.displayName,
      render: (_, avatarChildren) => <UserMenu>{avatarChildren}</UserMenu>,
    },
    footerRender: () => <Footer />,
    menuHeaderRender: () => (
      <Link to={homePath()}>
        <BrandMark />
      </Link>
    ),
    ErrorBoundary,
    childrenRender: (children) => children,
    ...initialState?.settings,
  };
};

export const request: RequestConfig = createRequestConfig();

function MenuSync() {
  const { setInitialState } = useModel('@@initialState');
  useEffect(() => {
    return subscribeMenu(() => {
      setInitialState((current) => ({ ...current, menu: getMenuSnapshot() }));
    });
  }, [setInitialState]);
  return null;
}

export function innerProvider(container: React.ReactNode) {
  return (
    <>
      <SessionBootstrap />
      <MenuSync />
      <RouteGuard>{container}</RouteGuard>
    </>
  );
}

function AuthBroadcast(): React.ReactElement | null {
  useEffect(() => {
    return subscribeAuthBroadcast(() => {
      history.replace(loginPath);
    });
  }, []);
  return null;
}

export function rootContainer(container: React.ReactNode) {
  return (
    <App>
      <FeedbackBridge />
      <AuthBroadcast />
      <OfflineBanner />
      <ErrorBoundary>{container}</ErrorBoundary>
    </App>
  );
}
