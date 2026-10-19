import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import LanguagePage from './index';
import type { LanguageRecord, NationRecord } from '@/services/mdm/languageRecord';

const listSupportLanguages = vi.hoisted(() => vi.fn());
const createSupportLanguage = vi.hoisted(() => vi.fn());
const updateSupportLanguage = vi.hoisted(() => vi.fn());
const deleteSupportLanguage = vi.hoisted(() => vi.fn());
const restoreSupportLanguage = vi.hoisted(() => vi.fn());
const listNations = vi.hoisted(() => vi.fn());
const createNation = vi.hoisted(() => vi.fn());
const updateNation = vi.hoisted(() => vi.fn());
const deleteNation = vi.hoisted(() => vi.fn());
const restoreNation = vi.hoisted(() => vi.fn());
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

vi.mock('@/services/mdm/languageAdmin', () => ({
  listSupportLanguages: (...args: unknown[]) => listSupportLanguages(...args),
  createSupportLanguage: (...args: unknown[]) => createSupportLanguage(...args),
  updateSupportLanguage: (...args: unknown[]) => updateSupportLanguage(...args),
  deleteSupportLanguage: (...args: unknown[]) => deleteSupportLanguage(...args),
  restoreSupportLanguage: (...args: unknown[]) => restoreSupportLanguage(...args),
  listNations: (...args: unknown[]) => listNations(...args),
  createNation: (...args: unknown[]) => createNation(...args),
  updateNation: (...args: unknown[]) => updateNation(...args),
  deleteNation: (...args: unknown[]) => deleteNation(...args),
  restoreNation: (...args: unknown[]) => restoreNation(...args),
}));

const korean: LanguageRecord = {
  supportLanguageCode: 'ko-KR',
  languageName: '한국어',
  nativeName: '한국어',
  isoLanguageCode: 'ko',
  fallbackLanguageCode: null,
  textDirectionCode: 'LTR',
  dateFormat: null,
  datetimeFormat: null,
  numberFormat: null,
  defaultLanguage: true,
  sortOrder: 1,
  useYn: 'Y',
};

const english: LanguageRecord = {
  ...korean,
  supportLanguageCode: 'en-US',
  languageName: '영어',
  nativeName: 'English',
  isoLanguageCode: 'en',
  fallbackLanguageCode: 'ko-KR',
  defaultLanguage: false,
  sortOrder: 2,
};

const french: LanguageRecord = {
  ...english,
  supportLanguageCode: 'fr-FR',
  languageName: '프랑스어',
  nativeName: 'français',
  fallbackLanguageCode: 'en-US',
  defaultLanguage: false,
  useYn: 'N',
  sortOrder: 9,
};

const korea: NationRecord = {
  nationPk: 1,
  nationCode: 'KR',
  isoAlpha3Code: 'KOR',
  isoNumericCode: '410',
  nationName: '한국',
  defaultLanguageCode: 'ko-KR',
  phoneCode: '82',
  currencyCode: 'KRW',
  timezoneId: 'Asia/Seoul',
  sortOrder: 1,
  useYn: 'Y',
};

function pageOf<T>(items: T[], total = items.length) {
  return { items, total, page: 1, size: 20 };
}

function rejection(codeName: string, message = ''): ApiError {
  const error = new ApiError(message);
  error.errorCode = codeName;
  return error;
}

function renderPage() {
  return render(
    <App>
      <LanguagePage />
    </App>,
  );
}

async function confirmOk() {
  const dialog = document.body.querySelector('.ant-modal');
  expect(dialog).toBeTruthy();
  fireEvent.click(within(dialog as HTMLElement).getByRole('button', { name: '확인' }));
}

