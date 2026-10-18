import { Button } from 'antd';
import React from 'react';
import { visibleActions } from './rules';

type TenantActionBarProps = {
  status: string;
  labels: {
    suspend: string;
    resume: string;
    retry: string;
    reissue: string;
  };
  onSuspend: () => void;
  onResume: () => void;
  onRetry: () => void;
  onReissue: () => void;
};

const TenantActionBar: React.FC<TenantActionBarProps> = ({
  status,
  labels,
  onSuspend,
  onResume,
  onRetry,
  onReissue,
}) => {
  const actions = visibleActions(status);
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      {actions.suspend ? (
        <Button onClick={onSuspend}>{labels.suspend}</Button>
      ) : null}
      {actions.resume ? (
        <Button onClick={onResume}>{labels.resume}</Button>
      ) : null}
      {actions.retry ? <Button onClick={onRetry}>{labels.retry}</Button> : null}
      {actions.reissue ? (
        <Button onClick={onReissue}>{labels.reissue}</Button>
      ) : null}
    </div>
  );
};

export default TenantActionBar;
