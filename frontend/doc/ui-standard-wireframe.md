# 화면 표준 (Ant Design Pro 템플릿 준용)

작성: 설계 담당(Claude) / 기준: 우리 저장소 `docs/template-reference/` (ant-design-pro v6.0.3 원본 복사본, 읽기 전용), 그 안의 `INDEX.md`, `MAPPING.md`, `COMPAT.md`.

사용자 결정(2026-10-29): 화면은 직접 설계하지 않고 템플릿(https://preview.pro.ant.design)의 화면 틀과 표준 CRUD 패턴을 준용한다. 이 문서는 "어떤 화면에 어떤 템플릿 패턴을 쓰는가"를 정한다. 새 화면은 이 문서를 먼저 따르고, 이 문서에 없는 모양이 필요하면 구현 전에 멈추고 질문으로 올린다.

## 1. 원칙

1. 템플릿(`@ant-design/pro-components`)에 부품이 있으면 직접 만들지 않는다. ProTable, ProForm, ModalForm, DrawerForm, ProFormList, ProDescriptions, ProCard, PageContainer, LoginForm 을 우선 쓴다.
2. 모든 화면은 `PageContainer`로 시작한다. 로그인, 비밀번호 변경, 지원 접속 진입·종료처럼 레이아웃 밖 화면은 예외다.
3. 템플릿에 대응이 없는 화면(조직도 트리, 권한 매트릭스, 공통코드 좌우 분할)은 antd 와 ProCard 로 구성하고, 구현 전에 와이어프레임을 사용자에게 확인받는다.
4. 템플릿 원본과 다르게 하는 부분은 4절 "의도한 차이"에만 둔다. 그 밖의 차이는 결함으로 본다.
5. 화면 단계는 마지막에 사용자가 실제 서버로 눈으로 확인한다. 자동 검증(lint, antd lint, tsc, test, build)만으로 승인하지 않는다.

## 2. 화면 유형과 쓰는 패턴

| 유형 | 쓰는 화면 | 템플릿 기준 | 구성 |
| --- | --- | --- | --- |
| A. 표준 CRUD 목록 | 테넌트 목록, 지원 언어, 회사, 사람, 사용자, 역할, 지원 접속 기록, (이후) 파일·팀·메뉴 | `table-list` | `PageContainer` + `ProTable`. 검색은 ProTable 내장 `search={{ labelWidth: 120 }}`(우리는 3~4개 항목만, 나머지는 숨김 열). `headerTitle`. `toolBarRender` 에 [등록] 버튼. `request={toTableRequest(service)}`. 상태·유형 열은 `valueEnum`. 작업 열은 `valueType: 'option'`. 이름 열은 링크 버튼으로 상세 Drawer 를 연다. 새로고침과 밀도·열 설정은 ProTable 기본을 그대로 둔다. |
| B. 간단 등록·수정 | 지원 언어, 역할 기본 정보, 코드 그룹 등 필드가 6개 이하인 것 | `CreateForm`(ModalForm) | `ModalForm` + `ProFormText`/`ProFormSelect`/..., 필드 `width="md"`, 모달 폭 400~520px, 트리거는 [등록] 버튼 또는 행의 [수정] 링크, 저장 성공 시 `actionRef.reload()` 와 성공 메시지. |
| C. 큰 등록·수정 | 회사, 사람, 사용자(필드 7개 이상 또는 탭이 있는 것) | `table-list` 의 Drawer + 템플릿 `UpdateForm` 의 구성 | `DrawerForm`(폭 600) 또는, 탭이 필요하면 Drawer 안 `Tabs`(1번 탭 기본 정보). 폼 부품은 ProForm 계열. |
| D. 전체 페이지 폼 | 테넌트 등록 | `form/basic-form`(+`advanced-form` 의 섹션 구성) | `PageContainer` + `Card variant="borderless"` + `ProForm`(`layout="vertical"`, 가운데 정렬, 최대 폭 720px, 필드 `width="md"`/`"xl"`). 섹션은 `ProForm.Group` 또는 `Card` 로 나눈다(기본 정보, 계약, 관리자, 도메인). 반복 입력은 `ProFormList`(최소 1줄, 줄마다 삭제 아이콘, 맨 아래 점선 [+ 추가] 버튼, 라벨은 첫 줄에만). 하단은 ProForm 기본 제출 영역: [등록] 주 버튼 + [취소]. |
| E. 상세 | 위 목록 화면의 상세 | `table-list` 의 Drawer + `ProDescriptions` | `Drawer`(size 600), `ProDescriptions column={2}`, 탭이 필요하면 `Tabs` 로 감싸고 첫 탭을 ProDescriptions 로. 수정·상태 변경 같은 작업 버튼은 Drawer 헤더 `extra` 에 둔다. |
| F. 삭제·상태 변경 | 모든 목록 | `Popconfirm`/모달 확인 | 우리는 논리삭제·복구를 쓰므로 행 작업 열에 [삭제]/[복구] 링크 + 확인창. 템플릿의 `FooterToolbar` 일괄 삭제는 일괄 동작이 필요한 화면에서만 쓴다(현재 없음). |
| G. 읽기 전용 기록 | 지원 접속 기록, (이후) 로그인 이력 | `table-list` 에서 쓰기 부분 제거 | ProTable(검색 포함, 툴바에 쓰기 버튼 없음) + 행 클릭 Drawer(ProDescriptions + 하위 목록 `Table`). |
| H. 결과·종료 | 지원 접속 종료, 오류 안내 | `result/success`, `result/fail`, `exception/*` | `Result`. |
| I. 로그인 | 로그인 | `user/login` | `LoginForm`/`ProFormText`(가능한 만큼. 서버 오류 표시와 언어 선택은 유지). 비밀번호 변경은 `form/basic-form` 모양(카드 + 세로 폼). |
| J. 대응 없는 화면 | 부서(조직도 트리 + 상세 탭), 역할 권한 매트릭스, 공통코드(그룹/코드 분할), 다국어(네임스페이스/메시지) | 없음 | `ProCard`(split) 또는 `Card` 로 영역을 나누고 각 영역은 위 부품을 쓴다. 와이어프레임을 사용자에게 먼저 확인받는다. |

## 3. 공통 규칙

- **메시지**: 성공·오류 토스트는 `App.useApp().message`. 서버 오류는 기존 `presentError` 규약(서버 `errorMessage` 우선)을 유지하고, 폼 필드 오류는 `fieldErrors` 를 폼 필드에 매핑한다. 한 화면에서 `Alert` 와 토스트가 같은 오류를 중복 표시하지 않는다.
- **페이징·정렬**: `src/core/request/table.ts` 의 `toTableRequest` 를 쓴다. 정렬은 서버 화이트리스트 열만 `sorter` 를 켠다.
- **권한**: `useAccess().can(menu, action)` 으로 [등록] 트리거, 행 작업 링크, 폼 제출을 숨기거나 비활성한다. 권한 이름을 화면에 흩뿌리지 않는 기존 규칙 유지.
- **다국어**: 모든 문구는 i18n 키. ProTable·ProForm 내장 문구(조회, 초기화, 새로고침, 밀도, 열 설정, 제출 등)도 한국어로 나오게 로케일을 확인한다.
- **입력칸 폭**: ProForm 필드는 `width` 속성(`xs/sm/md/lg/xl`)으로 정한다. 전체 폭으로 늘리지 않는다.
- **필수 표시**: 별표를 그대로 보인다(4절).
- **사용 여부 필터**: ProTable 검색 항목(`valueEnum` 전체/사용/중지)으로 둔다.
- **테스트**: ProTable·ProForm 은 목 없이 실제 antd/Pro 를 렌더한다. 서비스 호출만 목으로 한다. 레이아웃 전체를 목 없이 그리는 스모크 테스트를 유지한다(F12.1). 새 흐름 테스트는 3회 반복 통과를 기록한다.

## 4. 의도한 차이 (템플릿과 다르게 두는 것)

| 항목 | 템플릿 | 우리 | 이유 |
| --- | --- | --- | --- |
| 레이아웃 | `mix`(상단 메뉴+사이드) | `side` | F1 에서 합의한 좌측 메뉴 + 상단 헤더 |
| 헤더 고정 | `fixedHeader: false` | `true` | 지원 접속 띠, 사용자 메뉴가 항상 보여야 함 |
| 기본 로케일 | `zh-CN` | `ko-KR` | 한국어 서비스 |
| 필수 표시 | 숨김(`hideRequiredMark`) | 별표 표시 | 입력 오류 예방 |
| 삭제 | 선택 후 일괄 삭제 | 행 단위 논리삭제와 복구 | 데이터 정책 |
| 입력칸 스타일 | `variant: 'filled'` | 템플릿 그대로 | 변경하지 않는다 |
| 상세 | Drawer + ProDescriptions | 같음. 탭이 필요하면 Drawer 안에 `Tabs` | 템플릿에 탭 예시 없음 |

## 5. 이전(마이그레이션) 순서

한 번에 다 바꾸지 않고 유형별로 나누고, 단계마다 사용자가 화면을 눈으로 확인한다.

| 단계 | 내용 | 대상 |
| --- | --- | --- |
| F14 | 파일럿: 유형 A, D, G 한 번씩 + 공통 준비(로케일, 테스트 방법) | 테넌트 목록, 테넌트 등록, 지원 접속 기록(플랫폼·테넌트) |
| F15 | 유형 A, B, E | 지원 언어, 회사, 역할(기본 정보 부분은 유형 C) |
| F16 | 유형 A, C | 사람, 사용자 |
| F17 | 유형 J (와이어프레임 확인 필요) | 공통코드, 다국어, 부서, 역할 권한 매트릭스 |
| F18 | 유형 I, H, 테넌트 상세 | 로그인, 비밀번호 변경, 테넌트 상세 |

F14 가 승인되면 같은 방식으로 다음 단계를 진행한다. 이전 중에도 기존 기능(서버 호출, 권한, 오류 처리, 임시 비밀번호 모달 등)은 바꾸지 않고 화면 부품만 바꾼다.
