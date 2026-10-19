import type { ReactElement } from 'react';
import type { MenuAction, MenuSnapshot } from './types';

const idle: MenuSnapshot = { status: 'idle', items: [] };

export function menuDataRender<T>(items: T[]): T[] {
  return items;
}

export function viewFor(): 'allow' {
  return 'allow';
}

export function canMenu(
  _menu: MenuSnapshot | undefined,
  _menuCode: string,
  _action: MenuAction,
): boolean {
  return false;
}

export function MenuFailureNotice(): ReactElement | null {
  return null;
}

export function getMenuSnapshot(): MenuSnapshot {
  return idle;
}

export function noteAuthenticated(
  _user: { passwordChangeRequired: boolean } | null,
): Promise<MenuSnapshot> {
  return Promise.resolve(idle);
}

export function noteLocaleChanged(): Promise<MenuSnapshot> {
  return Promise.resolve(idle);
}

export function noteLoggedOut(): void {}

export function registerUnauthorized(_handler: () => void): void {}

export function retryMenus(): Promise<MenuSnapshot> {
  return Promise.resolve(idle);
}

export function subscribeMenu(): () => void {
  return () => undefined;
}
