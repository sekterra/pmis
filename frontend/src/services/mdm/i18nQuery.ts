import type { MessageListQuery, NamespaceListQuery } from './i18nRecord';

function pageParams(input: NamespaceListQuery): Record<string, string | number | string[]> {
  const params: Record<string, string | number | string[]> = {
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
  const keyword = input.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  const useYn = input.useYn?.trim().toUpperCase();
  if (useYn === 'Y' || useYn === 'N') {
    params.useYn = useYn;
  }
  if (input.sort && input.sort.length > 0) {
    params.sort = input.sort;
  }
  return params;
}

export function namespaceListParams(
  input: NamespaceListQuery,
): Record<string, string | number | string[]> {
  return pageParams(input);
}

export function messageListParams(
  input: MessageListQuery,
): Record<string, string | number | string[]> {
  const params = pageParams(input);
  const namespaceCode = input.namespaceCode?.trim();
  if (namespaceCode) {
    params.namespaceCode = namespaceCode;
  }
  const messageTypeCode = input.messageTypeCode?.trim();
  if (messageTypeCode) {
    params.messageTypeCode = messageTypeCode;
  }
  const translationStatusCode = input.translationStatusCode?.trim();
  if (translationStatusCode) {
    params.translationStatusCode = translationStatusCode;
  }
  return params;
}
