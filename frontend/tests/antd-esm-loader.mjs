import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { existsSync } from 'node:fs';

/**
 * antd / @rc-component 등 ESM이 확장자 없는 상대경로를 import할 때 Node 해석을 보정한다.
 */
export async function resolve(specifier, context, nextResolve) {
  const parent = context.parentURL ?? '';
  if (
    parent.includes('/node_modules/') &&
    specifier.startsWith('.') &&
    !/\.(js|mjs|cjs|json|ts|tsx|css|node)$/.test(specifier)
  ) {
    const parentPath = fileURLToPath(parent);
    const withJs = join(dirname(parentPath), `${specifier}.js`);
    if (existsSync(withJs)) {
      return {
        shortCircuit: true,
        url: pathToFileURL(withJs).href,
      };
    }
  }
  return nextResolve(specifier, context);
}
