export type NameTranslation = {
  supportLanguageCode: string;
  text: string;
};

export type Company = {
  companyPk: number;
  parentCompanyPk: number | null;
  parentCompanyCode: string | null;
  companyCode: string;
  companyName: string;
  companyShortName: string | null;
  companyTypeCode: string | null;
  businessRegistrationNo: string | null;
  corporateRegistrationNo: string | null;
  representativeName: string | null;
  nationPk: number | null;
  nationCode: string | null;
  postalCode: string | null;
  address: string | null;
  addressDetail: string | null;
  phoneNo: string | null;
  faxNo: string | null;
  email: string | null;
  websiteUrl: string | null;
  sortOrder: number | null;
  description: string | null;
  useYn: string;
  createDatetime: string | null;
  createUserPk: number | null;
  updateDatetime: string | null;
  updateUserPk: number | null;
  translations?: NameTranslation[] | null;
};

export type CompanyListQuery = {
  page?: number;
  size?: number;
  sort?: string[];
  keyword?: string;
  companyTypeCode?: string;
  useYn?: string;
};

export type CompanyWrite = {
  companyName: string;
  companyShortName: string | null;
  companyTypeCode: string;
  businessRegistrationNo: string | null;
  corporateRegistrationNo?: string | null;
  representativeName?: string | null;
  nationCode: string | null;
  parentCompanyCode: string | null;
  postalCode?: string | null;
  address?: string | null;
  addressDetail?: string | null;
  phoneNo?: string | null;
  faxNo?: string | null;
  email?: string | null;
  websiteUrl?: string | null;
  sortOrder: number | null;
  description?: string | null;
  translations: NameTranslation[];
};

export type CompanyCreateBody = CompanyWrite & {
  companyCode: string;
};

export type CompanyUpdateBody = CompanyWrite;
