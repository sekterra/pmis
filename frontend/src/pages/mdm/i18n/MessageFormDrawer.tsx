import { useIntl } from '@umijs/max';
import { Alert, Button, Drawer, Form, Input, Select, Tabs } from 'antd';
import React, { useState } from 'react';
import { ApiError, toFormFields } from '@/core/request/errors';
import type { SupportLanguage } from '@/services/mdm/data';
import type { MessageBody, MessageRecord } from '@/services/mdm/i18nRecord';
import { blankToNull, i18nErrorText, toMessageTranslations } from './rules';
import styles from './index.module.css';

type Option = { value: string; label: string };

type Values = {
  messageKey: string;
  messageTypeCode: string;
  defaultMessage?: string;
  paramDescription?: string;
  texts?: Record<
    string,
    {
      messageText?: string;
      translationStatusCode?: string;
      machineTranslated?: boolean;
    }
  >;
};

type Props = {
  open: boolean;
  saving: boolean;
  namespaceCode: string;
  message?: MessageRecord;
  languages: SupportLanguage[];
  messageTypes: Option[];
  translationStatuses: Option[];
  onClose: () => void;
  onSubmit: (body: MessageBody) => Promise<void>;
};

const MessageFormDrawer: React.FC<Props> = ({
  open,
  saving,
  namespaceCode,
  message,
  languages,
  messageTypes,
  translationStatuses,
  onClose,
  onSubmit,
}) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });
  const [form] = Form.useForm<Values>();
  const [notice, setNotice] = useState<string>();
  const editing = Boolean(message);

  const texts: Values['texts'] = {};
  for (const language of languages) {
    const current = message?.translations.find(
      (item) => item.supportLanguageCode === language.supportLanguageCode,
    );
    texts[language.supportLanguageCode] = {
      messageText: current?.messageText,
      translationStatusCode: current?.translationStatusCode ?? undefined,
      machineTranslated: current?.machineTranslated ?? undefined,
    };
  }

  const submit = async (values: Values) => {
    setNotice(undefined);
    try {
      await onSubmit({
        namespaceCode,
        messageKey: message?.messageKey ?? values.messageKey.trim(),
        messageTypeCode: values.messageTypeCode.trim(),
        defaultMessage: blankToNull(values.defaultMessage),
        paramDescription: blankToNull(values.paramDescription),
        translations: toMessageTranslations(
          languages.map((language) => ({
            supportLanguageCode: language.supportLanguageCode,
            messageText: values.texts?.[language.supportLanguageCode]?.messageText,
            translationStatusCode: values.texts?.[language.supportLanguageCode]?.translationStatusCode,
            machineTranslated: values.texts?.[language.supportLanguageCode]?.machineTranslated,
          })),
        ),
      });
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      if (api.fieldErrors?.length) {
        form.setFields(toFormFields(api.fieldErrors) as Parameters<typeof form.setFields>[0]);
      }
      setNotice(i18nErrorText(api));
    }
  };

  return (
    <Drawer
      open={open}
      size={520}
      destroyOnHidden
      title={text(editing ? 'pages.i18n.editMessage' : 'pages.i18n.createMessage', editing ? '메시지 수정' : '메시지 등록')}
      onClose={onClose}
      extra={
        <Button type="primary" loading={saving} onClick={() => form.submit()}>
          {text('pages.i18n.save', '저장')}
        </Button>
      }
    >
      {notice ? <Alert type="error" showIcon title={notice} /> : null}
      <Form<Values>
        form={form}
        layout="vertical"
        disabled={saving}
        initialValues={{
          messageKey: message?.messageKey,
          messageTypeCode: message?.messageTypeCode,
          defaultMessage: message?.defaultMessage ?? undefined,
          paramDescription: message?.paramDescription ?? undefined,
          texts,
        }}
        onFinish={(values) => {
          submit(values).catch(() => undefined);
        }}
      >
        <Tabs
          items={[
            {
              key: 'base',
              label: text('pages.i18n.basic', '기본 정보'),
              children: (
                <>
                  <Form.Item label={text('pages.i18n.namespaceCode', '네임스페이스 코드')}>
                    <Input value={namespaceCode} disabled />
                  </Form.Item>
                  <Form.Item
                    name="messageKey"
                    label={text('pages.i18n.messageKey', '메시지 키')}
                    rules={[{ required: true, message: text('pages.i18n.required', '필수 값입니다.') }]}
                  >
                    <Input maxLength={200} disabled={editing} />
                  </Form.Item>
                  <Form.Item
                    name="messageTypeCode"
                    label={text('pages.i18n.messageType', '메시지 유형')}
                    rules={[{ required: true, message: text('pages.i18n.required', '필수 값입니다.') }]}
                  >
                    {messageTypes.length > 0 ? (
                      <Select options={messageTypes} />
                    ) : (
                      <Input maxLength={30} />
                    )}
                  </Form.Item>
                  <Form.Item name="defaultMessage" label={text('pages.i18n.defaultMessage', '기본 문구')}>
                    <Input.TextArea maxLength={1000} rows={3} />
                  </Form.Item>
                  <Form.Item name="paramDescription" label={text('pages.i18n.paramDescription', '파라미터 설명')}>
                    <Input maxLength={500} />
                  </Form.Item>
                </>
              ),
            },
            {
              key: 'texts',
              label: text('pages.i18n.translations', '언어별 번역'),
              children: languages.map((language) => {
                const current = message?.translations.find(
                  (item) => item.supportLanguageCode === language.supportLanguageCode,
                );
                return (
                  <div key={language.supportLanguageCode}>
                    <div className={language.defaultLanguage ? styles.defaultLanguage : undefined}>
                      {language.languageName}
                      {language.defaultLanguage ? ` (${text('pages.i18n.defaultLanguage', '기본')})` : ''}
                    </div>
                    <Form.Item
                      name={['texts', language.supportLanguageCode, 'messageText']}
                      label={text('pages.i18n.messageText', '번역 문구')}
                    >
                      <Input.TextArea rows={2} />
                    </Form.Item>
                    <Form.Item
                      name={['texts', language.supportLanguageCode, 'translationStatusCode']}
                      label={text('pages.i18n.translationStatus', '번역 상태')}
                    >
                      {translationStatuses.length > 0 ? (
                        <Select allowClear options={translationStatuses} />
                      ) : (
                        <Input maxLength={30} />
                      )}
                    </Form.Item>
                    <Form.Item
                      name={['texts', language.supportLanguageCode, 'machineTranslated']}
                      label={text('pages.i18n.machineTranslated', '기계 번역')}
                    >
                      <Select
                        allowClear
                        options={[
                          { value: true, label: text('pages.i18n.yes', '예') },
                          { value: false, label: text('pages.i18n.no', '아니오') },
                        ]}
                      />
                    </Form.Item>
                    {editing ? (
                      <Form.Item label={text('pages.i18n.version', '버전')}>
                        <Input value={current?.versionNo ?? ''} disabled />
                      </Form.Item>
                    ) : null}
                  </div>
                );
              }),
            },
          ]}
        />
      </Form>
    </Drawer>
  );
};

export default MessageFormDrawer;
