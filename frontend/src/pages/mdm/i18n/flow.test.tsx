import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import I18nPage from './index';
import {
  languages,
  menuNamespace,
  oldNamespace,
  pageOf,
  saveMessage,
  saveMessageDetail,
} from './fixtures';

const listNamespaces = vi.hoisted(() => vi.fn());
const getNamespace = vi.hoisted(() => vi.fn());
const createNamespace = vi.hoisted(() => vi.fn());
const updateNamespace = vi.hoisted(() => vi.fn());
const deleteNamespace = vi.hoisted(() => vi.fn());
const restoreNamespace = vi.hoisted(() => vi.fn());
const listMessages = vi.hoisted(() => vi.fn());
const getMessage = vi.hoisted(() => vi.fn());
const createMessage = vi.hoisted(() => vi.fn());
const updateMessage = vi.hoisted(() => vi.fn());
const deleteMessage = vi.hoisted(() => vi.fn());
const restoreMessage = vi.hoisted(() => vi.fn());
const listCommonCodes = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const searchBag = vi.hoisted(() => ({ initial: '', text: '' }));
const fetchMock = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
}));

vi.mock('@ant-design/pro-components', () => {
  const ReactMock = require('react') as typeof import('react');
  return {
    PageContainer: ({ children, title }: { children?: React.ReactNode; title?: string }) =>
      ReactMock.createElement('div', null, title, children),
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

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: (...args: unknown[]) => fetchLanguages(...args),
}));

vi.mock('@/services/mdm/codes', () => ({
  listCommonCodes: (...args: unknown[]) => listCommonCodes(...args),
}));

vi.mock('@/services/mdm/i18nAdmin', () => ({
  listNamespaces: (...args: unknown[]) => listNamespaces(...args),
  getNamespace: (...args: unknown[]) => getNamespace(...args),
  createNamespace: (...args: unknown[]) => createNamespace(...args),
  updateNamespace: (...args: unknown[]) => updateNamespace(...args),
  deleteNamespace: (...args: unknown[]) => deleteNamespace(...args),
  restoreNamespace: (...args: unknown[]) => restoreNamespace(...args),
  listMessages: (...args: unknown[]) => listMessages(...args),
  getMessage: (...args: unknown[]) => getMessage(...args),
  createMessage: (...args: unknown[]) => createMessage(...args),
  updateMessage: (...args: unknown[]) => updateMessage(...args),
  deleteMessage: (...args: unknown[]) => deleteMessage(...args),
  restoreMessage: (...args: unknown[]) => restoreMessage(...args),
}));

function renderPage() {
  return render(
    <App>
      <I18nPage />
    </App>,
  );
}

function rejection(codeName: string, message = ''): ApiError {
  const error = new ApiError(message);
  error.errorCode = codeName;
  return error;
}

