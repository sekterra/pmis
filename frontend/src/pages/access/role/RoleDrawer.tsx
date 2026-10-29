import { useIntl } from '@umijs/max';
import { Alert, App, Button, Checkbox, Drawer, Form, Input, InputNumber, Space, Table, Tabs } from 'antd';
import React, { useEffect, useRef, useState } from 'react';
import { ApiError } from '@/core/request/errors';
import NameTranslations from '@/pages/org/shared/NameTranslations';
import type { SupportLanguage } from '@/services/mdm/data';
import { listRolePermissions, replaceRolePermissions } from '@/services/mdm/roles';
import { namesFromRole, toCreateBody, toUpdateBody } from '@/services/mdm/rolesQuery';
import type { Role, RoleCreateBody, RoleUpdateBody } from '@/services/mdm/rolesRecord';
import {
  buildMatrix,
  cloneMatrix,
  grantedCodes,
  isDirty,
  setGranted,
  toggleMenu,
  type PermissionMatrix,
} from './matrix';
import styles from './index.module.css';
import { readRequiredMenus, roleErrorText, roleFormErrors, validRoleCode } from './rules';

type Props = {
  open: boolean;
  role?: Role;
  tab: string;
  saving: boolean;
  languages: SupportLanguage[];
  canUpdate: boolean;
  onTab: (tab: string) => void;
  onClose: () => void;
  onSubmit: (body: RoleCreateBody | RoleUpdateBody) => Promise<void>;
  onNotice: (message: string) => void;
};

