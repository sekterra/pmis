import type { AccessCheck } from '@/core/menu/types';

/**
 * 권한 판정 자리다. 서버 메뉴 연동은 F4에서 채운다.
 */
const can: AccessCheck = () => false;

export default function access() {
  return { can };
}
