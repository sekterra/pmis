import { history, useModel } from '@umijs/max';
import { Spin } from 'antd';
import React, { useEffect } from 'react';
import { nextPathAfterRecover } from '@/core/auth/guard';
import type { Me } from '@/services/mdm/data';

const RouteGuard: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { initialState, loading } = useModel('@@initialState');
  const user = initialState?.currentUser as Me | undefined;
  const [pathname, setPathname] = React.useState(history.location.pathname);
  const [search, setSearch] = React.useState(history.location.search);
  const booting = loading || initialState?.loading === true;

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
    });
    if (next) {
      history.replace(next);
    }
  }, [booting, pathname, search, user]);

  if (booting) {
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

  return children;
};

export default RouteGuard;
