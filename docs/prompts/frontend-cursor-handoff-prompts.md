# Cursor AI 작업 지시서 (frontend)

이 파일은 Cursor AI가 읽고 수행하는 FE 작업 지시서이다.
사용자가 "이 파일의 FN단계를 수행해줘"라고 요청하면 **해당 단계 하나만** 수행한다. 다음 단계는 사용자가 요청할 때 진행한다.

## 공통 원칙 (모든 단계에 적용)
1. 저장소 루트 `CLAUDE.md`와 `.cursor/rules`를 먼저 읽고 따른다. 기준 문서는 `doc/fe-structure-proposal.md`(구조 합의 완료)이다.
2. 백엔드 API 명세(`backend/common/doc/api/mdm-api.md`, `platform-api.md`, `doc/auth-design.md`)가 기준이다. 명세에 없는 API/필드는 만들지 말고 질문 목록으로 정리한다.
3. 새 라이브러리 추가, 의존성 제거는 실행 전에 목록을 사용자에게 알리고 승인받는다.
4. 접속 정보, 토큰, 비밀 값, 서버 주소를 코드·문서·로그·채팅에 쓰지 않는다.
5. 되돌릴 수 없는 스크립트(`npm run simple`, `npm run i18n-remove`)는 실행하지 않는다.
6. 화면 구성(F2~)은 구현 전에 와이어프레임 수준으로 사용자 확인을 받는다.
7. **사용자가 요청하기 전에는 git 커밋·브랜치 생성·푸시를 하지 않는다.** 변경은 작업 트리에 그대로 두고, 사용자가 diff를 검토한 뒤 직접 커밋한다. (커밋을 요청받으면 Conventional Commits를 따른다.) 단계 끝에 `npm run lint`, `npx antd lint ./src`, `npm run tsc`, `npm run test`를 실행하고 결과를 기록한다. 실행하지 못한 검증은 이유를 적는다.
8. 단계가 끝나면 `doc/cursor-progress.md`(없으면 생성)에 아래 형식으로 **추가**하고 멈춘다.

```
## [FN단계] 단계 제목 - YYYY-MM-DD
- 변경/추가/삭제 파일: (목록. 삭제는 묶어서 요약)
- 검증 결과: (명령과 결과)
- 질문/확인 필요: (없으면 "없음")
- 가정한 사항: (없으면 "없음")
```

---

## F0단계: 템플릿 정리와 개발 환경

