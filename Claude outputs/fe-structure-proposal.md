# 프런트엔드 구조 제안서

작성: 설계 담당(Claude) / 검토: 사용자 / 개발: Cursor
기준: `frontend`(Ant Design Pro 6.0.3 기본 템플릿 상태), 백엔드 `doc/api/mdm-api.md`, `doc/auth-design.md`, 8.3 진행 중인 `platform-api.md`

이 문서는 FE 작업을 시작하기 전에 구조를 합의하기 위한 것입니다. 합의 전에는 Cursor에 FE 지시문을 주지 않습니다. 각 항목은 "현재 상태 → 제안 → 대안과 트레이드오프" 순서이고, 맨 끝에 결정 목록이 있습니다.

## 1. 현재 상태에서 확인한 사실

- 템플릿은 중국 사업용 샘플이 그대로 남아 있습니다. 데모 화면(dashboard, form, list, profile, result, account, chatbot, register)과 mock(`_mock.ts`, `mock/`), 중국어/기타 로케일 8종이 있고 `ko-KR`은 없습니다. 기본 로케일은 `zh-CN`입니다.
- `config/config.ts`에 Google Analytics 추적 ID(`analytics.ga_v2`)가 켜져 있습니다. 우리 서비스에서는 반드시 제거해야 합니다.
- `src/requestErrorConfig.ts`는 `errorCode`를 숫자로 가정합니다. 우리 백엔드는 문자열(`AUTH.INVALID_CREDENTIALS` 같은 형식)입니다.
- 로그인/내 정보는 템플릿 샘플 API(`/api/login/account`, `/api/currentUser`)를 부릅니다. 토큰 처리는 비어 있습니다.
- `.cursor/rules`가 API 경로를 `/api/v1/...`로 적고 있으나 확정된 경로는 `/api/mdm/v1/...`와 `/api/platform/v1/...`입니다. 규칙 파일은 합의 후 제가 갱신합니다.
- 백엔드 OpenAPI 명세(springdoc)는 없습니다. 템플릿의 `max openapi` 생성은 쓸 수 없습니다.

## 2. 전체 구조

### 2.1 앱은 하나, 화면은 기능(feature) 단위

PMIS 서버가 나중에 같은 화면 골격을 쓰므로 FE는 하나의 앱으로 두고 `src/pages`를 솔루션/기능으로 나눕니다.

```
src/
  core/                공통 기반. pages 를 import 하지 않는다
    auth/              토큰 저장소, 로그인/갱신/로그아웃, 강제 변경 흐름
    request/           request 설정, 오류 처리, 페이징 어댑터
    i18n/              서버 번들 로더, 언어 전환, 포맷
    codes/             공통코드 훅(useCodeOptions)과 캐시
    menu/              서버 메뉴 + 정적 라우트 결합, 권한 훅
  components/          여러 화면이 공유하는 UI(CrudTable, TranslationEditor 등)
  pages/
    system/            MDM 관리 화면(공통코드, 다국어, 이후 조직·사용자·역할·메뉴·권한)
    pmis/              PMIS 화면(이후)
    user/              로그인, 비밀번호 변경
    exception/         403, 404, 500
  services/mdm/        MDM API 클라이언트(auth, codes, i18n, ...)
  services/pmis/       PMIS API 클라이언트(이후)
```

규칙: `core`는 `pages`를 알지 못하고, `pages/system`과 `pages/pmis`는 서로 import 하지 않습니다. 화면별 폴더 안에 `index.tsx`, `data.d.ts`(타입), 필요하면 `components/`를 둡니다(기존 규칙 유지). 공통 API(auth, 코드, 번들)는 화면마다 흩어 두지 않고 `services/mdm`에 둡니다.

### 2.2 플랫폼 운영 콘솔(테넌트 관리)

테넌트 사용자 앱과 플랫폼 운영자 콘솔은 사용자, 토큰, 쿠키, 도메인이 모두 다릅니다. 선택지는 세 가지입니다.

