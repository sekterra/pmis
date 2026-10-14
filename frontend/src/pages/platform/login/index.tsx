import { history, useIntl, useModel, useSearchParams } from '@umijs/max';
import { Button, Card, Form, Input } from 'antd';
import React, { useState } from 'react';
import BrandMark from '@/components/BrandMark';
import LanguageSelect from '@/components/LanguageSelect';
import { completeLogin, invalidCredentialsMessage } from '@/core/auth/actions';
import { homePath } from '@/core/auth/mode';
import { toFormFields } from '@/core/request/errors';
import type { Me } from '@/services/mdm/data';
import { login, me } from '@/services/platform/auth';

const PlatformLoginPage: React.FC = () => {
  const intl = useIntl();
  const [form] = Form.useForm<{ loginId: string; password: string }>();
  const [searchParams] = useSearchParams();
  const { setInitialState } = useModel('@@initialState');
  const [submitting, setSubmitting] = useState(false);
  const [errorText, setErrorText] = useState<string>();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });

  const onFinish = async (values: { loginId: string; password: string }) => {
    setSubmitting(true);
    setErrorText(undefined);
    try {
      const result = await completeLogin(
        login,
        me,
        values,
        searchParams.get('redirect'),
        homePath('platform'),
      );
      if (result.user) {
        setInitialState((current) => ({
          ...current,
          currentUser: result.user,
        }));
      } else {
        setInitialState((current) => ({
          ...current,
          currentUser: { passwordChangeRequired: true } as Me,
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
          text('pages.login.failure', '로그인에 실패했습니다.'),
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
          <div style={{ fontSize: 20, fontWeight: 600 }}>
            {text('pages.platform.title', 'PMIS 플랫폼 콘솔')}
          </div>
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
            label={text('pages.login.loginId', '아이디')}
            rules={[{ required: true }]}
          >
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item
            name="password"
            label={text('pages.login.password', '비밀번호')}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {text('pages.login.submit', '로그인')}
          </Button>
        </Form>
      </Card>
      <LanguageSelect />
    </div>
  );
};

export default PlatformLoginPage;
