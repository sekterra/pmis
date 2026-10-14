import { describe, expect, it } from 'vitest';
import { nextPathAfterRecover } from './guard';
import { safeRedirect } from './redirect';

describe('route guard', () => {
  it('blocks other paths while a password change is required', () => {
    const user = { passwordChangeRequired: true };
    expect(nextPathAfterRecover({ pathname: '/welcome', user })).toBe(
      '/user/password',
    );
    expect(
      nextPathAfterRecover({ pathname: '/user/password', user }),
    ).toBeNull();
    expect(
      nextPathAfterRecover({ pathname: '/user/login', user }),
    ).toBeNull();
  });

  it('rejects external redirect targets', () => {
    expect(safeRedirect('https://example.com')).toBe('/welcome');
    expect(safeRedirect('//example.com')).toBe('/welcome');
    expect(safeRedirect('/%2F%2Fexample.com')).toBe('/welcome');
    expect(safeRedirect('/welcome')).toBe('/welcome');
    expect(
      nextPathAfterRecover({
        pathname: '/user/login',
        user: { passwordChangeRequired: false },
        redirectQuery: 'https://example.com',
      }),
    ).toBe('/welcome');
  });
});
