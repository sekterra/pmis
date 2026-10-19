import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import CommonCodePage from './index';
import {
  activeGroup,
  code,
  fixedAt,
  hierarchyGroup,
  historyRow,
  inactiveGroup,
  languages,
  pageOf,
} from './fixtures';

const listCodeGroups = vi.hoisted(() => vi.fn());
const listCommonCodes = vi.hoisted(() => vi.fn());
const getCodeGroup = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());
const createCodeGroup = vi.hoisted(() => vi.fn());
const updateCodeGroup = vi.hoisted(() => vi.fn());
const deleteCodeGroup = vi.hoisted(() => vi.fn());
const restoreCodeGroup = vi.hoisted(() => vi.fn());
const createCommonCode = vi.hoisted(() => vi.fn());
const updateCommonCode = vi.hoisted(() => vi.fn());
const listCodeHistory = vi.hoisted(() => vi.fn());
const listGroupCodes = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const searchBag = vi.hoisted(() => ({ initial: '', text: '' }));
const fetchMock = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

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

vi.mock('@umijs/max', () => {
  const ReactMock = require('react') as typeof import('react');
  return {
    useIntl: () => ({
      formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) =>
        defaultMessage ?? id,
    }),
    useAccess: () => ({ can: () => true }),
    useSearchParams: () => {
      const [params, setParams] = ReactMock.useState(() => {
        const next = new URLSearchParams(searchBag.initial);
        searchBag.text = next.toString();
        return next;
      });
      const update = (next: URLSearchParams) => {
        searchBag.text = next.toString();
        setParams(next);
      };
      return [params, update];
    },
  };
});

vi.mock('@/services/mdm/codes', () => ({
  listCodeGroups: (...args: unknown[]) => listCodeGroups(...args),
  listCommonCodes: (...args: unknown[]) => listCommonCodes(...args),
  getCodeGroup: (...args: unknown[]) => getCodeGroup(...args),
  createCodeGroup: (...args: unknown[]) => createCodeGroup(...args),
  updateCodeGroup: (...args: unknown[]) => updateCodeGroup(...args),
  deleteCodeGroup: (...args: unknown[]) => deleteCodeGroup(...args),
  restoreCodeGroup: (...args: unknown[]) => restoreCodeGroup(...args),
  createCommonCode: (...args: unknown[]) => createCommonCode(...args),
  updateCommonCode: (...args: unknown[]) => updateCommonCode(...args),
  deleteCommonCode: vi.fn(),
  restoreCommonCode: vi.fn(),
  listCodeHistory: (...args: unknown[]) => listCodeHistory(...args),
  listGroupCodes: (...args: unknown[]) => listGroupCodes(...args),
}));

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: (...args: unknown[]) => fetchLanguages(...args),
}));

function renderPage() {
  return render(
    <App>
      <CommonCodePage />
    </App>,
  );
}

function rejection(codeName: string, message = ''): ApiError {
  const error = new ApiError(message);
  error.errorCode = codeName;
  return error;
}

async function openGroupCreate() {
  fireEvent.click(screen.getByRole('button', { name: '그룹 등록' }));
  return screen.findByRole('dialog');
}

