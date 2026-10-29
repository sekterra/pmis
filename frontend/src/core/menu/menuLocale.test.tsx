import { render } from '@testing-library/react';
import { createIntl, RawIntlProvider } from 'react-intl';
import React from 'react';
import { beforeEach, describe, expect, it } from 'vitest';
import { resetAccessContext, setAccessContext } from '@/core/auth/context';
import { menuDataRender } from './runtime';
import { resetMenuState, replaceMenuItemsForTest } from './store';
import type { MyMenu } from './types';
import koMenu from '@/locales/ko-KR/menu';

type MenuNode = {
  name?: string;
  locale?: boolean;
  children?: MenuNode[];
};

function labelsAsLayout(
  nodes: MenuNode[],
  formatMessage: (descriptor: { id: string }) => string,
): string[] {
  return nodes.flatMap((node) => {
    const label =
      node.locale === false || !node.name
        ? node.name ?? ''
        : formatMessage({ id: `menu.${node.name}` });
    return [
      ...(label ? [label] : []),
      ...labelsAsLayout(node.children ?? [], formatMessage),
    ];
  });
}

describe('menu locale warnings', () => {
  beforeEach(() => {
    resetAccessContext();
    resetMenuState();
  });

  it('does not emit Missing message while rendering platform menus', () => {
    setAccessContext({ kind: 'PLATFORM' });
    const missing: string[] = [];
    const intl = createIntl({
      locale: 'ko-KR',
      messages: koMenu as Record<string, string>,
      onError: (error) => {
        missing.push(String(error));
      },
    });
    const items = menuDataRender([
      { path: '/welcome', name: 'welcome' },
    ]) as MenuNode[];
    expect(items.every((item) => item.locale === false)).toBe(true);
    render(
      <RawIntlProvider value={intl}>
        <ul>
          {labelsAsLayout(items, intl.formatMessage).map((name) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
      </RawIntlProvider>,
    );
    expect(missing.filter((message) => /Missing message/i.test(message))).toEqual(
      [],
    );
    expect(labelsAsLayout(items, intl.formatMessage)).toEqual([
      '테넌트 관리',
      '지원 접속 기록',
    ]);
  });

  it('does not emit Missing message while rendering tenant menus', () => {
    setAccessContext({ kind: 'TENANT' });
    const menus: MyMenu[] = [
      {
        menuCode: 'SYSTEM',
        parentMenuCode: null,
        menuName: '시스템',
        menuTypeCode: 'GROUP',
        routePath: null,
        iconName: 'setting',
        levelNo: 1,
        sortOrder: 1,
        actions: [],
      },
      {
        menuCode: 'COMPANY',
        parentMenuCode: 'SYSTEM',
        menuName: '회사',
        menuTypeCode: 'SCREEN',
        routePath: '/org/company',
        iconName: 'appstore',
        levelNo: 2,
        sortOrder: 1,
        actions: ['READ'],
      },
    ];
    replaceMenuItemsForTest(menus);
    const missing: string[] = [];
    const intl = createIntl({
      locale: 'ko-KR',
      messages: koMenu as Record<string, string>,
      onError: (error) => {
        missing.push(String(error));
      },
    });
    const items = menuDataRender([
      { path: '/welcome', name: 'welcome' },
      { path: '/org/company', name: 'org.company' },
    ]) as MenuNode[];
    expect(items.every((item) => item.locale === false)).toBe(true);
    const labels = labelsAsLayout(items, intl.formatMessage);
    render(
      <RawIntlProvider value={intl}>
        <ul>
          {labels.map((name) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
      </RawIntlProvider>,
    );
    expect(missing.filter((message) => /Missing message/i.test(message))).toEqual(
      [],
    );
    expect(labels).toContain('시스템');
    expect(labels).toContain('회사');
    expect(labels).toContain('환영');
  });
});