| 안 | 내용 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. 같은 저장소, 빌드 모드 2개 (추천) | `APP_MODE=tenant` / `platform` 환경변수로 라우트·로그인·API 경로를 골라 별도 번들을 만든다 | 코드 공유, 번들에 서로의 화면이 들어가지 않음, 배포 분리 | 설정이 한 겹 늘어남 |
| B. 같은 번들, 런타임 분기 | Host로 모드 판단 | 빌드 1개 | 운영자 화면 코드가 테넌트 사용자에게 내려감, 실수 위험 |
| C. 저장소 분리 | 별도 FE 프로젝트 | 가장 깨끗함 | 중복 기반 코드 유지 |

추천은 A입니다. 플랫폼 콘솔은 FE 단계 후반(F4)에 만들고, 지금은 `core`가 모드를 받아들일 구조만 갖춥니다.

## 3. 인증(백엔드 설계와 맞춤)

토큰 전략은 백엔드에서 확정됐습니다. Access는 응답 본문, 메모리 보관. Refresh는 HttpOnly 쿠키.

흐름:

1. 앱 시작: 메모리에 Access가 없으므로 `POST /auth/refresh`(쿠키, 헤더 `X-MDM-Request: 1`)를 먼저 부릅니다. 성공하면 Access를 받고 `GET /auth/me`로 사용자를 채웁니다. 실패하면 로그인 화면으로 보냅니다. (페이지 새로고침해도 로그인이 유지되는 이유)
2. 요청마다 `Authorization: Bearer`를 붙입니다(메모리의 토큰).
3. 401 `AUTH.TOKEN_EXPIRED`: 갱신 호출을 한 번만 실행(single-flight)하고 대기 중인 요청을 새 토큰으로 재시도합니다.
4. 409 `AUTH.REFRESH_IN_PROGRESS`: 다른 탭이 방금 갱신한 것이므로 0.3~1초 후 한 번 더 시도합니다.
5. 갱신이 401이면(만료, 재사용 감지) 상태를 비우고 로그인 화면으로 이동합니다. 원래 주소는 `redirect`로 보존합니다.
6. 로그인 응답의 `passwordChangeRequired=true`이면 비밀번호 변경 화면 외에는 이동하지 못하게 막습니다(서버도 403 `AUTH.PASSWORD_CHANGE_REQUIRED`로 막지만 UX를 위해).
7. 여러 탭: 한 탭에서 로그아웃하면 다른 탭도 로그아웃되도록 `BroadcastChannel`로 알립니다(새 라이브러리 없음, 브라우저 기본 API). 추천: 포함.

Access를 localStorage에 두지 않는 이유는 XSS 노출을 줄이기 위해서입니다. 대신 새로고침 때 refresh 한 번이 추가됩니다(수십 ms).

## 4. 요청 계층

- `core/request`에서 한 번만 설정합니다. 래퍼 `{ success, data, errorCode(문자열), errorMessage, showType, traceId, fieldErrors }`를 처리하고, 오류 알림에 `traceId`를 함께 보여 줍니다(문의 시 추적용).
- `fieldErrors`는 폼에 필드별 오류로 매핑하는 헬퍼를 둡니다(`ProForm`의 `setFields`).
- 페이징 어댑터: ProTable은 `current/pageSize/sorter`를 쓰고 우리 API는 `page(1부터)/size/sort=필드,방향`, 응답은 `data={items,total,page,size}`입니다. 어댑터 한 개(`toTableRequest(service)`)로 모든 목록 화면이 같은 방식으로 동작하게 합니다.
- API 클라이언트: 백엔드에 OpenAPI가 없으므로 손으로 작성합니다(`services/mdm/*.ts`, 타입은 `data.d.ts`). 백엔드에 springdoc을 도입하면 타입 생성으로 바꿀 수 있습니다. 도입 비용(의존성, 문서 어노테이션)이 있어 지금은 하지 않는 쪽을 추천합니다.
- 테넌트: 브라우저는 Host로 테넌트가 정해지므로 FE 코드에 테넌트 코드를 넣지 않습니다.

## 5. 다국어(i18n)

백엔드는 번들 API로 `common`, `error`, `auth`(공개)와 `menu` 등(로그인 후)을 줍니다. Umi의 로케일 플러그인(`useIntl`)과 합치는 방법이 두 가지입니다.

