export function ynParam(value: string | undefined): 'Y' | 'N' | undefined {
  if (value === 'Y' || value === 'N') {
    return value;
  }
  return undefined;
}
