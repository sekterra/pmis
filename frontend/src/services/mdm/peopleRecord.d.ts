export type PrimaryAssignment = {
  positionCode: string | null;
  assignmentStatusCode: string | null;
};

export type PersonListItem = {
  personPk: number;
  personName: string;
  primaryAssignment: PrimaryAssignment | null;
};

export type PersonListQuery = {
  deptPk: number;
  page?: number;
  size?: number;
};
