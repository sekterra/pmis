import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockRefresh = vi.fn();
const mockMe = vi.fn();

vi.mock('@umijs/max', () => ({
  history: {
    location: { pathname: '/welcome', search: '', hash: '' },
    replace: mockReplace,
  },
  Link: ({ children }: { children?: ReactNode }) => children,
  request: vi.fn(),
  useLocation: () => ({ pathname: '/welcome', search: '' }),
  useModel: () => ({ initialState: {}, loading: false }),
  useEffect: () => undefined,
}));

vi.mock('@/core/i18n/apply', () => ({
  loadPublicI18n: vi.fn(async () => undefined),
}));

vi.mock('@/services/mdm/auth', () => ({
  refresh: (...args: unknown[]) => mockRefresh(...args),
  me: (...args: unknown[]) => mockMe(...args),
  logout: vi.fn(),
}));

vi.mock('@/components', () => ({
  BrandMark: () => null,
  ErrorBoundary: ({ children }: { children?: ReactNode }) => children,
  FeedbackBridge: () => null,
  Footer: () => null,
  LanguageSelect: () => null,
  OfflineBanner: () => null,
  RouteGuard: ({ children }: { children?: ReactNode }) => children,
  UserMenu: () => null,
}));

vi.mock('antd', () => ({
  App: ({ children }: { children?: ReactNode }) => children,
}));

vi.mock('../config/defaultSettings', () => ({
  default: { title: 'PMIS' },
}));

describe('app startup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRefresh.mockResolvedValue({
      accessToken: 'access',
      tokenType: 'Bearer',
      expiresIn: 900,
      passwordChangeRequired: false,
    });
    mockMe.mockResolvedValue({
      userPk: 1,
      loginId: 'admin@example.com',
      displayName: '관리자',
      supportLanguageCode: 'ko-KR',
      timezoneId: 'Asia/Seoul',
      roleCodes: ['TENANT_ADMIN'],
      passwordChangeRequired: false,
    });
  });

  it('returns before session recovery so the first screen can paint', async () => {
    const { getInitialState } = await import('./app');
    const state = await getInitialState();
    expect(state.loading).toBe(true);
    expect(state.currentUser).toBeUndefined();
    expect(mockRefresh).not.toHaveBeenCalled();
    expect(mockReplace).not.toHaveBeenCalled();
  });
});
