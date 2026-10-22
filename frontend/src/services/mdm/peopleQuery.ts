import { emptyToNull, toNameTranslations } from './companyQuery';
import type {
  Assignment,
  AssignmentBody,
  AssignmentFormValues,
  AssignmentListQuery,
  EndAssignmentBody,
  EndAssignmentValues,
  Person,
  PersonCreateBody,
  PersonFormValues,
  PersonListQuery,
  PersonUpdateBody,
} from './peopleRecord';

const PERSON_SORTS = new Set(['personName', 'email', 'personPk']);
const ASSIGNMENT_SORTS = new Set(['startDate', 'employeeNo', 'assignmentPk']);

function pageParams(input: { page?: number; size?: number; sort?: string[]; useYn?: string }, sorts: Set<string>) {
  const params: Record<string, string | number | string[]> = {
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
  const useYn = input.useYn?.trim().toUpperCase();
  if (useYn === 'Y' || useYn === 'N') {
    params.useYn = useYn;
  }
  const sort = (input.sort ?? []).filter((item) => sorts.has(item.split(',')[0] ?? ''));
  if (sort.length > 0) {
    params.sort = sort;
  }
  return params;
}

export function peopleListParams(input: PersonListQuery): Record<string, string | number | string[]> {
  const params = pageParams(input, PERSON_SORTS);
  const keyword = input.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  const companyCode = input.companyCode?.trim();
  if (companyCode) {
    params.companyCode = companyCode;
  }
  if (input.deptPk) {
    params.deptPk = input.deptPk;
  }
  const employmentTypeCode = input.employmentTypeCode?.trim();
  if (employmentTypeCode) {
    params.employmentTypeCode = employmentTypeCode;
  }
  const assignmentStatusCode = input.assignmentStatusCode?.trim();
  if (assignmentStatusCode) {
    params.assignmentStatusCode = assignmentStatusCode;
  }
  return params;
}

export function assignmentListParams(input: AssignmentListQuery): Record<string, string | number | string[]> {
  return pageParams(input, ASSIGNMENT_SORTS);
}

function personText(values: PersonFormValues): Pick<PersonCreateBody, 'personName' | 'email' | 'mobilePhoneNo' | 'translations'> {
  return {
    personName: values.personName?.trim() ?? '',
    email: emptyToNull(values.email),
    mobilePhoneNo: emptyToNull(values.mobilePhoneNo),
    translations: toNameTranslations(values.names),
  };
}

export function toCreateBody(values: PersonFormValues): PersonCreateBody {
  return personText(values);
}

export function toUpdateBody(values: PersonFormValues, current: Person): PersonUpdateBody {
  return {
    ...personText(values),
    officePhoneNo: current.officePhoneNo,
    description: current.description,
  };
}

export function toAssignmentBody(
  values: AssignmentFormValues,
  current: Assignment | undefined,
  forcePrimary: boolean,
): AssignmentBody {
  const deptCode = emptyToNull(values.deptCode);
  return {
    companyCode: values.companyCode?.trim() ?? '',
    deptCode,
    employeeNo: emptyToNull(values.employeeNo),
    employmentTypeCode: values.employmentTypeCode?.trim() ?? '',
    positionCode: emptyToNull(values.positionCode),
    jobTitleCode: emptyToNull(values.jobTitleCode),
    assignmentStatusCode: values.assignmentStatusCode?.trim() ?? '',
    primaryYn: forcePrimary || values.primaryYn ? 'Y' : 'N',
    deptHeadYn: deptCode && values.deptHeadYn ? 'Y' : 'N',
    startDate: values.startDate ?? '',
    endDate: emptyToNull(values.endDate),
    description: current?.description ?? null,
  };
}

export function toEndBody(values: EndAssignmentValues, today: string): EndAssignmentBody {
  return {
    endDate: emptyToNull(values.endDate) ?? today,
    assignmentStatusCode: emptyToNull(values.assignmentStatusCode) ?? 'RESIGNED',
  };
}

export function namesFromPerson(person: Person | undefined): Record<string, string> {
  const names: Record<string, string> = {};
  for (const item of person?.translations ?? []) {
    names[item.supportLanguageCode] = item.text;
  }
  return names;
}
