const FLAG_KEY = 'mdm_support_session';

let active = false;
/** 개발 모드 지원 세션에서만 /api/mdm 에 붙일 테넌트 코드(메모리). */
let supportTenantCode: string | null = null;

export function isSupportSession(): boolean {
  return active;
}

export function beginSupportSession(): void {
  active = true;
  try {
    sessionStorage.setItem(FLAG_KEY, '1');
  } catch {
    // ignore
  }
}

export function endSupportSession(): void {
  active = false;
  supportTenantCode = null;
  try {
    sessionStorage.removeItem(FLAG_KEY);
  } catch {
    // ignore
  }
}

export function hadSupportSession(): boolean {
  if (active) {
    return true;
  }
  try {
    return sessionStorage.getItem(FLAG_KEY) === '1';
  } catch {
    return false;
  }
}

export function clearSupportSessionFlag(): void {
  active = false;
  supportTenantCode = null;
  try {
    sessionStorage.removeItem(FLAG_KEY);
  } catch {
    // ignore
  }
}

export function setSupportTenantCode(code: string | null | undefined): void {
  const trimmed = code?.trim();
  supportTenantCode = trimmed ? trimmed : null;
}

export function getSupportTenantCode(): string | null {
  return supportTenantCode;
}
