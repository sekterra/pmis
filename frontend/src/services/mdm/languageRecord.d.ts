export type LanguageRecord = {
  supportLanguageCode: string;
  languageName: string;
  nativeName: string | null;
  isoLanguageCode: string | null;
  fallbackLanguageCode: string | null;
  textDirectionCode: string;
  dateFormat: string | null;
  datetimeFormat: string | null;
  numberFormat: string | null;
  defaultLanguage: boolean;
  sortOrder: number | null;
  useYn: string;
};

export type LanguageBody = {
  supportLanguageCode: string;
  languageName: string;
  nativeName: string | null;
  isoLanguageCode: string | null;
  fallbackLanguageCode: string | null;
  textDirectionCode: string;
  dateFormat: string | null;
  datetimeFormat: string | null;
  numberFormat: string | null;
  defaultLanguage: boolean;
  sortOrder: number | null;
};

export type NationRecord = {
  nationPk: number;
  nationCode: string;
  isoAlpha3Code: string | null;
  isoNumericCode: string | null;
  nationName: string;
  defaultLanguageCode: string;
  phoneCode: string | null;
  currencyCode: string | null;
  timezoneId: string | null;
  sortOrder: number | null;
  useYn: string;
};

export type NationBody = {
  nationCode: string;
  isoAlpha3Code: string | null;
  isoNumericCode: string | null;
  nationName: string;
  defaultLanguageCode: string;
  phoneCode: string | null;
  currencyCode: string | null;
  timezoneId: string | null;
  sortOrder: number | null;
};

export type NationListQuery = {
  page?: number;
  size?: number;
  sort?: string[];
};
