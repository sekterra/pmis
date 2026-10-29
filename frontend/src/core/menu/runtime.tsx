import {
  AppstoreOutlined,
  GlobalOutlined,
  HomeOutlined,
  SettingOutlined,
  TableOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { useIntl } from '@umijs/max';
import { Button } from 'antd';
import React, { useSyncExternalStore } from 'react';
import { accessKind, areaForbidden } from '@/core/auth/context';
import { getI18nState } from '@/core/i18n/store';
import enMenu from '@/locales/en-US/menu';
import koMenu from '@/locales/ko-KR/menu';
import { buildSideMenu } from './buildSideMenu';
import { canAction } from './can';
import { guardView, type GuardView } from './guard';
import {
  getMenuSnapshot,
  noteAuthenticated,
  noteLocaleChanged,
  noteLoggedOut,
  registerUnauthorized,
  retryMenus,
  subscribeMenu,
} from './store';
import type { MenuAction, MenuSnapshot, SideMenuNode } from './types';

const ICONS: Record<string, React.ComponentType> = {
  appstore: AppstoreOutlined,
  global: GlobalOutlined,
  home: HomeOutlined,
  setting: SettingOutlined,
  table: TableOutlined,
};

function localLabel(key: string): string {
  const table = getI18nState().locale === 'en-US' ? enMenu : koMenu;
  return table[key as keyof typeof table] ?? key;
}

function withIcon(node: SideMenuNode): SideMenuNode & { icon: React.ReactNode } {
  const Icon = (node.iconName && ICONS[node.iconName]) || AppstoreOutlined;
  return {
    ...node,
    icon: <Icon />,
    children: node.children?.map(withIcon),
  };
}

export function menuDataRender<T>(items: T[]): T[] {
  if (accessKind() === 'PLATFORM') {
    return [
      {
        path: '/tenants',
        name: localLabel('menu.tenants'),
        icon: <TeamOutlined />,
      } as T,
      {
        path: '/support-accesses',
        name: localLabel('menu.supportAccesses'),
        icon: <SettingOutlined />,
      } as T,
    ];
  }
  const serverItems = buildSideMenu(getMenuSnapshot().items, localLabel).map(withIcon);
  return [...(serverItems as T[]), ...items];
}

export function viewFor(
  pathname: string,
  menu: MenuSnapshot | undefined,
  hasUser: boolean,
): GuardView {
  if (!hasUser) {
    return 'allow';
  }
  if (areaForbidden(pathname)) {
    return 'forbidden';
  }
  if (accessKind() === 'PLATFORM') {
    return 'allow';
  }
  return guardView(pathname, menu);
}

export function canMenu(
  menu: MenuSnapshot | undefined,
  menuCode: string,
  action: MenuAction,
): boolean {
  if (accessKind() === 'PLATFORM') {
    return false;
  }
  return canAction(menu, menuCode, action);
}

export function MenuFailureNotice(): React.ReactElement | null {
  const intl = useIntl();
  const menu = useSyncExternalStore(subscribeMenu, getMenuSnapshot, getMenuSnapshot);
  if (menu.status !== 'error') {
    return null;
  }
  return (
    <div style={{ padding: 16 }}>
      <p>{intl.formatMessage({ id: 'pages.menu.loadFailed', defaultMessage: '메뉴를 불러오지 못했습니다.' })}</p>
      <Button onClick={() => void retryMenus()}>
        {intl.formatMessage({ id: 'pages.menu.retry', defaultMessage: '다시 시도' })}
      </Button>
    </div>
  );
}

export {
  getMenuSnapshot,
  noteAuthenticated,
  noteLocaleChanged,
  noteLoggedOut,
  registerUnauthorized,
  retryMenus,
  subscribeMenu,
};
