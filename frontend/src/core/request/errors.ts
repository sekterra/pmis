import type { FieldErrorItem } from '@/services/mdm/data';

export class ApiError extends Error {
  errorCode?: string;
  showType?: number;
  traceId?: string;
  fieldErrors?: FieldErrorItem[];
  status?: number;
  retried = false;

  constructor(message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export type Feedback = {
  warning: (text: string) => void;
  error: (text: string) => void;
  notify: (title: string, description: string) => void;
};

type Envelope = {
  success?: boolean;
  errorCode?: string | null;
  errorMessage?: string | null;
  showType?: number | null;
  traceId?: string | null;
  fieldErrors?: FieldErrorItem[] | null;
};

export function toApiError(source: unknown, status?: number): ApiError {
  const fromResponse =
    source && typeof source === 'object' && 'response' in source
      ? (source as { response?: { data?: unknown; status?: number } }).response
      : undefined;
  const body = (fromResponse?.data ?? source) as Envelope | undefined;
  const resolvedStatus = status ?? fromResponse?.status;
  if (body && typeof body === 'object' && 'success' in body) {
    const error = new ApiError(body.errorMessage ?? '');
    error.errorCode = body.errorCode ?? undefined;
    error.showType = body.showType ?? undefined;
    error.traceId = body.traceId ?? undefined;
    error.fieldErrors = body.fieldErrors ?? undefined;
    error.status = resolvedStatus;
    return error;
  }
  if (source instanceof ApiError) {
    return source;
  }
  const error = new ApiError(
    source instanceof Error ? source.message : 'Request failed',
  );
  error.status = resolvedStatus;
  return error;
}

export function errorText(error: ApiError): string {
  if (error.traceId) {
    return `${error.message} (${error.traceId})`.trim();
  }
  return error.message;
}

export function presentError(
  error: ApiError,
  feedback: Feedback,
  goLogin: () => void,
): void {
  const text = errorText(error);
  switch (error.showType) {
    case 0:
      return;
    case 1:
      feedback.warning(text);
      return;
    case 2:
      feedback.error(text);
      return;
    case 3:
      feedback.notify(error.errorCode ?? '', text);
      return;
    case 9:
      goLogin();
      return;
    default:
      feedback.error(text);
  }
}

export function toFormFields(fieldErrors?: FieldErrorItem[] | null) {
  return (fieldErrors ?? []).map((item) => ({
    name: item.field,
    errors: [item.message],
  }));
}

export function unwrapData<T>(
  body: unknown,
  options?: { wrapped?: boolean },
): T {
  if (options?.wrapped === false) {
    return body as T;
  }
  if (body && typeof body === 'object' && 'success' in body) {
    const envelope = body as Envelope & { data?: T };
    if (envelope.success === false) {
      throw toApiError(envelope);
    }
    return envelope.data as T;
  }
  return body as T;
}
