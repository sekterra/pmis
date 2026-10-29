import { describe, expect, it } from 'vitest';
import { toCreateBody, toRoleGrantBody, toStatusBody, toUpdateBody, userListParams } from './usersQuery';

describe('user query', () => {
  it('omits an empty use filter and keeps only allowed sorts', () => {
    expect(userListParams({ keyword: '  ', useYn: 'ALL', sort: ['name,asc', 'loginId,desc'] })).toEqual({
      page: 1,
      size: 20,
      sort: ['loginId,desc'],
    });
    expect(
      userListParams({
        keyword: 'hong',
        useYn: 'y',
        userStatusCode: 'ACTIVE',
        accountTypeCode: 'NORMAL',
        roleCode: 'VIEWER',
        page: 2,
        sort: ['userStatusCode,asc', 'lastLoginDatetime,desc', 'createDatetime,asc', 'userPk,desc'],
      }),
    ).toEqual({
      page: 2,
      size: 20,
      keyword: 'hong',
      useYn: 'Y',
      userStatusCode: 'ACTIVE',
      accountTypeCode: 'NORMAL',
      roleCode: 'VIEWER',
      sort: ['userStatusCode,asc', 'lastLoginDatetime,desc', 'createDatetime,asc', 'userPk,desc'],
    });
  });

  it('trims the login id and sends null for empty create values', () => {
    expect(
      toCreateBody({
        loginId: ' Hong.Kim ',
        accountTypeCode: 'NORMAL',
        personPk: 7,
        supportLanguageCode: 'ko-KR',
        timezoneId: ' ',
        description: '  ',
        roleCodes: [' VIEWER ', ''],
      }),
    ).toEqual({
      loginId: 'Hong.Kim',
      accountTypeCode: 'NORMAL',
      personPk: 7,
      supportLanguageCode: 'ko-KR',
      timezoneId: null,
      description: null,
      roleCodes: ['VIEWER'],
    });
    expect(toCreateBody({ loginId: 'hong', roleCodes: [] })).toMatchObject({ personPk: null, roleCodes: null });
  });

  it('sends only the four update fields and null when the person is cleared', () => {
    expect(
      toUpdateBody({
        loginId: 'hong',
        accountTypeCode: 'SYSTEM',
        personPk: null,
        supportLanguageCode: 'en-US',
        timezoneId: 'Asia/Seoul',
        description: '메모',
        roleCodes: ['VIEWER'],
      }),
    ).toEqual({
      personPk: null,
      supportLanguageCode: 'en-US',
      timezoneId: 'Asia/Seoul',
      description: '메모',
    });
  });

  it('builds role grants and a status body', () => {
    expect(toRoleGrantBody([{ roleCode: ' VIEWER ', validFromDate: '2026-10-01', validToDate: '  ' }])).toEqual([
      { roleCode: 'VIEWER', validFromDate: '2026-10-01', validToDate: null },
    ]);
    expect(toStatusBody('LOCKED')).toEqual({ userStatusCode: 'LOCKED' });
  });
});