| 안 | 내용 | 장점 | 단점 |
| --- | --- | --- | --- |
| A. 로컬 기본 + 서버 덮어쓰기 (추천) | `src/locales/ko-KR.ts`, `en-US.ts`를 앱에 포함(기본). 시작 시 공개 번들(common, error, auth)을 받아 합치고, 로그인 후 나머지 네임스페이스를 받아 합친다. 서버 값이 우선, 없으면 로컬, 마지막으로 `defaultMessage`. ETag로 재다운로드 방지 | 서버가 느리거나 실패해도 화면은 뜸. 테넌트가 문구를 바꿀 수 있음 | 로컬과 서버 키 관리를 맞춰야 함 |
| B. 서버 전용 | 모든 문구를 서버에서 받음 | 한 곳에서 관리 | 로그인 화면조차 서버 응답을 기다림, 개발 중 화면 문구 추가마다 DB Seed 필요 |

추천 A. 키 규칙은 서버 `i18n_message.message_key`와 같은 점 표기(`button.save`, `menu.system.common-code`)를 씁니다. 언어를 바꾸면 `setLocale` + 해당 언어 번들을 다시 받습니다. antd 로케일, 날짜 포맷은 `i18n/languages` 응답(날짜·숫자 포맷)을 따릅니다. 기본 로케일은 `ko-KR`로 바꾸고 `ko-KR`, `en-US` 파일을 새로 만듭니다.

## 6. 메뉴와 권한

메뉴를 서버가 전부 만들면 라우트 컴포넌트가 정해지지 않습니다. 따라서:

- 라우트는 코드(`config/routes.ts`)에 정적으로 선언합니다. 각 라우트의 `name`은 서버 `menu_code`와 같은 키로 정합니다.
- 서버가 내려주는 "내 메뉴"(8.4 `GET /menus/my`)로 보이는 메뉴, 순서, 이름(다국어)을 정합니다. 서버에 없는 라우트는 숨기고, 라우트가 없는 서버 메뉴는 무시합니다. 직접 URL로 접근해도 라우트 가드가 막고 최종 보안은 서버가 보장합니다.
- 버튼 단위 권한(등록/수정/삭제/내보내기)을 위해 8.4에서 `menus/my`가 메뉴별로 허용된 기능 코드(READ/CREATE/…)를 함께 주도록 백엔드 명세를 확정하겠습니다(현재 `me`에는 권한 목록이 없음). FE에서는 `useAccess().can('COMMON_CODE', 'UPDATE')` 형태 하나로만 권한을 묻고, 권한 이름을 화면에 흩뿌리지 않습니다.

대안: 완전 동적 메뉴(서버가 컴포넌트까지 지정)는 번들 분리와 보안 검토가 복잡해서 비추천입니다.

## 7. 화면 만드는 표준 패턴

관리 화면이 대부분 "목록 + 검색 + 등록/수정 + 논리삭제/복구 + 이력"이므로 반복을 줄입니다.

- `CrudTable`: ProTable + 어댑터 + 논리삭제 확인 + 사용여부 필터를 합친 공용 컴포넌트(필요한 것만, 과하게 일반화하지 않음).
- `TranslationEditor`: 언어별 이름 입력 표(공통코드명, 그룹명, 메시지 번역에 공통 사용).
- `useCodeOptions(groupCode)`: 공통코드를 현재 언어 이름으로 가져와 캐시(react-query는 이미 포함).
- 삭제 버튼은 확인 대화상자 → 논리삭제 API(`use_yn='N'`), 복구는 별도 동작.

## 8. 템플릿 정리

데모 화면과 mock, 사용하지 않는 로케일, 광고성/중국 서비스용 구성이 섞여 있습니다. 한꺼번에 정리하되 되돌릴 수 있게 하나의 커밋으로 분리합니다.

