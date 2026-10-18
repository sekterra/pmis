import dayjs from 'dayjs';
import type { CreateTenantRequest } from '@/services/platform/data';

export const TENANT_CODE = /^[a-z0-9]+(-[a-z0-9]+)*$/;
export const HOSTNAME =
  /^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$/;
export const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const PROVISION_STEPS = [
  'REQUESTED',
  'SCHEMA_CREATED',
  'MIGRATED',
  'SEEDED',
  'ADMIN_CREATED',
  'COMPLETED',
] as const;

export const TENANT_LIST_SORT_FIELDS = [
  'tenantCode',
  'tenantName',
  'tenantStatusCode',
  'createDatetime',
] as const;

export const TENANT_STATUSES = [
  'PROVISIONING',
  'ACTIVE',
  'SUSPENDED',
  'TERMINATED',
  'FAILED',
];

export const SUPPORT_LANGUAGES = ['ko-KR', 'en-US'];

export type CreateFormValues = {
  tenantCode: string;
  tenantName: string;
  defaultLanguageCode: string;
  timezoneId?: string;
  contractStartDate?: string;
  contractEndDate?: string;
  adminName: string;
  adminEmail: string;
  domains: string[];
};

export function timezoneOptions(): string[] {
  const lookup = Intl as unknown as {
    supportedValuesOf?: (key: string) => string[];
  };
  const values = lookup.supportedValuesOf?.('timeZone') ?? ['UTC'];
  return values.includes('Asia/Seoul') ? values : ['Asia/Seoul', ...values];
}

export function validateCreate(
  values: CreateFormValues,
): Record<string, string> {
  const errors: Record<string, string> = {};
  const code = values.tenantCode?.trim() ?? '';
  if (!code || code.length > 50 || !TENANT_CODE.test(code)) {
    errors.tenantCode = 'code';
  }
  if (!values.tenantName?.trim()) {
    errors.tenantName = 'required';
  }
  if (!values.defaultLanguageCode) {
    errors.defaultLanguageCode = 'required';
  }
  if (!values.contractStartDate) {
    errors.contractStartDate = 'required';
  }
  if (
    values.contractEndDate &&
    values.contractStartDate &&
    values.contractEndDate < values.contractStartDate
  ) {
    errors.contractEndDate = 'range';
  }
  if (!values.adminName?.trim()) {
    errors.adminName = 'required';
  }
  const email = values.adminEmail?.trim() ?? '';
  if (!email || !EMAIL.test(email)) {
    errors.adminEmail = 'email';
  }
  const domains = (values.domains ?? [])
    .map((item) => item.trim())
    .filter(Boolean);
  if (domains.length === 0) {
    errors.domains = 'required';
  } else if (
    domains.some(
      (domain) =>
        domain.length > 253 ||
        domain !== domain.toLowerCase() ||
        !HOSTNAME.test(domain),
    )
  ) {
    errors.domains = 'domain';
  }
  return errors;
}

export function buildCreatePayload(
  values: CreateFormValues,
): CreateTenantRequest {
  return {
    tenantCode: values.tenantCode.trim(),
    tenantName: values.tenantName.trim(),
    defaultLanguageCode: values.defaultLanguageCode,
    timezoneId: values.timezoneId?.trim() || 'Asia/Seoul',
    contractStartDate: values.contractStartDate,
    contractEndDate: values.contractEndDate?.trim() || null,
    adminName: values.adminName.trim(),
    adminEmail: values.adminEmail.trim(),
    domains: values.domains.map((item) => item.trim()).filter(Boolean),
  };
}

export function visibleActions(status: string): {
  suspend: boolean;
  resume: boolean;
  retry: boolean;
  reissue: boolean;
} {
  return {
    suspend: status === 'ACTIVE',
    resume: status === 'SUSPENDED',
    retry: status === 'FAILED' || status === 'PROVISIONING',
    reissue: true,
  };
}

export function secretView(
  adminLoginId: string,
  temporaryPassword: string | null,
): { loginId: string; password?: string } {
  return {
    loginId: adminLoginId,
    ...(temporaryPassword ? { password: temporaryPassword } : {}),
  };
}

export function formatCreateDatetime(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }
  const parsed = dayjs(value);
  if (!parsed.isValid()) {
    return value;
  }
  return parsed.format('YYYY-MM-DD HH:mm');
}

export function provisioningLabel(
  code: string,
  text: (id: string, fallback: string) => string,
): string {
  if (
    code === 'FAILED' ||
    PROVISION_STEPS.includes(code as (typeof PROVISION_STEPS)[number])
  ) {
    return text(`pages.platform.step.${code}`, code);
  }
  return code;
}

export function provisioningTone(
  code: string | null | undefined,
): 'error' | 'success' | 'processing' | 'default' {
  if (code === 'FAILED') {
    return 'error';
  }
  if (code === 'COMPLETED') {
    return 'success';
  }
  if (
    code &&
    PROVISION_STEPS.includes(code as (typeof PROVISION_STEPS)[number])
  ) {
    return 'processing';
  }
  return 'default';
}

export function stepItems(provisioning: string | null): {
  title: string;
  status: 'wait' | 'process' | 'finish' | 'error';
}[] {
  const [step, execution] = (provisioning ?? '').split(':');
  const index = PROVISION_STEPS.indexOf(
    step as (typeof PROVISION_STEPS)[number],
  );
  const failed = execution === 'FAILED';
  return PROVISION_STEPS.map((title, itemIndex) => {
    if (index < 0) {
      return { title, status: 'wait' };
    }
    if (itemIndex < index) {
      return { title, status: 'finish' };
    }
    if (itemIndex === index) {
      if (failed) {
        return { title, status: 'error' };
      }
      return { title, status: title === 'COMPLETED' ? 'finish' : 'process' };
    }
    return { title, status: 'wait' };
  });
}
