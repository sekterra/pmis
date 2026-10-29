import {
  cleanup,
  fireEvent,
  screen,
  waitFor,
} from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import { bindFeedback } from '@/core/request/feedback';
import type { TenantSummary } from '@/services/platform/data';
import { renderWithPro } from '@/test/proRender';

const listTenants = vi.hoisted(() => vi.fn());
const push = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({
      defaultMessage,
      id,
    }: {
      defaultMessage?: string;
      id: string;
    }) => defaultMessage ?? id,
  }),
  history: {
    push: (...args: unknown[]) => push(...args),
  },
}));

vi.mock('@/services/platform/tenants', () => ({
  listTenants: (...args: unknown[]) => listTenants(...args),
}));

import TenantListPage from './index';

const row: TenantSummary = {
  tenantCode: 'acme',
  tenantName: 'Acme',
  schemaName: 't_acme',
  tenantStatusCode: 'ACTIVE',
  defaultLanguageCode: 'ko-KR',
  timezoneId: 'Asia/Seoul',
  provisioningStatus: 'COMPLETED',
  primaryDomain: 'acme.example.com',
  createDatetime: '2026-10-01T00:00:00Z',
};

describe('tenant list ProTable flow', () => {
  beforeEach(() => {
    listTenants.mockReset();
    push.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
    listTenants.mockResolvedValue({
      items: [row],
      total: 1,
      page: 1,
      size: 20,
    });
    bindFeedback({ warning: vi.fn(), error: vi.fn(), notify: vi.fn() });
  });

  afterEach(() => {
    cleanup();
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('renders rows, searches, navigates, and toasts once', async () => {
    const error = vi.fn();
    bindFeedback({ warning: vi.fn(), error, notify: vi.fn() });

    renderWithPro(<TenantListPage />);
    expect(await screen.findByText('테넌트 목록')).toBeTruthy();
    expect(await screen.findByRole('button', { name: '조회' })).toBeTruthy();
    expect(screen.getByRole('button', { name: '초기화' })).toBeTruthy();
    expect(await screen.findByText('Acme')).toBeTruthy();
    expect(listTenants).toHaveBeenCalled();

    fireEvent.change(screen.getAllByLabelText('코드')[0], {
      target: { value: 'acme' },
    });
    fireEvent.change(screen.getAllByLabelText('이름')[0], {
      target: { value: '에이' },
    });
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() =>
      expect(listTenants).toHaveBeenCalledWith(
        expect.objectContaining({
          keyword: expect.stringContaining('acme'),
          page: 1,
        }),
      ),
    );

    fireEvent.click(screen.getByRole('button', { name: '테넌트 등록' }));
    expect(push).toHaveBeenCalledWith('/tenants/new');

    fireEvent.click(screen.getByRole('button', { name: 'acme' }));
    expect(push).toHaveBeenCalledWith('/tenants/acme');

    const fail = new ApiError('목록 오류');
    fail.showType = 2;
    listTenants.mockRejectedValueOnce(fail);
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() => expect(error).toHaveBeenCalledTimes(1));
    expect(error).toHaveBeenCalledWith('목록 오류');
  });
});
