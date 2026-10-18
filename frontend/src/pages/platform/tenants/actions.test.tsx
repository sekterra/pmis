import { fireEvent, render, screen } from '@testing-library/react';
import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import TenantActionBar from './TenantActionBar';

const labels = {
  suspend: '중지',
  resume: '재개',
  retry: '프로비저닝 재시도',
  reissue: '임시 비밀번호 재발급',
};

describe('tenant action buttons', () => {
  it('shows suspend only for an active tenant', () => {
    render(
      <TenantActionBar
        status="ACTIVE"
        labels={labels}
        onSuspend={vi.fn()}
        onResume={vi.fn()}
        onRetry={vi.fn()}
        onReissue={vi.fn()}
      />,
    );
    expect(screen.getByRole('button', { name: '중지' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: '재개' })).toBeNull();
    expect(
      screen.queryByRole('button', { name: '프로비저닝 재시도' }),
    ).toBeNull();
  });

  it('requires a change reason before suspend', () => {
    const onOk = vi.fn();
    function Reason() {
      const [reason, setReason] = React.useState('');
      const [error, setError] = React.useState<string>();
      return (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            if (!reason.trim()) {
              setError('변경 사유를 입력하세요.');
              return;
            }
            onOk(reason.trim());
          }}
        >
          <label>
            변경 사유
            <input
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
          <button type="submit">확인</button>
          {error ? <div role="alert">{error}</div> : null}
        </form>
      );
    }
    render(<Reason />);
    fireEvent.click(screen.getByRole('button', { name: '확인' }));
    expect(onOk).not.toHaveBeenCalled();
    expect(screen.getByRole('alert').textContent).toContain('변경 사유');
  });
});
