import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/core/request/errors';
import { loadProfileUrl, profileFileIssue, releaseProfileUrl, sendProfileImage } from './profile';

describe('profile image', () => {
  it('uploads and then assigns, and skips assign when upload fails', async () => {
    const upload = vi.fn().mockResolvedValue({ filePk: 4 });
    const assign = vi.fn().mockResolvedValue(undefined);
    const file = new File(['png'], 'a.png', { type: 'image/png' });
    await sendProfileImage(7, file, upload, assign);
    expect(upload).toHaveBeenCalledWith(file);
    expect(assign).toHaveBeenCalledWith(7, 4);
    expect(upload.mock.invocationCallOrder[0]).toBeLessThan(assign.mock.invocationCallOrder[0]);

    const failed = vi.fn().mockRejectedValue(new ApiError('fail'));
    const skipped = vi.fn();
    await expect(sendProfileImage(7, file, failed, skipped)).rejects.toThrow('fail');
    expect(skipped).not.toHaveBeenCalled();
  });

  it('creates and releases a blob url, and treats a missing file as no photo', async () => {
    const create = vi.fn().mockReturnValue('blob:photo');
    const revoke = vi.fn();
    const blob = new Blob(['png']);
    const url = await loadProfileUrl(7, 'blob:old', async () => blob, create, revoke);
    expect(url).toBe('blob:photo');
    expect(revoke).toHaveBeenCalledWith('blob:old');
    releaseProfileUrl(url, revoke);
    expect(revoke).toHaveBeenCalledWith('blob:photo');

    const missing = new ApiError('');
    missing.errorCode = 'FILE.NOT_FOUND';
    const again = vi.fn();
    await expect(loadProfileUrl(7, 'blob:old', async () => { throw missing; }, create, again)).resolves.toBeUndefined();
    expect(again).toHaveBeenCalledWith('blob:old');
    expect(profileFileIssue({ name: 'a.txt', size: 1 })).toBe('extension');
    expect(profileFileIssue({ name: 'a.png', size: 50 * 1024 * 1024 + 1 })).toBe('size');
    expect(profileFileIssue({ name: 'a.png', size: 10 })).toBeUndefined();
  });
});
