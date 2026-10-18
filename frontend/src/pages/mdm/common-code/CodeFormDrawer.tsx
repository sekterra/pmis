import { useIntl } from '@umijs/max';
import { Alert, Button, DatePicker, Drawer, Form, Input, InputNumber, Select, Tabs } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import React, { useEffect, useState } from 'react';
import { ApiError, toFormFields } from '@/core/request/errors';
import type { CodeGroup, CommonCode, CommonCodeBody } from '@/services/mdm/code';
import type { SupportLanguage } from '@/services/mdm/data';
import { listGroupCodes } from '@/services/mdm/codes';
import {
  blankToNull,
  codeErrorText,
  datesInOrder,
  languagesMissingName,
  parentChoices,
  toTranslations,
  valueWithinLength,
} from './rules';

type Values = {
  codeValue: string;
  codeName: string;
  parentCodeValue?: string;
  validFromDate?: Dayjs | null;
  validToDate?: Dayjs | null;
  sortOrder?: number | null;
  extAttr1?: string;
  extAttr2?: string;
  extAttr3?: string;
  extAttr4?: string;
  extAttr5?: string;
  changeReason?: string;
  names?: Record<string, { name?: string; description?: string }>;
};

type Props = {
  open: boolean;
  saving: boolean;
  group: CodeGroup;
  code?: CommonCode;
  languages: SupportLanguage[];
  onClose: () => void;
  onSubmit: (body: CommonCodeBody) => Promise<void>;
};

function dateText(value?: Dayjs | null): string | null {
  return value ? value.format('YYYY-MM-DD') : null;
}

const CodeFormDrawer: React.FC<Props> = ({
  open,
  saving,
  group,
  code,
  languages,
  onClose,
  onSubmit,
}) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });
  const [form] = Form.useForm<Values>();
  const [message, setMessage] = useState<string>();
  const [parents, setParents] = useState<CommonCode[]>([]);

  useEffect(() => {
    if (!open || !group.hierarchy) {
      return;
    }
    let alive = true;
    listGroupCodes(group.codeGroupCode)
      .then((rows) => {
        if (alive) {
          setParents(parentChoices(rows, code?.codeValue));
        }
      })
      .catch((error: unknown) => {
        if (alive) {
          const api = error instanceof ApiError ? error : new ApiError('');
          setMessage(codeErrorText(api));
        }
      });
    return () => {
      alive = false;
    };
  }, [open, group.hierarchy, group.codeGroupCode, code?.codeValue]);

  const names: Values['names'] = {};
  for (const language of languages) {
    const current = code?.translations.find(
      (item) => item.supportLanguageCode === language.supportLanguageCode,
    );
    names[language.supportLanguageCode] = {
      name: current?.name,
      description: current?.description ?? undefined,
    };
  }

  const submit = async (values: Values) => {
    const from = dateText(values.validFromDate);
    const to = dateText(values.validToDate);
    if (!datesInOrder(from, to)) {
      setMessage(text('pages.mdm.invalidDate', '시작일은 종료일보다 늦을 수 없습니다.'));
      return;
    }
    if (!valueWithinLength(values.codeValue, group.codeValueLength)) {
      setMessage(text('pages.mdm.valueTooLong', '코드 값이 허용 길이보다 깁니다.'));
      return;
    }
    const rows = languages.map((language) => ({
      supportLanguageCode: language.supportLanguageCode,
      name: values.names?.[language.supportLanguageCode]?.name,
      description: values.names?.[language.supportLanguageCode]?.description,
    }));
    if (languagesMissingName(rows).length > 0) {
      setMessage(text('pages.mdm.nameRequired', '설명을 적은 언어는 이름도 입력하세요.'));
      return;
    }
    setMessage(undefined);
    try {
      await onSubmit({
        groupCode: group.codeGroupCode,
        codeValue: values.codeValue.trim(),
        codeName: values.codeName.trim(),
        parentCodeValue: group.hierarchy ? blankToNull(values.parentCodeValue) : null,
        sortOrder: values.sortOrder ?? null,
        extAttr1: blankToNull(values.extAttr1),
        extAttr2: blankToNull(values.extAttr2),
        extAttr3: blankToNull(values.extAttr3),
        extAttr4: blankToNull(values.extAttr4),
        extAttr5: blankToNull(values.extAttr5),
        validFromDate: from,
        validToDate: to,
        changeReason: blankToNull(values.changeReason),
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

  return (
    <Drawer
      title={
        code ? text('pages.mdm.editCode', '코드 수정') : text('pages.mdm.createCode', '코드 등록')
      }
      open={open}
      size={520}
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
          codeValue: code?.codeValue,
          codeName: code?.codeName,
          parentCodeValue: code?.parentCodeValue ?? undefined,
          validFromDate: code?.validFromDate ? dayjs(code.validFromDate) : undefined,
          validToDate: code?.validToDate ? dayjs(code.validToDate) : undefined,
          sortOrder: code?.sortOrder,
          extAttr1: code?.extAttr1 ?? undefined,
          extAttr2: code?.extAttr2 ?? undefined,
          extAttr3: code?.extAttr3 ?? undefined,
          extAttr4: code?.extAttr4 ?? undefined,
          extAttr5: code?.extAttr5 ?? undefined,
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
                    name="codeValue"
                    label={text('pages.mdm.codeValue', '코드 값')}
                    rules={[
                      { required: true, message: text('pages.mdm.required', '필수 값입니다.') },
                    ]}
                  >
                    <Input maxLength={group.codeValueLength ?? 100} />
                  </Form.Item>
                  <Form.Item
                    name="codeName"
                    label={text('pages.mdm.name', '이름')}
                    rules={[
                      { required: true, message: text('pages.mdm.required', '필수 값입니다.') },
                    ]}
                  >
                    <Input maxLength={200} />
                  </Form.Item>
                  {group.hierarchy ? (
                    <Form.Item
                      name="parentCodeValue"
                      label={text('pages.mdm.parent', '상위 코드')}
                    >
                      <Select
                        allowClear
                        showSearch
                        options={parents.map((item) => ({
                          value: item.codeValue,
                          label: `${item.codeValue} ${item.codeName}`,
                        }))}
                      />
                    </Form.Item>
                  ) : null}
                  <Form.Item name="validFromDate" label={text('pages.mdm.validFrom', '유효 시작일')}>
                    <DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="validToDate" label={text('pages.mdm.validTo', '유효 종료일')}>
                    <DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item name="sortOrder" label={text('pages.mdm.sortOrder', '정렬 순서')}>
                    <InputNumber precision={0} style={{ width: '100%' }} />
                  </Form.Item>
                  {([1, 2, 3, 4, 5] as const).map((index) => (
                    <Form.Item
                      key={index}
                      name={`extAttr${index}`}
                      label={`${text('pages.mdm.extAttr', '확장 속성')} ${index}`}
                    >
                      <Input maxLength={200} />
                    </Form.Item>
                  ))}
                  <Form.Item name="changeReason" label={text('pages.mdm.reason', '변경 사유')}>
                    <Input maxLength={500} />
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

export default CodeFormDrawer;
