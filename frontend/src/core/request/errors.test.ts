import { describe, expect, it, vi } from 'vitest';
import { presentError, toApiError, toFormFields, unwrapData } from './errors';

describe('request envelope', () => {
  it('returns data when success is true and throws a string error code otherwise', () => {
    expect(unwrapData({ success: true, data: { id: 1 } })).toEqual({ id: 1 });
    expect(unwrapData({ keys: [] }, { wrapped: false })).toEqual({ keys: [] });
    const error = toApiError({
      success: false,
      errorCode: 'AUTH.INVALID_CREDENTIALS',
      errorMessage: '로그인에 실패했습니다.',
      showType: 2,
      traceId: 'trace-1',
      fieldErrors: [{ field: 'newPassword', message: '짧습니다' }],
    });
    expect(error.errorCode).toBe('AUTH.INVALID_CREDENTIALS');
    expect(toFormFields(error.fieldErrors)).toEqual([
      { name: 'newPassword', errors: ['짧습니다'] },
    ]);
  });

  it('presents showType and includes traceId', () => {
    const feedback = {
      warning: vi.fn(),
      error: vi.fn(),
      notify: vi.fn(),
    };
    const goLogin = vi.fn();
    presentError(
      toApiError({
        success: false,
        errorMessage: '주의',
        showType: 0,
        traceId: 't',
      }),
      feedback,
      goLogin,
    );
    presentError(
      toApiError({
        success: false,
        errorMessage: '주의',
        showType: 1,
        traceId: 't1',
      }),
      feedback,
      goLogin,
    );
    presentError(
      toApiError({
        success: false,
        errorMessage: '오류',
        showType: 2,
        traceId: 't2',
      }),
      feedback,
      goLogin,
    );
    presentError(
      toApiError({
        success: false,
        errorCode: 'COMMON.VALIDATION_FAILED',
        errorMessage: '알림',
        showType: 3,
        traceId: 't3',
      }),
      feedback,
      goLogin,
    );
    presentError(
      toApiError({ success: false, errorMessage: '이동', showType: 9 }),
      feedback,
      goLogin,
    );
    expect(feedback.warning).toHaveBeenCalledWith('주의 (t1)');
    expect(feedback.error).toHaveBeenCalledWith('오류 (t2)');
    expect(feedback.notify).toHaveBeenCalledWith(
      'COMMON.VALIDATION_FAILED',
      '알림 (t3)',
    );
    expect(goLogin).toHaveBeenCalledTimes(1);
  });
});
