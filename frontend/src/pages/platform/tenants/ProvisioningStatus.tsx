import { Tag } from 'antd';
import React from 'react';
import { provisioningTone } from './rules';

type ProvisioningStatusProps = {
  code: string | null | undefined;
  label: (code: string) => string;
};

const ProvisioningStatus: React.FC<ProvisioningStatusProps> = ({
  code,
  label,
}) => {
  if (!code) {
    return <>-</>;
  }
  return <Tag color={provisioningTone(code)}>{label(code)}</Tag>;
};

export default ProvisioningStatus;
