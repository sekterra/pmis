import type { ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { history, useIntl } from '@umijs/max';
import { Button } from 'antd';
import React, { useMemo } from 'react';
import {
  mergeKeywordParams,
  toTableRequest,
} from '@/core/request/table';
import type { TenantSummary } from '@/services/platform/data';
import { listTenants } from '@/services/platform/tenants';
import ProvisioningStatus from './ProvisioningStatus';
import {
  formatCreateDatetime,
  provisioningLabel,
  TENANT_LIST_SORT_FIELDS,
  TENANT_STATUSES,
} from './rules';

const TenantListPage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });

  const statusEnum = useMemo(
    () =>
      Object.fromEntries(
        TENANT_STATUSES.map((status) => [
          status,
          {
            text: text(`pages.platform.status.${status}`, status),
            status:
              status === 'ACTIVE'
                ? ('Success' as const)
                : status === 'PROVISIONING'
                  ? ('Processing' as const)
                  : status === 'FAILED'
                    ? ('Error' as const)
                    : status === 'SUSPENDED'
                      ? ('Warning' as const)
                      : ('Default' as const),
          },
        ]),
      ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [intl],
  );

  const columns: ProColumns<TenantSummary>[] = [
    {
      title: text('pages.platform.code', '코드'),
      dataIndex: 'tenantCode',
      sorter: true,
      render: (_, record) => (
        <Button
          type="link"
          onClick={(event) => {
            event.stopPropagation();
            history.push(`/tenants/${record.tenantCode}`);
          }}
        >
          {record.tenantCode}
        </Button>
      ),
    },
    {
      title: text('pages.platform.name', '이름'),
      dataIndex: 'tenantName',
      sorter: true,
    },
    {
      title: text('pages.platform.status', '상태'),
      dataIndex: 'status',
      hideInTable: true,
      valueType: 'select',
      valueEnum: statusEnum,
    },
    {
      title: text('pages.platform.status', '상태'),
      dataIndex: 'tenantStatusCode',
      search: false,
      sorter: true,
      valueEnum: statusEnum,
    },
    {
      title: text('pages.platform.language', '기본 언어'),
      dataIndex: 'defaultLanguageCode',
      search: false,
    },
    {
      title: text('pages.platform.primaryDomain', '기본 도메인'),
      dataIndex: 'primaryDomain',
      search: false,
      render: (_, record) => record.primaryDomain || '-',
    },
    {
      title: text('pages.platform.created', '생성일'),
      dataIndex: 'createDatetime',
      search: false,
      sorter: true,
      render: (_, record) => formatCreateDatetime(record.createDatetime),
    },
    {
      title: text('pages.platform.provisioningStatus', '프로비저닝 단계'),
      dataIndex: 'provisioningStatus',
      search: false,
      render: (_, record) => (
        <ProvisioningStatus
          code={record.provisioningStatus}
          label={(code) => provisioningLabel(code, text)}
        />
      ),
    },
  ];

  const request = useMemo(() => {
    const base = toTableRequest(listTenants, {
      mapParams: (params) => mergeKeywordParams(params),
    });
    return (
      params: Parameters<typeof base>[0],
      sorter: Parameters<typeof base>[1] = {},
    ) => {
      const allowed = Object.fromEntries(
        Object.entries(sorter ?? {}).filter(([field]) =>
          TENANT_LIST_SORT_FIELDS.includes(
            field as (typeof TENANT_LIST_SORT_FIELDS)[number],
          ),
        ),
      ) as NonNullable<Parameters<typeof base>[1]>;
      return base(params, allowed);
    };
  }, []);

  return (
    <PageContainer>
      <ProTable<TenantSummary>
        headerTitle={text('pages.platform.listTitle', '테넌트 목록')}
        rowKey="tenantCode"
        columns={columns}
        search={{ labelWidth: 120 }}
        request={request}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => history.push('/tenants/new')}
          >
            {text('pages.platform.create', '테넌트 등록')}
          </Button>,
        ]}
      />
    </PageContainer>
  );
};

export default TenantListPage;
