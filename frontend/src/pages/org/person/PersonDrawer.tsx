import { useIntl } from '@umijs/max';
import { Alert, Button, DatePicker, Drawer, Form, Input, Modal, Select, Space, Table, Tabs } from 'antd';
import type { TableProps } from 'antd';
import dayjs from 'dayjs';
import React, { useEffect, useRef, useState } from 'react';
import { localToday } from '@/core/codes/options';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import { ApiError } from '@/core/request/errors';
import NameTranslations from '@/pages/org/shared/NameTranslations';
import type { Company } from '@/services/mdm/companyRecord';
import type { SupportLanguage } from '@/services/mdm/data';
import { assignProfileImage, clearProfileImage, fetchProfileImage, uploadFile } from '@/services/mdm/files';
import {
  createAssignment,
  deleteAssignment,
  endAssignment,
  listAssignments,
  restoreAssignment,
  updateAssignment,
} from '@/services/mdm/people';
import { namesFromPerson, toCreateBody, toEndBody, toUpdateBody } from '@/services/mdm/peopleQuery';
import type { Assignment, Person, PersonCreateBody, PersonUpdateBody } from '@/services/mdm/peopleRecord';
import AssignmentModal from './AssignmentModal';
import { isMissingProfile, profileFileIssue, releaseProfileUrl, sendProfileImage } from './profile';
import { assignmentFormErrors, isFirstCurrentAssignment, personErrorText, validEmail } from './rules';

type Props = {
  open: boolean;
  person?: Person;
  tab: string;
  saving: boolean;
  languages: SupportLanguage[];
  companies: Company[];
  canCreate: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  canUpload: boolean;
  onTab: (tab: string) => void;
  onClose: () => void;
  onSubmit: (body: PersonCreateBody | PersonUpdateBody) => Promise<void>;
  onDelete: () => void;
  onRestore: () => void;
  onNotice: (message: string) => void;
  onChanged: () => void;
};

