import { canAction } from '@/core/menu/can';
import type { MenuAction, MenuSnapshot } from '@/core/menu/types';

export default function access(initialState?: { menu?: MenuSnapshot }) {
  return {
    can: (menuCode: string, action: MenuAction) => canAction(initialState?.menu, menuCode, action),
  };
}
