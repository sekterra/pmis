# F14 와이어프레임 (파일럿 세 화면)

기준: `ui-standard-wireframe.md` 유형 A·D·G, 템플릿 table-list / basic-form. 표준과 다른 점은 목록 DTO에 계약일·마이그레이션이 없어 해당 열을 넣지 않는 것뿐이다(질문으로 올리지 않음 — 기능·API 유지).

## 1. 테넌트 목록 (유형 A)

- PageContainer + ProTable `headerTitle` "테넌트 목록", `search={{ labelWidth: 120 }}`
- 검색: 코드(텍스트), 이름(텍스트) → API `keyword`로 합침, 상태(`valueEnum`, 서버 `status`)
- 툴바: [+ 테넌트 등록] → `/tenants/new`. 새로고침·밀도·열 설정은 ProTable 기본
- 열: 코드(링크 → 상세), 이름, 상태(valueEnum 색), 기본 언어, 대표 도메인, 생성일, 프로비저닝 단계. 정렬: 서버 화이트리스트만
- 행 전체 클릭 대신 코드 링크로 이동(템플릿 table-list와 동일)

## 2. 테넌트 등록 (유형 D)

- PageContainer 제목 "테넌트 등록" + Card `borderless` + ProForm vertical, maxWidth 720, 필드 md/xl
- 섹션: 기본 정보 / 계약 / 관리자 / 도메인
- 도메인 Form.List(ProForm 안): 최소 1줄, 세로 쌓임, 줄 삭제, 점선 [+ 도메인 추가]. (문자열 배열이라 ProFormList 대신 Form.List)
- submitter: [등록] + [취소](목록). 필수 별표 표시
- 성공 시 TemporaryPasswordModal → 닫기 → 상세. 검증·fieldErrors 기존 유지

## 3. 지원 접속 기록 (유형 G)

- 공용 SupportAccessHistory → ProTable. 플랫폼 검색: 테넌트, 운영자, 상태, 기간(dateRange→from/to). 테넌트 화면은 테넌트 검색 없음. 쓰기 버튼 없음
- 안내 문구 PageContainer content 또는 headerTitle 아래
- 행 링크 → Drawer size 600 + ProDescriptions column 2 + 쓰기 요청 antd Table(기존 effect)
