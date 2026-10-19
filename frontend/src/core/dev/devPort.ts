import net from 'node:net';

export const PORT_BUSY_MESSAGE =
  '이미 다른 개발 서버가 떠 있을 수 있습니다. 8000 포트를 확인하세요';

const DEV_PORT = 8000;

export function resolveDevPort(env: { PORT?: string }): number {
  const raw = env.PORT?.trim();
  if (!raw) {
    return DEV_PORT;
  }
  if (!/^\d+$/.test(raw)) {
    throw new Error('PORT 값이 올바르지 않습니다.');
  }
  const port = Number(raw);
  if (port < 1 || port > 65535) {
    throw new Error('PORT 값이 올바르지 않습니다.');
  }
  return port;
}

export function portBusyMessage(port: number): string {
  return `${PORT_BUSY_MESSAGE} (${port})`;
}

/** 지정 포트가 비어 있을 때만 성공한다. 사용 중이면 Umi가 다른 포트로 넘어가기 전에 멈춘다. */
export async function assertDevPortFree(port: number): Promise<void> {
  const free = await canListen(port);
  if (!free) {
    throw new Error(portBusyMessage(port));
  }
}

function canListen(port: number): Promise<boolean> {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.unref();
    server.once('error', (error: NodeJS.ErrnoException) => {
      if (error.code === 'EADDRINUSE') {
        resolve(false);
        return;
      }
      reject(error);
    });
    server.listen(port, '0.0.0.0', () => {
      server.close(() => resolve(true));
    });
  });
}
