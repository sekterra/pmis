import { history, useIntl } from '@umijs/max';
import { Button, Result, Spin } from 'antd';
import React, { useEffect, useState } from 'react';
import { exchangeSupportAccessOnce } from './exchangeOnce';

type View = 'loading' | 'missing' | 'invalid' | 'done';

const SupportAccessEntryPage: React.FC = () => {
  const intl = useIntl();
  const [view, setView] = useState<View>('loading');

  useEffect(() => {
    let cancelled = false;
    void exchangeSupportAccessOnce().then((result) => {
      if (cancelled) {
        return;
      }
      if (result.view === 'done') {
        setView('done');
        history.replace('/');
        return;
      }
      setView(result.view);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (view === 'loading' || view === 'done') {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Spin
          description={intl.formatMessage({
            id: 'pages.supportAccess.exchanging',
            defaultMessage: '지원 접속을 준비하는 중입니다.',
          })}
        />
      </div>
    );
  }

  if (view === 'missing') {
    return (
      <Result
        status="warning"
        title={intl.formatMessage({
          id: 'pages.supportAccess.invalidUrl',
          defaultMessage: '유효하지 않은 접속 주소입니다',
        })}
        subTitle={intl.formatMessage({
          id: 'pages.supportAccess.closeTab',
          defaultMessage: '이 탭을 닫아도 됩니다',
        })}
      />
    );
  }

  return (
    <Result
      status="error"
      title={intl.formatMessage({
        id: 'pages.supportAccess.ticketInvalid',
        defaultMessage:
          '접속 주소가 만료되었거나 이미 사용되었습니다. 플랫폼 콘솔에서 다시 접속하세요',
      })}
      extra={
        <Button onClick={() => window.close()}>
          {intl.formatMessage({
            id: 'pages.supportAccess.closeTab',
            defaultMessage: '이 탭을 닫아도 됩니다',
          })}
        </Button>
      }
    />
  );
};

export default SupportAccessEntryPage;
