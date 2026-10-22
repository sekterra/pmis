import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { App } from 'antd';
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setAccessContext } from '@/core/auth/context';
import { areaForbidden } from '@/core/auth/context';
import { guardView } from '@/core/menu/guard';
import type { MyMenu } from '@/core/menu/types';
import { ApiError } from '@/core/request/errors';
import { resetCodeOptionsCache } from '@/core/codes/useCodeOptions';
import type { Company } from '@/services/mdm/companyRecord';
import CompanyPage from './index';

const listCompanies = vi.hoisted(() => vi.fn());
const getCompany = vi.hoisted(() => vi.fn());
const createCompany = vi.hoisted(() => vi.fn());
const updateCompany = vi.hoisted(() => vi.fn());
const deleteCompany = vi.hoisted(() => vi.fn());
const restoreCompany = vi.hoisted(() => vi.fn());
const fetchLanguages = vi.hoisted(() => vi.fn());
const listNations = vi.hoisted(() => vi.fn());
const listGroupCodes = vi.hoisted(() => vi.fn());
const rawRequest = vi.hoisted(() => vi.fn());
const fetchMock = vi.hoisted(() => vi.fn());
const accessBag = vi.hoisted(() => ({
  can: (_menu: string, _action: string): boolean => true,
}));

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

vi.mock('@umijs/max', () => ({
  useIntl: () => ({
    formatMessage: ({ defaultMessage, id }: { defaultMessage?: string; id: string }) => defaultMessage ?? id,
  }),
  useAccess: () => ({ can: (menu: string, action: string) => accessBag.can(menu, action) }),
}));

vi.mock('@/services/mdm/company', () => ({
  listCompanies: (...args: unknown[]) => listCompanies(...args),
  getCompany: (...args: unknown[]) => getCompany(...args),
  createCompany: (...args: unknown[]) => createCompany(...args),
  updateCompany: (...args: unknown[]) => updateCompany(...args),
  deleteCompany: (...args: unknown[]) => deleteCompany(...args),
  restoreCompany: (...args: unknown[]) => restoreCompany(...args),
}));

vi.mock('@/services/mdm/i18n', () => ({
  fetchLanguages: (...args: unknown[]) => fetchLanguages(...args),
}));

vi.mock('@/services/mdm/languageAdmin', () => ({
  listNations: (...args: unknown[]) => listNations(...args),
}));

vi.mock('@/services/mdm/codes', () => ({
  listGroupCodes: (...args: unknown[]) => listGroupCodes(...args),
}));

function company(patch: Partial<Company> = {}): Company {
  return {
    companyPk: 1,
    parentCompanyPk: null,
    parentCompanyCode: null,
    companyCode: 'ACME',
    companyName: '에이씨엠이',
    companyShortName: 'ACME',
    companyTypeCode: 'OWN',
    businessRegistrationNo: '123-45-67890',
    corporateRegistrationNo: null,
    representativeName: null,
    nationPk: 1,
    nationCode: 'KR',
    postalCode: null,
    address: '서울',
    addressDetail: null,
    phoneNo: null,
    faxNo: null,
    email: null,
    websiteUrl: null,
    sortOrder: 1,
    description: null,
    useYn: 'Y',
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
    translations: [{ supportLanguageCode: 'ko-KR', text: '에이씨엠이' }],
    ...patch,
  };
}

function menu(actions: string[]): MyMenu {
  return {
    menuCode: 'COMPANY',
    parentMenuCode: 'ORG',
    menuName: '회사',
    menuTypeCode: 'PAGE',
    routePath: null,
    iconName: null,
    levelNo: 2,
    sortOrder: 1,
    actions,
  };
}

