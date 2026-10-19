import { describe, expect, it } from 'vitest';
import access from './access';
import type { MenuSnapshot } from './core/menu/types';

const ready = (actions: string[]): MenuSnapshot => ({
  status: 'ready',
  items: [
    {
      menuCode: 'I18N',
      parentMenuCode: 'SYSTEM',
      menuName: '다국어',
      menuTypeCode: 'PAGE',
      routePath: '/system/i18n',
      iconName: null,
      levelNo: 2,
      sortOrder: 10,
      actions,
    },
  ],
});

describe('access', () => {
  it('allows an action that the menu lists', () => {
    expect(access({ menu: ready(['READ', 'CREATE']) }).can('I18N', 'CREATE')).toBe(true);
  });

  it('rejects a missing action, a missing menu, and a menu that is not ready', () => {
    expect(access({ menu: ready(['READ']) }).can('I18N', 'DELETE')).toBe(false);
    expect(access({ menu: ready(['READ']) }).can('COMMON_CODE', 'READ')).toBe(false);
    expect(access({ menu: { status: 'loading', items: ready(['READ']).items } }).can('I18N', 'READ')).toBe(false);
    expect(access({ menu: { status: 'error', items: [] } }).can('I18N', 'READ')).toBe(false);
    expect(access().can('I18N', 'READ')).toBe(false);
  });
});
