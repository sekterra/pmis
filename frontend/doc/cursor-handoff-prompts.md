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
