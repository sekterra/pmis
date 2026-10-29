export function parentEdge(name: string | null | undefined, pk: number | null | undefined): string {
  const label = name?.trim();
  if (label) {
    return label;
  }
  if (pk != null) {
    return String(pk);
  }
  return '-';
}

export function parentChange(row: {
  beforeParentDeptName?: string | null;
  beforeParentDeptPk: number | null;
  afterParentDeptName?: string | null;
  afterParentDeptPk: number | null;
}): string {
  return `${parentEdge(row.beforeParentDeptName, row.beforeParentDeptPk)} → ${parentEdge(row.afterParentDeptName, row.afterParentDeptPk)}`;
}
