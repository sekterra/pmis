## [F0단계] 템플릿 정리와 개발 환경 - 2026-10-11

이전 기록은 옛 지시(브랜치·커밋) 때문에 파일을 바꾸지 않고 멈춘 상태였다. 지시가 커밋·브랜치 금지와 git 유무에 따른 백업으로 바뀌어, 그 기준으로 F0을 마쳤다. 커밋과 브랜치는 만들지 않았다.

### 변경/추가/삭제 파일

git: `frontend`와 `D:\_project\PMIS` 모두 저장소가 아니다. `git status`는 `fatal: not a git repository`이며 `.git`이 없다. 삭제 전에 `D:\_project\PMIS\frontend-backup-F0.zip`(720,836바이트)을 만들었다. `node_modules`와 생성물 `.umi`를 뺐다.

삭제 전 후보: `src/pages`의 dashboard, form, list, profile, result, account, chatbot, table-list, user/register, user/register-result, Admin. `src/locales`의 ko-KR/en-US 외 로케일과 데모 전용 키. `src/services/ant-design-pro`(생성물). `config`의 analytics, mock, 데모 프록시, 과다 라우트. `mock/`, 루트의 `cloudflare-worker/`, service-worker, china-division, DocLink, VersionDropdown, 데모 전용 컴포넌트.

삭제한 화면·자원: 위 데모 페이지와 `Welcome.css`, `Welcome-dark.css`, `mock/`, `cloudflare-worker/`, `src/service-worker.js`, `src/utils/chinaDivision.ts`, ArticleListContent, AvatarList, StandardFormRow, TagSelect, VersionDropdown. 로케일 bn-BD, fa-IR, id-ID, ja-JP, pt-BR, zh-CN, zh-TW와 en-US의 component, globalHeader, settings.

함께 삭제한 테스트: `src/pages/table-list/index.test.tsx`, `src/pages/account/settings/service.test.ts`, `src/pages/account/settings/components/base.test.tsx`, `src/pages/dashboard/analysis/index.test.tsx`, `src/pages/form/basic-form/index.test.tsx`, `src/utils/chinaDivision.test.ts`.

남긴 라우트: 로그인, `/welcome` 빈 페이지, 예외 403/404/500, `/` → `/welcome`, 그 외 `/*`는 404.

설정: 기본 로케일 `ko-KR`, analytics와 mock 설정 제거, `process.env.APP_MODE` define(`tenant` 기본). 개발 프록시는 `DEV_BACKEND_URL`이 있을 때만 `/api/mdm`, `/api/platform`. `DEV_TENANT_CODE`가 있을 때만 `X-Tenant-Code`. `.env.example`은 이름만. `.gitignore`에 `.env`, `.env.*`를 넣고 `.env.example`은 제외. 실제 `.env*` 파일은 없었다. README는 실행 방법과 환경변수 이름만 남겼다.

`ko-KR` 로케일을 추가하고 en-US는 레이아웃·예외·로그인 키만 남겼다. 403/500도 404와 같이 문구 키를 쓴다.

승인 전이라 `package.json`에서는 빼지 않음.

- 스크립트: `simple`, `openapi`, `i18n-remove`, `record`, `start`(mock 포함), `deploy`, `analyze`
- 의존성: `@ant-design/x`, `@ant-design/x-markdown`, `@ant-design/x-sdk`, `@ant-design/plots`, `china-division`, `d3`, `topojson-client`, `highlight.js`
- 개발 의존성: `@umijs/max-plugin-openapi`, `@umijs/request-record`, `mockjs`, `express`, `@types/express`, `swagger-ui-dist`, `gh-pages`, `geojson`, `@types/d3`, `@types/topojson-client`, `@types/topojson-specification`

`config`의 openAPI 플러그인과 `config/oneapi.json`, `config/routes.simple.ts`, `scripts/simple.js`도 승인 전까지 그대로 둔다. `simple`은 지운 페이지를 가리키므로 지금은 실행하면 실패한다.

### 검증 결과

`node_modules`가 없어 lockfile 기준으로 `npm install`을 한 뒤 확인했다. 의존성 제거는 하지 않았다.

- `npm run lint` 통과 (biome, tsc)
- `npx antd lint ./src` 종료 코드 0. `requestErrorConfig.ts`의 정적 `message`/`notification` 경고 8건은 기존 파일이며 수정하지 않았다
- `npm run tsc` 통과
- `npm run test` 통과 (3파일, 27개). `login.test.tsx`는 vitest 설정에서 제외되어 있다
- `npm run build` 통과
- `npm run dev`는 포트 8000. 로그인은 계정/휴대폰 탭과 로그인 버튼이 보였고, `/exception/404`는 404 문구와 홈으로 버튼이 보였다. 브라우저 언어가 영어라 문구는 en-US였다. 백엔드 주소는 넣지 않았다

### 질문/확인 필요

위에 적은 스크립트와 의존성을 제거해도 되는지. 승인 전에는 삭제하지 않는다.

### 가정한 사항

- `src/services/ant-design-pro/` 전체를 지우면 로그인·레이아웃의 샘플 API import가 깨진다. F1 전까지 샘플 호출을 유지하라고 되어 있어 `currentUser`, `login`, `outLogin`, `getFakeCaptcha`는 남기고 rule/notice 함수만 삭제했다.
- 로케일 키는 레이아웃·예외에 더해, 유지하는 로그인 화면이 쓰는 `pages.login.*`도 남겼다.
- 샘플 사용자 조회가 실패하면 템플릿이 로그인으로 보내 404가 보이지 않는다. `/exception`으로 시작하는 경로만 그 이동에서 빼 두었다. `/welcome`과 그 외 경로는 그대로 로그인으로 보낸다. 인증 구현은 하지 않았다.
- 백업 zip은 지시의 `node_modules` 외에 생성 디렉터리 `.umi`도 제외했다.
- `test`/`pre` 프록시에 있던 데모 주소는 비웠다. 운영 빌드 설정에는 프록시를 넣지 않는다.

## [F0.1단계] 불필요한 의존성·스크립트 제거 - 2026-10-11

커밋은 하지 않았다. 시작 전 백업은 `D:\_project\PMIS\frontend-backup-F0.1.zip`(11,460,071바이트)이다. `node_modules`와 `.umi`는 제외했다.

### 변경/추가/삭제 파일

참조 확인(`src`, `config`, `tests`, `scripts`, 생성물 `.umi` 제외):

- 제거한 패키지는 소스 import가 없었다. `mockjs`는 `src/typings.d.ts`의 ambient 선언만 있어 그 줄만 지우고 패키지를 제거했다.
- `@umijs/request-record`는 `src/pages/user/login/login.test.tsx`가 `@@/requestRecordMock`을 import하므로 제거하지 않았다. `config`의 `plugins`와 `requestRecord` 설정도 남겼다. 이 테스트는 vitest 기본 실행에서 제외되어 있다.
- `@ant-design/plots`와 스크립트 `analyze`는 지시대로 유지했다. 소스 import는 없다.
- `start:no-mock`, `start:pre`, `start:test`는 제거 목록에 없어 유지했다.

제거한 스크립트: `simple`, `openapi`, `i18n-remove`, `record`, `start`, `deploy`.

제거한 의존성: `@ant-design/x`, `@ant-design/x-markdown`, `@ant-design/x-sdk`, `china-division`, `d3`, `topojson-client`, `highlight.js`.

제거한 개발 의존성: `@umijs/max-plugin-openapi`, `mockjs`, `express`, `@types/express`, `swagger-ui-dist`, `gh-pages`, `geojson`, `@types/d3`, `@types/topojson-client`, `@types/topojson-specification`. `npm uninstall`로 lockfile을 갱신했다. 새 라이브러리는 없다.

삭제한 파일: `config/oneapi.json`, `config/routes.simple.ts`, `scripts/simple.js`, `scripts/i18n-remove.js`. `config/config.ts`에서 openAPI 플러그인 설정을 뺐다.

`src/services/ant-design-pro/` 샘플 API와 `requestErrorConfig.ts`는 그대로 두었다. README에는 제거된 스크립트 안내가 없어 고치지 않았다.

### 검증 결과

- `npm run lint` 통과
- `npx antd lint ./src` 종료 코드 0. `requestErrorConfig.ts` 경고 8건은 기존 파일이며 수정하지 않았다
- `npm run tsc` 통과
- `npm run test` 통과 (3파일, 27개)
- `npm run build` 통과
- `npm run dev` 포트 8000. 로그인 화면과 `/exception/404`가 보였다. 브라우저 언어가 영어라 문구는 en-US였다

### 질문/확인 필요

`@umijs/request-record`를 로그인 테스트와 함께 뺄지. 이번 단계에서는 참조가 남아 제거하지 않았다.

### 가정한 사항

- `express` 직접 의존성은 소스 import가 없어 제거했다. request-record 패키지 자신의 의존성으로 express는 남는다.
- `start`(mock 포함)만 제거하고, mock을 끈 `start:*` 스크립트는 유지했다.

