# Cursor AI 작업 지시서 (frontend)

이 파일은 Cursor AI가 읽고 수행하는 FE 작업 지시서이다.
사용자가 "이 파일의 FN단계를 수행해줘"(또는 "frontend/doc/cursor-handoff-prompts.md 의 FN단계")라고 요청하면 **해당 단계 하나만** 수행한다. 다음 단계는 사용자가 요청할 때 진행한다.

## 공통 원칙 (모든 단계에 적용)
1. 저장소 루트 `CLAUDE.md`와 `.cursor/rules`를 먼저 읽고 따른다. 기준 문서는 `doc/fe-structure-proposal.md`(구조 합의 완료)이다.
2. 백엔드 API 명세(`backend/common/doc/api/mdm-api.md`, `platform-api.md`, `doc/auth-design.md`)가 기준이다. 명세에 없는 API/필드는 만들지 말고 질문 목록으로 정리한다.
3. 새 라이브러리 추가, 의존성 제거는 실행 전에 목록을 사용자에게 알리고 승인받는다.
4. 접속 정보, 토큰, 비밀 값, 서버 주소를 코드·문서·로그·채팅에 쓰지 않는다.
5. 되돌릴 수 없는 스크립트(`npm run simple`, `npm run i18n-remove`)는 실행하지 않는다.
6. 화면 구성(F2~)은 구현 전에 와이어프레임 수준으로 사용자 확인을 받는다.
7. **(2026-10-19 폐기: 아래 "작업 방식"이 대체한다.) 예전 규칙: 사용자가 요청하기 전에는 git 커밋·브랜치 생성·푸시를 하지 않는다.** 변경은 작업 트리에 그대로 두고, 사용자가 diff를 검토한 뒤 직접 커밋한다. (커밋을 요청받으면 Conventional Commits를 따른다.) 단계 끝에 `npm run lint`, `npx antd lint ./src`, `npm run tsc`, `npm run test`를 실행하고 결과를 기록한다. 실행하지 못한 검증은 이유를 적는다.
8. 단계가 끝나면 `doc/cursor-progress.md`(없으면 생성)에 아래 형식으로 **추가**하고 멈춘다.

```
## [FN단계] 단계 제목 - YYYY-MM-DD
- 변경/추가/삭제 파일: (목록. 삭제는 묶어서 요약)
- 검증 결과: (명령과 결과)
- 질문/확인 필요: (없으면 "없음")
- 가정한 사항: (없으면 "없음")
```


## 작업 방식 (2026-10-19 변경. 아래 옛 단계의 "커밋·브랜치·푸시 금지", "PR" 문구보다 우선한다)
1. 현재 브랜치 `main`에서 직접 작업하고 커밋한다. `step/...` 브랜치 생성, PR 생성, merge는 하지 않는다.
2. **push는 사용자가 "push 해줘"라고 요청할 때만** 한다(`git push origin main`만, force push·브랜치 삭제·이력 재작성 금지).
3. **시작 전 git 정리와 확인** (정리가 안 되거나 아래가 어긋나면 작업하지 말고 상황만 보고하고 멈춘다):
   - 현재 브랜치가 `main` 이 아니면(이전 단계의 `step/...` 브랜치가 남아 있는 경우가 많다) 먼저 `git fetch origin` 후 `git switch main` 으로 전환한다. 미커밋 변경은 그대로 따라온다. 전환이 충돌로 거절되면 `git stash push -u`, `git switch main`, 아래 pull, `git stash pop` 순서로 하고, 그래도 안 되면 멈춘다. 이전 `step/...` 브랜치는 삭제하지 않는다.
   - `git pull --ff-only` 성공(원격에 merge 된 이전 PR 이 있으면 여기서 반영된다).
   - `git status` 에서 Claude 작성 파일 밖의 미커밋 변경이 없다. Claude 작성 파일은 이 지시서 파일, `docs/reviews/` 안의 파일, `AGENTS.md`, `docs/WORKFLOW.md`, `.gitignore` 이다. 이 파일들의 미커밋 변경은 고치지 말고 시작 전에 `docs: Claude 지시서·규칙 갱신` 커밋으로 따로 커밋한다. 커밋 전에 `AGENTS.md`, `docs/WORKFLOW.md` 를 읽는다.
   - 시작 커밋 해시를 `git rev-parse --short HEAD` 로 기록한다.
4. **커밋**: 이 단계의 변경을 `[<단계>] 요약` 형식으로 커밋한다(여러 개로 나눠도 된다). 진행 기록과 보고서도 포함한다. `.env.local` 등 로컬 설정, zip, `*.diff` 는 커밋하지 않는다(`git status` 로 확인).
5. **보고서**: `docs/reports/<단계>.md` 를 쓰고(형식은 아래), `git diff <시작 커밋>..HEAD > docs/reports/<단계>.diff` 를 만든다. diff 는 `.gitignore` 의 `*.diff` 대상이라 커밋되지 않는 것이 정상이다. Claude가 이 두 파일을 읽고 검토한다.
6. 지시서 파일은 삭제하거나 수정하지 않는다. `docs/prompts/` 폴더는 쓰지 않는다(`be-8.7.md` 는 옛 기록으로 남김).
7. 보고서 형식:
   - a. 기본 정보: 시작 커밋, 이 단계 커밋 목록(`git log --oneline <시작>..HEAD`), 변경 파일(`git diff --stat <시작>..HEAD`)
   - b. 요구사항 체크리스트: 지시서 항목별 "충족/부분/미충족"과 근거(파일·테스트 이름)
   - c. 핵심 코드: 검토에 필요한 부분
   - d. 테스트·검증 결과: 명령별 결과와 개수
   - e. 가정, 질문, 알려진 한계, 지시에서 벗어난 점(없으면 "없음")
   마지막에 `git status` 로 의도하지 않은 파일이 커밋되거나 남지 않았는지 확인한 결과를 한 줄 쓴다.

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

---

## F4.1단계: 지원 언어·국가 관리 화면 + 메시지 표 번역 요약 연결

F4 승인(검토 기록 `docs/reviews/2026-10-14-F4.md`), 백엔드 8.7 머지됨. 기준 문서: `backend/common/doc/api/mdm-api.md`(다국어 관리, 국가, 지원 언어, 오류 코드), `doc/fe-structure-proposal.md`, F4 구현(`src/pages/mdm/i18n/`, `src/services/mdm/`). 이 단계만 수행하고 멈춘다. 시작 전 `docs/prompts/fe-F4.1.md` 가 있으면 폐기된 파일이므로 삭제하고 위 시작 전 커밋에 포함한다.

````
### 범위

#### 1-A. F4 메시지 표의 번역 상태 열을 백엔드 8.7 응답에 연결
백엔드 8.7로 `GET /i18n-messages` 목록 항목에 `translatedLanguageCodes`, `missingLanguageCodes`, `translationStatusCodes` 가 생겼다(`mdm-api.md` 다국어 관리 절). 목록의 `translations` 는 여전히 빈 배열이다.
- 타입(`src/services/mdm/` 의 메시지 타입)에 세 필드를 **선택 필드**로 추가한다(단건·등록·수정 응답에는 없다).
- 번역 상태 열: 번역된 언어는 일반 태그, 누락 언어(`missingLanguageCodes`)는 눈에 띄는 경고 태그로 표시한다. 누락이 없으면 "완료" 취지의 표시. 상태 코드(`translationStatusCodes`)는 있으면 보조로 보여 준다(예: DRAFT 포함 시 표시). 필드가 없는 응답이면 기존처럼 `—` 를 보여 준다(깨지지 않아야 한다).
- 행마다 단건 조회를 하지 않는다. 목록 호출 수가 늘면 안 된다.
- 문구는 모두 i18n 키(ko-KR/en-US 로컬 번들).

#### 1-B. 지원 언어·국가 관리 화면
- 경로 `/mdm/language`, 화면 제목 "언어·국가 관리". 메뉴 코드 `I18N`(국가 관리도 `I18N` 권한을 쓴다), 동작 READ/CREATE/UPDATE/DELETE. 권한은 F3·F4와 같은 임시 권한 파일 한 곳을 같은 방식으로 쓴다. 서버 403 `AUTH.FORBIDDEN` 은 안내로 보여 준다.
- 테넌트 앱 전용이다. 플랫폼 콘솔 라우트·빌드에 섞이지 않는다.
- 구현 전에 `frontend/doc/f4.1-wireframe.md` 에 아래 화면을 텍스트 도식으로 옮겨 쓴다. 아래 합의와 다른 점이 생기면 구현 전에 멈추고 보고서 "질문"에 올린다(그 부분은 구현하지 않는다).

화면 구성(합의됨):

