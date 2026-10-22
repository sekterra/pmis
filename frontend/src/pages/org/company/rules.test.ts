import { describe, expect, it } from 'vitest';
import { ApiError } from '@/core/request/errors';
import type { Company } from '@/services/mdm/companyRecord';
import { companyFormErrors, parentChoices, validCompanyCode } from './rules';

function company(code: string, useYn = 'Y'): Company {
  return {
    companyPk: code === 'ACME' ? 1 : 2,
    parentCompanyPk: null,
    parentCompanyCode: null,
    companyCode: code,
    companyName: code,
    companyShortName: null,
    companyTypeCode: 'OWN',
    businessRegistrationNo: null,
    corporateRegistrationNo: null,
    representativeName: null,
    nationPk: null,
    nationCode: null,
    postalCode: null,
    address: null,
    addressDetail: null,
    phoneNo: null,
    faxNo: null,
    email: null,
    websiteUrl: null,
    sortOrder: null,
    description: null,
    useYn,
    createDatetime: null,
    createUserPk: null,
    updateDatetime: null,
    updateUserPk: null,
  };
}

describe('company rules', () => {
  it('checks the company code and trims the business number through the form error map', () => {
    expect(validCompanyCode('ACME_1')).toBe(true);
    expect(validCompanyCode('acme')).toBe(false);
    expect(validCompanyCode('')).toBe(false);
    const duplicate = new ApiError('이미 있는 회사 코드입니다.');
    duplicate.errorCode = 'COMPANY.DUPLICATE';
    expect(companyFormErrors(duplicate)).toEqual([
      { name: 'companyCode', errors: ['이미 있는 회사 코드입니다.'] },
    ]);
    const cycle = new ApiError('');
    cycle.errorCode = 'COMPANY.PARENT_CYCLE';
    expect(companyFormErrors(cycle)[0]?.name).toBe('parentCompanyCode');
    const fields = new ApiError('확인해 주세요.');
    fields.fieldErrors = [{ field: 'companyName', message: '이름이 필요합니다.' }];
    expect(companyFormErrors(fields)[0]?.name).toBe('companyName');
  });

  it('drops the current company from parent choices', () => {
    const choices = parentChoices([company('ACME'), company('BETA'), company('OLD', 'N')], 'ACME');
    expect(choices.map((row) => row.companyCode)).toEqual(['BETA']);
  });
});
