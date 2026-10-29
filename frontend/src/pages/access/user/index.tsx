import { PageContainer } from '@ant-design/pro-components';
import { useAccess, useIntl, useModel } from '@umijs/max';
import { Alert, App, Button, Input, Select, Space, Table, Tag } from 'antd';
import type { TableProps } from 'antd';
import React, { useEffect, useRef, useState } from 'react';
import IssuedPasswordModal from '@/components/IssuedPasswordModal';
import { useCodeOptions } from '@/core/codes/useCodeOptions';
import type { MenuAction } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import { ynParam } from '@/pages/org/shared/useYn';
import type { Me, SupportLanguage } from '@/services/mdm/data';
import { fetchLanguages } from '@/services/mdm/i18n';
import { listRoles } from '@/services/mdm/roles';
import {
  changeUserStatus,
  createUser,
  deleteUser,
  getUser,
  listUsers,
  resetUserPassword,
  restoreUser,
  updateUser,
} from '@/services/mdm/users';
import type { IssuedPassword, TenantUser, UserCreateBody, UserListQuery, UserUpdateBody } from '@/services/mdm/usersRecord';
import UserDrawer from './UserDrawer';
import styles from './index.module.css';
import { formatRoles, isSelf, userErrorText, type UserAction } from './rules';

type Filters = {
  keyword?: string;
  userStatusCode?: string;
  accountTypeCode?: string;
  roleCode?: string;
  useYn?: string;
};

type Secret = {
  password: string;
  expire: string | null;
  enterEditPk?: number;
};

const STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'green',
  LOCKED: 'red',
  DORMANT: 'orange',
  WITHDRAWN: 'default',
};

const STATUS_TEXT: Record<string, [string, string]> = {
  ACTIVE: ['pages.access.user.status.ACTIVE', '정상'],
  LOCKED: ['pages.access.user.status.LOCKED', '잠금'],
  DORMANT: ['pages.access.user.status.DORMANT', '휴면'],
  WITHDRAWN: ['pages.access.user.status.WITHDRAWN', '탈퇴'],
};

function sortParams(
  sorter: { field?: unknown; order?: 'ascend' | 'descend' | null } | Array<{ field?: unknown; order?: 'ascend' | 'descend' | null }>,
): string[] {
  const allowed = new Set(['loginId', 'userStatusCode', 'lastLoginDatetime']);
  const list = Array.isArray(sorter) ? sorter : [sorter];
  return list.flatMap((item) => {
    const field = typeof item.field === 'string' ? item.field : '';
    if (!field || !item.order || !allowed.has(field)) {
      return [];
    }
    return [`${field},${item.order === 'descend' ? 'desc' : 'asc'}`];
  });
}

