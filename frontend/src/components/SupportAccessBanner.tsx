import { history, useIntl, useModel } from '@umijs/max';
import { App, Button } from 'antd';
import React, { useEffect, useState } from 'react';
import { formatCountdown, remainingSeconds } from '@/core/supportAccess/rules';
import { clearAccessToken } from '@/core/auth/token';
import { noteLoggedOut } from '@/core/menu/runtime';
import { endSupportAccess } from '@/services/mdm/supportAccess';
import type { Me } from '@/services/mdm/data';

const SupportAccessBanner: React.FC = () => {
  const intl = useIntl();
  const { modal } = App.useApp();
  const { initialState, setInitialState } = useModel('@@initialState');
  const user = initialState?.currentUser as Me | undefined;
  const support = user?.support;
  const [seconds, setSeconds] = useState(() =>
    remainingSeconds(support?.expireDatetime),
  );
  const [ending, setEnding] = useState(false);

  useEffect(() => {
    if (!support?.expireDatetime) {
      return undefined;
    }
    const tick = () => {
      const left = remainingSeconds(support.expireDatetime);
      setSeconds(left);
      if (left <= 0) {
        clearAccessToken();
        noteLoggedOut();
        setInitialState((current) => ({ ...current, currentUser: undefined }));
        if (history.location.pathname !== '/support-access/ended') {
          history.replace('/support-access/ended');
        }
      }
    };
    tick();
    const timer = window.setInterval(tick, 1000);
    return () => window.clearInterval(timer);
  }, [support?.expireDatetime, setInitialState]);

  if (!support) {
    return null;
  }

  const urgent = seconds <= 5 * 60;
  const endSession = () => {
    modal.confirm({
      title: intl.formatMessage({
        id: 'pages.supportAccess.endConfirm',
        defaultMessage: '지원 접속을 종료할까요?',
      }),
      onOk: async () => {
        setEnding(true);
        try {
          await endSupportAccess();
        } catch {
          // still clear local session
        } finally {
          clearAccessToken();
          noteLoggedOut();
          setInitialState((current) => ({
            ...current,
            currentUser: undefined,
          }));
          setEnding(false);
          history.replace('/support-access/ended');
        }
      },
    });
  };

  return (
    <div
      role="status"
      style={{
        position: 'sticky',
        top: 0,
        zIndex: 1000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 16,
        padding: '8px 16px',
        background: urgent ? '#fff1f0' : '#fff7e6',
        borderBottom: `1px solid ${urgent ? '#ffa39e' : '#ffd591'}`,
      }}
    >
      <span>
        {intl.formatMessage(
          {
            id: 'pages.supportAccess.banner',
            defaultMessage:
              '지원 접속 중 · {operator} · 남은 시간 {time}',
          },
          {
            operator: support.operatorLoginId,
            time: formatCountdown(seconds),
          },
        )}
      </span>
      <Button danger size="small" loading={ending} onClick={endSession}>
        {intl.formatMessage({
          id: 'pages.supportAccess.end',
          defaultMessage: '지원 접속 종료',
        })}
      </Button>
    </div>
  );
};

export default SupportAccessBanner;
