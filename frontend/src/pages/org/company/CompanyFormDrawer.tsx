import { useIntl } from '@umijs/max';
import { Button, Drawer, Form, Input, InputNumber, Select, Space } from 'antd';
import React, { useEffect, useState } from 'react';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import { ApiError } from '@/core/request/errors';
import NameTranslations from '@/pages/org/shared/NameTranslations';
import type { Company, CompanyCreateBody, CompanyUpdateBody } from '@/services/mdm/companyRecord';
import { toCreateBody, toUpdateBody } from '@/services/mdm/companyQuery';
import type { SupportLanguage } from '@/services/mdm/data';
import type { NationRecord } from '@/services/mdm/languageRecord';
import { listCompanies } from '@/services/mdm/company';
import { companyFormErrors, namesFromTranslations, parentChoices, validCompanyCode, companyErrorText } from './rules';

type Props = {
  open: boolean;
  saving: boolean;
  company?: Company;
  languages: SupportLanguage[];
  nations: NationRecord[];
  onClose: () => void;
  onSubmit: (body: CompanyCreateBody | CompanyUpdateBody) => Promise<void>;
  onNotice: (message: string) => void;
};

const CompanyFormDrawer: React.FC<Props> = ({
  open,
  saving,
  company,
  languages,
  nations,
  onClose,
  onSubmit,
  onNotice,
}) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) => intl.formatMessage({ id, defaultMessage });
  const [form] = Form.useForm();
  const types = useCodeOptions('COMPANY_TYPE');
  const [parents, setParents] = useState<Company[]>([]);
  const editing = Boolean(company);

  useEffect(() => {
    if (!open) {
      return;
    }
    form.setFieldsValue({
      companyCode: company?.companyCode,
      companyName: company?.companyName,
      companyShortName: company?.companyShortName ?? undefined,
      companyTypeCode: company?.companyTypeCode ?? undefined,
      businessRegistrationNo: company?.businessRegistrationNo ?? undefined,
      nationCode: company?.nationCode ?? undefined,
      parentCompanyCode: company?.parentCompanyCode ?? undefined,
      sortOrder: company?.sortOrder ?? undefined,
      names: namesFromTranslations(company),
    });
    listCompanies({ keyword: '', useYn: 'Y', page: 1, size: 20 })
      .then((page) => setParents(parentChoices(page.items, company?.companyCode)))
      .catch(() => setParents([]));
  }, [open, company, form]);

  const searchParents = (keyword: string) => {
    listCompanies({ keyword, useYn: 'Y', page: 1, size: 20 })
      .then((page) => setParents(parentChoices(page.items, company?.companyCode)))
      .catch(() => setParents([]));
  };

  const submit = async () => {
    const values = await form.validateFields();
    const body = company ? toUpdateBody(values, company) : toCreateBody(values);
    try {
      await onSubmit(body);
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      const fields = companyFormErrors(api);
      if (fields.length > 0) {
        form.setFields(fields);
        return;
      }
      onNotice(companyErrorText(api));
    }
  };

  return (
    <Drawer
      title={editing ? text('pages.org.company.edit', '회사 수정') : text('pages.org.company.create', '회사 등록')}
      open={open}
      size="large"
      onClose={onClose}
      extra={
        <Space>
          <Button onClick={onClose}>{text('pages.org.cancel', '취소')}</Button>
          <Button type="primary" loading={saving} disabled={saving} onClick={() => void submit()}>
            {text('pages.org.save', '저장')}
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="companyCode"
          label={text('pages.org.company.code', '회사 코드')}
          rules={
            editing
              ? []
              : [
                  { required: true, message: text('pages.org.required', '필수 값입니다.') },
                  {
                    validator: async (_, value: string) => {
                      if (!value || validCompanyCode(value.trim())) {
                        return;
                      }
                      throw new Error(text('pages.org.company.codeRule', '대문자, 숫자, 밑줄, 하이픈 1~50자입니다.'));
                    },
                  },
                ]
          }
        >
          <Input maxLength={50} disabled={editing} />
        </Form.Item>
        <Form.Item
          name="companyName"
          label={text('pages.org.company.name', '회사명')}
          rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}
        >
          <Input maxLength={200} />
        </Form.Item>
        <Form.Item name="companyShortName" label={text('pages.org.company.shortName', '약칭')}>
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item
          name="companyTypeCode"
          label={text('pages.org.company.type', '회사 유형')}
          extra={types.empty ? text('pages.org.codeEmpty', '공통코드에서 값을 먼저 등록하세요.') : undefined}
          rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}
        >
          <Select
            allowClear
            disabled={types.empty}
            loading={types.loading}
            options={types.options}
          />
        </Form.Item>
        <Form.Item name="businessRegistrationNo" label={text('pages.org.company.businessNo', '사업자등록번호')}>
          <Input maxLength={20} />
        </Form.Item>
        <Form.Item name="nationCode" label={text('pages.org.company.nation', '국가')}>
          <Select
            allowClear
            showSearch={{ optionFilterProp: 'label' }}
            options={nations.map((nation) => ({
              value: nation.nationCode,
              label: `${nation.nationCode} ${nation.nationName}`,
            }))}
          />
        </Form.Item>
        <Form.Item name="parentCompanyCode" label={text('pages.org.company.parent', '상위 회사')}>
          <Select
            allowClear
            showSearch={{ filterOption: false, onSearch: searchParents }}
            options={parentChoices(parents, company?.companyCode).map((row) => ({
              value: row.companyCode,
              label: `${row.companyCode} ${row.companyName}`,
            }))}
          />
        </Form.Item>
        <Form.Item name="sortOrder" label={text('pages.org.company.sortOrder', '정렬 순서')}>
          <InputNumber precision={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label={text('pages.org.translations', '이름 번역')}>
          <NameTranslations languages={languages} />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default CompanyFormDrawer;