describe('common code flows', () => {
  beforeEach(() => {
    searchBag.initial = '';
    searchBag.text = '';
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
    rawRequest.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    for (const fn of [
      listCodeGroups,
      listCommonCodes,
      getCodeGroup,
      fetchLanguages,
      createCodeGroup,
      updateCodeGroup,
      deleteCodeGroup,
      restoreCodeGroup,
      createCommonCode,
      updateCommonCode,
      listCodeHistory,
      listGroupCodes,
    ]) {
      fn.mockReset();
    }
    fetchLanguages.mockResolvedValue(languages);
    listCodeGroups.mockResolvedValue(pageOf([activeGroup]));
    getCodeGroup.mockResolvedValue(activeGroup);
    listCommonCodes.mockResolvedValue(pageOf([]));
    createCodeGroup.mockResolvedValue(activeGroup);
    updateCodeGroup.mockResolvedValue(activeGroup);
    deleteCodeGroup.mockResolvedValue(undefined);
    restoreCodeGroup.mockResolvedValue(inactiveGroup);
    createCommonCode.mockResolvedValue(code({ codePk: 11, codeValue: 'ON' }));
    updateCommonCode.mockResolvedValue(code({ codePk: 11, codeValue: 'ON' }));
    listCodeHistory.mockResolvedValue(pageOf([historyRow]));
    listGroupCodes.mockResolvedValue([]);
  });

  afterEach(() => {
    expect(fetchMock).not.toHaveBeenCalled();
    expect(rawRequest).not.toHaveBeenCalled();
    cleanup();
    vi.unstubAllGlobals();
  });

  it('shows the empty hint and does not load codes before a group is chosen', async () => {
    renderPage();
    expect(await screen.findByText('왼쪽에서 코드 그룹을 선택하세요.')).toBeTruthy();
    expect(listCommonCodes).not.toHaveBeenCalled();
  });

  it('searches groups with page, size, keyword, solution, and use flag', async () => {
    renderPage();
    await screen.findByText('상태');
    fireEvent.change(screen.getByPlaceholderText('검색'), { target: { value: '상태' } });
    fireEvent.change(screen.getByPlaceholderText('솔루션 코드'), { target: { value: 'MDM' } });
    fireEvent.mouseDown(screen.getByRole('combobox'));
    fireEvent.click(await screen.findByTitle('N'));
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() => {
      expect(listCodeGroups).toHaveBeenLastCalledWith(
        expect.objectContaining({
          page: 1,
          size: 20,
          keyword: '상태',
          solutionCode: 'MDM',
          useYn: 'N',
        }),
      );
    });
  });

  it('shows restore on a disabled group and hides disable', async () => {
    listCodeGroups.mockResolvedValue(pageOf([inactiveGroup]));
    renderPage();
    const row = (await screen.findByText('중지그룹')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    expect(within(row).getByRole('button', { name: '복구' })).toBeTruthy();
    expect(within(row).queryByRole('button', { name: '사용 중지' })).toBeNull();
  });

  it('puts the selected group in the query and loads that group on entry', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('상태'));
    await waitFor(() => {
      expect(searchBag.text).toContain('group=STATUS');
      expect(listCommonCodes).toHaveBeenCalledWith(
        expect.objectContaining({ groupCode: 'STATUS', page: 1, size: 20 }),
      );
    });
  });

  it('loads codes from an existing group query without another click', async () => {
    searchBag.initial = 'group=STATUS';
    renderPage();
    await waitFor(() => {
      expect(listCommonCodes).toHaveBeenCalledWith(
        expect.objectContaining({ groupCode: 'STATUS' }),
      );
    });
    expect(await screen.findAllByText('STATUS')).not.toHaveLength(0);
  });

  it('saves a group without empty languages and reloads the list', async () => {
    renderPage();
    await screen.findByText('상태');
    const dialog = await openGroupCreate();
    fireEvent.change(within(dialog).getByLabelText('그룹 코드'), { target: { value: 'CODE_NEW' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '새 그룹' } });
    fireEvent.click(within(dialog).getByText('언어별 이름'));
    fireEvent.change(within(dialog).getByLabelText('한국어 이름'), { target: { value: '한국어명' } });
    fireEvent.change(within(dialog).getByLabelText('한국어 설명'), { target: { value: '설명' } });
    const calls = listCodeGroups.mock.calls.length;
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => {
      expect(createCodeGroup).toHaveBeenCalledWith(
        expect.objectContaining({
          codeGroupCode: 'CODE_NEW',
          codeGroupName: '새 그룹',
          hierarchy: false,
          translations: [{ supportLanguageCode: 'ko-KR', name: '한국어명', description: '설명' }],
        }),
      );
    });
    await waitFor(() => expect(listCodeGroups.mock.calls.length).toBeGreaterThan(calls));
  });

  it('keeps the group code read-only when editing', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText('그룹 코드')).toHaveProperty('disabled', true);
  });

  it('disables save while the request is in flight', async () => {
    let finish: (value: typeof activeGroup) => void = () => undefined;
    createCodeGroup.mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    renderPage();
    await screen.findByText('상태');
    const dialog = await openGroupCreate();
    fireEvent.change(within(dialog).getByLabelText('그룹 코드'), { target: { value: 'CODE_NEW' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '새 그룹' } });
    const saveButton = () => document.querySelector('.ant-drawer-extra button');
    fireEvent.click(saveButton() as HTMLElement);
    await waitFor(() => expect(saveButton()?.className).toContain('ant-btn-loading'));
    fireEvent.click(saveButton() as HTMLElement);
    expect(createCodeGroup).toHaveBeenCalledTimes(1);
    finish(activeGroup);
  });

  it('saves code fields and blocks an end date before the start date', async () => {
    searchBag.initial = 'group=STATUS';
    renderPage();
    await screen.findByText('코드 등록');
    fireEvent.click(screen.getByRole('button', { name: '코드 등록' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).queryByLabelText('상위 코드')).toBeNull();
    fireEvent.change(within(dialog).getByLabelText('코드 값'), { target: { value: 'ON' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '사용' } });
    fireEvent.change(within(dialog).getByLabelText('정렬 순서'), { target: { value: '3' } });
    for (const index of [1, 2, 3, 4, 5]) {
      fireEvent.change(within(dialog).getByLabelText(`확장 속성 ${index}`), {
        target: { value: `v${index}` },
      });
    }
    fireEvent.change(within(dialog).getByLabelText('변경 사유'), { target: { value: '최초' } });
    fireEvent.click(within(dialog).getByText('언어별 이름'));
    fireEvent.change(within(dialog).getByLabelText('한국어 이름'), { target: { value: '사용' } });
    const [fromInput, toInput] = within(dialog).getAllByPlaceholderText(
      'Select date',
    ) as HTMLInputElement[];
    const pick = async (input: HTMLInputElement, fromEnd: boolean) => {
      fireEvent.mouseDown(input);
      fireEvent.click(input);
      const cell = await waitFor(() => {
        const panels = [
          ...document.querySelectorAll('.ant-picker-dropdown:not(.ant-picker-dropdown-hidden)'),
        ];
        const open = panels.at(-1);
        const cells = [
          ...(open?.querySelectorAll<HTMLElement>('td.ant-picker-cell-in-view') ?? []),
        ];
        const found = fromEnd ? cells.at(-1) : cells[0];
        if (!found?.getAttribute('title')) {
          throw new Error('calendar closed');
        }
        return found;
      });
      const title = cell.getAttribute('title') ?? '';
      fireEvent.mouseDown(cell);
      fireEvent.click(cell);
      await waitFor(() => expect(input.value).toBe(title));
      return title;
    };
    const late = await pick(fromInput, true);
    const early = await pick(toInput, false);
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('시작일은 종료일보다 늦을 수 없습니다.')).toBeTruthy();
    expect(createCommonCode).not.toHaveBeenCalled();
    const to = await pick(toInput, true);
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => {
      expect(createCommonCode).toHaveBeenCalledWith(
        expect.objectContaining({
          groupCode: 'STATUS',
          codeValue: 'ON',
          codeName: '사용',
          sortOrder: 3,
          extAttr1: 'v1',
          extAttr2: 'v2',
          extAttr3: 'v3',
          extAttr4: 'v4',
          extAttr5: 'v5',
          validFromDate: late,
          validToDate: to,
          changeReason: '최초',
          translations: [
            { supportLanguageCode: 'ko-KR', name: '사용', description: null },
          ],
        }),
      );
    });
    expect(late > early).toBe(true);
  });

  it('does not save a code value longer than the group limit', async () => {
    searchBag.initial = 'group=STATUS';
    renderPage();
    await screen.findByText('코드 등록');
    fireEvent.click(screen.getByRole('button', { name: '코드 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('코드 값'), { target: { value: 'TOOLONG' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '김' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('코드 값이 허용 길이보다 깁니다.')).toBeTruthy();
    expect(createCommonCode).not.toHaveBeenCalled();
  });

  it('shows a hierarchy as a tree and omits self and descendants from parents', async () => {
    const rows = [
      code({ codePk: 1, codeValue: 'ROOT', codeName: '뿌리' }),
      code({ codePk: 2, codeValue: 'CHILD', codeName: '자식', parentCodeValue: 'ROOT' }),
      code({ codePk: 3, codeValue: 'ORPHAN', codeName: '고아', parentCodeValue: 'GONE' }),
    ];
    searchBag.initial = 'group=TREE';
    getCodeGroup.mockResolvedValue(hierarchyGroup);
    listCommonCodes.mockResolvedValue(pageOf(rows));
    listGroupCodes.mockResolvedValue([
      ...rows,
      code({ codePk: 4, codeValue: 'GRAND', codeName: '손자', parentCodeValue: 'CHILD' }),
    ]);
    renderPage();
    expect(await screen.findByText('ROOT')).toBeTruthy();
    const orphan = screen.getByText('ORPHAN').closest('tr');
    const root = screen.getByText('ROOT').closest('tr');
    expect(orphan?.className).toContain('level-0');
    expect(root?.className).toContain('level-0');
    const child = screen.getByText('CHILD').closest('tr');
    if (!child) {
      throw new Error('child row missing');
    }
    fireEvent.click(within(child).getByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.mouseDown(within(dialog).getByRole('combobox'));
    expect(await screen.findByRole('option', { name: /ROOT/ })).toBeTruthy();
    expect(screen.queryByRole('option', { name: /CHILD/ })).toBeNull();
    expect(screen.queryByRole('option', { name: /GRAND/ })).toBeNull();
  });

  it('explains logical delete and skips the call on cancel', async () => {
    renderPage();
    const row = (await screen.findByText('상태')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    expect(await screen.findByText(/use_yn=N/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '취소' }));
    expect(deleteCodeGroup).not.toHaveBeenCalled();
  });

  it('reloads the list after a confirmed disable', async () => {
    renderPage();
    const row = (await screen.findByText('상태')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    const calls = listCodeGroups.mock.calls.length;
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    const hint = await screen.findByText(/use_yn=N/);
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '사용 중지' }));
    await waitFor(() => expect(deleteCodeGroup).toHaveBeenCalledWith('STATUS'));
    await waitFor(() => expect(listCodeGroups.mock.calls.length).toBeGreaterThan(calls));
  });

  it('shows the server conflict message', async () => {
    renderPage();
    deleteCodeGroup.mockRejectedValue(rejection('CODE.GROUP_IN_USE', '사용 중인 코드가 있습니다.'));
    const row = (await screen.findByText('상태')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    const hint = await screen.findByText(/use_yn=N/);
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '사용 중지' }));
    expect(await screen.findByText('사용 중인 코드가 있습니다.')).toBeTruthy();
  });

  it('uses the local conflict text when the server message is empty', async () => {
    renderPage();
    await screen.findByRole('button', { name: '사용 중지' });
    deleteCodeGroup.mockRejectedValue(rejection('CODE.GROUP_IN_USE', '   '));
    const row = screen.getByText('상태').closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    const hint = await screen.findByText(/use_yn=N/);
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '사용 중지' }));
    expect(await screen.findByText(/사용 중인 코드가 있어/)).toBeTruthy();
  });

  it('reloads after restore and keeps the selected group', async () => {
    searchBag.initial = 'group=OLD';
    getCodeGroup.mockResolvedValue(inactiveGroup);
    listCodeGroups.mockResolvedValue(pageOf([activeGroup, inactiveGroup]));
    renderPage();
    const row = (await screen.findAllByText('중지그룹'))[0].closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    const calls = listCodeGroups.mock.calls.length;
    fireEvent.click(within(row).getByRole('button', { name: '복구' }));
    await waitFor(() => expect(restoreCodeGroup).toHaveBeenCalledWith('OLD'));
    await waitFor(() => expect(listCodeGroups.mock.calls.length).toBeGreaterThan(calls));
    expect(searchBag.text).toContain('group=OLD');
    expect(screen.getAllByText('중지그룹').length).toBeGreaterThan(0);
  });

  it('pages history and shows the change, reason, user, and value diff', async () => {
    searchBag.initial = 'group=STATUS';
    listCommonCodes.mockResolvedValue(
      pageOf([code({ codePk: 11, codeValue: 'ON', codeName: '사용', groupCode: 'STATUS' })]),
    );
    listCodeHistory.mockImplementation((_pk: number, query: { page?: number }) =>
      Promise.resolve({
        items: [{ ...historyRow, changeDatetime: query.page === 2 ? '2026-04-02T00:00:00' : fixedAt }],
        total: 40,
        page: query.page ?? 1,
        size: 20,
      }),
    );
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: '변경 이력' }));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByText(fixedAt)).toBeTruthy();
    expect(within(dialog).getByText('수정')).toBeTruthy();
    expect(within(dialog).getByText('이름 수정')).toBeTruthy();
    expect(within(dialog).getByText('7')).toBeTruthy();
    await waitFor(() => {
      expect(listCodeHistory).toHaveBeenCalledWith(11, expect.objectContaining({ page: 1, size: 20 }));
    });
    fireEvent.click(within(dialog).getByTitle('2'));
    await waitFor(() => {
      expect(listCodeHistory).toHaveBeenCalledWith(11, expect.objectContaining({ page: 2, size: 20 }));
    });
    const expand = dialog.querySelector('.ant-table-row-expand-icon');
    if (!expand) {
      throw new Error('expand missing');
    }
    fireEvent.click(expand);
    expect(await screen.findByText('이전')).toBeTruthy();
    expect(screen.getByText('이후')).toBeTruthy();
  });

  it('shows a field error under the input', async () => {
    const error = rejection('COMMON.VALIDATION_FAILED', '입력값을 확인해 주세요.');
    error.fieldErrors = [{ field: 'codeGroupName', message: '이름이 너무 깁니다.' }];
    createCodeGroup.mockRejectedValue(error);
    renderPage();
    await screen.findByText('상태');
    const dialog = await openGroupCreate();
    fireEvent.change(within(dialog).getByLabelText('그룹 코드'), { target: { value: 'CODE_NEW' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '새 그룹' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await within(dialog).findByText('이름이 너무 깁니다.')).toBeTruthy();
  });

  it('shows a permission notice when the list is forbidden', async () => {
    listCodeGroups.mockRejectedValue(rejection('AUTH.FORBIDDEN', ''));
    renderPage();
    expect(await screen.findByText('이 작업을 할 권한이 없습니다.')).toBeTruthy();
    expect(screen.getByText('공통코드')).toBeTruthy();
  });

  it('keeps the selected group when a later list call fails', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('상태'));
    await screen.findAllByText('STATUS');
    listCodeGroups.mockRejectedValue(rejection('CODE.GROUP_NOT_FOUND', '목록을 다시 읽지 못했습니다.'));
    fireEvent.change(screen.getAllByPlaceholderText('검색')[0], { target: { value: 'x' } });
    fireEvent.click(screen.getAllByRole('button', { name: '조회' })[0]);
    expect(await screen.findByText('목록을 다시 읽지 못했습니다.')).toBeTruthy();
    expect(screen.getAllByText('STATUS').length).toBeGreaterThan(0);
  });
});
