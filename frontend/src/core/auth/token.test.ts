import { afterEach, describe, expect, it, vi } from 'vitest';
import { setAccessToken } from './token';

describe('access token storage', () => {
  afterEach(() => {
    setAccessToken(null);
    vi.restoreAllMocks();
  });

  it('does not write the access token to web storage', () => {
    const setLocal = vi.spyOn(Storage.prototype, 'setItem');
    setAccessToken('memory-only-token');
    expect(setLocal).not.toHaveBeenCalled();
    expect(window.localStorage.getItem('memory-only-token')).toBeNull();
    expect(window.sessionStorage.getItem('memory-only-token')).toBeNull();
    expect(JSON.stringify(window.localStorage)).not.toContain(
      'memory-only-token',
    );
    expect(JSON.stringify(window.sessionStorage)).not.toContain(
      'memory-only-token',
    );
  });
});
