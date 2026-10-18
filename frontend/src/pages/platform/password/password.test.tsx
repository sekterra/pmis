import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getAccessToken, setAccessToken } from '@/core/auth/token';
import PasswordPage from './index';

const replace = vi.fn();
const logout = vi.hoisted(() => vi.fn());

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({
      id,
      defaultMessage,
    }: {
      id: string;
      defaultMessage?: string;
    }) => defaultMessage ?? id,
  }),
  useModel: () => ({
    initialState: { currentUser: { passwordChangeRequired: true } },
    setInitialState: vi.fn(),
  }),
  history: {
    replace: (...args: unknown[]) => replace(...args),
    back: vi.fn(),
  },
}));

vi.mock('@/services/platform/auth', () => ({
  logout: (...args: unknown[]) => logout(...args),
  changePassword: vi.fn(),
  me: vi.fn(),
}));

describe('platform password exit', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setAccessToken('keep');
    logout.mockResolvedValue(undefined);
  });

  it('logs out through the platform auth path and opens the login screen', async () => {
    render(<PasswordPage />);
    expect(screen.queryByRole('button', { name: '취소' })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: '다른 계정으로 로그인' }));
    await waitFor(() => {
      expect(logout).toHaveBeenCalled();
      expect(replace).toHaveBeenCalledWith('/user/login');
    });
    expect(getAccessToken()).toBeNull();
  });
});
