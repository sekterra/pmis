import { hasRead } from './can';
import { routeForPath } from './menuRoutes';
import type { MenuSnapshot } from './types';

export type GuardView = 'allow' | 'loading' | 'error' | 'forbidden';

export function guardView(pathname: string, menu: MenuSnapshot | undefined): GuardView {
  const route = routeForPath(pathname);
  if (!route) {
    return 'allow';
  }
  if (!menu || menu.status === 'idle' || menu.status === 'loading') {
    return 'loading';
  }
  if (menu.status === 'error') {
    return 'error';
  }
  return hasRead(menu.items, route.menuCode) ? 'allow' : 'forbidden';
}
