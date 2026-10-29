const PREFERRED_ACTIONS = ['READ', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT'];

export type GrantSource = {
  permissionCode: string;
  menuCode: string;
  menuName: string;
  actionCode: string;
  granted: boolean;
};

export type MatrixCell = {
  permissionCode: string;
  granted: boolean;
};

export type MatrixRow = {
  menuCode: string;
  menuName: string;
  cells: Record<string, MatrixCell>;
};

export type PermissionMatrix = {
  columns: string[];
  rows: MatrixRow[];
};

export function buildMatrix(items: GrantSource[]): PermissionMatrix {
  const seen = new Set<string>();
  for (const item of items) {
    seen.add(item.actionCode);
  }
  const columns = [
    ...PREFERRED_ACTIONS.filter((action) => seen.has(action)),
    ...[...seen].filter((action) => !PREFERRED_ACTIONS.includes(action)),
  ];
  const rows: MatrixRow[] = [];
  const byMenu = new Map<string, MatrixRow>();
  for (const item of items) {
    let row = byMenu.get(item.menuCode);
    if (!row) {
      row = { menuCode: item.menuCode, menuName: item.menuName, cells: {} };
      byMenu.set(item.menuCode, row);
      rows.push(row);
    }
    row.cells[item.actionCode] = { permissionCode: item.permissionCode, granted: item.granted };
  }
  return { columns, rows };
}

export function cloneMatrix(matrix: PermissionMatrix): PermissionMatrix {
  return {
    columns: [...matrix.columns],
    rows: matrix.rows.map((row) => ({
      menuCode: row.menuCode,
      menuName: row.menuName,
      cells: Object.fromEntries(Object.entries(row.cells).map(([action, cell]) => [action, { ...cell }])),
    })),
  };
}

function withRow(matrix: PermissionMatrix, menuCode: string, change: (row: MatrixRow) => void): PermissionMatrix {
  const next = cloneMatrix(matrix);
  const row = next.rows.find((item) => item.menuCode === menuCode);
  if (!row) {
    return matrix;
  }
  change(row);
  return next;
}

export function setGranted(matrix: PermissionMatrix, menuCode: string, actionCode: string, granted: boolean): PermissionMatrix {
  return withRow(matrix, menuCode, (row) => {
    const cell = row.cells[actionCode];
    if (!cell) {
      return;
    }
    cell.granted = granted;
    if (granted && actionCode !== 'READ' && row.cells.READ) {
      row.cells.READ.granted = true;
    }
    if (!granted && actionCode === 'READ') {
      for (const item of Object.values(row.cells)) {
        item.granted = false;
      }
    }
  });
}

export function toggleMenu(matrix: PermissionMatrix, menuCode: string): PermissionMatrix {
  return withRow(matrix, menuCode, (row) => {
    const cells = Object.values(row.cells);
    const allOn = cells.length > 0 && cells.every((cell) => cell.granted);
    for (const cell of cells) {
      cell.granted = !allOn;
    }
  });
}

export function grantedCodes(matrix: PermissionMatrix): string[] {
  const codes: string[] = [];
  for (const row of matrix.rows) {
    for (const action of matrix.columns) {
      const cell = row.cells[action];
      if (cell?.granted) {
        codes.push(cell.permissionCode);
      }
    }
  }
  return codes;
}

export function matrixSnapshot(matrix: PermissionMatrix): string {
  return matrix.rows
    .map((row) => `${row.menuCode}:${Object.entries(row.cells).map(([action, cell]) => `${action}=${cell.granted ? 'Y' : 'N'}`).join(',')}`)
    .join('|');
}

export function isDirty(current: PermissionMatrix, baseline: PermissionMatrix): boolean {
  return matrixSnapshot(current) !== matrixSnapshot(baseline);
}
