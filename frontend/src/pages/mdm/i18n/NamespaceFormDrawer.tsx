import { useIntl } from '@umijs/max';
import { Alert, Button, Drawer, Form, Input } from 'antd';
import React, { useState } from 'react';
import { ApiError, toFormFields } from '@/core/request/errors';
import type { NamespaceBody, NamespaceRecord } from '@/services/mdm/i18nRecord';
import { blankToNull, i18nErrorText } from './rules';

type Values = {
  namespaceCode: string;
  namespaceName: string;
  solutionCode?: string;
  description?: string;
};

type Props = {
  open: boolean;
  saving: boolean;
  namespace?: NamespaceRecord;
  onClose: () => void;
  onSubmit: (body: NamespaceBody) => Promise<void>;
};

const NamespaceFormDrawer: React.FC<Props> = ({ open, saving, namespace, onClose, onSubmit }) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });
  const [form] = Form.useForm<Values>();
  const [message, setMessage] = useState<string>();
  const editing = Boolean(namespace);

  const submit = async (values: Values) => {
    setMessage(undefined);
    try {
      await onSubmit({
        namespaceCode: namespace?.namespaceCode ?? values.namespaceCode.trim(),
        namespaceName: values.namespaceName.trim(),
        solutionCode: blankToNull(values.solutionCode),
        description: blankToNull(values.description),
      });
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      if (api.fieldErrors?.length) {
        form.setFields(toFormFields(api.fieldErrors) as Parameters<typeof form.setFields>[0]);
      }
      setMessage(i18nErrorText(api));
    }
  };

  return (
    <Drawer
      open={open}
      size={480}
      destroyOnHidden
      title={text(
        editing ? 'pages.i18n.editNamespace' : 'pages.i18n.createNamespace',
        editing ? '네임스페이스 수정' : '네임스페이스 등록',
      )}
      onClose={onClose}
      extra={
        <Button type="primary" loading={saving} onClick={() => form.submit()}>
          {text('pages.i18n.save', '저장')}
        </Button>
      }
    >
      {message ? <Alert type="error" showIcon title={message} /> : null}
      <Form<Values>
        form={form}
        layout="vertical"
        disabled={saving}
        initialValues={{
          namespaceCode: namespace?.namespaceCode,
          namespaceName: namespace?.namespaceName,
          solutionCode: namespace?.solutionCode ?? undefined,
          description: namespace?.description ?? undefined,
        }}
        onFinish={(values) => {
          submit(values).catch(() => undefined);
        }}
      >
        <Form.Item
          name="namespaceCode"
          label={text('pages.i18n.namespaceCode', '네임스페이스 코드')}
          rules={[
            { required: true, message: text('pages.i18n.required', '필수 값입니다.') },
            {
              pattern: /^[A-Za-z][A-Za-z0-9_-]*$/,
              message: text(
                'pages.i18n.namespacePattern',
                '영문으로 시작하고 영문, 숫자, 밑줄, 하이픈만 쓸 수 있습니다.',
              ),
            },
          ]}
        >
          <Input maxLength={100} disabled={editing} />
        </Form.Item>
        <Form.Item
          name="namespaceName"
          label={text('pages.i18n.name', '이름')}
          rules={[{ required: true, message: text('pages.i18n.required', '필수 값입니다.') }]}
        >
          <Input maxLength={200} />
        </Form.Item>
        <Form.Item name="solutionCode" label={text('pages.i18n.solution', '솔루션 코드')}>
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item name="description" label={text('pages.i18n.description', '설명')}>
          <Input.TextArea maxLength={1000} rows={3} />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default NamespaceFormDrawer;
