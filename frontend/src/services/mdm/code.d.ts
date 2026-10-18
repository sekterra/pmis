export type Translation = {
  supportLanguageCode: string;
  name: string;
  description: string | null;
};

export type CodeGroup = {
  codeGroupPk: number;
  codeGroupCode: string;
  solutionCode: string | null;
  codeGroupName: string;
  description: string | null;
  hierarchy: boolean;
  codeValueLength: number | null;
  extAttrSchema: string | null;
  sortOrder: number | null;
  useYn: string;
  translations: Translation[];
};

export type CodeGroupBody = {
  codeGroupCode: string;
  solutionCode: string | null;
  codeGroupName: string;
  description: string | null;
  hierarchy: boolean;
  codeValueLength: number | null;
  extAttrSchema: string | null;
  sortOrder: number | null;
  translations: Translation[];
};

export type CommonCode = {
  codePk: number;
  groupCode: string;
  codeValue: string;
  codeName: string;
  parentCodeValue: string | null;
  levelNo: number | null;
  codePath: string | null;
  sortOrder: number | null;
  extAttr1: string | null;
  extAttr2: string | null;
  extAttr3: string | null;
  extAttr4: string | null;
  extAttr5: string | null;
  validFromDate: string | null;
  validToDate: string | null;
  useYn: string;
  translations: Translation[];
};

export type CommonCodeBody = {
  groupCode: string;
  codeValue: string;
  codeName: string;
  parentCodeValue: string | null;
  sortOrder: number | null;
  extAttr1: string | null;
  extAttr2: string | null;
  extAttr3: string | null;
  extAttr4: string | null;
  extAttr5: string | null;
  validFromDate: string | null;
  validToDate: string | null;
  changeReason: string | null;
  translations: Translation[];
};

export type CodeHistory = {
  historyPk: number;
  codePk: number;
  codeGroupCode: string;
  codeValue: string;
  changeTypeCode: string;
  beforeData: string | null;
  afterData: string | null;
  changeReason: string | null;
  changeDatetime: string | null;
  changeUserPk: number | null;
};

export type CodeListQuery = {
  page?: number;
  size?: number;
  sort?: string[];
  keyword?: string;
  useYn?: string;
  solutionCode?: string;
  groupCode?: string;
};
