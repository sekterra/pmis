import { PageContainer } from '@ant-design/pro-components';
import { useIntl, useParams } from '@umijs/max';
import {
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Space,
  Spin,
  Steps,
  Table,
  Tabs,
  Tag,
} from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { toApiError } from '@/core/request/errors';
import type { DomainView, TenantDetail } from '@/services/platform/data';
import {
  addDomain,
  deleteDomain,
  getTenant,
  reissueAdminPassword,
  resumeTenant,
  retryProvisioning,
  suspendTenant,
} from '@/services/platform/tenants';
import { HOSTNAME, secretView, stepItems } from './rules';
import TemporaryPasswordModal from './TemporaryPasswordModal';
import TenantActionBar from './TenantActionBar';

type Secret = { loginId?: string; password?: string };

const TenantDetailPage: React.FC = () => {
  const intl = useIntl();
  const params = useParams<{ tenantCode: string }>();
  const tenantCode = params.tenantCode ?? '';
  const [detail, setDetail] = useState<TenantDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState<string>();
  const [reasonOpen, setReasonOpen] = useState<'SUSPENDED' | 'ACTIVE' | null>(
    null,
  );
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string>();
  const [confirmText, setConfirmText] = useState<string>();
  const [confirmAction, setConfirmAction] = useState<
    'retry' | 'reissue' | null
  >(null);
  const [domainName, setDomainName] = useState('');
  const [secret, setSecret] = useState<Secret | null>(null);
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setDetail(await getTenant(tenantCode));
    } catch (error) {
      setNotice(toApiError(error).message);
    } finally {
      setLoading(false);
    }
  }, [tenantCode]);

  useEffect(() => {
    void load();
  }, [load]);

  const showError = (error: unknown) => {
    setNotice(toApiError(error).message);
  };

  const submitReason = async () => {
    if (!reason.trim()) {
      setReasonError(
        text('pages.platform.reasonRequired', '변경 사유를 입력하세요.'),
      );
      return;
    }
    try {
      if (reasonOpen === 'SUSPENDED') {
        await suspendTenant(tenantCode, reason.trim());
      } else if (reasonOpen === 'ACTIVE') {
        await resumeTenant(tenantCode, reason.trim());
      }
      setReasonOpen(null);
      setReason('');
      await load();
    } catch (error) {
      showError(error);
    }
  };

  const runConfirmed = async () => {
    try {
      if (confirmAction === 'retry') {
        const created = await retryProvisioning(tenantCode);
        setSecret(
          secretView(created.adminLoginId, created.temporaryPassword),
        );
      }
      if (confirmAction === 'reissue') {
        const reissued = await reissueAdminPassword(tenantCode);
        setSecret(
          secretView(reissued.adminLoginId, reissued.temporaryPassword),
        );
      }
      setConfirmAction(null);
      await load();
    } catch (error) {
      showError(error);
    }
  };

  const submitDomain = async () => {
    const domain = domainName.trim();
    if (!domain || domain !== domain.toLowerCase() || !HOSTNAME.test(domain)) {
      setNotice(
        text(
          'pages.platform.domainInvalid',
          '도메인은 소문자 호스트명이어야 합니다.',
        ),
      );
      return;
    }
    try {
      await addDomain(tenantCode, domain);
      setDomainName('');
      await load();
    } catch (error) {
      showError(error);
    }
  };

  const removeDomain = async (name: string) => {
    try {
      await deleteDomain(tenantCode, name);
      await load();
    } catch (error) {
      showError(error);
    }
  };

  if (loading && !detail) {
    return <Spin />;
  }

  return (
    <PageContainer
      title={detail?.tenantName}
      subTitle={detail?.tenantCode}
      tags={detail ? <Tag>{detail.tenantStatusCode}</Tag> : undefined}
      extra={
        detail ? (
          <TenantActionBar
            status={detail.tenantStatusCode}
            labels={{
              suspend: text('pages.platform.suspend', '중지'),
              resume: text('pages.platform.resume', '재개'),
              retry: text('pages.platform.retry', '프로비저닝 재시도'),
              reissue: text('pages.platform.reissue', '임시 비밀번호 재발급'),
            }}
            onSuspend={() => setReasonOpen('SUSPENDED')}
            onResume={() => setReasonOpen('ACTIVE')}
            onRetry={() => {
              setConfirmText(
                text(
                  'pages.platform.retryConfirm',
                  '프로비저닝을 다시 실행할까요?',
                ),
              );
              setConfirmAction('retry');
            }}
            onReissue={() => {
              setConfirmText(
                text(
                  'pages.platform.reissueConfirm',
                  '임시 비밀번호를 다시 발급할까요?',
                ),
              );
              setConfirmAction('reissue');
            }}
          />
        ) : null
      }
    >
      {notice ? <div role="alert">{notice}</div> : null}
      <Tabs
        items={[
          {
            key: 'basic',
            label: text('pages.platform.basic', '기본 정보'),
            children: (
              <>
                <Descriptions column={1}>
                  <Descriptions.Item
                    label={text('pages.platform.name', '이름')}
                  >
                    {detail?.tenantName}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={text('pages.platform.language', '기본 언어')}
                  >
                    {detail?.defaultLanguageCode}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={text('pages.platform.timezone', '시간대')}
                  >
                    {detail?.timezoneId}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={text('pages.platform.contractStart', '계약 시작')}
                  >
                    {detail?.contractStartDate} ~ {detail?.contractEndDate}
                  </Descriptions.Item>
                  <Descriptions.Item
                    label={text(
                      'pages.platform.migration',
                      '마지막 마이그레이션',
                    )}
                  >
                    {detail?.lastMigration
                      ? `${detail.lastMigration.targetVersion} ${detail.lastMigration.executionStatusCode} ${detail.lastMigration.startedDatetime}`
                      : '-'}
                  </Descriptions.Item>
                </Descriptions>
                <Steps items={stepItems(detail?.provisioning ?? null)} />
              </>
            ),
          },
          {
            key: 'domains',
            label: text('pages.platform.domainTab', '도메인'),
            children: (
              <>
                <Space>
                  <Input
                    value={domainName}
                    onChange={(event) => setDomainName(event.target.value)}
                  />
                  <Button type="primary" onClick={() => void submitDomain()}>
                    {text('pages.platform.addDomain', '도메인 추가')}
                  </Button>
                </Space>
                <Table<DomainView>
                  rowKey="domainName"
                  pagination={false}
                  dataSource={detail?.domains ?? []}
                  columns={[
                    {
                      title: text('pages.platform.domains', '도메인'),
                      dataIndex: 'domainName',
                    },
                    {
                      title: text('pages.platform.primary', '기본'),
                      dataIndex: 'primaryYn',
                      render: (value) =>
                        value === 'Y'
                          ? text('pages.platform.primary', '기본')
                          : '',
                    },
                    {
                      title: text('pages.platform.removeDomain', '삭제'),
                      render: (_, record) => (
                        <Button
                          onClick={() => void removeDomain(record.domainName)}
                        >
                          {text('pages.platform.removeDomain', '삭제')}
                        </Button>
                      ),
                    },
                  ]}
                />
              </>
            ),
          },
        ]}
      />
      <Modal
        open={reasonOpen !== null}
        title={text('pages.platform.reason', '변경 사유')}
        onCancel={() => setReasonOpen(null)}
        onOk={() => void submitReason()}
        okText={text('pages.platform.confirm', '확인')}
        cancelText={text('pages.platform.cancel', '취소')}
      >
        <Form layout="vertical">
          <Form.Item
            label={text('pages.platform.reason', '변경 사유')}
            validateStatus={reasonError ? 'error' : undefined}
            help={reasonError}
          >
            <Input
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        open={confirmAction !== null}
        title={confirmText}
        onCancel={() => setConfirmAction(null)}
        onOk={() => void runConfirmed()}
        okText={text('pages.platform.confirm', '확인')}
        cancelText={text('pages.platform.cancel', '취소')}
      />
      <TemporaryPasswordModal
        open={secret !== null}
        loginId={secret?.loginId}
        password={secret?.password}
        title={text('pages.platform.secretTitle', '임시 비밀번호')}
        hint={text(
          'pages.platform.secretHint',
          '지금 복사하지 않으면 다시 볼 수 없습니다.',
        )}
        noPasswordHint={text(
          'pages.platform.noPasswordHint',
          '이미 관리자 계정이 있어 새 임시 비밀번호를 만들지 않았습니다.',
        )}
        loginLabel={text('pages.platform.loginId', '관리자 로그인 아이디')}
        passwordLabel={text('pages.platform.password', '임시 비밀번호')}
        copyLabel={text('pages.platform.copy', '복사')}
        copyLoginLabel={text('pages.platform.copyLogin', '아이디 복사')}
        closeLabel={text('pages.platform.close', '닫기')}
        onClose={() => setSecret(null)}
      />
    </PageContainer>
  );
};

export default TenantDetailPage;
