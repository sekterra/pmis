import { useIntl } from '@umijs/max';
import { Alert, Button, Drawer, Form, Input, Select } from 'antd';
import React, { useEffect, useState } from 'react';
import { ApiError, toFormFields } from '@/core/request/errors';
import type { LanguageBody, LanguageRecord } from '@/services/mdm/languageRecord';
import { fallbackChoices, isLanguageCode, languageErrorText, toLanguageBody } from './rules';

type Props = {
  open: boolean;
  saving: boolean;
  language?: LanguageRecord;
  languages: LanguageRecord[];
  onClose: () => void;
  onSubmit: (body: LanguageBody) => Promise<void>;
};

const LanguageDrawer: React.FC<Props> = ({ open, saving, language, languages, onClose, onSubmit }) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) => intl.formatMessage({ id, defaultMessage });
  const [form] = Form.useForm();
  const [message, setMessage] = useState<string>();
  const editing = Boolean(language);

  useEffect(() => {
    if (!open) {
      return;
    }
    form.setFieldsValue({
      supportLanguageCode: language?.supportLanguageCode,
      languageName: language?.languageName,
      nativeName: language?.nativeName ?? undefined,
      isoLanguageCode: language?.isoLanguageCode ?? undefined,
      fallbackLanguageCode: language?.fallbackLanguageCode ?? undefined,
      textDirectionCode: language?.textDirectionCode ?? 'LTR',
      dateFormat: language?.dateFormat ?? undefined,
      datetimeFormat: language?.datetimeFormat ?? undefined,
      numberFormat: language?.numberFormat ?? undefined,
      defaultLanguage: language?.defaultLanguage ?? false,
      sortOrder: language?.sortOrder ?? undefined,
    });
    setMessage(undefined);
  }, [open, language, form]);

  return (
    <Drawer
      title={text(editing ? 'pages.language.edit' : 'pages.language.create', editing ? '언어 수정' : '언어 등록')}
      size={480}
      open={open}
      destroyOnHidden
      onClose={onClose}
      extra={
        <Button type="primary" loading={saving} onClick={() => form.submit()}>
          {text('pages.language.save', '저장')}
        </Button>
      }
    >
      {message ? <Alert title={message} type="error" showIcon style={{ marginBottom: 16 }} /> : null}
      <Form
        form={form}
        layout="vertical"
        onFinish={async (values) => {
          setMessage(undefined);
          try {
            await onSubmit(toLanguageBody(values, language?.supportLanguageCode));
          } catch (error) {
            const api = error instanceof ApiError ? error : new ApiError('');
            if (api.fieldErrors && api.fieldErrors.length > 0) {
              form.setFields(toFormFields(api.fieldErrors));
            }
            setMessage(languageErrorText(api));
          }
        }}
      >
        <Form.Item
          name="supportLanguageCode"
          label={text('pages.language.code', '언어 코드')}
          rules={[
            { required: true, message: text('pages.language.codeRequired', '언어 코드를 입력하세요.') },
            {
              validator: async (_rule, value) => {
                if (!value || isLanguageCode(value)) {
                  return;
                }
                throw new Error(text('pages.language.codePattern', 'BCP 47 언어 코드여야 합니다.'));
              },
            },
          ]}
        >
          <Input maxLength={20} disabled={editing} />
        </Form.Item>
        <Form.Item
          name="languageName"
          label={text('pages.language.name', '언어명')}
          rules={[{ required: true, message: text('pages.language.nameRequired', '언어명을 입력하세요.') }]}
        >
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="nativeName" label={text('pages.language.nativeName', '현지 표기')}>
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item name="isoLanguageCode" label={text('pages.language.iso', 'ISO 언어 코드')}>
          <Input maxLength={10} />
        </Form.Item>
        <Form.Item name="fallbackLanguageCode" label={text('pages.language.fallback', '대체 언어')}>
          <Select
            allowClear
            options={fallbackChoices(languages, language?.supportLanguageCode).map((item) => ({
              value: item.supportLanguageCode,
              label: item.languageName,
            }))}
          />
        </Form.Item>
        <Form.Item
          name="textDirectionCode"
          label={text('pages.language.direction', '방향')}
          rules={[{ required: true, message: text('pages.language.directionRequired', '방향을 선택하세요.') }]}
        >
          <Select
            options={[
              { value: 'LTR', label: 'LTR' },
              { value: 'RTL', label: 'RTL' },
            ]}
          />
        </Form.Item>
        <Form.Item name="dateFormat" label={text('pages.language.dateFormat', '날짜 형식')}>
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item name="datetimeFormat" label={text('pages.language.datetimeFormat', '일시 형식')}>
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item name="numberFormat" label={text('pages.language.numberFormat', '숫자 형식')}>
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item
          name="defaultLanguage"
          label={text('pages.language.default', '기본 언어')}
          rules={[{ required: true, message: text('pages.language.defaultRequired', '기본 언어 여부를 선택하세요.') }]}
        >
          <Select
            options={[
              { value: true, label: text('pages.language.yes', '예') },
              { value: false, label: text('pages.language.no', '아니오') },
            ]}
          />
        </Form.Item>
        <Form.Item name="sortOrder" label={text('pages.language.sort', '정렬')}>
          <Input />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default LanguageDrawer;
