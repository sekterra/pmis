const FLAG_KEY = 'mdm_support_session';

let active = false;

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
  try {
    sessionStorage.removeItem(FLAG_KEY);
  } catch {
    // ignore
  }
}
