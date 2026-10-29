import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { areaForbidden, setAccessContext } from '@/core/auth/context';
import {
  beginSupportSession,
  clearSupportSessionFlag,
  endSupportSession,
  getSupportTenantCode,
  isSupportSession,
} from '@/core/auth/supportSession';
import { clearAccessToken, getAccessToken, setAccessToken } from '@/core/auth/token';
import { resetExchangeSupportAccessOnce } from './exchangeOnce';

const exchangeSupportTicket = vi.hoisted(() => vi.fn());
const me = vi.hoisted(() => vi.fn());
const noteAuthenticated = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
const replace = vi.hoisted(() => vi.fn());

vi.mock('@/services/mdm/supportAccess', () => ({
  exchangeSupportTicket: (...args: unknown[]) => exchangeSupportTicket(...args),
}));
vi.mock('@/services/mdm/auth', () => ({
  me: (...args: unknown[]) => me(...args),
}));
vi.mock('@/core/menu/runtime', () => ({
  noteAuthenticated: (...args: unknown[]) => noteAuthenticated(...args),
}));
vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));
vi.mock('@umijs/max', () => ({
  history: { replace: (...args: unknown[]) => replace(...args), location: { pathname: '/' } },
  useIntl: () => ({
    formatMessage: ({ defaultMessage }: { defaultMessage?: string }) =>
      defaultMessage ?? '',
  }),
}));

import SupportAccessEntryPage from './index';
import SupportAccessEndedPage from './ended';

describe('support access entry flow', () => {
  beforeEach(() => {
    exchangeSupportTicket.mockReset();
    me.mockReset();
    noteAuthenticated.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    replace.mockReset();
    vi.stubGlobal('fetch', fetchMock);
    clearAccessToken();
    clearSupportSessionFlag();
    endSupportSession();
    resetExchangeSupportAccessOnce();
    window.history.replaceState(null, '', '/support-access');
  });

  afterEach(() => {
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
    clearAccessToken();
    clearSupportSessionFlag();
    endSupportSession();
    resetExchangeSupportAccessOnce();
  });

  it('shows a missing-ticket message when the hash has no ticket', async () => {
    window.history.replaceState(null, '', '/support-access');
    render(<SupportAccessEntryPage />);
    expect(
      await screen.findByText('유효하지 않은 접속 주소입니다'),
    ).toBeTruthy();
    expect(exchangeSupportTicket).not.toHaveBeenCalled();
  });

  it('exchanges a ticket, stores only the memory token, and leaves no ticket in the location', async () => {
    window.history.replaceState(
      null,
      '',
      '/support-access?tenant=acme#ticket=one-time-ticket',
    );
    exchangeSupportTicket.mockResolvedValue({
      accessToken: 'support-access-token',
      expiresInSeconds: 1800,
      supportAccessId: 3,
    });
    me.mockResolvedValue({
      userPk: 1,
      loginId: '$platform-support',
      displayName: '플랫폼 지원 (ops)',
      supportLanguageCode: 'ko-KR',
      timezoneId: 'Asia/Seoul',
      roleCodes: ['TENANT_ADMIN'],
      passwordChangeRequired: false,
      support: {
        supportAccessId: 3,
        operatorLoginId: 'ops@example.com',
        expireDatetime: '2099-01-01T00:00:00Z',
      },
    });
    render(<SupportAccessEntryPage />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith('/'));
    expect(exchangeSupportTicket).toHaveBeenCalledWith(
      'one-time-ticket',
      process.env.NODE_ENV === 'development' ? 'acme' : null,
    );
    expect(getAccessToken()).toBe('support-access-token');
    expect(isSupportSession()).toBe(true);
    if (process.env.NODE_ENV === 'development') {
      expect(getSupportTenantCode()).toBe('acme');
    }
    expect(window.location.hash).toBe('');
    expect(window.location.href).not.toContain('one-time-ticket');
    expect(sessionStorage.getItem('mdm_support_session')).toBe('1');
    expect(JSON.stringify(window.history.state)).not.toContain('one-time-ticket');
  });

  it('shares one exchange under StrictMode double mount', async () => {
    window.history.replaceState(
      null,
      '',
      '/support-access?tenant=acme#ticket=strict-ticket',
    );
    exchangeSupportTicket.mockResolvedValue({
      accessToken: 'support-access-token',
      expiresInSeconds: 1800,
      supportAccessId: 3,
    });
    me.mockResolvedValue({
      userPk: 1,
      loginId: '$platform-support',
      displayName: '플랫폼 지원 (ops)',
      supportLanguageCode: 'ko-KR',
      timezoneId: 'Asia/Seoul',
      roleCodes: ['TENANT_ADMIN'],
      passwordChangeRequired: false,
      support: {
        supportAccessId: 3,
        operatorLoginId: 'ops@example.com',
        expireDatetime: '2099-01-01T00:00:00Z',
      },
    });
    render(
      <React.StrictMode>
        <SupportAccessEntryPage />
      </React.StrictMode>,
    );
    await waitFor(() => expect(replace).toHaveBeenCalledWith('/'));
    expect(exchangeSupportTicket).toHaveBeenCalledTimes(1);
    expect(getAccessToken()).toBe('support-access-token');
    expect(screen.queryByText('유효하지 않은 접속 주소입니다')).toBeNull();
  });

  it('shows the expired ticket screen without refreshing', async () => {
    window.history.replaceState(null, '', '/support-access#ticket=used');
    exchangeSupportTicket.mockRejectedValue({
      errorCode: 'AUTH.SUPPORT_TICKET_INVALID',
      errorMessage: 'invalid',
    });
    render(<SupportAccessEntryPage />);
    expect(
      await screen.findByText(
        '접속 주소가 만료되었거나 이미 사용되었습니다. 플랫폼 콘솔에서 다시 접속하세요',
      ),
    ).toBeTruthy();
    expect(getAccessToken()).toBeNull();
  });

  it('renders the ended screen and blocks platform users from tenant history', () => {
    render(<SupportAccessEndedPage />);
    expect(screen.getByText('지원 접속이 종료되었습니다')).toBeTruthy();
    setAccessContext({ kind: 'PLATFORM' });
    expect(areaForbidden('/access/support-access')).toBe(true);
    setAccessContext({ kind: 'TENANT' });
    expect(areaForbidden('/access/support-access')).toBe(false);
  });

  it('clears the support flag when the access token is cleared', () => {
    beginSupportSession();
    setAccessToken('x');
    expect(isSupportSession()).toBe(true);
    clearAccessToken();
    expect(isSupportSession()).toBe(false);
    expect(getSupportTenantCode()).toBeNull();
  });
});
