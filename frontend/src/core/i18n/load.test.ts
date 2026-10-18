import { describe, expect, it, vi } from 'vitest';
import { loadCatalogForMode, loadPublicCatalog } from './load';
import { resolveText } from './messages';

const local = {
  'ko-KR': { 'button.save': '저장', 'only.local': '로컬' },
};

describe('public bundles', () => {
  it('prefers server text, then local text, then defaultMessage', () => {
    const server = { 'button.save': '서버 저장' };
    expect(resolveText('button.save', server, local['ko-KR'], '기본')).toBe(
      '서버 저장',
    );
    expect(resolveText('only.local', server, local['ko-KR'], '기본')).toBe(
      '로컬',
    );
    expect(resolveText('missing', server, local['ko-KR'], '기본')).toBe('기본');
  });

  it('keeps the previous bundle on 304 and falls back to local text when loading fails', async () => {
    const cache = {
      'ko-KR:common': { etag: 'W/"1"', messages: { 'button.save': '기존' } },
      'ko-KR:error': { messages: {} },
      'ko-KR:auth': { messages: {} },
    };
    const fetchBundle = vi.fn(async (namespace: string) => {
      if (namespace === 'common') {
        return { status: 304, etag: 'W/"1"' };
      }
      return {
        status: 200,
        etag: 'W/"2"',
        body: { namespaceCode: namespace, language: 'ko-KR', messages: {} },
      };
    });
    const loaded = await loadPublicCatalog({
      fetchLanguages: async () => [
        {
          supportLanguageCode: 'ko-KR',
          languageName: '한국어',
          nativeName: '한국어',
          defaultLanguage: true,
          fallbackLanguageCode: null,
          textDirectionCode: 'LTR',
          dateFormat: 'yyyy-MM-dd',
          datetimeFormat: 'yyyy-MM-dd HH:mm',
          numberFormat: '#,##0.##',
        },
      ],
      fetchBundle,
      localMessages: local,
      cache,
    });
    expect(loaded.messages['button.save']).toBe('기존');
    expect(fetchBundle).toHaveBeenCalledWith('common', 'ko-KR', 'W/"1"');

    const failed = await loadPublicCatalog({
      fetchLanguages: async () => {
        throw new Error('offline');
      },
      fetchBundle,
      localMessages: local,
    });
    expect(failed.messages['only.local']).toBe('로컬');
  });

  it('does not call server bundles in platform mode', async () => {
    const fetchLanguages = vi.fn();
    const fetchBundle = vi.fn();
    const loaded = await loadCatalogForMode('platform', {
      fetchLanguages,
      fetchBundle,
      localMessages: {
        ...local,
        'en-US': { 'pages.platform.title': 'PMIS Platform Console' },
      },
      savedLocale: 'en-US',
    });
    expect(fetchLanguages).not.toHaveBeenCalled();
    expect(fetchBundle).not.toHaveBeenCalled();
    expect(loaded.locale).toBe('en-US');
    expect(loaded.messages['pages.platform.title']).toBe(
      'PMIS Platform Console',
    );
  });
});
