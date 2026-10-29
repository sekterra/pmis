import {
  PageContainer,
  ProForm,
  ProFormDatePicker,
  ProFormSelect,
  ProFormText,
} from '@ant-design/pro-components';
import { history, useIntl } from '@umijs/max';
import { Button, Card, Form, Input, Typography } from 'antd';
import type { FormInstance } from 'antd/es/form';
import dayjs from 'dayjs';
import React, { useMemo, useRef, useState } from 'react';
import { toApiError, toFormFields } from '@/core/request/errors';
import { createTenant } from '@/services/platform/tenants';
import {
  buildCreatePayload,
  type CreateFormValues,
  secretView,
  SUPPORT_LANGUAGES,
  timezoneOptions,
  validateCreate,
} from './rules';
import TemporaryPasswordModal from './TemporaryPasswordModal';

const formWidth = { maxWidth: 720, margin: '0 auto' } as const;

/** vitest가 DatePicker 패널 대신 필드를 직접 채울 때 사용 */
export const tenantCreateFormRef = {
  current: null as FormInstance<CreateFormValues> | null,
};

const NewTenantPage: React.FC = () => {
  const intl = useIntl();
  const [form] = Form.useForm<CreateFormValues>();
  tenantCreateFormRef.current = form;
  const submittingRef = useRef(false);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string>();
  const [secret, setSecret] = useState<{
    tenantCode: string;
    loginId: string;
    password?: string;
  } | null>(null);
  const zones = useMemo(() => timezoneOptions(), []);
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });

  const validationMessage = (code: string) =>
    text(
      code === 'code'
        ? 'pages.platform.codeInvalid'
        : code === 'email'
          ? 'pages.platform.emailInvalid'
          : code === 'domain'
            ? 'pages.platform.domainInvalid'
            : code === 'range'
              ? 'pages.platform.contractRange'
              : 'pages.platform.required',
      '필수 항목입니다.',
    );

  const onFinish = async (values: CreateFormValues) => {
    if (submittingRef.current) {
      return;
    }
    const normalized: CreateFormValues = {
      ...values,
      domains: (values.domains ?? []).map((item) =>
        typeof item === 'string' ? item : String(item ?? ''),
      ),
    };
    const errors = validateCreate(normalized);
    if (Object.keys(errors).length > 0) {
      form.setFields(
        Object.entries(errors).map(([name, code]) => ({
          name: name === 'domains' ? 'domains' : name,
          errors: [validationMessage(code)],
        })) as Parameters<FormInstance['setFields']>[0],
      );
      return;
    }
    submittingRef.current = true;
    setSubmitting(true);
    setNotice(undefined);
    try {
      const payload = buildCreatePayload(normalized);
      const created = await createTenant(payload);
      const view = secretView(created.adminLoginId, created.temporaryPassword);
      if (view.loginId || view.password) {
        setSecret({ tenantCode: created.tenantCode, ...view });
      } else {
        history.push(`/tenants/${created.tenantCode}`);
      }
    } catch (error) {
      const apiError = toApiError(error);
      if (apiError.fieldErrors?.length) {
        form.setFields(
          toFormFields(apiError.fieldErrors) as Parameters<
            FormInstance['setFields']
          >[0],
        );
      }
      setNotice(apiError.message);
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  const closeSecret = () => {
    const tenantCode = secret?.tenantCode;
    setSecret(null);
    if (tenantCode) {
      history.push(`/tenants/${tenantCode}`);
    }
  };

  return (
    <PageContainer title={text('pages.platform.create', '테넌트 등록')}>
      <Card variant="borderless">
        <div style={formWidth}>
          {notice ? (
            <Typography.Text type="danger" role="alert">
              {notice}
            </Typography.Text>
          ) : null}
          <ProForm<CreateFormValues>
            form={form}
            layout="vertical"
            requiredMark
            disabled={submitting}
            initialValues={{
              defaultLanguageCode: 'ko-KR',
              timezoneId: 'Asia/Seoul',
              domains: [''],
            }}
            submitter={{
              searchConfig: {
                submitText: text('pages.platform.submit', '등록'),
              },
              submitButtonProps: {
                loading: submitting,
              },
              render: () => [
                <Button
                  key="cancel"
                  onClick={() => history.push('/tenants')}
                  disabled={submitting}
                >
                  {text('pages.platform.cancel', '취소')}
                </Button>,
                <Button
                  key="submit"
                  type="primary"
                  htmlType="submit"
                  loading={submitting}
                  disabled={submitting}
                >
                  {text('pages.platform.submit', '등록')}
                </Button>,
              ],
            }}
            onFinish={async (values) => {
              await onFinish(values);
            }}
          >
            <Typography.Title level={5}>
              {text('pages.platform.sectionBasic', '기본 정보')}
            </Typography.Title>
            <ProFormText
              name="tenantCode"
              label={text('pages.platform.code', '코드')}
              width="md"
              rules={[{ required: true }]}
            />
            <ProFormText
              name="tenantName"
              label={text('pages.platform.name', '이름')}
              width="md"
              rules={[{ required: true }]}
            />
            <ProFormSelect
              name="defaultLanguageCode"
              label={text('pages.platform.language', '기본 언어')}
              width="md"
              rules={[{ required: true }]}
              options={SUPPORT_LANGUAGES.map((value) => ({
                value,
                label: value,
              }))}
            />
            <ProFormSelect
              name="timezoneId"
              label={text('pages.platform.timezone', '시간대')}
              width="md"
              showSearch
              options={zones.map((value) => ({ value, label: value }))}
            />

            <Typography.Title level={5}>
              {text('pages.platform.sectionContract', '계약')}
            </Typography.Title>
            <ProFormDatePicker
              name="contractStartDate"
              label={text('pages.platform.contractStart', '계약 시작')}
              width="md"
              rules={[{ required: true }]}
              fieldProps={{ style: { width: '100%' } }}
              getValueProps={(value: string | undefined) => ({
                value: value ? dayjs(value) : undefined,
              })}
              normalize={(value: dayjs.Dayjs | null | undefined) =>
                value ? value.format('YYYY-MM-DD') : undefined
              }
            />
            <ProFormDatePicker
              name="contractEndDate"
              label={text('pages.platform.contractEnd', '계약 종료')}
              width="md"
              fieldProps={{ style: { width: '100%' } }}
              getValueProps={(value: string | undefined) => ({
                value: value ? dayjs(value) : undefined,
              })}
              normalize={(value: dayjs.Dayjs | null | undefined) =>
                value ? value.format('YYYY-MM-DD') : undefined
              }
            />

            <Typography.Title level={5}>
              {text('pages.platform.sectionAdmin', '관리자')}
            </Typography.Title>
            <ProFormText
              name="adminName"
              label={text('pages.platform.adminName', '관리자 이름')}
              width="md"
              rules={[{ required: true }]}
            />
            <ProFormText
              name="adminEmail"
              label={text('pages.platform.adminEmail', '관리자 이메일')}
              width="md"
              rules={[{ required: true }]}
            />

            <Typography.Title level={5}>
              {text('pages.platform.sectionDomains', '도메인')}
            </Typography.Title>
            <Form.List name="domains">
              {(fields, { add, remove }) => (
                <div>
                  {fields.map((field, index) => (
                    <div
                      key={field.key}
                      style={{
                        display: 'flex',
                        gap: 8,
                        marginBottom: 8,
                        maxWidth: 328,
                      }}
                    >
                      <Form.Item
                        {...field}
                        label={
                          index === 0
                            ? text('pages.platform.domains', '도메인')
                            : undefined
                        }
                        rules={[{ required: true }]}
                        style={{ flex: 1, marginBottom: 0 }}
                      >
                        <Input
                          aria-label={text('pages.platform.domains', '도메인')}
                        />
                      </Form.Item>
                      {fields.length > 1 ? (
                        <Button
                          aria-label={text(
                            'pages.platform.removeDomain',
                            '삭제',
                          )}
                          onClick={() => remove(field.name)}
                        >
                          {text('pages.platform.removeDomain', '삭제')}
                        </Button>
                      ) : null}
                    </div>
                  ))}
                  <Button
                    type="dashed"
                    block
                    style={{ maxWidth: 328 }}
                    onClick={() => add('')}
                  >
                    {text('pages.platform.addDomain', '도메인 추가')}
                  </Button>
                </div>
              )}
            </Form.List>
          </ProForm>
        </div>
      </Card>
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
        onClose={closeSecret}
      />
    </PageContainer>
  );
};

export default NewTenantPage;
