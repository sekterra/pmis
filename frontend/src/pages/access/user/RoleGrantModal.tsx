import { Button, DatePicker, Modal, Select, Typography } from 'antd';
import dayjs from 'dayjs';
import React, { useEffect, useState } from 'react';
import type { RoleGrantBody, UserRoleGrant } from '@/services/mdm/usersRecord';

type RoleOption = { roleCode: string; roleName: string };

type RoleGrantModalProps = {
  open: boolean;
  roles: RoleOption[];
  current: UserRoleGrant[];
  saving: boolean;
  notice?: string;
  rowErrors: Record<string, string>;
  title: string;
  hint: string;
  roleLabel: string;
  fromLabel: string;
  toLabel: string;
  dateError: string;
  saveLabel: string;
  cancelLabel: string;
  onClose: () => void;
  onSave: (body: RoleGrantBody[]) => void;
};

const RoleGrantModal: React.FC<RoleGrantModalProps> = ({
  open,
  roles,
  current,
  saving,
  notice,
  rowErrors,
  title,
  hint,
  roleLabel,
  fromLabel,
  toLabel,
  dateError,
  saveLabel,
  cancelLabel,
  onClose,
  onSave,
}) => {
  const [selected, setSelected] = useState<string[]>([]);
  const [dates, setDates] = useState<Record<string, { from?: string; to?: string }>>({});
  const [localErrors, setLocalErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) {
      return;
    }
    const active = current.filter((item) => item.useYn === 'Y');
    setSelected(active.map((item) => item.roleCode));
    const next: Record<string, { from?: string; to?: string }> = {};
    for (const item of active) {
      next[item.roleCode] = {
        from: item.validFromDate ?? undefined,
        to: item.validToDate ?? undefined,
      };
    }
    setDates(next);
    setLocalErrors({});
  }, [open, current]);

  const nameOf = (code: string) => roles.find((item) => item.roleCode === code)?.roleName ?? code;

  return (
    <Modal
      open={open}
      title={title}
      onCancel={onClose}
      destroyOnHidden
      transitionName=""
      maskTransitionName=""
      footer={[
        <Button key="cancel" onClick={onClose}>
          {cancelLabel}
        </Button>,
        <Button
          key="save"
          type="primary"
          loading={saving}
          onClick={() => {
            const body = selected.map((roleCode) => ({
              roleCode,
              validFromDate: dates[roleCode]?.from ?? null,
              validToDate: dates[roleCode]?.to ?? null,
            }));
            const invalid = body.filter((item) => item.validFromDate && item.validToDate && item.validToDate < item.validFromDate);
            if (invalid.length > 0) {
              const errors: Record<string, string> = {};
              for (const item of invalid) {
                errors[item.roleCode] = dateError;
              }
              setLocalErrors(errors);
              return;
            }
            setLocalErrors({});
            onSave(body);
          }}
        >
          {saveLabel}
        </Button>,
      ]}
    >
      <Typography.Paragraph>{hint}</Typography.Paragraph>
      {notice ? <Typography.Paragraph type="danger">{notice}</Typography.Paragraph> : null}
      <Select
        mode="multiple"
        allowClear
        aria-label={roleLabel}
        style={{ width: '100%', marginBottom: 12 }}
        value={selected}
        options={roles.map((item) => ({ value: item.roleCode, label: item.roleName }))}
        onChange={(value) => setSelected(value)}
      />
      {selected.map((code) => (
        <div key={code} style={{ marginBottom: 12 }}>
          <div>{nameOf(code)}</div>
          <DatePicker
            aria-label={`${nameOf(code)} ${fromLabel}`}
            value={dates[code]?.from ? dayjs(dates[code]?.from) : null}
            onChange={(value) =>
              setDates((currentDates) => ({
                ...currentDates,
                [code]: { ...currentDates[code], from: value ? value.format('YYYY-MM-DD') : undefined },
              }))
            }
          />
          <DatePicker
            aria-label={`${nameOf(code)} ${toLabel}`}
            value={dates[code]?.to ? dayjs(dates[code]?.to) : null}
            onChange={(value) =>
              setDates((currentDates) => ({
                ...currentDates,
                [code]: { ...currentDates[code], to: value ? value.format('YYYY-MM-DD') : undefined },
              }))
            }
          />
          {localErrors[code] || rowErrors[code] ? <div>{localErrors[code] || rowErrors[code]}</div> : null}
        </div>
      ))}
    </Modal>
  );
};

export default RoleGrantModal;