```
데모 템플릿을 정리하고 개발 환경을 맞춘다. 기능 구현은 하지 않는다.

0. 준비 (커밋/브랜치는 만들지 않는다)
   - 이 폴더가 git 저장소인지 `git status`로 확인한다.
     - git 저장소이면: 변경 상태만 기록하고 그대로 작업한다. 삭제는 git으로 되돌릴 수 있으므로 별도 백업은 필요 없다. 변경은 커밋하지 않고 작업 트리에 둔다.
     - git 저장소가 아니면: 삭제를 시작하기 전에 프로젝트 폴더 전체를 `node_modules` 제외하고 상위 폴더의 `frontend-backup-F0.zip`으로 압축해 두고, 위치를 보고한다. 이후에 진행한다.
   - 삭제 전에 현재 `src/pages`, `src/locales`, `src/services`, `config`, `mock`, 루트의 불필요 후보를 목록으로 만들어 진행 문서에 요약한다.
1. 화면/자원 삭제 (직접 삭제)
   - 삭제: 데모 페이지(dashboard, form, list, profile, result, account, chatbot, table-list, user/register*), 모든 `_mock.ts`와 `mock/`, `src/services/ant-design-pro/`(생성물), china-division 데이터, `DocLink`, `VersionDropdown`, `cloudflare-worker/`, `service-worker` 관련 파일과 등록 코드, 사용하지 않게 된 컴포넌트/스타일.
   - 유지: `exception/*`(403/404/500), 레이아웃 관련 컴포넌트, 로그인 화면(스타일 재사용 목적), `access.ts`, `app.tsx`의 구조.
   - 삭제 후 라우트(`config/routes.ts`)는 로그인, 환영/홈 빈 페이지, 예외 페이지만 남긴다. 컴파일이 되도록 참조를 정리한다.
2. 설정 정리
   - `config/config.ts`: Google Analytics(`analytics`) 제거, 기본 로케일 `ko-KR`, 불필요한 mock 설정 제거.
   - `src/locales/`: `ko-KR`, `en-US`만 남긴다(`ko-KR`은 새로 만든다. 남기는 키는 레이아웃·예외 페이지가 쓰는 것으로 한정하고 한국어 번역을 채운다). 다른 로케일 파일 삭제. 스크립트는 쓰지 않는다.
   - `package.json`: 쓰지 않게 된 의존성/스크립트(`simple`, `openapi`, `i18n-remove`, 챗봇용 `@ant-design/x*` 등)를 **목록으로 정리해 승인받은 뒤** 제거한다. 승인 전에는 삭제하지 않고 목록만 진행 문서에 둔다.
   - 빌드 모드: 환경변수 `APP_MODE`(`tenant` 기본, `platform`)를 읽어 `config`에서 사용할 수 있게 `define`으로 노출만 한다. 라우트 분기 구현은 하지 않는다(F1 이후).
3. 개발 프록시
   - `config/proxy.ts`의 `dev`를 `/api/mdm`, `/api/platform` 두 경로만 백엔드로 프록시하도록 바꾼다. 백엔드 주소는 환경변수(`DEV_BACKEND_URL`)로 받고 기본값을 코드에 쓰지 않는다.
   - 개발 전용으로 환경변수 `DEV_TENANT_CODE`가 있을 때만 프록시가 요청 헤더 `X-Tenant-Code`를 붙인다. 없으면 붙이지 않는다. 운영 빌드에는 영향이 없어야 한다.
   - `.env.example`(값 없이 항목만): `DEV_BACKEND_URL`, `DEV_TENANT_CODE`, `APP_MODE`. 실제 `.env*` 파일은 `.gitignore`에 있는지 확인한다.
   - `requestErrorConfig.ts`는 건드리지 않는다(F1에서 교체). 로그인 화면이 샘플 API를 부르는 것은 F1 전까지 그대로 둔다.
4. 문서
   - `README.md`의 데모/중국 서비스 안내 중 제거된 기능에 대한 부분을 짧게 정리하고, 실행 방법(`npm install`, `npm run dev`, 환경변수)을 적는다. 접속 정보는 쓰지 않는다.
5. 검증
   - `npm install`(의존성 제거가 승인된 경우만 변경 반영), `npm run lint`, `npx antd lint ./src`, `npm run tsc`, `npm run test`, `npm run build`가 통과해야 한다. 기존 테스트 중 삭제한 화면에 속한 것은 함께 삭제하고 목록에 적는다.
   - `npm run dev`로 기동해 로그인 화면과 404 화면이 뜨는지만 확인한다(백엔드 연동 없음). 포트 충돌이 있으면 포트를 바꿔 실행하고 설정에 하드코딩하지 않는다.

만들지 않는 것: 인증 로직, 요청 계층, i18n 서버 연동, 실제 화면. 모두 F1 이후다.
```

---

## F0.1단계: 불필요한 의존성·스크립트 제거 (사용자 승인 반영)

```
F0 진행 기록의 "승인 전이라 package.json에서 빼지 않음" 목록을 아래 결정대로 정리한다. 커밋은 하지 않는다. 시작 전 package.json과 package-lock.json을 상위 폴더 백업(frontend-backup-F0.1.zip, node_modules·.umi 제외)해 둔다.

제거 승인:
- 스크립트: simple, openapi, i18n-remove, record, start(mock 포함), deploy
- 의존성: @ant-design/x, @ant-design/x-markdown, @ant-design/x-sdk, china-division, d3, topojson-client, highlight.js
- 개발 의존성: @umijs/max-plugin-openapi, @umijs/request-record, mockjs, express, @types/express, swagger-ui-dist, gh-pages, geojson, @types/d3, @types/topojson-client, @types/topojson-specification
- 파일: config의 openAPI 플러그인 설정, config/oneapi.json, config/routes.simple.ts, scripts/simple.js 및 그 외 위 항목에만 쓰이던 스크립트

유지(제거 금지):
- @ant-design/plots (이후 PMIS 대시보드/차트에 쓸 가능성이 있어 유지), 스크립트 analyze
- 목록에 없는 모든 항목

절차:
1. 제거 전에 각 항목이 src, config, tests, scripts 어디에서도 import/참조되지 않는지 grep으로 확인하고 결과를 기록한다. 참조가 남은 항목은 제거하지 말고 목록으로 보고한다.
2. npm uninstall로 제거하고 package-lock.json을 갱신한다. 새 라이브러리는 추가하지 않는다.
3. 검증: npm run lint, npx antd lint ./src, npm run tsc, npm run test, npm run build, npm run dev 기동(로그인·404 화면 확인).
4. README에서 제거된 스크립트 안내가 있으면 정리한다.
5. `src/services/ant-design-pro/`의 샘플 API(currentUser, login, outLogin, getFakeCaptcha)와 requestErrorConfig.ts는 F1에서 교체하므로 그대로 둔다.

진행 기록은 [F0.1단계] 형식으로 추가하고 멈춘다.
```

---

## F1단계: 코어(요청·인증·다국어)와 레이아웃 셸

사용자와 합의된 사항(2026-10-11): 레이아웃은 좌측 사이드 메뉴 + 상단 헤더, 로그인은 중앙 카드 한 장, 라이트 테마만, 서비스명은 텍스트 `PMIS`(로고는 교체 가능한 컴포넌트 하나로 분리), 테넌트 사용자 앱(`APP_MODE=tenant`)만 구현한다. 플랫폼 콘솔은 F5이며 `core`가 모드를 받아들일 수 있는 구조만 갖춘다.

```
기준 문서: doc/fe-structure-proposal.md, 백엔드 doc/api/mdm-api.md(인증, 다국어 조회, 공통 규약), doc/auth-design.md. 명세에 없는 필드는 만들지 말고 질문으로 정리한다.

화면 구성(와이어프레임):
[로그인] 가운데 카드 한 장: 상단 'PMIS' 로고(컴포넌트), 아이디, 비밀번호, [로그인] 버튼, 카드 아래 언어 선택(Select). 휴대폰 로그인 탭, 소셜 로그인, 자동 로그인, 비밀번호 찾기 링크는 만들지 않는다.
[비밀번호 변경] 가운데 카드: 현재 비밀번호, 새 비밀번호, 새 비밀번호 확인(클라이언트에서 일치만 검사), [변경] 버튼. 임시 비밀번호로 로그인해 passwordChangeRequired=true이면 이 화면 외 이동을 막는다(자발적 변경은 헤더 사용자 메뉴에서 같은 화면으로 이동). 서버 정책 오류(AUTH.PASSWORD_POLICY 등)는 fieldErrors/errorMessage를 그대로 보여 준다.
[기본 레이아웃] 좌측 사이드 메뉴(접기 가능), 상단 헤더 오른쪽에 언어 선택과 사용자 메뉴(표시 이름, 비밀번호 변경, 로그아웃). 템플릿의 테마 설정 드로어(SettingDrawer), 알림, 검색, 다크 모드 토글은 제거한다. F1의 메뉴 항목은 정적 하나(환영 페이지)이다. 서버 메뉴 연동은 F4이므로 menus/my를 호출하지 않는다. 페이지 영역에는 /welcome 빈 페이지를 둔다.
예외 화면 403/404/500은 유지한다.

1. src/core/request
   - request 설정을 한 곳에서 한다(기존 requestErrorConfig.ts 교체). 응답 래퍼 {success,data,errorCode(문자열),errorMessage,showType,traceId,fieldErrors}를 처리한다. 성공이면 data만 화면에 넘기는 얇은 함수를 제공한다. bundles의 304/래퍼 없는 응답, jwks 같은 래퍼 없는 응답은 옵션으로 래퍼 처리를 끌 수 있게 한다.
   - showType: 0 무시, 1 경고, 2 오류, 3 알림, 9 로그인 이동. 오류 알림에 traceId가 있으면 함께 보여 준다. antd 정적 message/notification 대신 App 컨텍스트 방식을 쓴다(antd lint 경고 해소).
   - fieldErrors를 ProForm/Form의 필드 오류로 바꾸는 헬퍼. 목록 페이징 어댑터 toTableRequest(service)(ProTable current/pageSize/sorter -> page(1부터)/size/sort=필드,방향, 응답 data={items,total,page,size} -> {data,total,success}). 허용 정렬 필드는 서버가 검증하므로 FE는 그대로 전달만 한다.
   - 개발 프록시는 F0에서 끝났다. 코드에 서버 주소나 테넌트 코드를 넣지 않는다.
2. src/core/auth
   - Access 토큰은 모듈 내 메모리 변수에만 둔다(localStorage/sessionStorage/쿠키 직접 접근 금지). 요청마다 Authorization: Bearer를 붙인다(공개 경로 제외).
   - 앱 시작 시: POST /auth/refresh(헤더 X-MDM-Request: 1, 쿠키는 브라우저가 보냄)로 세션 복구를 한 번 시도한다. 성공하면 GET /auth/me로 initialState를 채운다. 실패(401)면 로그인 화면. 로그인/예외 화면에서는 복구 시도가 화면을 깜박이게 하지 않도록 로딩 상태를 둔다.
   - 로그인: POST /auth/login {loginId,password}. 성공 시 토큰 보관 -> passwordChangeRequired면 비밀번호 변경 화면, 아니면 me 조회 후 redirect 쿼리 또는 /welcome. 로그인 실패(401 AUTH.INVALID_CREDENTIALS)는 계정/비밀번호 구분 없는 단일 문구.
   - 401 AUTH.TOKEN_EXPIRED: refresh를 single-flight(동시에 하나만)로 호출하고 대기 중이던 요청을 새 토큰으로 한 번만 재시도한다(무한 재시도 금지). refresh가 409 AUTH.REFRESH_IN_PROGRESS면 300~1000ms 후 한 번 더 시도한다. refresh가 401(AUTH.TOKEN_REUSED 포함)이거나 재시도도 실패하면 메모리를 비우고 로그인으로 이동한다(원래 주소는 redirect로 보존, 외부 URL redirect는 거부해 열린 리다이렉트를 막는다).
   - 403 AUTH.PASSWORD_CHANGE_REQUIRED는 비밀번호 변경 화면으로 보낸다. 401 AUTH.ACCOUNT_DISABLED는 안내 후 로그아웃 처리한다.
   - 로그아웃: POST /auth/logout(헤더 X-MDM-Request: 1) 후 메모리 비움. BroadcastChannel로 다른 탭에 알려 같이 로그아웃한다(브라우저 기본 API만 사용, 라이브러리 추가 금지). 다른 탭에서 로그아웃 신호를 받은 탭은 서버 호출 없이 상태만 비운다.
   - 비밀번호 변경: POST /auth/password/change {currentPassword,newPassword}. 성공 응답의 새 Access로 교체하고 me를 다시 조회한다.
   - APP_MODE: 인증 모듈이 경로 접두사와 쿠키/토큰 구분을 설정으로 받게 한다(tenant: /api/mdm/v1/auth, platform: /api/platform/v1/auth). F1에서는 tenant만 연결한다.
3. src/core/i18n
   - src/locales의 ko-KR/en-US를 로컬 기본으로 하고, 서버 번들로 덮어쓴다(서버 > 로컬 > defaultMessage). 로그인 전: GET /i18n/languages(언어 목록·포맷)와 번들 common,error,auth를 받아 합친다(ETag/If-None-Match 사용, 304면 기존 값 유지). 로그인 후: 사용할 수 있는 나머지 네임스페이스는 F4에서 연결하므로 F1에서는 하지 않는다.
   - 언어 선택: 로그인 화면과 헤더에서 변경 가능. 변경 시 해당 언어 번들을 다시 받고 setLocale 한다. 선택한 로케일 코드(예 ko-KR)는 브라우저 저장소에 두어도 된다(토큰이 아님). 기본 언어는 서버의 defaultLanguage, 없으면 ko-KR.
   - 서버가 느리거나 실패해도 화면은 로컬 문구로 떠야 한다(번들 실패가 로그인을 막지 않는다).
   - antd 로케일과 날짜 포맷은 선택 언어에 맞춘다. 서버 응답의 dateFormat 등은 훅으로 노출만 하고 화면 적용은 이후 단계에서 한다.
4. 레이아웃 셸: 위 와이어프레임대로. 로고/서비스명은 하나의 컴포넌트로 분리해 이후 교체할 수 있게 한다. 로그인/비밀번호 변경 화면은 레이아웃 밖(전체 화면 카드)으로 렌더링한다. 라우트 가드: 로그인 안 한 상태에서 보호 경로 접근 시 /user/login?redirect=...
5. src/services/mdm/auth.ts, i18n.ts: 손으로 작성한 API 함수와 data.d.ts 타입. 템플릿의 src/services/ant-design-pro/ 샘플 API는 이 단계에서 삭제하고 참조를 모두 교체한다. @umijs/request-record와 login.test.tsx(@@/requestRecordMock 참조)도 함께 정리한다: 새 로그인 화면 테스트로 대체하고, request-record 패키지와 config의 requestRecord 설정을 제거한다(이 한 건은 승인된 것으로 본다).

테스트(vitest + Testing Library, 새 라이브러리 금지, fetch/네트워크는 모킹):
 (a) 래퍼 처리: success/실패, 문자열 errorCode, showType별 동작, traceId 표시, fieldErrors 매핑
 (b) 페이징 어댑터 변환(정렬 포함)
 (c) 401 TOKEN_EXPIRED -> 동시 요청 3개가 refresh 1번만 호출하고 모두 재시도 성공
 (d) refresh 409 -> 한 번 재시도 성공 / refresh 401 -> 로그인 이동, 재시도 무한 루프 없음
 (e) 앱 시작 세션 복구 성공/실패
 (f) 로그인 성공(passwordChangeRequired true/false 분기), 실패 문구
 (g) 로그아웃과 BroadcastChannel 수신 시 서버 호출 없이 상태 비움
 (h) 비밀번호 변경 강제 상태에서 다른 경로 이동 차단, 성공 후 토큰 교체
 (i) 번들 합치기 우선순위(서버 > 로컬 > defaultMessage), 304 처리, 번들 실패 시 로컬 문구 유지
 (j) 외부 URL redirect 거부
 (k) Access 토큰이 localStorage/sessionStorage에 저장되지 않는 것 확인

검증: npm run lint, npx antd lint ./src(requestErrorConfig 경고가 사라져야 한다), npm run tsc, npm run test, npm run build. 실제 백엔드 연동 수동 확인은 사용자가 한다(테넌트가 아직 없으면 사용자가 준비한다). 사용자가 확인할 수 있도록 doc/f1-manual-check.md에 확인 절차(환경변수 이름만, 값 없음, 로그인/강제 변경/세션 복구/다중 탭 로그아웃/언어 변경)를 정리한다.

만들지 않는 것: 서버 메뉴(F4), 권한 훅의 실제 구현(타입과 자리만), 공통코드·다국어 관리 화면, 플랫폼 콘솔, 다크 모드, 소셜/휴대폰 로그인.
진행 기록은 [F1단계] 형식으로 추가하고 멈춘다.
```

---

## F2단계: 플랫폼 콘솔 (운영자 로그인, 테넌트 관리)

사용자와 합의된 사항(2026-10-11): 테넌트 등록은 Postman이 아니라 이 콘솔에서 한다. 같은 저장소의 `APP_MODE=platform` 빌드로 만든다. 등록 폼은 별도 페이지 한 장. 범위는 목록, 등록, 상세, 중지/재개, 재시도, 관리자 임시 비밀번호 재발급, 도메인 추가/삭제이다. 스키마 업그레이드와 마이그레이션 이력 화면은 이후 단계이다.

```
기준 문서: 백엔드 doc/api/platform-api.md(전체), doc/auth-design.md의 플랫폼 운영자 부분, F1에서 만든 src/core. 이 문서에 없는 응답 필드는 backend/common의 Controller/DTO 소스를 읽기 전용으로 확인하고, 문서와 다르면 임의로 맞추지 말고 질문으로 보고한다. 백엔드 코드는 수정하지 않는다.

화면 구성(와이어프레임):
[플랫폼 로그인] 테넌트 로그인과 같은 중앙 카드. 제목은 'PMIS 플랫폼 콘솔'. 아이디, 비밀번호, 로그인. 언어 선택은 ko-KR/en-US 로컬 두 개.
[비밀번호 변경] 플랫폼 운영자용 같은 카드 화면. passwordChangeRequired면 이 화면만 열린다. 플랫폼 정책은 비밀번호 12자 이상, 문자 종류 3종 이상이고 로그인 아이디를 포함할 수 없다(서버가 검증, FE는 서버 오류를 필드에 표시). 필드 이름: currentPassword, newPassword(플랫폼 API 문서를 확인).
[레이아웃] F1과 같은 좌측 메뉴+상단 헤더. 메뉴는 정적 하나: '테넌트 관리'. 헤더에 언어 선택과 사용자 메뉴(운영자 이름, 비밀번호 변경, 로그아웃).
[테넌트 목록 /tenants] 검색(키워드, 상태 선택) + [테넌트 등록] 버튼. 표 컬럼: 코드, 이름, 상태, 프로비저닝 단계, 기본 도메인, 생성일. 서버 페이징(F1의 toTableRequest). 정렬은 서버 허용 필드(tenantCode, tenantName, tenantStatusCode, createDatetime)만. 행 클릭 시 상세.
[테넌트 등록 /tenants/new] 한 페이지 폼: 테넌트 코드, 테넌트 이름, 기본 언어(지원 언어 선택), 시간대(IANA 목록 선택, 기본 Asia/Seoul), 계약 시작일, 계약 종료일(선택), 관리자 이름, 관리자 이메일, 도메인(1개 이상, 추가/삭제, 소문자 호스트명). 스키마 이름은 입력받지 않는다. 등록하면 프로비저닝이 끝날 때까지 로딩을 보여 주고, 성공하면 '임시 비밀번호' 모달을 띄운 뒤 닫으면 상세로 이동한다.
[임시 비밀번호 모달] 로그인 아이디(관리자)와 임시 비밀번호를 한 번만 표시하고 복사 버튼을 둔다. '지금 복사하지 않으면 다시 볼 수 없습니다' 안내. 닫으면 값을 메모리에서도 즉시 지운다. 재발급에도 같은 모달을 쓴다.
[테넌트 상세 /tenants/:tenantCode] 상단: 코드, 이름, 상태 태그, 작업 버튼. 탭 두 개: 기본 정보(이름, 기본 언어, 시간대, 계약기간, 프로비저닝 단계 6단계를 Steps로: REQUESTED, SCHEMA_CREATED, MIGRATED, SEEDED, ADMIN_CREATED, COMPLETED, 실패 단계 표시, 마지막 마이그레이션), 도메인(목록, primary 표시, 추가, 삭제).
작업 버튼 규칙(상태에 따라 활성/숨김): 중지(ACTIVE만, 사유 changeReason 필수 입력 모달), 재개(SUSPENDED만), 프로비저닝 재시도(FAILED 또는 PROVISIONING만, 확인 대화상자), 관리자 임시 비밀번호 재발급(확인 대화상자). 서버가 409 TENANT.INVALID_STATUS_TRANSITION 등을 돌려주면 서버 메시지를 그대로 보여 준다. 마지막 활성 도메인 삭제는 서버가 409 TENANT.LAST_DOMAIN으로 막는다.

구현 사항:
1. APP_MODE 분기: platform 빌드에는 platform 라우트·화면·서비스만 포함하고 tenant 화면(로그인, 환영 등)은 포함하지 않는다. tenant 빌드에는 platform 화면이 들어가지 않는다. 실행 스크립트를 추가한다(예: dev:platform, build:platform). 환경변수 설정은 이미 쓰는 도구로 하고, Windows에서 동작해야 한다. 새 라이브러리가 필요하면 먼저 알린다.
2. 인증: F1 core/auth를 그대로 쓰되 platform 모드 설정을 사용한다(경로 /api/platform/v1/auth, 쿠키 mdm_platform_refresh). 플랫폼 요청에는 X-Tenant-Code 헤더를 붙이지 않는다(개발 프록시가 DEV_TENANT_CODE가 있어도 /api/platform 경로에는 붙이지 않도록 수정). 세션 복구, refresh 규칙, 강제 비밀번호 변경, 다중 탭 로그아웃은 F1과 동일하게 동작해야 한다. 플랫폼 me 필드: platformAdminPk, loginId, adminName, email, passwordChangeRequired.
3. i18n: 플랫폼은 서버 번들이 없다(테넌트 없음). platform 모드에서는 서버 번들/언어 목록 호출을 하지 않고 로컬 ko-KR/en-US만 쓴다. 플랫폼 화면 문구는 locales에 추가한다. 서버 오류 메시지는 Accept-Language에 따라 서버가 한글/영문으로 준다.
4. src/services/platform/(auth.ts, tenants.ts, data.d.ts): 손으로 작성한 API 클라이언트. 목록 API는 페이지 객체, 상세/도메인 응답은 문서를 따른다.
5. 임시 비밀번호는 컴포넌트 지역 상태에만 둔다. URL, 라우트 상태, initialState, 전역 모델, console, 로그, localStorage/sessionStorage, 에러 리포터에 넣지 않는다. 모달이 닫히면 지운다. 응답을 로깅하는 코드가 있으면 이 경로는 제외한다. 이 응답은 Cache-Control: no-store이다.
6. 유효성(FE 1차): 테넌트 코드(서버 규칙을 확인해 같은 패턴), 이메일 형식, 도메인 소문자 호스트명, 계약 종료일>=시작일. 서버 fieldErrors는 해당 필드에 표시한다. 중복은 409 TENANT.DUPLICATE.
7. 테스트(vitest + Testing Library, 네트워크 모킹): 
 (a) 목록 조회 파라미터 변환(page/size/sort/status/keyword)
 (b) 등록 폼: 필수값/도메인 추가·삭제, 제출 페이로드(스키마 이름 없음), fieldErrors 표시
 (c) 등록·재발급 응답의 임시 비밀번호가 모달에서만 보이고 닫으면 사라지며 어떤 저장소/URL/전역 상태에도 남지 않음
 (d) 상태별 작업 버튼 활성 규칙(중지/재개/재시도/재발급), 중지는 changeReason 없으면 제출 불가
 (e) 도메인 삭제 409 LAST_DOMAIN 처리, 추가 409 DUPLICATE 처리
 (f) platform 모드 인증 경로/쿠키 설정과 X-Tenant-Code 미부착
 (g) platform 모드에서 서버 번들 호출을 하지 않음
 (h) 빌드 분리: tenant 빌드 산출물에 platform 화면 고유 문자열이 없고 platform 빌드에 tenant 화면 고유 문자열이 없음(빌드 후 산출물 검색으로 확인하고 방법을 기록)
8. 검증: npm run lint, npx antd lint ./src, npm run tsc, npm run test, npm run build, npm run build:platform(스크립트 이름은 정한 대로). 사용자가 확인할 수 있도록 doc/f2-manual-check.md에 확인 절차를 쓴다(환경변수 이름만, 값 없음): 플랫폼 로그인 -> 강제 비밀번호 변경 -> 테넌트 등록(임시 비밀번호 모달) -> 상세/도메인 -> 중지/재개 -> 재발급, 그리고 등록한 테넌트로 테넌트 앱 로그인(F1 확인: 강제 변경, 세션 복구, 다중 탭 로그아웃, 언어 변경).

만들지 않는 것: 스키마 업그레이드/마이그레이션 이력 화면, 테넌트 폐기(TERMINATED), 도메인 소유 검증, 운영자(platform_admin) 관리 화면, 다크 모드.
진행 기록은 [F2단계] 형식으로 추가하고 멈춘다.
```

---

## F2.1단계: 비밀번호 변경 화면에서 나가기 (완료·승인됨)

```
강제/자발 비밀번호 변경 화면(src/pages/user/password, src/pages/platform/password)에서 빠져나갈 수 있게 한다.
1. 카드 아래에 '다른 계정으로 로그인'(로그아웃): POST /auth/logout(헤더 X-MDM-Request: 1) 후 메모리 비우고 BroadcastChannel로 다른 탭에 알린 뒤 로그인 화면으로 이동. 서버 호출이 실패해도 로컬 상태는 비우고 로그인으로 이동.
2. passwordChangeRequired=false(자발 변경)일 때만 '취소'(이전 화면, 없으면 홈).
3. 변경 성공은 새 토큰으로 교체 후 홈, 실패는 서버 오류 문구를 화면에 표시.
4. 테스트: 로그아웃 이동, 서버 오류에도 로컬 상태 비움, 강제 변경 상태에서도 로그아웃 경로 허용.
```

---

## F2.2단계: 백엔드 8.6 응답 반영 (관리자 로그인 아이디, 목록 열)

```
백엔드 8.6에서 플랫폼 API 응답이 늘었다. 백엔드 doc/api/platform-api.md(갱신됨)를 기준으로 플랫폼 콘솔을 맞춘다. 백엔드 코드는 수정하지 않는다.

1. 관리자 로그인 아이디
   - 테넌트 생성, 프로비저닝 재시도, 관리자 비밀번호 재발급 응답에 `adminLoginId`가 추가됐다. 임시 비밀번호 모달은 이 값을 그대로 보여 준다. 입력한 이메일을 소문자로 바꿔 아이디로 추정하는 코드는 제거한다.
   - 재발급 모달도 아이디와 임시 비밀번호를 함께 보여 준다. 임시 비밀번호가 없는 응답(이미 관리자 계정이 있어 새 비밀번호를 만들지 않은 경우)이면 비밀번호 칸 대신 안내 문구를 보여 주고 아이디만 표시한다.
   - 임시 비밀번호 취급 규칙(모달 안에서만 보임, 닫으면 지움, 저장소/URL/전역 상태/로그 금지)은 그대로 유지한다. adminLoginId는 비밀이 아니므로 모달에서 복사 버튼을 둘 수 있다.
2. 목록 열
   - GET /tenants 항목의 `provisioningStatus`, `primaryDomain`, `createDatetime`(UTC ISO, 모두 null일 수 있음)을 타입(src/services/platform/data.d.ts)에 추가하고 표에 열로 넣는다: 코드, 이름, 상태, 프로비저닝 단계(단계 코드를 사람이 읽는 문구와 태그 색으로, FAILED 단계는 강조), 기본 도메인, 생성일(브라우저 로컬 시간대 yyyy-MM-dd HH:mm, 값이 없으면 '-').
   - 정렬은 서버 허용 필드(tenantCode, tenantName, tenantStatusCode, createDatetime)만 헤더 정렬로 열어 둔다. 기본 정렬은 서버 기본을 따른다.
   - 알 수 없는 단계 코드가 와도 화면이 깨지지 않고 코드 값을 그대로 보여 준다.
3. 테스트
   - 생성/재발급 응답의 adminLoginId가 모달에 그대로 표시되고 이메일 추정 코드가 없음, 임시 비밀번호가 null일 때의 모달, 목록 새 열 렌더링(null 값 포함), 알 수 없는 단계 코드, 정렬 파라미터. 기존 비밀 취급 테스트가 계속 통과해야 한다.
4. doc/f2-manual-check.md의 해당 항목을 갱신한다. 개발 서버를 띄웠다면 확인 후 반드시 종료한다.
검증은 lint, antd lint, tsc, test, build, build:platform. 진행 기록은 [F2.2단계] 형식으로 추가하고 멈춘다.
```

---

## F2.3단계: 프런트 로컬 개발 설정(.env.local, 포트 고정)

사용자 결정(2026-10-11): 백엔드의 local 프로필처럼 프런트도 로컬 설정 파일로 개발 서버 주소를 받아, 매번 환경변수를 입력하지 않게 한다. 두 앱(테넌트 앱, 플랫폼 콘솔)을 동시에 띄울 수 있게 포트를 모드별로 고정한다.

```
코드는 최소로 바꾼다. 서버 주소 외 비밀 값은 다루지 않는다.

1. 로컬 설정 파일
   - Umi가 자동으로 읽는 `frontend/.env.local`(저장소 제외)을 로컬 설정 파일로 쓴다. 지금 읽는 변수 이름 그대로 `DEV_BACKEND_URL`, `DEV_TENANT_CODE`를 지원한다. 셸 환경변수가 있으면 파일보다 우선하는 Umi의 기본 동작을 확인하고 그대로 둔다(확인 결과를 기록).
   - `.env.local`이 `.gitignore`에 포함되어 있는지 확인하고(없으면 추가), `.env.local.example`(값 없이 항목, 주석, 자리표시자)을 만든다. 기존 `.env.example`과 중복이면 하나로 합치고 이유를 적는다.
   - 파일은 사용자가 만든다. 너는 `.env.local`을 읽거나 수정할 수 있지만(사용자가 허용), 값을 진행 기록, 커밋, 예시 파일, 채팅에 복사하지 않는다. 비밀번호, 토큰, 계정 같은 비밀을 이 파일에 쓰도록 안내하지 않는다(주의 문구를 예시 파일과 문서에 넣는다).
2. 모드별 포트 고정
   - 개발 서버 기본 포트를 테넌트 앱(`npm run dev`)은 8000, 플랫폼 콘솔(`npm run dev:platform`)은 8002로 고정한다. Windows에서 동작해야 하고, 이미 프로젝트에 있는 도구(cross-env 등)만 쓴다. 새 라이브러리가 필요하면 먼저 알린다.
   - 그 포트가 이미 사용 중일 때 Umi가 조용히 다른 포트로 바꾸거나 다른 서버로 요청이 가는 일이 없는지 확인한다. 포트 충돌 시 "이미 다른 개발 서버가 떠 있을 수 있습니다. 8000/8002 포트를 확인하세요" 같은 이유를 알려 주는 방식(실패 처리 또는 명확한 안내)으로 정한다. 방법을 정해 보고한다.
   - 설정 파일(config)에 포트를 하드코딩하지 말고 스크립트/환경으로 준다. 사용자가 `PORT`를 직접 주면 그 값이 우선한다.
3. 안내
   - DEV_BACKEND_URL이 비어 있으면 개발 서버 시작 시(또는 첫 /api 호출 시) "백엔드 주소가 없어 /api 요청이 프록시되지 않습니다. frontend/.env.local에 DEV_BACKEND_URL을 설정하세요" 안내를 콘솔에 한 줄 출력한다. (지금은 조용히 404가 나서 원인을 찾기 어렵다.)
   - README와 `doc/local-setup.md`(없으면 생성)에: 1) .env.local 만드는 법 2) 두 앱을 동시에 띄우는 법(포트) 3) PowerShell에서 `npm`이 실행 정책에 막히면 `npm.cmd` 또는 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` 4) 백엔드를 먼저 띄운다(bootRun, local 프로필) 5) 예전에 띄운 개발 서버가 포트를 잡고 있으면 먼저 종료한다. 값은 어디에도 적지 않는다.
4. 테스트: 프록시 설정 함수가 DEV_BACKEND_URL 유무에 따라 규칙을 만들고 비우는지, DEV_TENANT_CODE가 /api/mdm에만 헤더를 붙이고 /api/platform에는 붙이지 않는지(이미 있으면 유지), 안내 문구 출력 조건. 기존 테스트가 통과해야 한다.
5. 검증: lint, antd lint, tsc, test, build, build:platform. 개발 서버를 띄워 확인했다면 반드시 종료한다.
진행 기록은 [F2.3단계] 형식으로 추가하고 멈춘다.
```

---

## F3단계: 공통코드 관리 화면 (코드그룹 / 공통코드)

F2.3 승인. 사용자 결정(2026-10-11): 백엔드 공통코드 관리 API는 7단계에서 완료돼 있어 바로 화면을 만든다. 레이아웃은 좌우 분할, 다국어 이름은 폼 안 탭, 변경 이력은 Drawer, 계층형 그룹은 트리 테이블로 한다. 대상은 테넌트 앱(`APP_MODE=tenant`)이며 플랫폼 콘솔에는 넣지 않는다.

```
기준 문서: backend/common/doc/api/mdm-api.md 의 code-groups, common-codes, history 항목과 codes 읽기 API. 명세에 없는 필드·동작은 만들지 말고 질문 목록에 올린다. 시작 전에 아래 화면 구성을 와이어프레임 수준(텍스트 도식)으로 doc/f3-wireframe.md 에 먼저 쓰고, 기존 승인 범위와 다른 점이 있으면 구현 전에 사용자에게 알린다.

1. 라우트와 메뉴
   - 경로 /mdm/common-code. 화면 제목, 컬럼, 버튼, 오류 문구는 모두 i18n 키로 만든다(로컬 번들 ko-KR/en-US 에 추가. 서버 번들 연동은 F4/F5 범위).
   - 권한: 메뉴 코드 COMMON_CODE, 동작 READ/CREATE/UPDATE/DELETE. 기존 `useAccess().can(menuCode, action)` 가 이미 있으면 그것을 쓰고, 아직 서버 메뉴(menus/my)와 연결되기 전이라 임시 구현이면 그 사실을 보고한다. 권한 없는 버튼은 숨기고, 서버가 403(AUTH.FORBIDDEN)을 주면 안내 메시지를 보여 준다. 임시 우회 코드(항상 true)는 F5 에서 제거할 수 있도록 한 곳에만 둔다.

2. 화면 구성: 좌우 분할
   - 왼쪽(코드그룹 패널): 검색(keyword), 솔루션 코드 필터, 사용 여부 필터, 목록(페이지 크기 20, 서버 페이징·정렬 화이트리스트는 명세 따름), [그룹 등록] 버튼. 행을 선택하면 오른쪽이 그 그룹의 코드를 보여 준다. 선택 그룹은 URL 쿼리(?group=코드)에 반영해 새로고침해도 유지한다. 사용 중지 그룹은 흐리게 표시하고 [복구] 동작을 준다.
   - 오른쪽(코드 패널): 선택한 그룹 정보 요약(코드, 이름, 계층형 여부, 코드 값 길이), 검색(keyword), 사용 여부 필터, [코드 등록] 버튼, 코드 표. 그룹을 선택하기 전에는 안내 문구(빈 상태)를 보여 준다.
   - 계층형 그룹(hierarchy_yn='Y'): 코드 표를 트리 테이블로 표시한다. 서버 목록이 평면(상위 코드 값 포함)이면 FE 에서 트리로 조립한다. 부모 없이 남은 행(필터 결과)은 최상위로 보여 준다. 비계층 그룹은 일반 표.
   - 반응형: 좁은 화면에서는 위아래로 쌓는다.

3. 등록/수정: 우측 Drawer 폼
   - 그룹 폼 탭 [기본 정보] [언어별 이름]. 기본 정보: 그룹 코드(등록 후 수정 불가면 수정 시 읽기 전용, 명세 따름), 솔루션 코드, 이름, 설명, 계층형 여부, 코드 값 길이, 정렬 순서. 언어별 이름 탭: 지원 언어(GET /i18n/languages)를 행으로 보여 주고 언어별 이름·설명 입력. 비워 둔 언어는 요청에 포함하지 않는다(서버가 요청에 없는 언어는 유지한다).
   - 코드 폼 탭 [기본 정보] [언어별 이름]. 기본 정보: 코드 값(등록 후 수정 가능 여부는 명세 따름), 기본 이름, 상위 코드(계층형 그룹일 때만, 같은 그룹의 코드에서 선택, 자기 자신과 하위는 선택 불가), 유효 시작일/종료일(시작≤종료 검증), 정렬 순서, 확장 속성 1~5, 변경 사유(선택). 코드 값 길이 제한이 있으면 입력 검증에 반영.
   - 서버 검증 오류는 400 fieldErrors 를 해당 입력 아래에 표시한다. 409 CODE.DUPLICATE / CODE.GROUP_IN_USE / 400 CODE.INVALID_POLICY_VALUE 등 명세의 오류 코드별로 사용자가 이해할 문구를 보여 준다(서버 errorMessage 우선, 없으면 로컬 문구).

4. 삭제와 복구
   - [삭제]는 논리 삭제(use_yn='N')임을 확인창에 명시한다. 그룹에 사용 중인 코드가 있어 409 CODE.GROUP_IN_USE 가 오면 이유를 보여 준다. 사용 중지 행에는 [복구]. 실행 후 목록을 다시 불러온다.

5. 변경 이력 Drawer
   - 코드 행의 [이력] 버튼 → Drawer 에 GET /common-codes/{codePk}/history 를 서버 페이징으로 표시(변경 시각, 변경 유형 C/U/D 를 문구로, 변경 사유, 변경 사용자 pk). 변경 전/후 JSON 은 행을 펼치면 읽기 쉬운 key-value 비교로 보여 준다(새 라이브러리 없이 antd 컴포넌트로). 변경 사용자 이름 조회 API 가 없으므로 pk 만 표시한다.

6. 데이터 처리 규칙
   - 서비스 계층은 src/services/mdm/ 에 만들고(code-groups, common-codes, history, languages), 타입은 명세와 일치시킨다. 목록 응답은 PageResponse(items,total,page,size)로 받고 page 는 1부터다. 컴포넌트에서 fetch/axios 를 직접 쓰지 않는다(기존 src/core/request 사용).
   - 동시 요청/연타 방지(저장 중 버튼 비활성), 목록 새로고침 시 선택 그룹 유지, 언어 목록은 화면 진입 시 1회 로드 후 재사용.
   - 새 라이브러리는 추가하지 않는다. 필요해 보이면 먼저 알린다.

7. 테스트(vitest): 트리 조립 함수(부모 누락 처리 포함), 언어별 이름 폼 → 요청 본문 변환(빈 언어 제외), 오류 코드 → 문구 매핑, 서비스 계층의 쿼리 파라미터 생성(page/size/sort). 컴포넌트 테스트는 핵심 흐름만(그룹 선택 → 코드 목록 호출).
8. 검증: lint, antd lint, tsc, test, build, build:platform(플랫폼 빌드에 이 화면이 섞이지 않는지 확인). 개발 서버를 띄워 확인했다면 반드시 종료한다. 수동 확인 절차(등록/수정/삭제/복구/이력/언어별 이름, 계층형 그룹 트리)를 doc/f3-manual-check.md 에 쓴다. 값(주소, 계정, 비밀번호)은 쓰지 않는다.
진행 기록은 [F3단계] 형식으로 추가하고 멈춘다.
```

---

## F3.1단계: 공통코드 화면 흐름 테스트 보강 (목 기반)

F3 승인. 사용자 결정(2026-10-12): 수동 확인(doc/f3-manual-check.md) 중 백엔드 없이 가능한 항목을 자동 테스트로 옮겨, 사용자는 실제 서버 연결 부분만 확인한다. 화면 기능은 바꾸지 않는다.

```
목적은 테스트 보강이다. 테스트로 버그가 발견되면 최소한으로 고치고 무엇을 왜 고쳤는지 기록한다. 기능 추가, 구조 변경, 새 라이브러리 추가는 하지 않는다(필요해 보이면 먼저 알린다). 서버 주소, 계정, 비밀번호는 코드, 테스트, 기록 어디에도 쓰지 않는다. 개발 서버와 실제 백엔드는 띄우지 않는다.

1. 방식
   - vitest + 기존 테스트 도구(Testing Library 등 이미 있는 것)만 쓴다. 서비스 계층(src/services/mdm)을 모듈 목으로 대체하거나 요청 계층을 목으로 대체한다. 어느 쪽이든 실제 네트워크 호출이 0건임을 테스트가 보장한다.
   - 기존 page.test.tsx 의 목 방식과 일관되게 쓰고, 공통 목 데이터(그룹, 코드, 언어, 이력)는 한 파일로 모은다.
   - 시간, 날짜는 고정하고, 비동기는 waitFor/findBy 로 기다린다(임의 sleep 금지). 테스트끼리 상태를 공유하지 않는다.

2. 수동 확인 항목별 자동화 (doc/f3-manual-check.md 번호 기준)
   - 1: 라우트 진입 시 그룹 미선택 안내가 보이고 코드 목록 호출이 없다.
   - 2: 검색어, 솔루션 코드, 사용 여부 조회 시 올바른 쿼리(page=1, size=20, keyword, solutionCode, useYn)로 호출된다. 사용 중지 그룹 행에 [복구]가 보이고 [삭제]는 없다.
   - 3: 그룹 행 선택 시 URL 쿼리 ?group= 이 바뀌고, ?group= 이 이미 있는 상태로 진입(새로고침과 동일)해도 그 그룹의 코드가 조회된다.
   - 4: 그룹 등록 Drawer 에서 기본 정보와 언어별 이름 입력 후 저장 시 요청 본문이 맞고(이름이 빈 언어 제외), 성공 후 목록을 다시 부른다. 수정 Drawer 에서 그룹 코드 입력이 읽기 전용이다. 저장 중 저장 버튼이 비활성이다(연타해도 요청 1회).
   - 5: 코드 등록/수정 Drawer 의 본문(코드 값, 날짜, 정렬, 확장 속성 1~5, 변경 사유, 언어별 이름). 종료일이 시작일보다 빠르면 저장되지 않고 오류가 보인다. 코드 값 길이 제한이 있는 그룹에서 초과 입력은 저장되지 않는다.
   - 6: 계층형 그룹에서 트리로 렌더링되고, 부모가 없는 행은 최상위에 나온다. 상위 코드 선택지에서 자기 자신과 하위 코드가 빠진다. 비계층 그룹은 상위 코드 입력이 없다.
   - 7: [삭제] 확인창에 논리 삭제(use_yn=N) 문구가 있고, 취소하면 호출이 없다. 확인하면 삭제 호출 후 목록을 다시 부른다. 서버 409 CODE.GROUP_IN_USE 응답이면 서버 문구(없으면 로컬 문구)가 보인다. [복구] 후 목록이 다시 불러와지고 선택한 그룹이 유지된다.
   - 8: [이력] 에서 시각, 유형 문구(C/U/D), 사유, 사용자 pk 가 보이고, 행을 펼치면 변경 전/후 key-value 가 보인다. 이력 목록은 서버 페이징 파라미터로 호출된다.
   - 오류 경로: 400 fieldErrors 가 해당 입력 아래에 표시된다. 403 AUTH.FORBIDDEN 이면 권한 안내가 보이고 화면이 깨지지 않는다. 목록 호출 실패 시 오류 안내가 보이고 이전 선택이 사라지지 않는다.
   - 9(좁은 화면 배치)와 10(플랫폼 메뉴에 없음): 9 는 자동화하지 않는다(사용자 확인). 10 은 두 모드의 라우트 설정에 공통코드 경로가 각각 있고 없음을 확인하는 테스트로 대체하되, build:platform 결과 확인으로도 이미 검증됐다면 중복하지 않고 그 사실을 기록한다.

3. 문서 갱신
   - doc/f3-manual-check.md 의 각 항목 끝에 "자동 테스트: <파일>의 <테스트 이름>" 또는 "직접 확인 필요(이유)"를 표시한다. 사용자가 직접 볼 항목만 맨 위에 짧게 다시 모은다(예상: 실제 백엔드 저장 확인, 서버 오류 문구, 9번 좁은 화면, 전체 사용감). 값은 쓰지 않는다.

4. 검증: lint, antd lint, tsc, test(실행 시간과 전체 통과 수), build, build:platform. 테스트가 불안정(flaky)하지 않은지 새 테스트 파일을 3회 반복 실행해 모두 통과함을 기록한다.
진행 기록은 [F3.1단계] 형식으로 추가하고 멈춘다. 변경 파일 목록에 소스(비테스트) 수정이 있으면 따로 표시하고 이유를 쓴다.
```

---

## F4단계: 다국어 관리 화면 (네임스페이스 / 메시지 번역)

F3.1 승인. 사용자 결정(2026-10-14): F4 범위는 네임스페이스와 메시지 번역만이다. 지원 언어·국가 관리 화면은 F4.1로 분리한다. 레이아웃은 공통코드 화면과 같은 좌우 분할(왼쪽 네임스페이스, 오른쪽 메시지 표), 번역 입력은 Drawer. 대상은 테넌트 앱이며 플랫폼 콘솔에는 넣지 않는다.

```
기준 문서: backend/common/doc/api/mdm-api.md 의 i18n-namespaces, i18n-messages, i18n/languages 항목. 명세에 없는 필드·동작은 만들지 말고 질문 목록에 올린다. 구현 전에 화면 구성을 doc/f4-wireframe.md 에 텍스트 도식으로 먼저 쓰고, 승인된 범위와 다른 점이 있으면 구현 전에 알린다. F3 의 구조(src/pages/mdm/common-code/, services/mdm, rules, fixtures)와 같은 패턴을 따르고 공통 부분은 새로 만들지 말고 재사용한다(중복이 크면 어떻게 합칠지 보고하고 최소한만 정리).

1. 라우트와 메뉴
   - 경로 /mdm/i18n. 제목, 열, 버튼, 오류 문구는 모두 i18n 키(ko-KR/en-US 로컬 번들)로 만든다.
   - 권한: 메뉴 코드 I18N, 동작 READ/CREATE/UPDATE/DELETE. F3 의 임시 권한 한 곳(permission.ts 방식)을 같은 방식으로 쓰고, F5 에서 함께 교체할 수 있게 한 곳에만 둔다. 서버 403 AUTH.FORBIDDEN 은 안내로 보여 준다.

2. 화면: 좌우 분할
   - 왼쪽(네임스페이스): 검색(keyword), 사용 여부 필터, 목록(페이지 20, 서버 페이징·정렬 화이트리스트), [네임스페이스 등록]. 행 선택 시 오른쪽이 그 네임스페이스의 메시지를 보여 준다. 선택은 URL 쿼리(?ns=코드)에 반영해 새로고침해도 유지. 사용 중지 행은 흐리게, [수정]/[사용 중지]/[복구] 동작.
   - 오른쪽(메시지 표): 선택한 네임스페이스 요약, 검색(keyword), 메시지 유형 필터, 번역 상태 필터, 사용 여부 필터, [메시지 등록], 표(키, 기본 문구, 유형, 번역 상태 요약, 사용 여부, 동작 [수정][삭제|복구]). 선택 전에는 안내 문구. 서버 페이징(페이지 20).
   - 번역 상태 요약 열: 활성 지원 언어 중 번역이 없는 언어가 있으면 눈에 띄게 표시하는 정도만 한다(명세에 없는 집계 API 를 만들지 않는다. 목록 응답에 있는 필드만 쓰고, 부족하면 질문으로 올린다).
   - 좁은 화면에서는 위아래로 쌓는다.

3. 네임스페이스 Drawer(등록/수정): 코드(등록 후 변경 불가, 수정에서는 읽기 전용), 이름, 솔루션 코드, 설명 등 명세의 필드만. 사용 중지 시 "메시지는 유지되지만 번들 조회에서 빠진다"는 안내를 확인창에 쓴다.

4. 메시지 Drawer(등록/수정): 탭 [기본 정보] [언어별 번역].
   - 기본 정보: 메시지 키(네임스페이스 안에서 유일, 수정 가능 여부는 명세 따름), 유형(문자열 입력 또는 공통코드 I18N_MESSAGE_TYPE 값 목록에서 선택 — 서버는 검증하지 않으므로 FE 는 코드 목록 조회가 가능하면 선택형으로, 아니면 입력형으로 하고 정한 방식을 보고), 기본 문구, 파라미터 설명.
   - 언어별 번역: GET /i18n/languages 의 활성 언어를 행으로 보여 주고 언어마다 번역 문구, 번역 상태(DRAFT 등 명세의 값), 기계 번역 여부를 입력한다. 문구를 비워 둔 언어는 요청에서 뺀다(서버가 요청에 없는 언어는 유지한다). 기본 언어 행을 눈에 띄게 표시하고, 수정 시 기존 번역과 version_no 를 읽기 전용으로 보여 준다(version_no 는 서버가 올린다).
   - 서버 검증 오류(400 fieldErrors, 409 중복, 400 I18N.LANGUAGE_NOT_FOUND)는 입력 아래 또는 안내로 보여 주고, 서버 문구 우선, 없으면 로컬 문구.

5. 삭제/복구: 메시지 [삭제]는 논리 삭제(use_yn='N')임을 확인창에 명시. 삭제된 키와 같은 키로 다시 등록하는 것이 가능하다는 점은 안내하지 않아도 된다(서버가 처리). 복구 후 목록 재조회, 선택 네임스페이스 유지.

6. 데이터 처리: src/services/mdm/ 에 i18n 서비스(네임스페이스, 메시지, 언어 목록)를 만들고 PageResponse(page 는 1부터)로 받는다. 컴포넌트에서 fetch 를 직접 쓰지 않는다. 저장 중 버튼 비활성과 연타 방지, 언어 목록은 화면 진입 시 1회 로드 후 재사용, 목록 재조회 시 선택 유지. 새 라이브러리는 추가하지 않는다.

7. 테스트(vitest, F3.1 방식): 처음부터 흐름 테스트까지 함께 만든다. 서비스 목으로 실제 네트워크 호출이 0건임을 매 테스트 뒤에 확인한다.
   - 규칙 단위: 번역 폼 → 요청 본문 변환(빈 언어 제외, 상태·기계번역 값), 오류 코드 → 문구, 쿼리 파라미터(page/size/sort/keyword/필터).
   - 흐름: 선택 전 안내, 네임스페이스 선택 → ?ns= 반영과 새로고침 진입, 네임스페이스 등록·수정(코드 읽기 전용)·사용 중지(확인창 문구, 취소 시 호출 없음)·복구, 메시지 등록(번역 포함)·수정·삭제·복구, 번역 비운 언어 제외, 저장 연타 시 요청 1회, 400 fieldErrors, 409 서버 문구와 빈 문구일 때 로컬 문구, 403 안내, 목록 실패 시 선택 유지.
   - 새 흐름 테스트 파일은 3회 반복 실행해 모두 통과함을 기록한다.
8. 검증: lint, antd lint, tsc, test(실행 시간과 통과 수), build(테넌트 산출물에 mdm/i18n 경로), build:platform(플랫폼 산출물에 이 화면이 없음). 개발 서버와 백엔드는 띄우지 않는다. 사용자가 직접 볼 항목(실제 서버 반영, 서버 오류 문구, 좁은 화면, 사용감)만 doc/f4-manual-check.md 맨 위에 모으고, 나머지는 자동 테스트 이름을 표시한다. 값(주소, 계정, 비밀번호)은 쓰지 않는다.
진행 기록은 [F4단계] 형식으로 추가하고 멈춘다. 화면 소스 외 기존 파일 수정이 있으면 따로 표시하고 이유를 쓴다.
```
