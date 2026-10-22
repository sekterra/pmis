import { PageContainer } from '@ant-design/pro-components';
import { history, useAccess, useIntl, useLocation } from '@umijs/max';
import { Alert, App, Button, Checkbox, DatePicker, Select, Space, Table, Tabs, Tree } from 'antd';
import type { DataNode } from 'antd/es/tree';
import type { TableProps } from 'antd';
import dayjs from 'dayjs';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { localToday } from '@/core/codes/options';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import type { MenuAction } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import type { Company } from '@/services/mdm/companyRecord';
import { listCompanies } from '@/services/mdm/company';
import {
  closeDepartment,
  createDepartment,
  getDepartment,
  listDepartmentHistory,
  listDepartmentTree,
  restoreDepartment,
  updateDepartment,
} from '@/services/mdm/dept';
import type { Dept, DeptCreateBody, DeptHistory, DeptUpdateBody } from '@/services/mdm/deptRecord';
import type { SupportLanguage } from '@/services/mdm/data';
import { fetchLanguages } from '@/services/mdm/i18n';
import { listPeople } from '@/services/mdm/people';
import type { PersonListItem } from '@/services/mdm/peopleRecord';
import DeptFormDrawer from './DeptFormDrawer';
import styles from './index.module.css';
import { deptErrorText } from './rules';
import { defaultExpandedKeys, deptNodeLabel, nestDepts, type DeptNode } from './tree';

function toTreeData(nodes: DeptNode[], closedText: string): DataNode[] {
  return nodes.map((node) => ({
    key: String(node.deptPk),
    title: (
      <span className={node.useYn === 'Y' ? undefined : styles.closed}>{deptNodeLabel(node, closedText)}</span>
    ),
    children: toTreeData(node.children, closedText),
  }));
}

const CHANGE_LABELS: Record<string, string> = {
  CREATE: '생성',
  RENAME: '개명',
  MOVE: '이동',
  CLOSE: '종료',
  REOPEN: '재개',
};

function readQuery(search: string): { company: string; dept?: number } {
  const params = new URLSearchParams(search);
  const dept = Number(params.get('dept'));
  return {
    company: params.get('company') ?? '',
    dept: Number.isFinite(dept) && dept > 0 ? dept : undefined,
  };
}

