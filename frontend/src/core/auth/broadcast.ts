import { clearAccessToken } from './token';

export const AUTH_CHANNEL = 'pmis-auth';

export type AuthBroadcast = { type: 'logout' };

export function applyRemoteLogout(): void {
  clearAccessToken();
}

export function publishLogout(channel?: {
  postMessage: (data: AuthBroadcast) => void;
}): void {
  channel?.postMessage({ type: 'logout' });
}

export function subscribeAuthBroadcast(onLogout: () => void): () => void {
  if (typeof BroadcastChannel === 'undefined') {
    return () => undefined;
  }
  const channel = new BroadcastChannel(AUTH_CHANNEL);
  channel.onmessage = (event: MessageEvent<AuthBroadcast>) => {
    if (event.data?.type === 'logout') {
      applyRemoteLogout();
      onLogout();
    }
  };
  return () => channel.close();
}
