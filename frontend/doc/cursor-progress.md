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
