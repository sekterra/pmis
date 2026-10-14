import { history, useIntl, useModel } from '@umijs/max';
import { Button, Card, Form, Input } from 'antd';
import React, { useState } from 'react';
import BrandMark from '@/components/BrandMark';
import {
  cancelPasswordChange,
  completePasswordChange,
  signOutToLogin,
} from '@/core/auth/actions';
import { homePath } from '@/core/auth/mode';
import { toApiError, toFormFields } from '@/core/request/errors';
import type { Me } from '@/services/mdm/data';
import { changePassword, logout, me } from '@/services/platform/auth';

const PlatformPasswordPage: React.FC = () => {
  const intl = useIntl();
  const [form] = Form.useForm<{
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
  }>();
  const { initialState, setInitialState } = useModel('@@initialState');
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string>();
  const voluntary =
    (initialState?.currentUser as Me | undefined)?.passwordChangeRequired ===
    false;
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });

  const onFinish = async (values: {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
  }) => {
    if (values.newPassword !== values.confirmPassword) {
      form.setFields([
        {
          name: 'confirmPassword',
          errors: [
            text('pages.password.mismatch', '새 비밀번호가 일치하지 않습니다.'),
          ],
        },
      ]);
      return;
    }
    setSubmitting(true);
    setNotice(undefined);
    try {
      const profile = await completePasswordChange(changePassword, me, {
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      setInitialState((current) => ({ ...current, currentUser: profile }));
      history.replace(homePath('platform'));
    } catch (error) {
      const apiError = toApiError(error);
      if (apiError.message) {
        setNotice(apiError.message);
      }
      form.setFields(
        toFormFields(apiError.fieldErrors) as Parameters<
          typeof form.setFields
        >[0],
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
        gap: 8,
        padding: 24,
      }}
    >
      <Card style={{ width: 360 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ fontSize: 20, fontWeight: 600, marginBottom: 8 }}>
            {text('pages.platform.title', 'PMIS 플랫폼 콘솔')}
          </div>
          <BrandMark />
        </div>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="currentPassword"
            label={text('pages.password.current', '현재 비밀번호')}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label={text('pages.password.new', '새 비밀번호')}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label={text('pages.password.confirm', '새 비밀번호 확인')}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {text('pages.password.submit', '변경')}
          </Button>
        </Form>
        {notice ? <div role="alert">{notice}</div> : null}
      </Card>
      {voluntary ? (
        <Button
          type="link"
          onClick={() =>
            cancelPasswordChange(homePath('platform'), {
              idx: (window.history.state as { idx?: number } | null)?.idx,
              back: () => history.back(),
              replace: (path) => history.replace(path),
            })
          }
        >
          {text('pages.password.cancel', '취소')}
        </Button>
      ) : null}
      <Button
        type="link"
        onClick={() => {
          void signOutToLogin(
            logout,
            () =>
              setInitialState((current) => ({
                ...current,
                currentUser: undefined,
              })),
            () => history.replace('/user/login'),
          );
        }}
      >
        {text('pages.password.switchAccount', '다른 계정으로 로그인')}
      </Button>
    </div>
  );
};

export default PlatformPasswordPage;
