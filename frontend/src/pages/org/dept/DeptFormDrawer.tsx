import { useIntl } from '@umijs/max';
import { Button, DatePicker, Drawer, Form, Input, InputNumber, Select, Space, TreeSelect } from 'antd';
import dayjs from 'dayjs';
import React, { useEffect, useState } from 'react';
import { localToday } from '@/core/codes/options';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import { ApiError } from '@/core/request/errors';
import NameTranslations from '@/pages/org/shared/NameTranslations';
import { toCreateBody, toUpdateBody } from '@/services/mdm/deptQuery';
import type { Dept, DeptCreateBody, DeptUpdateBody } from '@/services/mdm/deptRecord';
import type { SupportLanguage } from '@/services/mdm/data';
import { datesOrdered, deptFormErrors, historyChange, namesFromDept, validDeptCode, deptErrorText } from './rules';
import { forbiddenParentPks, nestDepts } from './tree';

type Props = {
  open: boolean;
  saving: boolean;
  companyCode: string;
  dept?: Dept;
  parentDeptCode?: string | null;
  rows: Dept[];
  languages: SupportLanguage[];
  onClose: () => void;
  onSubmit: (body: DeptCreateBody | DeptUpdateBody) => Promise<void>;
  onNotice: (message: string) => void;
};

type ParentOption = {
  value: string;
  title: string;
  disabled?: boolean;
  children?: ParentOption[];
};

function dateText(value: unknown): string | null {
  if (!value) {
    return null;
  }
  if (typeof value === 'string') {
    return value;
  }
  if (dayjs.isDayjs(value)) {
    return value.format('YYYY-MM-DD');
  }
  return null;
}

const DeptFormDrawer: React.FC<Props> = ({
  open,
  saving,
  companyCode,
  dept,
  parentDeptCode,
  rows,
  languages,
  onClose,
  onSubmit,
  onNotice,
}) => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const [form] = Form.useForm();
  const types = useCodeOptions('DEPT_TYPE');
  const editing = Boolean(dept);
  const [change, setChange] = useState<{ name: boolean; parent: boolean; any: boolean }>();

  useEffect(() => {
    if (!open) {
      return;
    }
    setChange(undefined);
    form.setFieldsValue({
      deptCode: dept?.deptCode,
      deptName: dept?.deptName,
      deptTypeCode: dept?.deptTypeCode ?? undefined,
      parentDeptCode: dept ? (dept.parentDeptCode ?? undefined) : (parentDeptCode ?? undefined),
      sortOrder: dept?.sortOrder,
      validFromDate: dayjs(dept?.validFromDate ?? localToday()),
      validToDate: dept?.validToDate ? dayjs(dept.validToDate) : undefined,
      effectiveDate: dayjs(localToday()),
      changeReason: undefined,
      names: namesFromDept(dept),
    });
  }, [open, dept, parentDeptCode, form]);

  const parentData = () => {
    const blocked = dept ? forbiddenParentPks(rows, dept.deptPk) : new Set<number>();
    const active = nestDepts(rows.filter((row) => row.useYn === 'Y'));
    const map = (nodes: ReturnType<typeof nestDepts>): ParentOption[] =>
      nodes.map((node) => ({
        value: node.deptCode,
        title: `${node.deptName} ${node.deptCode}`,
        disabled: blocked.has(node.deptPk),
        children: map(node.children),
      }));
    return map(active);
  };

  const submit = async () => {
    const values = await form.validateFields();
    const payload = {
      ...values,
      validFromDate: dateText(values.validFromDate),
      validToDate: dateText(values.validToDate),
      effectiveDate: dateText(values.effectiveDate),
    };
    const track = Boolean(dept && historyChange(dept, payload).any);
    const body = dept ? toUpdateBody(payload, dept, track) : toCreateBody(companyCode, payload);
    try {
      await onSubmit(body);
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      const fields = deptFormErrors(api, text);
      if (fields.length > 0) {
        form.setFields(fields);
        return;
      }
      onNotice(deptErrorText(api, text));
    }
  };

  return (
    <Drawer
      title={editing ? text('pages.org.dept.edit', '부서 수정') : text('pages.org.dept.create', '부서 등록')}
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
      <Form
        form={form}
        layout="vertical"
        onValuesChange={(_, all) => {
          setChange(dept ? historyChange(dept, all) : undefined);
        }}
      >
        <Form.Item label={text('pages.org.dept.company', '회사')}>
          <Input value={companyCode} disabled aria-label={text('pages.org.dept.company', '회사')} />
        </Form.Item>
        <Form.Item
          name="deptCode"
          label={text('pages.org.dept.code', '부서 코드')}
          extra={text('pages.org.dept.codeRule', '50자 이내입니다.')}
          rules={[
            { required: !editing, message: text('pages.org.required', '필수 값입니다.') },
            {
              validator: async (_, value) => {
                if (editing || validDeptCode(value)) {
                  return;
                }
                throw new Error(text('pages.org.dept.codeRule', '50자 이내입니다.'));
              },
            },
          ]}
        >
          <Input maxLength={50} disabled={editing} />
        </Form.Item>
        <Form.Item
          name="deptName"
          label={text('pages.org.dept.name', '부서명')}
          rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}
        >
          <Input maxLength={200} />
        </Form.Item>
        <Form.Item
          name="deptTypeCode"
          label={text('pages.org.dept.type', '부서 유형')}
          extra={types.empty ? text('pages.org.codeEmpty', '공통코드에서 값을 먼저 등록하세요.') : undefined}
          rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}
        >
          <Select allowClear disabled={types.empty} loading={types.loading} options={types.options} />
        </Form.Item>
        <Form.Item name="parentDeptCode" label={text('pages.org.dept.parent', '상위 부서')}>
          <TreeSelect
            allowClear
            treeDefaultExpandAll
            treeData={parentData()}
            aria-label={text('pages.org.dept.parent', '상위 부서')}
          />
        </Form.Item>
        <Form.Item name="sortOrder" label={text('pages.org.dept.sortOrder', '정렬 순서')}>
          <InputNumber precision={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="validFromDate" label={text('pages.org.dept.validFrom', '유효 시작일')}>
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="validToDate"
          label={text('pages.org.dept.validTo', '유효 종료일')}
          rules={[
            {
              validator: async (_, value) => {
                if (datesOrdered(dateText(form.getFieldValue('validFromDate')), dateText(value))) {
                  return;
                }
                throw new Error(text('pages.org.dept.dateOrder', '종료일은 시작일보다 빠를 수 없습니다.'));
              },
            },
          ]}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <div hidden={!change?.any}>
          <Form.Item name="effectiveDate" label={text('pages.org.dept.effectiveDate', '적용일')}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="changeReason" label={text('pages.org.dept.changeReason', '변경 사유')}>
            <Input maxLength={500} />
          </Form.Item>
          {change?.name && change.parent ? (
            <p>{text('pages.org.dept.historyBoth', '이름과 상위 부서가 함께 바뀌면 이력이 두 줄 남습니다.')}</p>
          ) : null}
        </div>
        <Form.Item label={text('pages.org.translations', '이름 번역')}>
          <NameTranslations languages={languages} />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default DeptFormDrawer;
