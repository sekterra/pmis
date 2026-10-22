import type { NameTranslation } from './companyRecord';

export type Dept = {
  deptPk: number;
  companyPk: number;
  companyCode: string;
  parentDeptPk: number | null;
  parentDeptCode: string | null;
  deptCode: string;
  deptName: string;
  deptTypeCode: string | null;
  levelNo: number | null;
  deptPath: string;
  sortOrder: number | null;
  validFromDate: string | null;
  validToDate: string | null;
  description: string | null;
  useYn: string;
  createDatetime: string | null;
  createUserPk: number | null;
  updateDatetime: string | null;
  updateUserPk: number | null;
  translations?: NameTranslation[];
};

export type DeptListQuery = {
  companyCode?: string;
  keyword?: string;
  deptTypeCode?: string;
  useYn?: string;
  parentDeptPk?: number;
  page?: number;
  size?: number;
  sort?: string[];
};

export type DeptTreeQuery = {
  companyCode?: string;
  asOfDate?: string | null;
  includeClosed?: boolean;
};

export type DeptCreateBody = {
  companyCode: string;
  parentDeptCode: string | null;
  deptCode: string;
  deptName: string;
  deptTypeCode: string;
  sortOrder: number | null;
  validFromDate: string | null;
  validToDate: string | null;
  translations: NameTranslation[];
};

export type DeptUpdateBody = {
  parentDeptCode: string | null;
  deptName: string;
  deptTypeCode: string;
  sortOrder: number | null;
  validFromDate: string | null;
  validToDate: string | null;
  description: string | null;
  translations: NameTranslation[];
  effectiveDate?: string | null;
  changeReason?: string | null;
};

export type DeptHistory = {
  historyPk: number;
  deptPk: number;
  changeTypeCode: string;
  effectiveDate: string | null;
  beforeDeptName: string | null;
  afterDeptName: string | null;
  beforeParentDeptPk: number | null;
  afterParentDeptPk: number | null;
  changeReason: string | null;
  changeDatetime: string | null;
  changeUserPk: number | null;
};

export type DeptFormValues = {
  deptCode?: string;
  deptName?: string;
  deptTypeCode?: string;
  parentDeptCode?: string | null;
  sortOrder?: number | null;
  validFromDate?: string | null;
  validToDate?: string | null;
  names?: Record<string, string | undefined>;
  effectiveDate?: string | null;
  changeReason?: string | null;
};
