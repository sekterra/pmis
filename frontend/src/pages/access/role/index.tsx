import { PageContainer } from '@ant-design/pro-components';
import { useAccess, useIntl } from '@umijs/max';
import { Alert, App, Button, Input, Select, Space, Table } from 'antd';
import type { TableProps } from 'antd';
import React, { useEffect, useRef, useState } from 'react';
import type { MenuAction } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import { ynParam } from '@/pages/org/shared/useYn';
import type { SupportLanguage } from '@/services/mdm/data';
import { fetchLanguages } from '@/services/mdm/i18n';
import { createRole, deleteRole, getRole, listRoles, restoreRole, updateRole } from '@/services/mdm/roles';
import type { Role, RoleCreateBody, RoleListQuery, RoleUpdateBody } from '@/services/mdm/rolesRecord';
import RoleDrawer from './RoleDrawer';
import styles from './index.module.css';
import { roleErrorText } from './rules';

type Filters = {
  keyword?: string;
  useYn?: string;
};

function sortParams(
  sorter:
    | { field?: unknown; order?: 'ascend' | 'descend' | null }
    | Array<{ field?: unknown; order?: 'ascend' | 'descend' | null }>,
): string[] {
  const allowed = new Set(['roleCode', 'roleName', 'sortOrder']);
  const list = Array.isArray(sorter) ? sorter : [sorter];
  return list.flatMap((item) => {
    const field = typeof item.field === 'string' ? item.field : '';
    if (!field || !item.order || !allowed.has(field)) {
      return [];
    }
    return [`${field},${item.order === 'descend' ? 'desc' : 'asc'}`];
  });
}

