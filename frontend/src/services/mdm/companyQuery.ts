import type { Company, CompanyCreateBody, CompanyListQuery, CompanyUpdateBody, NameTranslation } from './companyRecord';

const SORTS = new Set(['companyCode', 'companyName', 'sortOrder', 'companyPk']);

export function emptyToNull(value: string | null | undefined): string | null {
  const text = value?.trim() ?? '';
  return text ? text : null;
}

export function companyListParams(
  input: CompanyListQuery,
): Record<string, string | number | string[]> {
  const params: Record<string, string | number | string[]> = {
    page: input.page && input.page > 0 ? input.page : 1,
    size: input.size && input.size > 0 ? input.size : 20,
  };
  const keyword = input.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  const companyTypeCode = input.companyTypeCode?.trim();
  if (companyTypeCode) {
    params.companyTypeCode = companyTypeCode;
  }
  const useYn = input.useYn?.trim().toUpperCase();
  if (useYn === 'Y' || useYn === 'N') {
    params.useYn = useYn;
  }
  const sort = (input.sort ?? []).filter((item) => SORTS.has(item.split(',')[0] ?? ''));
  if (sort.length > 0) {
    params.sort = sort;
  }
  return params;
}

export function trimBusinessNo(value: string | null | undefined): string | null {
  return emptyToNull(value);
}

export function toNameTranslations(
  names: Record<string, string | undefined> | undefined,
): NameTranslation[] {
  return Object.entries(names ?? {}).flatMap(([supportLanguageCode, raw]) => {
    const text = raw?.trim() ?? '';
    if (!text) {
      return [];
    }
    return [{ supportLanguageCode, text }];
  });
}

type CompanyFormValues = {
  companyCode?: string;
  companyName?: string;
  companyShortName?: string;
  companyTypeCode?: string;
  businessRegistrationNo?: string;
  nationCode?: string;
  parentCompanyCode?: string;
  sortOrder?: number | null;
  names?: Record<string, string | undefined>;
};

export function toCreateBody(values: CompanyFormValues): CompanyCreateBody {
  return {
    companyCode: values.companyCode?.trim() ?? '',
    ...toWriteBody(values),
  };
}

export function toUpdateBody(values: CompanyFormValues, current: Company): CompanyUpdateBody {
  return {
    ...toWriteBody(values),
    corporateRegistrationNo: current.corporateRegistrationNo,
    representativeName: current.representativeName,
    postalCode: current.postalCode,
    address: current.address,
    addressDetail: current.addressDetail,
    phoneNo: current.phoneNo,
    faxNo: current.faxNo,
    email: current.email,
    websiteUrl: current.websiteUrl,
    description: current.description,
  };
}

function toWriteBody(values: CompanyFormValues): CompanyUpdateBody {
  return {
    companyName: values.companyName?.trim() ?? '',
    companyShortName: emptyToNull(values.companyShortName),
    companyTypeCode: values.companyTypeCode?.trim() ?? '',
    businessRegistrationNo: trimBusinessNo(values.businessRegistrationNo),
    nationCode: emptyToNull(values.nationCode),
    parentCompanyCode: emptyToNull(values.parentCompanyCode),
    sortOrder: typeof values.sortOrder === 'number' ? values.sortOrder : null,
    translations: toNameTranslations(values.names),
  };
}
