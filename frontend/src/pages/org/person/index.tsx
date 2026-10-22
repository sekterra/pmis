import { PageContainer } from '@ant-design/pro-components';
import { history, useAccess, useIntl, useLocation } from '@umijs/max';
import { Alert, App, Button, Input, Select, Space, Table, TreeSelect } from 'antd';
import type { TableProps } from 'antd';
import React, { useEffect, useRef, useState } from 'react';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import type { MenuAction } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import { nestDepts } from '@/pages/org/dept/tree';
import { listCompanies } from '@/services/mdm/company';
import type { Company } from '@/services/mdm/companyRecord';
import type { SupportLanguage } from '@/services/mdm/data';
import { listDepartmentTree } from '@/services/mdm/dept';
import type { Dept } from '@/services/mdm/deptRecord';
import { fetchLanguages } from '@/services/mdm/i18n';
import { createPerson, deletePerson, getPerson, listPeople, restorePerson, updatePerson } from '@/services/mdm/people';
import type { Person, PersonCreateBody, PersonListItem, PersonListQuery, PersonUpdateBody } from '@/services/mdm/peopleRecord';
import PersonDrawer from './PersonDrawer';
import styles from './index.module.css';
import { personErrorText } from './rules';

function readPersonPk(search: string): number | undefined {
  const value = Number(new URLSearchParams(search).get('personPk'));
  return Number.isFinite(value) && value > 0 ? value : undefined;
}

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

