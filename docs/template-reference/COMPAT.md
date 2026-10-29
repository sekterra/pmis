# 호환 점검 (ProTable 등과 우리 규약)

| 주제 | 확인 | 해결/상태 |
| --- | --- | --- |
| 서버 페이징 | 우리 API는 `page`(1부터), `size`, `sort: string[]`. ProTable `request`는 `current`, `pageSize`, `sorter` | `frontend/src/core/request/table.ts`의 `toTableRequest` / `toSortQuery`가 이미 연결. 테넌트 목록에서 사용 중 |
| 오류 표시 | 우리 `ApiError` + `presentError` / 화면 `notice` | ProTable `request`가 throw하면 ProTable이 실패 UI를 냄. `toTableRequest`는 성공 시 `{ data, total, success: true }`만 반환하고 오류는 전파. 화면별 Alert(`notice`) 패턴과 병행 시 중복 가능 → 이전 시 한쪽으로 통일 필요 |
| 논리삭제·사용여부 | `useYn` 필터를 Space/Select로 둠 | ProTable `valueEnum`/`search` 필드로 옮길 수 있음. 난이도 중 |
| 권한 버튼 | `useAccess().can(menu, action)`로 숨김 | `toolBarRender`/컬럼 `render`에서 동일 조건 가능. 템플릿은 권한 없음 |
| 서버 메뉴·가드 | `menus/my` + `menuRoutes` + `RouteGuard` | Pro 화면 교체와 무관. 라우트 `hideInMenu` 유지 |
| i18n | 로컬 번들 + `formatMessage`/`text()` | ProTable 내장 문구는 Umi `locale.antd`·pro-components 로케일. 우리 기본 `ko-KR`. Pro 내장 키 한글 번들 보강은 이전 단계에서 확인 |
| vitest + ProTable | 기존 흐름 테스트는 `PageContainer`만 목 | ProTable을 실제 렌더하면 antd/Pro 의존·비동기 request 대기가 필요. 테넌트 목록 테스트 여부: 미확인(현 tenants 테스트가 ProTable을 깊게 타는지 재확인 권장). happy-dom에서 Dropdown 이슈는 F12.1에서 확인됨 |
| ModalForm vs Drawer | 우리는 antd Drawer+Form | ModalForm/DrawerForm으로 바꾸면 등록·수정 패턴이 table-list에 가까워짐 |
| FooterToolbar | 미사용 | 일괄 선택이 필요한 화면(현재 거의 없음)에만 검토 |

## 페이징 어댑터 인용

```21:40:frontend/src/core/request/table.ts
export function toTableRequest<T, Q extends Record<string, unknown>>(
  service: (
    query: Q & { page: number; size: number; sort?: string[] },
  ) => Promise<PageResult<T>>,
) {
  return async (params: TableParams, sorter: Sorter = {}) => {
    const { current = 1, pageSize = 20, ...rest } = params;
    ...
    return { data: page.items, total: page.total, success: true };
  };
}
```
