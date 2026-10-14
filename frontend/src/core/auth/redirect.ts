export function safeRedirect(
  raw: string | null | undefined,
  fallback = '/welcome',
): string {
  if (!raw) {
    return fallback;
  }
  let decoded = raw;
  try {
    decoded = decodeURIComponent(raw);
  } catch {
    return fallback;
  }
  if (!decoded.startsWith('/') || decoded.startsWith('//')) {
    return fallback;
  }
  if (decoded.includes('\\') || decoded.includes('://')) {
    return fallback;
  }
  return decoded;
}

export function loginPathWithRedirect(pathname: string, search = ''): string {
  const back = `${pathname}${search}`;
  return `/user/login?redirect=${encodeURIComponent(back)}`;
}
