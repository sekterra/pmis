import { PageContainer } from '@ant-design/pro-components';
import { useIntl } from '@umijs/max';
import React from 'react';

const Welcome: React.FC = () => {
  const intl = useIntl();
  return <PageContainer title={intl.formatMessage({ id: 'menu.welcome' })} />;
};

export default Welcome;