const RoleDrawer: React.FC<Props> = ({
  open,
  role,
  tab,
  saving,
  languages,
  canUpdate,
  onTab,
  onClose,
  onSubmit,
  onNotice,
}) => {
  const intl = useIntl();
  const text = (id: string, fallback: string) => intl.formatMessage({ id, defaultMessage: fallback });
  const { modal } = App.useApp();
  const [form] = Form.useForm();
  const [matrix, setMatrix] = useState<PermissionMatrix>();
  const [baseline, setBaseline] = useState<PermissionMatrix>();
  const [menuQuery, setMenuQuery] = useState('');
  const [menuErrors, setMenuErrors] = useState<Record<string, string>>({});
  const [permSaving, setPermSaving] = useState(false);
  const [savedHint, setSavedHint] = useState(false);
  const [permReload, setPermReload] = useState(0);
  const loadedKey = useRef('');
  const noticeRef = useRef(onNotice);
  const textRef = useRef(text);
  noticeRef.current = onNotice;
  textRef.current = text;
  const protectedRole = role?.roleCode === 'TENANT_ADMIN';
  const inactive = role?.useYn === 'N';
  const readOnly = !canUpdate || protectedRole || inactive;
  const dirty = Boolean(matrix && baseline && isDirty(matrix, baseline));

  useEffect(() => {
    if (!open) {
      loadedKey.current = '';
      setMatrix(undefined);
      setBaseline(undefined);
      setMenuQuery('');
      setMenuErrors({});
      setSavedHint(false);
      return;
    }
    form.setFieldsValue({
      roleCode: role?.roleCode,
      roleName: role?.roleName,
      sortOrder: role?.sortOrder ?? undefined,
      description: role?.description ?? undefined,
      names: namesFromRole(role),
    });
  }, [open, role, form]);

  useEffect(() => {
    if (!open || tab !== 'permissions' || !role) {
      return;
    }
    const key = `${role.rolePk}:${permReload}`;
    if (loadedKey.current === key) {
      return;
    }
    loadedKey.current = key;
    const rolePk = role.rolePk;
    let alive = true;
    listRolePermissions(rolePk)
      .then((items) => {
        if (!alive || loadedKey.current !== key) {
          return;
        }
        const built = buildMatrix(items);
        setMatrix(built);
        setBaseline(cloneMatrix(built));
        setMenuErrors({});
      })
      .catch((error: unknown) => {
        if (alive && loadedKey.current === key && error instanceof ApiError) {
          noticeRef.current(roleErrorText(error, textRef.current));
        }
      });
    return () => {
      alive = false;
    };
  }, [open, tab, role?.rolePk, permReload]);

  const readOnlyReason = () => {
    if (!canUpdate) {
      return text('pages.access.role.readOnlyUpdate', '권한을 수정할 수 없습니다.');
    }
    if (protectedRole) {
      return text('pages.access.role.readOnlyProtected', '테넌트 관리자 역할의 권한은 바꿀 수 없습니다.');
    }
    if (inactive) {
      return text('pages.access.role.readOnlyInactive', '사용 중지된 역할의 권한은 바꿀 수 없습니다.');
    }
    return '';
  };

  const actionLabel = (action: string) => {
    const fallback: Record<string, string> = {
      READ: '조회',
      CREATE: '등록',
      UPDATE: '수정',
      DELETE: '삭제',
      EXPORT: '내보내기',
      APPROVE: '승인',
    };
    return text(`pages.access.role.action.${action}`, fallback[action] ?? action);
  };

  const askDiscard = (then: () => void) => {
    modal.confirm({
      title: text('pages.access.role.discardTitle', '변경을 버릴까요?'),
      content: text('pages.access.role.discardHint', '저장하지 않은 권한 변경이 있습니다.'),
      okText: text('pages.access.role.discardOk', '버리기'),
      onOk: then,
    });
  };

  const changeTab = (next: string) => {
    if (!role && next !== 'basic') {
      return;
    }
    if (dirty) {
      askDiscard(() => {
        if (baseline) {
          setMatrix(cloneMatrix(baseline));
        }
        setMenuErrors({});
        onTab(next);
      });
      return;
    }
    onTab(next);
  };

  const close = () => {
    if (dirty) {
      askDiscard(onClose);
      return;
    }
    onClose();
  };

  const submit = async () => {
    let values: Parameters<typeof toCreateBody>[0];
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    const body = role ? toUpdateBody(values) : toCreateBody(values);
    try {
      await onSubmit(body);
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      const fields = roleFormErrors(api, text);
      if (fields.length > 0) {
        form.setFields(fields);
        return;
      }
      onNotice(roleErrorText(api, text));
    }
  };

  const savePermissions = async () => {
    if (!role || !matrix || !dirty || readOnly) {
      return;
    }
    setPermSaving(true);
    setSavedHint(false);
    try {
      await replaceRolePermissions(role.rolePk, { permissionCodes: grantedCodes(matrix) });
      setPermReload((value) => value + 1);
      setSavedHint(true);
    } catch (error) {
      const api = error instanceof ApiError ? error : new ApiError('');
      const menus = readRequiredMenus(api);
      if (menus.length > 0) {
        const message = roleErrorText(api, text);
        setMenuErrors(Object.fromEntries(menus.map((menu) => [menu, message])));
        return;
      }
      onNotice(roleErrorText(api, text));
    } finally {
      setPermSaving(false);
    }
  };

  const visibleRows = (matrix?.rows ?? []).filter((row) => {
    const query = menuQuery.trim().toLowerCase();
    return !query || row.menuName.toLowerCase().includes(query);
  });

  return (
    <Drawer
      title={role ? role.roleCode : text('pages.access.role.create', '역할 등록')}
      open={open}
      size="large"
      onClose={close}
      extra={
        <Space>
          <Button onClick={close}>{text('pages.org.cancel', '취소')}</Button>
          {tab === 'basic' ? (
            <Button type="primary" loading={saving} disabled={saving} onClick={() => void submit()}>
              {text('pages.org.save', '저장')}
            </Button>
          ) : null}
        </Space>
      }
    >
      {protectedRole ? <Alert type="info" showIcon title={text('pages.access.role.protectedHint', '테넌트 관리자 역할은 삭제할 수 없습니다. 이름과 설명은 수정할 수 있습니다.')} style={{ marginBottom: 16 }} /> : null}
      {!role ? <p>{text('pages.access.role.afterSave', '저장한 뒤에 권한을 설정할 수 있습니다.')}</p> : null}
      <Tabs
        activeKey={role ? tab : 'basic'}
        onChange={changeTab}
        items={[
          {
            key: 'basic',
            label: text('pages.access.role.tabBasic', '기본 정보'),
            children: (
              <Form form={form} layout="vertical">
                <Form.Item
                  name="roleCode"
                  label={text('pages.access.role.code', '역할 코드')}
                  rules={[
                    { required: !role, message: text('pages.access.role.codeRule', '대문자로 시작하고 대문자, 숫자, 밑줄만 2~50자입니다.') },
                    {
                      validator: async (_, value: string | undefined) => {
                        if (role || !value || validRoleCode(value)) {
                          return;
                        }
                        throw new Error(text('pages.access.role.codeRule', '대문자로 시작하고 대문자, 숫자, 밑줄만 2~50자입니다.'));
                      },
                    },
                  ]}
                >
                  <Input maxLength={50} disabled={Boolean(role)} aria-label={text('pages.access.role.code', '역할 코드')} />
                </Form.Item>
                <Form.Item name="roleName" label={text('pages.access.role.name', '역할명')} rules={[{ required: true, whitespace: true, message: text('pages.access.role.name', '역할명') }]}>
                  <Input maxLength={200} aria-label={text('pages.access.role.name', '역할명')} />
                </Form.Item>
                <Form.Item name="sortOrder" label={text('pages.org.company.sortOrder', '정렬 순서')}>
                  <InputNumber aria-label={text('pages.org.company.sortOrder', '정렬 순서')} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="description" label={text('pages.access.role.description', '설명')}>
                  <Input.TextArea maxLength={1000} aria-label={text('pages.access.role.description', '설명')} />
                </Form.Item>
                <NameTranslations languages={languages} />
              </Form>
            ),
          },
          {
            key: 'permissions',
            label: text('pages.access.role.tabPermission', '권한'),
            disabled: !role,
            children: matrix ? (
              <div>
                {readOnly ? <Alert type="info" showIcon title={readOnlyReason()} style={{ marginBottom: 12 }} /> : null}
                {savedHint ? <Alert type="success" showIcon title={text('pages.access.role.savedHint', '권한은 즉시 반영되지만 화면 메뉴는 다음 로그인 또는 새로고침 때 바뀝니다.')} style={{ marginBottom: 12 }} /> : null}
                <Input
                  allowClear
                  aria-label={text('pages.access.role.menuFilter', '메뉴 이름')}
                  placeholder={text('pages.access.role.menuFilter', '메뉴 이름')}
                  value={menuQuery}
                  onChange={(event) => setMenuQuery(event.target.value)}
                  style={{ marginBottom: 12, maxWidth: 240 }}
                />
                <Table<PermissionMatrix['rows'][number]>
                  rowKey="menuCode"
                  pagination={false}
                  dataSource={visibleRows}
                  columns={[
                    {
                      title: text('pages.access.role.menuFilter', '메뉴 이름'),
                      render: (_, row) => (
                        <div>
                          <Button type="link" disabled={readOnly} onClick={() => setMatrix(toggleMenu(matrix, row.menuCode))}>
                            {row.menuName}
                          </Button>
                          {menuErrors[row.menuCode] ? <div>{menuErrors[row.menuCode]}</div> : null}
                        </div>
                      ),
                    },
                    ...matrix.columns.map((action) => ({
                      title: actionLabel(action),
                      render: (_: unknown, row: PermissionMatrix['rows'][number]) => {
                        const cell = row.cells[action];
                        if (!cell) {
                          return null;
                        }
                        return (
                          <Checkbox
                            checked={cell.granted}
                            disabled={readOnly}
                            onChange={(event) => {
                              setSavedHint(false);
                              setMatrix(setGranted(matrix, row.menuCode, action, event.target.checked));
                            }}
                          >
                            <span className={styles.srOnly}>{`${row.menuName} ${actionLabel(action)}`}</span>
                          </Checkbox>
                        );
                      },
                    })),
                  ]}
                />
                <Space style={{ marginTop: 16 }}>
                  <Button onClick={() => baseline && setMatrix(cloneMatrix(baseline))} disabled={!dirty || readOnly}>
                    {text('pages.access.role.revert', '되돌리기')}
                  </Button>
                  <Button type="primary" loading={permSaving} disabled={!dirty || readOnly || permSaving} onClick={() => void savePermissions()}>
                    {text('pages.access.role.savePermissions', '권한 저장')}
                  </Button>
                </Space>
              </div>
            ) : null,
          },
        ]}
      />
    </Drawer>
  );
};

export default RoleDrawer;