## [F1단계] 코어(요청·인증·다국어)와 레이아웃 셸 - 2026-10-11

커밋은 하지 않았다. 테넌트 사용자 앱만 연결했다. 플랫폼 콘솔 화면은 없다.

### 변경/추가/삭제 파일

추가:

- `src/core/request/errors.ts`, `feedback.ts`, `runtime.ts`, `table.ts`, `timeout.ts`와 테스트 `errors.test.ts`, `table.test.ts`
- `src/core/auth/mode.ts`, `token.ts`, `redirect.ts`, `guard.ts`, `refresh.ts`, `broadcast.ts`, `actions.ts`와 테스트 `refresh.test.ts`, `actions.test.ts`, `guard.test.ts`, `token.test.ts`
- `src/core/i18n/messages.ts`, `load.ts`, `store.ts`, `apply.ts`, `load.test.ts`
- `src/core/menu/types.ts`
- `src/services/mdm/data.d.ts`, `auth.ts`, `i18n.ts`
- `src/components/BrandMark.tsx`, `LanguageSelect.tsx`, `RouteGuard.tsx`, `FeedbackBridge.tsx`, `RightContent/UserMenu.tsx`
- `src/pages/user/password/index.tsx`
- `doc/f1-manual-check.md`

변경:

- `src/app.tsx`, `src/app.test.tsx`, `src/access.ts`, `src/access.test.ts`, `src/global.tsx`, `src/typings.d.ts`
- `src/pages/user/login/index.tsx`, `src/pages/user/login/login.test.tsx`
- `src/components/index.ts`, `src/components/ErrorBoundary/index.tsx`
- `src/locales/ko-KR.ts`, `src/locales/en-US.ts`, `ko-KR/pages.ts`, `en-US/pages.ts`
- `config/config.ts`, `config/routes.ts`, `config/defaultSettings.ts`, `vitest.config.ts`
- `package.json`, `package-lock.json` (`@umijs/request-record` 제거)

삭제:

- `src/services/ant-design-pro/`
- `src/requestErrorConfig.ts`, `src/requestErrorConfig.test.ts`
- `src/components/RightContent/AvatarDropdown.tsx`, `LangDropdown.tsx`, `index.tsx`
- `src/locales/ko-KR/settingDrawer.ts`, `src/locales/en-US/settingDrawer.ts`
- `src/pages/user/login/__snapshots__/login.test.tsx.snap`
- `config`의 `requestRecord`와 `@umijs/request-record` 플러그인

### 검증 결과

최종 코드 기준으로 다시 실행했다.

- `npm run lint`: 통과. biome 76파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npx antd lint ./src`: 통과. 62파일, 이슈 0. `requestErrorConfig.ts` 정적 `message`/`notification` 경고는 파일 삭제로 없음
- `npm run tsc`: 통과. `npm run lint` 안의 `tsc --noEmit`이 종료 코드 0
- `npm run test`: 통과. 10파일, 20개
- `npm run build`: 통과. 종료 코드 0

개발 서버를 다시 띄운 뒤 `http://localhost:8000/user/login`에서 PMIS, 아이디, 비밀번호, 로그인, 언어 선택이 보였다. 백엔드가 없어도 이 화면은 요청이 끝날 때까지 멈추지 않는다.

### 지시서 테스트 대응

| 항목 | 확인하는 테스트 |
| --- | --- |
| (a) 래퍼 성공/실패, 문자열 errorCode, showType, traceId, fieldErrors | `src/core/request/errors.test.ts` |
| (b) 페이징 어댑터, 정렬 포함 | `src/core/request/table.test.ts` |
| (c) 401 TOKEN_EXPIRED, 동시 3요청이 refresh 1번 후 재시도 성공 | `src/core/auth/refresh.test.ts`의 동시 요청 테스트 |
| (d) refresh 409 한 번 재시도 / 401이면 로그인 이동, 무한 재시도 없음 | `src/core/auth/refresh.test.ts`의 409·401 테스트 |
| (e) 앱 시작 세션 복구 성공/실패 | `src/core/auth/actions.test.ts`의 `startSession`, `recoverSession`. `src/app.test.tsx`는 첫 화면이 복구보다 먼저 그려지는지만 확인 |
| (f) 로그인 성공의 passwordChangeRequired 분기, 실패 문구 | `src/core/auth/actions.test.ts`의 로그인 분기. `src/pages/user/login/login.test.tsx`의 실패 문구와 비밀번호 변경 화면 이동 |
| (g) 로그아웃, BroadcastChannel 수신 시 서버 호출 없이 상태 비움 | `src/core/auth/actions.test.ts`의 원격 로그아웃과 `logoutSession` |
| (h) 비밀번호 변경 강제 시 다른 경로 차단, 성공 후 토큰 교체 | `src/core/auth/guard.test.ts`의 경로 차단. `src/core/auth/actions.test.ts`의 `completePasswordChange` |
| (i) 번들 우선순위, 304, 실패 시 로컬 문구 | `src/core/i18n/load.test.ts` |
| (j) 외부 URL redirect 거부 | `src/core/auth/guard.test.ts`의 `safeRedirect` |
| (k) Access 토큰이 localStorage/sessionStorage에 없음 | `src/core/auth/token.test.ts` |

### 질문/확인 필요

없음.

### 가정한 사항

- 공개 번들은 `GET /api/mdm/v1/i18n/bundles/{namespaceCode}`로 받는다. 명세에서 ETag와 래퍼 없는 304가 이 단건 조회에 있다. `namespaces` 목록 조회는 F1에서 쓰지 않았다.
- `config`의 `baseNavigator`는 끈다. 언어는 저장한 로케일, 없으면 서버 `defaultLanguage`, 그것도 없으면 `ko-KR`이다. 선택한 로케일만 `localStorage` 키 `pmis.locale`에 둔다.
- `APP_MODE=platform`이면 인증 경로 `/api/platform/v1/auth`와 쿠키 이름 `mdm_platform_refresh`를 쓴다. F1 화면과 라우트는 테넌트용만 있다.
- 첫 화면은 `getInitialState`에서 네트워크를 기다리지 않는다. 세션 복구와 공개 번들은 그린 뒤에 실행하고, 그 동안은 스피너만 보인다. 요청 timeout은 15초, 공개 번들 대기는 8초다.
- 라우트 가드는 라우터 밖이라 `useLocation` 대신 `history.listen`을 쓴다.
- `access().can`은 F4까지 항상 `false`다. `menus/my`는 호출하지 않는다.
- `passwordChangeRequired`가 아니면 `/user/password`는 자발적 변경으로 열어 둔다.

## [F2단계] 플랫폼 콘솔 - 2026-10-11

테넌트 앱과 플랫폼 콘솔을 `APP_MODE`로 나눠 빌드한다. 플랫폼 화면은 운영자 로그인, 비밀번호 변경, 테넌트 목록·등록·상세·도메인·중지/재개·프로비저닝 재시도·임시 비밀번호 재발급이다. 스키마 업그레이드 화면은 만들지 않았다. 백엔드 코드는 수정하지 않았다. 커밋은 하지 않았다.

### 변경/추가/삭제 파일

추가:

- `config/routes.platform.ts`
- `src/core/i18n/extra.ts`, `src/core/i18n/extra.platform.ts`
- `src/core/request/tenantHeader.ts`
- `src/core/auth/mode.test.ts`
- `src/services/platform/auth.ts`, `tenants.ts`, `data.d.ts`
- `src/locales/platform/ko-KR.ts`, `en-US.ts`
- `src/pages/platform/login/index.tsx`, `password/index.tsx`
- `src/pages/platform/tenants/index.tsx`, `new.tsx`, `detail.tsx`, `rules.ts`, `TemporaryPasswordModal.tsx`, `TenantActionBar.tsx`
- 테스트: `rules.test.ts`, `list.test.ts`, `secret.test.tsx`, `actions.test.tsx`, `conflicts.test.tsx`, `formFields.test.tsx`
- `doc/f2-manual-check.md`

수정:

- `package.json`: `dev:platform`, `build:platform`
- `config/config.ts`: 모드별 라우트, 문서 제목, 플랫폼 문구 alias
- `config/proxy.ts`: `/api/platform`에는 `X-Tenant-Code`를 붙이지 않음
- `src/app.tsx`: 모드별 세션 API와 홈 경로
- `src/core/auth/mode.ts`, `actions.ts`, `guard.ts`, `redirect.ts`: 플랫폼 홈 `/tenants`
- `src/core/i18n/apply.ts`, `load.ts`, `load.test.ts`: 플랫폼 모드에서는 서버 번들을 호출하지 않음

삭제한 파일은 없다.

### 검증 결과

- `npm run lint`: 통과. biome 97파일, 이어서 tsc. 종료 코드 0
- `npx antd lint ./src`: 통과. 85파일, 이슈 0
- `npm run tsc`: 통과
- `npm run test`: 통과. 17파일, 31개
- `npm run build`: 통과
- `npm run build:platform`: 통과

