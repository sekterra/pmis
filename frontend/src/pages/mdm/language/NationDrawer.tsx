import { useIntl } from '@umijs/max';
import { Alert, Button, Drawer, Form, Input, Select } from 'antd';
import React, { useEffect, useState } from 'react';
import { ApiError, toFormFields } from '@/core/request/errors';
import type { LanguageRecord, NationBody, NationRecord } from '@/services/mdm/languageRecord';
import { isNationCode, languageErrorText, normalizeNationCode, toNationBody } from './rules';

type Props = {
  open: boolean;
  saving: boolean;
  nation?: NationRecord;
  languages: LanguageRecord[];
  onClose: () => void;
  onSubmit: (body: NationBody) => Promise<void>;
};

const NationDrawer: React.FC<Props> = ({ open, saving, nation, languages, onClose, onSubmit }) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) => intl.formatMessage({ id, defaultMessage });
  const [form] = Form.useForm();
  const [message, setMessage] = useState<string>();
  const editing = Boolean(nation);
  const activeLanguages = languages.filter((language) => language.useYn === 'Y');

  useEffect(() => {
    if (!open) {
      return;
    }
    form.setFieldsValue({
      nationCode: nation?.nationCode,
      isoAlpha3Code: nation?.isoAlpha3Code ?? undefined,
      isoNumericCode: nation?.isoNumericCode ?? undefined,
      nationName: nation?.nationName,
      defaultLanguageCode: nation?.defaultLanguageCode,
      phoneCode: nation?.phoneCode ?? undefined,
      currencyCode: nation?.currencyCode ?? undefined,
      timezoneId: nation?.timezoneId ?? undefined,
      sortOrder: nation?.sortOrder ?? undefined,
    });
    setMessage(undefined);
  }, [open, nation, form]);

  return (
    <Drawer
      title={text(editing ? 'pages.language.editNation' : 'pages.language.createNation', editing ? '국가 수정' : '국가 등록')}
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
            await onSubmit(toNationBody(values, nation?.nationCode));
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
          name="nationCode"
          label={text('pages.language.nationCode', '국가 코드')}
          normalize={(value) => normalizeNationCode(value)}
          rules={[
            { required: true, message: text('pages.language.nationCodeRequired', '국가 코드를 입력하세요.') },
            {
              validator: async (_rule, value) => {
                if (!value || isNationCode(value)) {
                  return;
                }
                throw new Error(text('pages.language.nationCodePattern', '국가 코드는 대문자 2자입니다.'));
              },
            },
          ]}
        >
          <Input maxLength={2} disabled={editing} />
        </Form.Item>
        <Form.Item
          name="nationName"
          label={text('pages.language.nationName', '국가명')}
          rules={[{ required: true, message: text('pages.language.nationNameRequired', '국가명을 입력하세요.') }]}
        >
          <Input maxLength={200} />
        </Form.Item>
        <Form.Item name="isoAlpha3Code" label={text('pages.language.isoAlpha3', 'ISO alpha-3')}>
          <Input maxLength={3} />
        </Form.Item>
        <Form.Item name="isoNumericCode" label={text('pages.language.isoNumeric', 'ISO 숫자 코드')}>
          <Input maxLength={3} />
        </Form.Item>
        <Form.Item
          name="defaultLanguageCode"
          label={text('pages.language.defaultLanguage', '기본 언어')}
          rules={[{ required: true, message: text('pages.language.defaultLanguageRequired', '기본 언어를 선택하세요.') }]}
        >
          <Select
            options={activeLanguages.map((language) => ({
              value: language.supportLanguageCode,
              label: language.languageName,
            }))}
          />
        </Form.Item>
        <Form.Item name="phoneCode" label={text('pages.language.phoneCode', '전화 코드')}>
          <Input maxLength={10} />
        </Form.Item>
        <Form.Item name="currencyCode" label={text('pages.language.currency', '통화 코드')}>
          <Input maxLength={3} />
        </Form.Item>
        <Form.Item name="timezoneId" label={text('pages.language.timezone', '시간대')}>
          <Input maxLength={50} />
        </Form.Item>
        <Form.Item name="sortOrder" label={text('pages.language.sort', '정렬')}>
          <Input />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default NationDrawer;
