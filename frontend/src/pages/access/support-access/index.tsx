import { useIntl } from '@umijs/max';
import React from 'react';
import SupportAccessHistory from '@/components/supportAccess/SupportAccessHistory';
import {
  listTenantSupportAccessActions,
  listTenantSupportAccesses,
} from '@/services/mdm/supportAccess';

const TenantSupportAccessPage: React.FC = () => {
  const intl = useIntl();
  return (
    <SupportAccessHistory
      mode="tenant"
      list={listTenantSupportAccesses}
      listActions={listTenantSupportAccessActions}
      hint={intl.formatMessage({
        id: 'pages.supportAccess.tenantHint',
        defaultMessage:
          '플랫폼 운영자의 지원 접속 기록입니다. 기록은 수정하거나 삭제할 수 없습니다.',
      })}
    />
  );
};

export default TenantSupportAccessPage;
