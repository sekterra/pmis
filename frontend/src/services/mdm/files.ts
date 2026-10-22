import { rawBlob, rawRequest } from '@/core/request/runtime';
import type { StoredFile } from './peopleRecord';

export function uploadFile(file: File): Promise<StoredFile> {
  const body = new FormData();
  body.append('file', file);
  return rawRequest<StoredFile>('/api/mdm/v1/files', { method: 'POST', data: body });
}

export function assignProfileImage(personPk: number, filePk: number): Promise<void> {
  return rawRequest<void>(`/api/mdm/v1/people/${personPk}/profile-image`, {
    method: 'PUT',
    data: { fileId: filePk },
  });
}

export function clearProfileImage(personPk: number): Promise<void> {
  return rawRequest<void>(`/api/mdm/v1/people/${personPk}/profile-image`, { method: 'DELETE' });
}

export function fetchProfileImage(personPk: number): Promise<Blob> {
  return rawBlob(`/api/mdm/v1/people/${personPk}/profile-image`);
}