describe('i18n flows', () => {
  beforeEach(() => {
    searchBag.initial = '';
    searchBag.text = '';
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
    rawRequest.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    for (const fn of [
      listNamespaces,
      getNamespace,
      createNamespace,
      updateNamespace,
      deleteNamespace,
      restoreNamespace,
      listMessages,
      getMessage,
      createMessage,
      updateMessage,
      deleteMessage,
      restoreMessage,
      listCommonCodes,
      fetchLanguages,
    ]) {
      fn.mockReset();
    }
    fetchLanguages.mockResolvedValue(languages);
    listCommonCodes.mockImplementation((query: { groupCode?: string }) => {
      if (query.groupCode === 'I18N_MESSAGE_TYPE') {
        return Promise.resolve(pageOf([{ codeValue: 'LABEL', codeName: '라벨' }]));
      }
      if (query.groupCode === 'I18N_TRANSLATION_STATUS') {
        return Promise.resolve(pageOf([{ codeValue: 'DRAFT', codeName: '작성 중' }]));
      }
      return Promise.resolve(pageOf([]));
    });
    listNamespaces.mockResolvedValue(pageOf([menuNamespace]));
    getNamespace.mockResolvedValue(menuNamespace);
    listMessages.mockResolvedValue(pageOf([saveMessage]));
    getMessage.mockResolvedValue(saveMessageDetail);
    createNamespace.mockResolvedValue(menuNamespace);
    updateNamespace.mockResolvedValue(menuNamespace);
    deleteNamespace.mockResolvedValue(undefined);
    restoreNamespace.mockResolvedValue(oldNamespace);
    createMessage.mockResolvedValue(saveMessage);
    updateMessage.mockResolvedValue(saveMessageDetail);
    deleteMessage.mockResolvedValue(undefined);
    restoreMessage.mockResolvedValue(saveMessage);
  });

  afterEach(() => {
    expect(fetchMock).not.toHaveBeenCalled();
    expect(rawRequest).not.toHaveBeenCalled();
    cleanup();
    vi.unstubAllGlobals();
  });

  it('shows the empty hint and does not load messages before a namespace is chosen', async () => {
    renderPage();
    expect(await screen.findByText('왼쪽에서 네임스페이스를 선택하세요.')).toBeTruthy();
    expect(listMessages).not.toHaveBeenCalled();
  });

  it('searches namespaces with page, size, keyword, and use flag', async () => {
    renderPage();
    await screen.findByText('메뉴');
    fireEvent.change(screen.getByPlaceholderText('검색'), { target: { value: '메뉴' } });
    fireEvent.mouseDown(screen.getByRole('combobox'));
    fireEvent.click(await screen.findByTitle('N'));
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() => {
      expect(listNamespaces).toHaveBeenLastCalledWith(
        expect.objectContaining({
          page: 1,
          size: 20,
          keyword: '메뉴',
          useYn: 'N',
        }),
      );
    });
  });

  it('puts the selected namespace in the query and loads it on entry', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('메뉴'));
    await waitFor(() => expect(searchBag.text).toContain('ns=menu'));
    expect(listMessages).toHaveBeenCalledWith(
      expect.objectContaining({ namespaceCode: 'menu', page: 1, size: 20 }),
    );
  });

  it('loads messages from an existing namespace query without another click', async () => {
    searchBag.initial = 'ns=menu';
    renderPage();
    expect(await screen.findByText('SAVE')).toBeTruthy();
    expect(listMessages).toHaveBeenCalledWith(
      expect.objectContaining({ namespaceCode: 'menu' }),
    );
    expect(screen.getByText('—')).toBeTruthy();
  });

  it('saves a namespace and reloads the list', async () => {
    renderPage();
    await screen.findByText('메뉴');
    const calls = listNamespaces.mock.calls.length;
    fireEvent.click(screen.getByRole('button', { name: '네임스페이스 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('네임스페이스 코드'), { target: { value: 'menuNew' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '새 메뉴' } });
    fireEvent.change(within(dialog).getByLabelText('솔루션 코드'), { target: { value: 'MDM' } });
    fireEvent.change(within(dialog).getByLabelText('설명'), { target: { value: '설명' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => {
      expect(createNamespace).toHaveBeenCalledWith({
        namespaceCode: 'menuNew',
        namespaceName: '새 메뉴',
        solutionCode: 'MDM',
        description: '설명',
      });
    });
    await waitFor(() => expect(listNamespaces.mock.calls.length).toBeGreaterThan(calls));
  });

  it('keeps the namespace code read-only when editing', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText('네임스페이스 코드')).toHaveProperty('disabled', true);
  });

  it('disables save while the request is in flight', async () => {
    let finish: (value: typeof menuNamespace) => void = () => undefined;
    createNamespace.mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    renderPage();
    await screen.findByText('메뉴');
    fireEvent.click(screen.getByRole('button', { name: '네임스페이스 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('네임스페이스 코드'), { target: { value: 'menuNew' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '새 메뉴' } });
    const saveButton = () => document.querySelector('.ant-drawer-extra button');
    fireEvent.click(saveButton() as HTMLElement);
    await waitFor(() => expect(saveButton()?.className).toContain('ant-btn-loading'));
    fireEvent.click(saveButton() as HTMLElement);
    expect(createNamespace).toHaveBeenCalledTimes(1);
    finish(menuNamespace);
  });

  it('explains that a disabled namespace leaves messages out of bundles and skips cancel', async () => {
    renderPage();
    const row = (await screen.findByText('메뉴')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    expect(await screen.findByText(/번들 조회에서는 빠집니다/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '취소' }));
    expect(deleteNamespace).not.toHaveBeenCalled();
  });

  it('reloads after a confirmed namespace disable', async () => {
    renderPage();
    const row = (await screen.findByText('메뉴')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    const calls = listNamespaces.mock.calls.length;
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    const hint = await screen.findByText(/번들 조회에서는 빠집니다/);
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '사용 중지' }));
    await waitFor(() => expect(deleteNamespace).toHaveBeenCalledWith('menu'));
    await waitFor(() => expect(listNamespaces.mock.calls.length).toBeGreaterThan(calls));
  });

  it('reloads after restore and keeps the selected namespace', async () => {
    searchBag.initial = 'ns=old';
    listNamespaces.mockResolvedValue(pageOf([oldNamespace]));
    getNamespace.mockResolvedValue(oldNamespace);
    renderPage();
    const row = (await screen.findAllByText('이전'))[0].closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    const calls = listNamespaces.mock.calls.length;
    fireEvent.click(within(row).getByRole('button', { name: '복구' }));
    await waitFor(() => expect(restoreNamespace).toHaveBeenCalledWith('old'));
    await waitFor(() => expect(listNamespaces.mock.calls.length).toBeGreaterThan(calls));
    expect(searchBag.text).toContain('ns=old');
  });

  it('saves a message without empty languages', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('메뉴'));
    await screen.findByText('메시지 등록');
    fireEvent.click(screen.getByRole('button', { name: '메시지 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('메시지 키'), { target: { value: 'SAVE' } });
    fireEvent.mouseDown(within(dialog).getByRole('combobox'));
    fireEvent.click(await screen.findByTitle('라벨'));
    fireEvent.change(within(dialog).getByLabelText('기본 문구'), { target: { value: '저장' } });
    fireEvent.change(within(dialog).getByLabelText('파라미터 설명'), { target: { value: '없음' } });
    fireEvent.click(within(dialog).getByText('언어별 번역'));
    const texts = within(dialog).getAllByLabelText('번역 문구');
    fireEvent.change(texts[0], { target: { value: '저장' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => {
      expect(createMessage).toHaveBeenCalledWith({
        namespaceCode: 'menu',
        messageKey: 'SAVE',
        messageTypeCode: 'LABEL',
        defaultMessage: '저장',
        paramDescription: '없음',
        translations: [
          {
            supportLanguageCode: 'ko-KR',
            messageText: '저장',
            translationStatusCode: null,
            machineTranslated: null,
          },
        ],
      });
    });
  });

  it('keeps the message key read-only and shows the version when editing', async () => {
    searchBag.initial = 'ns=menu';
    renderPage();
    const row = (await screen.findByText('SAVE')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    fireEvent.click(within(row).getByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText('메시지 키')).toHaveProperty('disabled', true);
    fireEvent.click(within(dialog).getByText('언어별 번역'));
    expect(await within(dialog).findByDisplayValue('2')).toBeTruthy();
  });

  it('explains logical delete and skips the call on cancel', async () => {
    searchBag.initial = 'ns=menu';
    renderPage();
    const row = (await screen.findByText('SAVE')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    expect(await screen.findByText(/use_yn=N/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: '취소' }));
    expect(deleteMessage).not.toHaveBeenCalled();
  });

  it('reloads after a confirmed message disable and keeps the namespace', async () => {
    searchBag.initial = 'ns=menu';
    renderPage();
    const row = (await screen.findByText('SAVE')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    const calls = listMessages.mock.calls.length;
    fireEvent.click(within(row).getByRole('button', { name: '사용 중지' }));
    const hint = await screen.findByText(/use_yn=N/);
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '사용 중지' }));
    await waitFor(() => expect(deleteMessage).toHaveBeenCalledWith(5));
    await waitFor(() => expect(listMessages.mock.calls.length).toBeGreaterThan(calls));
    expect(searchBag.text).toContain('ns=menu');
  });

  it('reloads after restoring a message', async () => {
    searchBag.initial = 'ns=menu';
    listMessages.mockResolvedValue(pageOf([{ ...saveMessage, useYn: 'N' }]));
    renderPage();
    const row = (await screen.findByText('SAVE')).closest('tr');
    if (!row) {
      throw new Error('row missing');
    }
    const calls = listMessages.mock.calls.length;
    fireEvent.click(within(row).getByRole('button', { name: '복구' }));
    await waitFor(() => expect(restoreMessage).toHaveBeenCalledWith(5));
    await waitFor(() => expect(listMessages.mock.calls.length).toBeGreaterThan(calls));
  });

  it('shows a field error under the input', async () => {
    createNamespace.mockRejectedValue(
      Object.assign(rejection('COMMON.VALIDATION_FAILED', '입력값을 확인해 주세요.'), {
        fieldErrors: [{ field: 'namespaceName', message: '이름이 너무 깁니다.' }],
      }),
    );
    renderPage();
    await screen.findByText('메뉴');
    fireEvent.click(screen.getByRole('button', { name: '네임스페이스 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('네임스페이스 코드'), { target: { value: 'menuNew' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '새 메뉴' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('이름이 너무 깁니다.')).toBeTruthy();
  });

  it('shows the server conflict message', async () => {
    createNamespace.mockRejectedValue(rejection('I18N.DUPLICATE', '이미 있는 네임스페이스입니다.'));
    renderPage();
    await screen.findByText('메뉴');
    fireEvent.click(screen.getByRole('button', { name: '네임스페이스 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('네임스페이스 코드'), { target: { value: 'menu' } });
    fireEvent.change(within(dialog).getByLabelText('이름'), { target: { value: '메뉴' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await within(dialog).findByText('이미 있는 네임스페이스입니다.')).toBeTruthy();
  });

  it('uses the local conflict text when the server message is empty', async () => {
    createMessage.mockRejectedValue(rejection('I18N.DUPLICATE', '   '));
    renderPage();
    fireEvent.click(await screen.findByText('메뉴'));
    await screen.findByText('메시지 등록');
    fireEvent.click(screen.getByRole('button', { name: '메시지 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('메시지 키'), { target: { value: 'SAVE' } });
    fireEvent.mouseDown(within(dialog).getByRole('combobox'));
    fireEvent.click(await screen.findByTitle('라벨'));
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await within(dialog).findByText('이미 사용 중인 코드 또는 키입니다.')).toBeTruthy();
  });

  it('shows a permission notice when the list is forbidden', async () => {
    listNamespaces.mockRejectedValue(rejection('AUTH.FORBIDDEN', '숨김'));
    renderPage();
    expect(await screen.findByText('이 작업을 할 권한이 없습니다.')).toBeTruthy();
    expect(screen.getByText('다국어')).toBeTruthy();
  });

  it('shows missing language tags from the list summary and does not load each message', async () => {
    listMessages.mockResolvedValue(
      pageOf([
        {
          ...saveMessage,
          translatedLanguageCodes: ['ko-KR'],
          missingLanguageCodes: ['en-US'],
          translationStatusCodes: ['DRAFT'],
        },
      ]),
    );
    renderPage();
    fireEvent.click(await screen.findByText('메뉴'));
    const missing = await screen.findByText('en-US');
    expect(missing.closest('.ant-tag')?.className).toContain('ant-tag-warning');
    expect(screen.getByText('ko-KR').closest('.ant-tag')?.className).not.toContain('ant-tag-warning');
    expect(screen.getByText('DRAFT')).toBeTruthy();
    expect(getMessage).not.toHaveBeenCalled();
  });

  it('shows a dash when the list has no translation summary', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('메뉴'));
    expect(await screen.findByText('—')).toBeTruthy();
    expect(getMessage).not.toHaveBeenCalled();
  });

  it('keeps the selected namespace when a later list call fails', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('메뉴'));
    await screen.findByText('메뉴 문구');
    listNamespaces.mockRejectedValue(rejection('COMMON.VALIDATION_FAILED', '목록을 불러오지 못했습니다.'));
    fireEvent.click(screen.getAllByRole('button', { name: '조회' })[0]);
    expect(await screen.findByText('목록을 불러오지 못했습니다.')).toBeTruthy();
    expect(screen.getByText('메뉴 문구')).toBeTruthy();
  });
});
