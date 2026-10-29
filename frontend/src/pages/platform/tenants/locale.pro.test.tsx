import { cleanup, screen } from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithPro } from '@/test/proRender';

const listTenants = vi.hoisted(() => vi.fn());

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
  history: { push: vi.fn() },
}));

vi.mock('@/services/platform/tenants', () => ({
  listTenants: (...args: unknown[]) => listTenants(...args),
}));

import TenantListPage from './index';

describe('ProTable built-in locale', () => {
  beforeEach(() => {
    listTenants.mockResolvedValue({
      items: [],
      total: 0,
      page: 1,
      size: 20,
    });
  });

  afterEach(() => {
    cleanup();
  });

  it('shows Korean query and reset labels', async () => {
    renderWithPro(<TenantListPage />, { locale: 'ko' });
    expect(await screen.findByRole('button', { name: '조회' })).toBeTruthy();
    expect(screen.getByRole('button', { name: '초기화' })).toBeTruthy();
  });

  it('shows English query and reset labels', async () => {
    renderWithPro(<TenantListPage />, { locale: 'en' });
    expect(await screen.findByRole('button', { name: 'Query' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Reset' })).toBeTruthy();
  });
});
