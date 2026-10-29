# 현재 화면 ↔ 템플릿 대응표

줄 번호는 조사 시점(2026-10-29) `main` 기준이다. 코드는 바꾸지 않고 읽기만 했다.

## 공통: pro-components 사용 현황

| 부품 | 우리 앱 | 템플릿 v6.0.3 |
| --- | --- | --- |
| package `@ant-design/pro-components` | `^3.1.14-2` (`frontend/package.json` 34) | `^3.1.14-2` (동일) |
| `antd` | `^6.6.0` | `^6.6.0` |
| ant-design-pro version | `6.0.3` | `6.0.3` |
| PageContainer | 대부분의 CRUD 화면 | 사용 |
| ProTable | 테넌트 목록만 | table-list 등 |
| ProForm / ModalForm / DrawerForm / StepsForm | **미사용** | 폼·CRUD에 사용 |
| ProDescriptions / FooterToolbar / ProList / ProCard | **미사용** | table-list 등 |

## 화면 대응

| 화면 | 파일 | 현재 부품 | 가장 가까운 템플릿 | 다른 점 | 이전 난이도 |
| --- | --- | --- | --- | --- | --- |
| 플랫폼 로그인 | `frontend/src/pages/user/login/index.tsx` | antd `Card`+`Form`+`Input`(L14–16, Card 영역 하단) | `/user/login` (LoginForm/ProFormText) | Pro LoginForm 없음, BrandMark·LanguageSelect 자체 구성 | 중 |
| 비밀번호 변경 | `frontend/src/pages/user/password/index.tsx` | antd Form | basic-form | ProForm 아님, 강제 변경 흐름 | 하 |
| 테넌트 목록 | `frontend/src/pages/platform/tenants/index.tsx` | PageContainer+ProTable(L90–107), `toTableRequest` | table-list | search 내장 없음(`search: false` 컬럼), FooterToolbar·상세 Drawer 없음, 행 클릭→상세 라우트 | 하 |
| 테넌트 등록 | `frontend/src/pages/platform/tenants/new.tsx` | PageContainer+antd Form(L90–91), Form.List 도메인 | basic-form / advanced-form | ProForm·maxWidth Card 없음, 도메인 Space/Form.List 가로 배치 | 중 |
| 테넌트 상세 | `frontend/src/pages/platform/tenants/detail.tsx` | PageContainer(L152), Tabs, antd Form/Table | profile/advanced | ProDescriptions 없음, 액션바·지원접속 모달 자체 | 중 |
| 지원 접속 기록(플랫폼) | `frontend/src/pages/platform/support-accesses/index.tsx` → `SupportAccessHistory` | PageContainer+Form 필터+antd Table(L200–280) | table-list | ProTable 아님, 읽기 전용, Drawer는 공용 컴포넌트 | 중 |
| 지원 접속 진입 | `frontend/src/pages/support-access/index.tsx` | Spin/Result | 없음(공개 교환) | 템플릿 대응 약함 | 하 |
| 지원 접속 종료 | `frontend/src/pages/support-access/ended.tsx` | Result | result/success\|fail | 유사 Result 패턴 | 하 |
| 공통코드 | `frontend/src/pages/mdm/common-code/index.tsx` | PageContainer(L363), antd Table×2(L416,510), Drawer 폼 | table-list + basic-form | 좌우 그룹/코드 분할, ProTable·ModalForm 없음 | 상 |
| 다국어 | `frontend/src/pages/mdm/i18n/index.tsx` | PageContainer(L433), Table, Drawer | table-list | 네임스페이스·메시지 이중 목록, Pro 폼 없음 | 상 |
| 지원 언어 | `frontend/src/pages/mdm/language/index.tsx` | PageContainer(L337), Table, Drawer | table-list | antd Table+DrawerForm 패턴(자체) | 중 |
| 회사 | `frontend/src/pages/org/company/index.tsx` | PageContainer(L222), Space 검색(L224), Table(L285), Drawer | table-list | 검색이 ProTable search가 아니라 Space+Button | 중 |
| 부서 | `frontend/src/pages/org/dept/index.tsx` | PageContainer(L330), Tree+Table, Drawer | 없음에 가까움 / basic-list | 조직도·기준일·이력, ProTable 부적합 | 상 |
| 사람 | `frontend/src/pages/org/person/index.tsx` | PageContainer(L198), Table, Drawer, Modal | table-list | 소속·이미지, antd Form | 상 |
| 역할 | `frontend/src/pages/access/role/index.tsx` | PageContainer(L210), Table, Drawer | table-list | 권한 매트릭스 자체 UI | 상 |
| 사용자 | `frontend/src/pages/access/user/index.tsx` | PageContainer(L354), Space 검색(L356), Table, Drawer, Modal | table-list | 임시비밀번호 모달·역할 탭, Pro ModalForm 없음 | 상 |
| 지원 접속 기록(테넌트) | `frontend/src/pages/access/support-access/index.tsx` | 공용 History(위와 동일) | table-list | 테넌트 필터 없음, 안내 문구 | 중 |
| 파일·팀·메뉴·권한 관리 화면 | — | 아직 없음 | table-list / advanced-form | 미구현 | — |

## 레이아웃·antd 설정 비교

| 항목 | 우리 (`frontend/config`) | 템플릿 |
| --- | --- | --- |
| layout | `side` (`defaultSettings.ts` 11) | `mix` |
| fixedHeader | `true` | `false` |
| contentWidth | `Fluid` | `Fluid` |
| antd variant | `filled` (`config.ts` 147) | `filled` |
| title | PMIS | Ant Design Pro |
