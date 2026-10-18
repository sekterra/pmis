import { useIntl } from '@umijs/max';
import { Alert, Drawer, Table } from 'antd';
import type { TableProps } from 'antd';
import React, { useEffect, useState } from 'react';
import { ApiError } from '@/core/request/errors';
import type { CodeHistory } from '@/services/mdm/code';
import { listCodeHistory } from '@/services/mdm/codes';
import { codeErrorText, historyPairs } from './rules';

type Props = {
  codePk?: number;
  onClose: () => void;
};

const CHANGE_LABEL: Record<string, string> = {
  C: 'pages.mdm.changeCreate',
  U: 'pages.mdm.changeUpdate',
  D: 'pages.mdm.changeDelete',
};

const HistoryDrawer: React.FC<Props> = ({ codePk, onClose }) => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });
  const [rows, setRows] = useState<CodeHistory[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string>();

  useEffect(() => {
    if (!codePk) {
      return;
    }
    let alive = true;
    setLoading(true);
    listCodeHistory(codePk, { page, size: 20 })
      .then((result) => {
        if (!alive) {
          return;
        }
        setRows(result.items);
        setTotal(result.total);
        setMessage(undefined);
      })
      .catch((error: unknown) => {
        if (!alive) {
          return;
        }
        const api = error instanceof ApiError ? error : new ApiError('');
        setMessage(codeErrorText(api));
      })
      .finally(() => {
        if (alive) {
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [codePk, page]);

  const columns: TableProps<CodeHistory>['columns'] = [
    {
      title: text('pages.mdm.changedAt', '변경 시각'),
      dataIndex: 'changeDatetime',
      render: (value: string | null) => value || '-',
    },
    {
      title: text('pages.mdm.changeType', '변경 유형'),
      dataIndex: 'changeTypeCode',
      render: (value: string) => {
        const id = CHANGE_LABEL[value];
        if (!id) {
          return value;
        }
        const fallback = { C: '등록', U: '수정', D: '삭제' }[value] ?? value;
        return text(id, fallback);
      },
    },
    {
      title: text('pages.mdm.reason', '변경 사유'),
      dataIndex: 'changeReason',
      render: (value: string | null) => value || '-',
    },
    {
      title: text('pages.mdm.changeUser', '변경 사용자'),
      dataIndex: 'changeUserPk',
      render: (value: number | null) => (value == null ? '-' : String(value)),
    },
  ];

  return (
    <Drawer
      title={text('pages.mdm.history', '변경 이력')}
      open={codePk != null}
      size={720}
      destroyOnHidden
      onClose={onClose}
    >
      {message ? <Alert type="error" showIcon title={message} /> : null}
      <Table<CodeHistory>
        rowKey="historyPk"
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{
          current: page,
          pageSize: 20,
          total,
          showSizeChanger: false,
          onChange: setPage,
        }}
        expandable={{
          expandedRowRender: (record) => (
            <Table
              rowKey="key"
              pagination={false}
              dataSource={historyPairs(record.beforeData, record.afterData)}
              columns={[
                { title: text('pages.mdm.field', '항목'), dataIndex: 'key' },
                { title: text('pages.mdm.before', '변경 전'), dataIndex: 'before' },
                { title: text('pages.mdm.after', '변경 후'), dataIndex: 'after' },
              ]}
            />
          ),
        }}
      />
    </Drawer>
  );
};

export default HistoryDrawer;
