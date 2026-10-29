import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const startSupportAccess = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());

vi.mock('@/services/platform/supportAccess', () => ({
  startSupportAccess: (...args: unknown[]) => startSupportAccess(...args),
}));
vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));
vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: (
      { defaultMessage }: { defaultMessage?: string },
      values?: Record<string, unknown>,
    ) => {
      let text = defaultMessage ?? '';
      if (values) {
        for (const [key, value] of Object.entries(values)) {
          text = text.replace(`{${key}}`, String(value));
        }
      }
      return text;
    },
  }),
}));

import SupportAccessModal from './SupportAccessModal';

describe('support access modal', () => {
  beforeEach(() => {
    startSupportAccess.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('issues access and keeps the url only in modal state until close', async () => {
    const onClose = vi.fn();
    const accessUrl =
      'http://localhost:8000/support-access?tenant=acme#ticket=modal-ticket';
    startSupportAccess.mockResolvedValue({
      supportAccessId: 1,
      accessUrl,
      ticketExpireDatetime: new Date(Date.now() + 60_000).toISOString(),
    });
    const { rerender, unmount } = render(
      <SupportAccessModal open tenantCode="acme" onClose={onClose} />,
    );
    fireEvent.click(screen.getByRole('button', { name: '접속 준비' }));
    await waitFor(() =>
      expect(screen.getByRole('link', { name: '새 탭에서 열기' })).toBeTruthy(),
    );
    const link = screen.getByRole('link', { name: '새 탭에서 열기' });
    expect(link.getAttribute('href')).toBe(accessUrl);
    expect(link.getAttribute('target')).toBe('_blank');
    expect(startSupportAccess).toHaveBeenCalledWith('acme', '');
    expect(sessionStorage.getItem(accessUrl)).toBeNull();
    expect(localStorage.getItem(accessUrl)).toBeNull();
    fireEvent.click(link);
    fireEvent.click(screen.getByRole('button', { name: '닫기' }));
    expect(onClose).toHaveBeenCalled();
    rerender(
      <SupportAccessModal open={false} tenantCode="acme" onClose={onClose} />,
    );
    unmount();
    expect(document.body.textContent ?? '').not.toContain('modal-ticket');
  });

  it('disables the open link when the countdown reaches zero', async () => {
    startSupportAccess.mockResolvedValue({
      supportAccessId: 1,
      accessUrl: 'http://localhost:8000/support-access#ticket=soon',
      ticketExpireDatetime: new Date(Date.now() - 1000).toISOString(),
    });
    render(<SupportAccessModal open tenantCode="acme" onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: '접속 준비' }));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: '다시 발급' })).toBeTruthy(),
    );
    expect(screen.queryByRole('link', { name: '새 탭에서 열기' })).toBeNull();
  });
});
