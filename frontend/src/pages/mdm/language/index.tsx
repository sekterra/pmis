import { PageContainer } from '@ant-design/pro-components';
import { useAccess, useIntl, useSearchParams } from '@umijs/max';
import { Alert, App, Button, Select, Space, Table, Tabs, Tag } from 'antd';
import type { TableProps } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/core/request/errors';
import type { MenuAction } from '@/core/menu/types';
import {
  createNation,
  createSupportLanguage,
  deleteNation,
  deleteSupportLanguage,
  listNations,
  listSupportLanguages,
  restoreNation,
  restoreSupportLanguage,
  updateNation,
  updateSupportLanguage,
} from '@/services/mdm/languageAdmin';
import type { LanguageBody, LanguageRecord, NationBody, NationRecord } from '@/services/mdm/languageRecord';
import LanguageDrawer from './LanguageDrawer';
import NationDrawer from './NationDrawer';
import styles from './index.module.css';
import { grantLanguage } from './permission';
import { inUse, languageErrorText } from './rules';

const NATION_SORTS = new Set(['nationCode', 'nationName', 'sortOrder', 'nationPk']);

function sortParams(
  sorter: { field?: unknown; order?: 'ascend' | 'descend' | null } | Array<{ field?: unknown; order?: 'ascend' | 'descend' | null }>,
  allowed: Set<string>,
): string[] {
  const list = Array.isArray(sorter) ? sorter : [sorter];
  return list.flatMap((item) => {
    const field = typeof item.field === 'string' ? item.field : '';
    if (!field || !item.order || !allowed.has(field)) {
      return [];
    }
    return [`${field},${item.order === 'descend' ? 'desc' : 'asc'}`];
  });
}

