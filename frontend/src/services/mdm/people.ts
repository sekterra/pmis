import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import type { PersonListItem, PersonListQuery } from './peopleRecord';

export function peopleListParams(input: PersonListQuery): Record<string, number> {
  return {
    deptPk: input.deptPk,
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
}

export function listPeople(query: PersonListQuery): Promise<PageResult<PersonListItem>> {
  return rawRequest<PageResult<PersonListItem>>('/api/mdm/v1/people', {
    method: 'GET',
    params: peopleListParams(query),
  });
}
