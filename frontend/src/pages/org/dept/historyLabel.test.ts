import { describe, expect, it } from 'vitest';
import { parentChange, parentEdge } from './historyLabel';

describe('department history parent label', () => {
  it('prefers the name, then the pk, then a dash', () => {
    expect(parentEdge('본사', 1)).toBe('본사');
    expect(parentEdge('  ', 4)).toBe('4');
    expect(parentEdge(null, null)).toBe('-');
    expect(
      parentChange({
        beforeParentDeptName: null,
        beforeParentDeptPk: null,
        afterParentDeptName: null,
        afterParentDeptPk: null,
      }),
    ).toBe('- → -');
  });
});
