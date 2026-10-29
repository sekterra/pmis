import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { areaForbidden, resetAccessContext, setAccessContext } from '@/core/auth/context';
import { guardView } from '@/core/menu/guard';
import type { MyMenu } from '@/core/menu/types';
import type { SupportAccessItem } from '@/services/platform/data';

const listTenantSupportAccesses = vi.hoisted(() => vi.fn());
const listTenantSupportAccessActions = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
const accessBag = vi.hoisted(() => ({
  can: (_menu: string, _action: string): boolean => true,
}));

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

vi.mock('@ant-design/pro-components', () => {
  const ReactMock = require('react') as typeof import('react');
  return {
    PageContainer: ({ children }: { children?: React.ReactNode }) =>
      ReactMock.createElement('div', null, children),
  };
});

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) =>
      defaultMessage ?? id,
  }),
  useAccess: () => ({
    can: (menu: string, action: string) => accessBag.can(menu, action),
  }),
}));

vi.mock('@/services/mdm/supportAccess', () => ({
  listTenantSupportAccesses: (...args: unknown[]) => listTenantSupportAccesses(...args),
  listTenantSupportAccessActions: (...args: unknown[]) =>
    listTenantSupportAccessActions(...args),
}));

import TenantSupportAccessPage from './index';

const row: SupportAccessItem = {
  supportAccessId: 4,
  operatorLoginId: 'ops@example.com',
  reason: '자동',
  reasonDefault: true,
  status: 'ACTIVE',
  issuedDatetime: '2026-10-01T00:00:00Z',
  startedDatetime: '2026-10-01T00:01:00Z',
  expireDatetime: '2026-10-01T00:31:00Z',
  endedDatetime: null,
  endReasonCode: null,
  startIp: '127.0.0.1',
  writeCount: 1,
};

const supportMenu: MyMenu = {
  menuCode: 'SUPPORT_ACCESS',
  parentMenuCode: 'SYSTEM',
  menuName: '지원 접속 기록',
  menuTypeCode: 'SCREEN',
  routePath: '/access/support-access',
  iconName: null,
  levelNo: 2,
  sortOrder: 62,
  actions: ['READ'],
};

describe('tenant support access history flow', () => {
  beforeEach(() => {
    listTenantSupportAccesses.mockReset();
    listTenantSupportAccessActions.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
    resetAccessContext();
    setAccessContext({ kind: 'TENANT' });
    accessBag.can = () => true;
    listTenantSupportAccesses.mockResolvedValue({
      items: [row],
      total: 1,
      page: 1,
      size: 20,
    });
    listTenantSupportAccessActions.mockResolvedValue({
      items: [
        {
          actionId: 2,
          occurredDatetime: '2026-10-01T00:02:00Z',
          httpMethod: 'PUT',
          path: '/api/mdm/v1/users/1',
          statusCode: 200,
          sensitive: false,
        },
      ],
      total: 1,
      page: 1,
      size: 50,
    });
  });

  afterEach(() => {
    cleanup();
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('lists, filters, opens drawer actions, and blocks users without READ', async () => {
    expect(areaForbidden('/access/support-access')).toBe(false);
    expect(
      guardView('/access/support-access', {
        status: 'ready',
        items: [supportMenu],
      }),
    ).toBe('allow');
    expect(
      guardView('/access/support-access', { status: 'ready', items: [] }),
    ).toBe('forbidden');

    render(<TenantSupportAccessPage />);
    expect(
      await screen.findByText(
        '플랫폼 운영자의 지원 접속 기록입니다. 기록은 수정하거나 삭제할 수 없습니다.',
      ),
    ).toBeTruthy();
    expect(await screen.findByText('ops@example.com')).toBeTruthy();
    expect(screen.queryByLabelText('테넌트')).toBeNull();

    fireEvent.change(screen.getByLabelText('운영자'), {
      target: { value: 'ops' },
    });
    fireEvent.click(screen.getByRole('button', { name: '검색' }));
    await waitFor(() =>
      expect(listTenantSupportAccesses).toHaveBeenCalledWith(
        expect.objectContaining({ platformLoginId: 'ops' }),
      ),
    );

    fireEvent.click(screen.getByText('ops@example.com'));
    expect(await screen.findByText('/api/mdm/v1/users/1')).toBeTruthy();
    expect(listTenantSupportAccessActions).toHaveBeenCalledWith(4, 1, 50);
  });
});
