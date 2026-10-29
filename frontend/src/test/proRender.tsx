import {
  ProConfigProvider,
  createIntl,
} from '@ant-design/pro-components';
import { App } from 'antd';
import React from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import enMessages from '@ant-design/pro-components/es/provider/locale/en_US';
import koMessages from '@ant-design/pro-components/es/provider/locale/ko_KR';

const koIntl = createIntl('ko-KR', koMessages as Record<string, unknown>);
const enIntl = createIntl('en-US', enMessages as Record<string, unknown>);

type Options = RenderOptions & { locale?: 'ko' | 'en' };

/** ProTable/ProForm 내장 문구용. antd/locale 직접 import는 vitest ESM에서 깨질 수 있어 Pro intl을 쓴다. */
export function renderWithPro(
  ui: React.ReactElement,
  options: Options = {},
) {
  const { locale = 'ko', ...rest } = options;
  const intl = locale === 'en' ? enIntl : koIntl;
  return render(
    <ProConfigProvider intl={intl}>
      <App>{ui}</App>
    </ProConfigProvider>,
    rest,
  );
}

export { koIntl, enIntl };
