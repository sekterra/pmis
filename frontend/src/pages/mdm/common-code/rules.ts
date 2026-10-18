import type { ApiError } from '@/core/request/errors';
import type { CommonCode, Translation } from '@/services/mdm/code';

export type CodeNode = CommonCode & { children?: CodeNode[] };

const LOCAL_ERRORS: Record<string, string> = {
  'AUTH.FORBIDDEN': '이 작업을 할 권한이 없습니다.',
  'CODE.DUPLICATE': '이미 있는 코드입니다.',
  'CODE.GROUP_IN_USE': '사용 중인 코드가 있어 그룹을 사용 중지할 수 없습니다.',
  'CODE.INVALID_POLICY_VALUE': '정책 값이 올바르지 않습니다.',
  'CODE.GROUP_NOT_FOUND': '코드 그룹을 찾을 수 없습니다.',
  'CODE.NOT_FOUND': '공통코드를 찾을 수 없습니다.',
  'CODE.PARENT_NOT_FOUND': '상위 코드를 찾을 수 없습니다.',
  'CODE.PARENT_NOT_ALLOWED': '계층형이 아닌 그룹에는 상위 코드를 넣을 수 없습니다.',
  'CODE.CYCLE': '상위 코드가 순환됩니다.',
  'CODE.VALUE_LENGTH': '코드 값이 허용 길이보다 깁니다.',
  'CODE.INVALID_DATE': '시작일은 종료일보다 늦을 수 없습니다.',
  'CODE.LANGUAGE_NOT_FOUND': '지원하지 않는 언어입니다.',
  'COMMON.VALIDATION_FAILED': '입력값을 확인해 주세요.',
  'COMMON.INVALID_PAGE': '페이지 값이 올바르지 않습니다.',
  'COMMON.INVALID_SORT': '정렬 값이 올바르지 않습니다.',
};

export function codeErrorText(error: Pick<ApiError, 'message' | 'errorCode'>): string {
  const server = error.message?.trim();
  if (server) {
    return server;
  }
  if (error.errorCode && LOCAL_ERRORS[error.errorCode]) {
    return LOCAL_ERRORS[error.errorCode];
  }
  return error.errorCode ?? '';
}

export function toTranslations(
  rows: Array<{
    supportLanguageCode: string;
    name?: string;
    description?: string;
  }>,
): Translation[] {
  return rows.flatMap((row) => {
    const name = row.name?.trim() ?? '';
    if (!name) {
      return [];
    }
    const description = row.description?.trim();
    return [
      {
        supportLanguageCode: row.supportLanguageCode,
        name,
        description: description ? description : null,
      },
    ];
  });
}

export function languagesMissingName(
  rows: Array<{ supportLanguageCode: string; name?: string; description?: string }>,
): string[] {
  return rows
    .filter((row) => !row.name?.trim() && Boolean(row.description?.trim()))
    .map((row) => row.supportLanguageCode);
}

export function toCodeTree(rows: CommonCode[]): CodeNode[] {
  const nodes = new Map<string, CodeNode>();
  for (const row of rows) {
    nodes.set(row.codeValue, { ...row });
  }
  const roots: CodeNode[] = [];
  for (const row of rows) {
    const node = nodes.get(row.codeValue);
    if (!node) {
      continue;
    }
    const parentValue = row.parentCodeValue;
    const parent = parentValue ? nodes.get(parentValue) : undefined;
    if (!parent || parent.codeValue === node.codeValue || createsCycle(node, parent, nodes)) {
      roots.push(node);
      continue;
    }
    parent.children = [...(parent.children ?? []), node];
  }
  return roots;
}

function createsCycle(node: CodeNode, parent: CodeNode, nodes: Map<string, CodeNode>): boolean {
  let cursor: string | null | undefined = parent.codeValue;
  const seen = new Set<string>();
  while (cursor) {
    if (cursor === node.codeValue || seen.has(cursor)) {
      return true;
    }
    seen.add(cursor);
    cursor = nodes.get(cursor)?.parentCodeValue;
  }
  return false;
}

export function parentChoices(rows: CommonCode[], selfValue?: string): CommonCode[] {
  const blocked = new Set<string>();
  if (selfValue) {
    blocked.add(selfValue);
    let grew = true;
    while (grew) {
      grew = false;
      for (const row of rows) {
        if (
          row.parentCodeValue &&
          blocked.has(row.parentCodeValue) &&
          !blocked.has(row.codeValue)
        ) {
          blocked.add(row.codeValue);
          grew = true;
        }
      }
    }
  }
  return rows.filter((row) => row.useYn === 'Y' && !blocked.has(row.codeValue));
}

export function datesInOrder(from?: string | null, to?: string | null): boolean {
  if (!from || !to) {
    return true;
  }
  return from <= to;
}

export function valueWithinLength(value: string, length?: number | null): boolean {
  if (length == null) {
    return true;
  }
  return value.trim().length <= length;
}

export type HistoryPair = {
  key: string;
  before: string;
  after: string;
};

export function historyPairs(
  beforeRaw: string | null,
  afterRaw: string | null,
): HistoryPair[] {
  const before = jsonObject(beforeRaw);
  const after = jsonObject(afterRaw);
  const keys = [...new Set([...Object.keys(before), ...Object.keys(after)])];
  return keys.map((key) => ({
    key,
    before: before[key] ?? '',
    after: after[key] ?? '',
  }));
}

function jsonObject(raw: string | null): Record<string, string> {
  if (!raw) {
    return {};
  }
  try {
    const value = JSON.parse(raw) as unknown;
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return { '': raw };
    }
    const entries: Record<string, string> = {};
    for (const [key, item] of Object.entries(value)) {
      entries[key] = item == null ? '' : typeof item === 'string' ? item : JSON.stringify(item);
    }
    return entries;
  } catch {
    return { '': raw };
  }
}

export function blankToNull(value?: string | null): string | null {
  const text = value?.trim();
  return text ? text : null;
}

export function inUse(useYn: string): boolean {
  return useYn === 'Y';
}
