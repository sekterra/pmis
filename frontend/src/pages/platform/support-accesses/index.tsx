import React from 'react';
import SupportAccessHistory from '@/components/supportAccess/SupportAccessHistory';
import {
  listPlatformSupportAccessActions,
  listPlatformSupportAccesses,
} from '@/services/platform/supportAccess';

const PlatformSupportAccessesPage: React.FC = () => (
  <SupportAccessHistory
    mode="platform"
    list={listPlatformSupportAccesses}
    listActions={listPlatformSupportAccessActions}
  />
);

export default PlatformSupportAccessesPage;
