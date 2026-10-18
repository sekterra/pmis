import { describe, expect, it } from 'vitest';
import access from './access';

describe('access', () => {
  it('keeps a permission placeholder until menu permissions are connected', () => {
    expect(access().can('COMMON_CODE', 'UPDATE')).toBe(false);
  });
});