번들 분리 확인: 각 빌드 직후 `dist`의 js/html을 `findstr`로 검색했다. 테넌트 빌드에는 `platform/login`, `pages/platform`, `PMIS 플랫폼 콘솔`이 없고 `welcome` 경로가 있다. 플랫폼 빌드에는 `welcome` 디렉터리가 없고 `pages/Welcome`, `pages/user/login`이 없으며 `pages.platform.title`과 `platform/tenants`가 있다.

8000 포트는 이미 사용 중이라 플랫폼 개발 서버만 다른 로컬 포트에서 잠깐 띄웠다. 로그인 카드에 `PMIS 플랫폼 콘솔`, 아이디, 비밀번호, 로그인, 언어 선택(한국어/English)이 보였다. 언어 변경 중 서버 번들 요청은 없고, 세션 복구는 `/api/platform/v1/auth/refresh`만 호출했다. 빈 로그인 제출은 필수값 문구를 보여 줬다. `/tenants`는 로그인으로 돌려보냈다. 확인 후 그 플랫폼 개발 서버는 중지했다. 실제 등록·중지·재발급은 `doc/f2-manual-check.md` 순서다.

### 지시서 테스트 대응

| 항목 | 확인 |
| --- | --- |
| (a) 목록 조회 파라미터 | `src/pages/platform/tenants/list.test.ts` |
| (b) 등록 필수값, 도메인, 스키마명 없는 본문, fieldErrors | `rules.test.ts`의 검증·본문. `formFields.test.tsx`는 서버 fieldErrors가 해당 입력칸을 오류 상태로 만드는지 확인 |
| (c) 임시 비밀번호는 모달 안에만, 닫으면 사라짐 | `secret.test.tsx` |
| (d) 상태별 버튼, 중지 사유 필수 | `rules.test.ts`의 `visibleActions`. `actions.test.tsx` |
| (e) 마지막 도메인·중복 도메인 409 메시지 | `conflicts.test.tsx`. 상세 화면은 같은 서버 `errorMessage`를 알림으로 보여 준다 |
| (f) 플랫폼 인증 경로·쿠키, `X-Tenant-Code` 없음 | `src/core/auth/mode.test.ts` |
| (g) 플랫폼 모드에서 서버 번들 미호출 | `src/core/i18n/load.test.ts` |
| (h) 빌드 산출물 분리 | 위 `findstr` 검색 |

### 질문/확인 필요

- 목록 응답 `TenantSummary`에는 코드, 이름, 스키마명, 상태, 기본 언어, 시간대만 있다. 지시서 열의 프로비저닝 단계, 기본 도메인, 생성일은 목록 응답에 없어서 표에 넣지 않았다. 정렬 가능 필드 중 `createDatetime`도 값이 없어 열로 만들지 않았다.
- `platform-api.md`는 재개 본문을 적지 않는다. Controller `ReasonRequest`는 재개에도 `changeReason`이 필수라 재개 창에서도 사유를 받는다.
- 생성·재발급 응답에는 관리자 로그인 아이디가 없다. 등록 직후 창은 입력한 이메일을 소문자로 만들어 로그인 아이디로 보여 준다. 재발급 창은 임시 비밀번호와 테넌트 코드만 보여 준다.
- 프로비저닝 재시도 응답도 `CreatedTenant`라 임시 비밀번호가 있으면 같은 일회 창을 연다.

### 가정한 사항

- 플랫폼 기본 언어 선택은 서버 언어 API를 부르지 않고 `ko-KR`, `en-US`만 둔다.
- 시간대는 `Intl.supportedValuesOf('timeZone')` 목록이고 기본값은 `Asia/Seoul`이다.
- 플랫폼 문구는 `src/locales/platform`에 두고, 빌드 alias로 플랫폼 빌드만 그 파일을 포함한다.
- 상세의 스키마명은 응답에 있으나 화면 기본 정보에는 보이지 않는다. 등록 폼에서도 받지 않는다.
- 재개·중지의 상태 전이는 서버가 거절하면 `TENANT.INVALID_STATUS_TRANSITION` 메시지를 그대로 보여 준다.

## [F2.1단계] 비밀번호 변경 화면에서 나가기 - 2026-10-11

테넌트와 플랫폼 비밀번호 변경 카드 아래에 `다른 계정으로 로그인`을 두었다. 누르면 각 모드의 `POST /auth/logout`(`X-MDM-Request: 1`) 후 메모리 토큰을 지우고, BroadcastChannel로 다른 탭에 알린 뒤 로그인으로 간다. 서버 호출이 실패해도 로컬 상태를 비우고 로그인으로 간다. `passwordChangeRequired`가 false일 때만 `취소`가 보이며, 이전 화면이 있으면 뒤로, 없으면 홈으로 간다. 강제 변경 중에도 `/user/login`은 가드가 비밀번호 화면으로 되돌리지 않는다. 변경 성공은 새 액세스 토큰으로 바꾼 뒤 홈으로 가고, 실패 문구는 카드 안 알림으로 보인다. 커밋은 하지 않았다.

### 변경/추가/삭제 파일

- `src/core/auth/actions.ts`, `actions.test.ts`: `signOutToLogin`, `cancelPasswordChange`
- `src/core/auth/guard.ts`, `guard.test.ts`: 강제 변경 중 로그인 경로 허용
- `src/core/auth/refresh.test.ts`: 기존 테스트의 타입 오류 수정
- `src/pages/user/password/index.tsx`, `password.test.tsx`
- `src/pages/platform/password/index.tsx`, `password.test.tsx`
- `src/locales/ko-KR/pages.ts`, `src/locales/en-US/pages.ts`

삭제한 파일은 없다.

### 검증 결과

- `npm run lint`: 통과. biome 99파일, 이어서 tsc. 종료 코드 0
- `npx antd lint ./src`: 통과. 87파일, 이슈 0
- `npm run tsc`: 통과. lint 안의 `tsc --noEmit`이 종료 코드 0
- `npm run test`: 통과. 19파일, 39개
- `npm run build`: 통과. 종료 코드 0

로그인하지 않은 상태에서 `/user/password`는 로그인으로 돌아갔다. 로그아웃 버튼, 서버 실패 후의 로컬 정리, 성공 시 토큰 교체와 홈 이동, 실패 문구는 테스트로 확인했다. 확인 후 8000과 8002의 개발 서버는 종료했다.

### 지시서 테스트 대응

| 항목 | 확인 |
| --- | --- |
| 로그아웃 후 로그인 이동 | `src/pages/user/password/password.test.tsx`, `src/pages/platform/password/password.test.tsx` |
| 서버 오류여도 로컬 상태 비움 | 위 테스트와 `src/core/auth/actions.test.ts` |
| 강제 변경 중에도 로그아웃 경로 허용 | `src/core/auth/guard.test.ts` |
| 성공 시 토큰 교체 후 홈 | `password.test.tsx`, `actions.test.ts`의 `completePasswordChange` |
| 실패 시 서버 문구 | `password.test.tsx`의 alert |

### 질문/확인 필요

없음.

### 가정한 사항

- 이전 화면은 `history.state.idx`가 0보다 클 때만 뒤로 간다. 그렇지 않으면 홈이다.
- 테넌트 홈은 `/welcome`, 플랫폼 홈은 `/tenants`다.

## [F2.2단계] 백엔드 8.6 응답 반영 - 2026-10-11

### 변경/추가 파일

- 수정: `src/services/platform/data.d.ts`, `src/pages/platform/tenants/index.tsx`, `new.tsx`, `detail.tsx`, `TemporaryPasswordModal.tsx`, `rules.ts`
- 추가: `src/pages/platform/tenants/ProvisioningStatus.tsx`, `columns.test.tsx`
- 수정: `secret.test.tsx`, `rules.test.ts`, `list.test.ts`, `src/locales/platform/ko-KR.ts`, `en-US.ts`, `doc/f2-manual-check.md`
- 백엔드는 수정하지 않았다

### 검증 결과

- `npm run lint`: 통과. biome 101파일, 이어서 tsc. 종료 코드 0
- `npx antd lint ./src`: 통과. 89파일, 이슈 0
- `npm run tsc`: 통과. lint 안의 `tsc --noEmit`이 종료 코드 0
- `npm run test`: 통과. 20파일, 43개
- `npm run build`, `npm run build:platform`: 통과. 종료 코드 0
- 개발 서버는 띄우지 않았다

### 지시서 테스트 대응

| 항목 | 확인 |
| --- | --- |
| `adminLoginId`를 그대로 표시하고 이메일 추정 없음 | `rules.test.ts`의 `secretView`, `secret.test.tsx` |
| 임시 비밀번호가 없을 때 모달 | `secret.test.tsx` |
| 목록 새 열, null, 알 수 없는 단계 | `columns.test.tsx`, `rules.test.ts` |
| 정렬 파라미터 | `list.test.ts`의 `createDatetime,desc` |
| 기존 비밀 취급 | `secret.test.tsx`가 닫은 뒤 비밀번호를 남기지 않음 |

