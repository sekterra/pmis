import { history } from '@umijs/max';
import React, { useEffect } from 'react';
import { homePath } from '@/core/auth/mode';

const HomeRedirect: React.FC = () => {
  useEffect(() => {
    history.replace(homePath());
  }, []);
  return null;
};

export default HomeRedirect;
