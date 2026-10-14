import dayjs from 'dayjs';
import { describe, expect, it } from 'vitest';
import {
  buildCreatePayload,
  formatCreateDatetime,
  provisioningLabel,
  provisioningTone,
  secretView,
  stepItems,
  TENANT_LIST_SORT_FIELDS,
  validateCreate,
  visibleActions,
} from './rules';

const valid = {
  tenantCode: 'acme',
  tenantName: 'Acme',
  defaultLanguageCode: 'ko-KR',
  timezoneId: 'Asia/Seoul',
  contractStartDate: '2026-01-01',
  contractEndDate: '2026-12-31',
  adminName: '관리자',
  adminEmail: 'admin@example.com',
  domains: ['acme.example.com'],
};

describe('tenant create rules', () => {
  it('requires the fields and rejects a bad domain', () => {
    expect(
      validateCreate({ ...valid, tenantCode: '', domains: [''] }).tenantCode,
    ).toBe('code');
    expect(
      validateCreate({ ...valid, domains: ['ACME.Example.com'] }).domains,
    ).toBe('domain');
    expect(
      validateCreate({ ...valid, contractEndDate: '2025-01-01' })
        .contractEndDate,
    ).toBe('range');
    expect(validateCreate(valid)).toEqual({});
  });

  it('sends the create body without a schema name', () => {
    const payload = buildCreatePayload(valid);
    expect(payload.domains).toEqual(['acme.example.com']);
    expect(payload).not.toHaveProperty('schemaName');
    expect(Object.keys(payload)).toEqual([
      'tenantCode',
      'tenantName',
      'defaultLanguageCode',
      'timezoneId',
      'contractStartDate',
      'contractEndDate',
      'adminName',
      'adminEmail',
      'domains',
    ]);
  });

  it('shows actions by status and marks a failed provisioning step', () => {
    expect(visibleActions('ACTIVE').suspend).toBe(true);
    expect(visibleActions('ACTIVE').resume).toBe(false);
    expect(visibleActions('SUSPENDED').resume).toBe(true);
    expect(visibleActions('FAILED').retry).toBe(true);
    expect(visibleActions('PROVISIONING').retry).toBe(true);
    expect(visibleActions('ACTIVE').retry).toBe(false);
    expect(
      stepItems('MIGRATED:FAILED').find((item) => item.title === 'MIGRATED')
        ?.status,
    ).toBe('error');
  });

  it('uses the stored admin login id and formats list fields', () => {
    expect(secretView('Stored.Admin', null)).toEqual({
      loginId: 'Stored.Admin',
    });
    expect(secretView('stored-admin', 'Temp-1')).toEqual({
      loginId: 'stored-admin',
      password: 'Temp-1',
    });
    expect(formatCreateDatetime(null)).toBe('-');
    expect(formatCreateDatetime('not-a-date')).toBe('not-a-date');
    const iso = '2026-10-11T11:00:00Z';
    expect(formatCreateDatetime(iso)).toBe(dayjs(iso).format('YYYY-MM-DD HH:mm'));
    expect(provisioningTone('FAILED')).toBe('error');
    expect(provisioningTone('COMPLETED')).toBe('success');
    expect(provisioningTone('MYSTERY')).toBe('default');
    expect(provisioningLabel('MYSTERY', (_id, fallback) => fallback)).toBe(
      'MYSTERY',
    );
    expect(provisioningLabel('FAILED', () => '실패')).toBe('실패');
    expect([...TENANT_LIST_SORT_FIELDS]).toEqual([
      'tenantCode',
      'tenantName',
      'tenantStatusCode',
      'createDatetime',
    ]);
  });
});
