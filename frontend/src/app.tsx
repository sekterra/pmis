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
import { subscribeAuthBroadcast } from '@/core/auth/broadcast';
import { authMode, homePath } from '@/core/auth/mode';
import { refreshAccess } from '@/core/auth/refresh';
import { clearAccessToken } from '@/core/auth/token';
import { loadPublicI18n } from '@/core/i18n/apply';
import {
  createRequestConfig,
  registerRequestRuntime,
} from '@/core/request/runtime';
import * as tenantSession from '@/services/mdm/auth';
import type { Me } from '@/services/mdm/data';
import * as platformSession from '@/services/platform/auth';
import defaultSettings from '../config/defaultSettings';

const sessionApi =
  authMode.mode === 'platform' ? platformSession : tenantSession;

dayjs.extend(relativeTime);

const loginPath = '/user/login';

registerRequestRuntime({
  refresh: () =>
    refreshAccess(sessionApi.refresh, {
      sleep: (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
      onFailure: () => {
        const { pathname, search, hash } = history.location;
        if (pathname !== loginPath) {
          history.replace(
            `${loginPath}?redirect=${encodeURIComponent(pathname + search + hash)}`,
          );
        }
      },
    }),
  goLogin: () => {
    const { pathname, search, hash } = history.location;
    if (pathname !== loginPath) {
      history.replace(
        `${loginPath}?redirect=${encodeURIComponent(pathname + search + hash)}`,
      );
    }
  },
  goPassword: () => {
    if (history.location.pathname !== '/user/password') {
      history.replace('/user/password');
    }
  },
  onAccountDisabled: () => {
    void sessionApi
      .logout()
      .catch(() => undefined)
      .finally(() => {
        clearAccessToken();
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
      return await sessionApi.me();
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
    void startSession({
      pathname: history.location.pathname,
      search: history.location.search,
      loadI18n: loadPublicI18n,
      recover: () =>
        recoverSession({
          refresh: sessionApi.refresh,
          loadMe: sessionApi.me,
          sleep: (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
        }),
    }).then(({ user, next }) => {
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
      }));
    });
    return () => {
      cancelled = true;
    };
  }, [setInitialState]);
  return null;
}

export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
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

export function innerProvider(container: React.ReactNode) {
  return (
    <>
      <SessionBootstrap />
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
