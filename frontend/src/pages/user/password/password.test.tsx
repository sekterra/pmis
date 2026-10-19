import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getAccessToken, setAccessToken } from '@/core/auth/token';
import { ApiError } from '@/core/request/errors';
import PasswordPage from './index';

const replace = vi.fn();
const back = vi.fn();
const logout = vi.hoisted(() => vi.fn());
const changePassword = vi.hoisted(() => vi.fn());
const me = vi.hoisted(() => vi.fn());
const state = vi.hoisted(() => ({
  passwordChangeRequired: true as boolean,
}));

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({ id }: { id: string }) => id,
  }),
  useModel: () => ({
    initialState: {
      currentUser: { passwordChangeRequired: state.passwordChangeRequired },
    },
    setInitialState: vi.fn(),
  }),
  history: {
    replace: (...args: unknown[]) => replace(...args),
    back: () => back(),
  },
}));

vi.mock('@/services/mdm/menu', () => ({
  fetchMyMenus: vi.fn(async () => []),
}));

vi.mock('@/services/mdm/auth', () => ({
  logout: (...args: unknown[]) => logout(...args),
  changePassword: (...args: unknown[]) => changePassword(...args),
  me: (...args: unknown[]) => me(...args),
}));

describe('password page exit', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    state.passwordChangeRequired = true;
    setAccessToken('keep');
    logout.mockResolvedValue(undefined);
  });

  it('logs out and goes to the login screen', async () => {
    render(<PasswordPage />);
    fireEvent.click(screen.getByRole('button', { name: 'pages.password.switchAccount' }));
    await waitFor(() => {
      expect(logout).toHaveBeenCalled();
      expect(replace).toHaveBeenCalledWith('/user/login');
    });
    expect(getAccessToken()).toBeNull();
    expect(screen.queryByRole('button', { name: 'pages.password.cancel' })).toBeNull();
  });

  it('clears local state when logout fails', async () => {
    logout.mockRejectedValue(new Error('down'));
    render(<PasswordPage />);
    fireEvent.click(screen.getByRole('button', { name: 'pages.password.switchAccount' }));
    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith('/user/login');
    });
    expect(getAccessToken()).toBeNull();
  });

  it('offers cancel only for a voluntary change', () => {
    state.passwordChangeRequired = false;
    render(<PasswordPage />);
    fireEvent.click(screen.getByRole('button', { name: 'pages.password.cancel' }));
    expect(replace).toHaveBeenCalledWith('/welcome');
  });

  it('shows the server message when the change fails and goes home after success', async () => {
    const policy = new ApiError('비밀번호 정책에 맞지 않습니다.');
    changePassword.mockRejectedValueOnce(policy);
    render(<PasswordPage />);
    fireEvent.change(screen.getByLabelText('pages.password.current'), {
      target: { value: 'old-secret' },
    });
    fireEvent.change(screen.getByLabelText('pages.password.new'), {
      target: { value: 'new-secret-1' },
    });
    fireEvent.change(screen.getByLabelText('pages.password.confirm'), {
      target: { value: 'new-secret-1' },
    });
    fireEvent.submit(document.querySelector('form') as HTMLFormElement);
    expect(await screen.findByRole('alert')).toHaveTextContent('비밀번호 정책에 맞지 않습니다.');

    changePassword.mockResolvedValueOnce({
      accessToken: 'replaced',
      tokenType: 'Bearer',
      expiresIn: 900,
      passwordChangeRequired: false,
    });
    me.mockResolvedValueOnce({
      userPk: 1,
      loginId: 'admin',
      displayName: 'Admin',
      supportLanguageCode: 'ko-KR',
      timezoneId: 'Asia/Seoul',
      roleCodes: [],
      passwordChangeRequired: false,
    });
    fireEvent.submit(document.querySelector('form') as HTMLFormElement);
    await waitFor(() => {
      expect(getAccessToken()).toBe('replaced');
      expect(replace).toHaveBeenCalledWith('/welcome');
    });
  });
});
