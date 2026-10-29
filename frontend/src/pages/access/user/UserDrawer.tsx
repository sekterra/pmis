import { Alert, Button, Drawer, Form, Input, Select, Space, Table, Tabs } from 'antd';
import type { TableProps } from 'antd';
import React, { useEffect, useRef, useState } from 'react';
import type { SupportLanguage } from '@/services/mdm/data';
import { listPeople } from '@/services/mdm/people';
import type { PersonListItem } from '@/services/mdm/peopleRecord';
import { replaceUserRoles, listUserRoles } from '@/services/mdm/users';
import { toCreateBody, toRoleGrantBody, toUpdateBody } from '@/services/mdm/usersQuery';
import type { RoleGrantBody, TenantUser, UserCreateBody, UserFormValues, UserRoleGrant, UserUpdateBody } from '@/services/mdm/usersRecord';
import { ApiError } from '@/core/request/errors';
import RoleGrantModal from './RoleGrantModal';
import {
  formatRoles,
  hiddenWhenSelf,
  isSelf,
  timeZoneOptions,
  userActions,
  userErrorText,
  userFormErrors,
  roleGrantRowErrors,
  validLoginId,
  type ErrorText,
  type UserAction,
} from './rules';

type RoleOption = { roleCode: string; roleName: string };

type UserDrawerProps = {
  open: boolean;
  user?: TenantUser;
  tab: string;
  saving: boolean;
  languages: SupportLanguage[];
  roleOptions: RoleOption[];
  codeLabels: { status: { value: string; label: string }[]; account: { value: string; label: string }[] };
  canUpdate: boolean;
  canDelete: boolean;
  canRoleRead: boolean;
  currentUserPk?: number;
  text: ErrorText;
  onTab: (tab: string) => void;
  onClose: () => void;
  onSubmit: (body: UserCreateBody | UserUpdateBody) => Promise<void>;
  onAction: (action: UserAction) => void;
  onNotice: (message: string) => void;
};

const STATUS_FALLBACK: Record<string, [string, string]> = {
  ACTIVE: ['pages.access.user.status.ACTIVE', '정상'],
  LOCKED: ['pages.access.user.status.LOCKED', '잠금'],
  DORMANT: ['pages.access.user.status.DORMANT', '휴면'],
  WITHDRAWN: ['pages.access.user.status.WITHDRAWN', '탈퇴'],
};

