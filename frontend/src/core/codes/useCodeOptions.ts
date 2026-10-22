import { useEffect, useState, useSyncExternalStore } from 'react';
import { getI18nState, subscribeI18n } from '@/core/i18n/store';
import { listGroupCodes } from '@/services/mdm/codes';
import type { CommonCode } from '@/services/mdm/code';
import { toCodeOptions } from './options';

const cache = new Map<string, CommonCode[]>();
const pending = new Map<string, Promise<CommonCode[]>>();

export function resetCodeOptionsCache(): void {
  cache.clear();
  pending.clear();
}

export function loadCodeGroup(groupCode: string): Promise<CommonCode[]> {
  const cached = cache.get(groupCode);
  if (cached) {
    return Promise.resolve(cached);
  }
  const inflight = pending.get(groupCode);
  if (inflight) {
    return inflight;
  }
  const request = listGroupCodes(groupCode)
    .then((rows) => {
      cache.set(groupCode, rows);
      pending.delete(groupCode);
      return rows;
    })
    .catch((error: unknown) => {
      pending.delete(groupCode);
      throw error;
    });
  pending.set(groupCode, request);
  return request;
}

export function useCodeOptions(groupCode: string) {
  const locale = useSyncExternalStore(subscribeI18n, () => getI18nState().locale, () => getI18nState().locale);
  const [rows, setRows] = useState<CommonCode[] | undefined>(() => cache.get(groupCode));
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let alive = true;
    setFailed(false);
    loadCodeGroup(groupCode)
      .then((items) => {
        if (alive) {
          setRows(items);
        }
      })
      .catch(() => {
        if (alive) {
          setFailed(true);
          setRows([]);
        }
      });
    return () => {
      alive = false;
    };
  }, [groupCode]);

  const options = toCodeOptions(rows ?? [], locale);
  return {
    options,
    loading: rows === undefined && !failed,
    empty: rows !== undefined && options.length === 0,
  };
}
