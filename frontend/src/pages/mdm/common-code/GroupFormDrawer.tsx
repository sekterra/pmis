import { useIntl } from '@umijs/max';
import { Alert, Button, Drawer, Form, Input, InputNumber, Select, Tabs } from 'antd';
import React, { useState } from 'react';
import { ApiError, toFormFields } from '@/core/request/errors';
import type { CodeGroup, CodeGroupBody } from '@/services/mdm/code';
import type { SupportLanguage } from '@/services/mdm/data';
import { blankToNull, codeErrorText, languagesMissingName, toTranslations } from './rules';

type Values = {
  codeGroupCode: string;
  solutionCode?: string;
  codeGroupName: string;
  description?: string;
  hierarchy: boolean;
  codeValueLength?: number | null;
  sortOrder?: number | null;
  names?: Record<string, { name?: string; description?: string }>;
};

type Props = {
  open: boolean;
  saving: boolean;
  group?: CodeGroup;
  languages: SupportLanguage[];
  onClose: () => void;
  onSubmit: (body: CodeGroupBody) => Promise<void>;
};

const GroupFormDrawer: React.FC<Props> = ({
  open,
  saving,
  group,
  languages,
  onClose,
  onSubmit,
}) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });
  const [form] = Form.useForm<Values>();
  const [message, setMessage] = useState<string>();

  const submit = async (values: Values) => {
    const rows = languages.map((language) => ({
      supportLanguageCode: language.supportLanguageCode,
      name: values.names?.[language.supportLanguageCode]?.name,
      description: values.names?.[language.supportLanguageCode]?.description,
    }));
    const missing = languagesMissingName(rows);
    if (missing.length > 0) {
      setMessage(text('pages.mdm.nameRequired', '설명을 적은 언어는 이름도 입력하세요.'));
      return;
    }
    setMessage(undefined);
    try {
      await onSubmit({
        codeGroupCode: group?.codeGroupCode ?? values.codeGroupCode.trim(),
        solutionCode: blankToNull(values.solutionCode),
        codeGroupName: values.codeGroupName.trim(),
        description: blankToNull(values.description),
        hierarchy: values.hierarchy,
        codeValueLength: values.codeValueLength ?? null,
        extAttrSchema: group?.extAttrSchema ?? null,
        sortOrder: values.sortOrder ?? null,
        translations: toTranslations(rows),
      });
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      if (api.fieldErrors?.length) {
        form.setFields(
          toFormFields(api.fieldErrors) as Parameters<typeof form.setFields>[0],
        );
      }
      setMessage(codeErrorText(api));
    }
  };

  const names: Values['names'] = {};
  for (const language of languages) {
    const current = group?.translations.find(
      (item) => item.supportLanguageCode === language.supportLanguageCode,
    );
    names[language.supportLanguageCode] = {
      name: current?.name,
      description: current?.description ?? undefined,
    };
  }

  return (
    <Drawer
      title={
        group
          ? text('pages.mdm.editGroup', '그룹 수정')
          : text('pages.mdm.createGroup', '그룹 등록')
      }
      open={open}
      size={480}
      destroyOnHidden
      onClose={onClose}
      extra={
        <Button type="primary" loading={saving} onClick={() => form.submit()}>
          {text('pages.mdm.save', '저장')}
        </Button>
      }
    >
      {message ? <Alert type="error" showIcon title={message} /> : null}
      <Form<Values>
        form={form}
        layout="vertical"
        disabled={saving}
        initialValues={{
          codeGroupCode: group?.codeGroupCode,
          solutionCode: group?.solutionCode ?? undefined,
          codeGroupName: group?.codeGroupName,
          description: group?.description ?? undefined,
          hierarchy: group?.hierarchy ?? false,
          codeValueLength: group?.codeValueLength,
          sortOrder: group?.sortOrder,
          names,
        }}
        onFinish={(values) => {
          submit(values).catch(() => undefined);
        }}
      >
        <Tabs
          items={[
            {
              key: 'base',
              label: text('pages.mdm.basic', '기본 정보'),
              children: (
                <>
                  <Form.Item
                    name="codeGroupCode"
                    label={text('pages.mdm.groupCode', '그룹 코드')}
                    rules={[
                      { required: true, message: text('pages.mdm.required', '필수 값입니다.') },
                      {
                        pattern: /^[A-Z][A-Z0-9_]*$/,
                        message: text(
                          'pages.mdm.groupCodePattern',
                          '대문자로 시작하고 대문자, 숫자, 밑줄만 쓸 수 있습니다.',
                        ),
                      },
                    ]}
                  >
                    <Input maxLength={100} disabled={Boolean(group)} />
                  </Form.Item>
                  <Form.Item
                    name="solutionCode"
                    label={text('pages.mdm.solution', '솔루션 코드')}
                  >
                    <Input maxLength={50} />
                  </Form.Item>
                  <Form.Item
                    name="codeGroupName"
                    label={text('pages.mdm.name', '이름')}
                    rules={[
                      { required: true, message: text('pages.mdm.required', '필수 값입니다.') },
                    ]}
                  >
                    <Input maxLength={200} />
                  </Form.Item>
                  <Form.Item name="description" label={text('pages.mdm.description', '설명')}>
                    <Input.TextArea maxLength={1000} rows={3} />
                  </Form.Item>
                  <Form.Item
                    name="hierarchy"
                    label={text('pages.mdm.hierarchy', '계층형')}
                    rules={[
                      { required: true, message: text('pages.mdm.required', '필수 값입니다.') },
                    ]}
                  >
                    <Select
                      options={[
                        { value: false, label: text('pages.mdm.no', '아니오') },
                        { value: true, label: text('pages.mdm.yes', '예') },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item
                    name="codeValueLength"
                    label={text('pages.mdm.valueLength', '코드 값 길이')}
                  >
                    <InputNumber min={1} precision={0} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="sortOrder" label={text('pages.mdm.sortOrder', '정렬 순서')}>
                    <InputNumber precision={0} style={{ width: '100%' }} />
                  </Form.Item>
                </>
              ),
            },
            {
              key: 'lang',
              label: text('pages.mdm.translations', '언어별 이름'),
              children: languages.map((language) => (
                <div key={language.supportLanguageCode}>
                  <Form.Item
                    name={['names', language.supportLanguageCode, 'name']}
                    label={`${language.languageName} ${text('pages.mdm.name', '이름')}`}
                  >
                    <Input maxLength={200} />
                  </Form.Item>
                  <Form.Item
                    name={['names', language.supportLanguageCode, 'description']}
                    label={`${language.languageName} ${text('pages.mdm.description', '설명')}`}
                  >
                    <Input maxLength={1000} />
                  </Form.Item>
                </div>
              )),
            },
          ]}
        />
      </Form>
    </Drawer>
  );
};

export default GroupFormDrawer;