const LanguagePage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) => intl.formatMessage({ id, defaultMessage });
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (action: MenuAction) => grantLanguage(access.can ?? (() => false), action);
  const [params, setParams] = useSearchParams();
  const tab = params.get('tab') === 'nation' ? 'nation' : 'language';

  const [notice, setNotice] = useState<string>();
  const [useYn, setUseYn] = useState<string>();
  const [languages, setLanguages] = useState<LanguageRecord[]>([]);
  const [allLanguages, setAllLanguages] = useState<LanguageRecord[]>([]);
  const [languageLoading, setLanguageLoading] = useState(false);
  const [nations, setNations] = useState<NationRecord[]>([]);
  const [nationTotal, setNationTotal] = useState(0);
  const [nationPage, setNationPage] = useState(1);
  const [nationSort, setNationSort] = useState<string[]>([]);
  const [nationLoading, setNationLoading] = useState(false);
  const [reload, setReload] = useState(0);
  const [saving, setSaving] = useState(false);
  const [languageOpen, setLanguageOpen] = useState(false);
  const [editingLanguage, setEditingLanguage] = useState<LanguageRecord>();
  const [nationOpen, setNationOpen] = useState(false);
  const [editingNation, setEditingNation] = useState<NationRecord>();

  const fail = useCallback((error: unknown) => {
    const api = error instanceof ApiError ? error : new ApiError('');
    if (api.errorCode === 'AUTH.FORBIDDEN') {
      setNotice(languageErrorText({ message: '', errorCode: 'AUTH.FORBIDDEN' }));
      return;
    }
    setNotice(languageErrorText(api));
  }, []);

  const chooseTab = (next: string) => {
    const query = new URLSearchParams(params);
    query.set('tab', next);
    setParams(query);
  };

  useEffect(() => {
    let alive = true;
    listSupportLanguages()
      .then((rows) => {
        if (alive) {
          setAllLanguages(rows);
        }
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, [reload]);

  useEffect(() => {
    if (tab !== 'language') {
      return;
    }
    let alive = true;
    setLanguageLoading(true);
    listSupportLanguages(useYn)
      .then((rows) => {
        if (!alive) {
          return;
        }
        setLanguages(rows);
        setNotice(undefined);
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      })
      .finally(() => {
        if (alive) {
          setLanguageLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [tab, useYn, reload, fail]);

  useEffect(() => {
    if (tab !== 'nation') {
      return;
    }
    let alive = true;
    setNationLoading(true);
    listNations({ page: nationPage, size: 20, sort: nationSort })
      .then((result) => {
        if (!alive) {
          return;
        }
        setNations(result.items);
        setNationTotal(result.total);
        setNotice(undefined);
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      })
      .finally(() => {
        if (alive) {
          setNationLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [tab, nationPage, nationSort, reload, fail]);

  const ask = (title: string, content: string, onOk: () => Promise<void>) => {
    modal.confirm({
      title,
      content,
      okText: text('pages.language.confirm', '확인'),
      cancelText: text('pages.language.cancel', '취소'),
      onOk,
    });
  };

  const persistLanguage = async (body: LanguageBody) => {
    setSaving(true);
    try {
      if (editingLanguage) {
        await updateSupportLanguage(editingLanguage.supportLanguageCode, body);
      } else {
        await createSupportLanguage(body);
      }
      setLanguageOpen(false);
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const saveLanguage = (body: LanguageBody) => {
    const becomingDefault = body.defaultLanguage && !editingLanguage?.defaultLanguage;
    if (!becomingDefault) {
      return persistLanguage(body);
    }
    ask(
      text('pages.language.defaultChange', '기본 언어를 바꿀까요?'),
      text('pages.language.defaultChangeHint', '기존 기본 언어가 해제된다'),
      () => persistLanguage(body),
    );
    return Promise.resolve();
  };

  const saveNation = async (body: NationBody) => {
    setSaving(true);
    try {
      if (editingNation) {
        await updateNation(editingNation.nationCode, body);
      } else {
        await createNation(body);
      }
      setNationOpen(false);
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const languageColumns: TableProps<LanguageRecord>['columns'] = [
    { title: text('pages.language.code', '언어 코드'), dataIndex: 'supportLanguageCode' },
    { title: text('pages.language.name', '언어명'), dataIndex: 'languageName' },
    { title: text('pages.language.nativeName', '현지 표기'), dataIndex: 'nativeName' },
    {
      title: text('pages.language.default', '기본'),
      dataIndex: 'defaultLanguage',
      render: (value: boolean) => (value ? <Tag>{text('pages.language.defaultTag', '기본')}</Tag> : null),
    },
    { title: text('pages.language.fallback', '대체 언어'), dataIndex: 'fallbackLanguageCode' },
    { title: text('pages.language.direction', '방향'), dataIndex: 'textDirectionCode' },
    { title: text('pages.language.sort', '정렬'), dataIndex: 'sortOrder' },
    { title: text('pages.language.use', '사용'), dataIndex: 'useYn' },
    {
      title: text('pages.language.actions', '동작'),
      key: 'actions',
      render: (_value, record) => (
        <Space>
          {can('UPDATE') ? (
            <Button
              type="link"
              onClick={() => {
                setEditingLanguage(record);
                setLanguageOpen(true);
              }}
            >
              {text('pages.language.editAction', '수정')}
            </Button>
          ) : null}
          {can('DELETE') && inUse(record.useYn) && !record.defaultLanguage ? (
            <Button
              type="link"
              onClick={() =>
                ask(
                  text('pages.language.disable', '사용 중지'),
                  text('pages.language.disableHint', '중지하면 번들 조회·언어 선택에서 빠진다'),
                  async () => {
                    try {
                      await deleteSupportLanguage(record.supportLanguageCode);
                      setReload((value) => value + 1);
                    } catch (error) {
                      fail(error);
                    }
                  },
                )
              }
            >
              {text('pages.language.disable', '사용 중지')}
            </Button>
          ) : null}
          {can('UPDATE') && !inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                restoreSupportLanguage(record.supportLanguageCode)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail);
              }}
            >
              {text('pages.language.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const nationColumns: TableProps<NationRecord>['columns'] = [
    { title: text('pages.language.nationCode', '국가 코드'), dataIndex: 'nationCode', sorter: true },
    { title: text('pages.language.nationName', '국가명'), dataIndex: 'nationName', sorter: true },
    { title: text('pages.language.defaultLanguage', '기본 언어'), dataIndex: 'defaultLanguageCode' },
    { title: text('pages.language.sort', '정렬'), dataIndex: 'sortOrder', sorter: true },
    { title: text('pages.language.use', '사용'), dataIndex: 'useYn' },
    {
      title: text('pages.language.actions', '동작'),
      key: 'actions',
      render: (_value, record) => (
        <Space>
          {can('UPDATE') ? (
            <Button
              type="link"
              onClick={() => {
                setEditingNation(record);
                setNationOpen(true);
              }}
            >
              {text('pages.language.editAction', '수정')}
            </Button>
          ) : null}
          {can('DELETE') && inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() =>
                ask(
                  text('pages.language.disableNation', '국가를 사용 중지할까요?'),
                  text('pages.language.disableNationHint', '삭제하면 사용 중지(use_yn=N)됩니다. 데이터는 지워지지 않습니다.'),
                  async () => {
                    try {
                      await deleteNation(record.nationCode);
                      setReload((value) => value + 1);
                    } catch (error) {
                      fail(error);
                    }
                  },
                )
              }
            >
              {text('pages.language.disable', '사용 중지')}
            </Button>
          ) : null}
          {can('UPDATE') && !inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                restoreNation(record.nationCode)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail);
              }}
            >
              {text('pages.language.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title={text('pages.language.title', '언어·국가 관리')}>
      {notice ? <Alert title={notice} type="warning" showIcon style={{ marginBottom: 16 }} /> : null}
      <Tabs
        activeKey={tab}
        onChange={chooseTab}
        items={[
          {
            key: 'language',
            label: text('pages.language.tab', '지원 언어'),
            children: (
              <section className={styles.panel}>
                <Space style={{ marginBottom: 16 }}>
                  <Select
                    aria-label={text('pages.language.use', '사용')}
                    value={useYn ?? ''}
                    style={{ width: 120 }}
                    onChange={(value) => setUseYn(value || undefined)}
                    options={[
                      { value: '', label: text('pages.language.all', '전체') },
                      { value: 'Y', label: 'Y' },
                      { value: 'N', label: 'N' },
                    ]}
                  />
                  {can('CREATE') ? (
                    <Button
                      type="primary"
                      onClick={() => {
                        setEditingLanguage(undefined);
                        setLanguageOpen(true);
                      }}
                    >
                      {text('pages.language.create', '언어 등록')}
                    </Button>
                  ) : null}
                </Space>
                <Table<LanguageRecord>
                  rowKey="supportLanguageCode"
                  loading={languageLoading}
                  columns={languageColumns}
                  dataSource={languages}
                  pagination={false}
                  rowClassName={(record) => (inUse(record.useYn) ? '' : styles.inactive)}
                />
              </section>
            ),
          },
          {
            key: 'nation',
            label: text('pages.language.nationTab', '국가'),
            children: (
              <section className={styles.panel}>
                <Space style={{ marginBottom: 16 }}>
                  {can('CREATE') ? (
                    <Button
                      type="primary"
                      onClick={() => {
                        setEditingNation(undefined);
                        setNationOpen(true);
                      }}
                    >
                      {text('pages.language.createNation', '국가 등록')}
                    </Button>
                  ) : null}
                </Space>
                <Table<NationRecord>
                  rowKey="nationCode"
                  loading={nationLoading}
                  columns={nationColumns}
                  dataSource={nations}
                  onChange={(pagination, _filters, sorter) => {
                    setNationPage(pagination.current ?? 1);
                    setNationSort(sortParams(sorter, NATION_SORTS));
                  }}
                  pagination={{
                    current: nationPage,
                    pageSize: 20,
                    total: nationTotal,
                    showSizeChanger: false,
                  }}
                  rowClassName={(record) => (inUse(record.useYn) ? '' : styles.inactive)}
                />
              </section>
            ),
          },
        ]}
      />
      <LanguageDrawer
        open={languageOpen}
        saving={saving}
        language={editingLanguage}
        languages={allLanguages}
        onClose={() => setLanguageOpen(false)}
        onSubmit={saveLanguage}
      />
      <NationDrawer
        open={nationOpen}
        saving={saving}
        nation={editingNation}
        languages={allLanguages}
        onClose={() => setNationOpen(false)}
        onSubmit={saveNation}
      />
    </PageContainer>
  );
};

export default LanguagePage;
