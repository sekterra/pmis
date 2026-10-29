import type { ProDescriptionsColumn } from '@ant-design/pro-components';
import { ProDescriptions } from '@ant-design/pro-components';
import { useIntl } from '@umijs/max';
import { Drawer, Table, Tag, Typography } from 'antd';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { statusTagColor } from '@/core/supportAccess/rules';
import { toApiError } from '@/core/request/errors';
import type {
  PageResult,
  SupportAccessActionItem,
  SupportAccessItem,
} from '@/services/platform/data';

type SupportAccessDrawerProps = {
  open: boolean;
  item: SupportAccessItem | null;
  onClose: () => void;
  listActions: (
    id: number,
    page: number,
    size: number,
  ) => Promise<PageResult<SupportAccessActionItem>>;
};

const SupportAccessDrawer: React.FC<SupportAccessDrawerProps> = ({
  open,
  item,
  onClose,
  listActions,
}) => {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [actions, setActions] = useState<SupportAccessActionItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState<string>();
  const loadedKey = useRef('');

  useEffect(() => {
    if (!open || !item) {
      loadedKey.current = '';
      return undefined;
    }
    const key = `${item.supportAccessId}:${page}`;
    if (loadedKey.current === key) {
      return undefined;
    }
    loadedKey.current = key;
    let alive = true;
    setLoading(true);
    void listActions(item.supportAccessId, page, 50)
      .then((result) => {
        if (alive && loadedKey.current === key) {
          setActions(result.items);
          setTotal(result.total);
          setNotice(undefined);
        }
      })
      .catch((error) => {
        if (alive && loadedKey.current === key) {
          setNotice(toApiError(error).message);
        }
      })
      .finally(() => {
        if (alive && loadedKey.current === key) {
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [open, item?.supportAccessId, page, listActions]);

  useEffect(() => {
    if (!open) {
      setPage(1);
      setActions([]);
      setTotal(0);
      setNotice(undefined);
    }
  }, [open]);

  const columns = useMemo(() => {
    const items: ProDescriptionsColumn<SupportAccessItem>[] = [];
    if (item?.tenantCode) {
      items.push({
        title: intl.formatMessage({
          id: 'pages.supportAccess.tenant',
          defaultMessage: '테넌트',
        }),
        dataIndex: 'tenantCode',
      });
    }
    items.push(
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
        render: (_, row) =>
          row.reasonDefault ? (
            <Typography.Text type="secondary">
              {row.reason}{' '}
              {intl.formatMessage({
                id: 'pages.supportAccess.reasonAuto',
                defaultMessage: '(자동)',
              })}
            </Typography.Text>
          ) : (
            row.reason
          ),
      },
      {
        title: intl.formatMessage({
          id: 'pages.supportAccess.status',
          defaultMessage: '상태',
        }),
        dataIndex: 'status',
        render: (_, row) => (
          <Tag color={statusTagColor(row.status)}>{row.status}</Tag>
        ),
      },
      {
        title: intl.formatMessage({
          id: 'pages.supportAccess.issued',
          defaultMessage: '발급',
        }),
        dataIndex: 'issuedDatetime',
      },
      {
        title: intl.formatMessage({
          id: 'pages.supportAccess.started',
          defaultMessage: '시작',
        }),
        dataIndex: 'startedDatetime',
      },
      {
        title: intl.formatMessage({
          id: 'pages.supportAccess.ended',
          defaultMessage: '종료/만료',
        }),
        dataIndex: 'endedDatetime',
        render: (_, row) => row.endedDatetime ?? row.expireDatetime,
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
    );
    return items;
  }, [intl, item?.tenantCode]);

  return (
    <Drawer
      size={600}
      open={open}
      onClose={onClose}
      title={intl.formatMessage({
        id: 'pages.supportAccess.detail',
        defaultMessage: '지원 접속 상세',
      })}
      destroyOnHidden
    >
      {item ? (
        <ProDescriptions<SupportAccessItem>
          column={2}
          size="small"
          bordered
          style={{ marginBottom: 16 }}
          dataSource={item}
          columns={columns}
        />
      ) : null}
      {notice ? <div role="alert">{notice}</div> : null}
      <Table
        rowKey="actionId"
        size="small"
        loading={loading}
        dataSource={actions}
        pagination={{
          current: page,
          pageSize: 50,
          total,
          onChange: setPage,
        }}
        columns={[
          {
            title: intl.formatMessage({
              id: 'pages.supportAccess.occurred',
              defaultMessage: '발생 시각',
            }),
            dataIndex: 'occurredDatetime',
          },
          {
            title: intl.formatMessage({
              id: 'pages.supportAccess.method',
              defaultMessage: '메서드',
            }),
            dataIndex: 'httpMethod',
          },
          {
            title: intl.formatMessage({
              id: 'pages.supportAccess.path',
              defaultMessage: '경로',
            }),
            dataIndex: 'path',
          },
          {
            title: intl.formatMessage({
              id: 'pages.supportAccess.statusCode',
              defaultMessage: '결과 코드',
            }),
            dataIndex: 'statusCode',
          },
          {
            title: intl.formatMessage({
              id: 'pages.supportAccess.sensitive',
              defaultMessage: '민감',
            }),
            dataIndex: 'sensitive',
            render: (value: boolean) =>
              value ? (
                <Tag color="error">
                  {intl.formatMessage({
                    id: 'pages.supportAccess.sensitiveYes',
                    defaultMessage: '민감',
                  })}
                </Tag>
              ) : null,
          },
        ]}
      />
    </Drawer>
  );
};

export default SupportAccessDrawer;
