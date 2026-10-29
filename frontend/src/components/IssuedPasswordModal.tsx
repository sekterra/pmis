import { Button, Modal, Typography } from 'antd';
import React, { useState } from 'react';

type IssuedPasswordModalProps = {
  open: boolean;
  password?: string;
  expire?: string | null;
  title: string;
  hint: string;
  expireLabel: string;
  copyLabel: string;
  copyFailed: string;
  closeLabel: string;
  onClose: () => void;
};

const IssuedPasswordModal: React.FC<IssuedPasswordModalProps> = ({
  open,
  password,
  expire,
  title,
  hint,
  expireLabel,
  copyLabel,
  copyFailed,
  closeLabel,
  onClose,
}) => {
  const [copyError, setCopyError] = useState(false);
  if (!open || !password) {
    return null;
  }
  const shown = password;
  return (
    <Modal
      open
      title={title}
      closable={false}
      mask={{ closable: false }}
      keyboard={false}
      onCancel={onClose}
      destroyOnHidden
      footer={[
        <Button
          key="copy"
          onClick={() => {
            const write = navigator.clipboard?.writeText;
            if (!write) {
              setCopyError(true);
              return;
            }
            write.call(navigator.clipboard, shown).then(
              () => setCopyError(false),
              () => setCopyError(true),
            );
          }}
        >
          {copyLabel}
        </Button>,
        <Button key="close" type="primary" onClick={onClose}>
          {closeLabel}
        </Button>,
      ]}
    >
      <Typography.Paragraph>{hint}</Typography.Paragraph>
      <Typography.Paragraph style={{ fontFamily: 'monospace', fontSize: 24, marginBottom: 8 }}>{shown}</Typography.Paragraph>
      {expire ? (
        <Typography.Paragraph>
          {expireLabel}: {expire}
        </Typography.Paragraph>
      ) : null}
      {copyError ? <Typography.Paragraph>{copyFailed}</Typography.Paragraph> : null}
    </Modal>
  );
};

export default IssuedPasswordModal;
