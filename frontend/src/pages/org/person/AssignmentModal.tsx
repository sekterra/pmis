import { useIntl } from '@umijs/max';
import { Button, Checkbox, DatePicker, Form, Input, Modal, Select, TreeSelect } from 'antd';
import dayjs from 'dayjs';
import React, { useEffect, useRef, useState } from 'react';
import { localToday } from '@/core/codes/options';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import { ApiError } from '@/core/request/errors';
import type { Company } from '@/services/mdm/companyRecord';
import { listDepartmentTree } from '@/services/mdm/dept';
import type { Dept } from '@/services/mdm/deptRecord';
import type { Assignment, AssignmentBody } from '@/services/mdm/peopleRecord';
import { toAssignmentBody } from '@/services/mdm/peopleQuery';
import { nestDepts } from '@/pages/org/dept/tree';
import { assignmentFormErrors, datesOrdered, headNeedsDept, personErrorText, resignedNeedsEnd } from './rules';

type Props = {
  open: boolean;
  saving: boolean;
  assignment?: Assignment;
  companies: Company[];
  forcePrimary: boolean;
  onClose: () => void;
  onSubmit: (body: AssignmentBody) => Promise<void>;
  onNotice: (message: string) => void;
};

type DeptOption = { value: string; title: string; children?: DeptOption[] };

function deptOptions(rows: Dept[]): DeptOption[] {
  return nestDepts(rows.filter((row) => row.useYn === 'Y')).map(function mapNode(node): DeptOption {
    return {
      value: node.deptCode,
      title: `${node.deptName} ${node.deptCode}`,
      children: node.children.length > 0 ? node.children.map(mapNode) : undefined,
    };
  });
}

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

