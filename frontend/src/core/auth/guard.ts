import { loginPathWithRedirect, safeRedirect } from './redirect';

export function nextPathAfterRecover(input: {
  pathname: string;
  search?: string;
  user?: { passwordChangeRequired: boolean } | null;
  redirectQuery?: string | null;
  home?: string;
}): string | null {
  const { pathname, user } = input;
  const search = input.search ?? '';
  if (!user) {
    if (pathname === '/user/login' || pathname.startsWith('/exception')) {
      return null;
    }
    return loginPathWithRedirect(pathname, search);
  }
  if (user.passwordChangeRequired) {
    if (pathname === '/user/password' || pathname === '/user/login') {
      return null;
    }
    return '/user/password';
  }
  if (pathname === '/user/login') {
    return safeRedirect(input.redirectQuery, input.home ?? '/welcome');
  }
  return null;
}