const DeptPage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const location = useLocation();
  const initial = readQuery(location.search);
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (action: MenuAction) => access.can?.('DEPT', action) ?? false;
  const canPeople = access.can?.('PERSON', 'READ') ?? false;
  const types = useCodeOptions('DEPT_TYPE');
  const positions = useCodeOptions('POSITION');
  const statuses = useCodeOptions('ASSIGNMENT_STATUS');
  const [languages, setLanguages] = useState<SupportLanguage[]>([]);
  const [companies, setCompanies] = useState<Company[]>([]);
  const [companyCode, setCompanyCode] = useState(initial.company);
  const [asOfDraft, setAsOfDraft] = useState(localToday());
  const [asOf, setAsOf] = useState(localToday());
  const [includeDraft, setIncludeDraft] = useState(false);
  const [includeClosed, setIncludeClosed] = useState(false);
  const [rows, setRows] = useState<Dept[]>([]);
  const [selectedPk, setSelectedPk] = useState<number | undefined>(initial.dept);
  const [detail, setDetail] = useState<Dept>();
  const [expanded, setExpanded] = useState<string[]>([]);
  const [tab, setTab] = useState('basic');
  const [historyRows, setHistoryRows] = useState<DeptHistory[]>([]);
  const [historyTotal, setHistoryTotal] = useState(0);
  const [historyPage, setHistoryPage] = useState(1);
  const [peopleRows, setPeopleRows] = useState<PersonListItem[]>([]);
  const [peopleTotal, setPeopleTotal] = useState(0);
  const [peoplePage, setPeoplePage] = useState(1);
  const [notice, setNotice] = useState<string>();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Dept>();
  const [presetParent, setPresetParent] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [reload, setReload] = useState(0);
  const selectedRef = useRef(selectedPk);
  const historyKey = useRef('');
  const peopleKey = useRef('');
  const failRef = useRef<(error: unknown) => void>(() => undefined);
  selectedRef.current = selectedPk;

  const writeQuery = useCallback(
    (company: string, dept?: number) => {
      const params = new URLSearchParams();
      if (company) {
        params.set('company', company);
      }
      if (dept) {
        params.set('dept', String(dept));
      }
      const search = params.toString();
      history.replace({ pathname: location.pathname, search: search ? `?${search}` : '' });
    },
    [location.pathname],
  );

  const fail = useCallback((error: unknown) => {
    const api = error instanceof ApiError ? error : new ApiError('');
    setNotice(deptErrorText(api, (id, fallback) => intl.formatMessage({ id, defaultMessage: fallback })));
  }, [intl]);
  failRef.current = fail;

  useEffect(() => {
    let alive = true;
    fetchLanguages()
      .then((items) => {
        if (alive) {
          setLanguages(items);
        }
      })
      .catch(() => undefined);
    listCompanies({ useYn: 'Y', page: 1, size: 200 })
      .then((result) => {
        if (!alive) {
          return;
        }
        setCompanies(result.items);
        setCompanyCode((current) => current || result.items[0]?.companyCode || '');
      })
      .catch((error: unknown) => failRef.current(error));
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    if (!companyCode) {
      return;
    }
    writeQuery(companyCode, selectedRef.current);
    let alive = true;
    listDepartmentTree({ companyCode, asOfDate: asOf, includeClosed })
      .then((items) => {
        if (!alive) {
          return;
        }
        setRows(items);
        const current = selectedRef.current;
        if (current && !items.some((item) => item.deptPk === current)) {
          setSelectedPk(undefined);
          setDetail(undefined);
          writeQuery(companyCode, undefined);
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
  }, [companyCode, asOf, includeClosed, reload, writeQuery]);

  useEffect(() => {
    const next = defaultExpandedKeys(rows, selectedPk);
    setExpanded((current) => (current.join() === next.join() ? current : next));
  }, [rows, selectedPk]);

  useEffect(() => {
    if (!selectedPk) {
      setDetail(undefined);
      return;
    }
    let alive = true;
    getDepartment(selectedPk)
      .then((item) => {
        if (alive) {
          setDetail(item);
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
  }, [selectedPk, reload]);

  useEffect(() => {
    if (tab !== 'history' || !selectedPk) {
      return;
    }
    const key = `${selectedPk}:${historyPage}:${reload}`;
    if (historyKey.current === key) {
      return;
    }
    historyKey.current = key;
    listDepartmentHistory(selectedPk, { page: historyPage, size: 20 })
      .then((result) => {
        setHistoryRows(result.items);
        setHistoryTotal(result.total);
      })
      .catch((error: unknown) => failRef.current(error));
  }, [tab, selectedPk, historyPage, reload]);

  useEffect(() => {
    if (tab !== 'people' || !selectedPk || !canPeople) {
      return;
    }
    const key = `${selectedPk}:${peoplePage}:${reload}`;
    if (peopleKey.current === key) {
      return;
    }
    peopleKey.current = key;
    listPeople({ deptPk: selectedPk, page: peoplePage, size: 20 })
      .then((result) => {
        setPeopleRows(result.items);
        setPeopleTotal(result.total);
      })
      .catch((error: unknown) => failRef.current(error));
  }, [tab, selectedPk, peoplePage, canPeople, reload]);

  const selectDept = (deptPk?: number) => {
    setSelectedPk(deptPk);
    setTab('basic');
    setHistoryPage(1);
    setPeoplePage(1);
    historyKey.current = '';
    peopleKey.current = '';
    writeQuery(companyCode, deptPk);
  };

  const save = async (body: DeptCreateBody | DeptUpdateBody) => {
    setSaving(true);
    try {
      if (editing) {
        await updateDepartment(editing.deptPk, body as DeptUpdateBody);
      } else {
        await createDepartment(body as DeptCreateBody);
      }
      setOpen(false);
      setEditing(undefined);
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const askClose = (deptPk: number) => {
    modal.confirm({
      title: text('pages.org.dept.closeTitle', '부서를 종료할까요?'),
      content: text('pages.org.dept.closeHint', '종료일이 오늘로 설정되고 이력이 남습니다.'),
      okText: text('pages.org.dept.closeOk', '종료 확인'),
      onOk: () =>
        closeDepartment(deptPk)
          .then(() => setReload((value) => value + 1))
          .catch(fail),
    });
  };

  const labelOf = (options: { value: string; label: string }[], code: string | null | undefined) =>
    options.find((item) => item.value === code)?.label ?? code ?? text('pages.org.dept.none', '없음');

  const selected = rows.find((row) => row.deptPk === selectedPk);
  const closedText = text('pages.org.dept.closed', '종료');
  const treeData = toTreeData(nestDepts(rows), closedText);

  const historyColumns: TableProps<DeptHistory>['columns'] = [
    {
      title: text('pages.org.dept.historyType', '변경 유형'),
      dataIndex: 'changeTypeCode',
      render: (value: string) =>
        CHANGE_LABELS[value] ? text(`pages.org.dept.change.${value}`, CHANGE_LABELS[value]) : value,
    },
    { title: text('pages.org.dept.effectiveDate', '적용일'), dataIndex: 'effectiveDate' },
    {
      title: text('pages.org.dept.historyNames', '이름'),
      render: (_, row) => `${row.beforeDeptName ?? '-'} → ${row.afterDeptName ?? '-'}`,
    },
    {
      title: text('pages.org.dept.historyParents', '상위 부서'),
      render: (_, row) => `${row.beforeParentDeptPk ?? '-'} → ${row.afterParentDeptPk ?? '-'}`,
    },
    { title: text('pages.org.dept.historyReason', '사유'), dataIndex: 'changeReason' },
    { title: text('pages.org.dept.historyAt', '변경 시각'), dataIndex: 'changeDatetime' },
  ];

  const peopleColumns: TableProps<PersonListItem>['columns'] = [
    { title: text('pages.org.dept.personName', '이름'), dataIndex: 'personName' },
    {
      title: text('pages.org.dept.position', '직위'),
      render: (_, row) => labelOf(positions.options, row.primaryAssignment?.positionCode),
    },
    {
      title: text('pages.org.dept.assignmentStatus', '재직 상태'),
      render: (_, row) => labelOf(statuses.options, row.primaryAssignment?.assignmentStatusCode),
    },
  ];

  return (
    <PageContainer title={text('pages.org.dept.title', '부서')}>
      {notice ? <Alert type="error" showIcon title={notice} style={{ marginBottom: 16 }} /> : null}
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          aria-label={text('pages.org.dept.company', '회사')}
          placeholder={text('pages.org.dept.company', '회사')}
          style={{ minWidth: 180 }}
          options={companies.map((company) => ({
            value: company.companyCode,
            label: `${company.companyCode} ${company.companyName}`,
          }))}
          value={companyCode || undefined}
          onChange={(value: string) => {
            setCompanyCode(value);
            setSelectedPk(undefined);
            setDetail(undefined);
            writeQuery(value, undefined);
          }}
        />
        <DatePicker
          aria-label={text('pages.org.dept.asOf', '기준일')}
          value={asOfDraft ? dayjs(asOfDraft) : null}
          onChange={(value) => setAsOfDraft(value ? value.format('YYYY-MM-DD') : '')}
        />
        <Checkbox checked={includeDraft} onChange={(event) => setIncludeDraft(event.target.checked)}>
          {text('pages.org.dept.includeClosed', '종료된 부서 포함')}
        </Checkbox>
        <Button
          type="primary"
          onClick={() => {
            setAsOf(asOfDraft);
            setIncludeClosed(includeDraft);
          }}
        >
          {text('pages.org.search', '조회')}
        </Button>
      </Space>
      {companies.length === 0 ? (
        <p>{text('pages.org.dept.noCompany', '회사를 먼저 등록하세요.')}</p>
      ) : (
        <div className={styles.split}>
          <div className={styles.treePane}>
            {rows.length === 0 ? <p>{text('pages.org.dept.empty', '최상위 부서를 등록하세요.')}</p> : null}
            <Tree
              blockNode
              motion={false}
              virtual={false}
              treeData={treeData}
              expandedKeys={expanded}
              selectedKeys={selectedPk ? [String(selectedPk)] : []}
              onExpand={(keys) => setExpanded(keys.map(String))}
              onSelect={(keys) => selectDept(keys[0] ? Number(keys[0]) : undefined)}
            />
            {can('CREATE') ? (
              <Button
                style={{ marginTop: 12 }}
                onClick={() => {
                  setEditing(undefined);
                  setPresetParent(null);
                  setOpen(true);
                }}
              >
                {text('pages.org.dept.addRoot', '최상위 부서')}
              </Button>
            ) : null}
          </div>
          <div className={styles.detail}>
            {!selected ? (
              <p>{text('pages.org.dept.pick', '부서를 선택하세요.')}</p>
            ) : (
              <>
                <Space wrap style={{ marginBottom: 12 }}>
                  <strong>{selected.deptName}</strong>
                  {selected.useYn === 'Y' && can('CREATE') ? (
                    <Button
                      onClick={() => {
                        setEditing(undefined);
                        setPresetParent(selected.deptCode);
                        setOpen(true);
                      }}
                    >
                      {text('pages.org.dept.addChild', '하위 부서 추가')}
                    </Button>
                  ) : null}
                  {can('UPDATE') && selected.useYn === 'Y' ? (
                    <Button
                      onClick={() => {
                        if (!detail) {
                          return;
                        }
                        setEditing(detail);
                        setPresetParent(null);
                        setOpen(true);
                      }}
                    >
                      {text('pages.org.edit', '수정')}
                    </Button>
                  ) : null}
                  {can('DELETE') && selected.useYn === 'Y' ? (
                    <Button onClick={() => askClose(selected.deptPk)}>{text('pages.org.dept.close', '종료')}</Button>
                  ) : null}
                  {can('UPDATE') && selected.useYn !== 'Y' ? (
                    <Button
                      onClick={() =>
                        restoreDepartment(selected.deptPk)
                          .then(() => setReload((value) => value + 1))
                          .catch(fail)
                      }
                    >
                      {text('pages.org.dept.reopen', '재개')}
                    </Button>
                  ) : null}
                </Space>
                <Tabs
                  activeKey={tab}
                  onChange={setTab}
                  items={[
                    {
                      key: 'basic',
                      label: text('pages.org.dept.tabBasic', '기본 정보'),
                      children: (
                        <dl>
                          <div>
                            {text('pages.org.dept.code', '부서 코드')}: {detail?.deptCode ?? selected.deptCode}
                          </div>
                          <div>
                            {text('pages.org.dept.company', '회사')}: {detail?.companyCode ?? selected.companyCode}
                          </div>
                          <div>
                            {text('pages.org.dept.type', '부서 유형')}: {labelOf(types.options, detail?.deptTypeCode ?? selected.deptTypeCode)}
                          </div>
                          <div>
                            {text('pages.org.dept.parent', '상위 부서')}: {detail?.parentDeptCode ?? selected.parentDeptCode ?? text('pages.org.dept.none', '없음')}
                          </div>
                          <div>
                            {text('pages.org.dept.period', '유효기간')}: {detail?.validFromDate ?? selected.validFromDate ?? ''} ~{' '}
                            {detail?.validToDate ?? selected.validToDate ?? text('pages.org.dept.none', '없음')}
                          </div>
                          <div>
                            {text('pages.org.dept.level', '레벨')}: {detail?.levelNo ?? selected.levelNo}
                          </div>
                          <div>
                            {text('pages.org.dept.sortOrder', '정렬 순서')}: {detail?.sortOrder ?? selected.sortOrder ?? ''}
                          </div>
                          <div>
                            {text('pages.org.use', '사용 여부')}:{' '}
                            {(detail?.useYn ?? selected.useYn) === 'Y'
                              ? text('pages.org.inUse', '사용')
                              : text('pages.org.dept.closed', '종료')}
                          </div>
                          <div>
                            {text('pages.org.translations', '이름 번역')}:{' '}
                            {(detail?.translations ?? []).map((item) => `${item.supportLanguageCode} ${item.text}`).join(' / ') ||
                              text('pages.org.dept.none', '없음')}
                          </div>
                        </dl>
                      ),
                    },
                    {
                      key: 'history',
                      label: text('pages.org.dept.tabHistory', '변경 이력'),
                      children: (
                        <>
                          <Table<DeptHistory>
                            rowKey="historyPk"
                            columns={historyColumns}
                            dataSource={historyRows}
                            pagination={{ current: historyPage, pageSize: 20, total: historyTotal, showSizeChanger: false }}
                            onChange={(pagination) => setHistoryPage(pagination.current ?? 1)}
                          />
                          <p>{text('pages.org.dept.historyParentHint', '상위 부서는 번호만 표시됩니다.')}</p>
                        </>
                      ),
                    },
                    {
                      key: 'people',
                      label: text('pages.org.dept.tabPeople', '소속 인원'),
                      children: canPeople ? (
                        <>
                          <p>{text('pages.org.dept.peopleHint', '주 소속 기준입니다.')}</p>
                          <Table<PersonListItem>
                            rowKey="personPk"
                            columns={peopleColumns}
                            dataSource={peopleRows}
                            pagination={{ current: peoplePage, pageSize: 20, total: peopleTotal, showSizeChanger: false }}
                            onChange={(pagination) => setPeoplePage(pagination.current ?? 1)}
                          />
                        </>
                      ) : (
                        <p>{text('pages.org.dept.peopleForbidden', '조회 권한이 없습니다.')}</p>
                      ),
                    },
                  ]}
                />
              </>
            )}
          </div>
        </div>
      )}
      <DeptFormDrawer
        open={open}
        saving={saving}
        companyCode={companyCode}
        dept={editing}
        parentDeptCode={presetParent}
        rows={rows}
        languages={languages}
        onClose={() => {
          setOpen(false);
          setEditing(undefined);
        }}
        onSubmit={save}
        onNotice={setNotice}
      />
    </PageContainer>
  );
};

export default DeptPage;
