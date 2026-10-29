import { LockOutlined, LogoutOutlined } from '@ant-design/icons';
import { history, useIntl, useModel } from '@umijs/max';
import type { MenuProps } from 'antd';
import { Spin } from 'antd';
import React from 'react';
import { logoutSession } from '@/core/auth/actions';
import { getMenuSnapshot } from '@/core/menu/runtime';
import { AUTH_CHANNEL } from '@/core/auth/broadcast';
import { logout } from '@/core/auth/authApi';
import type { Me } from '@/services/mdm/data';
import HeaderDropdown from '../HeaderDropdown';

const UserMenu: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const intl = useIntl();
  const { initialState, setInitialState } = useModel('@@initialState');
  const user = initialState?.currentUser as Me | undefined;

  const signOut = async () => {
    const channel =
      typeof BroadcastChannel === 'undefined'
        ? undefined
        : new BroadcastChannel(AUTH_CHANNEL);
    const support = Boolean(user?.support);
    await logoutSession(logout, channel);
    channel?.close();
    setInitialState((current) => ({
      ...current,
      currentUser: undefined,
      menu: getMenuSnapshot(),
    }));
    history.replace(support ? '/support-access/ended' : '/user/login');
  };

  const items: MenuProps['items'] = [
    ...(user?.support
      ? []
      : [
          {
            key: 'password',
            icon: <LockOutlined />,
            label: intl.formatMessage({ id: 'pages.account.password' }),
          },
        ]),
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: intl.formatMessage({ id: 'pages.account.logout' }),
    },
  ];

  if (!user) {
    return <Spin size="small" />;
  }

  return (
    <HeaderDropdown
      menu={{
        selectedKeys: [],
        items,
        onClick: (event) => {
          if (event.key === 'password') {
            history.push('/user/password');
            return;
          }
          if (event.key === 'logout') {
            void signOut();
          }
        },
      }}
    >
      <span>{user.displayName}</span>
      {children}
    </HeaderDropdown>
  );
};

export default UserMenu;
