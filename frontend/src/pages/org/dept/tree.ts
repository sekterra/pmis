import type { Dept } from '@/services/mdm/deptRecord';

export type DeptNode = Dept & { children: DeptNode[] };

export function pathParts(deptPath: string | null | undefined): string[] {
  return (deptPath ?? '').split('/').filter(Boolean);
}

export function deptLevel(row: Pick<Dept, 'levelNo' | 'deptPath'>): number {
  if (row.levelNo != null) {
    return row.levelNo;
  }
  return pathParts(row.deptPath).length;
}

export function nestDepts(rows: Dept[]): DeptNode[] {
  const nodes = new Map<number, DeptNode>();
  for (const row of rows) {
    nodes.set(row.deptPk, { ...row, children: [] });
  }
  const roots: DeptNode[] = [];
  for (const row of rows) {
    const node = nodes.get(row.deptPk);
    if (!node) {
      continue;
    }
    const parts = pathParts(row.deptPath);
    const parentId = parts.length >= 2 ? Number(parts[parts.length - 2]) : Number.NaN;
    const parent = Number.isFinite(parentId) ? nodes.get(parentId) : undefined;
    if (parent && parent.deptPk !== node.deptPk) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }
  return roots;
}

export function deptNodeLabel(row: Pick<Dept, 'deptName' | 'deptCode' | 'useYn'>, closedText: string): string {
  const base = `${row.deptName} ${row.deptCode}`;
  return row.useYn === 'Y' ? base : `${base} ${closedText}`;
}

export function ancestorKeys(rows: Dept[], deptPk: number): string[] {
  const row = rows.find((item) => item.deptPk === deptPk);
  if (!row) {
    return [];
  }
  const parts = pathParts(row.deptPath);
  parts.pop();
  return parts;
}

export function defaultExpandedKeys(rows: Dept[], selectedPk?: number): string[] {
  const open = rows.filter((row) => deptLevel(row) <= 1).map((row) => String(row.deptPk));
  const ancestors = selectedPk ? ancestorKeys(rows, selectedPk) : [];
  return [...new Set([...open, ...ancestors])];
}

export function forbiddenParentPks(rows: Dept[], selfPk: number): Set<number> {
  const self = rows.find((row) => row.deptPk === selfPk);
  const blocked = new Set<number>([selfPk]);
  if (!self?.deptPath) {
    return blocked;
  }
  for (const row of rows) {
    if (row.deptPath === self.deptPath || row.deptPath.startsWith(`${self.deptPath}/`)) {
      blocked.add(row.deptPk);
    }
  }
  return blocked;
}
