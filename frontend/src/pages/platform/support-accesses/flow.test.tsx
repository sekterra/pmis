import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { areaForbidden, resetAccessContext, setAccessContext } from '@/core/auth/context';
import type { SupportAccessItem } from '@/services/platform/data';

const listPlatformSupportAccesses = vi.hoisted(() => vi.fn());
const listPlatformSupportAccessActions = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());

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
}));

vi.mock('@/services/platform/supportAccess', () => ({
  listPlatformSupportAccesses: (...args: unknown[]) =>
    listPlatformSupportAccesses(...args),
  listPlatformSupportAccessActions: (...args: unknown[]) =>
    listPlatformSupportAccessActions(...args),
}));

import PlatformSupportAccessesPage from './index';

const row: SupportAccessItem = {
  supportAccessId: 9,
  tenantCode: 'acme',
  operatorLoginId: 'ops@example.com',
  reason: '점검',
  reasonDefault: false,
  status: 'ENDED',
  issuedDatetime: '2026-10-01T00:00:00Z',
  startedDatetime: '2026-10-01T00:01:00Z',
  expireDatetime: '2026-10-01T00:31:00Z',
  endedDatetime: '2026-10-01T00:10:00Z',
  endReasonCode: 'USER_ENDED',
  startIp: '127.0.0.1',
  writeCount: 2,
};

describe('platform support access history flow', () => {
  beforeEach(() => {
    listPlatformSupportAccesses.mockReset();
    listPlatformSupportAccessActions.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
    resetAccessContext();
    setAccessContext({ kind: 'PLATFORM' });
    listPlatformSupportAccesses.mockResolvedValue({
      items: [row],
      total: 1,
      page: 1,
      size: 20,
    });
    listPlatformSupportAccessActions.mockResolvedValue({
      items: [
        {
          actionId: 1,
          occurredDatetime: '2026-10-01T00:02:00Z',
          httpMethod: 'POST',
          path: '/api/mdm/v1/users',
          statusCode: 200,
          sensitive: true,
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

  it('lists, filters, opens drawer actions, and allows platform path', async () => {
    expect(areaForbidden('/support-accesses')).toBe(false);
    expect(areaForbidden('/access/support-access')).toBe(true);
    render(<PlatformSupportAccessesPage />);
    expect(await screen.findByText('ops@example.com')).toBeTruthy();
    expect(listPlatformSupportAccesses).toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText('테넌트'), {
      target: { value: 'acme' },
    });
    fireEvent.click(screen.getByRole('button', { name: '검색' }));
    await waitFor(() =>
      expect(listPlatformSupportAccesses).toHaveBeenCalledWith(
        expect.objectContaining({ tenantCode: 'acme' }),
      ),
    );

    fireEvent.click(screen.getByText('ops@example.com'));
    expect(await screen.findByText('/api/mdm/v1/users')).toBeTruthy();
    expect(listPlatformSupportAccessActions).toHaveBeenCalledWith(9, 1, 50);
  });
});
