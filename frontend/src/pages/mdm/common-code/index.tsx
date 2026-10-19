import { PageContainer } from '@ant-design/pro-components';
import { useAccess, useIntl, useSearchParams } from '@umijs/max';
import { Alert, App, Button, Descriptions, Empty, Input, Select, Space, Table } from 'antd';
import type { TableProps } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/core/request/errors';
import type { MenuAction } from '@/core/menu/types';
import type { CodeGroup, CommonCode } from '@/services/mdm/code';
import type { SupportLanguage } from '@/services/mdm/data';
import {
  createCodeGroup,
  createCommonCode,
  deleteCodeGroup,
  deleteCommonCode,
  getCodeGroup,
  listCodeGroups,
  listCommonCodes,
  restoreCodeGroup,
  restoreCommonCode,
  updateCodeGroup,
  updateCommonCode,
} from '@/services/mdm/codes';
import { fetchLanguages } from '@/services/mdm/i18n';
import CodeFormDrawer from './CodeFormDrawer';
import GroupFormDrawer from './GroupFormDrawer';
import HistoryDrawer from './HistoryDrawer';
import styles from './index.module.css';
import { codeErrorText, inUse, toCodeTree } from './rules';

type Filters = {
  keyword?: string;
  useYn?: string;
  solutionCode?: string;
};

function sortParams(
  sorter: { field?: unknown; order?: 'ascend' | 'descend' | null } | Array<{ field?: unknown; order?: 'ascend' | 'descend' | null }>,
): string[] {
  const list = Array.isArray(sorter) ? sorter : [sorter];
  return list.flatMap((item) => {
    const field = typeof item.field === 'string' ? item.field : '';
    if (!field || !item.order) {
      return [];
    }
    return [`${field},${item.order === 'descend' ? 'desc' : 'asc'}`];
  });
}