```
[언어·국가 관리]                                     ?tab=language | nation
 ┌ [지원 언어] [국가] ──────────────────────────────────────┐
 │ (지원 언어 탭)                                            │
 │  사용 여부 필터 [전체▾]                  [언어 등록]       │
 │  코드 | 언어명 | 현지 표기 | 기본 | 대체 언어 | 방향 |    │
 │       | 정렬 | 사용 | 동작[수정][사용 중지|복구]            │
 │ (국가 탭)                                                 │
 │  검색 [keyword] 사용 여부 [전체▾]        [국가 등록]       │
 │  국가 코드 | 국가명 | 기본 언어 | 정렬 | 사용 | 동작       │
 │  서버 페이징(페이지 20)                                    │
 └───────────────────────────────────────────────────────────┘
 등록/수정은 우측 Drawer. 탭 선택은 URL 쿼리 ?tab= 에 반영, 새로고침해도 유지.
```

**지원 언어 탭**
- 목록: `GET /support-languages`(배열, 페이징 없음). 필터 `useYn` 을 생략하면 사용 중지 포함 전체이므로 기본은 생략("전체"). 사용 중지 행은 흐리게, 기본 언어 행은 "기본" 태그.
- 등록/수정 Drawer 필드는 **백엔드 DTO 를 읽기 전용으로 확인해** 정한다(`backend/common/src/main/java/.../i18n/` 의 지원 언어 관련 Controller·요청 DTO). `mdm-api.md` 에 요청 본문 필드가 없으므로 추측하지 않는다. DTO 와 문서가 다르거나 모르는 필드가 있으면 보고서 "질문"에 적고 그 필드는 만들지 않는다.
- 규칙(문서 기준):
  - 언어 코드는 BCP 47 이고 **등록 후 수정 불가**(수정 Drawer 에서 읽기 전용).
  - 기본 언어는 활성 1개다. 다른 언어를 기본으로 바꾸면 서버가 이전 기본을 내린다. 저장 전 확인창에 "기존 기본 언어가 해제된다"를 쓴다.
  - 대체 언어 선택지: 활성 언어 중 자기 자신 제외. 서버가 순환을 막는다(400 `I18N.FALLBACK_CYCLE`).
  - [사용 중지]는 기본 언어 행에는 보이지 않는다(FE 1차). 그 외에는 서버가 판단하며 409 `I18N.LANGUAGE_IN_USE`(기본 언어이거나 다른 활성 언어의 대체 언어) 와 400 `I18N.DEFAULT_REQUIRED` 의 서버 문구를 그대로 보여 준다(문구가 비면 로컬 문구).
  - 중지 확인창에 "중지하면 번들 조회·언어 선택에서 빠진다"를 쓴다.
- 저장·중지·복구에 성공하면 이 화면이 쓰는 언어 목록뿐 아니라 **앱 전체의 언어 선택 목록(로그인·헤더의 언어 선택, F4 메시지 번역 탭의 언어 목록)이 다음 사용 시 새로 불러와지게** 한다. 기존 `src/core/i18n` 에 언어 목록 캐시가 있으면 무효화 함수를 쓰고, 없으면 만들지 말고 어떻게 되는지(재로그인 필요 등)만 보고서에 쓴다.

**국가 탭**
- 목록: `GET /nations?managed=true`(페이지, page 는 1부터). 정렬 허용 필드는 `nationCode`, `nationName`, `sortOrder`, `nationPk` 만 열고 FE 는 그대로 전달한다. 검색·필터 파라미터는 DTO/Controller 를 읽기 전용으로 확인해 있는 것만 쓴다.
- 국가 코드는 ISO 3166-1 alpha-2 **대문자 2자**이고 등록 후 수정 불가. 입력은 자동으로 대문자로 바꾸고 `^[A-Z]{2}$` 를 FE 1차 검증한다.
- `defaultLanguageCode` 는 활성 지원 언어에서 선택한다(활성 언어 목록 사용).
- Drawer 필드는 DTO 를 읽기 전용으로 확인해 정한다(위와 같은 원칙). 오류: 404 `I18N.NATION_NOT_FOUND`, 409 `I18N.DUPLICATE`, 400 fieldErrors 는 해당 입력 아래에 표시, 서버 문구 우선.
- 사용 중지(논리 삭제)·복구는 확인창에 논리 삭제(`use_yn='N'`)임을 쓰고 실행 후 목록을 다시 불러온다.

**공통 처리**
- F3·F4의 패턴을 재사용한다: `src/pages/mdm/` 아래 새 폴더(예: `language/`), 서비스는 `src/services/mdm/` 에 지원 언어·국가 함수를 추가(기존 `i18n` 서비스 파일을 늘리거나 새 파일, 중복 최소). 컴포넌트에서 fetch/axios 직접 호출 금지, `src/core/request` 사용.
- 저장 중 버튼 비활성과 연타 방지(요청 1회), 목록 새로고침 시 탭·필터 유지.
- 문구·열·버튼·오류는 모두 i18n 키(ko-KR/en-US 로컬 번들).
- 새 라이브러리는 추가하지 않는다.

### 테스트 (vitest, F3.1·F4 방식. 서비스 목으로 실제 네트워크 호출 0건을 매 테스트 후 확인)

- 1-A: 세 필드가 있는 응답에서 누락 언어 경고 태그, 필드가 없는 응답에서 `—`, 목록 호출 외 추가 호출 없음.
- 규칙 단위: 국가 코드 정규화·검증, 지원 언어/국가 폼 → 요청 본문 변환, 오류 코드 → 문구, 쿼리 파라미터(page/size/sort/필터).
- 흐름(언어 탭): 목록 호출(useYn 생략), 사용 중지 행 흐림과 복구 버튼, 등록, 수정에서 코드 읽기 전용, 기본 언어 변경 확인창 문구, 기본 언어 행에 중지 버튼 없음, 대체 언어 선택지에 자기 자신 없음, 중지 확인창 문구와 취소 시 호출 없음, 409 `LANGUAGE_IN_USE` 서버 문구/빈 문구 시 로컬 문구, 400 `FALLBACK_CYCLE`, 저장 연타 시 요청 1회, 성공 후 언어 캐시 무효화 호출.
- 흐름(국가 탭): 탭 전환과 `?tab=` 반영·새로고침 진입, 서버 페이징·정렬 호출, 코드 소문자 입력이 대문자로 바뀜, 형식 위반 시 저장 안 됨, 수정에서 코드 읽기 전용, 기본 언어 선택지는 활성 언어만, 사용 중지/복구와 재조회, 409 `DUPLICATE`, 400 fieldErrors, 403 안내, 목록 실패 시 탭·필터 유지.
- 새 흐름 테스트 파일은 3회 반복 실행해 모두 통과함을 기록한다.

### 검증

`npm.cmd run lint`, `npx.cmd antd lint ./src`, `npm.cmd run tsc`, `npm.cmd run test`(실행 시간·통과 수), `npm.cmd run build`(테넌트 산출물에 `mdm/language` 경로 있음), `npm.cmd run build:platform`(플랫폼 산출물에 이 화면이 없음). 개발 서버와 백엔드는 띄우지 않는다. 사용자가 직접 볼 항목(실제 서버 반영, 서버 오류 문구, 좁은 화면, 언어 변경이 로그인·헤더 언어 선택에 반영되는지, 사용감)만 `frontend/doc/f4.1-manual-check.md` 맨 위에 모으고 나머지는 자동 테스트 이름을 표시한다. 값(주소, 계정, 비밀번호)은 쓰지 않는다.

### 만들지 않는 것
- 국가·언어의 다국어 이름 편집, 일괄 가져오기/내보내기, 누락 번역 필터(백엔드에 없음), 메뉴·권한의 서버 연동(F5), 플랫폼 콘솔 화면.

### 기록
`frontend/doc/cursor-progress.md` 에 `[F4.1단계] 지원 언어·국가 관리 - 날짜` 형식(변경 파일, 검증 결과, 질문/확인 필요, 가정)으로 추가한다. 보고서는 파일 앞쪽 "작업 방식"의 형식으로 `docs/reports/F4.1.md` 에 쓰고 `docs/reports/F4.1.diff` 를 만든다. 커밋 메시지는 `[F4.1] 요약`. push 는 하지 않는다. 완료 후 멈춘다.
````

---

## F5단계: 서버 메뉴(menus/my)와 실제 권한 연결

F4.1 승인(`docs/reviews/2026-10-19-F4.1.md`). 사용자 결정(2026-10-19): 사이드 메뉴는 서버 `menus/my` 로 만들고, 화면·버튼 권한도 같은 응답의 `actions` 로 판정한다. 대상은 테넌트 앱(`APP_MODE=tenant`)이다. 플랫폼 콘솔은 메뉴가 정적 하나라 바꾸지 않는다. 이 단계만 수행하고 멈춘다.

````
기준 문서: backend/common/doc/api/mdm-api.md 의 menus/my 절(응답 필드, 노출 규칙, 오류), doc/auth-design.md, 현재 코드 src/access.ts, src/core/menu/types.ts, src/app.tsx(layout, SessionBootstrap), src/core/auth/actions.ts, src/components(RouteGuard, UserMenu, LanguageSelect), config/routes.ts. 명세에 없는 필드·동작은 만들지 말고 질문으로 올린다. 구현 전에 frontend/doc/f5-wireframe.md 에 아래 동작을 텍스트로 정리한다(화면 레이아웃은 바뀌지 않으므로 와이어프레임은 메뉴 트리 예시와 권한 판정 표 정도). 아래 합의와 다른 점이 생기면 구현 전에 멈추고 보고서 "질문"에 올린다.

