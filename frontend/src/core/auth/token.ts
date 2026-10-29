import { endSupportSession } from './supportSession';

let accessToken: string | null = null;
let epoch = 0;

export function getAccessToken(): string | null {
  return accessToken;
}

export function accessTokenEpoch(): number {
  return epoch;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
  epoch += 1;
}

export function clearAccessToken(): void {
  accessToken = null;
  epoch += 1;
  endSupportSession();
}
