import { spawn } from 'node:child_process';
import type { ClientRequest } from 'node:http';
import net from 'node:net';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { PORT_BUSY_MESSAGE, assertDevPortFree, portBusyMessage, resolveDevPort } from './devPort';
import { applyUmiEnvFiles, parseEnvFile } from './localEnv';
import { MISSING_BACKEND_MESSAGE, backendMissingNotice, createDevProxy } from './proxyRules';

function headerSink() {
  const headers: Record<string, string> = {};
  const req = {
    setHeader(name: string, value: string) {
      headers[name] = value;
    },
  } as ClientRequest;
  return { headers, req };
}

describe('dev proxy', () => {
  it('builds proxy rules only when the backend url is set', () => {
    expect(createDevProxy(undefined, 'acme')).toEqual({});
    expect(createDevProxy('  ', 'acme')).toEqual({});
    const rules = createDevProxy('http://backend.invalid', 'acme');
    expect(Object.keys(rules).sort()).toEqual(['/api/mdm', '/api/platform']);
    expect(rules['/api/mdm']?.target).toBe('http://backend.invalid');
    expect(rules['/api/platform']?.target).toBe('http://backend.invalid');
  });

  it('adds X-Tenant-Code on the tenant prefix only', () => {
    const rules = createDevProxy('http://backend.invalid', 'acme');
    const mdm = headerSink();
    const platform = headerSink();
    rules['/api/mdm']?.onProxyReq(mdm.req);
    rules['/api/platform']?.onProxyReq(platform.req);
    expect(mdm.headers['X-Tenant-Code']).toBe('acme');
    expect(platform.headers['X-Tenant-Code']).toBeUndefined();
  });

  it('warns only when the backend url is missing', () => {
    expect(backendMissingNotice(undefined)).toBe(MISSING_BACKEND_MESSAGE);
    expect(backendMissingNotice('')).toBe(MISSING_BACKEND_MESSAGE);
    expect(backendMissingNotice('http://backend.invalid')).toBeUndefined();
  });
});

describe('dev port', () => {
  it('uses 8000 unless PORT is set', () => {
    expect(resolveDevPort({})).toBe(8000);
    expect(resolveDevPort({ PORT: '9000' })).toBe(9000);
    expect(() => resolveDevPort({ PORT: 'nope' })).toThrow('PORT 값이 올바르지 않습니다.');
  });

  it('refuses a port that is already taken', async () => {
    const server = net.createServer();
    await new Promise<void>((resolve, reject) => {
      server.once('error', reject);
      server.listen(0, '0.0.0.0', () => resolve());
    });
    const address = server.address();
    const port = typeof address === 'object' && address ? address.port : 0;
    await expect(assertDevPortFree(port)).rejects.toThrow(portBusyMessage(port));
    expect(PORT_BUSY_MESSAGE).toBe(
      '이미 다른 개발 서버가 떠 있을 수 있습니다. 8000 포트를 확인하세요',
    );
    await new Promise<void>((resolve) => {
      server.close(() => resolve());
    });
    await expect(assertDevPortFree(port)).resolves.toBeUndefined();
  });
});

describe('local env files', () => {
  it('lets a later file replace an earlier key and keeps keys the file omits', () => {
    const dir = mkdtempSync(path.join(tmpdir(), 'pmis-env-'));
    writeFileSync(path.join(dir, '.env'), 'DEV_BACKEND_URL=http://from-env.invalid\n');
    writeFileSync(
      path.join(dir, '.env.local'),
      'DEV_BACKEND_URL=http://from-local.invalid\nDEV_TENANT_CODE=acme\n',
    );
    const env: Record<string, string | undefined> = {
      DEV_BACKEND_URL: 'http://from-shell.invalid',
      KEEP: 'from-shell',
      PORT: '9000',
    };
    applyUmiEnvFiles(dir, env);
    expect(env.DEV_BACKEND_URL).toBe('http://from-local.invalid');
    expect(env.DEV_TENANT_CODE).toBe('acme');
    expect(env.KEEP).toBe('from-shell');
    expect(env.PORT).toBe('9000');
    expect(parseEnvFile('DEV_BACKEND_URL="http://quoted.invalid" # note').DEV_BACKEND_URL).toBe(
      'http://quoted.invalid',
    );
  });
});

describe('dev launcher', () => {
  it('resolves its modules and stops when the port is taken', async () => {
    const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
    const env: Record<string, string | undefined> = { ...process.env };
    applyUmiEnvFiles(root, env);
    const port = resolveDevPort(env);
    const held = net.createServer();
    held.unref();
    await new Promise<void>((resolve, reject) => {
      held.once('error', (error: NodeJS.ErrnoException) => {
        if (error.code === 'EADDRINUSE') {
          resolve();
          return;
        }
        reject(error);
      });
      held.listen(port, '0.0.0.0', () => resolve());
    });
    const child = spawn(process.execPath, ['src/core/dev/runDev.ts'], {
      cwd: root,
      env: env as NodeJS.ProcessEnv,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    let output = '';
    child.stdout.on('data', (chunk: Buffer) => {
      output += chunk.toString();
    });
    child.stderr.on('data', (chunk: Buffer) => {
      output += chunk.toString();
    });
    const code = await new Promise<number>((resolve, reject) => {
      const timer = setTimeout(() => {
        child.kill();
        reject(new Error('dev launcher did not exit'));
      }, 4000);
      child.once('exit', (exitCode) => {
        clearTimeout(timer);
        resolve(exitCode ?? 1);
      });
    }).finally(() => {
      held.close();
    });
    expect(output).not.toContain('ERR_MODULE_NOT_FOUND');
    expect(output).toContain('이미 다른 개발 서버가 떠 있을 수 있습니다');
    expect(code).toBe(1);
  }, 5000);
});
