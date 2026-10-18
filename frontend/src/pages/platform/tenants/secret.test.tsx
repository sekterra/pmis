import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import React, { useState } from 'react';
import { describe, expect, it } from 'vitest';
import TemporaryPasswordModal from './TemporaryPasswordModal';

function Host() {
  const [open, setOpen] = useState(true);
  const [password, setPassword] = useState('Temp-Pass-1');
  return (
    <TemporaryPasswordModal
      open={open}
      loginId="admin@example.com"
      password={password}
      title="임시 비밀번호"
      hint="지금 복사하지 않으면 다시 볼 수 없습니다."
      noPasswordHint="이미 관리자 계정이 있어 새 임시 비밀번호를 만들지 않았습니다."
      loginLabel="관리자 로그인 아이디"
      passwordLabel="임시 비밀번호"
      copyLabel="복사"
      copyLoginLabel="아이디 복사"
      closeLabel="닫기"
      onClose={() => {
        setPassword('');
        setOpen(false);
      }}
    />
  );
}

describe('temporary password modal', () => {
  it('keeps the password only while the modal is open', async () => {
    render(<Host />);
    expect(screen.getByText(/Temp-Pass-1/)).toBeTruthy();
    fireEvent.click(screen.getByTestId('close-secret'));
    await waitFor(() => {
      expect(screen.queryByText(/Temp-Pass-1/)).toBeNull();
    });
    expect(JSON.stringify(window.localStorage)).not.toContain('Temp-Pass-1');
    expect(JSON.stringify(window.sessionStorage)).not.toContain('Temp-Pass-1');
    expect(window.location.href).not.toContain('Temp-Pass-1');
  });

  it('shows adminLoginId and hides the password when it is absent', () => {
    render(
      <TemporaryPasswordModal
        open
        loginId="stored-admin"
        title="임시 비밀번호"
        hint="지금 복사하지 않으면 다시 볼 수 없습니다."
        noPasswordHint="이미 관리자 계정이 있어 새 임시 비밀번호를 만들지 않았습니다."
        loginLabel="관리자 로그인 아이디"
        passwordLabel="임시 비밀번호"
        copyLabel="복사"
        copyLoginLabel="아이디 복사"
        closeLabel="닫기"
        onClose={() => undefined}
      />,
    );
    expect(screen.getByText(/stored-admin/)).toBeTruthy();
    expect(
      screen.getByText(
        '이미 관리자 계정이 있어 새 임시 비밀번호를 만들지 않았습니다.',
      ),
    ).toBeTruthy();
    expect(screen.queryByText(/임시 비밀번호:/)).toBeNull();
  });
});
