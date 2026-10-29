import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { defineConfig } from 'vitest/config';

const antdLoader = pathToFileURL(
  join(__dirname, 'tests/antd-esm-loader.mjs'),
).href;

export default defineConfig({
  resolve: {
    alias: {
      '@/core/menu/runtime': join(__dirname, 'src/core/menu/runtime.tsx'),
      '@': join(__dirname, 'src'),
      '@root': join(__dirname),
      '@@': join(__dirname, 'src', '.umi'),
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: ['./tests/setupTests.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    exclude: ['node_modules', 'dist', '.umi'],
    pool: 'forks',
    execArgv: ['--experimental-loader', antdLoader],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/.umi/**',
        'src/services/ant-design-pro/**',
        'src/**/*.d.ts',
        'src/**/index.style.ts',
      ],
    },
    passWithNoTests: true,
    fileParallelism: false,
    testTimeout: 30000,
  },
});
