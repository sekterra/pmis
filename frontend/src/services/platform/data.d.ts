export type TenantSummary = {
  tenantCode: string;
  tenantName: string;
  schemaName: string;
  tenantStatusCode: string;
  defaultLanguageCode: string;
  timezoneId: string;
  provisioningStatus: string | null;
  primaryDomain: string | null;
  createDatetime: string | null;
};

export type DomainView = {
  domainName: string;
  domainTypeCode: string;
  primaryYn: string;
  useYn: string;
};

export type MigrationView = {
  migrationTypeCode: string;
  targetVersion: string;
  executionStatusCode: string;
  startedDatetime: string;
  finishedDatetime: string | null;
  errorMessage: string | null;
};

export type TenantDetail = {
  tenantCode: string;
  tenantName: string;
  schemaName: string;
  tenantStatusCode: string;
  defaultLanguageCode: string;
  timezoneId: string;
  schemaVersion: string;
  contractStartDate: string | null;
  contractEndDate: string | null;
  domains: DomainView[];
  provisioning: string | null;
  lastMigration: MigrationView | null;
};

export type CreateTenantRequest = {
  tenantCode: string;
  tenantName: string;
  defaultLanguageCode: string;
  timezoneId: string;
  contractStartDate?: string;
  contractEndDate: string | null;
  adminName: string;
  adminEmail: string;
  domains: string[];
};

export type CreatedTenant = {
  tenantCode: string;
  schemaName: string;
  tenantStatus: string;
  provisioningStep: string;
  executionStatus: string;
  temporaryPassword: string | null;
  adminLoginId: string;
};

export type ReissuedPassword = {
  tenantCode: string;
  temporaryPassword: string | null;
  adminLoginId: string;
};

export type TenantListQuery = {
  status?: string;
  keyword?: string;
  page: number;
  size: number;
  sort?: string[];
};

export type PageResult<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
};

export type PlatformMe = {
  platformAdminPk: number;
  loginId: string;
  adminName: string;
  email: string;
  passwordChangeRequired: boolean;
};