1. 메뉴 로딩 (src/core/menu)
   - GET /api/mdm/v1/menus/my 를 호출하는 서비스 함수(src/services/mdm/menu.ts)와 타입(menuCode, parentMenuCode, menuName, menuTypeCode, routePath, iconName, levelNo, sortOrder, actions)을 만든다. 응답은 배열이다(페이징 없음).
   - 로딩 시점: ① 로그인 성공 후(me 조회 다음) ② 앱 시작 세션 복구 성공 후 ③ 언어 변경 후(메뉴 이름이 요청 언어로 오므로 다시 받는다) ④ 비밀번호 변경 성공 후(강제 변경이 끝나 passwordChangeRequired 가 풀린 시점). 비밀번호 변경이 강제된 상태에서는 호출하지 않는다(서버가 403 AUTH.PASSWORD_CHANGE_REQUIRED). 로그아웃, 401로 인한 로그인 이동, 다른 탭 로그아웃 수신 시 메뉴 상태를 비운다.
   - 메뉴는 initialState 또는 별도 모듈 상태에 둔다. 토큰이 아니므로 메모리 상태면 충분하다. 브라우저 저장소에 저장하지 않는다.
   - 로딩 실패(네트워크, 5xx)는 화면을 막지 않는다. 사이드 메뉴 위치에 오류 안내와 [다시 시도] 버튼을 보이고, 권한 판정은 모두 false(가장 안전한 쪽)로 둔다. 401 계열은 기존 요청 계층 규칙(refresh, 로그인 이동)을 그대로 따른다.
   - 동시에 여러 번 호출되지 않게 한다(진행 중이면 같은 요청을 재사용).

2. 서버 메뉴 → FE 라우트 연결표 (한 곳에만 둔다: src/core/menu/menuRoutes.ts)
   - 서버 Seed 의 route_path 는 /system/<소문자코드> 라서 FE 라우트와 다르다. 서버 routePath 는 쓰지 않고 FE 가 가진 표로 연결한다.
   - 표 항목: { menuCode, path, labelKey, primary }. 지금 FE 화면이 있는 것만 둔다: COMMON_CODE → /mdm/common-code, I18N → /mdm/i18n (primary), I18N → /mdm/language (같은 메뉴 코드의 두 번째 화면. 국가 관리도 I18N 권한). 새 화면이 생기면 이 표에 한 줄 추가하는 것으로 끝나게 한다.
   - 사이드 메뉴 구성: menus/my 응답에 menuCode 가 있는(= READ 가 있는) 항목만 표에서 찾아 보인다. 서버 그룹 메뉴(예: SYSTEM)는 표시 이름을 서버 menuName 으로 쓰고, 그 아래에 화면 항목을 둔다(표의 항목이 속할 부모는 서버 응답의 parentMenuCode 로 정한다). 화면 항목의 이름은 primary 항목이면 서버 menuName, 아니면 로컬 i18n 키(labelKey)를 쓴다. 서버 응답에는 있지만 FE 표에 없는 메뉴(아직 화면이 없는 COMPANY, DEPT 등)는 사이드 메뉴에 보이지 않는다. 그룹의 하위가 FE 에서 하나도 안 남으면 그룹도 숨긴다. 정렬은 서버의 levelNo, sortOrder, menuCode 순을 따르고, 같은 메뉴 코드 안에서는 표 순서를 따른다.
   - 정적 항목(환영 페이지)은 서버 메뉴와 무관하게 항상 보인다. 비밀번호 변경, 예외 화면은 메뉴가 아니다.
   - 아이콘은 iconName 을 antd 아이콘 이름으로 매핑하는 작은 표를 두고, 없으면 기본 아이콘을 쓴다(새 라이브러리 금지, 동적 import 로 전체 아이콘을 끌어오지 않는다).
   - 레이아웃 연결은 app.tsx 의 layout 설정(menuDataRender 등 ProLayout 의 방식)으로 한다. 기존 레이아웃 셸 모양(F1 합의)은 바꾸지 않는다. config/routes.ts 의 mdm 라우트에서 정적 menu 이름 표시에만 의존하던 부분이 있으면 서버 메뉴 방식과 충돌하지 않게 정리하고 어떻게 정리했는지 보고한다.

3. 권한 판정
   - src/access.ts 의 can(menuCode, action) 을 menus/my 의 actions 로 판정한다: 해당 menuCode 항목이 있고 actions 에 action 이 있으면 true. 메뉴가 아직 로딩되지 않았거나 실패했으면 false.
   - 임시 허용 3곳(src/pages/mdm/common-code, src/pages/mdm/i18n, src/pages/mdm/language 의 permission.ts 의 grant* 함수)을 실제 can 호출로 바꾼다. 가능하면 세 permission.ts 를 없애고 화면이 useAccess().can 을 직접 쓰게 하되, 화면 코드 변경은 이 교체에 필요한 만큼으로 한정한다. 메뉴 코드/동작: COMMON_CODE, I18N 각각 READ/CREATE/UPDATE/DELETE. 권한 없는 버튼은 숨긴다(기존 규칙). 서버 403 AUTH.FORBIDDEN 안내는 그대로 둔다(화면 상태와 서버 판정이 어긋날 수 있다).
   - 화면 진입 가드: 경로가 FE 표에 있는 화면인데 해당 메뉴의 READ 가 없으면(주소창으로 직접 접근 포함) 403 예외 화면으로 보낸다. 메뉴가 로딩 중이면 깜박임 없이 로딩 상태를 보여 주고 끝난 뒤 판정한다. 로딩 실패 상태에서는 오류 안내(다시 시도)를 보여 준다. 기존 RouteGuard 와 역할이 겹치면 그 안에서 처리하고 중복 컴포넌트를 만들지 않는다. 표에 없는 경로(환영, 비밀번호, 로그인, 예외)에는 이 가드가 적용되지 않는다.

4. 문구: 새 안내 문구(메뉴 로딩 실패, 다시 시도, 권한 없음 등)는 ko-KR/en-US 로컬 번들의 i18n 키로 만든다.

5. 만들지 않는 것: 서버 route_path 의 사용, 메뉴/역할/권한 관리 화면, 사용자 이름 조회, 플랫폼 콘솔 변경, 새 화면(회사·부서 등), 다크 모드, 새 라이브러리.

테스트(vitest, 서비스 목, 실제 네트워크 호출 0건을 매 테스트 뒤 확인):
 (a) menus/my 응답 → 사이드 메뉴 구조 변환: 그룹/화면 중첩, 표에 없는 메뉴 제외, 하위가 없는 그룹 숨김, 정렬, primary 가 아닌 항목의 로컬 이름, 언어 변경 후 이름 갱신
 (b) can 판정: actions 에 있음/없음, 메뉴 없음, 로딩 전·실패 시 false
 (c) 로딩 시점: 로그인 후 1회, 세션 복구 후 1회, 언어 변경 후 재조회, 비밀번호 변경 강제 상태에서는 호출 없음, 변경 성공 후 호출, 로그아웃·다른 탭 로그아웃 수신 시 비움, 동시 호출 중복 제거
 (d) 로딩 실패: 오류 안내와 다시 시도, 권한 모두 false, 다시 시도 성공 시 회복, 401 은 기존 규칙
 (e) 진입 가드: READ 없는 직접 접근 → 403 화면, READ 있음 → 통과, 로딩 중 깜박임 없음, 표에 없는 경로는 가드 제외
 (f) 화면 권한 연결: READ 만 있는 사용자가 공통코드·다국어·언어/국가 화면에서 등록/수정/삭제 버튼이 보이지 않음, 모두 있는 사용자는 보임(기존 화면 테스트가 임시 허용에 기대고 있으면 can 목으로 바꾸고 바꾼 파일을 보고)
 (g) Access 토큰·메뉴가 localStorage/sessionStorage 에 저장되지 않음
 (h) 플랫폼 모드에서 menus/my 를 호출하지 않음
 새 흐름 테스트 파일은 3회 반복 실행해 모두 통과함을 기록한다.

검증: lint, antd lint, tsc, test(실행 시간·통과 수), build(테넌트 산출물), build:platform(플랫폼 산출물에 menus/my, 메뉴 연결표가 섞이지 않았는지 문자열 검색으로 확인하고 방법을 기록). 개발 서버와 백엔드는 띄우지 않는다. 사용자가 직접 볼 항목(실제 서버의 menus/my 응답으로 사이드 메뉴가 나오는지, TENANT_ADMIN 과 권한이 적은 사용자 비교, 직접 주소 접근 시 403, 언어 변경 시 메뉴 이름, 사용감)만 frontend/doc/f5-manual-check.md 맨 위에 모으고 나머지는 자동 테스트 이름을 표시한다. 값(주소, 계정, 비밀번호)은 쓰지 않는다.
진행 기록은 [F5단계] 형식으로 frontend/doc/cursor-progress.md 에 추가한다. 보고서는 파일 앞쪽 "작업 방식"의 형식으로 docs/reports/F5.md 와 F5.diff 를 만든다. 커밋 메시지는 [F5] 요약. push 는 하지 않는다. 완료 후 멈춘다.
````

