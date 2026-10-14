import type { PageResult } from '@/services/mdm/data';

type TableParams = {
  current?: number;
  pageSize?: number;
  [key: string]: unknown;
};

type Sorter = Record<string, 'ascend' | 'descend' | null | undefined>;

export function toSortQuery(sorter: Sorter): string[] {
  return Object.entries(sorter)
    .filter((entry): entry is [string, 'ascend' | 'descend'] => {
      return entry[1] === 'ascend' || entry[1] === 'descend';
    })
    .map(
      ([field, order]) => `${field},${order === 'descend' ? 'desc' : 'asc'}`,
    );
}

export function toTableRequest<T, Q extends Record<string, unknown>>(
  service: (
    query: Q & { page: number; size: number; sort?: string[] },
  ) => Promise<PageResult<T>>,
) {
  return async (params: TableParams, sorter: Sorter = {}) => {
    const { current = 1, pageSize = 20, ...rest } = params;
    const sort = toSortQuery(sorter);
    const page = await service({
      ...(rest as Q),
      page: current,
      size: pageSize,
      ...(sort.length > 0 ? { sort } : {}),
    });
    return {
      data: page.items,
      total: page.total,
      success: true,
    };
  };
}
