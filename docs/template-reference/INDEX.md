# 템플릿 패턴 요약

출처: ant-design-pro **v6.0.3** (`adfd44085738ca953573a13322c1ba84aca8b9e3`). pro-blocks는 사용하지 않았다(해당 태그에 form/list 등이 있음).

## 표준 CRUD 한 장 요약 (table-list 기준)

| 단계 | 부품 | 핵심 |
| --- | --- | --- |
| 목록 | `PageContainer` + `ProTable` | `search={{ labelWidth: 120 }}`, `request={rule}`, `rowSelection`, `toolBarRender`에 등록 |
| 등록 | `ModalForm` + `ProFormText`/`ProFormTextArea` | `CreateForm.tsx`, `width="400px"`, 성공 시 `message` + `actionRef.reload` |
| 수정 | `StepsForm` in `Modal` | `UpdateForm.tsx`, 트리거 링크, 성공 시 reload |
| 상세 | `Drawer` + `ProDescriptions` | 행 이름 클릭 → Drawer, columns를 descriptions에 재사용 |
| 삭제 | `FooterToolbar` + 버튼 | 선택 행 일괄 삭제, `useMutation` + `reloadAndRest` |
| 새로고침 | `actionRef.current?.reload` / `reloadAndRest` | react-query `invalidateQueries`와 병행 |

인용 (`src/pages/table-list/index.tsx`):

```246:266:docs/template-reference/src/pages/table-list/index.tsx
      <ProTable<API.RuleListItem, API.PageParams>
        ...
        search={{
          labelWidth: 120,
        }}
        toolBarRender={() => [
          <CreateForm key="create" reload={actionRef.current?.reload} />,
        ]}
        request={rule}
```

## 전역 설정

| 설정 | 값 | 위치 |
| --- | --- | --- |
| antd `configProvider.variant` | `'filled'` | `config/config.ts` 147–156 |
| layout | `mix` | `config/defaultSettings.ts` |
| contentWidth | `Fluid` | 동상 |
| fixedHeader | `false` | 동상 |
| fixSiderbar | `true` | 동상 |
| colorPrimary | `#1677ff` | 동상 |
| locale default | `zh-CN`, `antd: true` | `config/config.ts` |
| 메시지 | `message.useMessage()` + contextHolder | table-list 등 |

## 화면별 요약

### `/list/table-list` → `src/pages/table-list`

- 부품: PageContainer, ProTable, FooterToolbar, Drawer, ProDescriptions, ModalForm(Create), StepsForm+Modal(Update)
- search: `labelWidth: 120`, defaultCollapsed 미지정
- toolBarRender: CreateForm(신규)
- 행 선택 + FooterToolbar 일괄 삭제/승인
- 페이지네이션: ProTable 기본(request가 current/pageSize)
- 성공·오류: `messageApi.success` / `error`

### `/form/basic-form` → `src/pages/form/basic-form`

- 부품: PageContainer, Card(`variant="borderless"`), ProForm, ProFormText/DateRange/TextArea/Digit/Radio/Select/Dependency
- 레이아웃: ProForm `maxWidth: 600`, `layout="vertical"`, 필드 `width="md"`
- 성공: `message.success('提交成功')`

### `/form/step-form` → `src/pages/form/step-form`

- 부품: PageContainer, Card, StepsForm, ProForm* 필드
- 단계 폼으로 긴 작업을 나눔

### `/form/advanced-form` → `src/pages/form/advanced-form`

- 부품: PageContainer, ProForm, EditableProTable, FooterToolbar, Card, Row/Col
- 하단 FooterToolbar에 제출

### `/list/basic-list` → `src/pages/list/basic-list`

- 부품: PageContainer, Card, List(antd), Modal(`OperationModal`), Progress, Segmented
- ProTable 아님. 카드 헤더 + 리스트 + 모달 CRUD

### `/list/card-list` → `src/pages/list/card-list`

- 부품: PageContainer, Card, List(grid)
- 카드 그리드 목록

### `/list/search/*` → `src/pages/list/search`

- 부품: PageContainer, 탭 + articles/projects/applications 하위
- 검색형 리스트(필터 태그·아바타 등 페이지 로컬 컴포넌트)

### `/profile/basic` → `src/pages/profile/basic`

- 부품: PageContainer, Card, ProTable(읽기 전용 표 2개)
- 상세 요약 + 표

### `/profile/advanced` → `src/pages/profile/advanced`

- 부품: PageContainer(extra/tabList), Card, Steps, Descriptions(antd), Table
- 복잡한 상세 헤드

### `/account/center` → `src/pages/account/center`

- 부품: PageContainer, Card, 탭(Articles/Applications/Projects 로컬)
- 개인 센터

### `/account/settings` → `src/pages/account/settings`

- 부품: PageContainer, 메뉴+콘텐츠, Form(antd) 기반 base/security/binding/notification
- ProForm 아님

### `/result/success`, `/result/fail`

- 부품: PageContainer, Result(antd), Card
- 결과 안내

### `/dashboard/analysis|monitor|workplace`

- 부품: PageContainer, Card, Charts(@ant-design/plots 등), 로컬 Charts 컴포넌트
- 모니터링·분석 대시보드

### `/user/login` → `src/pages/user/login`

- 부품: LoginForm / ProFormText / ProFormCaptcha / ProFormCheckbox (`@ant-design/pro-components`)
- layout: false 라우트

### `/user/register`, `/user/register-result`

- 등록 폼 + 결과 Result

## 라우트 원본

`config/routes.ts`: `/form/*`, `/list/table-list`→`./table-list`, `/list/basic-list`, `/list/card-list`, `/list/search/*`, `/profile/*`, `/account/*`, `/result/*`, `/dashboard/*`, `/user/login|register`.
