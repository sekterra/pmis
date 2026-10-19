import type { MenuAction, MenuSnapshot, MyMenu } from './types';

export function canAction(
  menu: MenuSnapshot | undefined,
  menuCode: string,
  action: MenuAction,
): boolean {
  if (menu?.status !== 'ready') {
    return false;
  }
  const item = menu.items.find((row) => row.menuCode === menuCode);
  return Boolean(item?.actions.includes(action));
}

export function hasRead(items: MyMenu[], menuCode: string): boolean {
  return items.some((row) => row.menuCode === menuCode && row.actions.includes('READ'));
}
