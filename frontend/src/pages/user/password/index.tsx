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
import { getMenuSnapshot, noteAuthenticated } from '@/core/menu/runtime';
import { toApiError, toFormFields } from '@/core/request/errors';
import { changePassword, logout, me } from '@/services/mdm/auth';
import type { Me } from '@/services/mdm/data';

const PasswordPage: React.FC = () => {
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

  const onFinish = async (values: {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
  }) => {
    if (values.newPassword !== values.confirmPassword) {
      form.setFields([
        {
          name: 'confirmPassword',
          errors: [intl.formatMessage({ id: 'pages.password.mismatch' })],
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
      await noteAuthenticated(profile);
      setInitialState((current) => ({
        ...current,
        currentUser: profile,
        menu: getMenuSnapshot(),
      }));
      history.replace(homePath());
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
          <BrandMark />
        </div>
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="currentPassword"
            label={intl.formatMessage({ id: 'pages.password.current' })}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label={intl.formatMessage({ id: 'pages.password.new' })}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label={intl.formatMessage({ id: 'pages.password.confirm' })}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {intl.formatMessage({ id: 'pages.password.submit' })}
          </Button>
        </Form>
        {notice ? <div role="alert">{notice}</div> : null}
      </Card>
      {voluntary ? (
        <Button
          type="link"
          onClick={() =>
            cancelPasswordChange(homePath(), {
              idx: (window.history.state as { idx?: number } | null)?.idx,
              back: () => history.back(),
              replace: (path) => history.replace(path),
            })
          }
        >
          {intl.formatMessage({ id: 'pages.password.cancel' })}
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
                menu: getMenuSnapshot(),
              })),
            () => history.replace('/user/login'),
          );
        }}
      >
        {intl.formatMessage({ id: 'pages.password.switchAccount' })}
      </Button>
    </div>
  );
};

export default PasswordPage;