### 질문/확인 필요

없음.

### 가정한 사항

- 생성일은 브라우저 로컬 시간대로 `YYYY-MM-DD HH:mm`이다. 없거나 빈 값이면 `-`이고, 파싱할 수 없는 문자열은 그대로 보여 준다.
- 알려진 단계와 `FAILED`만 문구로 바꾸고, 그 외 코드는 값 그대로 태그로 보여 준다. `FAILED`는 error 색이다.
- 목록의 기본 언어와 시간대 열은 그대로 둔다. 헤더 정렬은 코드, 이름, 상태, 생성일만 연다.

## [F2.3단계] 로컬 환경 파일과 개발 포트 - 2026-10-11

### 변경/추가 파일

- 추가: `src/core/dev/localEnv.ts`, `devPort.ts`, `proxyRules.ts`, `runDev.ts`, `devServer.test.ts`
- 추가: `.env.local.example`, `doc/local-setup.md`
- 수정: `config/proxy.ts`, `package.json`, `.gitignore`, `.env.example`, `README.md`
- `.env.local`은 이미 있었고 수정하지 않았다. 값은 기록하지 않는다

### 검증 결과

- `npm run lint`: 통과. biome 106파일, 이어서 tsc. 종료 코드 0
- `npx antd lint ./src`: 통과. 94파일, 이슈 0
- `npm run tsc`: 통과. lint 안의 `tsc --noEmit`이 종료 코드 0
- `npm run test`: 통과. 21파일, 49개
- `npm run build`, `npm run build:platform`: 통과. 종료 코드 0
- 개발 서버는 띄우지 않았다

### 지시서 테스트 대응

| 항목 | 확인 |
| --- | --- |
| 주소가 있으면 프록시 규칙, 없으면 빈 객체 | `devServer.test.ts`의 `builds proxy rules only when the backend url is set` |
| `X-Tenant-Code`는 `/api/mdm`만 | `devServer.test.ts`와 기존 `src/core/auth/mode.test.ts` |
| 경고는 주소가 없을 때만 | `devServer.test.ts`의 `warns only when the backend url is missing` |
| 포트 기본값과 `PORT` 우선, 사용 중이면 실패 | `devServer.test.ts`의 `dev port` |
| 파일 키가 셸 값을 덮고, 없는 키는 유지 | `devServer.test.ts`의 `local env files` |

### 환경 파일과 포트

- `.gitignore`의 `.env.*`가 `.env.local`을 이미 무시했다. `.env.local.example`도 같은 규칙에 걸리므로 `!.env.local.example`을 추가했다. `git status`에서 예제 파일은 추적 대상으로 보인다.
- Umi `@umijs/core` `Service.run`은 설정보다 먼저 `loadEnv({ envFile: ".env" })`를 호출한다. `.env` 다음 `.env.local`을 읽고, 파일에 있는 키는 `process.env`를 덮어쓴다. 셸이 파일을 이기는 동작은 이 버전에 없어서 그대로 두었다. 시작 스크립트도 같은 순서로 덮어쓴 뒤 포트와 경고를 판단한다.
- 포트는 `config.ts`에 넣지 않았다. `npm run dev`는 8000, `npm run dev:platform`은 8002이고, `PORT`가 있으면 그 값을 쓴다. 파일에 `PORT`가 있으면 셸보다 파일 값이 우선한다. 그 포트에 `0.0.0.0`으로 바인드가 안 되면 `max dev`를 띄우기 전에 종료한다. Umi `portfinder`가 다음 포트로 넘어가는 경로를 타지 않게 하려는 것이다. 문구는 `이미 다른 개발 서버가 떠 있을 수 있습니다. 8000/8002 포트를 확인하세요`이고 뒤에 해당 포트 번호를 붙인다.
- 예제 키 목록이 `.env.example`과 겹쳐서 값은 `.env.local.example` 하나만 둔다. `.env.example`은 그 파일로 안내만 남긴다. 빈 `APP_MODE=`를 복사하면 스크립트가 넣은 platform 값이 지워지므로 템플릿에 `APP_MODE`를 넣지 않았다.
- 기존 `.env.local`의 `DEV_BACKEND_URL` 키는 값이 있다. `DEV_TENANT_CODE` 키는 비어 있다. 테넌트 헤더가 필요하면 사용자가 채운다.

### 질문/확인 필요

없음.

### 가정한 사항

- 환경 파일 파서는 한 줄 `KEY=value`, 따옴표, `export`만 처리한다. Umi가 쓰는 `${}` 확장은 시작 전 검사에서 하지 않는다. 주소와 포트를 그대로 적으면 프록시가 읽는 결과와 같다.
- 경고는 `npm run dev` / `dev:platform`이 `max`를 띄우기 전에 한 번 낸다. 프로덕션 빌드에서는 내지 않는다.

## [F3단계] 공통코드 관리 화면 - 2026-10-12

### 변경/추가 파일

- 추가: `src/pages/mdm/common-code/` (목록, 그룹/코드 Drawer, 이력 Drawer, 규칙, 임시 권한), `src/services/mdm/codes.ts`, `code.d.ts`, `codeQuery.ts`
- 추가: `doc/f3-wireframe.md`, `doc/f3-manual-check.md`
- 수정: `config/routes.ts`, `src/locales/ko-KR/menu.ts`, `en-US/menu.ts`, `ko-KR/pages.ts`, `en-US/pages.ts`, `src/typings.d.ts`
- 플랫폼 라우트는 수정하지 않았다
- `.env.local`에서 비어 있던 `APP_MODE` 키만 지웠다. 다른 값은 읽거나 기록하지 않았다

### 검증 결과

- `npm run lint`: 통과. biome 115파일, 이어서 tsc. 종료 코드 0
- `npx antd lint ./src`: 통과. 105파일, 이슈 0
- `npm run tsc`: 통과. lint 안의 `tsc --noEmit`이 종료 코드 0
- `npm run test`: 통과. 23파일, 56개
- `npm run build`: 통과. 테넌트 라우트에 `/mdm/common-code`가 있다
- `npm run build:platform`: 통과. 라우트에 공통코드 화면이 없고 `/tenants`가 있다
- 개발 서버는 띄우지 않았다

### 지시서 테스트 대응

| 항목 | 확인 |
| --- | --- |
| 트리 조립, 부모 없는 행 | `rules.test.ts` |
| 언어별 이름에서 빈 언어 제외 | `rules.test.ts` |
| 오류 코드 문구, 서버 문구 우선 | `rules.test.ts` |
| page/size/sort 쿼리 | `rules.test.ts`의 `listParams` |
| 그룹 선택 후 코드 목록 호출 | `page.test.tsx` |

### 질문/확인 필요

없음. 와이어프레임은 승인된 좌우 분할, 폼 안 언어 탭, 이력 Drawer, 계층형 트리와 같다.

### 가정한 사항

- `useAccess().can`은 있으나 `access()`가 아직 항상 false다. 버튼 표시는 `permission.ts`의 `grantCommonCode` 한 곳이 임시로 허용한다. F5에서 그 함수 본문을 서버 권한 호출로 바꾸면 된다. 서버 403 `AUTH.FORBIDDEN`은 화면 안내로 보여 준다.
- 그룹 코드는 수정할 수 없고, 코드 값은 수정 API가 바꾸므로 수정할 수 있다. 폼에 없는 `extAttrSchema`는 수정 때 기존 값을 다시 보낸다.
- 계층 트리는 현재 페이지(20건)만 조립한다. 그 페이지에 부모가 없으면 맨 위에 둔다. 상위 코드 후보는 200건씩 페이지를 따라 모으고, 자신과 하위는 빼며 사용 중인 코드만 둔다.
- 관리 응답 필드는 명세의 요청 필드와 백엔드 응답 레코드(`codeGroupPk`, `useYn`, `levelNo`, `codePath`, 이력 필드)를 따른다. 명세에 없는 입력은 만들지 않았다.
- `.env.local`의 빈 `APP_MODE`는 Umi가 스크립트의 `APP_MODE=platform`을 빈 값으로 덮어 플랫폼 빌드가 테넌트 라우트를 쓰게 했다. 그 키만 제거했다.

## [F3.1단계] 공통코드 화면 흐름 테스트 보강 - 2026-10-12

### 변경/추가 파일

- 추가: `src/pages/mdm/common-code/fixtures.ts`, `src/pages/mdm/common-code/flow.test.tsx`
- 수정: `src/pages/mdm/common-code/page.test.tsx` (공통 목 데이터 사용), `doc/f3-manual-check.md`
- 화면 소스(비테스트)는 수정하지 않았다. 테스트에서 나온 실패는 화면 동작이 아니라 테스트가 antd 6 셀렉트, 로딩 버튼, 날짜 패널, 확인창을 누르는 방식 문제였다.