const CommonCodePage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (action: MenuAction) => access.can?.('COMMON_CODE', action) ?? false;
  const [params, setParams] = useSearchParams();
  const selectedCode = params.get('group') ?? undefined;

  const [languages, setLanguages] = useState<SupportLanguage[]>([]);
  const [notice, setNotice] = useState<string>();
  const [groupFilters, setGroupFilters] = useState<Filters>({});
  const [groupDraft, setGroupDraft] = useState<Filters>({});
  const [groups, setGroups] = useState<CodeGroup[]>([]);
  const [groupTotal, setGroupTotal] = useState(0);
  const [groupPage, setGroupPage] = useState(1);
  const [groupSort, setGroupSort] = useState<string[]>([]);
  const [groupLoading, setGroupLoading] = useState(false);
  const [selected, setSelected] = useState<CodeGroup>();
  const [codeFilters, setCodeFilters] = useState<Filters>({});
  const [codeDraft, setCodeDraft] = useState<Filters>({});
  const [codes, setCodes] = useState<CommonCode[]>([]);
  const [codeTotal, setCodeTotal] = useState(0);
  const [codePage, setCodePage] = useState(1);
  const [codeSort, setCodeSort] = useState<string[]>([]);
  const [codeLoading, setCodeLoading] = useState(false);
  const [groupOpen, setGroupOpen] = useState(false);
  const [editingGroup, setEditingGroup] = useState<CodeGroup>();
  const [codeOpen, setCodeOpen] = useState(false);
  const [editingCode, setEditingCode] = useState<CommonCode>();
  const [historyPk, setHistoryPk] = useState<number>();
  const [saving, setSaving] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    fetchLanguages()
      .then((rows) => {
        if (alive) {
          setLanguages(rows);
        }
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, []);

  const fail = useCallback((error: unknown) => {
    const api = error instanceof ApiError ? error : new ApiError('');
    if (api.errorCode === 'AUTH.FORBIDDEN') {
      setNotice(codeErrorText({ message: '', errorCode: 'AUTH.FORBIDDEN' }));
      return;
    }
    setNotice(codeErrorText(api));
  }, []);

  useEffect(() => {
    let alive = true;
    setGroupLoading(true);
    listCodeGroups({
      page: groupPage,
      size: 20,
      keyword: groupFilters.keyword,
      solutionCode: groupFilters.solutionCode,
      useYn: groupFilters.useYn,
      sort: groupSort,
    })
      .then((result) => {
        if (!alive) {
          return;
        }
        setGroups(result.items);
        setGroupTotal(result.total);
        setNotice(undefined);
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      })
      .finally(() => {
        if (alive) {
          setGroupLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [groupPage, groupFilters, groupSort, reload, fail]);

  useEffect(() => {
    if (!selectedCode) {
      setSelected(undefined);
      setCodes([]);
      return;
    }
    let alive = true;
    getCodeGroup(selectedCode)
      .then((group) => {
        if (alive) {
          setSelected(group);
        }
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      });
    setCodeLoading(true);
    listCommonCodes({
      groupCode: selectedCode,
      page: codePage,
      size: 20,
      keyword: codeFilters.keyword,
      useYn: codeFilters.useYn,
      sort: codeSort,
    })
      .then((result) => {
        if (!alive) {
          return;
        }
        setCodes(result.items);
        setCodeTotal(result.total);
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      })
      .finally(() => {
        if (alive) {
          setCodeLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [selectedCode, codePage, codeFilters, codeSort, reload, fail]);

  const chooseGroup = (groupCode: string) => {
    const next = new URLSearchParams(params);
    next.set('group', groupCode);
    setParams(next);
    setCodePage(1);
  };

  const askDisable = (title: string, onOk: () => Promise<void>) => {
    modal.confirm({
      title,
      content: text('pages.mdm.disableHint', '삭제하면 사용 중지(use_yn=N)됩니다. 데이터는 지워지지 않습니다.'),
      okText: text('pages.mdm.disable', '사용 중지'),
      cancelText: text('pages.mdm.cancel', '취소'),
      onOk,
    });
  };

  const groupColumns: TableProps<CodeGroup>['columns'] = [
    {
      title: text('pages.mdm.groupCode', '그룹 코드'),
      dataIndex: 'codeGroupCode',
      sorter: true,
    },
    {
      title: text('pages.mdm.name', '이름'),
      dataIndex: 'codeGroupName',
      sorter: true,
    },
    {
      title: text('pages.mdm.solution', '솔루션 코드'),
      dataIndex: 'solutionCode',
      sorter: true,
      render: (value: string | null) => value || '-',
    },
    {
      title: text('pages.mdm.sortOrder', '정렬 순서'),
      dataIndex: 'sortOrder',
      sorter: true,
      render: (value: number | null) => (value == null ? '-' : value),
    },
    {
      title: text('pages.mdm.use', '사용 여부'),
      dataIndex: 'useYn',
    },
    {
      title: text('pages.mdm.actions', '작업'),
      render: (_, record) => (
        <Space>
          {can('UPDATE') && inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                setEditingGroup(record);
                setGroupOpen(true);
              }}
            >
              {text('pages.mdm.edit', '수정')}
            </Button>
          ) : null}
          {can('DELETE') && inUse(record.useYn) ? (
            <Button
              type="link"
              danger
              onClick={() =>
                askDisable(text('pages.mdm.disableGroup', '그룹을 사용 중지할까요?'), async () => {
                  try {
                    await deleteCodeGroup(record.codeGroupCode);
                    setReload((value) => value + 1);
                  } catch (error) {
                    fail(error);
                  }
                })
              }
            >
              {text('pages.mdm.disable', '사용 중지')}
            </Button>
          ) : null}
          {can('UPDATE') && !inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                restoreCodeGroup(record.codeGroupCode)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail);
              }}
            >
              {text('pages.mdm.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const codeColumns: TableProps<CommonCode>['columns'] = [
    {
      title: text('pages.mdm.codeValue', '코드 값'),
      dataIndex: 'codeValue',
      sorter: true,
    },
    {
      title: text('pages.mdm.name', '이름'),
      dataIndex: 'codeName',
      sorter: true,
    },
    {
      title: text('pages.mdm.sortOrder', '정렬 순서'),
      dataIndex: 'sortOrder',
      sorter: true,
      render: (value: number | null) => (value == null ? '-' : value),
    },
    {
      title: text('pages.mdm.use', '사용 여부'),
      dataIndex: 'useYn',
    },
    {
      title: text('pages.mdm.actions', '작업'),
      render: (_, record) => (
        <Space>
          {can('UPDATE') && inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                setEditingCode(record);
                setCodeOpen(true);
              }}
            >
              {text('pages.mdm.edit', '수정')}
            </Button>
          ) : null}
          {can('DELETE') && inUse(record.useYn) ? (
            <Button
              type="link"
              danger
              onClick={() =>
                askDisable(text('pages.mdm.disableCode', '코드를 사용 중지할까요?'), async () => {
                  try {
                    await deleteCommonCode(record.codePk);
                    setReload((value) => value + 1);
                  } catch (error) {
                    fail(error);
                  }
                })
              }
            >
              {text('pages.mdm.disable', '사용 중지')}
            </Button>
          ) : null}
          {can('UPDATE') && !inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                restoreCommonCode(record.codePk)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail);
              }}
            >
              {text('pages.mdm.restore', '복구')}
            </Button>
          ) : null}
          {can('READ') ? (
            <Button type="link" onClick={() => setHistoryPk(record.codePk)}>
              {text('pages.mdm.history', '변경 이력')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const codeRows = selected?.hierarchy ? toCodeTree(codes) : codes;

  return (
    <PageContainer title={text('pages.mdm.title', '공통코드')}>
      {notice ? <Alert type="error" showIcon title={notice} /> : null}
      <div className={styles.layout}>
        <section className={`${styles.panel} ${styles.groups}`}>
          <Space orientation="vertical" style={{ width: '100%' }}>
            <Input
              allowClear
              placeholder={text('pages.mdm.keyword', '검색')}
              value={groupDraft.keyword}
              onChange={(event) =>
                setGroupDraft((current) => ({ ...current, keyword: event.target.value }))
              }
            />
            <Input
              allowClear
              placeholder={text('pages.mdm.solution', '솔루션 코드')}
              value={groupDraft.solutionCode}
              onChange={(event) =>
                setGroupDraft((current) => ({ ...current, solutionCode: event.target.value }))
              }
            />
            <Space>
              <Select
                allowClear
                placeholder={text('pages.mdm.use', '사용 여부')}
                style={{ width: 140 }}
                value={groupDraft.useYn}
                onChange={(value) => setGroupDraft((current) => ({ ...current, useYn: value }))}
                options={[
                  { value: 'Y', label: 'Y' },
                  { value: 'N', label: 'N' },
                ]}
              />
              <Button
                onClick={() => {
                  setGroupPage(1);
                  setGroupFilters(groupDraft);
                }}
              >
                {text('pages.mdm.search', '조회')}
              </Button>
              {can('CREATE') ? (
                <Button
                  type="primary"
                  onClick={() => {
                    setEditingGroup(undefined);
                    setGroupOpen(true);
                  }}
                >
                  {text('pages.mdm.createGroup', '그룹 등록')}
                </Button>
              ) : null}
            </Space>
            <Table<CodeGroup>
              rowKey="codeGroupCode"
              loading={groupLoading}
              columns={groupColumns}
              dataSource={groups}
              rowClassName={(record) => (inUse(record.useYn) ? '' : styles.inactive)}
              rowSelection={{
                type: 'radio',
                selectedRowKeys: selectedCode ? [selectedCode] : [],
                onChange: (keys) => {
                  const key = keys[0];
                  if (typeof key === 'string') {
                    chooseGroup(key);
                  }
                },
              }}
              onRow={(record) => ({
                onClick: () => chooseGroup(record.codeGroupCode),
              })}
              onChange={(pagination, _filters, sorter) => {
                setGroupPage(pagination.current ?? 1);
                setGroupSort(sortParams(sorter));
              }}
              pagination={{
                current: groupPage,
                pageSize: 20,
                total: groupTotal,
                showSizeChanger: false,
              }}
            />
          </Space>
        </section>
        <section className={`${styles.panel} ${styles.codes}`}>
          {selected ? (
            <Space orientation="vertical" style={{ width: '100%' }}>
              <Descriptions
                column={2}
                items={[
                  { key: 'code', label: text('pages.mdm.groupCode', '그룹 코드'), children: selected.codeGroupCode },
                  { key: 'name', label: text('pages.mdm.name', '이름'), children: selected.codeGroupName },
                  {
                    key: 'hierarchy',
                    label: text('pages.mdm.hierarchy', '계층형'),
                    children: selected.hierarchy
                      ? text('pages.mdm.yes', '예')
                      : text('pages.mdm.no', '아니오'),
                  },
                  {
                    key: 'length',
                    label: text('pages.mdm.valueLength', '코드 값 길이'),
                    children: selected.codeValueLength ?? '-',
                  },
                ]}
              />
              <Space>
                <Input
                  allowClear
                  placeholder={text('pages.mdm.keyword', '검색')}
                  value={codeDraft.keyword}
                  onChange={(event) =>
                    setCodeDraft((current) => ({ ...current, keyword: event.target.value }))
                  }
                />
                <Select
                  allowClear
                  placeholder={text('pages.mdm.use', '사용 여부')}
                  style={{ width: 140 }}
                  value={codeDraft.useYn}
                  onChange={(value) => setCodeDraft((current) => ({ ...current, useYn: value }))}
                  options={[
                    { value: 'Y', label: 'Y' },
                    { value: 'N', label: 'N' },
                  ]}
                />
                <Button
                  onClick={() => {
                    setCodePage(1);
                    setCodeFilters(codeDraft);
                  }}
                >
                  {text('pages.mdm.search', '조회')}
                </Button>
                {can('CREATE') ? (
                  <Button
                    type="primary"
                    onClick={() => {
                      setEditingCode(undefined);
                      setCodeOpen(true);
                    }}
                  >
                    {text('pages.mdm.createCode', '코드 등록')}
                  </Button>
                ) : null}
              </Space>
              <Table<CommonCode>
                rowKey="codePk"
                loading={codeLoading}
                columns={codeColumns}
                dataSource={codeRows}
                rowClassName={(record) => (inUse(record.useYn) ? '' : styles.inactive)}
                expandable={selected.hierarchy ? { defaultExpandAllRows: true } : undefined}
                onChange={(pagination, _filters, sorter) => {
                  setCodePage(pagination.current ?? 1);
                  setCodeSort(sortParams(sorter));
                }}
                pagination={{
                  current: codePage,
                  pageSize: 20,
                  total: codeTotal,
                  showSizeChanger: false,
                }}
              />
            </Space>
          ) : (
            <Empty description={text('pages.mdm.pickGroup', '왼쪽에서 코드 그룹을 선택하세요.')} />
          )}
        </section>
      </div>
      <GroupFormDrawer
        open={groupOpen}
        saving={saving}
        group={editingGroup}
        languages={languages}
        onClose={() => setGroupOpen(false)}
        onSubmit={async (body) => {
          setSaving(true);
          try {
            if (editingGroup) {
              await updateCodeGroup(editingGroup.codeGroupCode, body);
            } else {
              const created = await createCodeGroup(body);
              chooseGroup(created.codeGroupCode);
            }
            setGroupOpen(false);
            setReload((value) => value + 1);
          } finally {
            setSaving(false);
          }
        }}
      />
      {selected ? (
        <CodeFormDrawer
          open={codeOpen}
          saving={saving}
          group={selected}
          code={editingCode}
          languages={languages}
          onClose={() => setCodeOpen(false)}
          onSubmit={async (body) => {
            setSaving(true);
            try {
              if (editingCode) {
                await updateCommonCode(editingCode.codePk, body);
              } else {
                await createCommonCode(body);
              }
              setCodeOpen(false);
              setReload((value) => value + 1);
            } finally {
              setSaving(false);
            }
          }}
        />
      ) : null}
      <HistoryDrawer codePk={historyPk} onClose={() => setHistoryPk(undefined)} />
    </PageContainer>
  );
};

export default CommonCodePage;
