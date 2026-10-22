import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import { assignmentListParams, peopleListParams } from './peopleQuery';
import type {
  Assignment,
  AssignmentBody,
  AssignmentListQuery,
  EndAssignmentBody,
  Person,
  PersonCreateBody,
  PersonListItem,
  PersonListQuery,
  PersonUpdateBody,
} from './peopleRecord';

const PEOPLE = '/api/mdm/v1/people';

function personUrl(personPk: number, suffix = ''): string {
  return `${PEOPLE}/${personPk}${suffix}`;
}

export function listPeople(query: PersonListQuery): Promise<PageResult<PersonListItem>> {
  return rawRequest<PageResult<PersonListItem>>(PEOPLE, {
    method: 'GET',
    params: peopleListParams(query),
  });
}

export function getPerson(personPk: number): Promise<Person> {
  return rawRequest<Person>(personUrl(personPk), { method: 'GET' });
}

export function createPerson(body: PersonCreateBody): Promise<Person> {
  return rawRequest<Person>(PEOPLE, { method: 'POST', data: body });
}

export function updatePerson(personPk: number, body: PersonUpdateBody): Promise<Person> {
  return rawRequest<Person>(personUrl(personPk), { method: 'PUT', data: body });
}

export function deletePerson(personPk: number): Promise<void> {
  return rawRequest<void>(personUrl(personPk), { method: 'DELETE' });
}

export function restorePerson(personPk: number): Promise<Person> {
  return rawRequest<Person>(personUrl(personPk, '/restore'), { method: 'POST' });
}

export function listAssignments(personPk: number, query: AssignmentListQuery): Promise<PageResult<Assignment>> {
  return rawRequest<PageResult<Assignment>>(personUrl(personPk, '/assignments'), {
    method: 'GET',
    params: assignmentListParams(query),
  });
}

export function createAssignment(personPk: number, body: AssignmentBody): Promise<Assignment> {
  return rawRequest<Assignment>(personUrl(personPk, '/assignments'), { method: 'POST', data: body });
}

export function updateAssignment(assignmentPk: number, body: AssignmentBody): Promise<Assignment> {
  return rawRequest<Assignment>(`/api/mdm/v1/person-assignments/${assignmentPk}`, { method: 'PUT', data: body });
}

export function endAssignment(assignmentPk: number, body: EndAssignmentBody): Promise<Assignment> {
  return rawRequest<Assignment>(`/api/mdm/v1/person-assignments/${assignmentPk}/end`, { method: 'POST', data: body });
}

export function deleteAssignment(assignmentPk: number): Promise<void> {
  return rawRequest<void>(`/api/mdm/v1/person-assignments/${assignmentPk}`, { method: 'DELETE' });
}

export function restoreAssignment(assignmentPk: number): Promise<Assignment> {
  return rawRequest<Assignment>(`/api/mdm/v1/person-assignments/${assignmentPk}/restore`, { method: 'POST' });
}
