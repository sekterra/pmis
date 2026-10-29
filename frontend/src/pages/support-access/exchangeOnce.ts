import { beginSupportSession, setSupportTenantCode } from '@/core/auth/supportSession';
import { setAccessToken } from '@/core/auth/token';
import {
  clearTicketFromLocation,
  tenantCodeFromSearch,
} from '@/core/supportAccess/rules';
import { noteAuthenticated } from '@/core/menu/runtime';
import { me } from '@/services/mdm/auth';
import { exchangeSupportTicket } from '@/services/mdm/supportAccess';
import type { Me } from '@/services/mdm/data';

export type ExchangeResult =
  | { view: 'missing' }
  | { view: 'invalid' }
  | { view: 'done'; user: Me };

let pending: Promise<ExchangeResult> | null = null;

async function runExchange(): Promise<ExchangeResult> {
  const ticket = clearTicketFromLocation({
    pathname: window.location.pathname,
    search: window.location.search,
    hash: window.location.hash,
  });
  if (!ticket) {
    return { view: 'missing' };
  }
  const tenantCode = tenantCodeFromSearch(
    window.location.search,
    process.env.NODE_ENV === 'development',
  );
  if (process.env.NODE_ENV === 'development') {
    setSupportTenantCode(tenantCode);
  }
  try {
    const exchanged = await exchangeSupportTicket(ticket, tenantCode);
    setAccessToken(exchanged.accessToken);
    beginSupportSession();
    const user = await me();
    await noteAuthenticated(user);
    return { view: 'done', user };
  } catch {
    setSupportTenantCode(null);
    return { view: 'invalid' };
  }
}

/** StrictMode 이중 effect가 같은 교환 결과를 쓰도록 모듈 수준에서 한 번만 실행한다. */
export function exchangeSupportAccessOnce(): Promise<ExchangeResult> {
  if (!pending) {
    pending = runExchange().finally(() => {
      pending = null;
    });
  }
  return pending;
}

export function resetExchangeSupportAccessOnce(): void {
  pending = null;
}