const AssignmentModal: React.FC<Props> = ({
  open,
  saving,
  assignment,
  companies,
  forcePrimary,
  onClose,
  onSubmit,
  onNotice,
}) => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const [form] = Form.useForm();
  const [depts, setDepts] = useState<Dept[]>([]);
  const employment = useCodeOptions('EMPLOYMENT_TYPE');
  const status = useCodeOptions('ASSIGNMENT_STATUS');
  const positions = useCodeOptions('POSITION');
  const titles = useCodeOptions('JOB_TITLE');
  const companyCode = Form.useWatch('companyCode', form) as string | undefined;
  const deptCode = Form.useWatch('deptCode', form) as string | undefined;
  const noticeRef = useRef(onNotice);
  const textRef = useRef(text);
  noticeRef.current = onNotice;
  textRef.current = text;

  useEffect(() => {
    if (!open) {
      return;
    }
    form.setFieldsValue({
      companyCode: assignment?.companyCode,
      deptCode: assignment?.deptCode ?? undefined,
      employeeNo: assignment?.employeeNo ?? undefined,
      employmentTypeCode: assignment?.employmentTypeCode,
      positionCode: assignment?.positionCode ?? undefined,
      jobTitleCode: assignment?.jobTitleCode ?? undefined,
      assignmentStatusCode: assignment?.assignmentStatusCode,
      primaryYn: forcePrimary || assignment?.primaryYn === 'Y',
      deptHeadYn: assignment?.deptHeadYn === 'Y',
      startDate: dayjs(assignment?.startDate ?? localToday()),
      endDate: assignment?.endDate ? dayjs(assignment.endDate) : undefined,
    });
  }, [open, assignment, forcePrimary, form]);

  useEffect(() => {
    if (!open || !companyCode) {
      setDepts([]);
      return;
    }
    let alive = true;
    listDepartmentTree({ companyCode, includeClosed: false })
      .then((rows) => {
        if (alive) {
          setDepts(rows);
        }
      })
      .catch((error: unknown) => {
        if (alive && error instanceof ApiError) {
          noticeRef.current(personErrorText(error, textRef.current));
        }
      });
    return () => {
      alive = false;
    };
  }, [open, companyCode]);

  const submit = async () => {
    const values = await form.validateFields();
    const payload = {
      ...values,
      startDate: dateText(values.startDate) ?? undefined,
      endDate: dateText(values.endDate) ?? undefined,
      primaryYn: forcePrimary || Boolean(values.primaryYn),
      deptHeadYn: Boolean(values.deptHeadYn),
    };
    try {
      await onSubmit(toAssignmentBody(payload, assignment, forcePrimary));
    } catch (error) {
      if (!(error instanceof ApiError)) {
        throw error;
      }
      const fields = assignmentFormErrors(error, text);
      if (fields.length > 0) {
        form.setFields(fields);
      } else {
        onNotice(personErrorText(error, text));
      }
    }
  };

  return (
    <Modal
      open={open}
      title={text(assignment ? 'pages.org.person.editAssignment' : 'pages.org.person.addAssignment', assignment ? '소속 수정' : '소속 추가')}
      onCancel={onClose}
      destroyOnHidden
      footer={[
        <Button key="cancel" onClick={onClose}>
          {text('pages.org.cancel', '취소')}
        </Button>,
        <Button key="save" type="primary" loading={saving} disabled={saving} onClick={() => void submit()}>
          {text('pages.org.save', '저장')}
        </Button>,
      ]}
    >
      <Form
        form={form}
        layout="vertical"
        onValuesChange={(changed) => {
          if ('companyCode' in changed) {
            form.setFieldsValue({ deptCode: undefined, deptHeadYn: false });
          }
          if ('deptCode' in changed && !changed.deptCode) {
            form.setFieldsValue({ deptHeadYn: false });
          }
        }}
      >
        <Form.Item name="companyCode" label={text('pages.org.dept.company', '회사')} rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}>
          <Select
            aria-label={text('pages.org.dept.company', '회사')}
            options={companies.map((company) => ({ value: company.companyCode, label: `${company.companyCode} ${company.companyName}` }))}
          />
        </Form.Item>
        <Form.Item name="deptCode" label={text('pages.org.dept.name', '부서')}>
          <TreeSelect
            aria-label={text('pages.org.dept.name', '부서')}
            allowClear
            disabled={!companyCode}
            treeData={deptOptions(depts)}
            treeDefaultExpandAll
          />
        </Form.Item>
        <Form.Item name="employmentTypeCode" label={text('pages.org.person.employmentType', '고용 유형')} rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}>
          <Select aria-label={text('pages.org.person.employmentType', '고용 유형')} disabled={employment.empty} options={employment.options} />
        </Form.Item>
        {employment.empty ? <p>{text('pages.org.codeEmpty', '공통코드에서 값을 먼저 등록하세요.')}</p> : null}
        <Form.Item name="assignmentStatusCode" label={text('pages.org.dept.assignmentStatus', '재직 상태')} rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}>
          <Select aria-label={text('pages.org.dept.assignmentStatus', '재직 상태')} disabled={status.empty} options={status.options} />
        </Form.Item>
        {status.empty ? <p>{text('pages.org.codeEmpty', '공통코드에서 값을 먼저 등록하세요.')}</p> : null}
        <Form.Item name="positionCode" label={text('pages.org.dept.position', '직위')}>
          <Select aria-label={text('pages.org.dept.position', '직위')} allowClear disabled={positions.empty} options={positions.options} />
        </Form.Item>
        {positions.empty ? <p>{text('pages.org.codeEmpty', '공통코드에서 값을 먼저 등록하세요.')}</p> : null}
        <Form.Item name="jobTitleCode" label={text('pages.org.person.jobTitle', '직책')}>
          <Select aria-label={text('pages.org.person.jobTitle', '직책')} allowClear disabled={titles.empty} options={titles.options} />
        </Form.Item>
        {titles.empty ? <p>{text('pages.org.codeEmpty', '공통코드에서 값을 먼저 등록하세요.')}</p> : null}
        <Form.Item name="employeeNo" label={text('pages.org.person.employeeNo', '사원번호')}>
          <Input aria-label={text('pages.org.person.employeeNo', '사원번호')} maxLength={50} />
        </Form.Item>
        <Form.Item name="startDate" label={text('pages.org.dept.validFrom', '시작일')} rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}>
          <DatePicker aria-label={text('pages.org.dept.validFrom', '시작일')} />
        </Form.Item>
        <Form.Item
          name="endDate"
          label={text('pages.org.dept.validTo', '종료일')}
          dependencies={['startDate', 'assignmentStatusCode']}
          rules={[
            {
              validator: async (_, value) => {
                const start = dateText(form.getFieldValue('startDate'));
                const end = dateText(value);
                const statusCode = form.getFieldValue('assignmentStatusCode') as string | undefined;
                if (!datesOrdered(start, end) || !resignedNeedsEnd(statusCode, end)) {
                  throw new Error(text('pages.org.person.dateOrder', '종료일은 시작일 이상이어야 하고, 퇴직이면 종료일이 필요합니다.'));
                }
              },
            },
          ]}
        >
          <DatePicker aria-label={text('pages.org.dept.validTo', '종료일')} />
        </Form.Item>
        <Form.Item name="primaryYn" valuePropName="checked">
          <Checkbox disabled={forcePrimary} aria-label={text('pages.org.person.primary', '주 소속')}>
            {text('pages.org.person.primary', '주 소속')}
          </Checkbox>
        </Form.Item>
        {forcePrimary ? <p>{text('pages.org.person.firstPrimary', '첫 소속은 주 소속이 됩니다.')}</p> : <p>{text('pages.org.person.primarySwitch', '주 소속으로 바꾸면 기존 주 소속이 해제됩니다.')}</p>}
        <Form.Item
          name="deptHeadYn"
          valuePropName="checked"
          dependencies={['deptCode']}
          rules={[
            {
              validator: async (_, value) => {
                if (!headNeedsDept(Boolean(value), form.getFieldValue('deptCode'))) {
                  throw new Error(text('pages.org.person.headNeedsDept', '부서가 없으면 부서장이 될 수 없습니다.'));
                }
              },
            },
          ]}
        >
          <Checkbox disabled={!deptCode} aria-label={text('pages.org.person.deptHead', '부서장')}>
            {text('pages.org.person.deptHead', '부서장')}
          </Checkbox>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AssignmentModal;
