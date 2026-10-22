import { describe, expect, it } from 'vitest';
import type { Company } from '@/services/mdm/companyRecord';
import { companyListParams, toCreateBody, toNameTranslations, toUpdateBody, trimBusinessNo } from './companyQuery';

const current = {
  companyPk: 1,
  parentCompanyPk: null,
  parentCompanyCode: null,
  companyCode: 'ACME',
  companyName: '에이씨엠이',
  companyShortName: null,
  companyTypeCode: 'OWN',
  businessRegistrationNo: null,
  corporateRegistrationNo: '110111',
  representativeName: null,
  nationPk: null,
  nationCode: null,
  postalCode: '06236',
  address: '서울',
  addressDetail: null,
  phoneNo: null,
  faxNo: null,
  email: null,
  websiteUrl: null,
  sortOrder: 1,
  description: '메모',
  useYn: 'Y',
  createDatetime: null,
  createUserPk: null,
  updateDatetime: null,
  updateUserPk: null,
} satisfies Company;

describe('company query', () => {
  it('omits an empty use flag and unknown sort fields', () => {
    expect(
      companyListParams({
        page: 0,
        size: 0,
        keyword: '  ',
        useYn: 'ALL',
        companyTypeCode: ' OWN ',
        sort: ['email,asc', 'companyName,desc'],
      }),
    ).toEqual({
      page: 1,
      size: 20,
      companyTypeCode: 'OWN',
      sort: ['companyName,desc'],
    });
  });

  it('keeps only filled translations and sends null for empty choices', () => {
    expect(trimBusinessNo('  123-45-67890  ')).toBe('123-45-67890');
    expect(toNameTranslations({ 'ko-KR': ' 한글 ', 'en-US': '  ' })).toEqual([
      { supportLanguageCode: 'ko-KR', text: '한글' },
    ]);
    const created = toCreateBody({
      companyCode: ' ACME ',
      companyName: ' 에이씨엠이 ',
      companyShortName: ' ',
      companyTypeCode: 'OWN',
      nationCode: '',
      parentCompanyCode: undefined,
      names: { 'ko-KR': '에이씨엠이' },
    });
    expect(created.companyCode).toBe('ACME');
    expect(created.companyShortName).toBeNull();
    expect(created.nationCode).toBeNull();
    expect(created.parentCompanyCode).toBeNull();
    expect(created.translations).toEqual([{ supportLanguageCode: 'ko-KR', text: '에이씨엠이' }]);
  });

  it('omits the company code on update and keeps fields the form does not edit', () => {
    const body = toUpdateBody({ companyName: '바뀐이름', companyTypeCode: 'OWN', names: {} }, current);
    expect(body).not.toHaveProperty('companyCode');
    expect(body.companyName).toBe('바뀐이름');
    expect(body.address).toBe('서울');
    expect(body.description).toBe('메모');
    expect(body.translations).toEqual([]);
  });
});