const PersonPage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const location = useLocation();
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (menu: string, action: MenuAction) => access.can?.(menu, action) ?? false;
  const employment = useCodeOptions('EMPLOYMENT_TYPE');
  const statuses = useCodeOptions('ASSIGNMENT_STATUS');
  const positions = useCodeOptions('POSITION');
  const [languages, setLanguages] = useState<SupportLanguage[]>([]);
  const [companies, setCompanies] = useState<Company[]>([]);
  const [deptRows, setDeptRows] = useState<Dept[]>([]);
  const [keyword, setKeyword] = useState('');
  const [companyCode, setCompanyCode] = useState<string>();
  const [deptCode, setDeptCode] = useState<string>();
  const [employmentTypeCode, setEmploymentTypeCode] = useState<string>();
  const [assignmentStatusCode, setAssignmentStatusCode] = useState<string>();
  const [useYn, setUseYn] = useState<string>();
  const [applied, setApplied] = useState<PersonListQuery>({});
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<string[]>();
  const [rows, setRows] = useState<PersonListItem[]>([]);
  const [total, setTotal] = useState(0);
  const [notice, setNotice] = useState<string>();
  const [open, setOpen] = useState(false);
  const [person, setPerson] = useState<Person>();
  const [tab, setTab] = useState('basic');
  const [saving, setSaving] = useState(false);
  const [reload, setReload] = useState(0);
  const failRef = useRef<(error: unknown) => void>(() => undefined);
  failRef.current = (error: unknown) => {
    if (error instanceof ApiError) {
      setNotice(personErrorText(error, text));
    }
  };

  useEffect(() => {
    fetchLanguages()
      .then(setLanguages)
      .catch((error: unknown) => failRef.current(error));
    listCompanies({ useYn: 'Y', page: 1, size: 200 })
      .then((result) => setCompanies(result.items))
      .catch((error: unknown) => failRef.current(error));
  }, []);

  useEffect(() => {
    if (!companyCode) {
      setDeptRows([]);
      return;
    }
    let alive = true;
    listDepartmentTree({ companyCode, includeClosed: false })
      .then((items) => {
        if (alive) {
          setDeptRows(items);
        }
      })
      .catch((error: unknown) => {
        if (alive) {
          failRef.current(error);
        }
      });
    return () => {
      alive = false;
    };
  }, [companyCode]);

  useEffect(() => {
    let alive = true;
    listPeople({ ...applied, page, size: 20, sort })
      .then((result) => {
        if (alive) {
          setRows(result.items);
          setTotal(result.total);
        }
      })
      .catch((error: unknown) => {
        if (alive) {
          failRef.current(error);
        }
      });
    return () => {
      alive = false;
    };
  }, [applied, page, sort, reload]);

  const showPerson = (personPk: number, nextTab = 'basic') => {
    getPerson(personPk)
      .then((item) => {
        setPerson(item);
        setTab(nextTab);
        setOpen(true);
        history.replace({ pathname: '/org/person', search: `?personPk=${personPk}` });
      })
      .catch((error: unknown) => failRef.current(error));
  };

  useEffect(() => {
    const personPk = readPersonPk(location.search);
    if (personPk) {
      showPerson(personPk);
    }
    // 처음 주소의 personPk 만 연다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const labelOf = (code: string | null | undefined) =>
    code ? (positions.options.find((item) => item.value === code)?.label ?? code) : '-';
  const statusOf = (code: string | null | undefined) =>
    code ? (statuses.options.find((item) => item.value === code)?.label ?? code) : '-';

  const columns: TableProps<PersonListItem>['columns'] = [
    { title: text('pages.org.name', '이름'), dataIndex: 'personName', sorter: true },
    { title: text('pages.org.person.email', '이메일'), dataIndex: 'email', sorter: true, render: (value) => value ?? '-' },
    { title: text('pages.org.person.mobile', '휴대전화'), dataIndex: 'mobilePhoneNo', render: (value) => value ?? '-' },
    { title: text('pages.org.dept.company', '회사'), render: (_, row) => row.primaryAssignment?.companyName ?? '-' },
    { title: text('pages.org.dept.name', '부서'), render: (_, row) => row.primaryAssignment?.deptName ?? '-' },
    { title: text('pages.org.dept.position', '직위'), render: (_, row) => labelOf(row.primaryAssignment?.positionCode) },
    { title: text('pages.org.person.employeeNo', '사원번호'), render: (_, row) => row.primaryAssignment?.employeeNo ?? '-' },
    { title: text('pages.org.dept.assignmentStatus', '재직 상태'), render: (_, row) => statusOf(row.primaryAssignment?.assignmentStatusCode) },
    { title: text('pages.org.person.photo', '사진'), render: (_, row) => (row.hasProfileImage ? text('pages.org.person.hasPhoto', '있음') : text('pages.org.person.noPhoto', '사진 없음')) },
    { title: text('pages.org.use', '사용 여부'), render: (_, row) => (row.useYn === 'Y' ? text('pages.org.inUse', '사용') : text('pages.org.stopped', '중지')) },
    {
      title: text('pages.org.actions', '작업'),
      render: (_, row) => (
        <Button
          size="small"
          onClick={(event) => {
            event.stopPropagation();
            showPerson(row.personPk);
          }}
        >
          {text('pages.org.person.detail', '상세')}
        </Button>
      ),
    },
  ];

  const save = async (body: PersonCreateBody | PersonUpdateBody) => {
    setSaving(true);
    try {
      if (person) {
        setPerson(await updatePerson(person.personPk, body as PersonUpdateBody));
      } else {
        const created = await createPerson(body as PersonCreateBody);
        setPerson(created);
        setTab('assignments');
        history.replace({ pathname: '/org/person', search: `?personPk=${created.personPk}` });
      }
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const deptPk = deptRows.find((row) => row.deptCode === deptCode)?.deptPk;

  return (
    <PageContainer title={text('pages.org.person.title', '사람')}>
      {notice ? <Alert type="error" showIcon title={notice} style={{ marginBottom: 16 }} /> : null}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input aria-label={text('pages.org.keyword', '검색어')} placeholder={text('pages.org.keyword', '검색어')} value={keyword} onChange={(event) => setKeyword(event.target.value)} />
        <Select
          aria-label={text('pages.org.dept.company', '회사')}
          allowClear
          placeholder={text('pages.org.dept.company', '회사')}
          style={{ minWidth: 160 }}
          value={companyCode}
          options={companies.map((company) => ({ value: company.companyCode, label: `${company.companyCode} ${company.companyName}` }))}
          onChange={(value) => {
            setCompanyCode(value);
            setDeptCode(undefined);
          }}
        />
        <TreeSelect
          aria-label={text('pages.org.dept.name', '부서')}
          allowClear
          disabled={!companyCode}
          placeholder={text('pages.org.dept.name', '부서')}
          style={{ minWidth: 180 }}
          value={deptCode}
          treeData={deptOptions(deptRows)}
          onChange={(value) => setDeptCode(value)}
        />
        <Select
          aria-label={text('pages.org.person.employmentType', '고용 유형')}
          allowClear
          placeholder={text('pages.org.person.employmentType', '고용 유형')}
          style={{ minWidth: 140 }}
          value={employmentTypeCode}
          options={employment.options}
          onChange={setEmploymentTypeCode}
        />
        <Select
          aria-label={text('pages.org.dept.assignmentStatus', '재직 상태')}
          allowClear
          placeholder={text('pages.org.dept.assignmentStatus', '재직 상태')}
          style={{ minWidth: 140 }}
          value={assignmentStatusCode}
          options={statuses.options}
          onChange={setAssignmentStatusCode}
        />
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
          onChange={setUseYn}
        />
        <Button
          type="primary"
          onClick={() => {
            setPage(1);
            setApplied({ keyword, companyCode, deptPk, employmentTypeCode, assignmentStatusCode, useYn });
          }}
        >
          {text('pages.org.search', '조회')}
        </Button>
        <Button
          onClick={() => {
            setKeyword('');
            setCompanyCode(undefined);
            setDeptCode(undefined);
            setEmploymentTypeCode(undefined);
            setAssignmentStatusCode(undefined);
            setUseYn(undefined);
            setPage(1);
            setSort(undefined);
            setApplied({});
          }}
        >
          {text('pages.org.reset', '초기화')}
        </Button>
      </Space>
      {can('PERSON', 'CREATE') ? (
        <Button
          style={{ marginBottom: 16 }}
          onClick={() => {
            setPerson(undefined);
            setTab('basic');
            setOpen(true);
          }}
        >
          {text('pages.org.person.create', '사람 등록')}
        </Button>
      ) : null}
      <Table<PersonListItem>
        rowKey="personPk"
        columns={columns}
        dataSource={rows}
        rowClassName={(row) => (row.useYn === 'Y' ? '' : styles.inactive)}
        pagination={{ current: page, pageSize: 20, total, showSizeChanger: false }}
        onRow={(row) => ({ onClick: () => showPerson(row.personPk) })}
        onChange={(pagination, _filters, sorter) => {
          const column = Array.isArray(sorter) ? sorter[0] : sorter;
          const field = column?.field === 'personName' || column?.field === 'email' ? String(column.field) : undefined;
          const order = column?.order === 'ascend' ? 'asc' : column?.order === 'descend' ? 'desc' : undefined;
          setSort(field && order ? [`${field},${order}`] : undefined);
          setPage(pagination.current ?? 1);
        }}
      />
      <PersonDrawer
        open={open}
        person={person}
        tab={tab}
        saving={saving}
        languages={languages}
        companies={companies}
        canCreate={can('PERSON', 'CREATE')}
        canUpdate={can('PERSON', 'UPDATE')}
        canDelete={can('PERSON', 'DELETE')}
        canUpload={can('FILE', 'CREATE')}
        onTab={setTab}
        onClose={() => {
          setOpen(false);
          history.replace({ pathname: '/org/person', search: '' });
        }}
        onSubmit={save}
        onNotice={setNotice}
        onChanged={() => {
          setReload((value) => value + 1);
          if (person) {
            showPerson(person.personPk, tab);
          }
        }}
        onDelete={() => {
          if (!person) {
            return;
          }
          modal.confirm({
            title: text('pages.org.person.deleteTitle', '사람을 사용 중지할까요?'),
            content: text('pages.org.person.deleteHint', '삭제하면 사용 중지됩니다. 데이터는 지워지지 않습니다.'),
            okText: text('pages.org.company.deleteOk', '삭제 확인'),
            onOk: () =>
              deletePerson(person.personPk)
                .then(() => {
                  setOpen(false);
                  setReload((value) => value + 1);
                })
                .catch((error: unknown) => failRef.current(error)),
          });
        }}
        onRestore={() => {
          if (!person) {
            return;
          }
          restorePerson(person.personPk)
            .then((item) => {
              setPerson(item);
              setReload((value) => value + 1);
            })
            .catch((error: unknown) => failRef.current(error));
        }}
      />
    </PageContainer>
  );
};

export default PersonPage;
