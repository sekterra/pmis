import { resetCodeOptionsCache } from '@/core/codes/useCodeOptions';
import { toApiError } from '@/core/request/errors';
import { accessKind } from '@/core/auth/context';
import { fetchMyMenus } from '@/services/mdm/menu';
import type { MenuSnapshot } from './types';

const idle = (): MenuSnapshot => ({ status: 'idle', items: [] });

let snapshot: MenuSnapshot = idle();
let allowed = false;
let epoch = 0;
let pending: Promise<MenuSnapshot> | null = null;
let onUnauthorized: () => void = () => undefined;
const listeners = new Set<() => void>();

function emit(): void {
  for (const listener of listeners) {
    listener();
  }
}

export function getMenuSnapshot(): MenuSnapshot {
  return snapshot;
}

export function subscribeMenu(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function registerUnauthorized(handler: () => void): void {
  onUnauthorized = handler;
}

export function noteLoggedOut(): void {
  resetCodeOptionsCache();
  epoch += 1;
  allowed = false;
  pending = null;
  snapshot = idle();
  emit();
}

function isPasswordChange(error: unknown): boolean {
  return toApiError(error).errorCode === 'AUTH.PASSWORD_CHANGE_REQUIRED';
}

function isUnauthorized(error: unknown): boolean {
  const api = toApiError(error);
  return (
    api.status === 401 ||
    api.errorCode === 'AUTH.UNAUTHORIZED' ||
    api.errorCode === 'AUTH.TOKEN_EXPIRED' ||
    api.errorCode === 'AUTH.TOKEN_REUSED'
  );
}

export function loadMenus(): Promise<MenuSnapshot> {
  if (accessKind() === 'PLATFORM') {
    snapshot = { status: 'ready', items: [] };
    emit();
    return Promise.resolve(snapshot);
  }
  if (!allowed) {
    return Promise.resolve(snapshot);
  }
  if (pending) {
    return pending;
  }
  const generation = epoch;
  snapshot = { status: 'loading', items: [] };
  emit();
  let task!: Promise<MenuSnapshot>;
  task = Promise.resolve()
    .then(() => fetchMyMenus())
    .then((items) => {
      if (generation !== epoch) {
        return snapshot;
      }
      snapshot = { status: 'ready', items };
      emit();
      return snapshot;
    })
    .catch((error: unknown) => {
      if (generation !== epoch) {
        return snapshot;
      }
      if (isPasswordChange(error)) {
        snapshot = idle();
        emit();
        return snapshot;
      }
      if (isUnauthorized(error)) {
        snapshot = idle();
        emit();
        onUnauthorized();
        return snapshot;
      }
      snapshot = { status: 'error', items: [] };
      emit();
      return snapshot;
    })
    .finally(() => {
      if (pending === task) {
        pending = null;
      }
    });
  pending = task;
  return task;
}

export function noteAuthenticated(user: { passwordChangeRequired: boolean } | null): Promise<MenuSnapshot> {
  if (!user || user.passwordChangeRequired) {
    noteLoggedOut();
    return Promise.resolve(snapshot);
  }
  allowed = true;
  return loadMenus();
}

export function noteLocaleChanged(): Promise<MenuSnapshot> {
  if (!allowed) {
    return Promise.resolve(snapshot);
  }
  return loadMenus();
}

export function retryMenus(): Promise<MenuSnapshot> {
  return loadMenus();
}

export function resetMenuState(): void {
  epoch += 1;
  allowed = false;
  pending = null;
  snapshot = idle();
  onUnauthorized = () => undefined;
  emit();
}
