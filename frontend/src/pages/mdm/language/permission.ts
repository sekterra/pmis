import type { MenuAction } from '@/core/menu/types';

/**
 * 서버 메뉴(menus/my) 연동 전 임시 허용.
 * F5에서는 이 함수 본문을 `return can('I18N', action)` 으로 바꾼다.
 */
export function grantLanguage(
  can: (menuCode: string, action: MenuAction) => boolean,
  action: MenuAction,
): boolean {
  void can;
  void action;
  return true;
}