const UserDrawer: React.FC<UserDrawerProps> = ({
  open,
  user,
  tab,
  saving,
  languages,
  roleOptions,
  codeLabels,
  canUpdate,
  canDelete,
  canRoleRead,
  currentUserPk,
  text,
  onTab,
  onClose,
  onSubmit,
  onAction,
  onNotice,
}) => {
  const [form] = Form.useForm();
  const watchedRoles = Form.useWatch('roleCodes', form) as string[] | undefined;
  const [people, setPeople] = useState<PersonListItem[]>([]);
  const [personKeyword, setPersonKeyword] = useState('');
  const [grants, setGrants] = useState<UserRoleGrant[]>([]);
  const [roleUse, setRoleUse] = useState<string>();
  const [rolePage, setRolePage] = useState(1);
  const [roleReload, setRoleReload] = useState(0);
  const [grantOpen, setGrantOpen] = useState(false);
  const [grantSaving, setGrantSaving] = useState(false);
  const [grantNotice, setGrantNotice] = useState<string>();
  const [rowErrors, setRowErrors] = useState<Record<string, string>>({});
  const loadedKey = useRef('');
  const noticeRef = useRef(onNotice);
  const textRef = useRef(text);
  noticeRef.current = onNotice;
  textRef.current = text;
  const self = user ? isSelf(currentUserPk, user.userPk) : false;
  const active = Boolean(user) && user?.useYn === 'Y' && user.userStatusCode !== 'WITHDRAWN';
  const editable = !user || (active && canUpdate);
  const zones = timeZoneOptions();

  useEffect(() => {
    if (!open) {
      loadedKey.current = '';
      setGrantOpen(false);
      return;
    }
    if (!user) {
      form.resetFields();
      form.setFieldsValue({ accountTypeCode: 'NORMAL', supportLanguageCode: 'ko-KR', timezoneId: 'Asia/Seoul', roleCodes: [] });
      return;
    }
    form.setFieldsValue({
      loginId: user.loginId,
      accountTypeCode: user.accountTypeCode,
      personPk: user.personPk ?? undefined,
      supportLanguageCode: user.supportLanguageCode ?? undefined,
      timezoneId: user.timezoneId ?? undefined,
      description: user.description ?? undefined,
    });
  }, [open, user, form]);

  useEffect(() => {
    const keyword = personKeyword.trim();
    if (!open || !keyword) {
      return;
    }
    let alive = true;
    listPeople({ keyword, useYn: 'Y', page: 1, size: 20 })
      .then((result) => {
        if (alive) {
          setPeople(result.items);
        }
      })
      .catch((error: unknown) => {
        if (alive && error instanceof ApiError) {
          noticeRef.current(userErrorText(error, textRef.current));
        }
      });
    return () => {
      alive = false;
    };
  }, [open, personKeyword]);

  useEffect(() => {
    if (!open || tab !== 'roles' || !user) {
      return;
    }
    const userPk = user.userPk;
    const key = `${userPk}:${rolePage}:${roleUse ?? ''}:${roleReload}`;
    if (loadedKey.current === key) {
      return;
    }
    loadedKey.current = key;
    let alive = true;
    listUserRoles(userPk, roleUse)
      .then((items) => {
        if (alive && loadedKey.current === key) {
          setGrants(items);
        }
      })
      .catch((error: unknown) => {
        if (alive && loadedKey.current === key && error instanceof ApiError) {
          noticeRef.current(userErrorText(error, textRef.current));
        }
      });
    return () => {
      alive = false;
    };
  }, [open, tab, user?.userPk, rolePage, roleUse, roleReload]);

  const labelOf = (options: { value: string; label: string }[], code: string | null | undefined, fallback?: [string, string]) => {
    const found = options.find((item) => item.value === code)?.label;
    if (found) {
      return found;
    }
    if (code && fallback) {
      return text(fallback[0], fallback[1]);
    }
    return code || '-';
  };

  const personOptions = [
    ...(user?.personPk ? [{ value: user.personPk, label: user.personName ?? String(user.personPk) }] : []),
    ...people
      .filter((item) => item.personPk !== user?.personPk)
      .map((item) => ({
        value: item.personPk,
        label: item.email ? `${item.personName} (${item.email})` : item.personName,
      })),
  ];

  const submit = async () => {
    let values: UserFormValues;
    try {
      values = (await form.validateFields()) as UserFormValues;
    } catch {
      return;
    }
    try {
      await onSubmit(user ? toUpdateBody(values) : toCreateBody(values));
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      const fields = userFormErrors(api, text);
      if (fields.length > 0) {
        form.setFields(fields);
        return;
      }
      onNotice(userErrorText(api, text));
    }
  };

  const saveGrants = async (body: RoleGrantBody[]) => {
    if (!user) {
      return;
    }
    setGrantSaving(true);
    setGrantNotice(undefined);
    setRowErrors({});
    try {
      await replaceUserRoles(user.userPk, toRoleGrantBody(body));
      setGrantOpen(false);
      loadedKey.current = '';
      setRoleReload((value) => value + 1);
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      const rows = roleGrantRowErrors(api, body.map((item) => item.roleCode));
      setRowErrors(rows);
      if (Object.keys(rows).length === 0) {
        setGrantNotice(userErrorText(api, text));
      }
    } finally {
      setGrantSaving(false);
    }
  };

  const actions = user ? userActions(user).filter((action) => !(self && hiddenWhenSelf(action))) : [];
  const showAction = (action: UserAction) => {
    if (!actions.includes(action)) {
      return false;
    }
    if (action === 'delete') {
      return canDelete;
    }
    return canUpdate;
  };

  const actionText: Record<UserAction, [string, string]> = {
    lock: ['pages.access.user.lock', '잠금'],
    unlock: ['pages.access.user.unlock', '잠금 해제'],
    dormant: ['pages.access.user.dormant', '휴면'],
    wake: ['pages.access.user.wake', '휴면 해제'],
    reset: ['pages.access.user.reset', '비밀번호 초기화'],
    delete: ['pages.org.delete', '삭제'],
    restore: ['pages.org.restore', '복구'],
  };

  const roleNames = new Map(roleOptions.map((item) => [item.roleCode, canRoleRead ? item.roleName : item.roleCode]));
  const grantColumns: TableProps<UserRoleGrant>['columns'] = [
    {
      title: text('pages.access.user.role', '역할'),
      render: (_, row) => (canRoleRead ? row.roleName || row.roleCode : row.roleCode),
    },
    { title: text('pages.access.user.validFrom', '시작일'), dataIndex: 'validFromDate', render: (value) => value || '-' },
    { title: text('pages.access.user.validTo', '종료일'), dataIndex: 'validToDate', render: (value) => value || '-' },
    {
      title: text('pages.org.use', '사용 여부'),
      dataIndex: 'useYn',
      render: (value: string) => (value === 'Y' ? text('pages.org.inUse', '사용') : text('pages.org.stopped', '중지')),
    },
  ];

  const basic = (
    <Form form={form} layout="vertical">
      {!user ? (
        <>
          <Form.Item
            name="loginId"
            label={text('pages.access.user.loginId', '로그인 아이디')}
            rules={[{ validator: async (_, value) => (validLoginId(value) ? Promise.resolve() : Promise.reject(new Error(text('pages.access.user.loginRule', '영문, 숫자, ._@- 만 3~100자입니다.')))) }]}
            extra={text('pages.access.user.loginHint', '소문자로 저장됩니다. 논리 삭제한 아이디는 다시 쓸 수 없습니다.')}
          >
            <Input />
          </Form.Item>
          {canRoleRead ? (
            <Form.Item name="roleCodes" label={text('pages.access.user.initialRoles', '초기 역할')}>
              <Select mode="multiple" allowClear options={roleOptions.map((item) => ({ value: item.roleCode, label: item.roleName }))} />
            </Form.Item>
          ) : (
            <Alert type="info" showIcon title={text('pages.access.user.noRoleRead', '역할 조회 권한이 없어 역할 코드를 그대로 보여 주고 편집은 숨깁니다.')} style={{ marginBottom: 16 }} />
          )}
          {watchedRoles?.includes('TENANT_ADMIN') ? (
            <Alert type="warning" showIcon title={text('pages.access.user.adminWarn', '테넌트 관리자 역할입니다. 마지막 관리자는 잠그거나 뺄 수 없습니다.')} style={{ marginBottom: 16 }} />
          ) : null}
        </>
      ) : (
        <>
          <Form.Item name="loginId" label={text('pages.access.user.loginId', '로그인 아이디')}>
            <Input disabled />
          </Form.Item>
          <Form.Item label={text('pages.access.user.status', '상태')}>
            <Input disabled value={labelOf(codeLabels.status, user.userStatusCode, STATUS_FALLBACK[user.userStatusCode])} />
          </Form.Item>
        </>
      )}
      <Form.Item name="accountTypeCode" label={text('pages.access.user.accountType', '계정 유형')}>
        <Select
          disabled={Boolean(user)}
          options={
            codeLabels.account.length > 0
              ? codeLabels.account
              : [
                  { value: 'NORMAL', label: text('pages.access.user.account.NORMAL', '일반') },
                  { value: 'SYSTEM', label: text('pages.access.user.account.SYSTEM', '시스템') },
                ]
          }
        />
      </Form.Item>
      <Form.Item name="personPk" label={text('pages.access.user.person', '연결할 사람')}>
        <Select
          allowClear
          showSearch={{ filterOption: false, onSearch: setPersonKeyword }}
          disabled={!editable}
          options={personOptions}
        />
      </Form.Item>
      <Form.Item name="supportLanguageCode" label={text('pages.access.user.language', '언어')}>
        <Select
          disabled={!editable}
          options={languages.map((item) => ({ value: item.supportLanguageCode, label: item.languageName || item.supportLanguageCode }))}
        />
      </Form.Item>
      <Form.Item name="timezoneId" label={text('pages.access.user.timezone', '시간대')}>
        <Select showSearch disabled={!editable} options={zones.map((zone) => ({ value: zone, label: zone }))} />
      </Form.Item>
      <Form.Item name="description" label={text('pages.access.user.description', '설명')}>
        <Input.TextArea rows={3} disabled={!editable} />
      </Form.Item>
      {user ? (
        <Space orientation="vertical" style={{ width: '100%' }}>
          {self ? <Alert type="info" showIcon title={text('pages.access.user.selfHint', '본인 계정은 변경할 수 없습니다.')} /> : null}
          {!active ? <Alert type="info" showIcon title={text('pages.access.user.inactiveHint', '사용 중지되었거나 탈퇴한 계정은 복구만 할 수 있습니다.')} /> : null}
          <div>{`${text('pages.access.user.lockType', '잠금 종류')}: ${user.lockType === 'ADMIN' ? text('pages.access.user.lockAdmin', '관리자 잠금') : user.lockType === 'AUTO' ? text('pages.access.user.lockAuto', '자동 잠금') : '-'}`}</div>
          <div>{`${text('pages.access.user.failCount', '로그인 실패 횟수')}: ${user.loginFailCount ?? 0}`}</div>
          <div>{`${text('pages.access.user.passwordChanged', '비밀번호 변경일')}: ${user.passwordChangedDatetime || '-'}`}</div>
          <div>{`${text('pages.access.user.passwordExpire', '비밀번호 만료')}: ${user.passwordExpireDatetime || '-'}`}</div>
          <div>{`${text('pages.access.user.lastLogin', '마지막 로그인')}: ${user.lastLoginDatetime || '-'}`}</div>
          <div>{`${text('pages.access.user.roles', '역할')}: ${formatRoles(user.roleCodes, roleNames, (count) => `${text('pages.access.user.more', '외')} ${count}`)}`}</div>
        </Space>
      ) : null}
      {editable ? (
        <Button type="primary" loading={saving} onClick={() => void submit()}>
          {text('pages.org.save', '저장')}
        </Button>
      ) : null}
    </Form>
  );

  return (
    <Drawer
      open={open}
      size="large"
      title={user ? user.loginId : text('pages.access.user.create', '사용자 등록')}
      extra={
        user ? (
          <Space wrap>
            {actions.map((action) =>
              showAction(action) ? (
                <Button key={action} size="small" disabled={saving} onClick={() => onAction(action)}>
                  {text(actionText[action][0], actionText[action][1])}
                </Button>
              ) : null,
            )}
          </Space>
        ) : null
      }
      onClose={onClose}
    >
      <Tabs
        activeKey={user ? tab : 'basic'}
        onChange={onTab}
        items={[
          { key: 'basic', label: text('pages.access.user.tabBasic', '기본 정보'), children: basic },
          {
            key: 'roles',
            label: text('pages.access.user.tabRoles', '역할'),
            disabled: !user,
            children: user ? (
              <>
                {!canRoleRead ? <Alert type="info" showIcon title={text('pages.access.user.noRoleRead', '역할 조회 권한이 없어 역할 코드를 그대로 보여 주고 편집은 숨깁니다.')} style={{ marginBottom: 12 }} /> : null}
                <Space style={{ marginBottom: 12 }}>
                  <Select
                    allowClear
                    aria-label={text('pages.org.use', '사용 여부')}
                    placeholder={text('pages.org.useAll', '사용 여부 전체')}
                    style={{ minWidth: 140 }}
                    value={roleUse}
                    options={[
                      { value: 'Y', label: text('pages.org.inUse', '사용') },
                      { value: 'N', label: text('pages.org.stopped', '중지') },
                    ]}
                    onChange={(value) => {
                      setRolePage(1);
                      setRoleUse(value);
                    }}
                  />
                  {canUpdate && canRoleRead && !self && active ? (
                    <Button onClick={() => setGrantOpen(true)}>{text('pages.access.user.editRoles', '역할 편집')}</Button>
                  ) : null}
                </Space>
                <Table<UserRoleGrant> rowKey="userRolePk" columns={grantColumns} dataSource={grants} pagination={false} />
                <RoleGrantModal
                  open={grantOpen}
                  roles={roleOptions}
                  current={grants}
                  saving={grantSaving}
                  notice={grantNotice}
                  rowErrors={rowErrors}
                  title={text('pages.access.user.editRoles', '역할 편집')}
                  hint={text('pages.access.user.revokeHint', '선택에서 뺀 역할은 철회됩니다.')}
                  roleLabel={text('pages.access.user.role', '역할')}
                  fromLabel={text('pages.access.user.validFrom', '시작일')}
                  toLabel={text('pages.access.user.validTo', '종료일')}
                  dateError={text('pages.access.user.errors.roleDate', '시작일은 종료일보다 늦을 수 없습니다.')}
                  saveLabel={text('pages.org.save', '저장')}
                  cancelLabel={text('pages.org.cancel', '취소')}
                  onClose={() => setGrantOpen(false)}
                  onSave={(body) => void saveGrants(body)}
                />
              </>
            ) : null,
          },
        ]}
      />
    </Drawer>
  );
};

export default UserDrawer;
