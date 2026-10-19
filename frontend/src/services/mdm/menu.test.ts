import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchMyMenus, MY_MENUS_PATH } from './menu';

const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

describe('my menu service', () => {
  afterEach(() => {
    expect(fetchMock).not.toHaveBeenCalled();
    fetchMock.mockReset();
    rawRequest.mockReset();
  });

  it('requests the menus array without paging', async () => {
    vi.stubGlobal('fetch', fetchMock);
    rawRequest.mockResolvedValue([]);
    await fetchMyMenus();
    expect(MY_MENUS_PATH).toBe('/api/mdm/v1/menus/my');
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/menus/my');
  });
});