### 검증 결과

- `npm run lint`: 통과. biome 117파일, 이어서 tsc. 종료 코드 0
- `npx antd lint ./src`: 통과. 107파일, 이슈 0
- `npm run tsc`: 통과. 종료 코드 0
- `npm run test`: 통과. 24파일, 76개. 약 63초
- `flow.test.tsx`를 3회 반복 실행했다. 매번 20개 통과
- `npm run build`: 통과. 테넌트 산출물에 `mdm/common-code`가 있다
- `npm run build:platform`: 통과. 산출물에 공통코드 화면이 없고 `tenants`가 있다. 10번은 이 결과와 F3 플랫폼 빌드로 확인했고, 라우트 테스트는 중복으로 넣지 않았다
- 개발 서버와 백엔드는 띄우지 않았다

### 지시서 테스트 대응

| 항목 | 확인 |
| --- | --- |
| 1 그룹 미선택 | `flow.test.tsx`의 `shows the empty hint and does not load codes before a group is chosen` |
| 2 검색 쿼리, 중지 그룹의 복구 | `searches groups with page, size, keyword, solution, and use flag`, `shows restore on a disabled group and hides disable` |
| 3 쿼리와 새로고침 진입 | `puts the selected group in the query and loads that group on entry`, `loads codes from an existing group query without another click` |
| 4 그룹 저장, 코드 읽기 전용, 저장 연타 | `saves a group without empty languages and reloads the list`, `keeps the group code read-only when editing`, `disables save while the request is in flight` |
| 5 코드 본문, 날짜, 길이 | `saves code fields and blocks an end date before the start date`, `does not save a code value longer than the group limit` |
| 6 트리와 상위 코드 | `shows a hierarchy as a tree and omits self and descendants from parents` |
| 7 논리 삭제, 409, 복구 | `explains logical delete and skips the call on cancel`, `reloads the list after a confirmed disable`, `shows the server conflict message`, `uses the local conflict text when the server message is empty`, `reloads after restore and keeps the selected group` |
| 8 이력 | `pages history and shows the change, reason, user, and value diff` |
| 400, 403, 목록 실패 | `shows a field error under the input`, `shows a permission notice when the list is forbidden`, `keeps the selected group when a later list call fails` |
| 9 좁은 화면 | 자동화하지 않음. `doc/f3-manual-check.md`에 직접 확인으로 남김 |
| 10 플랫폼 메뉴 | 라우트 테스트는 추가하지 않음. `build:platform` 산출물에 공통코드 경로가 없다 |

서비스 목과 `rawRequest` 목으로 실제 `fetch`가 호출되지 않는 것을 매 테스트 뒤에서 확인한다.

### 질문/확인 필요

사용자가 직접 볼 항목은 `doc/f3-manual-check.md` 맨 위에 모았다. 실제 서버 반영, 서버 오류 문구, 좁은 화면 배치, 전체 사용감이다.

### 가정한 사항

- 저장 중 버튼은 antd 6이 HTML `disabled` 대신 로딩 상태에서 클릭을 무시한다. 테스트는 로딩 표시와 요청 1회를 확인한다.
- 날짜는 열린 달력의 같은 달 첫날과 마지막 날을 고른다. 종료일이 시작일보다 이르면 저장하지 않고, 순서가 맞으면 그 날짜가 요청 본문에 들어간다.

## [F4단계] 다국어 관리 화면 (네임스페이스 / 메시지 번역) - 2026-10-14

### 변경/추가 파일

- 추가: `src/pages/mdm/i18n/` (목록, 네임스페이스 Drawer, 메시지 Drawer, 규칙, 임시 권한, 흐름 테스트)
- 추가: `src/services/mdm/i18nAdmin.ts`, `i18nRecord.d.ts`, `i18nQuery.ts`
- 추가: `doc/f4-wireframe.md`, `doc/f4-manual-check.md`
- 기존 파일 수정: `config/routes.ts`에 테넌트 경로 `/mdm/i18n`을 넣었다. `src/locales/ko-KR/pages.ts`, `en-US/pages.ts`, `ko-KR/menu.ts`, `en-US/menu.ts`에 화면 문구를 넣었다. 플랫폼 라우트는 수정하지 않았다
- 언어 목록은 기존 `src/services/mdm/i18n.ts`의 `fetchLanguages`를 그대로 쓴다. 공통코드 화면 소스는 수정하지 않았다

### 검증 결과

- `npm run lint`: 통과. biome 126파일, 이어서 tsc. 종료 코드 0
- `npx antd lint ./src`: 통과. 118파일, 이슈 0
- `npm run tsc`: 통과. lint 안의 `tsc --noEmit`이 종료 코드 0
- `npm run test`: 통과. 26파일, 99개. 약 81초
- `src/pages/mdm/i18n/flow.test.tsx`를 3회 반복 실행했다. 매번 20개 통과
- `npm run build`: 통과. 테넌트 산출물에 `mdm/i18n`이 있다
- `npm run build:platform`: 통과. 산출물에 `mdm/i18n`이 없고 `tenants`가 있다
- 개발 서버와 백엔드는 띄우지 않았다

### 지시서 테스트 대응

| 항목 | 확인 |
| --- | --- |
| 번역 본문, 빈 언어 제외 | `rules.test.ts` |
| 오류 문구, 쿼리 파라미터 | `rules.test.ts` |
| 선택 전 안내, `?ns=`, 등록·수정·중지·복구, 메시지 등록·수정·삭제·복구, 연타, 400, 409, 403, 선택 유지 | `flow.test.tsx` |

### 질문/확인 필요

- 메시지 목록 응답의 `translations`는 비어 있고, 번역과 `versionNo`는 단건 조회에만 있다. 그래서 표의 번역 상태 열은 빠진 언어를 계산하지 못하고 `—`만 보여 준다. 목록에 번역이 포함되면 그 열을 채울 수 있다.

### 가정한 사항

- 메시지 키와 네임스페이스는 수정 API가 바꾸지 않으므로 수정 화면에서 읽기 전용이다.
- 메시지 유형과 번역 상태는 공통코드 `I18N_MESSAGE_TYPE`, `I18N_TRANSLATION_STATUS` 조회가 되면 선택이고, 실패하거나 비어 있으면 입력이다.
- 권한은 `permission.ts`의 `grantI18n`이 임시로 허용한다. F5에서 본문을 `can('I18N', action)`으로 바꾸면 된다.
- 좌우 배치 스타일은 공통코드 화면과 같게 이 화면 파일에 두었다. 공통코드 화면은 건드리지 않았다.

## [F4.1단계] 지원 언어·국가 관리 - 2026-10-19

### 변경/추가 파일

