import type { NameTranslation } from './companyRecord';

export type PrimaryAssignment = {
  companyCode: string | null;
  companyName: string | null;
  deptCode: string | null;
  deptName: string | null;
  positionCode: string | null;
  jobTitleCode: string | null;
  employeeNo: string | null;
  assignmentStatusCode: string | null;
};

export type PersonListItem = {
  personPk: number;
  personName: string;
  email: string | null;
  mobilePhoneNo: string | null;
  hasProfileImage: boolean;
  useYn: string;
  primaryAssignment: PrimaryAssignment | null;
};

export type Assignment = {
  assignmentPk: number;
  personPk: number;
  companyPk: number | null;
  companyCode: string;
  companyName: string | null;
  deptPk: number | null;
  deptCode: string | null;
  deptName: string | null;
  employeeNo: string | null;
  employmentTypeCode: string;
  positionCode: string | null;
  jobTitleCode: string | null;
  assignmentStatusCode: string;
  primaryYn: string;
  deptHeadYn: string;
  startDate: string;
  endDate: string | null;
  description: string | null;
  useYn: string;
  createDatetime: string | null;
  createUserPk: number | null;
  updateDatetime: string | null;
  updateUserPk: number | null;
};

export type Person = PersonListItem & {
  officePhoneNo: string | null;
  profileFilePk: number | null;
  description: string | null;
  createDatetime: string | null;
  createUserPk: number | null;
  updateDatetime: string | null;
  updateUserPk: number | null;
  translations?: NameTranslation[] | null;
  assignments?: Assignment[] | null;
};

export type PersonListQuery = {
  page?: number;
  size?: number;
  sort?: string[];
  keyword?: string;
  companyCode?: string;
  deptPk?: number;
  employmentTypeCode?: string;
  assignmentStatusCode?: string;
  useYn?: string;
};

export type PersonFormValues = {
  personName?: string;
  email?: string;
  mobilePhoneNo?: string;
  names?: Record<string, string | undefined>;
};

export type PersonCreateBody = {
  personName: string;
  email: string | null;
  mobilePhoneNo: string | null;
  translations: NameTranslation[];
};

export type PersonUpdateBody = PersonCreateBody & {
  officePhoneNo: string | null;
  description: string | null;
};

export type AssignmentListQuery = {
  page?: number;
  size?: number;
  sort?: string[];
  useYn?: string;
};

export type AssignmentFormValues = {
  companyCode?: string;
  deptCode?: string;
  employeeNo?: string;
  employmentTypeCode?: string;
  positionCode?: string;
  jobTitleCode?: string;
  assignmentStatusCode?: string;
  primaryYn?: boolean;
  deptHeadYn?: boolean;
  startDate?: string;
  endDate?: string;
};

export type AssignmentBody = {
  companyCode: string;
  deptCode: string | null;
  employeeNo: string | null;
  employmentTypeCode: string;
  positionCode: string | null;
  jobTitleCode: string | null;
  assignmentStatusCode: string;
  primaryYn: 'Y' | 'N';
  deptHeadYn: 'Y' | 'N';
  startDate: string;
  endDate: string | null;
  description: string | null;
};

export type EndAssignmentValues = {
  endDate?: string;
  assignmentStatusCode?: string;
};

export type EndAssignmentBody = {
  endDate: string;
  assignmentStatusCode: string;
};

export type StoredFile = {
  filePk: number;
};