- 제거 대상: `dashboard`, `form`, `list`, `profile`, `result`, `account`, `chatbot`, `table-list`, `user/register*`, 모든 `_mock.ts`와 `mock/`, `china-division`, `@ant-design/x*`(챗봇 전용), 사용하지 않는 로케일 7종, `analytics`(GA), `DocLink`/`VersionDropdown`, `cloudflare-worker/`, `service-worker.js`.
- 제거하지 않음: `exception/*`, 레이아웃 컴포넌트, 로그인 화면(스타일 재사용).
- 정리 방법: 시작 전에 브랜치를 만들고, 화면 제거 → 의존성 제거(승인) → 빌드·테스트 확인 순서로 합니다. `npm run simple`(자동 정리 스크립트)은 되돌릴 수 없어 사용하지 않고, 직접 삭제합니다.

## 9. 개발 환경

- 프록시: `/api/mdm`, `/api/platform` → 로컬 백엔드. 브라우저는 FE 개발 서버 한 곳만 보므로 쿠키(SameSite=Lax)가 그대로 동작합니다.
- 테넌트: 로컬에서는 Host가 `localhost`라 테넌트를 식별할 수 없으므로 프록시가 개발 전용 헤더 `X-Tenant-Code`를 붙입니다(환경변수 `DEV_TENANT_CODE`, 백엔드 dev 프로필에서만 허용). 대안은 `acme.localhost:8000`처럼 서브도메인을 쓰는 방식이며 실제 운영과 더 비슷하지만 설정이 한 단계 늘어납니다. 추천: 헤더 방식으로 시작.
- 개발 서버는 `npm run dev`(mock 없음)만 사용합니다. 포트 충돌(8080)이 있었으므로 백엔드 포트는 설정으로 둡니다.

## 10. 테스트

기존 vitest + Testing Library를 유지합니다. 새 라이브러리는 추가하지 않습니다. 인증 흐름(single-flight 갱신, 409 재시도, 강제 변경), 페이징 어댑터, 번들 합치기는 단위 테스트로 검증합니다. 브라우저 E2E는 도구 승인 후 후속 단계에서 검토합니다.

## 11. 단계 계획

| 단계 | 내용 | 선행 조건 |
| --- | --- | --- |
| F0 | 템플릿 정리, `ko-KR` 기본, GA 제거, 프록시 설정, 규칙 파일 갱신 | 이 문서 합의 |
| F1 | `core`: request, auth(로그인, 갱신, 강제 변경, 로그아웃, 멀티탭), i18n 로더, 레이아웃 셸(정적 메뉴로 시작) | 8.2 완료(됨) |
| F2 | 공통코드 관리 화면 + `useCodeOptions` | F1 |
| F3 | 다국어 관리 화면(언어, 국가, 네임스페이스, 메시지·번역) | F1 |
| F4 | 서버 메뉴 연동(`menus/my`), 권한 훅 | 백엔드 8.4 |
| F5 | 플랫폼 운영 콘솔(테넌트 목록·생성·상세·재발급·중지·도메인) | 8.3 완료 |
| F6~ | 조직(회사·부서·사람), 사용자, 역할·권한·메뉴, 파일, 팀 | 해당 백엔드 API |

각 단계는 지금과 같은 방식으로 Cursor 지시문을 만들고, 진행 문서를 검토한 뒤 다음 단계로 갑니다. 화면 설계(와이어프레임 수준)는 F2부터 단계별로 사용자와 함께 확인한 뒤 구현합니다.

## 12. 결정 목록

확정(2026-10-11, 사용자 선택):

1. 플랫폼 콘솔: 같은 저장소 + 빌드 모드 2개(`APP_MODE=tenant/platform`).
2. 서버 번역 연동: 로컬 기본(ko-KR, en-US) + 서버 번들 덮어쓰기.
3. 메뉴: 정적 라우트 + 서버 메뉴로 노출 제어.
4. 템플릿 정리: F0에서 브랜치를 만들고 한 번에 정리.

기본안으로 진행(이의가 있으면 알려 주면 바꾼다):

5. API 클라이언트: 손으로 작성(OpenAPI/springdoc 도입은 보류).
6. 로컬 테넌트: 프록시가 개발 전용 헤더 `X-Tenant-Code`를 붙이는 방식으로 시작.
7. 멀티탭 로그아웃 동기화: `BroadcastChannel`로 포함.
8. 백엔드 추가 요청: 8.4 `GET /menus/my`가 메뉴별 허용 기능 코드(READ/CREATE/UPDATE/DELETE/EXPORT)를 함께 반환.