- 추가: `src/pages/mdm/language/` (탭 화면, 언어·국가 Drawer, 규칙, 임시 권한, 흐름·규칙 테스트)
- 추가: `src/services/mdm/languageAdmin.ts`, `languageQuery.ts`, `languageRecord.d.ts`
- 추가: `doc/f4.1-wireframe.md`, `doc/f4.1-manual-check.md`
- 수정: `config/routes.ts`에 테넌트 경로 `/mdm/language`. 플랫폼 라우트는 수정하지 않았다
- 수정: `src/locales/ko-KR`, `en-US`의 `menu.ts`, `pages.ts`
- 수정: `src/services/mdm/i18nRecord.d.ts`에 목록 요약 3필드를 선택 필드로 추가. `src/pages/mdm/i18n/index.tsx` 번역 상태 열과 `flow.test.tsx`
- 백엔드는 수정하지 않았다. `src/core/i18n`에 언어 목록 무효화 함수가 없어 새로 만들지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 134파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npx.cmd antd lint ./src`: 통과. 128파일, 이슈 0
- `npm.cmd run tsc`: 통과. lint 안의 `tsc --noEmit`이 종료 코드 0
- `npm.cmd run test`: 통과. 28파일, 128개. 약 125초
- `src/pages/mdm/language/flow.test.tsx`를 3회 반복 실행했다. 매번 21개 통과
- `npm.cmd run build`: 통과. 테넌트 산출물에 `mdm/language`가 있다
- `npm.cmd run build:platform`: `dist`와 `src/.umi-production`을 지운 뒤 통과. 산출물에 `mdm/language`가 없고 `tenants`가 있다
- 개발 서버와 백엔드는 띄우지 않았다

### 질문/확인 필요

- 합의 도식의 국가 탭 검색·사용 여부 필터는 `GET /nations?managed=true`에 해당 파라미터가 없어 만들지 않았다. 열, 페이징, 정렬, Drawer는 구현했다.
- `src/core/i18n`에는 언어 목록 캐시 무효화 함수가 없다. 이 화면의 목록은 저장·중지·복구 후 다시 조회한다. 로그인·헤더 언어 선택은 다음 `loadPublicI18n`(앱 시작 또는 로케일 변경) 때 바뀌고, F4 번역 탭 언어 목록은 그 화면이 다시 마운트될 때 `fetchLanguages`로 다시 받는다.
- 사용자가 직접 볼 항목은 `doc/f4.1-manual-check.md` 맨 위에 모았다.

### 가정한 사항

- 권한은 `permission.ts`의 `grantLanguage`가 임시로 허용한다. F5에서 본문을 `can('I18N', action)`으로 바꾸면 된다. `src/access.ts`의 `can()`은 그대로 두었다.
- 저장 중 버튼은 antd 6이 HTML `disabled` 대신 로딩 상태에서 클릭을 무시한다. 테스트는 요청 1회를 확인한다.
- 새로 등록하면서 기본 언어로 저장할 때도 확인창을 띄운다. 이미 기본인 언어를 수정할 때는 띄우지 않는다.
- 403 `AUTH.FORBIDDEN`은 서버 문구를 쓰지 않고 로컬 안내만 보여 준다.

## [F5단계] 서버 메뉴와 실제 권한 연결 - 2026-10-19

### 변경/추가 파일

- 추가: `src/services/mdm/menu.ts`, `src/core/menu/`(연결표, 사이드 메뉴 조립, 로딩, 가드, 테넌트/플랫폼 런타임)
- 추가: `doc/f5-wireframe.md`, `doc/f5-manual-check.md`
- 수정: `src/access.ts`가 `menus/my`의 actions로 판정한다. 공통코드·다국어·언어 화면은 `useAccess().can`을 직접 쓰고 `permission.ts` 3개는 삭제했다
- 수정: `src/app.tsx`, `RouteGuard`, 로그인·비밀번호 변경·로그아웃·언어 변경이 메뉴를 불러오거나 비운다
- 수정: `config/routes.ts`의 기준정보 화면은 `hideInMenu`다. 사이드 메뉴는 `menuDataRender`가 서버 메뉴를 앞에 붙인다. 예외 화면도 메뉴에서 뺐다. 환영은 그대로다
- 수정: `config/config.ts`가 플랫폼 빌드에서 `@/core/menu/runtime`을 빈 구현으로 바꾼다
- 백엔드는 수정하지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 139파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npx.cmd antd lint ./src`: 통과. 135파일, 이슈 0
- `npm.cmd run tsc`: 통과
- `npm.cmd run test`: 통과. 30파일, 141개. 약 93초
- `src/core/menu/flow.test.tsx`를 3회 반복 실행했다. 매번 11개 통과
- `npm.cmd run build`: 통과. 테넌트 산출물에 `mdm/common-code`, `mdm/i18n`, `mdm/language`가 있다
- `npm.cmd run build:platform`: `dist`와 `src/.umi-production`을 지운 뒤 통과. 산출물 전체에서 `menus/my`, `/mdm/common-code`, `/mdm/i18n`, `/mdm/language` 문자열이 없고 `tenants`가 있다
- 개발 서버와 백엔드는 띄우지 않았다

### 질문/확인 필요

사용자가 직접 볼 항목은 `doc/f5-manual-check.md` 맨 위에 모았다.

### 가정한 사항

- 서버 `routePath`는 쓰지 않고 `src/core/menu/menuRoutes.ts`만 쓴다
- 모르는 `iconName`은 `AppstoreOutlined`다. 아이콘은 표에 있는 것만 정적 import 한다
- 메뉴 응답이 401이거나 `AUTH.PASSWORD_CHANGE_REQUIRED`이면 다시 시도 안내를 띄우지 않는다. 401은 기존 로그인 이동을 호출한다
- 기준정보 라우트의 `name`은 빵 부스러기용으로 남기고, 사이드 메뉴에는 `hideInMenu`로 나오지 않게 했다

## [F6단계] 한 앱에서 접속 주소로 화면을 가름 - 2026-10-19

### 변경/추가 파일

