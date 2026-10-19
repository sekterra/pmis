import { history, useIntl, useModel, useSearchParams } from '@umijs/max';
import { Button, Card, Form, Input } from 'antd';
import React, { useState } from 'react';
import BrandMark from '@/components/BrandMark';
import LanguageSelect from '@/components/LanguageSelect';
import { completeLogin, invalidCredentialsMessage } from '@/core/auth/actions';
import { login, me } from '@/core/auth/authApi';
import { accessContext, loadAccessContext } from '@/core/auth/context';
import { getMenuSnapshot, noteAuthenticated } from '@/core/menu/runtime';
import { toFormFields } from '@/core/request/errors';
import type { Me } from '@/services/mdm/data';

const LoginPage: React.FC = () => {
  const intl = useIntl();
  const [form] = Form.useForm<{ loginId: string; password: string }>();
  const [searchParams] = useSearchParams();
  const { initialState, setInitialState } = useModel('@@initialState');
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
      await noteAuthenticated(result.user ?? { passwordChangeRequired: true });
      setInitialState((current) => ({
        ...current,
        currentUser: result.user ?? ({ passwordChangeRequired: true } as Me),
        menu: getMenuSnapshot(),
      }));
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

  const contextStatus = (initialState as { contextStatus?: string } | undefined)?.contextStatus;
  const context = accessContext();
  if (contextStatus === 'offline' || contextStatus === 'unknown') {
    const message =
      contextStatus === 'offline' ? '서버에 연결할 수 없습니다' : '알 수 없는 사이트입니다';
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Card style={{ width: 360 }}>
          <p>{message}</p>
          {contextStatus === 'offline' ? (
            <Button
              onClick={() => {
                void loadAccessContext().then(() => window.location.reload());
              }}
            >
              다시 시도
            </Button>
          ) : null}
        </Card>
      </div>
    );
  }

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
          {context.kind === 'PLATFORM' ? (
            <h1>
              {intl.formatMessage({
                id: 'pages.login.platformTitle',
                defaultMessage: '플랫폼 콘솔',
              })}
            </h1>
          ) : context.tenantName ? (
            <h1>{context.tenantName}</h1>
          ) : (
            <BrandMark />
          )}
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
