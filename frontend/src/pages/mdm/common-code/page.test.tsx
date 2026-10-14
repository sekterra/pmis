import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CommonCodePage from './index';

const listCodeGroups = vi.hoisted(() => vi.fn());
const listCommonCodes = vi.hoisted(() => vi.fn());
const getCodeGroup = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());

vi.mock('@ant-design/pro-components', () => {
  const ReactMock = require('react') as typeof import('react');
  return {
    PageContainer: ({
      children,
      title,
    }: {
      children?: React.ReactNode;
      title?: string;
    }) => ReactMock.createElement('div', null, title, children),
  };
});

vi.mock('./CodeFormDrawer', () => ({ default: () => null }));
vi.mock('./GroupFormDrawer', () => ({ default: () => null }));
vi.mock('./HistoryDrawer', () => ({ default: () => null }));

vi.mock('@umijs/max', () => {
  const ReactMock = require('react') as typeof import('react');
  return {
    useIntl: () => ({
      formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) =>
        defaultMessage ?? id,
    }),
    useAccess: () => ({ can: () => false }),
    useSearchParams: () => {
      const [params, setParams] = ReactMock.useState(() => new URLSearchParams());
      return [params, setParams];
    },
  };
});

vi.mock('@/services/mdm/codes', () => ({
  listCodeGroups: (...args: unknown[]) => listCodeGroups(...args),
  listCommonCodes: (...args: unknown[]) => listCommonCodes(...args),
  getCodeGroup: (...args: unknown[]) => getCodeGroup(...args),
  createCodeGroup: vi.fn(),
  updateCodeGroup: vi.fn(),
  deleteCodeGroup: vi.fn(),
  restoreCodeGroup: vi.fn(),
  createCommonCode: vi.fn(),
  updateCommonCode: vi.fn(),
  deleteCommonCode: vi.fn(),
  restoreCommonCode: vi.fn(),
  listCodeHistory: vi.fn(),
  listGroupCodes: vi.fn(),
}));

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: (...args: unknown[]) => fetchLanguages(...args),
}));

import { activeGroup, pageOf } from './fixtures';

describe('common code page', () => {
  beforeEach(() => {
    listCodeGroups.mockReset();
    listCommonCodes.mockReset();
    getCodeGroup.mockReset();
    fetchLanguages.mockReset();
    fetchLanguages.mockResolvedValue([]);
    listCodeGroups.mockResolvedValue(pageOf([activeGroup]));
    getCodeGroup.mockResolvedValue(activeGroup);
    listCommonCodes.mockResolvedValue({ items: [], total: 0, page: 1, size: 20 });
  });

  it('loads codes for the group that was selected', async () => {
    render(
      <App>
        <CommonCodePage />
      </App>,
    );
    fireEvent.click(await screen.findByText('상태'));
    await waitFor(() => {
      expect(listCommonCodes).toHaveBeenCalledWith(
        expect.objectContaining({ groupCode: 'STATUS', page: 1, size: 20 }),
      );
    });
  });
});