- 추가: `src/core/auth/context.ts`, `src/core/auth/authApi.ts`, `src/pages/HomeRedirect.tsx`
- 추가: `doc/f6-wireframe.md`, `doc/f6-manual-check.md`
- 삭제: `src/pages/platform/login`, `src/pages/platform/password`, `src/core/menu/runtime.platform.tsx`, `config/routes.platform.ts`
- 수정: 로그인·비밀번호·로그아웃·세션 복구가 `authApi`로 kind별 인증 API를 호출한다
- 수정: `config/routes.ts`에 테넌트 화면과 `/tenants`를 같이 둔다. `/`는 런타임 홈으로 보낸다
- 수정: `config/config.ts`에서 `APP_MODE`, 라우트 선택, extra/runtime 별칭을 뺐다. title은 `PMIS`
- 수정: `package.json`에서 `dev:platform`, `build:platform`을 뺐다. 개발 포트 기본값은 8000
- 수정: `README.md`, `doc/local-setup.md`, `.env.local.example`
- 백엔드는 이 단계에서 수정하지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 137파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npm.cmd run test`: 통과. 29파일, 146개. 약 89초
- `npm.cmd run build`: 통과. 종료 코드 0. `welcome`, `tenants`, `mdm/language` HTML이 한 산출물에 있다
- 개발 서버와 백엔드는 띄우지 않았다. `dist`는 커밋하지 않는다

### 질문/확인 필요

- 플랫폼 문구는 기본 로케일 모듈에 합치지 않았다. kind가 PLATFORM일 때만 `extra.platform.ts`를 붙이고, 서버 `/i18n/bundles`는 테넌트일 때만 불러온다. 이 동작이 기존 플랫폼 콘솔과 같다.
- CI 설정에는 플랫폼 전용 빌드가 없었다. 예전 수동 확인 문서의 `build:platform` 문장은 그 단계 기록으로 남겨 두었다.
- 사용자가 직접 볼 항목은 `doc/f6-manual-check.md`에 있다.

### 가정한 사항

- 플랫폼 비밀번호 화면은 입력, 변경 강제, 취소, 로그아웃이 테넌트 화면과 같았다. 차이는 문구의 `defaultMessage`와 홈을 `/tenants`로 고정한 점뿐이었다. 합친 화면은 테넌트 화면이고, 홈은 context의 kind를 따른다.
- 플랫폼에서 `can()`은 항상 false다. 화면 진입은 kind가 PLATFORM이고 로그인한 경우 `viewFor` 한 곳에서 허용한다.
- 플랫폼 호스트가 아닌 잘못된 주소의 context 실패 코드 `TENANT.NOT_FOUND`, `TENANT.UNRESOLVED`, `TENANT.INACTIVE`는 모두 "알 수 없는 사이트입니다"다.

## [F7단계] 조직 화면 기반과 회사 관리 화면 - 2026-10-22

### 변경/추가 파일

- 추가: `src/services/mdm/company.ts`, `companyQuery.ts`, `companyRecord.d.ts`와 해당 테스트
- 추가: `src/core/codes/options.ts`, `useCodeOptions.ts`와 테스트
- 추가: `src/pages/org/shared/NameTranslations.tsx`, `useYn.ts`
- 추가: `src/pages/org/company`(목록, Drawer, 규칙, 흐름 테스트)
- 추가: `doc/f7-wireframe.md`, `doc/f7-manual-check.md`
- 수정: `src/core/menu/menuRoutes.ts`에 `COMPANY` → `/org/company`
- 수정: `config/routes.ts`에 `/org/company`
- 수정: `src/core/auth/context.ts`의 `areaForbidden`. 플랫폼 접속은 `/org`와 `/org/**`도 막는다
- 수정: `ko-KR`/`en-US`의 `menu.ts`, `pages.ts`에 회사 문구
- 수정: `src/core/menu/flow.test.tsx`가 회사 메뉴를 알려진 화면으로 기대한다
- 공통코드 화면과 백엔드는 수정하지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 149파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npx.cmd antd lint ./src`: 통과. 150파일, 이슈 0
- `src/pages/org/company/flow.test.tsx`를 3회 반복 실행했다. 매번 3개 통과
- `npm.cmd run test`: 통과. 35파일, 158개. 약 106초
- `npm.cmd run build`: 통과. 한 산출물에 `org/company`와 `tenants`가 있다
- `build:platform` 스크립트는 F6에서 제거되어 실행하지 않았다
- 개발 서버와 백엔드는 띄우지 않았다

### 질문/확인 필요

- 합의 화면과 같다. 이름 번역은 공통코드 화면을 고치지 않고 `NameTranslations`를 새로 만들었다. 공통코드 번역은 나중에 이 컴포넌트로 옮길 수 있다.
- 수정 API는 요청에 없는 주소·전화·설명 등을 null로 덮어쓴다. 화면에는 그 입력칸이 없고, 수정 저장 때 상세에 있던 값을 다시 보낸다.
- 사용자가 직접 볼 항목은 `doc/f7-manual-check.md` 맨 위에 모았다.

### 가정한 사항

- 목록은 공통코드 화면과 같이 antd `Table`이다. 지시서의 ProTable 문구보다 기존 화면 방식을 따랐다.
- 회사 유형은 API가 필수라 폼에서도 필수로 둔다.
- 국가 목록 API에는 `useYn`이 없다. 사용 중 국가만 화면에서 고른다. `nationListParams`는 바꾸지 않았다.
- 플랫폼과 테넌트는 빌드가 하나다. `/org/company`는 그 라우트에 있고, 플랫폼 사용자는 `areaForbidden`으로 들어가지 못한다. 회사 API 모듈은 `services/mdm`에 둔다.

## [F8단계] 부서 관리 화면 - 2026-10-22

### 변경/추가 파일

- 추가: `src/services/mdm/dept.ts`, `deptQuery.ts`, `deptRecord.d.ts`, `people.ts`, `peopleRecord.d.ts`와 부서 서비스·쿼리 테스트
- 추가: `src/pages/org/dept`(트리, 상세 탭, Drawer, 규칙, 흐름 테스트)
- 추가: `doc/f8-wireframe.md`, `doc/f8-manual-check.md`
- 수정: `src/core/menu/store.ts`. `noteLoggedOut`에서 `resetCodeOptionsCache`를 호출한다
- 수정: `src/pages/org/company/rules.ts`. 회사 오류 문구를 i18n 키로 옮겼다. 서버 `errorMessage`가 있으면 그 문구를 쓴다
- 수정: `src/core/menu/menuRoutes.ts`에 `DEPT` → `/org/dept`
- 수정: `config/routes.ts`에 `/org/dept`
- 수정: `ko-KR`/`en-US`의 `menu.ts`, `pages.ts`에 부서 문구와 회사 오류 키
- 수정: `vitest.config.ts`. 화면 흐름 테스트가 파일 병렬에서 시간 제한에 걸려 `fileParallelism: false`, `testTimeout: 30000`
- 공통코드·다국어 화면과 백엔드는 수정하지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 157파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npx.cmd antd lint ./src`: 통과. 164파일, 이슈 0
- `src/pages/org/dept/flow.test.tsx`를 3회 반복 실행했다. 매번 5개 통과(약 61초, 62초, 66초)
- `npm.cmd test`: 통과. 40파일, 171개. 약 262초
- `npm.cmd run build`: 통과. 종료 코드 0. 한 산출물에 `org/dept`, `org/company`, `tenants`가 있다
- `build:platform` 스크립트는 F6에서 제거되어 실행하지 않았다
- 개발 서버와 백엔드는 띄우지 않았다

### 질문/확인 필요

- 합의 와이어프레임 3절은 소속 인원 행 클릭 시 사람 화면으로 간다. F8은 사람 화면이 없어 행 이동을 만들지 말라고 해서 그대로 두었다. `doc/f8-wireframe.md`에 적었다.
- 변경 이력의 상위 부서는 PK만 온다. 이름 조회 API가 없어 PK로 보이고, 안내 문구를 붙였다.
- 부서 수정 API는 요청에 없는 `description`을 null로 덮어쓴다. 화면 입력칸은 없고, 수정 저장 때 상세 값을 다시 보낸다. 등록 때는 보내지 않는다.
- 부서 코드는 회사 코드와 달리 정규식이 없다. 폼은 길이 1–50만 본다.
- 회사 Select는 사용 중 회사 첫 페이지(size 200)만 담는다. 200건을 넘으면 뒤 페이지는 목록에 없다.
- 사용자가 직접 볼 항목은 `doc/f8-manual-check.md` 맨 위에 모았다.

### 가정한 사항

- 코드 목록 응답은 `CommonCodeService.toResponse`가 언어별 번역을 모두 싣는다. 화면은 `codeOptionLabel`이 현재 언어를 고르므로 언어 변경 때 캐시를 비우지 않는다. 로그아웃, 401 로그인 이동, 다른 탭 로그아웃은 모두 `noteLoggedOut`을 타서 캐시를 비운다.
- 이력·소속 인원 표는 회사 화면과 같이 antd `Table`이다.
- 종료는 본문 없이 DELETE 한다. 서버가 종료일을 오늘로 둔다.
- 상위 부서 후보는 현재 트리의 사용 중 행이다. 목록 API는 두었고 이 화면은 호출하지 않는다.
- 기준일과 종료 포함은 [조회]를 누를 때 적용한다. 회사를 바꾸면 선택을 비우고 트리를 다시 불러온다.
- 이름 번역은 F7 `NameTranslations`를 그대로 쓴다. 공통코드 화면은 고치지 않았다.
- 트리는 `motion={false}`, `virtual={false}`다. 짧은 조직도가 펼친 뒤 바로 보이게 하기 위해서다.
- 플랫폼과 테넌트는 빌드가 하나다. `/org/dept`는 그 라우트에 있고, 플랫폼 사용자는 기존 `areaForbidden`이 `/org/**`를 막는다.

## [F9단계] 사람 관리 화면 - 2026-10-22

### 변경/추가 파일

- 추가: `src/pages/org/person`(목록, Drawer, 소속 폼, 프로필, 규칙, 흐름 테스트)
- 추가: `doc/f9-wireframe.md`, `doc/f9-manual-check.md`
- 확장: `src/services/mdm/people.ts`, `peopleQuery.ts`, `peopleRecord.d.ts`. 소속 API를 포함한다
- 추가: `src/services/mdm/files.ts`. 업로드와 프로필 이미지
- 수정: `src/core/request/runtime.ts`, `errors.ts`. FormData 의 수동 Content-Type 을 빼고, 이미지 응답은 blob 으로 받는다
- 수정: `src/pages/org/dept/index.tsx`. 이력·소속 인원의 늦은 응답을 버리고, 변경 유형 문구는 번들 키만 쓴다. 소속 인원 행은 `/org/person?personPk=` 로 이동한다
- 수정: `src/core/menu/menuRoutes.ts`에 `PERSON` → `/org/person`
- 수정: `config/routes.ts`에 `/org/person`
- 수정: `ko-KR`/`en-US`의 `menu.ts`, `pages.ts`에 사람 문구
- 공통코드·다국어 화면과 백엔드는 수정하지 않았다. `vitest.config.ts` 는 바꾸지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 167파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npx.cmd antd lint ./src`: 통과. 177파일, 이슈 0
- `src/pages/org/person/flow.test.tsx`를 3회 반복 실행했다. 매번 5개 통과(약 21초, 21초, 21초)
- `npm.cmd test`: 통과. 46파일, 189개. 약 262초
- `npm.cmd run build`: 통과. 종료 코드 0. 한 산출물에 `org/person`이 있다
- `build:platform` 스크립트는 F6에서 제거되어 실행하지 않았다
- 개발 서버와 백엔드는 띄우지 않았다

### 질문/확인 필요

- 합의 와이어프레임은 계정 연결을 이 화면에서 읽기만 한다고 했다. F9는 읽기 전용 표시도 하지 말라고 해서 계정 정보는 두지 않았다.
- 소속 API 권한은 지시서의 "모두 UPDATE"와 다르다. 등록은 `PERSON:CREATE`, 수정·종료·복구는 `UPDATE`, 삭제는 `DELETE`다. 명세를 따랐다.
- 프로필 설정 본문 필드는 `fileId`다. 업로드 응답의 `filePk`를 넣는다.
- 사람 수정은 사무실 전화·설명을, 소속 수정은 설명을 상세 값으로 다시 보낸다. 화면 입력칸은 없다.
- 같은 회사·부서의 현재 소속 중복은 부서 칸, 사원번호 중복은 사원번호 칸에 둔다.
- 회사 Select는 사용 중 회사 첫 페이지(size 200)만 담는다.
- 클라이언트 크기 검사는 문서의 기본 상한 50MB다. 저장소 한도가 더 작으면 서버 문구를 보여 준다.
- 전체 테스트는 약 262초로 F8과 비슷하다. 사람 흐름 파일은 약 21초다. 테스트 설정은 바꾸지 않았다.
- 사용자가 직접 볼 항목은 `doc/f9-manual-check.md` 맨 위에 모았다.

### 가정한 사항

- 목록·소속 표는 회사 화면과 같이 antd `Table`이다.
- 고용 유형과 재직 상태는 API가 필수라 폼에서도 필수로 둔다.
- 사진이 없다고 표시된 사람은 이미지 GET을 하지 않는다. 있다고 표시된 뒤 404면 사진 없음으로 둔다.
- 부서 화면의 변경 유형 한글은 컴포넌트에 두지 않고 `ko-KR`/`en-US` 번들만 쓴다.
- 플랫폼과 테넌트는 빌드가 하나다. `/org/person`은 그 라우트에 있고, 플랫폼 사용자는 기존 `areaForbidden`이 `/org/**`를 막는다.

## [F10단계] 역할 관리 화면 - 2026-10-29

### 변경/추가 파일

- 추가: `src/pages/access/role`(목록, Drawer, 권한 표, 규칙, 흐름 테스트)
- 추가: `src/services/mdm/roles.ts`, `rolesQuery.ts`, `rolesRecord.d.ts`
- 추가: `doc/f10-wireframe.md`, `doc/f10-manual-check.md`
- 수정: `src/pages/org/person/PersonDrawer.tsx`. 소속 조회 의존성을 `personPk`로 바꿨다
- 수정: `src/core/menu/menuRoutes.ts`에 `ROLE` → `/access/role`
- 수정: `src/core/auth/context.ts`. 플랫폼은 `/access/**`도 막는다
- 수정: `config/routes.ts`에 `/access/role`
- 수정: `ko-KR`/`en-US`의 `menu.ts`, `pages.ts`에 역할 문구
- 사용자 화면, 권한 관리 화면, 백엔드는 수정하지 않았다. `vitest.config.ts`는 바꾸지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 175파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npm.cmd exec antd -- lint ./src`: 통과. 188파일, 이슈 0
- `src/pages/access/role/flow.test.tsx`를 3회 반복 실행했다. 매번 4개 통과(약 21초, 21초, 21초)
- `npm.cmd test`: 통과. 50파일, 200개. 약 299초
- `npm.cmd run build`: 통과. 종료 코드 0. 한 산출물에 `access/role`이 있다
- 개발 서버와 백엔드는 띄우지 않았다

### 질문/확인 필요

- 초안은 테넌트 관리자 권한 편집을 숨기고, 지시서는 표를 읽기 전용으로 보이라고 했다. 지시서를 따랐다. 차이는 `doc/f10-wireframe.md`에 적었다.
- 메뉴 이름은 응답 `menuName`이고, 기능 열 이름은 로컬 문구다. 없는 기능 코드는 코드 그대로다.
- 부서 화면의 이력·소속 인원 조회는 숫자 PK만 의존한다. 회사 목록은 같은 키로 건너뛰지 않는다. 소속 표가 비는 패턴은 사람 Drawer만 고쳤다.
- 전체 테스트는 약 299초다. 역할 흐름 파일은 약 21초다. 테스트 설정은 바꾸지 않았다.
- 사용자가 직접 볼 항목은 `doc/f10-manual-check.md` 맨 위에 모았다.

### 가정한 사항

- 목록은 회사 화면과 같이 antd `Table`이다. Drawer 너비는 기존 `size="large"`다.
- 부모 메뉴 그룹은 서버 `menus/my`의 `parentMenuCode`를 그대로 쓴다. 화면 코드에서 그룹을 가정하지 않았다.
- 권한 조회는 `granted`를 보내지 않아 메뉴의 사용 중 권한을 모두 받는다. 메뉴 이름은 서버가 `Accept-Language`로 고른 값이다.
- 역할 수정 필드는 모두 화면에 있다. 빈 설명은 null이다.
- 플랫폼과 테넌트는 빌드가 하나다. `/access/role`은 그 라우트에 있고, 플랫폼 사용자는 `/access/**`를 막는다.

## [F11단계] 사용자 관리 화면 - 2026-10-29

### 변경/추가 파일

- 추가: `src/pages/access/user`(목록, Drawer, 역할 편집, 규칙, 흐름 테스트)
- 추가: `src/services/mdm/users.ts`, `usersQuery.ts`, `usersRecord.d.ts`
- 추가: `src/components/IssuedPasswordModal.tsx`
- 추가: `doc/f11-wireframe.md`, `doc/f11-manual-check.md`
- 수정: 부서 이력 상위 부서 표시. `deptRecord.d.ts`, `dept/historyLabel.ts`, `dept/index.tsx`
- 수정: `src/core/menu/menuRoutes.ts`에 `USER` → `/access/user`
- 수정: `config/routes.ts`에 `/access/user`
- 수정: `ko-KR`/`en-US`의 `menu.ts`, `pages.ts`에 사용자 문구
- 권한 관리 화면과 백엔드는 수정하지 않았다. `vitest.config.ts`는 바꾸지 않았다

### 검증 결과

- `npm.cmd run lint`: 통과. biome 186파일, 이어서 `tsc --noEmit`. 종료 코드 0
- `npm.cmd exec antd -- lint ./src`: 통과. 201파일, 이슈 0
- `src/pages/access/user/flow.test.tsx`를 3회 반복 실행했다. 매번 3개 통과(약 29초, 28초, 30초)
- `npm.cmd test`: 통과. 54파일, 212개. 약 367초
- `npm.cmd run build`: 통과. 종료 코드 0. 한 산출물에 `access/user`가 있다
- 개발 서버와 백엔드는 띄우지 않았다

### 질문/확인 필요

- 초안은 계정이 없는 사람을 우선하라고 했다. 지시서는 사용 중 사람을 검색하고, 이미 연결된 사람은 서버 오류를 사람 칸에 보이라고 했다. 지시서를 따랐다. 차이는 `doc/f11-wireframe.md`에 적었다.
- 수정 DTO에 `clearPerson`이 있으나 서비스는 이 값을 쓰지 않는다. 사람을 비우면 `personPk: null`만 보낸다.
- `ROLE.INACTIVE`의 필드는 `roleCode`라 어느 역할인지 알 수 없다. 편집 창 안내에 보인다.
- 플랫폼 임시 비밀번호 모달은 로그인 아이디 복사도 있어 그대로 두었다. 사용자 화면은 `IssuedPasswordModal`을 새로 만들었다.
- 전체 테스트는 약 367초다. 파일별 시간은 보고서에 적었다. 테스트 설정은 바꾸지 않았다.
- 사용자가 직접 볼 항목은 `doc/f11-manual-check.md` 맨 위에 모았다.

### 가정한 사항

- 목록은 회사 화면과 같이 antd `Table`이다. Drawer 너비는 `size="large"`다.
- 부모 메뉴 그룹은 서버 `menus/my`의 `parentMenuCode`를 그대로 쓴다.
- 생성일 정렬은 API가 받지만 이 단계 열에는 없다. 화면 정렬은 로그인 아이디, 상태, 마지막 로그인만이다.
- 역할 조회 API는 페이지가 없다. 탭을 처음 열 때 한 번 받고, 같은 키면 다시 받지 않는다.
- 플랫폼과 테넌트는 빌드가 하나다. `/access/user`는 그 라우트에 있고, 플랫폼 사용자는 `/access/**`를 막는다.

## [F11.1단계] 개발 서버 시작 오류 수정 - 2026-10-29

### 변경/추가 파일

- 수정: `src/core/dev/runDev.ts`의 `./devPort.ts`, `./localEnv.ts`, `./proxyRules.ts`
- 수정: `src/core/dev/proxyRules.ts`의 `../request/tenantHeader.ts`
- `devPort.ts`, `localEnv.ts`, `tenantHeader.ts`는 상대 import가 없어 본문은 그대로다
- 수정: `tsconfig.json`에 `noEmit`, `allowImportingTsExtensions`
- 수정: `package.json`의 `engines.node`를 `>=22.18.0 <23.0.0 || >=23.6.0`
- 수정: `doc/local-setup.md`에 Node 최소 버전과 `node -v`
- 추가: `devServer.test.ts`의 `dev launcher` 테스트
- 화면, 라우트, 라이브러리는 바꾸지 않았다

### 검증 결과

- 수정 전 `node src/core/dev/runDev.ts`는 `ERR_MODULE_NOT_FOUND`(`devPort`)로 끝났다. `node -v`는 v24.13.0
- `npm.cmd run lint`: 통과. biome 185파일, `tsc --noEmit`. 종료 코드 0
- `npm.cmd exec antd -- lint ./src`: 통과. 201파일, 이슈 0
- `dev launcher` 테스트: 1개 통과. 약 170ms. 종료 코드 1, 포트 점유 문구, `ERR_MODULE_NOT_FOUND` 없음
- `npm.cmd test`: 통과. 54파일, 213개. 약 343초. 이전 212개보다 테스트 1개. 파일 수는 같다
- `npm.cmd run build`: 통과. 종료 코드 0
- `npm.cmd run dev`는 시작 로그(`ready in`)까지 확인한 뒤 프로세스 트리를 종료했다. `runDev`/`max` 프로세스는 남지 않았다. 백엔드는 띄우지 않았다

### 질문/확인 필요

- 23.0부터 23.5는 타입 제거가 기본이 아니다. `engines`는 그 구간을 빼는 범위로 올렸다.
- `tsc`가 `.ts` import를 TS5097로 거부해 `allowImportingTsExtensions`와 `noEmit`을 켰다. `npm run tsc`는 원래 `--noEmit`이다.

### 가정한 사항

- Node 공식 문서 기준 타입 제거 기본 활성은 22.18.0과 23.6.0이다. 이 머신에서 재현한 버전은 v24.13.0이다.
- vitest가 도는 테스트 import와 `config/proxy.ts`의 import는 확장자를 붙이지 않았다. Node가 직접 읽는 사슬만 `.ts`다.
- 빌드가 통과해 번들러는 `proxyRules.ts` 안의 `tenantHeader.ts` import를 받아들인다.


