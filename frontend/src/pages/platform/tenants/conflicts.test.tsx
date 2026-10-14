import { render, screen } from '@testing-library/react';
import React from 'react';
import { describe, expect, it } from 'vitest';
import { ApiError } from '@/core/request/errors';

function Notice({ error }: { error: ApiError }) {
  const watched = [
    'TENANT.LAST_DOMAIN',
    'TENANT.DUPLICATE',
    'TENANT.INVALID_STATUS_TRANSITION',
  ];
  if (!watched.includes(error.errorCode ?? '')) {
    return null;
  }
  return <div role="alert">{error.message}</div>;
}

describe('tenant conflict messages', () => {
  it('shows the server message for the last domain and a duplicate domain', () => {
    const last = new ApiError('마지막 도메인은 삭제할 수 없습니다.');
    last.errorCode = 'TENANT.LAST_DOMAIN';
    const duplicate = new ApiError('도메인이 이미 있습니다.');
    duplicate.errorCode = 'TENANT.DUPLICATE';
    const { rerender } = render(<Notice error={last} />);
    expect(screen.getByRole('alert').textContent).toBe(
      '마지막 도메인은 삭제할 수 없습니다.',
    );
    rerender(<Notice error={duplicate} />);
    expect(screen.getByRole('alert').textContent).toBe(
      '도메인이 이미 있습니다.',
    );
    const transition = new ApiError('상태를 바꿀 수 없습니다.');
    transition.errorCode = 'TENANT.INVALID_STATUS_TRANSITION';
    rerender(<Notice error={transition} />);
    expect(screen.getByRole('alert').textContent).toBe(
      '상태를 바꿀 수 없습니다.',
    );
  });
});
