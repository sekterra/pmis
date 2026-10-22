import { ApiError } from '@/core/request/errors';

export const PROFILE_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif', 'webp'];
/** 문서의 기본 업로드 상한. 저장소 설정이 더 작으면 서버가 거절한다. */
export const PROFILE_MAX_BYTES = 50 * 1024 * 1024;

export function profileExtension(name: string): string {
  const parts = name.toLowerCase().split('.');
  return parts.length > 1 ? (parts[parts.length - 1] ?? '') : '';
}

export function profileFileIssue(file: { name: string; size: number }): 'extension' | 'size' | undefined {
  if (!PROFILE_EXTENSIONS.includes(profileExtension(file.name))) {
    return 'extension';
  }
  if (file.size > PROFILE_MAX_BYTES) {
    return 'size';
  }
  return undefined;
}

export function isMissingProfile(error: unknown): boolean {
  return error instanceof ApiError && error.errorCode === 'FILE.NOT_FOUND';
}

export async function sendProfileImage(
  personPk: number,
  file: File,
  upload: (file: File) => Promise<{ filePk: number }>,
  assign: (personPk: number, filePk: number) => Promise<void>,
): Promise<void> {
  const uploaded = await upload(file);
  await assign(personPk, uploaded.filePk);
}

export async function loadProfileUrl(
  personPk: number,
  previous: string | undefined,
  fetchBlob: (personPk: number) => Promise<Blob>,
  create: (blob: Blob) => string,
  revoke: (url: string) => void,
): Promise<string | undefined> {
  try {
    const blob = await fetchBlob(personPk);
    const url = create(blob);
    if (previous && previous !== url) {
      revoke(previous);
    }
    return url;
  } catch (error) {
    if (isMissingProfile(error)) {
      if (previous) {
        revoke(previous);
      }
      return undefined;
    }
    throw error;
  }
}

export function releaseProfileUrl(url: string | undefined, revoke: (url: string) => void): void {
  if (url) {
    revoke(url);
  }
}
