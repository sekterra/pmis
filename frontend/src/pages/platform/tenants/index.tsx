import type { ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { history, useIntl } from '@umijs/max';
import { Button } from 'antd';
import React from 'react';
import { toTableRequest } from '@/core/request/table';
import type { TenantSummary } from '@/services/platform/data';
import { listTenants } from '@/services/platform/tenants';
import ProvisioningStatus from './ProvisioningStatus';
import {
  formatCreateDatetime,
  provisioningLabel,
  TENANT_STATUSES,
} from './rules';

const TenantListPage: React.FC = () => {
  const intl = useIntl();
  const text = (id: string, defaultMessage: string) =>
    intl.formatMessage({ id, defaultMessage });
  const columns: ProColumns<TenantSummary>[] = [
    {
      title: text('pages.platform.keyword', '검색어'),
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: text('pages.platform.status', '상태'),
      dataIndex: 'status',
      hideInTable: true,
      valueType: 'select',
      valueEnum: Object.fromEntries(
        TENANT_STATUSES.map((status) => [status, { text: status }]),
      ),
    },
    {
      title: text('pages.platform.code', '코드'),
      dataIndex: 'tenantCode',
      search: false,
      sorter: true,
    },
    {
      title: text('pages.platform.name', '이름'),
      dataIndex: 'tenantName',
      search: false,
      sorter: true,
    },
    {
      title: text('pages.platform.status', '상태'),
      dataIndex: 'tenantStatusCode',
      search: false,
      sorter: true,
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
      title: text('pages.platform.language', '기본 언어'),
      dataIndex: 'defaultLanguageCode',
      search: false,
    },
    {
      title: text('pages.platform.timezone', '시간대'),
      dataIndex: 'timezoneId',
      search: false,
    },
  ];

  return (
    <PageContainer>
      <ProTable<TenantSummary>
        rowKey="tenantCode"
        columns={columns}
        request={toTableRequest(listTenants)}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => history.push('/tenants/new')}
          >
            {text('pages.platform.create', '테넌트 등록')}
          </Button>,
        ]}
        onRow={(record) => ({
          onClick: () => history.push(`/tenants/${record.tenantCode}`),
        })}
      />
    </PageContainer>
  );
};

export default TenantListPage;
