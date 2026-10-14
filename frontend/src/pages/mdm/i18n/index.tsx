import { PageContainer } from '@ant-design/pro-components';
import { useAccess, useIntl, useSearchParams } from '@umijs/max';
import { Alert, App, Button, Descriptions, Empty, Input, Select, Space, Table } from 'antd';
import type { TableProps } from 'antd';
import React, { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/core/request/errors';
import type { MenuAction } from '@/core/menu/types';
import { listCommonCodes } from '@/services/mdm/codes';
import type { SupportLanguage } from '@/services/mdm/data';
import {
  createMessage,
  createNamespace,
  deleteMessage,
  deleteNamespace,
  getMessage,
  getNamespace,
  listMessages,
  listNamespaces,
  restoreMessage,
  restoreNamespace,
  updateMessage,
  updateNamespace,
} from '@/services/mdm/i18nAdmin';
import type { MessageBody, MessageRecord, NamespaceBody, NamespaceRecord } from '@/services/mdm/i18nRecord';
import { fetchLanguages } from '@/services/mdm/i18n';
import MessageFormDrawer from './MessageFormDrawer';
import NamespaceFormDrawer from './NamespaceFormDrawer';
import styles from './index.module.css';
import { grantI18n } from './permission';
import { i18nErrorText, inUse } from './rules';

type Filters = {
  keyword?: string;
  useYn?: string;
};

type MessageFilters = Filters & {
  messageTypeCode?: string;
  translationStatusCode?: string;
};

type Option = { value: string; label: string };

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

const I18nPage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) => intl.formatMessage({ id, defaultMessage });
  const { modal } = App.useApp();
  const access = useAccess() as { can?: (menuCode: string, action: MenuAction) => boolean };
  const can = (action: MenuAction) => grantI18n(access.can ?? (() => false), action);
  const [params, setParams] = useSearchParams();
  const selectedCode = params.get('ns') ?? undefined;

  const [languages, setLanguages] = useState<SupportLanguage[]>([]);
  const [messageTypes, setMessageTypes] = useState<Option[]>([]);
  const [translationStatuses, setTranslationStatuses] = useState<Option[]>([]);
  const [notice, setNotice] = useState<string>();
  const [namespaceFilters, setNamespaceFilters] = useState<Filters>({});
  const [namespaceDraft, setNamespaceDraft] = useState<Filters>({});
  const [namespaces, setNamespaces] = useState<NamespaceRecord[]>([]);
  const [namespaceTotal, setNamespaceTotal] = useState(0);
  const [namespacePage, setNamespacePage] = useState(1);
  const [namespaceSort, setNamespaceSort] = useState<string[]>([]);
  const [namespaceLoading, setNamespaceLoading] = useState(false);
  const [selected, setSelected] = useState<NamespaceRecord>();
  const [messageFilters, setMessageFilters] = useState<MessageFilters>({});
  const [messageDraft, setMessageDraft] = useState<MessageFilters>({});
  const [messages, setMessages] = useState<MessageRecord[]>([]);
  const [messageTotal, setMessageTotal] = useState(0);
  const [messagePage, setMessagePage] = useState(1);
  const [messageSort, setMessageSort] = useState<string[]>([]);
  const [messageLoading, setMessageLoading] = useState(false);
  const [namespaceOpen, setNamespaceOpen] = useState(false);
  const [editingNamespace, setEditingNamespace] = useState<NamespaceRecord>();
  const [messageOpen, setMessageOpen] = useState(false);
  const [editingMessage, setEditingMessage] = useState<MessageRecord>();
  const [saving, setSaving] = useState(false);
  const [reload, setReload] = useState(0);

  const fail = useCallback((error: unknown) => {
    const api = error instanceof ApiError ? error : new ApiError('');
    if (api.errorCode === 'AUTH.FORBIDDEN') {
      setNotice(i18nErrorText({ message: '', errorCode: 'AUTH.FORBIDDEN' }));
      return;
    }
    setNotice(i18nErrorText(api));
  }, []);

  useEffect(() => {
    let alive = true;
    fetchLanguages()
      .then((rows) => {
        if (alive) {
          setLanguages(rows);
        }
      })
      .catch(() => undefined);
    listCommonCodes({ groupCode: 'I18N_MESSAGE_TYPE', page: 1, size: 200, useYn: 'Y' })
      .then((result) => {
        if (alive) {
          setMessageTypes(result.items.map((item) => ({ value: item.codeValue, label: item.codeName })));
        }
      })
      .catch(() => undefined);
    listCommonCodes({ groupCode: 'I18N_TRANSLATION_STATUS', page: 1, size: 200, useYn: 'Y' })
      .then((result) => {
        if (alive) {
          setTranslationStatuses(
            result.items.map((item) => ({ value: item.codeValue, label: item.codeName })),
          );
        }
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    let alive = true;
    setNamespaceLoading(true);
    listNamespaces({
      page: namespacePage,
      size: 20,
      keyword: namespaceFilters.keyword,
      useYn: namespaceFilters.useYn,
      sort: namespaceSort,
    })
      .then((result) => {
        if (!alive) {
          return;
        }
        setNamespaces(result.items);
        setNamespaceTotal(result.total);
        setNotice(undefined);
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      })
      .finally(() => {
        if (alive) {
          setNamespaceLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [namespacePage, namespaceFilters, namespaceSort, reload, fail]);

  useEffect(() => {
    if (!selectedCode) {
      setSelected(undefined);
      setMessages([]);
      return;
    }
    let alive = true;
    getNamespace(selectedCode)
      .then((namespace) => {
        if (alive) {
          setSelected(namespace);
        }
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      });
    setMessageLoading(true);
    listMessages({
      namespaceCode: selectedCode,
      page: messagePage,
      size: 20,
      keyword: messageFilters.keyword,
      useYn: messageFilters.useYn,
      messageTypeCode: messageFilters.messageTypeCode,
      translationStatusCode: messageFilters.translationStatusCode,
      sort: messageSort,
    })
      .then((result) => {
        if (!alive) {
          return;
        }
        setMessages(result.items);
        setMessageTotal(result.total);
      })
      .catch((error: unknown) => {
        if (alive) {
          fail(error);
        }
      })
      .finally(() => {
        if (alive) {
          setMessageLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [selectedCode, messagePage, messageFilters, messageSort, reload, fail]);

  const chooseNamespace = (namespaceCode: string) => {
    const next = new URLSearchParams(params);
    next.set('ns', namespaceCode);
    setParams(next);
    setMessagePage(1);
  };

  const saveNamespace = async (body: NamespaceBody) => {
    setSaving(true);
    try {
      if (editingNamespace) {
        await updateNamespace(editingNamespace.namespaceCode, body);
      } else {
        await createNamespace(body);
      }
      setNamespaceOpen(false);
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const saveMessage = async (body: MessageBody) => {
    setSaving(true);
    try {
      if (editingMessage) {
        await updateMessage(editingMessage.messagePk, body);
      } else {
        await createMessage(body);
      }
      setMessageOpen(false);
      setReload((value) => value + 1);
    } finally {
      setSaving(false);
    }
  };

  const ask = (title: string, content: string, okText: string, onOk: () => Promise<void>) => {
    modal.confirm({
      title,
      content,
      okText,
      cancelText: text('pages.i18n.cancel', '취소'),
      onOk,
    });
  };

  const namespaceColumns: TableProps<NamespaceRecord>['columns'] = [
    {
      title: text('pages.i18n.namespaceCode', '네임스페이스 코드'),
      dataIndex: 'namespaceCode',
      sorter: true,
    },
    {
      title: text('pages.i18n.name', '이름'),
      dataIndex: 'namespaceName',
      sorter: true,
    },
    { title: text('pages.i18n.use', '사용 여부'), dataIndex: 'useYn' },
    {
      title: text('pages.i18n.actions', '작업'),
      key: 'actions',
      render: (_value, record) => (
        <Space>
          {can('UPDATE') ? (
            <Button
              type="link"
              onClick={() => {
                setEditingNamespace(record);
                setNamespaceOpen(true);
              }}
            >
              {text('pages.i18n.edit', '수정')}
            </Button>
          ) : null}
          {can('DELETE') && inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() =>
                ask(
                  text('pages.i18n.disableNamespace', '네임스페이스를 사용 중지할까요?'),
                  text(
                    'pages.i18n.disableNamespaceHint',
                    '사용 중지하면 메시지는 남고 번들 조회에서는 빠집니다.',
                  ),
                  text('pages.i18n.disable', '사용 중지'),
                  async () => {
                    await deleteNamespace(record.namespaceCode);
                    setReload((value) => value + 1);
                  },
                )
              }
            >
              {text('pages.i18n.disable', '사용 중지')}
            </Button>
          ) : null}
          {can('UPDATE') && !inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                restoreNamespace(record.namespaceCode)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail);
              }}
            >
              {text('pages.i18n.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const messageColumns: TableProps<MessageRecord>['columns'] = [
    {
      title: text('pages.i18n.messageKey', '메시지 키'),
      dataIndex: 'messageKey',
      sorter: true,
    },
    { title: text('pages.i18n.defaultMessage', '기본 문구'), dataIndex: 'defaultMessage' },
    {
      title: text('pages.i18n.messageType', '메시지 유형'),
      dataIndex: 'messageTypeCode',
      sorter: true,
    },
    {
      title: text('pages.i18n.translationSummary', '번역 상태'),
      key: 'translationSummary',
      render: () => text('pages.i18n.translationUnknown', '—'),
    },
    { title: text('pages.i18n.use', '사용 여부'), dataIndex: 'useYn' },
    {
      title: text('pages.i18n.actions', '작업'),
      key: 'actions',
      render: (_value, record) => (
        <Space>
          {can('UPDATE') ? (
            <Button
              type="link"
              onClick={() => {
                getMessage(record.messagePk)
                  .then((message) => {
                    setEditingMessage(message);
                    setMessageOpen(true);
                  })
                  .catch(fail);
              }}
            >
              {text('pages.i18n.edit', '수정')}
            </Button>
          ) : null}
          {can('DELETE') && inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() =>
                ask(
                  text('pages.i18n.disableMessage', '메시지를 사용 중지할까요?'),
                  text(
                    'pages.i18n.disableMessageHint',
                    '삭제하면 사용 중지(use_yn=N)됩니다. 데이터는 지워지지 않습니다.',
                  ),
                  text('pages.i18n.disable', '사용 중지'),
                  async () => {
                    await deleteMessage(record.messagePk);
                    setReload((value) => value + 1);
                  },
                )
              }
            >
              {text('pages.i18n.disable', '사용 중지')}
            </Button>
          ) : null}
          {can('UPDATE') && !inUse(record.useYn) ? (
            <Button
              type="link"
              onClick={() => {
                restoreMessage(record.messagePk)
                  .then(() => setReload((value) => value + 1))
                  .catch(fail);
              }}
            >
              {text('pages.i18n.restore', '복구')}
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <PageContainer title={text('pages.i18n.title', '다국어')}>
      {notice ? <Alert type="error" showIcon title={notice} /> : null}
      <div className={styles.layout}>
        <section className={`${styles.panel} ${styles.namespaces}`}>
          <Space orientation="vertical" style={{ width: '100%' }}>
            <Input
              allowClear
              placeholder={text('pages.i18n.keyword', '검색')}
              value={namespaceDraft.keyword}
              onChange={(event) =>
                setNamespaceDraft((current) => ({ ...current, keyword: event.target.value }))
              }
            />
            <Space>
              <Select
                allowClear
                placeholder={text('pages.i18n.use', '사용 여부')}
                style={{ width: 140 }}
                value={namespaceDraft.useYn}
                onChange={(value) => setNamespaceDraft((current) => ({ ...current, useYn: value }))}
                options={[
                  { value: 'Y', label: 'Y' },
                  { value: 'N', label: 'N' },
                ]}
              />
              <Button
                onClick={() => {
                  setNamespacePage(1);
                  setNamespaceFilters(namespaceDraft);
                }}
              >
                {text('pages.i18n.search', '조회')}
              </Button>
              {can('CREATE') ? (
                <Button
                  type="primary"
                  onClick={() => {
                    setEditingNamespace(undefined);
                    setNamespaceOpen(true);
                  }}
                >
                  {text('pages.i18n.createNamespace', '네임스페이스 등록')}
                </Button>
              ) : null}
            </Space>
            <Table<NamespaceRecord>
              rowKey="namespaceCode"
              loading={namespaceLoading}
              columns={namespaceColumns}
              dataSource={namespaces}
              rowClassName={(record) => (inUse(record.useYn) ? '' : styles.inactive)}
              rowSelection={{
                type: 'radio',
                selectedRowKeys: selectedCode ? [selectedCode] : [],
                onChange: (keys) => {
                  const key = keys[0];
                  if (typeof key === 'string') {
                    chooseNamespace(key);
                  }
                },
              }}
              onRow={(record) => ({
                onClick: () => chooseNamespace(record.namespaceCode),
              })}
              onChange={(pagination, _filters, sorter) => {
                setNamespacePage(pagination.current ?? 1);
                setNamespaceSort(sortParams(sorter));
              }}
              pagination={{
                current: namespacePage,
                pageSize: 20,
                total: namespaceTotal,
                showSizeChanger: false,
              }}
            />
          </Space>
        </section>
        <section className={`${styles.panel} ${styles.messages}`}>
          {selected ? (
            <Space orientation="vertical" style={{ width: '100%' }}>
              <Descriptions
                column={2}
                items={[
                  {
                    key: 'code',
                    label: text('pages.i18n.namespaceCode', '네임스페이스 코드'),
                    children: selected.namespaceCode,
                  },
                  {
                    key: 'name',
                    label: text('pages.i18n.name', '이름'),
                    children: selected.namespaceName,
                  },
                  {
                    key: 'description',
                    label: text('pages.i18n.description', '설명'),
                    children: selected.description ?? '-',
                  },
                ]}
              />
              <Input
                allowClear
                placeholder={text('pages.i18n.keyword', '검색')}
                value={messageDraft.keyword}
                onChange={(event) =>
                  setMessageDraft((current) => ({ ...current, keyword: event.target.value }))
                }
              />
              <Space wrap>
                <Select
                  allowClear
                  placeholder={text('pages.i18n.messageType', '메시지 유형')}
                  style={{ width: 160 }}
                  value={messageDraft.messageTypeCode}
                  onChange={(value) =>
                    setMessageDraft((current) => ({ ...current, messageTypeCode: value }))
                  }
                  options={messageTypes}
                />
                <Select
                  allowClear
                  placeholder={text('pages.i18n.translationStatus', '번역 상태')}
                  style={{ width: 160 }}
                  value={messageDraft.translationStatusCode}
                  onChange={(value) =>
                    setMessageDraft((current) => ({ ...current, translationStatusCode: value }))
                  }
                  options={translationStatuses}
                />
                <Select
                  allowClear
                  placeholder={text('pages.i18n.use', '사용 여부')}
                  style={{ width: 140 }}
                  value={messageDraft.useYn}
                  onChange={(value) => setMessageDraft((current) => ({ ...current, useYn: value }))}
                  options={[
                    { value: 'Y', label: 'Y' },
                    { value: 'N', label: 'N' },
                  ]}
                />
                <Button
                  onClick={() => {
                    setMessagePage(1);
                    setMessageFilters(messageDraft);
                  }}
                >
                  {text('pages.i18n.search', '조회')}
                </Button>
                {can('CREATE') ? (
                  <Button
                    type="primary"
                    onClick={() => {
                      setEditingMessage(undefined);
                      setMessageOpen(true);
                    }}
                  >
                    {text('pages.i18n.createMessage', '메시지 등록')}
                  </Button>
                ) : null}
              </Space>
              <Table<MessageRecord>
                rowKey="messagePk"
                loading={messageLoading}
                columns={messageColumns}
                dataSource={messages}
                rowClassName={(record) => (inUse(record.useYn) ? '' : styles.inactive)}
                onChange={(pagination, _filters, sorter) => {
                  setMessagePage(pagination.current ?? 1);
                  setMessageSort(sortParams(sorter));
                }}
                pagination={{
                  current: messagePage,
                  pageSize: 20,
                  total: messageTotal,
                  showSizeChanger: false,
                }}
              />
            </Space>
          ) : (
            <Empty description={text('pages.i18n.pickNamespace', '왼쪽에서 네임스페이스를 선택하세요.')} />
          )}
        </section>
      </div>
      <NamespaceFormDrawer
        open={namespaceOpen}
        saving={saving}
        namespace={editingNamespace}
        onClose={() => setNamespaceOpen(false)}
        onSubmit={saveNamespace}
      />
      {selected ? (
        <MessageFormDrawer
          open={messageOpen}
          saving={saving}
          namespaceCode={selected.namespaceCode}
          message={editingMessage}
          languages={languages}
          messageTypes={messageTypes}
          translationStatuses={translationStatuses}
          onClose={() => setMessageOpen(false)}
          onSubmit={saveMessage}
        />
      ) : null}
    </PageContainer>
  );
};

export default I18nPage;
