import { rawRequest } from '@/core/request/runtime';
import type { MyMenu } from '@/core/menu/types';

export const MY_MENUS_PATH = '/api/mdm/v1/menus/my';

export function fetchMyMenus(): Promise<MyMenu[]> {
  return rawRequest<MyMenu[]>(MY_MENUS_PATH);
}