describe('company page', () => {
  beforeEach(() => {
    resetCodeOptionsCache();
    accessBag.can = () => true;
    rawRequest.mockReset();
    rawRequest.mockRejectedValue(new Error('network'));
    fetchMock.mockReset();
    fetchMock.mockRejectedValue(new Error('network'));
    vi.stubGlobal('fetch', fetchMock);
    listCompanies.mockReset();
    listCompanies.mockResolvedValue({
      items: [
        company(),
        company({ companyPk: 2, companyCode: 'BETA', companyName: '베타', useYn: 'N' }),
        company({ companyPk: 3, companyCode: 'GAMMA', companyName: '감마', useYn: 'Y' }),
      ],
      total: 3,
      page: 1,
      size: 20,
    });
    getCompany.mockReset();
    getCompany.mockResolvedValue(company());
    createCompany.mockReset();
    createCompany.mockResolvedValue(company());
    updateCompany.mockReset();
    updateCompany.mockResolvedValue(company());
    deleteCompany.mockReset();
    deleteCompany.mockResolvedValue(undefined);
    restoreCompany.mockReset();
    restoreCompany.mockResolvedValue(company({ useYn: 'Y' }));
    fetchLanguages.mockReset();
    fetchLanguages.mockResolvedValue([
      {
        supportLanguageCode: 'ko-KR',
        languageName: '한국어',
        nativeName: '한국어',
        defaultLanguage: true,
        fallbackLanguageCode: null,
        textDirectionCode: 'LTR',
        dateFormat: '',
        datetimeFormat: '',
        numberFormat: '',
      },
    ]);
    listNations.mockReset();
    listNations.mockResolvedValue({
      items: [
        { nationPk: 1, nationCode: 'KR', nationName: '한국', useYn: 'Y' },
        { nationPk: 2, nationCode: 'XX', nationName: '중지', useYn: 'N' },
      ],
      total: 2,
      page: 1,
      size: 200,
    });
    listGroupCodes.mockReset();
    listGroupCodes.mockResolvedValue([
      {
        codePk: 1,
        groupCode: 'COMPANY_TYPE',
        codeValue: 'OWN',
        codeName: '자사',
        parentCodeValue: null,
        levelNo: null,
        codePath: null,
        sortOrder: 1,
        extAttr1: null,
        extAttr2: null,
        extAttr3: null,
        extAttr4: null,
        extAttr5: null,
        validFromDate: null,
        validToDate: null,
        useYn: 'Y',
        translations: [{ supportLanguageCode: 'ko-KR', name: '자사', description: null }],
      },
    ]);
  });

  afterEach(() => {
    cleanup();
    expect(rawRequest).not.toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
  });

  it('searches, saves, edits a read-only code, deletes, and restores', async () => {
    render(
      <App>
        <CompanyPage />
      </App>,
    );
    await waitFor(() => expect(listCompanies).toHaveBeenCalled());
    expect(listCompanies.mock.calls[0]?.[0]).toMatchObject({ page: 1, size: 20 });
    expect(listCompanies.mock.calls[0]?.[0].useYn).toBeUndefined();

    fireEvent.change(screen.getByLabelText('검색어'), { target: { value: 'ACME' } });
    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    await waitFor(() => expect(listCompanies).toHaveBeenCalledWith(expect.objectContaining({ keyword: 'ACME' })));

    fireEvent.click(screen.getByRole('button', { name: '회사 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('회사 코드'), { target: { value: 'NEW_CO' } });
    fireEvent.change(within(dialog).getByLabelText('회사명'), { target: { value: '새회사' } });
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    fireEvent.click(await screen.findByTitle('자사'));
    fireEvent.change(within(dialog).getByLabelText('한국어 이름'), { target: { value: '새회사' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(createCompany).toHaveBeenCalled());
    expect(createCompany.mock.calls[0]?.[0]).toMatchObject({
      companyCode: 'NEW_CO',
      companyName: '새회사',
      translations: [{ supportLanguageCode: 'ko-KR', text: '새회사' }],
    });

    fireEvent.click(screen.getAllByRole('button', { name: '수정' })[0]);
    const editor = await screen.findByRole('dialog');
    await waitFor(() => expect(getCompany).toHaveBeenCalledWith(1));
    expect(within(editor).getByLabelText('회사 코드')).toBeDisabled();
    fireEvent.mouseDown(within(editor).getAllByRole('combobox')[2]);
    expect(await screen.findByTitle('GAMMA 감마')).toBeTruthy();
    expect(screen.queryByTitle('ACME 에이씨엠이')).toBeNull();
    fireEvent.click(within(editor).getByRole('button', { name: '저장' }));
    await waitFor(() => expect(updateCompany).toHaveBeenCalled());
    const updated = updateCompany.mock.calls[0]?.[1] as Record<string, unknown>;
    expect(updated.companyCode).toBeUndefined();
    expect(updated.address).toBe('서울');

    fireEvent.click(screen.getAllByRole('button', { name: '삭제' })[0]);
    const hint = await screen.findByText('삭제하면 사용 중지됩니다. 데이터는 지워지지 않습니다.');
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '삭제 확인' }));
    await waitFor(() => expect(deleteCompany).toHaveBeenCalledWith(1));

    fireEvent.click(screen.getByRole('button', { name: '복구' }));
    await waitFor(() => expect(restoreCompany).toHaveBeenCalledWith(2));
  });

  it('shows field errors and a server message, and hides buttons without permission', async () => {
    createCompany.mockRejectedValueOnce(
      Object.assign(new ApiError('이미 있는 회사 코드입니다.'), { errorCode: 'COMPANY.DUPLICATE' }),
    );
    render(
      <App>
        <CompanyPage />
      </App>,
    );
    await screen.findByText('에이씨엠이');
    fireEvent.click(screen.getByRole('button', { name: '회사 등록' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('회사 코드'), { target: { value: 'ACME' } });
    fireEvent.change(within(dialog).getByLabelText('회사명'), { target: { value: '에이씨엠이' } });
    fireEvent.mouseDown(within(dialog).getAllByRole('combobox')[0]);
    fireEvent.click(await screen.findByTitle('자사'));
    fireEvent.click(within(dialog).getByRole('button', { name: '저장' }));
    expect(await screen.findByText('이미 있는 회사 코드입니다.')).toBeTruthy();

    cleanup();
    const rejected = new ApiError('사용 중인 하위 회사가 있습니다.');
    rejected.errorCode = 'COMPANY.HAS_CHILD_COMPANY';
    deleteCompany.mockRejectedValueOnce(rejected);
    render(
      <App>
        <CompanyPage />
      </App>,
    );
    await screen.findByText('에이씨엠이');
    fireEvent.click(screen.getAllByRole('button', { name: '삭제' })[0]);
    const hint = await screen.findByText('삭제하면 사용 중지됩니다. 데이터는 지워지지 않습니다.');
    const popup = hint.closest('.ant-modal');
    if (!(popup instanceof HTMLElement)) {
      throw new Error('confirm missing');
    }
    fireEvent.click(within(popup).getByRole('button', { name: '삭제 확인' }));
    expect(await screen.findByText('사용 중인 하위 회사가 있습니다.')).toBeTruthy();

    cleanup();
    accessBag.can = (menu, action) => menu === 'COMPANY' && action === 'READ';
    render(
      <App>
        <CompanyPage />
      </App>,
    );
    await screen.findByText('에이씨엠이');
    expect(screen.queryByRole('button', { name: '회사 등록' })).toBeNull();
    expect(screen.queryByRole('button', { name: '수정' })).toBeNull();
    expect(screen.queryByRole('button', { name: '삭제' })).toBeNull();
    expect(screen.queryByRole('button', { name: '복구' })).toBeNull();
  });

  it('blocks a direct visit without read and keeps the company route out of the platform area', () => {
    expect(guardView('/org/company', { status: 'ready', items: [menu(['READ'])] })).toBe('allow');
    expect(guardView('/org/company', { status: 'ready', items: [menu(['CREATE'])] })).toBe('forbidden');
    setAccessContext({ kind: 'PLATFORM' });
    expect(areaForbidden('/org/company')).toBe(true);
    setAccessContext({ kind: 'TENANT' });
    expect(areaForbidden('/org/company')).toBe(false);
    expect(areaForbidden('/mdm/language')).toBe(false);
  });
});
