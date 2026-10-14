import { Button, Modal, Typography } from 'antd';
import React from 'react';

type TemporaryPasswordModalProps = {
  open: boolean;
  loginId?: string;
  password?: string;
  title: string;
  hint: string;
  noPasswordHint: string;
  loginLabel: string;
  passwordLabel: string;
  copyLabel: string;
  copyLoginLabel: string;
  closeLabel: string;
  onClose: () => void;
};

const TemporaryPasswordModal: React.FC<TemporaryPasswordModalProps> = ({
  open,
  loginId,
  password,
  title,
  hint,
  noPasswordHint,
  loginLabel,
  passwordLabel,
  copyLabel,
  copyLoginLabel,
  closeLabel,
  onClose,
}) => {
  if (!open || (!password && !loginId)) {
    return null;
  }
  const shown = password;
  const shownLogin = loginId;
  return (
    <Modal
      open={open}
      title={title}
      onCancel={onClose}
      destroyOnHidden
      transitionName=""
      maskTransitionName=""
      footer={[
        shownLogin ? (
          <Button
            key="copy-login"
            onClick={() => navigator.clipboard.writeText(shownLogin)}
          >
            {copyLoginLabel}
          </Button>
        ) : null,
        shown ? (
          <Button key="copy" onClick={() => navigator.clipboard.writeText(shown)}>
            {copyLabel}
          </Button>
        ) : null,
        <Button
          key="close"
          type="primary"
          data-testid="close-secret"
          onClick={onClose}
        >
          {closeLabel}
        </Button>,
      ].filter((item) => item !== null)}
    >
      <Typography.Paragraph>
        {shown ? hint : noPasswordHint}
      </Typography.Paragraph>
      {shownLogin ? (
        <Typography.Paragraph>
          {loginLabel}: {shownLogin}
        </Typography.Paragraph>
      ) : null}
      {shown ? (
        <Typography.Paragraph>
          {passwordLabel}: {shown}
        </Typography.Paragraph>
      ) : null}
    </Modal>
  );
};

export default TemporaryPasswordModal;