const RolePage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (action: MenuAction) => access.can?.('ROLE', action) ?? false;
  const [languages, setLanguages] = useState<SupportLanguage[]>([]);
  const [draft, setDraft] = useState<Filters>({});
  const [filters, setFilters] = useState<Filters>({});
  const [rows, setRows] = useState<Role[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState<string>();
  const [open, setOpen] = useState(false);
  const [role, setRole] = useState<Role>();
  const [tab, setTab] = useState('basic');
  const [saving, setSaving] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    fetchLanguages()
      .then((items) => {
        if (alive) {
          setLanguages(items);
        }
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, []);

  const failRef = useRef<(error: unknown) => void>(() => undefined);
  failRef.current = (error: unknown) => {
    const api = error instanceof ApiError ? error : new ApiError('');
    setNotice(roleErrorText(api, (id, fallback) => intl.formatMessage({ id, defaultMessage: fallback })));
  };
  const fail = (error: unknown) => failRef.current(error);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    const query: RoleListQuery = {
      page,
      size: 20,
      keyword: filters.keyword,
      useYn: ynParam(filters.useYn),
      sort,
    };
    listRoles(query)
      .then((result) => {
        if (!alive) {
          return;
        }
        setRows(result.items);
        setTotal(result.total);
        setNotice(undefined);
      })
      .catch((error: unknown) => {
        if (alive) {
          failRef.current(error);
        }
      })
      .finally(() => {
        if (alive) {
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [page, filters, sort, reload]);

  const save = async (body: RoleCreateBody | RoleUpdateBody) => {
    setSaving(true);
    try {
      if (role) {
        setRole(await updateRole(role.rolePk, body as RoleUpdateBody));
      } else {
        const created = await createRole(body as RoleCreateBody);
        setRole(created);
        setTab('permissions');
      }
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const openRole = (rolePk: number) => {
    getRole(rolePk)
      .then((detail) => {
        setRole(detail);
        setTab('basic');
        setOpen(true);
      })
      .catch(fail);
  };

  const askDelete = (row: Role) => {
    modal.confirm({
      title: text('pages.access.role.deleteTitle', '역할을 사용 중지할까요?'),
      content: text('pages.access.role.deleteHint', '삭제하면 사용 중지됩니다. 데이터는 지워지지 않습니다.'),
      okText: text('pages.org.company.deleteOk', '삭제 확인'),
      onOk: async () => {
        try {
          await deleteRole(row.rolePk);
          setReload((value) => value + 1);
        } catch (error) {
          fail(error);
        }
      },
    });
  };

  const columns: TableProps<Role>['columns'] = [
    { title: text('pages.access.role.code', '역할 코드'), dataIndex: 'roleCode', sorter: true },
    { title: text('pages.access.role.name', '역할명'), dataIndex: 'roleName', sorter: true },
    { title: text('pages.org.company.sortOrder', '정렬 순서'), dataIndex: 'sortOrder', sorter: true },
    { title: text('pages.access.role.userCount', '사용자 수'), dataIndex: 'userCount' },
    {
      title: text('pages.org.use', '사용 여부'),
      dataIndex: 'useYn',
      render: (value: string) => (value === 'Y' ? text('pages.org.inUse', '사용') : text('pages.org.stopped', '중지')),
    },
    {
      title: text('pages.org.actions', '작업'),
      render: (_, row) => (
        <Space>
          <Button
            size="small"
            onClick={(event) => {
              event.stopPropagation();
              openRole(row.rolePk);
            }}
          >
            {text('pages.access.role.detail', '상세')}
          </Button>
          {row.roleCode === 'TENANT_ADMIN' ? <span>{text('pages.access.role.protected', '보호 역할')}</span> : null}
          {row.useYn === 'Y' && row.roleCode !== 'TENANT_ADMIN' && can('DELETE') ? (
            <Button
              size="small"
              onClick={(event) => {
                event.stopPropagation();
                askDelete(row);
              }}
            >
              {text('pages.org.delete', '삭제')}
            </Button>
          ) : null}
          {row.useYn !== 'Y' && can('UPDATE') ? (
            <Button
              size="small"
              onClick={(event) => {
                event.stopPropagation();
                restoreRole(row.rolePk)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail);
              }}
            >
              {text('pages.org.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title={text('pages.access.role.title', '역할')}>
      {notice ? <Alert type="error" showIcon title={notice} style={{ marginBottom: 16 }} /> : null}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          aria-label={text('pages.org.keyword', '검색어')}
          placeholder={text('pages.org.keyword', '검색어')}
          value={draft.keyword}
          onChange={(event) => setDraft((current) => ({ ...current, keyword: event.target.value }))}
        />
        <Select
          allowClear
          aria-label={text('pages.org.use', '사용 여부')}
          placeholder={text('pages.org.useAll', '사용 여부 전체')}
          style={{ minWidth: 140 }}
          value={draft.useYn}
          options={[
            { value: 'Y', label: text('pages.org.inUse', '사용') },
            { value: 'N', label: text('pages.org.stopped', '중지') },
          ]}
          onChange={(value) => setDraft((current) => ({ ...current, useYn: value }))}
        />
        <Button
          type="primary"
          onClick={() => {
            setPage(1);
            setFilters(draft);
          }}
        >
          {text('pages.org.search', '조회')}
        </Button>
        <Button
          onClick={() => {
            setDraft({});
            setFilters({});
            setPage(1);
          }}
        >
          {text('pages.org.reset', '초기화')}
        </Button>
      </Space>
      <div style={{ marginBottom: 12, textAlign: 'right' }}>
        {can('CREATE') ? (
          <Button
            type="primary"
            onClick={() => {
              setRole(undefined);
              setTab('basic');
              setOpen(true);
            }}
          >
            {text('pages.access.role.create', '역할 등록')}
          </Button>
        ) : null}
      </div>
      <Table<Role>
        rowKey="rolePk"
        loading={loading}
        columns={columns}
        dataSource={rows}
        rowClassName={(row) => (row.useYn === 'Y' ? '' : styles.inactive)}
        pagination={{ current: page, pageSize: 20, total, showSizeChanger: false }}
        onRow={(row) => ({
          onClick: () => openRole(row.rolePk),
        })}
        onChange={(pagination, _filters, sorter) => {
          setPage(pagination.current ?? 1);
          setSort(sortParams(sorter));
        }}
      />
      <RoleDrawer
        open={open}
        role={role}
        tab={tab}
        saving={saving}
        languages={languages}
        canUpdate={can('UPDATE')}
        onTab={setTab}
        onClose={() => {
          setOpen(false);
          setRole(undefined);
          setTab('basic');
        }}
        onSubmit={save}
        onNotice={setNotice}
      />
    </PageContainer>
  );
};

export default RolePage;