const UserPage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (action: MenuAction) => access.can?.('USER', action) ?? false;
  const canRoleRead = access.can?.('ROLE', 'READ') ?? false;
  const { initialState } = useModel('@@initialState');
  const currentUserPk = (initialState?.currentUser as Me | undefined)?.userPk;
  const statusCodes = useCodeOptions('USER_STATUS');
  const accountCodes = useCodeOptions('ACCOUNT_TYPE');
  const [languages, setLanguages] = useState<SupportLanguage[]>([]);
  const [roleOptions, setRoleOptions] = useState<{ roleCode: string; roleName: string }[]>([]);
  const [draft, setDraft] = useState<Filters>({});
  const [filters, setFilters] = useState<Filters>({});
  const [rows, setRows] = useState<TenantUser[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState<string>();
  const [open, setOpen] = useState(false);
  const [user, setUser] = useState<TenantUser>();
  const [tab, setTab] = useState('basic');
  const [saving, setSaving] = useState(false);
  const [reload, setReload] = useState(0);
  const [secret, setSecret] = useState<Secret>();

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

  useEffect(() => {
    if (!canRoleRead) {
      setRoleOptions([]);
      return;
    }
    let alive = true;
    listRoles({ useYn: 'Y', page: 1, size: 200 })
      .then((result) => {
        if (alive) {
          setRoleOptions(result.items.map((item) => ({ roleCode: item.roleCode, roleName: item.roleName })));
        }
      })
      .catch(() => {
        if (alive) {
          setRoleOptions([]);
        }
      });
    return () => {
      alive = false;
    };
  }, [canRoleRead]);

  const failRef = useRef<(error: unknown) => void>(() => undefined);
  failRef.current = (error: unknown) => {
    const api = error instanceof ApiError ? error : new ApiError('');
    setNotice(userErrorText(api, text));
  };
  const fail = (error: unknown) => failRef.current(error);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    const query: UserListQuery = {
      page,
      size: 20,
      keyword: filters.keyword,
      userStatusCode: filters.userStatusCode,
      accountTypeCode: filters.accountTypeCode,
      roleCode: filters.roleCode,
      useYn: ynParam(filters.useYn),
      sort,
    };
    listUsers(query)
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

  const refreshUser = async (userPk: number) => {
    setUser(await getUser(userPk));
    setReload((value) => value + 1);
  };

  const save = async (body: UserCreateBody | UserUpdateBody) => {
    setSaving(true);
    try {
      if (user) {
        setUser(await updateUser(user.userPk, body as UserUpdateBody));
        setReload((value) => value + 1);
        return;
      }
      const issued: IssuedPassword = await createUser(body as UserCreateBody);
      setSecret({ password: issued.temporaryPassword, expire: issued.passwordExpireDatetime, enterEditPk: issued.userPk });
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const closeSecret = () => {
    const enterEditPk = secret?.enterEditPk;
    const refreshPk = enterEditPk ?? user?.userPk;
    setSecret(undefined);
    if (refreshPk != null) {
      getUser(refreshPk)
        .then((row) => {
          setUser(row);
          setOpen(true);
          setReload((value) => value + 1);
        })
        .catch(fail);
    }
  };

  const confirmAction = (action: UserAction, row: TenantUser) => {
    const copy: Record<UserAction, [string, string, string]> = {
      lock: ['pages.access.user.lock', '잠금', 'pages.access.user.sessionHint'],
      unlock: ['pages.access.user.unlock', '잠금 해제', 'pages.access.user.sessionHint'],
      dormant: ['pages.access.user.dormant', '휴면', 'pages.access.user.sessionHint'],
      wake: ['pages.access.user.wake', '휴면 해제', 'pages.access.user.sessionHint'],
      reset: ['pages.access.user.reset', '비밀번호 초기화', 'pages.access.user.resetHint'],
      delete: ['pages.org.delete', '삭제', 'pages.access.user.deleteHint'],
      restore: ['pages.org.restore', '복구', 'pages.access.user.restoreHint'],
    };
    const hints: Record<string, string> = {
      'pages.access.user.sessionHint': '로그인 중인 세션이 종료됩니다.',
      'pages.access.user.resetHint': '새 임시 비밀번호가 발급되고 세션이 종료됩니다.',
      'pages.access.user.deleteHint': '탈퇴 처리되고 세션이 종료됩니다.',
      'pages.access.user.restoreHint': '정상 상태로 돌아오고 비밀번호 변경이 필요합니다.',
    };
    const status: Partial<Record<UserAction, string>> = { lock: 'LOCKED', unlock: 'ACTIVE', dormant: 'DORMANT', wake: 'ACTIVE' };
    modal.confirm({
      title: text(copy[action][0], copy[action][1]),
      content: text(copy[action][2], hints[copy[action][2]] ?? ''),
      okText: text(copy[action][0], copy[action][1]),
      onOk: async () => {
        try {
          if (action === 'delete') {
            await deleteUser(row.userPk);
            setOpen(false);
            setUser(undefined);
            setReload((value) => value + 1);
            return;
          }
          if (action === 'restore') {
            setUser(await restoreUser(row.userPk));
            setOpen(true);
            setReload((value) => value + 1);
            return;
          }
          if (action === 'reset') {
            const issued = await resetUserPassword(row.userPk);
            setSecret({ password: issued.temporaryPassword, expire: issued.passwordExpireDatetime });
            await refreshUser(row.userPk);
            return;
          }
          const next = status[action];
          if (next) {
            setUser(await changeUserStatus(row.userPk, next));
            setReload((value) => value + 1);
          }
        } catch (error) {
          fail(error);
        }
      },
    });
  };

  const openUser = (userPk: number) => {
    setUser(undefined);
    setTab('basic');
    setOpen(true);
    getUser(userPk).then(setUser).catch(fail);
  };

  const names = new Map(roleOptions.map((item) => [item.roleCode, item.roleName]));
  const statusLabel = (code: string) => statusCodes.options.find((item) => item.value === code)?.label ?? text(STATUS_TEXT[code]?.[0] ?? code, STATUS_TEXT[code]?.[1] ?? code);
  const accountLabel = (code: string) =>
    accountCodes.options.find((item) => item.value === code)?.label ??
    (code === 'NORMAL' ? text('pages.access.user.account.NORMAL', '일반') : code === 'SYSTEM' ? text('pages.access.user.account.SYSTEM', '시스템') : code);

  const columns: TableProps<TenantUser>['columns'] = [
    { title: text('pages.access.user.loginId', '로그인 아이디'), dataIndex: 'loginId', sorter: true },
    { title: text('pages.access.user.person', '사람'), dataIndex: 'personName', render: (value) => value || '-' },
    { title: text('pages.access.user.accountType', '계정 유형'), dataIndex: 'accountTypeCode', render: (value: string) => accountLabel(value) },
    {
      title: text('pages.access.user.status', '상태'),
      dataIndex: 'userStatusCode',
      sorter: true,
      render: (value: string) => <Tag color={STATUS_COLOR[value] ?? 'default'}>{statusLabel(value)}</Tag>,
    },
    {
      title: text('pages.access.user.roles', '역할'),
      dataIndex: 'roleCodes',
      render: (value: string[]) => formatRoles(value, canRoleRead ? names : new Map(), (count) => `${text('pages.access.user.more', '외')} ${count}`),
    },
    { title: text('pages.access.user.lastLogin', '마지막 로그인'), dataIndex: 'lastLoginDatetime', sorter: true, render: (value) => value || '-' },
    {
      title: text('pages.access.user.mustChange', '비밀번호 변경 필요'),
      dataIndex: 'mustChangePasswordYn',
      render: (value: string) => (value === 'Y' ? text('pages.access.user.mustChangeYes', '필요') : '-'),
    },
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
              openUser(row.userPk);
            }}
          >
            {text('pages.access.user.detail', '상세')}
          </Button>
          {row.useYn === 'Y' && row.userStatusCode !== 'WITHDRAWN' && can('DELETE') && !isSelf(currentUserPk, row.userPk) ? (
            <Button
              size="small"
              onClick={(event) => {
                event.stopPropagation();
                confirmAction('delete', row);
              }}
            >
              {text('pages.org.delete', '삭제')}
            </Button>
          ) : null}
          {(row.useYn !== 'Y' || row.userStatusCode === 'WITHDRAWN') && can('UPDATE') ? (
            <Button
              size="small"
              onClick={(event) => {
                event.stopPropagation();
                confirmAction('restore', row);
              }}
            >
              {text('pages.org.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const statusOptions =
    statusCodes.options.length > 0
      ? statusCodes.options
      : Object.entries(STATUS_TEXT).map(([value, entry]) => ({ value, label: text(entry[0], entry[1]) }));

  return (
    <PageContainer title={text('pages.access.user.title', '사용자')}>
      {notice ? <Alert type="error" showIcon title={notice} style={{ marginBottom: 16 }} /> : null}
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          allowClear
          aria-label={text('pages.access.user.keyword', '로그인 아이디·사람')}
          placeholder={text('pages.access.user.keyword', '로그인 아이디·사람')}
          value={draft.keyword}
          onChange={(event) => setDraft((current) => ({ ...current, keyword: event.target.value }))}
        />
        <Select
          allowClear
          aria-label={text('pages.access.user.status', '상태')}
          placeholder={text('pages.access.user.statusAll', '상태 전체')}
          style={{ minWidth: 140 }}
          value={draft.userStatusCode}
          options={statusOptions}
          onChange={(value) => setDraft((current) => ({ ...current, userStatusCode: value }))}
        />
        <Select
          allowClear
          aria-label={text('pages.access.user.accountType', '계정 유형')}
          placeholder={text('pages.access.user.accountAll', '계정 유형 전체')}
          style={{ minWidth: 140 }}
          value={draft.accountTypeCode}
          options={
            accountCodes.options.length > 0
              ? accountCodes.options
              : [
                  { value: 'NORMAL', label: text('pages.access.user.account.NORMAL', '일반') },
                  { value: 'SYSTEM', label: text('pages.access.user.account.SYSTEM', '시스템') },
                ]
          }
          onChange={(value) => setDraft((current) => ({ ...current, accountTypeCode: value }))}
        />
        {canRoleRead ? (
          <Select
            allowClear
            aria-label={text('pages.access.user.role', '역할')}
            placeholder={text('pages.access.user.roleAll', '역할 전체')}
            style={{ minWidth: 140 }}
            value={draft.roleCode}
            options={roleOptions.map((item) => ({ value: item.roleCode, label: item.roleName }))}
            onChange={(value) => setDraft((current) => ({ ...current, roleCode: value }))}
          />
        ) : null}
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
              setUser(undefined);
              setTab('basic');
              setOpen(true);
            }}
          >
            {text('pages.access.user.create', '사용자 등록')}
          </Button>
        ) : null}
      </div>
      <Table<TenantUser>
        rowKey="userPk"
        loading={loading}
        columns={columns}
        dataSource={rows}
        rowClassName={(row) => (row.useYn === 'Y' && row.userStatusCode !== 'WITHDRAWN' ? '' : styles.inactive)}
        pagination={{ current: page, pageSize: 20, total, showSizeChanger: false }}
        onRow={(row) => ({ onClick: () => openUser(row.userPk) })}
        onChange={(pagination, _filters, sorter) => {
          setPage(pagination.current ?? 1);
          setSort(sortParams(sorter));
        }}
      />
      <UserDrawer
        open={open}
        user={user}
        tab={tab}
        saving={saving}
        languages={languages}
        roleOptions={roleOptions}
        codeLabels={{ status: statusOptions, account: accountCodes.options }}
        canUpdate={can('UPDATE')}
        canDelete={can('DELETE')}
        canRoleRead={canRoleRead}
        currentUserPk={currentUserPk}
        text={text}
        onTab={setTab}
        onClose={() => {
          setOpen(false);
          setUser(undefined);
          setTab('basic');
        }}
        onSubmit={save}
        onAction={(action) => {
          if (user) {
            confirmAction(action, user);
          }
        }}
        onNotice={setNotice}
      />
      <IssuedPasswordModal
        open={Boolean(secret?.password)}
        password={secret?.password}
        expire={secret?.expire}
        title={text('pages.access.user.tempTitle', '임시 비밀번호')}
        hint={text('pages.access.user.tempHint', '이 창을 닫으면 다시 볼 수 없습니다. 사용자는 첫 로그인에서 비밀번호를 바꿔야 합니다.')}
        expireLabel={text('pages.access.user.passwordExpire', '비밀번호 만료')}
        copyLabel={text('pages.access.user.copy', '복사')}
        copyFailed={text('pages.access.user.copyFailed', '복사하지 못했습니다. 비밀번호를 선택해서 복사하세요.')}
        closeLabel={text('pages.access.user.closeSecret', '닫기')}
        onClose={closeSecret}
      />
    </PageContainer>
  );
};

export default UserPage;
