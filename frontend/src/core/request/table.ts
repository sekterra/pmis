import dayjs, { type Dayjs } from 'dayjs';
import type { PageResult } from '@/services/mdm/data';
import { ApiError, presentError, toApiError } from './errors';
import { getFeedback } from './feedback';

type TableParams = {
  current?: number;
  pageSize?: number;
  [key: string]: unknown;
};

type Sorter = Record<string, 'ascend' | 'descend' | null | undefined>;

export type TableRequestOptions = {
  /** throw면 ProTable 기본 실패 UI. toast-empty(기본)면 토스트 한 번 + 빈 결과. */
  onError?: 'toast-empty' | 'throw';
  /** ProTable 검색 필드를 API 쿼리 키로 바꿀 때. */
  mapParams?: (params: Record<string, unknown>) => Record<string, unknown>;
};

export function toSortQuery(sorter: Sorter): string[] {
  return Object.entries(sorter)
    .filter((entry): entry is [string, 'ascend' | 'descend'] => {
      return entry[1] === 'ascend' || entry[1] === 'descend';
    })
    .map(
      ([field, order]) => `${field},${order === 'descend' ? 'desc' : 'asc'}`,
    );
}

function toIso(value: unknown): string | undefined {
  if (value == null || value === '') {
    return undefined;
  }
  if (typeof value === 'string') {
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.toISOString() : value;
  }
  if (dayjs.isDayjs(value as Dayjs)) {
    const d = value as Dayjs;
    return d.isValid() ? d.toISOString() : undefined;
  }
  if (value instanceof Date) {
    return value.toISOString();
  }
  return undefined;
}

/** ProTable dateRange 값([start,end])을 from/to 문자열로 펼친다. */
export function expandDateRangeParams(
  params: Record<string, unknown>,
  rangeKey = 'issuedRange',
  fromKey = 'from',
  toKey = 'to',
): Record<string, unknown> {
  const next = { ...params };
  const range = next[rangeKey];
  delete next[rangeKey];
  if (Array.isArray(range) && range.length >= 2) {
    const from = toIso(range[0]);
    const to = toIso(range[1]);
    if (from) {
      next[fromKey] = from;
    }
    if (to) {
      next[toKey] = to;
    }
  }
  return next;
}

/** 코드·이름 검색을 API keyword 한 칸으로 합친다. */
export function mergeKeywordParams(
  params: Record<string, unknown>,
  fields: string[] = ['tenantCode', 'tenantName'],
  keywordKey = 'keyword',
): Record<string, unknown> {
  const next = { ...params };
  const parts: string[] = [];
  for (const field of fields) {
    const value = next[field];
    delete next[field];
    if (typeof value === 'string' && value.trim()) {
      parts.push(value.trim());
    }
  }
  if (parts.length > 0) {
    next[keywordKey] = parts.join(' ');
  }
  return next;
}

export function toTableRequest<T, Q extends Record<string, unknown>>(
  service: (
    query: Q & { page: number; size: number; sort?: string[] },
  ) => Promise<PageResult<T>>,
  options: TableRequestOptions = {},
) {
  const onError = options.onError ?? 'toast-empty';
  return async (params: TableParams, sorter: Sorter = {}) => {
    const { current = 1, pageSize = 20, ...rest } = params;
    let mapped: Record<string, unknown> = { ...rest };
    if (options.mapParams) {
      mapped = options.mapParams(mapped);
    }
    const sort = toSortQuery(sorter);
    try {
      const page = await service({
        ...(mapped as Q),
        page: current,
        size: pageSize,
        ...(sort.length > 0 ? { sort } : {}),
      });
      return {
        data: page.items,
        total: page.total,
        success: true,
      };
    } catch (error) {
      if (onError === 'throw') {
        throw error;
      }
      const apiError =
        error instanceof ApiError ? error : toApiError(error);
      presentError(apiError, getFeedback(), () => undefined);
      return {
        data: [] as T[],
        total: 0,
        success: false,
      };
    }
  };
}
