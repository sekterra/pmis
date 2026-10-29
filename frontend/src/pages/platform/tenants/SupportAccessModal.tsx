import { useIntl } from '@umijs/max';
import { Alert, Button, Input, Modal, Space, Typography } from 'antd';
import React, { useEffect, useState } from 'react';
import { remainingSeconds } from '@/core/supportAccess/rules';
import { toApiError } from '@/core/request/errors';
import { startSupportAccess } from '@/services/platform/supportAccess';
import type { IssuedSupportAccess } from '@/services/platform/data';

type SupportAccessModalProps = {
  open: boolean;
  tenantCode: string;
  onClose: () => void;
};

const SupportAccessModal: React.FC<SupportAccessModalProps> = ({
  open,
  tenantCode,
  onClose,
}) => {
  const intl = useIntl();
  const [step, setStep] = useState<1 | 2>(1);
  const [reason, setReason] = useState('');
  const [issuing, setIssuing] = useState(false);
  const [error, setError] = useState<string>();
  const [issued, setIssued] = useState<IssuedSupportAccess | null>(null);
  const [left, setLeft] = useState(0);
  const [opened, setOpened] = useState(false);

  useEffect(() => {
    if (!open) {
      setStep(1);
      setReason('');
      setError(undefined);
      setIssued(null);
      setLeft(0);
      setOpened(false);
      setIssuing(false);
    }
  }, [open]);

  useEffect(() => {
    if (!issued?.ticketExpireDatetime) {
      return undefined;
    }
    const tick = () => setLeft(remainingSeconds(issued.ticketExpireDatetime));
    tick();
    const timer = window.setInterval(tick, 1000);
    return () => window.clearInterval(timer);
  }, [issued?.ticketExpireDatetime]);

  const close = () => {
    setIssued(null);
    onClose();
  };

  const issue = async () => {
    setIssuing(true);
    setError(undefined);
    try {
      const result = await startSupportAccess(tenantCode, reason);
      setIssued(result);
      setOpened(false);
      setStep(2);
    } catch (err) {
      setError(toApiError(err).message);
    } finally {
      setIssuing(false);
    }
  };

  return (
    <Modal
      open={open}
      title={intl.formatMessage({
        id: 'pages.platform.tenantAccess',
        defaultMessage: '테넌트 접속',
      })}
      onCancel={close}
      footer={
        step === 1
          ? [
              <Button key="cancel" onClick={close}>
                {intl.formatMessage({
                  id: 'pages.platform.cancel',
                  defaultMessage: '취소',
                })}
              </Button>,
              <Button
                key="prepare"
                type="primary"
                loading={issuing}
                onClick={() => void issue()}
              >
                {intl.formatMessage({
                  id: 'pages.platform.tenantAccessPrepare',
                  defaultMessage: '접속 준비',
                })}
              </Button>,
            ]
          : [
              <Button key="close" onClick={close}>
                {intl.formatMessage({
                  id: 'pages.platform.close',
                  defaultMessage: '닫기',
                })}
              </Button>,
            ]
      }
      destroyOnHidden
    >
      {error ? (
        <Alert type="error" showIcon style={{ marginBottom: 12 }} title={error} />
      ) : null}
      {step === 1 ? (
        <Space orientation="vertical" style={{ width: '100%' }} size="middle">
          <Alert
            type="warning"
            showIcon
            title={intl.formatMessage({
              id: 'pages.platform.tenantAccessNotice',
              defaultMessage:
                '이 테넌트의 관리자 권한 전체로 접속합니다. 모든 변경 요청이 기록되며 테넌트 관리자가 기록을 볼 수 있습니다',
            })}
          />
          <div>
            <Typography.Text>
              {intl.formatMessage({
                id: 'pages.platform.tenantAccessReason',
                defaultMessage: '접속 사유',
              })}
            </Typography.Text>
            <Input.TextArea
              value={reason}
              maxLength={200}
              showCount
              rows={3}
              onChange={(event) => setReason(event.target.value)}
              placeholder={intl.formatMessage({
                id: 'pages.platform.tenantAccessReasonHint',
                defaultMessage:
                  '비워 두면 접속 일시와 담당자 ID가 기록됩니다',
              })}
            />
          </div>
        </Space>
      ) : (
        <Space orientation="vertical" style={{ width: '100%' }} size="middle">
          <Typography.Text>
            {intl.formatMessage(
              {
                id: 'pages.platform.tenantAccessCountdown',
                defaultMessage: '티켓 남은 시간 {seconds}초',
              },
              { seconds: left },
            )}
          </Typography.Text>
          {issued && left > 0 ? (
            <a
              href={issued.accessUrl}
              target="_blank"
              rel="noopener noreferrer"
              onClick={() => setOpened(true)}
            >
              {intl.formatMessage({
                id: 'pages.platform.tenantAccessOpen',
                defaultMessage: '새 탭에서 열기',
              })}
            </a>
          ) : (
            <Button disabled>
              {intl.formatMessage({
                id: 'pages.platform.tenantAccessOpen',
                defaultMessage: '새 탭에서 열기',
              })}
            </Button>
          )}
          {left <= 0 ? (
            <Button
              type="primary"
              loading={issuing}
              onClick={() => void issue()}
            >
              {intl.formatMessage({
                id: 'pages.platform.tenantAccessReissue',
                defaultMessage: '다시 발급',
              })}
            </Button>
          ) : null}
          {opened ? (
            <Typography.Text type="secondary">
              {intl.formatMessage({
                id: 'pages.platform.tenantAccessOpened',
                defaultMessage: '새 탭을 열었습니다. 이 창을 닫아도 됩니다.',
              })}
            </Typography.Text>
          ) : null}
        </Space>
      )}
    </Modal>
  );
};

export default SupportAccessModal;
