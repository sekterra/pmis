import { history, useModel } from '@umijs/max';
import { Spin } from 'antd';
import React, { useEffect } from 'react';
import { homePath } from '@/core/auth/mode';
import { nextPathAfterRecover } from '@/core/auth/guard';
import { MenuFailureNotice, viewFor } from '@/core/menu/runtime';
import type { MenuSnapshot } from '@/core/menu/types';
import type { Me } from '@/services/mdm/data';

const RouteGuard: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { initialState, loading } = useModel('@@initialState');
  const user = initialState?.currentUser as Me | undefined;
  const menu = (initialState as { menu?: MenuSnapshot } | undefined)?.menu;
  const [pathname, setPathname] = React.useState(history.location.pathname);
  const [search, setSearch] = React.useState(history.location.search);
  const booting = loading || initialState?.loading === true;
  const view = viewFor(pathname, menu, Boolean(user));

  useEffect(() => {
    return history.listen((update) => {
      setPathname(update.location.pathname);
      setSearch(update.location.search);
    });
  }, []);

  useEffect(() => {
    if (booting) {
      return;
    }
    const redirectQuery = new URLSearchParams(search).get('redirect');
    const next = nextPathAfterRecover({
      pathname,
      search,
      user,
      redirectQuery,
      home: homePath(),
    });
    if (next) {
      history.replace(next);
    }
  }, [booting, pathname, search, user]);

  useEffect(() => {
    if (booting || view !== 'forbidden' || pathname === '/exception/403') {
      return;
    }
    history.replace('/exception/403');
  }, [booting, pathname, view]);

  if (booting || view === 'loading') {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Spin />
      </div>
    );
  }

  if (view === 'error') {
    return <MenuFailureNotice />;
  }

  if (view === 'forbidden') {
    return null;
  }

  return children;
};

export default RouteGuard;
