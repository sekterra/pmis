# ProTable / ProForm vitest 안내

## Setup

`tests/setupTests.ts`에 이미 `matchMedia`와 `ResizeObserver` 스텁이 있다. ProTable·ProForm을 목 없이 렌더할 때 추가로 필요한 것은 보통 없다.

## 렌더 골격

```tsx
import { renderWithPro } from '@/test/proRender';
import { screen, waitFor } from '@testing-library/react';

renderWithPro(<PageUnderTest />); // 기본 한국어 Pro intl
renderWithPro(<PageUnderTest />, { locale: 'en' });
```

- `@umijs/max`의 `useIntl`·`history`는 테스트에서 목한다.
- 서비스(`listTenants` 등)만 `vi.mock`한다. 실제 `fetch`/`rawRequest` 호출은 0건으로 확인한다.
- ProTable `request`는 마운트 후 비동기다. `await waitFor(() => expect(list).toHaveBeenCalled())`로 기다린다.
- 검색: 검색 폼의 입력에 값을 넣고 **조회** 버튼(내장 문구)을 누른 뒤 `request` 인자를 확인한다.
- ProForm 제출: 필드 입력 후 **등록**/제출 버튼을 누르고 서비스 본문을 확인한다.
- `antd/locale/*`를 직접 import하지 않는다. vitest ESM이 antd locale의 확장자 없는 상대경로를 해석하지 못한다. Pro 내장 문구는 `ProConfigProvider` + `createIntl`로 맞춘다(`src/test/proRender.tsx`).

## 로케일

앱은 `config.locale.antd: true`와 기본 `ko-KR`이라 런타임에서 Pro/antd 내장 문구가 한국어다(umi locale → antd + pro). 테스트는 `renderWithPro`로 Pro 조회·초기화 문구를 확인한다.
