import { describe, expect, it } from 'vitest';
import { buildMatrix, cloneMatrix, grantedCodes, isDirty, setGranted, toggleMenu, type GrantSource } from './matrix';

function grant(menuCode: string, menuName: string, action: string, granted = false): GrantSource {
  return { permissionCode: `${menuCode}:${action}`, menuCode, menuName, actionCode: action, granted };
}

describe('permission matrix', () => {
  const items = [
    grant('FILE', '파일', 'READ'),
    grant('COMPANY', '회사', 'CREATE'),
    grant('COMPANY', '회사', 'APPROVE'),
    grant('COMPANY', '회사', 'READ'),
    grant('DEPT', '부서', 'EXPORT'),
  ];

  it('keeps menu order and places known actions before the rest', () => {
    const matrix = buildMatrix(items);
    expect(matrix.rows.map((row) => row.menuCode)).toEqual(['FILE', 'COMPANY', 'DEPT']);
    expect(matrix.columns).toEqual(['READ', 'CREATE', 'EXPORT', 'APPROVE']);
    expect(matrix.rows[0]?.cells.CREATE).toBeUndefined();
    expect(matrix.rows[1]?.cells.EXPORT).toBeUndefined();
    expect(matrix.rows[1]?.menuName).toBe('회사');
  });

  it('links read with the other actions and toggles a whole menu', () => {
    const matrix = buildMatrix(items);
    const created = setGranted(matrix, 'COMPANY', 'CREATE', true);
    expect(created.rows[1]?.cells.CREATE?.granted).toBe(true);
    expect(created.rows[1]?.cells.READ?.granted).toBe(true);
    expect(created.rows[1]?.cells.APPROVE?.granted).toBe(false);
    const cleared = setGranted(created, 'COMPANY', 'READ', false);
    expect(cleared.rows[1]?.cells.CREATE?.granted).toBe(false);
    expect(cleared.rows[1]?.cells.READ?.granted).toBe(false);
    const all = toggleMenu(matrix, 'COMPANY');
    expect(Object.values(all.rows[1]?.cells ?? {}).every((cell) => cell.granted)).toBe(true);
    const none = toggleMenu(all, 'COMPANY');
    expect(Object.values(none.rows[1]?.cells ?? {}).every((cell) => !cell.granted)).toBe(true);
    expect(isDirty(all, matrix)).toBe(true);
    expect(isDirty(cloneMatrix(matrix), matrix)).toBe(false);
    expect(grantedCodes(created)).toEqual(['COMPANY:READ', 'COMPANY:CREATE']);
  });
});
