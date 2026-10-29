import { spawn } from 'node:child_process';
import path from 'node:path';
import { assertDevPortFree, resolveDevPort } from './devPort.ts';
import { applyUmiEnvFiles } from './localEnv.ts';
import { backendMissingNotice } from './proxyRules.ts';

async function main(): Promise<void> {
  const root = process.cwd();
  applyUmiEnvFiles(root, process.env);
  const notice = backendMissingNotice(process.env.DEV_BACKEND_URL);
  if (notice) {
    console.warn(notice);
  }
  const port = resolveDevPort(process.env);
  await assertDevPortFree(port);
  process.env.PORT = String(port);
  const bin = path.join(
    root,
    'node_modules',
    '.bin',
    process.platform === 'win32' ? 'max.cmd' : 'max',
  );
  const child = spawn(bin, ['dev'], {
    cwd: root,
    env: process.env,
    stdio: 'inherit',
    shell: process.platform === 'win32',
  });
  child.on('exit', (code) => {
    process.exit(code ?? 1);
  });
  child.on('error', (error) => {
    console.error(error.message);
    process.exit(1);
  });
}

main().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : '개발 서버를 시작하지 못했습니다.';
  console.error(message);
  process.exit(1);
});
