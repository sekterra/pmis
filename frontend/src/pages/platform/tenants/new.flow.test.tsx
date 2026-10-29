import {
  cleanup,
  fireEvent,
  screen,
  waitFor,
} from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import { renderWithPro } from '@/test/proRender';

const createTenant = vi.hoisted(() => vi.fn());
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
  createTenant: (...args: unknown[]) => createTenant(...args),
}));

import NewTenantPage, { tenantCreateFormRef } from './new';

async function fillRequired() {
  await waitFor(() => expect(tenantCreateFormRef.current).toBeTruthy());
  const form = tenantCreateFormRef.current;
  if (!form) {
    throw new Error('tenant create form is not ready');
  }
  form.setFieldsValue({
    tenantCode: 'acme',
    tenantName: 'Acme',
    defaultLanguageCode: 'ko-KR',
    timezoneId: 'Asia/Seoul',
    contractStartDate: '2026-10-15',
    adminName: 'Admin',
    adminEmail: 'admin@example.com',
    domains: ['acme.example.com'],
  });
}

async function submitForm() {
  await fillRequired();
  fireEvent.click(screen.getByRole('button', { name: '등록' }));
}

describe('tenant create ProForm flow', () => {
  beforeEach(() => {
    createTenant.mockReset();
    push.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
    createTenant.mockResolvedValue({
      tenantCode: 'acme',
      schemaName: 't_acme',
      tenantStatus: 'PROVISIONING',
      provisioningStep: 'REQUESTED',
      executionStatus: 'RUNNING',
      temporaryPassword: 'Temp-1',
      adminLoginId: 'admin@example.com',
    });
  });

  afterEach(() => {
    cleanup();
    tenantCreateFormRef.current = null;
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('shows sections, manages domains, validates, submits, and opens the secret modal', async () => {
    renderWithPro(<NewTenantPage />);
    expect(screen.getByText('기본 정보')).toBeTruthy();
    expect(screen.getByText('계약')).toBeTruthy();
    expect(screen.getByText('관리자')).toBeTruthy();
    expect(screen.getAllByText('도메인').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: /도메인 추가/ }));
    await waitFor(() =>
      expect(screen.getAllByLabelText('도메인').length).toBeGreaterThan(1),
    );

    fireEvent.click(screen.getByRole('button', { name: '등록' }));
    await waitFor(() => expect(createTenant).not.toHaveBeenCalled());

    await submitForm();
    await waitFor(() => expect(createTenant).toHaveBeenCalledTimes(1));
    expect(createTenant.mock.calls[0][0]).toMatchObject({
      tenantCode: 'acme',
      tenantName: 'Acme',
      adminName: 'Admin',
      adminEmail: 'admin@example.com',
      domains: expect.arrayContaining(['acme.example.com']),
      contractStartDate: '2026-10-15',
    });
    expect(await screen.findByText(/Temp-1/)).toBeTruthy();
    fireEvent.click(screen.getByTestId('close-secret'));
    await waitFor(() => expect(push).toHaveBeenCalledWith('/tenants/acme'));
  });

  it('maps server fieldErrors and cancels to the list', async () => {
    const err = new ApiError('검증 실패');
    err.fieldErrors = [
      { field: 'tenantCode', message: '코드가 올바르지 않습니다.' },
    ];
    createTenant.mockRejectedValueOnce(err);

    renderWithPro(<NewTenantPage />);
    await submitForm();
    await waitFor(() => expect(createTenant).toHaveBeenCalled());
    expect(
      await screen.findByText('코드가 올바르지 않습니다.'),
    ).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: '취소' }));
    expect(push).toHaveBeenCalledWith('/tenants');
  });

  it('blocks duplicate submit while pending', async () => {
    let resolveCreate: (value: unknown) => void = () => undefined;
    createTenant.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveCreate = resolve;
        }),
    );
    renderWithPro(<NewTenantPage />);
    await submitForm();
    fireEvent.click(screen.getByRole('button', { name: '등록' }));
    await waitFor(() => expect(createTenant).toHaveBeenCalledTimes(1));
    resolveCreate({
      tenantCode: 'acme',
      schemaName: 't_acme',
      tenantStatus: 'ACTIVE',
      provisioningStep: 'COMPLETED',
      executionStatus: 'DONE',
      temporaryPassword: null,
      adminLoginId: '',
    });
    await waitFor(() => expect(push).toHaveBeenCalledWith('/tenants/acme'));
  });
});

