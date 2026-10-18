import { history, useIntl, useModel, useSearchParams } from '@umijs/max';
import { Button, Card, Form, Input } from 'antd';
import React, { useState } from 'react';
import BrandMark from '@/components/BrandMark';
import LanguageSelect from '@/components/LanguageSelect';
import { completeLogin, invalidCredentialsMessage } from '@/core/auth/actions';
import { toFormFields } from '@/core/request/errors';
import { login, me } from '@/services/mdm/auth';
import type { Me } from '@/services/mdm/data';

const LoginPage: React.FC = () => {
  const intl = useIntl();
  const [form] = Form.useForm<{ loginId: string; password: string }>();
  const [searchParams] = useSearchParams();
  const { setInitialState } = useModel('@@initialState');
  const [submitting, setSubmitting] = useState(false);
  const [errorText, setErrorText] = useState<string>();

  const onFinish = async (values: { loginId: string; password: string }) => {
    setSubmitting(true);
    setErrorText(undefined);
    try {
      const result = await completeLogin(
        login,
        me,
        values,
        searchParams.get('redirect'),
      );
      if (result.user) {
        setInitialState((current) => ({
          ...current,
          currentUser: result.user,
        }));
      } else {
        setInitialState((current) => ({
          ...current,
          currentUser: {
            passwordChangeRequired: true,
          } as Me,
        }));
      }
      history.replace(result.path);
    } catch (error) {
      const apiError = error as {
        fieldErrors?: { field: string; message: string }[];
      };
      if (apiError.fieldErrors?.length) {
        form.setFields(
          toFormFields(apiError.fieldErrors) as Parameters<
            typeof form.setFields
          >[0],
        );
      }
      setErrorText(
        invalidCredentialsMessage(
          error,
          intl.formatMessage({ id: 'pages.login.failure' }),
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 16,
        padding: 24,
      }}
    >
      <Card style={{ width: 360 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <BrandMark />
        </div>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          {errorText ? (
            <div role="alert" style={{ marginBottom: 12 }}>
              {errorText}
            </div>
          ) : null}
          <Form.Item
            name="loginId"
            label={intl.formatMessage({ id: 'pages.login.loginId' })}
            rules={[{ required: true }]}
          >
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item
            name="password"
            label={intl.formatMessage({ id: 'pages.login.password' })}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {intl.formatMessage({ id: 'pages.login.submit' })}
          </Button>
        </Form>
      </Card>
      <LanguageSelect />
    </div>
  );
};

export default LoginPage;