const PersonDrawer: React.FC<Props> = ({
  open,
  person,
  tab,
  saving,
  languages,
  companies,
  canCreate,
  canUpdate,
  canDelete,
  canUpload,
  onTab,
  onClose,
  onSubmit,
  onDelete,
  onRestore,
  onNotice,
  onChanged,
}) => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const [form] = Form.useForm();
  const [rows, setRows] = useState<Assignment[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [useYn, setUseYn] = useState<string>();
  const [photo, setPhoto] = useState<string>();
  const [uploading, setUploading] = useState(false);
  const [assignmentOpen, setAssignmentOpen] = useState(false);
  const [editing, setEditing] = useState<Assignment>();
  const [ending, setEnding] = useState<Assignment>();
  const [endDate, setEndDate] = useState(localToday());
  const [endStatus, setEndStatus] = useState('RESIGNED');
  const [busy, setBusy] = useState(false);
  const [reload, setReload] = useState(0);
  const [imageVersion, setImageVersion] = useState(0);
  const noticeRef = useRef(onNotice);
  const textRef = useRef(text);
  const loadedKey = useRef('');
  noticeRef.current = onNotice;
  textRef.current = text;
  const active = person?.useYn !== 'N';
  const editable = Boolean(person) && active && canUpdate;
  const statuses = useCodeOptions('ASSIGNMENT_STATUS');
  const positions = useCodeOptions('POSITION');
  const employment = useCodeOptions('EMPLOYMENT_TYPE');
  const titles = useCodeOptions('JOB_TITLE');

  useEffect(() => {
    if (!open) {
      return;
    }
    form.setFieldsValue({
      personName: person?.personName,
      email: person?.email ?? undefined,
      mobilePhoneNo: person?.mobilePhoneNo ?? undefined,
      names: namesFromPerson(person),
    });
  }, [open, person, form]);

  useEffect(() => {
    if (!open || !person?.hasProfileImage) {
      setPhoto(undefined);
      return;
    }
    const personPk = person.personPk;
    let alive = true;
    let created: string | undefined;
    fetchProfileImage(personPk)
      .then((blob) => {
        if (!alive) {
          return;
        }
        created = URL.createObjectURL(blob);
        setPhoto(created);
      })
      .catch((error: unknown) => {
        if (!alive || isMissingProfile(error)) {
          if (alive) {
            setPhoto(undefined);
          }
          return;
        }
        if (error instanceof ApiError) {
          noticeRef.current(personErrorText(error, textRef.current));
        }
      });
    return () => {
      alive = false;
      releaseProfileUrl(created, (url) => URL.revokeObjectURL(url));
    };
  }, [open, person?.personPk, person?.hasProfileImage, imageVersion]);

  useEffect(() => {
    if (!open || tab !== 'assignments' || !person) {
      return;
    }
    const personPk = person.personPk;
    const key = `${personPk}:${page}:${useYn ?? ''}:${reload}`;
    if (loadedKey.current === key) {
      return;
    }
    loadedKey.current = key;
    let alive = true;
    listAssignments(personPk, { page, size: 20, useYn })
      .then((result) => {
        if (alive && loadedKey.current === key) {
          setRows(result.items);
          setTotal(result.total);
        }
      })
      .catch((error: unknown) => {
        if (alive && loadedKey.current === key && error instanceof ApiError) {
          noticeRef.current(personErrorText(error, textRef.current));
        }
      });
    return () => {
      alive = false;
    };
  }, [open, tab, person?.personPk, page, useYn, reload]);

  const labelOf = (options: { value: string; label: string }[], code: string | null | undefined) =>
    code ? (options.find((item) => item.value === code)?.label ?? code) : text('pages.org.dept.none', '없음');

  const upload = async (file: File) => {
    if (!person) {
      return;
    }
    const issue = profileFileIssue(file);
    if (issue === 'extension') {
      onNotice(text('pages.org.person.errors.extension', '허용되지 않는 파일 형식입니다.'));
      return;
    }
    if (issue === 'size') {
      onNotice(text('pages.org.person.errors.tooLarge', '파일이 너무 큽니다.'));
      return;
    }
    setUploading(true);
    try {
      await sendProfileImage(person.personPk, file, uploadFile, assignProfileImage);
      setImageVersion((value) => value + 1);
      onChanged();
    } catch (error) {
      if (error instanceof ApiError) {
        onNotice(personErrorText(error, text));
      }
    } finally {
      setUploading(false);
    }
  };

  const columns: TableProps<Assignment>['columns'] = [
    { title: text('pages.org.dept.company', '회사'), dataIndex: 'companyName' },
    { title: text('pages.org.dept.name', '부서'), render: (_, row) => row.deptName ?? '-' },
    { title: text('pages.org.person.employmentType', '고용 유형'), render: (_, row) => labelOf(employment.options, row.employmentTypeCode) },
    { title: text('pages.org.dept.position', '직위'), render: (_, row) => labelOf(positions.options, row.positionCode) },
    { title: text('pages.org.person.jobTitle', '직책'), render: (_, row) => labelOf(titles.options, row.jobTitleCode) },
    { title: text('pages.org.person.employeeNo', '사원번호'), render: (_, row) => row.employeeNo ?? '-' },
    { title: text('pages.org.dept.period', '기간'), render: (_, row) => `${row.startDate} ~ ${row.endDate ?? text('pages.org.dept.none', '없음')}` },
    { title: text('pages.org.person.primary', '주 소속'), render: (_, row) => (row.primaryYn === 'Y' ? 'Y' : 'N') },
    { title: text('pages.org.person.deptHead', '부서장'), render: (_, row) => (row.deptHeadYn === 'Y' ? 'Y' : 'N') },
    { title: text('pages.org.dept.assignmentStatus', '재직 상태'), render: (_, row) => labelOf(statuses.options, row.assignmentStatusCode) },
    { title: text('pages.org.use', '사용 여부'), render: (_, row) => (row.useYn === 'Y' ? text('pages.org.inUse', '사용') : text('pages.org.stopped', '중지')) },
    {
      title: text('pages.org.actions', '작업'),
      render: (_, row) =>
        editable ? (
          <Space>
            {row.useYn === 'Y' ? (
              <Button
                size="small"
                onClick={() => {
                  setEditing(row);
                  setAssignmentOpen(true);
                }}
              >
                {text('pages.org.edit', '수정')}
              </Button>
            ) : null}
            {row.useYn === 'Y' && !row.endDate ? (
              <Button
                size="small"
                onClick={() => {
                  setEnding(row);
                  setEndDate(localToday());
                  setEndStatus('RESIGNED');
                }}
              >
                {text('pages.org.dept.close', '종료')}
              </Button>
            ) : null}
            {row.useYn === 'Y' && canDelete ? (
              <Button size="small" onClick={() => void deleteAssignment(row.assignmentPk).then(() => { setReload((value) => value + 1); onChanged(); }).catch((error: unknown) => error instanceof ApiError && onNotice(personErrorText(error, text)))}>
                {text('pages.org.delete', '삭제')}
              </Button>
            ) : null}
            {row.useYn !== 'Y' ? (
              <Button size="small" onClick={() => void restoreAssignment(row.assignmentPk).then(() => { setReload((value) => value + 1); onChanged(); }).catch((error: unknown) => error instanceof ApiError && onNotice(personErrorText(error, text)))}>
                {text('pages.org.restore', '복구')}
              </Button>
            ) : null}
          </Space>
        ) : null,
    },
  ];

  return (
    <Drawer
      open={open}
      size="large"
      title={person?.personName ?? text('pages.org.person.create', '사람 등록')}
      onClose={onClose}
      extra={
        person ? (
          person.useYn === 'Y' && canDelete ? (
            <Button onClick={onDelete}>{text('pages.org.delete', '삭제')}</Button>
          ) : person.useYn !== 'Y' && canUpdate ? (
            <Button onClick={onRestore}>{text('pages.org.restore', '복구')}</Button>
          ) : null
        ) : null
      }
    >
      {!person ? <p>{text('pages.org.person.afterSave', '저장한 뒤에 소속과 프로필 이미지를 등록할 수 있습니다.')}</p> : null}
      {person && !active ? <Alert type="info" showIcon title={text('pages.org.person.inactive', '사용 중지된 사람은 소속과 이미지를 바꿀 수 없습니다.')} /> : null}
      <Tabs
        activeKey={person ? tab : 'basic'}
        onChange={onTab}
        items={[
          {
            key: 'basic',
            label: text('pages.org.dept.tabBasic', '기본 정보'),
            children: (
              <>
                <Form
                  form={form}
                  layout="vertical"
                  onFinish={async (values) => {
                    const body = person ? toUpdateBody(values, person) : toCreateBody(values);
                    try {
                      await onSubmit(body);
                    } catch (error) {
                      if (!(error instanceof ApiError)) {
                        throw error;
                      }
                      const fields = assignmentFormErrors(error, text);
                      const known = fields.filter((field) => field.name === 'personName' || field.name === 'email' || field.name === 'mobilePhoneNo');
                      if (known.length > 0) {
                        form.setFields(known);
                      } else if ((error.fieldErrors ?? []).length > 0) {
                        form.setFields(error.fieldErrors?.map((item) => ({ name: item.field, errors: [item.message] })) ?? []);
                      } else {
                        onNotice(personErrorText(error, text));
                      }
                    }
                  }}
                >
                  <Form.Item name="personName" label={text('pages.org.name', '이름')} rules={[{ required: true, message: text('pages.org.required', '필수 값입니다.') }]}>
                    <Input aria-label={text('pages.org.name', '이름')} maxLength={100} />
                  </Form.Item>
                  <Form.Item
                    name="email"
                    label={text('pages.org.person.email', '이메일')}
                    rules={[{ validator: async (_, value) => { if (!validEmail(value)) { throw new Error(text('pages.org.person.emailRule', '이메일 형식이 아닙니다.')); } } }]}
                  >
                    <Input aria-label={text('pages.org.person.email', '이메일')} maxLength={200} />
                  </Form.Item>
                  <Form.Item name="mobilePhoneNo" label={text('pages.org.person.mobile', '휴대전화')}>
                    <Input aria-label={text('pages.org.person.mobile', '휴대전화')} maxLength={30} />
                  </Form.Item>
                  <NameTranslations languages={languages} />
                  <Button type="primary" htmlType="submit" loading={saving} disabled={saving}>
                    {text('pages.org.save', '저장')}
                  </Button>
                </Form>
                {person ? (
                  <div style={{ marginTop: 16 }}>
                    {photo ? <img src={photo} alt={text('pages.org.person.photo', '프로필 이미지')} style={{ maxWidth: 120 }} /> : <p>{text('pages.org.person.noPhoto', '사진 없음')}</p>}
                    {editable && canUpload ? (
                      <input
                        aria-label={text('pages.org.person.upload', '이미지 올리기')}
                        type="file"
                        accept=".png,.jpg,.jpeg,.gif,.webp"
                        disabled={uploading}
                        onChange={(event) => {
                          const file = event.target.files?.[0];
                          if (file) {
                            void upload(file);
                          }
                          event.target.value = '';
                        }}
                      />
                    ) : null}
                    {editable && !canUpload ? <p>{text('pages.org.person.fileForbidden', '파일을 올릴 권한이 없습니다.')}</p> : null}
                    {editable ? (
                      <Button
                        disabled={uploading || !person.hasProfileImage}
                        onClick={() =>
                          void clearProfileImage(person.personPk)
                            .then(onChanged)
                            .catch((error: unknown) => error instanceof ApiError && onNotice(personErrorText(error, text)))
                        }
                      >
                        {text('pages.org.person.removePhoto', '이미지 삭제')}
                      </Button>
                    ) : null}
                  </div>
                ) : null}
              </>
            ),
          },
          {
            key: 'assignments',
            label: text('pages.org.person.tabAssignment', '소속'),
            disabled: !person,
            children: person ? (
              <>
                <Space style={{ marginBottom: 12 }}>
                  <Select
                    aria-label={text('pages.org.use', '사용 여부')}
                    allowClear
                    placeholder={text('pages.org.useAll', '사용 여부 전체')}
                    style={{ minWidth: 140 }}
                    value={useYn}
                    options={[
                      { value: 'Y', label: text('pages.org.inUse', '사용') },
                      { value: 'N', label: text('pages.org.stopped', '중지') },
                    ]}
                    onChange={(value) => {
                      setUseYn(value);
                      setPage(1);
                      loadedKey.current = '';
                    }}
                  />
                  {canCreate && active ? (
                    <Button
                      onClick={() => {
                        setEditing(undefined);
                        setAssignmentOpen(true);
                      }}
                    >
                      {text('pages.org.person.addAssignment', '소속 추가')}
                    </Button>
                  ) : null}
                </Space>
                <Table<Assignment>
                  rowKey="assignmentPk"
                  columns={columns}
                  dataSource={rows}
                  pagination={{ current: page, pageSize: 20, total, showSizeChanger: false }}
                  onChange={(pagination) => setPage(pagination.current ?? 1)}
                />
              </>
            ) : null,
          },
        ]}
      />
      <AssignmentModal
        open={assignmentOpen}
        saving={busy}
        assignment={editing}
        companies={companies}
        forcePrimary={isFirstCurrentAssignment(person?.assignments)}
        onClose={() => setAssignmentOpen(false)}
        onNotice={onNotice}
        onSubmit={async (body) => {
          if (!person) {
            return;
          }
          setBusy(true);
          try {
            if (editing) {
              await updateAssignment(editing.assignmentPk, body);
            } else {
              await createAssignment(person.personPk, body);
            }
            setAssignmentOpen(false);
            loadedKey.current = '';
            setReload((value) => value + 1);
            onChanged();
          } finally {
            setBusy(false);
          }
        }}
      />
      <Modal
        open={Boolean(ending)}
        title={text('pages.org.person.endTitle', '소속을 종료할까요?')}
        onCancel={() => setEnding(undefined)}
        okText={text('pages.org.dept.closeOk', '종료 확인')}
        cancelText={text('pages.org.cancel', '취소')}
        confirmLoading={busy}
        onOk={() => {
          if (!ending) {
            return;
          }
          setBusy(true);
          endAssignment(ending.assignmentPk, toEndBody({ endDate, assignmentStatusCode: endStatus }, localToday()))
            .then(() => {
              setEnding(undefined);
              loadedKey.current = '';
              setReload((value) => value + 1);
              onChanged();
            })
            .catch((error: unknown) => {
              if (error instanceof ApiError) {
                onNotice(personErrorText(error, text));
              }
            })
            .finally(() => setBusy(false));
        }}
      >
        <DatePicker aria-label={text('pages.org.dept.validTo', '종료일')} value={dayjs(endDate)} onChange={(value) => setEndDate(value ? value.format('YYYY-MM-DD') : '')} />
        <Select
          aria-label={text('pages.org.dept.assignmentStatus', '재직 상태')}
          style={{ minWidth: 160, marginLeft: 8 }}
          value={endStatus}
          options={statuses.options}
          onChange={setEndStatus}
        />
      </Modal>
    </Drawer>
  );
};

export default PersonDrawer;