describe('language and nation flows', () => {
  beforeEach(() => {
    searchBag.initial = '';
    searchBag.text = '';
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
    rawRequest.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    for (const fn of [
      listSupportLanguages,
      createSupportLanguage,
      updateSupportLanguage,
      deleteSupportLanguage,
      restoreSupportLanguage,
      listNations,
      createNation,
      updateNation,
      deleteNation,
      restoreNation,
    ]) {
      fn.mockReset();
    }
    listSupportLanguages.mockResolvedValue([korean, english, french]);
    listNations.mockResolvedValue(pageOf([korea], 40));
    createSupportLanguage.mockResolvedValue(english);
    updateSupportLanguage.mockResolvedValue(english);
    deleteSupportLanguage.mockResolvedValue(undefined);
    restoreSupportLanguage.mockResolvedValue(french);
    createNation.mockResolvedValue(korea);
    updateNation.mockResolvedValue(korea);
    deleteNation.mockResolvedValue(undefined);
    restoreNation.mockResolvedValue({ ...korea, useYn: 'Y' });
  });

  afterEach(() => {
    cleanup();
    expect(fetchMock).not.toHaveBeenCalled();
    expect(rawRequest).not.toHaveBeenCalled();
  });

  it('loads languages without a use flag and shows restore on an inactive row', async () => {
    renderPage();
    await screen.findByText('프랑스어');
    expect(listSupportLanguages.mock.calls.every((call) => call[0] === undefined || call.length === 0)).toBe(true);
    const row = screen.getByText('프랑스어').closest('tr');
    expect(row?.className).toContain('inactive');
    expect(within(row as HTMLElement).getByRole('button', { name: '복구' })).toBeTruthy();
    expect(within(row as HTMLElement).queryByRole('button', { name: '사용 중지' })).toBeNull();
  });

  it('creates a language', async () => {
    renderPage();
    await screen.findAllByText('한국어');
    fireEvent.click(screen.getByRole('button', { name: '언어 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('언어 코드'), { target: { value: 'ja-JP' } });
    fireEvent.change(within(dialog).getByLabelText('언어명'), { target: { value: '일본어' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(createSupportLanguage).toHaveBeenCalledTimes(1));
    expect(createSupportLanguage.mock.calls[0][0].supportLanguageCode).toBe('ja-JP');
  });

  it('keeps the language code read-only when editing', async () => {
    renderPage();
    const row = (await screen.findByText('영어')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText('언어 코드')).toHaveProperty('disabled', true);
  });

  it('asks before replacing the default language', async () => {
    renderPage();
    const row = (await screen.findByText('영어')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    const combos = within(dialog).getAllByRole('combobox');
    fireEvent.mouseDown(combos[2]);
    fireEvent.click(await screen.findByTitle('예'));
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('기존 기본 언어가 해제된다')).toBeTruthy();
    await confirmOk();
    await waitFor(() => expect(updateSupportLanguage).toHaveBeenCalledTimes(1));
  });

  it('hides disable on the default language and omits that language from fallback choices', async () => {
    renderPage();
    const row = (await screen.findAllByText('한국어'))[0].closest('tr');
    expect(within(row as HTMLElement).queryByRole('button', { name: '사용 중지' })).toBeNull();
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    expect(screen.queryByTitle('한국어')).toBeNull();
    expect(await screen.findByTitle('영어')).toBeTruthy();
  });

  it('explains disable and skips the call on cancel', async () => {
    renderPage();
    const row = (await screen.findByText('영어')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '사용 중지' }));
    expect(await screen.findByText('중지하면 번들 조회·언어 선택에서 빠진다')).toBeTruthy();
    fireEvent.click(within(document.body.querySelector('.ant-modal') as HTMLElement).getByRole('button', { name: '취소' }));
    expect(deleteSupportLanguage).not.toHaveBeenCalled();
  });

  it('shows the server conflict message', async () => {
    deleteSupportLanguage.mockRejectedValue(rejection('I18N.LANGUAGE_IN_USE', '대체 언어로 쓰입니다.'));
    renderPage();
    const row = (await screen.findByText('영어')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '사용 중지' }));
    await confirmOk();
    expect(await screen.findByText('대체 언어로 쓰입니다.')).toBeTruthy();
  });

  it('uses the local conflict text when the server message is empty', async () => {
    deleteSupportLanguage.mockRejectedValue(rejection('I18N.LANGUAGE_IN_USE', ' '));
    renderPage();
    const row = (await screen.findByText('영어')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '사용 중지' }));
    await confirmOk();
    expect(await screen.findByText('기본 언어이거나 다른 언어의 대체 언어라 사용 중지할 수 없습니다.')).toBeTruthy();
  });

  it('shows a fallback cycle message', async () => {
    updateSupportLanguage.mockRejectedValue(rejection('I18N.FALLBACK_CYCLE', '순환입니다.'));
    renderPage();
    const row = (await screen.findByText('영어')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '수정' }));
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('순환입니다.')).toBeTruthy();
  });

  it('sends one create while the request is in flight', async () => {
    let release: (value: LanguageRecord) => void = () => undefined;
    createSupportLanguage.mockImplementation(
      () =>
        new Promise((resolve) => {
          release = resolve;
        }),
    );
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: '언어 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('언어 코드'), { target: { value: 'ja-JP' } });
    fireEvent.change(within(dialog).getByLabelText('언어명'), { target: { value: '일본어' } });
    const save = within(dialog).getByRole('button', { name: '저장' });
    fireEvent.click(save);
    await waitFor(() => expect(save.className).toContain('ant-btn-loading'));
    fireEvent.click(save);
    expect(createSupportLanguage).toHaveBeenCalledTimes(1);
    release(english);
  });

  it('reloads languages after save and does not call a cache invalidator', async () => {
    renderPage();
    await screen.findAllByText('한국어');
    const before = listSupportLanguages.mock.calls.length;
    fireEvent.click(screen.getByRole('button', { name: '언어 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('언어 코드'), { target: { value: 'ja-JP' } });
    fireEvent.change(within(dialog).getByLabelText('언어명'), { target: { value: '일본어' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(listSupportLanguages.mock.calls.length).toBeGreaterThan(before));
    expect(rawRequest).not.toHaveBeenCalled();
  });

  it('puts the nation tab in the query and opens it from the query', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('국가'));
    expect(searchBag.text).toContain('tab=nation');
    cleanup();
    searchBag.initial = 'tab=nation';
    renderPage();
    expect(await screen.findByText('한국')).toBeTruthy();
    expect(listNations).toHaveBeenCalledWith({ page: 1, size: 20, sort: [] });
  });

  it('pages and sorts nations on the server', async () => {
    searchBag.initial = 'tab=nation';
    renderPage();
    await screen.findByText('한국');
    fireEvent.click(screen.getByText('국가 코드'));
    await waitFor(() =>
      expect(listNations).toHaveBeenCalledWith({ page: 1, size: 20, sort: ['nationCode,asc'] }),
    );
    fireEvent.click(screen.getByRole('listitem', { name: '2' }) ?? screen.getByTitle('2'));
    await waitFor(() => expect(listNations.mock.calls.some((call) => call[0].page === 2)).toBe(true));
  });

  it('uppercases a nation code and blocks an invalid code', async () => {
    searchBag.initial = 'tab=nation';
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: '국가 등록' }));
    const dialog = await screen.findByRole('dialog');
    const code = within(dialog).getByLabelText('국가 코드');
    fireEvent.change(code, { target: { value: 'kr' } });
    expect((code as HTMLInputElement).value).toBe('KR');
    fireEvent.change(code, { target: { value: 'K' } });
    fireEvent.change(within(dialog).getByLabelText('국가명'), { target: { value: '한국' } });
    fireEvent.mouseDown(within(dialog).getByRole('combobox'));
    const named = await screen.findAllByTitle('한국어');
    const option = named.find((node) => node.className.includes('ant-select-item-option')) ?? named[named.length - 1];
    fireEvent.click(option);
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await within(dialog).findByText('국가 코드는 대문자 2자입니다.')).toBeTruthy();
    expect(createNation).not.toHaveBeenCalled();
  });

  it('keeps the nation code read-only and offers only active languages', async () => {
    searchBag.initial = 'tab=nation';
    renderPage();
    const row = (await screen.findByText('한국')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '수정' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText('국가 코드')).toHaveProperty('disabled', true);
    fireEvent.mouseDown(within(dialog).getByRole('combobox'));
    const titles = await screen.findAllByTitle('한국어');
    expect(titles.some((node) => node.className.includes('ant-select-item-option'))).toBe(true);
    expect(screen.queryByTitle('프랑스어')).toBeNull();
  });

  it('reloads after a confirmed nation disable and after restore', async () => {
    listNations.mockResolvedValue(pageOf([{ ...korea, useYn: 'N' }]));
    searchBag.initial = 'tab=nation';
    renderPage();
    const row = (await screen.findByText('한국')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '복구' }));
    await waitFor(() => expect(restoreNation).toHaveBeenCalledWith('KR'));
  });

  it('explains logical delete and reloads after disable', async () => {
    searchBag.initial = 'tab=nation';
    renderPage();
    const row = (await screen.findByText('한국')).closest('tr');
    fireEvent.click(within(row as HTMLElement).getByRole('button', { name: '사용 중지' }));
    expect(await screen.findByText('삭제하면 사용 중지(use_yn=N)됩니다. 데이터는 지워지지 않습니다.')).toBeTruthy();
    const before = listNations.mock.calls.length;
    await confirmOk();
    await waitFor(() => expect(listNations.mock.calls.length).toBeGreaterThan(before));
  });

  it('shows the server duplicate message', async () => {
    createNation.mockRejectedValue(rejection('I18N.DUPLICATE', '이미 있는 국가입니다.'));
    searchBag.initial = 'tab=nation';
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: '국가 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('국가 코드'), { target: { value: 'KR' } });
    fireEvent.change(within(dialog).getByLabelText('국가명'), { target: { value: '한국' } });
    fireEvent.mouseDown(within(dialog).getByRole('combobox'));
    const named = await screen.findAllByTitle('한국어');
    const option = named.find((node) => node.className.includes('ant-select-item-option')) ?? named[named.length - 1];
    fireEvent.click(option);
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('이미 있는 국가입니다.')).toBeTruthy();
  });

  it('shows a field error under the input', async () => {
    const error = rejection('COMMON.VALIDATION_FAILED', '입력값을 확인해 주세요.');
    error.fieldErrors = [{ field: 'nationName', message: '너무 깁니다.' }];
    createNation.mockRejectedValue(error);
    searchBag.initial = 'tab=nation';
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: '국가 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('국가 코드'), { target: { value: 'JP' } });
    fireEvent.change(within(dialog).getByLabelText('국가명'), { target: { value: '일본' } });
    fireEvent.mouseDown(within(dialog).getByRole('combobox'));
    const named = await screen.findAllByTitle('한국어');
    const option = named.find((node) => node.className.includes('ant-select-item-option')) ?? named[named.length - 1];
    fireEvent.click(option);
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await within(dialog).findByText('너무 깁니다.')).toBeTruthy();
  });

  it('shows a permission notice when the nation list is forbidden', async () => {
    listNations.mockRejectedValue(rejection('AUTH.FORBIDDEN', '숨김'));
    searchBag.initial = 'tab=nation';
    renderPage();
    expect(await screen.findByText('이 작업을 할 권한이 없습니다.')).toBeTruthy();
    expect(searchBag.text).toContain('tab=nation');
  });

  it('keeps the tab when a later nation list fails', async () => {
    searchBag.initial = 'tab=nation';
    renderPage();
    await screen.findByText('한국');
    listNations.mockRejectedValue(rejection('COMMON.VALIDATION_FAILED', '목록을 불러오지 못했습니다.'));
    fireEvent.click(screen.getByText('국가 코드'));
    expect(await screen.findByText('목록을 불러오지 못했습니다.')).toBeTruthy();
    expect(screen.getByText('한국')).toBeTruthy();
    expect(searchBag.text).toContain('tab=nation');
  });
});