## F6단계: 한 앱·한 로그인 화면으로 통합 (플랫폼 콘솔 합치기)

F5 승인. 사용자 결정(2026-10-19): 관리자(플랫폼 운영자)와 일반 사용자의 로그인 화면이 따로 있는 것은 어색하다. 한 앱, 한 로그인 화면(`/user/login`)으로 합치고, 접속 주소로 플랫폼 콘솔과 테넌트를 구분하며, 로그인한 주체에 따라 메뉴가 달라지게 한다. 이 단계는 **백엔드 9.2.1단계(GET /api/mdm/v1/context)가 끝난 뒤** 수행한다. 이 단계만 수행하고 멈춘다.

````
기준 문서: backend/common/doc/api/mdm-api.md 의 context 절(9.2.1), doc/auth-design.md, 현재 코드 config/config.ts·routes.ts·routes.platform.ts, src/core/auth(mode.ts, actions.ts, guard.ts, refresh.ts), src/core/menu(runtime.tsx, runtime.platform.tsx, store.ts), src/core/i18n(extra.ts, extra.platform.ts), src/app.tsx, src/services/mdm/auth.ts·services/platform/auth.ts, src/pages/user·pages/platform, src/locales/platform, package.json 의 dev/build 스크립트, src/core/dev(devPort, runDev). 명세에 없는 동작은 만들지 말고 질문으로 올린다. 구현 전에 frontend/doc/f6-wireframe.md 에 아래 동작을 텍스트로 정리한다(레이아웃 그림 불필요).

배경: 지금은 APP_MODE(빌드 시점)로 테넌트 앱(포트 8000)과 플랫폼 콘솔(8002)을 따로 빌드하고 로그인 화면도 두 개다. 백엔드는 두 주체를 분리해 둔다(테넌트 사용자 /api/mdm/v1/auth + 쿠키 mdm_refresh, 플랫폼 운영자 /api/platform/v1/auth + 쿠키 mdm_platform_refresh, 토큰 typ/aud 가 다르다). 이 분리는 그대로 두고 화면과 앱만 하나로 합친다.

1. 앱 시작 순서 (src/app.tsx, src/core/auth)
   - 가장 먼저 GET /api/mdm/v1/context 를 호출해 kind(PLATFORM | TENANT)와 tenantCode/tenantName 을 얻는다. 그 결과를 메모리 모듈(core/auth/context.ts 등)에 두고, 이후 인증 경로(authBasePath), 리프레시 쿠키 이름, 홈 경로, 메뉴 방식이 모두 이 값에서 나온다. 빌드 시점 APP_MODE 는 없앤다(process.env.APP_MODE 참조, define, authMode 상수, homePath() 기본 인자 모두 런타임 값으로 교체).
   - context 호출이 실패하면: 네트워크/5xx → "서버에 연결할 수 없습니다" 안내와 [다시 시도] 버튼(로그인 폼을 보이지 않는다). TENANT.NOT_FOUND 계열 → "알 수 없는 사이트입니다" 안내 화면(로그인 폼 없음, 어느 테넌트인지 추측하게 하는 문구 금지). 둘 다 로그인 화면 경로(/user/login) 안에서 보여도 되고 별도 안내 컴포넌트여도 된다.
   - context 는 한 번 받으면 세션 동안 유지한다(주소가 바뀌지 않으므로). 저장소에 저장하지 않는다. 언어 번들 로딩과 세션 복구(refresh)는 context 이후에 시작한다. 다른 탭 로그아웃 방송(BroadcastChannel)과 401 처리는 현재 kind 의 경로를 쓴다.
2. 로그인 화면 하나 (src/pages/user/login)
   - 라우트 /user/login 하나만 둔다. pages/platform/login 은 삭제하고 pages/user/login 이 kind 에 맞는 login 서비스를 호출한다(kind=PLATFORM → services/platform/auth, kind=TENANT → services/mdm/auth. 두 서비스 파일을 core/auth 의 어댑터 하나(예: authApi)로 감싸 호출부는 kind 를 몰라도 되게 한다).
   - 화면 모양은 같다. 입력은 아이디·비밀번호·언어 선택뿐이다. 다른 점은 상단 제목 한 줄만이다: TENANT → tenantName(없으면 BrandMark 만), PLATFORM → "플랫폼 콘솔"(i18n 키). 플랫폼 전용 문구/스타일을 더 만들지 않는다. 테넌트 코드 입력란은 두지 않는다.
   - 비밀번호 변경 화면(/user/password)도 하나로 합친다(pages/platform/password 삭제, 서비스 호출만 kind 별 어댑터). 변경 강제 흐름, 취소 동작, 오류 처리는 지금 테넌트 앱의 것을 그대로 쓴다. 플랫폼 쪽에만 있던 차이(문구, 동작)가 있으면 테스트로 확인하고 차이를 보고서에 적는다.
