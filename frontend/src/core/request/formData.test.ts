import { describe, expect, it } from 'vitest';
import { headersForBody } from '@/core/request/runtime';
import { toBlobApiError } from '@/core/request/errors';

describe('request body headers', () => {
  it('drops a manual content type for form data', () => {
    const body = new FormData();
    body.append('file', new Blob(['png']), 'a.png');
    expect(headersForBody({ 'Content-Type': 'application/json', Authorization: 'Bearer t' }, body)).toEqual({
      Authorization: 'Bearer t',
    });
    expect(headersForBody({ 'Content-Type': 'application/json' }, { personName: '홍' })?.['Content-Type']).toBe(
      'application/json',
    );
  });

  it('reads an error envelope from a blob body', async () => {
    const error = await toBlobApiError({
      response: {
        status: 404,
        data: new Blob([JSON.stringify({ success: false, errorCode: 'FILE.NOT_FOUND', errorMessage: '' })]),
      },
    });
    expect(error.errorCode).toBe('FILE.NOT_FOUND');
    expect(error.status).toBe(404);
  });
});
