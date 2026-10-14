import { PageContainer } from '@ant-design/pro-components';
import { history, useIntl } from '@umijs/max';
import { Button, DatePicker, Form, Input, Select, Space } from 'antd';
import dayjs from 'dayjs';
import React, { useMemo, useState } from 'react';
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

const NewTenantPage: React.FC = () => {
  const intl = useIntl();
  const [form] = Form.useForm<CreateFormValues>();
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

  const onFinish = async (values: CreateFormValues) => {
    const errors = validateCreate(values);
    if (Object.keys(errors).length > 0) {
      form.setFields(
        Object.entries(errors).map(([name, code]) => ({
          name: name === 'domains' ? 'domains' : name,
          errors: [
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
            ),
          ],
        })) as Parameters<typeof form.setFields>[0],
      );
      return;
    }
    setSubmitting(true);
    try {
      const payload = buildCreatePayload(values);
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
            typeof form.setFields
          >[0],
        );
      }
      setNotice(apiError.message);
    } finally {
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
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          defaultLanguageCode: 'ko-KR',
          timezoneId: 'Asia/Seoul',
          domains: [''],
        }}
        onFinish={onFinish}
        disabled={submitting}
      >
        {notice ? <div role="alert">{notice}</div> : null}
        <Form.Item
          name="tenantCode"
          label={text('pages.platform.code', '코드')}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="tenantName"
          label={text('pages.platform.name', '이름')}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="defaultLanguageCode"
          label={text('pages.platform.language', '기본 언어')}
          rules={[{ required: true }]}
        >
          <Select
            options={SUPPORT_LANGUAGES.map((value) => ({
              value,
              label: value,
            }))}
          />
        </Form.Item>
        <Form.Item
          name="timezoneId"
          label={text('pages.platform.timezone', '시간대')}
        >
          <Select
            showSearch
            options={zones.map((value) => ({ value, label: value }))}
          />
        </Form.Item>
        <Form.Item
          name="contractStartDate"
          label={text('pages.platform.contractStart', '계약 시작')}
          rules={[{ required: true }]}
          getValueProps={(value) => ({
            value: value ? dayjs(value) : undefined,
          })}
          getValueFromEvent={(value) =>
            value ? value.format('YYYY-MM-DD') : undefined
          }
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="contractEndDate"
          label={text('pages.platform.contractEnd', '계약 종료')}
          getValueProps={(value) => ({
            value: value ? dayjs(value) : undefined,
          })}
          getValueFromEvent={(value) =>
            value ? value.format('YYYY-MM-DD') : undefined
          }
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="adminName"
          label={text('pages.platform.adminName', '관리자 이름')}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="adminEmail"
          label={text('pages.platform.adminEmail', '관리자 이메일')}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.List name="domains">
          {(fields, { add, remove }) => (
            <div>
              {fields.map((field) => (
                <Space key={field.key} align="baseline">
                  <Form.Item
                    {...field}
                    label={text('pages.platform.domains', '도메인')}
                    rules={[{ required: true }]}
                  >
                    <Input />
                  </Form.Item>
                  {fields.length > 1 ? (
                    <Button onClick={() => remove(field.name)}>
                      {text('pages.platform.removeDomain', '삭제')}
                    </Button>
                  ) : null}
                </Space>
              ))}
              <Button onClick={() => add('')}>
                {text('pages.platform.addDomain', '도메인 추가')}
              </Button>
            </div>
          )}
        </Form.List>
        <Button
          type="primary"
          htmlType="submit"
          loading={submitting}
          style={{ marginTop: 16 }}
        >
          {text('pages.platform.submit', '등록')}
        </Button>
      </Form>
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
