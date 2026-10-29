export type FieldErrorItem = {
  field: string;
  message: string;
};

export type ApiEnvelope<T> = {
  success: boolean;
  data: T;
  errorCode?: string | null;
  errorMessage?: string | null;
  showType?: number | null;
  traceId?: string | null;
  fieldErrors?: FieldErrorItem[] | null;
};

export type Session = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  passwordChangeRequired: boolean;
};

export type LoginRequest = {
  loginId: string;
  password: string;
};

export type PasswordChangeRequest = {
  currentPassword: string;
  newPassword: string;
};

export type SupportSessionInfo = {
  supportAccessId: number;
  operatorLoginId: string;
  expireDatetime: string;
};

export type Me = {
  userPk: number;
  loginId: string;
  displayName: string;
  supportLanguageCode: string;
  timezoneId: string;
  roleCodes: string[];
  passwordChangeRequired: boolean;
  support?: SupportSessionInfo | null;
};

export type SupportLanguage = {
  supportLanguageCode: string;
  languageName: string;
  nativeName: string;
  defaultLanguage: boolean;
  fallbackLanguageCode: string | null;
  textDirectionCode: string;
  dateFormat: string;
  datetimeFormat: string;
  numberFormat: string;
};

export type Bundle = {
  namespaceCode: string;
  language: string;
  messages: Record<string, string>;
};

export type PageResult<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
};
