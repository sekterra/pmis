import { describe, expect, it } from 'vitest';
import { roleListParams, toCreateBody, toUpdateBody } from './rolesQuery';

describe('role query', () => {
  it('omits an empty use filter and keeps only allowed sorts', () => {
    expect(roleListParams({ keyword: '  ', useYn: 'ALL', sort: ['name,asc', 'roleCode,desc'] })).toEqual({
      page: 1,
      size: 20,
      sort: ['roleCode,desc'],
    });
    expect(roleListParams({ keyword: '관리', useYn: 'y', page: 2, sort: ['roleName,asc', 'sortOrder,desc', 'rolePk,asc'] })).toEqual({
      page: 2,
      size: 20,
      keyword: '관리',
      useYn: 'Y',
      sort: ['roleName,asc', 'sortOrder,desc', 'rolePk,asc'],
    });
  });

  it('sends null for empty values and leaves the role code out of updates', () => {
    expect(
      toCreateBody({
        roleCode: ' VIEWER ',
        roleName: ' 조회 ',
        description: '  ',
        sortOrder: null,
        names: { 'ko-KR': '조회자', 'en-US': ' ' },
      }),
    ).toEqual({
      roleCode: 'VIEWER',
      roleName: '조회',
      sortOrder: null,
      description: null,
      translations: [{ supportLanguageCode: 'ko-KR', text: '조회자' }],
    });
    expect(toUpdateBody({ roleCode: 'VIEWER', roleName: '조회', description: '메모', sortOrder: 10, names: {} })).toEqual({
      roleName: '조회',
      sortOrder: 10,
      description: '메모',
      translations: [],
    });
  });
});
