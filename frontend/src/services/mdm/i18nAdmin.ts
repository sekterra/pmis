import { rawRequest } from '@/core/request/runtime';
import type { PageResult } from './data';
import type {
  MessageBody,
  MessageListQuery,
  MessageRecord,
  NamespaceBody,
  NamespaceListQuery,
  NamespaceRecord,
} from './i18nRecord';
import { messageListParams, namespaceListParams } from './i18nQuery';

const NAMESPACES = '/api/mdm/v1/i18n-namespaces';
const MESSAGES = '/api/mdm/v1/i18n-messages';

function namespaceUrl(namespaceCode: string, suffix = ''): string {
  return `${NAMESPACES}/${encodeURIComponent(namespaceCode)}${suffix}`;
}

function messageUrl(messagePk: number, suffix = ''): string {
  return `${MESSAGES}/${messagePk}${suffix}`;
}

export function listNamespaces(query: NamespaceListQuery): Promise<PageResult<NamespaceRecord>> {
  return rawRequest<PageResult<NamespaceRecord>>(NAMESPACES, {
    method: 'GET',
    params: namespaceListParams(query),
  });
}

export function getNamespace(namespaceCode: string): Promise<NamespaceRecord> {
  return rawRequest<NamespaceRecord>(namespaceUrl(namespaceCode), { method: 'GET' });
}

export function createNamespace(body: NamespaceBody): Promise<NamespaceRecord> {
  return rawRequest<NamespaceRecord>(NAMESPACES, { method: 'POST', data: body });
}

export function updateNamespace(
  namespaceCode: string,
  body: NamespaceBody,
): Promise<NamespaceRecord> {
  return rawRequest<NamespaceRecord>(namespaceUrl(namespaceCode), { method: 'PUT', data: body });
}

export function deleteNamespace(namespaceCode: string): Promise<void> {
  return rawRequest<void>(namespaceUrl(namespaceCode), { method: 'DELETE' });
}

export function restoreNamespace(namespaceCode: string): Promise<NamespaceRecord> {
  return rawRequest<NamespaceRecord>(namespaceUrl(namespaceCode, '/restore'), { method: 'POST' });
}

export function listMessages(query: MessageListQuery): Promise<PageResult<MessageRecord>> {
  return rawRequest<PageResult<MessageRecord>>(MESSAGES, {
    method: 'GET',
    params: messageListParams(query),
  });
}

export function getMessage(messagePk: number): Promise<MessageRecord> {
  return rawRequest<MessageRecord>(messageUrl(messagePk), { method: 'GET' });
}

export function createMessage(body: MessageBody): Promise<MessageRecord> {
  return rawRequest<MessageRecord>(MESSAGES, { method: 'POST', data: body });
}

export function updateMessage(messagePk: number, body: MessageBody): Promise<MessageRecord> {
  return rawRequest<MessageRecord>(messageUrl(messagePk), { method: 'PUT', data: body });
}

export function deleteMessage(messagePk: number): Promise<void> {
  return rawRequest<void>(messageUrl(messagePk), { method: 'DELETE' });
}

export function restoreMessage(messagePk: number): Promise<MessageRecord> {
  return rawRequest<MessageRecord>(messageUrl(messagePk, '/restore'), { method: 'POST' });
}