3. 메뉴와 라우트 (src/core/menu, config/routes.ts)
   - routes.platform.ts 를 없애고 routes.ts 하나에 합친다. 플랫폼 화면 라우트(/tenants, /tenants/new, /tenants/:tenantCode)도 같은 파일에 둔다.
   - 메뉴 방식은 kind 로 갈린다. TENANT: 지금처럼 GET /api/mdm/v1/menus/my 로 만든 서버 메뉴. PLATFORM: 호출하지 않고 정적 메뉴 한 개("테넌트 관리", 경로 /tenants)를 같은 사이드 메뉴 영역에 같은 방식으로 그린다. runtime.platform.tsx 별칭 파일은 없애고 하나의 runtime 이 kind 로 분기한다.
   - 라우트 가드: PLATFORM 로그인 상태에서 테넌트 업무 화면(/mdm/** 등)으로 가면 403 화면, TENANT 로그인 상태에서 /tenants* 로 가면 403 화면. 로그인 전 직접 접근은 기존처럼 /user/login?redirect=... 로 보낸다. 홈은 TENANT → /welcome, PLATFORM → /tenants. 존재하지 않는 경로는 404.
   - can(menuCode, action) 은 PLATFORM 에서 항상 false(플랫폼에는 메뉴 권한 모델이 없다. 8단계 설계)이고, 플랫폼 화면의 접근 판단은 "kind==PLATFORM 로그인 상태"로만 한다. 이 규칙을 한 곳(core/menu 또는 core/auth)에 두고 화면에서 kind 를 직접 비교하지 않는다.
4. 사용자 메뉴·표시 (src/components/RightContent/UserMenu.tsx, 레이아웃)
   - 우상단 사용자 표시, 비밀번호 변경 링크, 로그아웃은 kind 와 상관없이 같은 컴포넌트다. 로그아웃은 현재 kind 의 /logout 을 호출한다. 쿠키 이름을 코드에서 직접 쓰는 곳이 있으면 kind 에서 얻게 바꾼다(프런트는 쿠키를 읽지 않으므로 상수 정리 수준).
   - 언어: PLATFORM 은 지금 별도 번들(src/locales/platform, core/i18n/extra.platform.ts)을 쓴다. 합친 뒤에도 플랫폼 화면 문구는 필요하므로 platform 문구를 기본 로케일 모듈에 합치거나(키 충돌이 없는지 확인), 플랫폼 화면에서만 지연 로딩한다. 어느 쪽이든 테넌트 번들 크기와 서버 번들(/i18n/bundles) 로딩 방식을 바꾸지 않는다. PLATFORM 에서는 서버 i18n 번들 호출이 불가능하면(테넌트 필요 시) 지금 플랫폼 콘솔이 하는 방식 그대로 로컬 문구만 쓴다 — 현재 동작을 확인해 보고서에 적고, 달라지는 점이 있으면 질문한다.
5. 개발·빌드 설정
   - package.json: dev:platform, build:platform 스크립트와 APP_MODE 관련 설정(cross-env, config.ts 의 appMode 분기, define, title 분기, layout runtime 별칭, routes 선택)을 제거해 빌드를 하나로 만든다. title 은 'PMIS' 하나. 포트는 8000 하나(플랫폼 8002 기본값 제거). devPort/runDev 의 플랫폼 분기가 있으면 정리한다.
   - 개발 프록시: DEV_TENANT_CODE 가 있으면 /api/mdm 요청에 X-Tenant-Code 를 붙이는 지금 동작을 유지한다. 플랫폼 콘솔을 로컬에서 확인할 때는 DEV_TENANT_CODE 를 비워 실행한다(헤더가 없으면 9.2.1 의 dev 기본 설정으로 PLATFORM 이 된다). 이 사용법을 .env.local.example, doc/local-setup.md, README 에 쓴다. /api/platform 에는 지금처럼 테넌트 헤더를 붙이지 않는다(tenantHeader.ts 규칙 유지).
   - CI/배포 문서에 플랫폼 콘솔을 따로 빌드한다는 내용이 있으면 갱신한다.
6. 보존할 것: 토큰은 메모리에만(저장소 금지), refresh 쿠키는 HttpOnly 라 프런트가 못 읽음, X-MDM-Request 헤더, 비밀번호 변경 강제 처리, 401 refresh 재시도, safeRedirect, 다른 탭 로그아웃, 지금의 F1~F5 화면 동작. 로그에 토큰·비밀번호를 남기지 않는다.
7. 테스트(vitest, 서버 없이 목 기반):
   - context 로딩: PLATFORM/TENANT 각각 이후 호출 경로가 올바름, 실패 두 종류(재시도 버튼, 알 수 없는 사이트) 화면, 로그인 폼이 보이지 않음.
   - 로그인 흐름: 같은 화면에서 kind 별로 올바른 login/me 서비스가 호출됨, 비밀번호 변경 강제 이동, 실패 메시지, redirect 안전 처리.
   - 메뉴: TENANT 는 menus/my 호출, PLATFORM 은 호출하지 않고 정적 "테넌트 관리" 메뉴.
   - 라우트 가드: PLATFORM 이 /mdm/** → 403, TENANT 가 /tenants* → 403, 비로그인 → 로그인으로 이동 + redirect, 홈 경로.
   - 로그아웃/세션 복구가 kind 경로를 씀.
   - 기존 테스트 전부 통과(현재 개수를 보고서에 적는다). npm run lint(또는 biome check), 타입 검사, 단위 테스트, 빌드(`npm run build`)를 실행해 결과를 적는다. 빌드 산출물(dist)은 커밋하지 않는다.
8. 사용자 확인용으로 frontend/doc/f6-manual-check.md 를 만든다: (a) DEV_TENANT_CODE 설정 상태에서 테넌트 관리자·일반 사용자로 로그인해 같은 화면인지와 메뉴 차이, (b) DEV_TENANT_CODE 비움 상태에서 플랫폼 운영자 로그인과 "테넌트 관리" 메뉴, (c) 플랫폼 로그인 상태에서 /mdm/language 직접 입력 → 403, (d) 비밀번호 변경 강제 계정, (e) 서버를 끈 상태의 연결 안내. 접속 정보·비밀번호는 문서에 쓰지 않는다.
9. 진행 기록은 frontend/doc/cursor-progress.md 에 [F6단계] 형식으로 추가하고, 보고서는 파일 앞의 "작업 방식" 형식(docs/reports/F6.md, F6.diff)으로 쓴다. 커밋은 `[F6] 요약` 한 개, push 는 하지 않는다.
````

---

## F7단계: 조직 화면 기반 + 회사 관리 화면

F6 승인. 사용자 결정(2026-10-22): 조직 화면은 회사(F7) → 부서(F8) → 사람(F9) 순서로 만든다. 와이어프레임은 `frontend/doc/org-screens-wireframe.md` 로 합의했다(부서 = 트리 + 상세 탭, 사람 = 오른쪽 Drawer + 탭, 프로필 이미지는 F9). 이 단계는 회사 화면과 F8·F9 도 쓰는 조직 공통 기반만 만든다. 이 단계만 수행하고 멈춘다.

````
기준 문서: frontend/doc/org-screens-wireframe.md (1절 공통 규칙, 2절 회사), backend/common/doc/api/mdm-api.md 의 "조직·사람" 절(회사, 오류 표)과 코드 조회(/codes, /nations), 현재 코드 src/pages/mdm/common-code(화면·Drawer·rules 구조), src/core/menu(menuRoutes.ts, can), src/core/request(table.ts), config/routes.ts, src/locales. 명세에 없는 필드·동작은 만들지 말고 질문으로 올린다. 구현 전에 frontend/doc/f7-wireframe.md 에 회사 화면의 컬럼·검색·Drawer 필드·버튼 표시 조건·오류 문구 매핑을 텍스트로 정리한다. org-screens-wireframe.md 와 다른 점이 있으면 구현 전에 멈추고 보고서 "질문"에 올린다.

1. 서비스와 타입 (src/services/mdm)
   - org.ts(또는 company.ts): 회사 목록(GET /companies), 상세, 등록, 수정, 삭제, 복구. 타입은 data.d.ts 계열 파일에 명세와 같은 필드명으로 둔다: 목록·상세 응답(companyPk, companyCode, companyName, companyShortName, companyTypeCode, businessRegistrationNo, nationPk, nationCode, parentCompanyPk, parentCompanyCode, sortOrder, useYn 등 명세에 있는 것만. 상세만 translations), 등록·수정 요청(수정 본문에는 companyCode 없음). 목록 쿼리: page(1부터), size, sort, keyword, companyTypeCode, useYn. 허용 정렬 필드는 companyCode, companyName, sortOrder, companyPk.
   - 국가 목록은 기존 /nations 서비스를 재사용한다(새로 만들지 않는다). 공통코드 옵션은 기존 코드 조회 서비스/훅을 재사용한다. 없으면 만든다: useCodeOptions(groupCode) 는 사용 중이고 유효기간 안인 코드만, 현재 언어 이름으로 Select 옵션(value=코드 값, label=이름)을 돌려주고 같은 그룹은 캐시한다. 이 훅은 F8·F9 도 쓰므로 src/core/codes 아래에 둔다(core 는 pages 를 import 하지 않는다).
   - 컴포넌트에서 fetch/axios 를 직접 쓰지 않는다. 기존 src/core/request 를 쓴다.

2. 조직 공통 컴포넌트 (src/components 또는 src/pages/mdm/org/shared 중 기존 구조에 맞는 쪽)
   - 이름 번역 입력 표: 지원 언어(GET /i18n/languages, 화면 진입 때 1회)를 행으로 보여 주고 언어별 이름 입력. 값은 [{supportLanguageCode, text}] 로 주고받고, 비워 둔 언어는 요청에 넣지 않는다(서버는 요청에 없는 언어를 그대로 둔다). 기존 공통코드 화면의 언어별 이름 탭에 재사용 가능한 부분이 있어도 그 화면들은 수정하지 않는다. 새 공통 컴포넌트를 만들고, 기존 화면을 나중에 옮길 수 있다는 점만 보고서에 적는다.
   - 사용 여부 필터 값 처리: 전체는 useYn 파라미터를 보내지 않고, 사용·중지는 Y/N 을 보낸다(명세: ALL 없음).
   - 서버 오류 → 화면 반영 헬퍼: fieldErrors 는 해당 입력칸에, 그 밖의 업무 오류(409 등)는 서버 errorMessage 를 알림으로 보여 주고 없으면 로컬 문구. 기존 core/request 의 오류 처리와 겹치면 그것을 쓰고 중복을 만들지 않는다.

3. 메뉴·라우트 연결
   - src/core/menu/menuRoutes.ts 에 한 줄 추가: COMPANY → /org/company (primary). 새 화면의 부모 그룹은 서버 menus/my 의 parentMenuCode 로 정한다(F5 방식). 서버에서 COMPANY 의 부모가 무엇인지 코드로 가정하지 말고 응답대로 따른다.
   - config/routes.ts 에 /org/company 라우트를 추가한다. 테넌트 앱에만 넣고 플랫폼 빌드에는 섞이지 않게 한다(F6 이후의 구조에 맞게 처리하고 방법을 보고).
   - 권한: 메뉴 COMPANY, READ/CREATE/UPDATE/DELETE. 복구는 UPDATE. 버튼은 can(메뉴, 동작) 하나로 판단하고 권한 없는 버튼은 숨긴다. READ 가 없으면 F5 진입 가드로 403 화면.

4. 회사 화면 (src/pages/org/company)
   - 구성: 검색 영역 + ProTable + 우측 Drawer (공통코드 화면 방식). 검색: keyword(코드·이름·약칭), 회사 유형(useCodeOptions('COMPANY_TYPE')), 사용 여부. 목록 열: 코드, 회사명, 약칭, 유형(코드 이름), 상위 회사(parentCompanyCode), 국가(nationCode), 정렬, 사용 여부, 작업. 정렬 가능 열은 코드, 회사명, 정렬. 서버 페이징(size 20 기본). 사용 중지 행은 흐리게, 작업에 [복구]. 사용 중인 행의 작업은 [수정] [삭제].
   - Drawer 필드(등록/수정 공통): 회사 코드(등록만 입력, 수정은 읽기 전용, 형식 대문자·숫자·밑줄·하이픈 1~50자), 회사명(필수), 약칭, 회사 유형(Select), 사업자등록번호(앞뒤 공백 제거, 하이픈 유지), 국가(Select, 사용 중 국가), 상위 회사(검색형 Select: 회사 목록 API 를 keyword 로 호출, useYn=Y, 자기 자신 제외, 선택 해제 가능), 정렬 순서(정수), 이름 번역 표. 수정 Drawer 를 열 때 상세 API 로 translations 를 채운다.
   - 값이 없는 선택 필드는 null 로 보낸다(빈 문자열 금지). 코드 값 Select 가 비어 있으면(그룹 값 없음) 비활성 + 안내.
   - 삭제: 논리 삭제임을 확인창에 적는다. 거절 사유(COMPANY.HAS_CHILD_COMPANY, HAS_ACTIVE_DEPT, HAS_ACTIVE_ASSIGNMENT, LAST_OWN)는 서버 메시지를 그대로 보여 준다. 복구 거절(COMPANY.PARENT_INACTIVE)도 같다. 실행 뒤 목록을 다시 불러온다.
   - 저장 중 버튼 비활성(연타 방지). 409 COMPANY.DUPLICATE, DUPLICATE_BUSINESS_NO 는 해당 입력칸 근처에 보인다. COMPANY.PARENT_CYCLE 은 상위 회사 칸에 보인다.
   - 문구는 모두 i18n 키로 ko-KR/en-US 로컬 번들에 추가한다(menu.org.company 포함).

5. 만들지 않는 것: 부서·사람 화면, 첨부 파일, 회사 트리 보기(목록만), 새 라이브러리, 기존 화면 리팩터링, 플랫폼 콘솔 변경, 사용자 이름 조회.

테스트(vitest, 서비스 목, 실제 네트워크 호출 0건을 매 테스트 뒤 확인):
 (a) 서비스: 목록 쿼리 파라미터 생성(page/size/sort/useYn 전체 생략), 등록·수정 본문 변환(빈 값 null, 수정에 companyCode 없음, 번역은 입력된 언어만)
 (b) 규칙: 회사 코드 형식, 사업자번호 공백 제거, 번역 변환
 (c) useCodeOptions: 사용 중·유효기간 안 코드만, 현재 언어 이름, 캐시, 빈 그룹
 (d) 화면 흐름: 목록 호출 → 검색 → 등록 Drawer 저장 → 수정에서 코드 읽기 전용 → 삭제 확인 → 복구, 서버 fieldErrors 와 409 오류 문구, 권한별 버튼 표시(READ 만 / 모두), 직접 접근 가드
 (e) 상위 회사 선택에서 자기 자신 제외, 선택 해제
 (f) 플랫폼 빌드에 /org/company 와 회사 서비스가 섞이지 않음
 새 흐름 테스트 파일은 3회 반복 실행해 통과를 기록한다.

검증: lint, antd lint, tsc, test, build, build:platform. 개발 서버와 백엔드는 띄우지 않는다. 사용자가 직접 볼 항목(실제 서버로 목록·등록·수정·삭제·복구, 상위 회사 선택, 번역 입력, 메뉴 노출, 권한이 적은 사용자의 버튼, 사용감)만 frontend/doc/f7-manual-check.md 맨 위에 모으고 나머지는 자동 테스트 이름을 표시한다. 값(주소, 계정, 비밀번호)은 쓰지 않는다.
진행 기록은 [F7단계] 형식으로 frontend/doc/cursor-progress.md 에 추가한다. 보고서는 "작업 방식"의 형식으로 docs/reports/F7.md 와 F7.diff 를 만든다. 커밋 메시지는 [F7] 요약. push 는 하지 않는다. 완료 후 멈춘다.
````

---

## F8단계: 부서 관리 화면 (조직도 트리 + 상세 탭)

F7 승인(`docs/reviews/2026-10-22-F7.md`). 사용자 결정(2026-10-22): 부서 화면은 왼쪽 조직도 트리 + 오른쪽 상세 탭(기본 정보, 변경 이력, 소속 인원)이다. 와이어프레임은 `frontend/doc/org-screens-wireframe.md` 3절로 합의했다. 이 단계만 수행하고 멈춘다.

````
기준 문서: frontend/doc/org-screens-wireframe.md (1절 공통 규칙, 3절 부서), backend/common/doc/api/mdm-api.md "조직·사람"의 부서·이력·사람 목록(deptPk 필터) 절과 오류 표. 응답 필드명은 명세와 백엔드 DTO(backend/common 의 org 패키지 dto)를 대조해 정확히 맞추고, 명세와 DTO 가 다르면 DTO 를 따르되 보고서 "질문"에 올린다. 현재 코드 src/pages/org/company(구조·Drawer·rules), src/pages/org/shared, src/core/codes, src/core/menu/menuRoutes.ts, src/services/mdm/company*.ts, config/routes.ts. 구현 전에 frontend/doc/f8-wireframe.md 에 트리 표시 규칙, 탭별 내용, Drawer 필드와 표시 조건, 오류 문구 매핑을 텍스트로 정리한다. org-screens-wireframe.md 와 다른 점이 있으면 구현 전에 멈추고 "질문"에 올린다.

A. F7 보완 (먼저 처리)
 1. useCodeOptions 캐시(src/core/codes): 로그아웃, 401 로 인한 로그인 이동, 다른 탭 로그아웃 수신, 언어 변경(해당 언어 번역이 응답에 이미 모두 있으면 불필요하니 그렇게 확인하고 근거를 적는다) 때 resetCodeOptionsCache() 가 호출되게 연결한다. 다른 테넌트나 다른 사용자로 로그인했을 때 이전 코드 목록이 보이면 안 된다. 테스트 추가(로그아웃 후 재호출 시 서비스가 다시 불린다).
 2. 회사 화면의 오류 문구(src/pages/org/company/rules.ts 의 LOCAL_ERRORS)를 로컬 번들 i18n 키로 옮긴다(ko-KR/en-US). 서버 errorMessage 우선 규칙은 그대로. 부서 화면은 처음부터 i18n 키를 쓴다.

1. 서비스와 타입 (src/services/mdm)
 - dept.ts / deptQuery.ts / deptRecord.d.ts: 부서 트리(GET /departments/tree?companyCode=&asOfDate=&includeClosed=), 목록(GET /departments, 필요한 곳에서만), 상세, 등록, 수정, 삭제(종료), 복구(재개), 이력(GET /departments/{deptPk}/history, 서버 페이징). 사람 목록 조회 서비스(GET /people)는 F9 에서 본격적으로 만들기 때문에, 여기서는 deptPk 로 거른 목록을 읽는 최소 함수만 둔다(people.ts, 이름·직위·재직 상태에 필요한 필드만 타입에 둔다. F9 에서 확장).
 - 등록·수정 본문: 수정에는 deptCode, 회사가 없다. 빈 값은 null. 날짜는 'YYYY-MM-DD'. 번역은 입력한 언어만.
 - 회사 목록 Select 용으로 F7 의 회사 목록 서비스를 재사용한다(useYn=Y, size 200).

2. 메뉴·라우트
 - menuRoutes.ts: DEPT → /org/dept (primary). config/routes.ts 에 /org/dept 추가. 플랫폼 영역 차단 목록(areaForbidden)에 포함돼 있는지 확인하고 테스트에 넣는다.
 - 권한: 메뉴 DEPT, READ/CREATE/UPDATE/DELETE. 복구(재개)는 UPDATE. 사람 소속 인원 탭은 PERSON:READ 가 있을 때만 내용을 보이고, 없으면 "조회 권한이 없습니다" 안내를 보인다(탭 자체는 보인다).

3. 화면 (src/pages/org/dept)
 - 상단: 회사 Select(필수, 사용 중 회사. 기본값은 목록의 첫 회사. 선택은 URL 쿼리 ?company= 로 유지), 기준일 DatePicker(기본 오늘, 비우면 서버 기본), "종료된 부서 포함" 체크, [조회]. 회사가 하나도 없으면 "회사를 먼저 등록하세요" 안내.
 - 왼쪽 트리: tree API 결과(전위 순회 배열)를 deptPath 로 중첩 변환한다. 변환 함수는 순수 함수로 두고 테스트한다(부모가 결과에 없으면 최상위로, 순서 유지). 노드 표시: 부서명 + 코드, 종료된 부서는 회색 + "종료" 표시. 선택한 부서는 URL 쿼리 ?dept= 로 유지한다. 맨 아래 [+ 최상위 부서] 버튼(CREATE). 트리는 기본 전체 펼침이 아니라 2레벨까지 펼침, 선택 노드의 조상은 펼친다.
 - 오른쪽 상단: 선택 부서 이름, [하위 부서 추가](CREATE, 종료된 부서에는 숨김), [수정](UPDATE), [종료](DELETE), 종료된 부서는 [재개](UPDATE). 부서를 선택하기 전에는 안내 문구.
 - 탭 1 기본 정보: 읽기 화면(코드, 회사, 유형(코드 이름), 상위 부서, 유효기간, 레벨, 정렬, 이름 번역 목록, 사용 여부). 상세 API 로 채운다.
 - 탭 2 변경 이력: 서버 페이징 표. 열: 변경 유형(CREATE/RENAME/MOVE/CLOSE/REOPEN 을 문구로), 적용일, 변경 전→후 이름, 변경 전→후 상위 부서(PK 만 오면 PK 로 표시하고 이름 조회 API 가 없다는 점을 보고), 사유, 변경 시각. 수정·삭제 버튼 없음. 탭을 처음 열 때만 호출하고 부서가 바뀌면 다시 호출한다.
 - 탭 3 소속 인원: GET /people?deptPk= 서버 페이징 표(이름, 직위, 재직 상태). 행 클릭 이동은 사람 화면(F9)이 생기기 전이라 만들지 않는다. 주 소속 기준이라는 안내 문구를 둔다.

4. Drawer (등록/수정)
 - 필드: 부서 코드(등록만. 회사는 상단 선택값을 사용하고 화면에 읽기 전용으로 표시), 부서명(필수), 부서 유형(useCodeOptions('DEPT_TYPE')), 상위 부서(트리 선택. 같은 회사의 사용 중 부서만, 수정 시 자기 자신과 하위는 선택 불가. "하위 부서 추가"로 열면 상위가 미리 채워진다), 정렬 순서, 유효 시작일(등록 기본 오늘), 유효 종료일(시작일 이상 검증), 이름 번역 표(F7 의 NameTranslations 재사용).
 - 수정에서 이름 또는 상위 부서가 바뀌면 "적용일(기본 오늘)"과 "변경 사유(선택)" 칸을 보인다. 바뀌지 않으면 보이지 않고 보내지도 않는다. 두 가지가 동시에 바뀌면 이력이 두 줄 남는다는 안내를 한 줄 넣는다.
 - 종료: 확인창에 "종료일이 오늘로 설정되고 이력이 남습니다"를 적는다. 거절 사유(DEPT.HAS_CHILD, DEPT.HAS_ACTIVE_ASSIGNMENT)는 서버 메시지를 그대로 보여 준다. 재개 거절(DEPT.PARENT_INACTIVE)도 같다.
 - 서버 오류 매핑: DEPT.DUPLICATE → 부서 코드 칸, DEPT.PARENT_CYCLE / PARENT_COMPANY_MISMATCH / PARENT_INACTIVE → 상위 부서 칸, DEPT.INVALID_DATE → 날짜 칸, 그 밖의 fieldErrors 는 해당 칸. 저장 중 버튼 비활성(연타 방지).
 - 저장·종료·재개 뒤에는 트리를 다시 불러오고 선택 부서를 유지한다(종료로 사라지는 경우 포함 옵션이 꺼져 있으면 선택을 비운다).

5. 문구는 모두 i18n 키(ko-KR/en-US 로컬 번들, menu.org.dept 포함). 

6. 만들지 않는 것: 사람 화면과 사람 상세 이동, 드래그로 부서 이동, 조직도 이미지 내보내기, 부서 첨부 파일, 변경 이력의 이름 조회, 새 라이브러리, 기존 공통코드·다국어 화면 수정.

테스트(vitest, 서비스 목, 실제 네트워크 호출 0건을 매 테스트 뒤 확인):
 (a) 트리 변환(부모 누락, 순서, 종료 노드 표시), 조상 펼침 계산
 (b) 서비스: tree/history/people 쿼리 파라미터, 등록·수정 본문(코드·회사 없음, 이름·상위가 바뀔 때만 적용일·사유 포함, 빈 값 null)
 (c) 규칙: 유효기간 검증, 상위 부서 선택 불가 계산(자기·하위), 오류 → 칸 매핑
 (d) 화면 흐름: 회사 선택 → 트리 → 부서 선택 → 탭 3개 호출 시점 → 하위 부서 추가(상위 미리 채움) → 수정(이름만 바뀜: 적용일·사유 표시) → 종료 확인 → 재개, 서버 409 문구, 권한별 버튼(READ 만 / 모두), PERSON:READ 없을 때 소속 인원 탭 안내, 직접 접근 가드, 플랫폼 영역 차단
 (e) A 항목: 로그아웃 뒤 코드 캐시 재조회, 회사 오류 문구 i18n
 새 흐름 테스트 파일은 3회 반복 실행해 통과를 기록한다.

검증: lint, antd lint, tsc, test, build. 개발 서버와 백엔드는 띄우지 않는다. 사용자가 직접 볼 항목(실제 서버로 조직도 보기, 부서 등록·수정·이동·종료·재개, 이력, 소속 인원, 기준일 변경, 권한이 적은 사용자, 사용감)만 frontend/doc/f8-manual-check.md 맨 위에 모으고 나머지는 자동 테스트 이름을 표시한다. 값(주소, 계정, 비밀번호)은 쓰지 않는다.
진행 기록은 [F8단계] 형식으로 frontend/doc/cursor-progress.md 에 추가한다. 보고서는 "작업 방식"의 형식으로 docs/reports/F8.md 와 F8.diff 를 만든다. 커밋 메시지는 [F8] 요약. push 는 하지 않는다. 완료 후 멈춘다.
````

---

## F9단계: 사람 관리 화면 (목록 + Drawer 탭: 기본 정보·소속·프로필 이미지)

F8 승인(`docs/reviews/2026-10-22-F8.md`). 사용자 결정(2026-10-22): 사람 상세는 오른쪽 Drawer + 탭이고 프로필 이미지는 이 단계에 포함한다. 와이어프레임은 `frontend/doc/org-screens-wireframe.md` 4절로 합의했다. 이 단계만 수행하고 멈춘다.

````
기준 문서: frontend/doc/org-screens-wireframe.md (1절 공통 규칙, 4절 사람), backend/common/doc/api/mdm-api.md "조직·사람"의 사람·소속 절과 오류 표, "파일·팀"의 업로드·프로필 이미지 절. 응답 필드명은 명세와 백엔드 DTO(backend/common 의 org 패키지 PersonResponse 등, file 패키지 dto)를 대조해 정확히 맞추고, 다르면 DTO 를 따르되 보고서 "질문"에 올린다. 현재 코드 src/pages/org/company, src/pages/org/dept(트리·탭·Drawer 패턴), src/pages/org/shared, src/services/mdm/people.ts(F8 의 최소 함수), src/core/codes, src/core/request, src/core/menu/menuRoutes.ts. 구현 전에 frontend/doc/f9-wireframe.md 에 목록 컬럼, Drawer 탭별 필드와 표시 조건, 소속 폼 규칙, 프로필 이미지 흐름, 오류 문구 매핑을 텍스트로 정리한다. org-screens-wireframe.md 와 다른 점이 있으면 구현 전에 멈추고 "질문"에 올린다.

A. F8 보완 (먼저 처리)
 1. 부서 화면의 이력·소속 인원 조회(src/pages/org/dept/index.tsx)에 응답 도착 시점 검사를 넣는다. 부서를 빠르게 바꾸거나 페이지를 넘길 때 늦게 도착한 이전 응답이 화면을 덮으면 안 된다(요청 시작 시 키를 저장해 두고 도착 시 현재 키와 같을 때만 반영하거나 alive 플래그를 쓴다. 기존 dedupe 키 방식은 유지). 이 케이스를 테스트에 추가한다(느린 응답 → 부서 변경 → 이전 응답이 도착해도 화면은 새 부서 데이터).
 2. 부서 이력 변경 유형 문구(CHANGE_LABELS)의 한글 기본값을 i18n 로컬 번들 키로 완전히 옮긴다(키는 이미 text() 호출이 쓰고 있으니 번들에 ko-KR/en-US 값이 있는지 확인하고 없으면 추가한다).

1. 서비스와 타입 (src/services/mdm)
 - people.ts 확장(F8 의 최소 함수를 유지): 목록(GET /people), 상세, 등록, 수정, 삭제, 복구. 소속: 목록(GET /people/{personPk}/assignments), 등록, 수정(PUT /person-assignments/{assignmentPk}), 삭제, 종료(POST .../end), 복구(POST .../restore). 타입은 peopleRecord.d.ts 에 명세와 같은 필드명으로. 목록 쿼리: page, size, sort(personName, email, personPk), keyword, companyCode, deptPk, employmentTypeCode, assignmentStatusCode, useYn. 목록 항목에는 주 소속 요약(primaryAssignment)이 있고 없으면 null. 상세에는 translations 와 현재 소속 목록이 있다.
 - 프로필 이미지: files.ts(업로드 POST /files multipart 필드 file, 응답의 filePk 사용), PUT/DELETE/GET /people/{personPk}/profile-image. 업로드는 core/request 의 방식에 맞게 FormData 로 보내고 Content-Type 헤더는 브라우저가 정하게 둔다(수동으로 multipart 헤더를 쓰지 않는다). 기존 core/request 가 FormData 를 지원하지 않으면 최소한으로 확장하고 보고한다.
 - 프로필 이미지 보기: GET .../profile-image 는 Authorization 헤더가 필요해서 <img src> 로 직접 쓸 수 없다. 응답을 blob 으로 받아 URL.createObjectURL 로 보여 주고, 사람을 바꾸거나 Drawer 를 닫거나 이미지를 바꿀 때 URL.revokeObjectURL 로 정리한다. 404 FILE.NOT_FOUND 는 "사진 없음" 상태로 처리한다(오류 알림 아님). 목록에서는 hasProfileImage 로 사진 유무 표시만 하고(작은 아이콘 또는 표시), 목록 행마다 이미지를 내려받지 않는다.
 - 회사/부서/코드 옵션은 F7·F8 의 서비스와 useCodeOptions 를 재사용한다.

2. 메뉴·라우트
 - menuRoutes.ts: PERSON → /org/person (primary). config/routes.ts 에 /org/person 추가, 플랫폼 영역 차단에 포함되는지 확인하고 테스트에 넣는다.
 - 권한: 메뉴 PERSON, READ/CREATE/UPDATE/DELETE. 복구는 UPDATE. 소속 추가·수정·종료·삭제·복구도 PERSON 의 UPDATE 로 판단한다(명세의 소속 API 권한을 확인해 다르면 명세를 따르고 보고). 프로필 이미지 설정·삭제는 PERSON:UPDATE 이고 업로드는 FILE:CREATE 도 필요하다. FILE:CREATE 가 없으면 이미지 올리기 버튼을 숨기거나 비활성으로 하고 이유 안내를 한 줄 보인다.
 - F8 부서 화면의 소속 인원 탭에서 행 클릭 시 /org/person?personPk= 로 이동하는 연결을 이 단계에서 만든다(PERSON:READ 가 있을 때만). 사람 화면은 ?personPk= 쿼리가 있으면 그 사람의 Drawer 를 바로 연다.

3. 목록 화면 (src/pages/org/person)
 - 검색: keyword(이름·이메일·휴대전화·사원번호), 회사 Select, 부서 선택(선택한 회사의 부서. tree API 로 받아 Select 또는 TreeSelect 로. 회사를 먼저 고르기 전에는 비활성), 고용 유형(useCodeOptions('EMPLOYMENT_TYPE')), 재직 상태('ASSIGNMENT_STATUS'), 사용 여부(전체는 파라미터 없음). [조회] [초기화].
 - 표 열: 이름, 이메일, 휴대전화, 회사, 부서, 직위('POSITION' 코드 이름, 값이 없으면 코드 그대로 또는 '-'), 사원번호, 재직 상태, 사진 유무, 사용 여부. 회사·부서·직위·사원번호·재직 상태는 모두 주 소속 요약 값이고 주 소속이 없으면 '-'. 서버 페이징·정렬(이름, 이메일). 사용 중지 행은 흐리게. 행 클릭 또는 [상세]로 Drawer 를 연다. [+ 사람 등록] 버튼(CREATE).

4. Drawer (넓은 폭, 탭 2개)
 - 등록 모드: 기본 정보 탭만 활성(소속·프로필 이미지는 저장 후 사용 가능하다는 안내). 저장하면 같은 Drawer 가 수정 모드로 전환되고 소속 탭이 열린다.
 - 탭 1 기본 정보: 이름(필수), 이메일(형식만 검사. 서버 오류가 오면 칸에 표시), 휴대전화, 이름 번역 표(NameTranslations 재사용). 그 밖에 상세 응답에 있고 수정 API 가 받는 필드는 화면에 없어도 상세 값을 다시 실어 보낸다(F7·F8 방식). 프로필 이미지 영역: 현재 이미지(blob), [이미지 올리기](파일 선택. 클라이언트에서 확장자 png/jpg/jpeg/gif/webp 와 크기 상한 안내만 가볍게 검사하고, 최종 판정은 서버 오류 FILE.EXTENSION_NOT_ALLOWED / MIME_MISMATCH / TOO_LARGE / NOT_IMAGE 문구를 그대로 보여 준다), [이미지 삭제]. 올리기 = 업로드 후 PUT profile-image 를 순서대로 호출하고, 두 번째 호출이 실패하면 올린 파일이 TEMP 로 남는다는 점을 오류 문구에 쓰지 말고(사용자에게 의미 없음) 조용히 둔다(정리는 서버 스케줄러 몫). 연타 방지.
 - 탭 2 소속: 목록(과거 포함, 시작일 내림차순, 사용 여부 필터). 열: 회사, 부서, 고용 유형, 직위, 직책, 사원번호, 기간(시작~종료), 주 소속, 부서장, 재직 상태, 사용 여부, 작업(수정/종료/삭제/복구). [+ 소속 추가].
 - 소속 폼(작은 모달 또는 중첩 Drawer): 회사(필수, 사용 중), 부서(선택한 회사의 사용 중 부서. 회사를 바꾸면 부서를 비운다), 고용 유형, 재직 상태, 직위, 직책, 사원번호, 시작일(필수), 종료일(시작일 이상), 부서장 여부(부서가 있을 때만 켤 수 있음), 주 소속 여부. 규칙: 첫 현재 소속은 서버가 주 소속으로 만든다는 안내(체크 비활성 + 문구는 이 사람의 현재 소속이 없을 때), 다른 소속을 주 소속으로 바꾸면 기존 주 소속이 해제된다는 안내 한 줄, 재직 상태가 퇴직(RESIGNED)이면 종료일 필수(폼에서 먼저 검사), 부서 없이 부서장 불가. 코드 값 Select 가 비어 있으면(POSITION, JOB_TITLE 은 시드 값이 없을 수 있다) 비활성 + "공통코드에서 값을 먼저 등록하세요" 안내.
 - 종료: 종료일(기본 오늘)과 재직 상태(기본 RESIGNED)를 받는 작은 대화상자. 이미 종료된 소속에는 버튼을 숨긴다. 사용 중지된 소속은 종료 대신 복구 후 종료하라는 서버 메시지(ASSIGNMENT.INACTIVE)를 그대로 보여 준다.
 - 서버 오류 매핑: ASSIGNMENT.DUPLICATE / EMPLOYEE_NO_DUPLICATE → 사원번호 또는 부서 칸 근처, ASSIGNMENT.DEPT_COMPANY_MISMATCH / DEPT_INACTIVE → 부서 칸, COMPANY_INACTIVE → 회사 칸, INVALID_DATE → 날짜 칸, DEPT_HEAD_REQUIRES_DEPT → 부서장 칸, RESIGNED_REQUIRES_END → 종료일 칸, ORG.INVALID_CODE_VALUE 와 fieldErrors 는 해당 칸. 나머지는 서버 errorMessage 알림.
 - 사람 삭제/복구: 헤더의 [삭제](DELETE) / [복구](UPDATE). 삭제 거절(PERSON.HAS_ACTIVE_ASSIGNMENT, PERSON.HAS_ACCOUNT)은 서버 메시지를 그대로 보여 준다. 사용 중지 사람은 소속 추가·수정과 이미지 변경을 막고(버튼 숨김) 안내한다(서버는 PERSON.INACTIVE).

5. 문구는 모두 i18n 키(ko-KR/en-US 로컬 번들, menu.org.person 포함). 서버 errorMessage 우선 규칙 유지.

6. 만들지 않는 것: 사용자 계정 연결·생성 화면(읽기 전용 표시도 하지 않는다), 첨부 파일 탭, 파일 관리 화면, 팀 화면, 대량 가져오기·내보내기, 소속 이동 전용 흐름, 사진 자르기·편집, 새 라이브러리, 기존 공통코드·다국어 화면 수정.

테스트(vitest, 서비스 목, 실제 네트워크 호출 0건을 매 테스트 뒤 확인):
 (a) 서비스: 목록 쿼리(전체 useYn 생략, 정렬 화이트리스트), 등록·수정 본문(빈 값 null, 번역 변환), 소속 본문(부서 없음 → null, 종료 본문 기본값), 업로드 FormData 구성(필드명 file, 수동 Content-Type 없음)
 (b) 규칙: 소속 폼 검증(퇴직 → 종료일 필수, 부서 없이 부서장 불가, 날짜 순서), 첫 소속 안내 조건, 오류 → 칸 매핑
 (c) 프로필 이미지: 올리기 = 업로드 → PUT 순서, 업로드 실패 시 PUT 호출 없음, blob URL 생성·해제(Drawer 닫기, 사람 변경), 404 는 사진 없음 상태, FILE:CREATE 없을 때 올리기 숨김
 (d) 화면 흐름: 검색 → 목록 → 등록(저장 후 수정 모드 전환·소속 탭 활성) → 소속 추가(첫 소속 주 소속 안내) → 주 소속 변경 안내 → 종료 대화상자 → 복구, 사람 삭제 거절 메시지, 권한별 버튼(READ 만 / 모두), 직접 접근 가드, 플랫폼 영역 차단, ?personPk= 로 Drawer 자동 열기
 (e) 부서 화면: 소속 인원 행 클릭 → /org/person?personPk= 이동(PERSON:READ 없으면 이동 없음), A 항목(늦게 도착한 응답이 화면을 덮지 않음)
 새 흐름 테스트 파일은 3회 반복 실행해 통과를 기록한다. 전체 테스트 시간이 길어지면(현재 약 260초) 느린 파일을 보고하고, 설정(fileParallelism, testTimeout)은 바꾸지 말고 제안만 한다.

검증: lint, antd lint, tsc, test, build. 개발 서버와 백엔드는 띄우지 않는다. 사용자가 직접 볼 항목(실제 서버로 사람 등록·수정, 소속 추가·종료·주 소속 변경, 프로필 이미지 올리기·삭제·보기, 부서 화면에서 사람으로 이동, 권한이 적은 사용자, 사용감)만 frontend/doc/f9-manual-check.md 맨 위에 모으고 나머지는 자동 테스트 이름을 표시한다. 값(주소, 계정, 비밀번호)은 쓰지 않는다.
진행 기록은 [F9단계] 형식으로 frontend/doc/cursor-progress.md 에 추가한다. 보고서는 "작업 방식"의 형식으로 docs/reports/F9.md 와 F9.diff 를 만든다. 커밋 메시지는 [F9] 요약. push 는 하지 않는다. 완료 후 멈춘다.
````
