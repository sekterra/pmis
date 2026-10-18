import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';

const ENV_FILES = ['.env', '.env.local'] as const;

/** Umi `loadEnv`와 같이 `.env` 다음 `.env.local`을 읽고, 파일에 있는 키는 기존 값을 덮어쓴다. */
export function applyUmiEnvFiles(
  cwd: string,
  env: Record<string, string | undefined>,
): void {
  for (const name of ENV_FILES) {
    const file = path.join(cwd, name);
    if (!existsSync(file)) {
      continue;
    }
    const parsed = parseEnvFile(readFileSync(file, 'utf8'));
    for (const key of Object.keys(parsed)) {
      env[key] = parsed[key];
    }
  }
}

export function parseEnvFile(text: string): Record<string, string> {
  const parsed: Record<string, string> = {};
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) {
      continue;
    }
    const body = line.startsWith('export ') ? line.slice('export '.length).trim() : line;
    const separator = body.indexOf('=');
    if (separator <= 0) {
      continue;
    }
    const key = body.slice(0, separator).trim();
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) {
      continue;
    }
    parsed[key] = unquoteEnvValue(body.slice(separator + 1).trim());
  }
  return parsed;
}

function unquoteEnvValue(value: string): string {
  if (value.startsWith('"') || value.startsWith("'")) {
    const quote = value[0];
    const end = value.indexOf(quote, 1);
    if (end > 0) {
      return value.slice(1, end);
    }
  }
  const comment = value.search(/\s#/);
  return comment >= 0 ? value.slice(0, comment).trim() : value;
}
