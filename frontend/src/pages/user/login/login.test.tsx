import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { completeLogin } from '@/core/auth/actions';
import LoginPage from './index';

const replace = vi.fn();

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({ id }: { id: string }) => id,
  }),
  useModel: () => ({ setInitialState: vi.fn() }),
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
  });

  it('shows one login card and a single failure message', async () => {
    vi.mocked(completeLogin).mockRejectedValue(new Error('fail'));
    render(<LoginPage />);
    expect(screen.getByText('PMIS')).toBeTruthy();
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
});
