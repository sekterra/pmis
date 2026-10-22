import { beforeEach, describe, expect, it, vi } from 'vitest';
import { assignProfileImage, uploadFile } from './files';
import { createAssignment, createPerson, endAssignment } from './people';

const rawRequest = vi.hoisted(() => vi.fn());
const rawBlob = vi.hoisted(() => vi.fn());

vi.mock('@/core/request/runtime', () => ({
  rawRequest: (...args: unknown[]) => rawRequest(...args),
  rawBlob: (...args: unknown[]) => rawBlob(...args),
}));

describe('people service', () => {
  beforeEach(() => {
    rawRequest.mockReset();
    rawRequest.mockResolvedValue({ filePk: 4 });
  });

  it('posts a file field without a manual content type', async () => {
    const file = new File(['png'], 'a.png', { type: 'image/png' });
    await uploadFile(file);
    const [url, options] = rawRequest.mock.calls[0] as [string, { method: string; data: FormData; headers?: unknown }];
    expect(url).toBe('/api/mdm/v1/files');
    expect(options.method).toBe('POST');
    expect(options.headers).toBeUndefined();
    expect(options.data).toBeInstanceOf(FormData);
    expect(options.data.get('file')).toBe(file);
  });

  it('uses fileId when assigning a profile image', async () => {
    await assignProfileImage(7, 4);
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/people/7/profile-image', {
      method: 'PUT',
      data: { fileId: 4 },
    });
  });

  it('creates a person and posts an assignment end body', async () => {
    await createPerson({ personName: '홍길동', email: null, mobilePhoneNo: null, translations: [] });
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/people', {
      method: 'POST',
      data: { personName: '홍길동', email: null, mobilePhoneNo: null, translations: [] },
    });
    await createAssignment(7, {
      companyCode: 'ACME',
      deptCode: null,
      employeeNo: null,
      employmentTypeCode: 'FULL',
      positionCode: null,
      jobTitleCode: null,
      assignmentStatusCode: 'ACTIVE',
      primaryYn: 'Y',
      deptHeadYn: 'N',
      startDate: '2026-10-22',
      endDate: null,
      description: null,
    });
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/people/7/assignments', expect.objectContaining({ method: 'POST' }));
    await endAssignment(9, { endDate: '2026-10-22', assignmentStatusCode: 'RESIGNED' });
    expect(rawRequest).toHaveBeenCalledWith('/api/mdm/v1/person-assignments/9/end', {
      method: 'POST',
      data: { endDate: '2026-10-22', assignmentStatusCode: 'RESIGNED' },
    });
  });
});
