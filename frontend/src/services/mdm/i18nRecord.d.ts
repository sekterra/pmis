export type NamespaceRecord = {
  namespacePk: number;
  namespaceCode: string;
  namespaceName: string;
  solutionCode: string | null;
  description: string | null;
  useYn: string;
};

export type NamespaceBody = {
  namespaceCode: string;
  namespaceName: string;
  solutionCode: string | null;
  description: string | null;
};

export type MessageText = {
  supportLanguageCode: string;
  messageText: string;
  translationStatusCode: string | null;
  machineTranslated: boolean | null;
};

export type MessageTextView = MessageText & {
  versionNo: number | null;
};

export type MessageRecord = {
  messagePk: number;
  namespaceCode: string;
  messageKey: string;
  messageTypeCode: string;
  defaultMessage: string | null;
  paramDescription: string | null;
  useYn: string;
  translations: MessageTextView[];
};

export type MessageBody = {
  namespaceCode: string;
  messageKey: string;
  messageTypeCode: string;
  defaultMessage: string | null;
  paramDescription: string | null;
  translations: MessageText[];
};

export type NamespaceListQuery = {
  page?: number;
  size?: number;
  keyword?: string;
  useYn?: string;
  sort?: string[];
};

export type MessageListQuery = NamespaceListQuery & {
  namespaceCode?: string;
  messageTypeCode?: string;
  translationStatusCode?: string;
};
