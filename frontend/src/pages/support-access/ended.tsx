import { useIntl } from '@umijs/max';
import { Result } from 'antd';
import React from 'react';

const SupportAccessEndedPage: React.FC = () => {
  const intl = useIntl();
  return (
    <Result
      status="info"
      title={intl.formatMessage({
        id: 'pages.supportAccess.endedTitle',
        defaultMessage: '지원 접속이 종료되었습니다',
      })}
      subTitle={intl.formatMessage({
        id: 'pages.supportAccess.endedHint',
        defaultMessage:
          '이 탭을 닫아도 됩니다. 다시 접속하려면 플랫폼 콘솔에서 테넌트 접속을 발급하세요.',
      })}
    />
  );
};

export default SupportAccessEndedPage;
