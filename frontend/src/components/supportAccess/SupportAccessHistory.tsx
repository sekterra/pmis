import type { ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { useIntl } from '@umijs/max';
import { Button, Typography } from 'antd';
import React, { useMemo, useState } from 'react';
import { SUPPORT_ACCESS_STATUSES } from '@/core/supportAccess/rules';
import {
  expandDateRangeParams,
  toTableRequest,
} from '@/core/request/table';
import type {
  PageResult,
  SupportAccessItem,
  SupportAccessListQuery,
} from '@/services/platform/data';
import SupportAccessDrawer from './SupportAccessDrawer';

type SupportAccessHistoryProps = {
  mode: 'platform' | 'tenant';
  list: (
    query: SupportAccessListQuery,
  ) => Promise<PageResult<SupportAccessItem>>;
  listActions: (
    id: number,
    page: number,
    size: number,
  ) => Promise<
    PageResult<{
      actionId: number;
      occurredDatetime: string;
      httpMethod: string;
      path: string;
      statusCode: number | null;
      sensitive: boolean;
    }>
  >;
  hint?: string;
};

const SupportAccessHistory: React.FC<SupportAccessHistoryProps> = ({
  mode,
  list,
  listActions,
  hint,
}) => {
  const intl = useIntl();
  const [selected, setSelected] = useState<SupportAccessItem | null>(null);
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });

  const statusEnum = useMemo(
    () =>
      Object.fromEntries(
        SUPPORT_ACCESS_STATUSES.map((status) => [
          status,
          {
            text: text(`pages.supportAccess.status.${status}`, status),
            status:
              status === 'ACTIVE'
                ? ('Processing' as const)
                : status === 'ISSUED'
                  ? ('Default' as const)
                  : status === 'EXPIRED'
                    ? ('Warning' as const)
                    : status === 'REVOKED'
                      ? ('Error' as const)
                      : ('Default' as const),
          },
        ]),
      ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [intl],
  );

  const columns: ProColumns<SupportAccessItem>[] = [
    ...(mode === 'platform'
      ? ([
          {
            title: text('pages.supportAccess.tenant', '테넌트'),
            dataIndex: 'tenantCode',
            render: (_: unknown, record: SupportAccessItem) => (
              <Button type="link" onClick={() => setSelected(record)}>
                {record.tenantCode}
              </Button>
            ),
          },
        ] as ProColumns<SupportAccessItem>[])
      : []),
    {
      title: text('pages.supportAccess.operator', '운영자'),
      dataIndex: 'platformLoginId',
      hideInTable: true,
      fieldProps: {
        'aria-label': text('pages.supportAccess.operator', '운영자'),
      },
    },
    {
      title: text('pages.supportAccess.operator', '운영자'),
      dataIndex: 'operatorLoginId',
      search: false,
      render: (_, record) => (
        <Button type="link" onClick={() => setSelected(record)}>
          {record.operatorLoginId}
        </Button>
      ),
    },
    {
      title: text('pages.supportAccess.status', '상태'),
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusEnum,
    },
    {
      title: text('pages.supportAccess.period', '기간'),
      dataIndex: 'issuedRange',
      hideInTable: true,
      valueType: 'dateTimeRange',
    },
    {
      title: text('pages.supportAccess.reason', '사유'),
      dataIndex: 'reason',
      search: false,
      render: (_, row) =>
        row.reasonDefault ? (
          <Typography.Text type="secondary">
            {row.reason} {text('pages.supportAccess.reasonAuto', '(자동)')}
          </Typography.Text>
        ) : (
          row.reason
        ),
    },
    {
      title: text('pages.supportAccess.started', '시작'),
      dataIndex: 'startedDatetime',
      search: false,
      sorter: true,
    },
    {
      title: text('pages.supportAccess.ended', '종료/만료'),
      dataIndex: 'endedDatetime',
      search: false,
      render: (_, row) => row.endedDatetime ?? row.expireDatetime,
    },
    {
      title: text('pages.supportAccess.startIp', '접속 IP'),
      dataIndex: 'startIp',
      search: false,
    },
    {
      title: text('pages.supportAccess.writeCount', '쓰기 요청 수'),
      dataIndex: 'writeCount',
      search: false,
    },
  ];

  const request = useMemo(
    () =>
      toTableRequest(list, {
        mapParams: (params) => {
          const next = expandDateRangeParams(
            params,
            'issuedRange',
            'from',
            'to',
          );
          if (mode !== 'platform') {
            delete next.tenantCode;
          }
          return next;
        },
      }),
    [list, mode],
  );

  return (
    <PageContainer content={hint}>
      <ProTable<SupportAccessItem>
        headerTitle={text('pages.supportAccess.listTitle', '지원 접속 기록')}
        rowKey="supportAccessId"
        columns={columns}
        search={{ labelWidth: 120 }}
        toolBarRender={false}
        request={request}
        pagination={{ defaultPageSize: 20 }}
      />
      <SupportAccessDrawer
        open={Boolean(selected)}
        item={selected}
        onClose={() => setSelected(null)}
        listActions={listActions}
      />
    </PageContainer>
  );
};

export default SupportAccessHistory;
