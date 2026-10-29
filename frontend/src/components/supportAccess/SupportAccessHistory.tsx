import { PageContainer } from '@ant-design/pro-components';
import { useIntl } from '@umijs/max';
import {
  Button,
  DatePicker,
  Form,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import dayjs from 'dayjs';
import React, { useCallback, useEffect, useState } from 'react';
import {
  SUPPORT_ACCESS_STATUSES,
  statusTagColor,
} from '@/core/supportAccess/rules';
import { toApiError } from '@/core/request/errors';
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
  const [filters, setFilters] = useState<{
    tenantCode?: string;
    platformLoginId?: string;
    status?: string;
    from?: string;
    to?: string;
  }>({});
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [sort, setSort] = useState<string[]>(['issuedDatetime,desc']);
  const [rows, setRows] = useState<SupportAccessItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState<string>();
  const [selected, setSelected] = useState<SupportAccessItem | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await list({
        ...filters,
        page,
        size,
        sort,
      });
      setRows(result.items);
      setTotal(result.total);
      setNotice(undefined);
    } catch (error) {
      setNotice(toApiError(error).message);
    } finally {
      setLoading(false);
    }
  }, [filters, list, page, size, sort]);

  useEffect(() => {
    void load();
  }, [load]);

  const columns: ColumnsType<SupportAccessItem> = [
    ...(mode === 'platform'
      ? [
          {
            title: intl.formatMessage({
              id: 'pages.supportAccess.tenant',
              defaultMessage: '테넌트',
            }),
            dataIndex: 'tenantCode',
          },
        ]
      : []),
    {
      title: intl.formatMessage({
        id: 'pages.supportAccess.operator',
        defaultMessage: '운영자',
      }),
      dataIndex: 'operatorLoginId',
    },
    {
      title: intl.formatMessage({
        id: 'pages.supportAccess.reason',
        defaultMessage: '사유',
      }),
      dataIndex: 'reason',
      render: (value: string, row) =>
        row.reasonDefault ? (
          <Typography.Text type="secondary">
            {value}{' '}
            {intl.formatMessage({
              id: 'pages.supportAccess.reasonAuto',
              defaultMessage: '(자동)',
            })}
          </Typography.Text>
        ) : (
          value
        ),
    },
    {
      title: intl.formatMessage({
        id: 'pages.supportAccess.status',
        defaultMessage: '상태',
      }),
      dataIndex: 'status',
      render: (value: string) => (
        <Tag color={statusTagColor(value)}>{value}</Tag>
      ),
    },
    {
      title: intl.formatMessage({
        id: 'pages.supportAccess.started',
        defaultMessage: '시작',
      }),
      dataIndex: 'startedDatetime',
      sorter: true,
    },
    {
      title: intl.formatMessage({
        id: 'pages.supportAccess.ended',
        defaultMessage: '종료/만료',
      }),
      dataIndex: 'endedDatetime',
      render: (_: unknown, row) => row.endedDatetime ?? row.expireDatetime,
    },
    {
      title: intl.formatMessage({
        id: 'pages.supportAccess.startIp',
        defaultMessage: '접속 IP',
      }),
      dataIndex: 'startIp',
    },
    {
      title: intl.formatMessage({
        id: 'pages.supportAccess.writeCount',
        defaultMessage: '쓰기 요청 수',
      }),
      dataIndex: 'writeCount',
    },
  ];

  const onTableChange = (
    pagination: TablePaginationConfig,
    _filters: Record<string, unknown>,
    sorter: unknown,
  ) => {
    setPage(pagination.current ?? 1);
    setSize(pagination.pageSize ?? 20);
    const single = Array.isArray(sorter) ? sorter[0] : sorter;
    if (
      single &&
      typeof single === 'object' &&
      'field' in single &&
      'order' in single
    ) {
      const field = (single as { field?: React.Key | React.Key[] }).field;
      const order = (single as { order?: 'ascend' | 'descend' | null }).order;
      if (field && !Array.isArray(field) && (order === 'ascend' || order === 'descend')) {
        setSort([`${String(field)},${order === 'ascend' ? 'asc' : 'desc'}`]);
      }
    }
  };

  return (
    <PageContainer>
      {hint ? (
        <Typography.Paragraph type="secondary">{hint}</Typography.Paragraph>
      ) : null}
      {notice ? (
        <Typography.Text type="danger" role="alert">
          {notice}
        </Typography.Text>
      ) : null}
      <Form
        layout="inline"
        style={{ marginBottom: 16 }}
        onFinish={(values) => {
          setPage(1);
          setFilters({
            tenantCode: values.tenantCode,
            platformLoginId: values.platformLoginId,
            status: values.status,
            from: values.range?.[0]
              ? dayjs(values.range[0]).toISOString()
              : undefined,
            to: values.range?.[1]
              ? dayjs(values.range[1]).toISOString()
              : undefined,
          });
        }}
      >
        {mode === 'platform' ? (
          <Form.Item name="tenantCode" label={intl.formatMessage({
            id: 'pages.supportAccess.tenant',
            defaultMessage: '테넌트',
          })}>
            <Input allowClear />
          </Form.Item>
        ) : null}
        <Form.Item
          name="platformLoginId"
          label={intl.formatMessage({
            id: 'pages.supportAccess.operator',
            defaultMessage: '운영자',
          })}
        >
          <Input allowClear />
        </Form.Item>
        <Form.Item
          name="status"
          label={intl.formatMessage({
            id: 'pages.supportAccess.status',
            defaultMessage: '상태',
          })}
        >
          <Select
            allowClear
            style={{ width: 140 }}
            options={SUPPORT_ACCESS_STATUSES.map((status) => ({
              value: status,
              label: status,
            }))}
          />
        </Form.Item>
        <Form.Item
          name="range"
          label={intl.formatMessage({
            id: 'pages.supportAccess.period',
            defaultMessage: '기간',
          })}
        >
          <DatePicker.RangePicker showTime />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              {intl.formatMessage({
                id: 'pages.supportAccess.search',
                defaultMessage: '검색',
              })}
            </Button>
          </Space>
        </Form.Item>
      </Form>
      <Table
        rowKey="supportAccessId"
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ current: page, pageSize: size, total }}
        onChange={onTableChange as never}
        onRow={(record) => ({
          onClick: () => setSelected(record),
          style: { cursor: 'pointer' },
        })}
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
