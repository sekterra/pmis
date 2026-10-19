import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { completeLogin } from '@/core/auth/actions';
import LoginPage from './index';

const replace = vi.fn();

const pageState = vi.hoisted(() => ({ contextStatus: undefined as string | undefined }));

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({ id, defaultMessage }: { id: string; defaultMessage?: string }) =>
      defaultMessage ?? id,
  }),
  useModel: () => ({
    setInitialState: vi.fn(),
    initialState: { contextStatus: pageState.contextStatus },
  }),
  useSearchParams: () => [new URLSearchParams('redirect=/welcome')],
  history: { replace: (...args: unknown[]) => replace(...args) },
}));

vi.mock('@/components/LanguageSelect', () => ({
  default: () => <div>language</div>,
}));

vi.mock('@/core/auth/actions', () => ({
  completeLogin: vi.fn(),
  invalidCredentialsMessage: () => '로그인에 실패했습니다.',
}));

vi.mock('@/services/mdm/auth', () => ({
  login: vi.fn(),
  me: vi.fn(),
}));

describe('Login page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    pageState.contextStatus = undefined;
  });

  it('shows one login card and a single failure message', async () => {
    vi.mocked(completeLogin).mockRejectedValue(new Error('fail'));
    render(<LoginPage />);
    expect(screen.getByText('pages.login.loginId')).toBeTruthy();
    expect(screen.queryByText('pages.login.phoneLogin.tab')).toBeNull();
    fireEvent.change(screen.getByLabelText('pages.login.loginId'), {
      target: { value: 'admin@example.com' },
    });
    fireEvent.change(screen.getByLabelText('pages.login.password'), {
      target: { value: 'secret' },
    });
    fireEvent.submit(document.querySelector('form') as HTMLFormElement);
    expect(await screen.findByRole('alert')).toHaveTextContent(
      '로그인에 실패했습니다.',
    );
  });

  it('goes to the password screen when a change is required', async () => {
    vi.mocked(completeLogin).mockResolvedValue({ path: '/user/password' });
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('pages.login.loginId'), {
      target: { value: 'admin@example.com' },
    });
    fireEvent.change(screen.getByLabelText('pages.login.password'), {
      target: { value: 'secret' },
    });
    fireEvent.submit(document.querySelector('form') as HTMLFormElement);
    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith('/user/password');
    });
  });

  it('hides the form when the server cannot be reached', () => {
    pageState.contextStatus = 'offline';
    render(<LoginPage />);
    expect(screen.getByText('서버에 연결할 수 없습니다')).toBeTruthy();
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeTruthy();
    expect(screen.queryByLabelText('pages.login.loginId')).toBeNull();
  });

  it('hides the form for an unknown site', () => {
    pageState.contextStatus = 'unknown';
    render(<LoginPage />);
    expect(screen.getByText('알 수 없는 사이트입니다')).toBeTruthy();
    expect(screen.queryByRole('button', { name: '다시 시도' })).toBeNull();
    expect(screen.queryByLabelText('pages.login.loginId')).toBeNull();
  });
});
