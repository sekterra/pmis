# MDM API

기본 경로 `/api/mdm/v1`. 이후 단계의 엔드포인트도 이 파일에 이어서 적는다.

## 공통 규약

성공과 실패 모두 같은 래퍼다.

```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "errorMessage": null,
  "showType": null,
  "traceId": null,
  "fieldErrors": null
}
```

`showType`은 0 무시, 1 경고, 2 오류, 3 알림, 9 로그인 이동이다. 오류 코드는 `<DOMAIN>.<NAME>` 문자열이다.

건수가 적은 조회 목록(지원 언어, 국가, 코드 조회)은 `data`에 배열을 바로 담는다. 관리용 검색 목록만 페이지를 쓴다. `page`는 1부터다.

```json
{ "items": [], "total": 0, "page": 1, "size": 20 }
```

쿼리 `page` 기본 1, `size` 기본 20, 최대 200. `page`가 1보다 작거나 `size`가 1보다 작거나 200보다 크면 `400 COMMON.INVALID_PAGE`.

정렬은 `sort=field,asc`를 반복한다. 방향 생략은 asc. 리소스마다 허용 필드만 받고, 그 밖이면 `400 COMMON.INVALID_SORT`. 정렬을 생략하면 리소스 기본 정렬을 쓰고, 항상 PK를 마지막 키로 붙여 페이지가 흔들리지 않게 한다. Spring Data의 0부터 세는 `Pageable` 바인딩은 쓰지 않는다.

요청 본문 검증 실패는 `400 COMMON.VALIDATION_FAILED`와 `fieldErrors`다. 읽을 수 없는 JSON도 같은 코드다.

테넌트는 요청마다 식별한다. 개발 프로필(`dev`)에서만 `X-Tenant-Code` 헤더를 쓴다. `local` 프로필은 `dev`를 포함하므로 로컬 `gradlew.bat bootRun`에서도 같은 헤더를 쓴다. 프로필 없이 실행한 jar에서는 이 헤더를 쓰지 않는다. 로컬 파일 위치는 `doc/local-setup.md`다. 테넌트가 없거나 사용 중이 아니면 `TENANT.NOT_FOUND` 또는 `TENANT.INACTIVE`다.

Access 토큰은 `Authorization: Bearer`다. 관리 API는 `@RequirePermission(menu, action)`이다. 권한 코드는 `메뉴코드:기능코드`다. 예: `COMMON_CODE:UPDATE`. 인증이 없으면 `401 AUTH.UNAUTHORIZED`, 토큰 만료는 `401 AUTH.TOKEN_EXPIRED`, 서명·kid·aud·typ 불일치는 `401 AUTH.TOKEN_REJECTED`, 권한이 없으면 `403 AUTH.FORBIDDEN`, 계정이 `ACTIVE`가 아니거나 `use_yn='N'`이면 `401 AUTH.ACCOUNT_DISABLED`다. `mdm.security.permit-all`은 `dev`에서만 켤 수 있고 기본은 끈다.

| 경로 | 인증 |
| --- | --- |
| POST `/api/mdm/v1/auth/login`, `/auth/refresh` | 공개. 구현은 8.2 |
| GET `/api/mdm/v1/auth/jwks` | 공개. 래퍼 없음. `Cache-Control: public, max-age=300`. 공개키만 |
| GET `/api/mdm/v1/i18n/languages` | 공개. 테넌트 식별 필요 |
| GET `/api/mdm/v1/i18n/bundles`, `/bundles/{code}` | `common`, `error`, `auth`만 공개. 그 외 네임스페이스는 인증 필요 |
| GET `/api/mdm/v1/codes`, `/codes/{group}`, `/nations` | 인증 필요. 권한 선언 없음 |
| `@RequirePermission` 관리 API | 인증과 권한 |

JWKS 예:

```json
{ "keys": [ { "kty": "RSA", "kid": "...", "n": "...", "e": "AQAB" } ] }
```

`errorMessage`와 `fieldErrors` 문구는 `error` 네임스페이스 번들에서 오류 코드(필드 오류는 메시지 문자열)를 요청 언어로 찾는다. 없거나 테넌트가 아직 없거나 번들 조회가 실패하면 예외의 기본 문구를 쓴다. 번들 실패가 오류 응답을 깨뜨리지 않는다.

공통코드 유효기간의 오늘은 테넌트 `timezone_id` 기준이다. 시간대가 없거나 올바르지 않으면 시스템 기본 시간대를 쓴다.

## 코드 조회

권한 선언 없음.

`GET /api/mdm/v1/codes/{groupCode}`

사용 중이고 유효기간 안인 코드만 평면 목록으로 준다. 언어는 `lang` 쿼리, `Accept-Language`, 테넌트 기본 언어 순이다. 이름은 `common_code_lang`의 그 언어, `support_language.fallback_language_code` 체인, `common_code.code_name` 순이다. 계층 그룹도 `parentCodeValue`가 있는 평면 목록이다.

```json
{
  "success": true,
  "data": [
    {
      "codeValue": "GROUP",
      "codeName": "그룹",
      "parentCodeValue": null,
      "sortOrder": 1,
      "extAttr1": null,
      "extAttr2": null,
      "extAttr3": null,
      "extAttr4": null,
      "extAttr5": null
    }
  ]
}
```

없거나 사용 중지된 그룹은 `404 CODE.GROUP_NOT_FOUND`.

`GET /api/mdm/v1/codes?groups=A,B` 는 최대 30개 그룹의 맵이다. `data`는 `{ "A": [ ... ], "B": [ ... ] }` 다.

## 코드 그룹 관리

메뉴 `COMMON_CODE`.

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | `/api/mdm/v1/code-groups` | READ. `keyword`, `solutionCode`, `useYn` |
| GET | `/api/mdm/v1/code-groups/{groupCode}` | READ |
| POST | `/api/mdm/v1/code-groups` | CREATE |
| PUT | `/api/mdm/v1/code-groups/{groupCode}` | UPDATE |
| DELETE | `/api/mdm/v1/code-groups/{groupCode}` | DELETE. `use_yn=N`. 사용 중인 코드가 있으면 `409 CODE.GROUP_IN_USE` |
| POST | `/api/mdm/v1/code-groups/{groupCode}/restore` | UPDATE. `use_yn=Y` |

정렬 필드: `codeGroupCode`, `codeGroupName`, `sortOrder`, `solutionCode`, `codeGroupPk`. 기본은 그룹 코드, PK.

요청 예:

```json
{
  "codeGroupCode": "HIER_IT",
  "solutionCode": "MDM",
  "codeGroupName": "계층",
  "description": null,
  "hierarchy": true,
  "codeValueLength": null,
  "extAttrSchema": null,
  "sortOrder": 1,
  "translations": [
    { "supportLanguageCode": "ko-KR", "name": "계층", "description": null }
  ]
}
```

`translations`는 그룹과 언어 기준으로 있으면 고치고 없으면 넣는다. 요청에 없는 언어는 그대로 둔다. 그룹 코드가 있으면 `409 CODE.DUPLICATE`. 지원하지 않는 언어는 `400 CODE.LANGUAGE_NOT_FOUND`.

## 공통코드 관리

메뉴 `COMMON_CODE`.

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | `/api/mdm/v1/common-codes?groupCode=` | READ. `groupCode` 필수. `keyword`, `useYn` |
| GET | `/api/mdm/v1/common-codes/{codePk}` | READ |
| POST | `/api/mdm/v1/common-codes` | CREATE |
| PUT | `/api/mdm/v1/common-codes/{codePk}` | UPDATE. 그룹 코드는 바꿀 수 없다 |
| DELETE | `/api/mdm/v1/common-codes/{codePk}` | DELETE. `use_yn=N`. `changeReason` 선택 |
| POST | `/api/mdm/v1/common-codes/{codePk}/restore` | UPDATE. 복구 이력은 `U` |
| GET | `/api/mdm/v1/common-codes/{codePk}/history` | READ |

정렬 필드: `codeValue`, `codeName`, `sortOrder`, `codePk`. 기본은 정렬순서, 코드값, PK. 이력 기본 정렬은 변경 시각 내림차순, 이력 PK.

사용 중인 `code_value`만 그룹 안에서 유일하다. 논리 삭제한 값은 다시 등록할 수 있다. 겹치면 `409 CODE.DUPLICATE`. `code_value_length`보다 길면 `400 CODE.VALUE_LENGTH`. 계층이 아닌 그룹에 상위 코드를 주면 `400 CODE.PARENT_NOT_ALLOWED`. 상위 코드는 같은 그룹의 사용 중인 코드여야 하고, 없으면 `404 CODE.PARENT_NOT_FOUND`, 순환이면 `400 CODE.CYCLE`. `levelNo`와 `codePath`는 서버가 계산한다. 예: `/ROOT/CHILD`. `validFromDate`가 `validToDate`보다 늦으면 `400 CODE.INVALID_DATE`.

이름 번역도 그룹과 같이 언어 기준 upsert이고, 요청에 없는 언어는 유지한다.

등록, 수정, 삭제, 복구는 같은 트랜잭션에서 `common_code_history`에 `C`/`U`/`D`를 남긴다. 복구는 `U`다. 변경 사용자는 아직 없어 NULL이다.

`SECURITY_POLICY` / `TEMP_PASSWORD_VALID_DAYS`의 `extAttr1`은 양의 정수여야 한다. 아니거나 그 코드를 삭제하면 `400 CODE.INVALID_POLICY_VALUE`.

## 이번 단계 오류 코드

| 코드 | HTTP | 의미 |
| --- | --- | --- |
| COMMON.INVALID_PAGE | 400 | 페이지 또는 크기 |
| COMMON.INVALID_SORT | 400 | 정렬 필드 또는 방향 |
| COMMON.VALIDATION_FAILED | 400 | 본문 검증, 필수 그룹, 읽을 수 없는 JSON |
| AUTH.FORBIDDEN | 403 | 기본 프로필의 관리 API |
| CODE.GROUP_NOT_FOUND | 404 | 그룹 없음 또는 조회 API에서 사용 중지 |
| CODE.NOT_FOUND | 404 | 코드 없음 |
| CODE.PARENT_NOT_FOUND | 404 | 상위 코드 없음 |
| CODE.DUPLICATE | 409 | 그룹 코드 또는 사용 중인 코드 값 중복 |
| CODE.GROUP_IN_USE | 409 | 사용 중인 코드가 있어 그룹 삭제 불가 |
| CODE.PARENT_NOT_ALLOWED | 400 | 비계층 그룹의 상위 코드 |
| CODE.CYCLE | 400 | 계층 순환 |
| CODE.VALUE_LENGTH | 400 | 코드 값 길이 |
| CODE.INVALID_DATE | 400 | 시작일, 종료일 |
| CODE.INVALID_POLICY_VALUE | 400 | 임시 비밀번호 유효 일수 |
| CODE.LANGUAGE_NOT_FOUND | 400 | 지원하지 않는 언어 |

## 다국어 조회

권한 선언 없음. 국가 관리도 메뉴 `I18N` 권한을 쓴다.

`GET /api/mdm/v1/i18n/languages` 는 사용 중인 지원 언어 배열이다. `data` 예:

```json
[{ "supportLanguageCode": "ko-KR", "languageName": "한국어", "nativeName": "한국어", "defaultLanguage": true, "fallbackLanguageCode": null, "textDirectionCode": "LTR", "dateFormat": "yyyy-MM-dd", "datetimeFormat": "yyyy-MM-dd HH:mm", "numberFormat": "#,##0.##" }]
```

`GET /api/mdm/v1/i18n/bundles/{namespaceCode}?lang=` 의 `data`는 `{ namespaceCode, language, messages }` 다. 언어는 `lang`, `Accept-Language`, 테넌트 기본 언어 순이다. 문구는 요청 언어, 대체 언어 체인, 테넌트 기본 언어, `default_message` 순이며 순환은 한 번 본 언어에서 끊는다. 약한 `ETag`와 `Cache-Control: no-cache`를 준다. `If-None-Match`가 같으면 본문 없는 `304`다. 이 응답만 래퍼가 없다. 없거나 사용 중지된 네임스페이스는 `404 I18N.NAMESPACE_NOT_FOUND`.

`GET /api/mdm/v1/i18n/bundles?namespaces=a,b` 는 최대 20개. `data`는 네임스페이스 코드를 키로 한 번들 맵이다.

`GET /api/mdm/v1/nations` 는 사용 중인 국가 배열이다. 관리 목록은 같은 경로에 `managed=true`를 붙이며 페이지다. 국가 관리도 `I18N` 메뉴 권한이다.

번들 캐시는 테넌트별 메모리 맵이고 TTL은 60초다. 언어·네임스페이스·메시지 변경은 그 테넌트의 해당 캐시를 바로 지운다. 다른 인스턴스에는 TTL 안에 반영된다.

## 다국어 관리

메뉴 `I18N`.

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| GET | `/api/mdm/v1/support-languages` | READ. 배열. `useYn` 생략 시 사용 중지 포함 |
| POST/PUT/DELETE | `/api/mdm/v1/support-languages` 와 `/{code}`, `/{code}/restore` | 등록, 수정, 사용 중지, 복구 |
| GET | `/api/mdm/v1/nations?managed=true` | READ. 페이지 |
| POST/PUT/DELETE | `/api/mdm/v1/nations` 와 `/{nationCode}`, `/{nationCode}/restore` | 등록, 수정, 사용 중지, 복구 |
| GET/POST/PUT/DELETE | `/api/mdm/v1/i18n-namespaces` | 페이지 목록, 단건, 등록, 수정, 사용 중지, 복구 |
| GET/POST/PUT/DELETE | `/api/mdm/v1/i18n-messages` | 페이지 목록, 단건(번역 포함), 등록, 수정, 논리 삭제, 복구 |

지원 언어는 기본 언어가 활성 1개다. 바꾸면 한 트랜잭션에서 이전 기본을 내린다. 기본 언어이거나 다른 활성 언어의 대체 언어이면 `409 I18N.LANGUAGE_IN_USE`. 대체 체인 순환은 `400 I18N.FALLBACK_CYCLE`. 코드는 BCP 47이고 등록 후 바꿀 수 없다.

국가 코드는 ISO 3166-1 alpha-2 대문자 2자이고 등록 후 바꿀 수 없다. `defaultLanguageCode`는 활성 지원 언어여야 한다.

네임스페이스 코드는 등록 후 바꿀 수 없다. 사용 중지하면 메시지는 남고 번들 조회에서는 빠진다.

메시지 키는 네임스페이스 안 사용 중 행만 유일하다. 논리 삭제 후 같은 키를 다시 등록할 수 있다. `messageTypeCode`는 공통코드 검증 없이 저장한다. 번역은 `(메시지, 언어)` upsert이고 요청에 없는 언어는 유지한다. 문구가 바뀔 때만 `versionNo`가 1 증가한다. 상태는 요청값 또는 신규 `DRAFT`, 기계 번역은 요청값 또는 `N`이다. 없는 언어는 `400 I18N.LANGUAGE_NOT_FOUND`.

`GET /api/mdm/v1/i18n-messages` 목록 항목은 기존 필드를 유지하고 번역 본문 `translations`는 빈 배열이다. 번역 본문은 단건 조회에만 있다. 목록에만 아래 요약 필드를 더한다. 활성 지원 언어(`use_yn='Y'`)만 집계하고, 순서는 언어의 `sort_order`, `support_language_code`다. 비활성 언어의 번역은 세 배열 모두에서 빠진다. 문구가 비어 있으면 그 언어는 번역된 언어가 아니다.

- `translatedLanguageCodes`: 문구가 비어 있지 않은 활성 언어 코드. 없으면 `[]`.
- `missingLanguageCodes`: 활성 지원 언어 중 번역이 없는 언어 코드.
- `translationStatusCodes`: 위 번역들의 `translation_status_code`를 언어 순서로 중복 없이 모은 배열. 예: `["DRAFT","APPROVED"]`.

```json
{
  "messagePk": 10,
  "namespaceCode": "menu",
  "messageKey": "SAVE",
  "messageTypeCode": "LABEL",
  "defaultMessage": "저장",
  "paramDescription": null,
  "useYn": "Y",
  "translations": [],
  "translatedLanguageCodes": ["ko-KR"],
  "missingLanguageCodes": ["en-US"],
  "translationStatusCodes": ["APPROVED"]
}
```

목록의 번역 상태 필터는 기존과 같다. 누락 언어만 보는 필터는 없다. 번들 조회와 단건 조회 필드, 번들 ETag는 바꾸지 않는다.

국가 정렬 필드: `nationCode`, `nationName`, `sortOrder`, `nationPk`. 네임스페이스: `namespaceCode`, `namespaceName`, `namespacePk`. 메시지: `messageKey`, `messageTypeCode`, `messagePk`.

## 이번 단계 오류 코드

| 코드 | HTTP | 의미 |
| --- | --- | --- |
| I18N.NAMESPACE_NOT_FOUND | 404 | 네임스페이스 없음 또는 사용 중지 |
| I18N.LANGUAGE_NOT_FOUND | 400 또는 404 | 언어 없음. 참조는 400, 단건 조회는 404 |
| I18N.NATION_NOT_FOUND | 404 | 국가 없음 |
| I18N.MESSAGE_NOT_FOUND | 404 | 메시지 없음 |
| I18N.DUPLICATE | 409 | 사용 중인 코드·키 중복 |
| I18N.LANGUAGE_IN_USE | 409 | 기본 언어 또는 대체 언어로 쓰여 중지 불가 |
| I18N.FALLBACK_CYCLE | 400 | 대체 언어 순환 |
| I18N.DEFAULT_REQUIRED | 400 | 기본 언어가 하나가 아님 |

## 인증

`POST /api/mdm/v1/auth/login` 본문 `{ "loginId", "password" }`. 성공 `data`는 `accessToken`, `tokenType`(`Bearer`), `expiresIn`(초), `passwordChangeRequired`다. Refresh는 `Set-Cookie: mdm_refresh`만이다. Path는 `/api/mdm/v1/auth`, HttpOnly, SameSite=Lax, Secure는 설정값이다.

`POST /api/mdm/v1/auth/refresh`와 `POST /api/mdm/v1/auth/logout`은 본문이 없고 헤더 `X-MDM-Request: 1`이 필요하다. 없으면 `403 AUTH.CSRF`. refresh는 쿠키를 회전한다. 같은 쿠키가 유예 시간 안에 다시 오면 `409 AUTH.REFRESH_IN_PROGRESS`이고 가족은 유지된다. 프론트는 새 쿠키로 한 번 더 호출한다. 유예가 지난 재사용은 `401 AUTH.TOKEN_REUSED`다.

`POST /api/mdm/v1/auth/password/change` 본문 `{ "currentPassword", "newPassword" }`. 성공하면 새 Access와 새 refresh 쿠키를 준다. `GET /api/mdm/v1/auth/me`는 `userPk`, `loginId`, `displayName`, `supportLanguageCode`, `timezoneId`, `roleCodes`, `passwordChangeRequired`다.

`pwd_change_required=true`이면 비밀번호 변경, 로그아웃, me, refresh 외 테넌트 API는 `403 AUTH.PASSWORD_CHANGE_REQUIRED`다. `GET /api/mdm/v1/menus/my`는 이 허용 목록에 없다.

## 접속 주소

`GET /api/mdm/v1/context`. 인증이 필요 없다. `Cache-Control: no-store`다. 테넌트 필터를 타지 않는다.

판정 순서:

1. `dev` 프로필이고 `X-Tenant-Code`가 있으면 그 테넌트다. 없으면 `404 TENANT.NOT_FOUND`다.
2. 요청 Host의 호스트명(포트 제외, 소문자, 앞뒤 공백 제거)이 `mdm.platform.hosts`에 있으면 플랫폼이다. 환경변수는 `MDM_PLATFORM_HOSTS`다.
3. 그 외에는 Host로 `tenant_domain`을 찾는다. 없으면 `400 TENANT.UNRESOLVED`다. 테넌트가 사용 가능 상태가 아니면 로그인과 같이 `403 TENANT.INACTIVE`다.

```json
{ "kind": "PLATFORM" }
```

```json
{ "kind": "TENANT", "tenantCode": "acme", "tenantName": "에이씨엠이" }
```

`tenantName`은 테넌트 기본 이름이다. 도메인 목록, 상태, 스키마 이름은 주지 않는다. 설정 목록이 비어 있으면 플랫폼으로 판정하지 않고 기동도 막지 않는다. `dev` 기본값은 `localhost,127.0.0.1`이다. 헤더가 없으면 로컬은 플랫폼이고, 헤더가 있으면 테넌트다.

`mdm.platform.hosts`가 비어 있지 않으면 `/api/platform/**`의 Host가 목록에 없을 때 `404 TENANT.NOT_FOUND`다. 목록이 비어 있으면 제한하지 않는다.

## 내 메뉴

`GET /api/mdm/v1/menus/my`. 인증된 테넌트 사용자라면 권한 선언 없이 호출한다. 쿼리 `lang`은 선택이다.

응답 `data`는 배열이다. 페이지 객체가 아니다. `Cache-Control: no-store`다. ETag와 304는 주지 않는다.

```json
{
  "success": true,
  "data": [
    {
      "menuCode": "SYSTEM",
      "parentMenuCode": null,
      "menuName": "시스템",
      "menuTypeCode": "GROUP",
      "routePath": "/system",
      "iconName": null,
      "levelNo": 1,
      "sortOrder": 10,
      "actions": ["READ"]
    },
    {
      "menuCode": "COMPANY",
      "parentMenuCode": "SYSTEM",
      "menuName": "회사",
      "menuTypeCode": "PAGE",
      "routePath": "/system/company",
      "iconName": null,
      "levelNo": 2,
      "sortOrder": 30,
      "actions": ["READ", "CREATE", "UPDATE", "DELETE", "EXPORT"]
    }
  ]
}
```

노출 규칙:

- `use_yn='Y'`이고 `visible_yn='Y'`인 메뉴만 후보가 된다.
- `PAGE`는 그 메뉴의 `READ`가 있을 때만 포함한다. `actions`에는 같은 메뉴의 `READ`가 함께 있는 기능만 들어간다. 그래서 `READ`가 없는 메뉴는 목록에 없고, 있는 메뉴의 `actions`에는 `READ`가 항상 있다.
- `GROUP`은 노출되는 하위 메뉴가 하나라도 있을 때만 포함한다. 하위가 모두 빠지면 그룹도 빠진다. 그룹의 `actions`는 `["READ"]`다.
- 정렬은 `levelNo`, `sortOrder`, `menuCode`다. 트리는 프론트가 조립한다.

권한은 `DbPermissionChecker`의 캐시와 같은 조회를 쓴다. 역할 유효기간, 역할 권한 합집합, `READ`가 없으면 다른 기능을 빼는 규칙은 그 클래스에만 있다.

메뉴 이름 키는 네임스페이스 `menu`의 `message_key`이고 값은 `menu_code`와 같다. 예: `SYSTEM`, `I18N`, `COMPANY`. 언어는 `lang`, 없으면 `Accept-Language`의 첫 언어, 없으면 테넌트 기본 언어다. 그다음 지원 언어의 대체 언어 체인과 테넌트 기본 언어, 메시지 `default_message`를 기존 번들 캐시로 고른다. 키가 없거나 번들 결과가 비어 있으면 `menu.menu_name`을 쓴다.

오류는 인증 없음 `401 AUTH.UNAUTHORIZED`, 비밀번호 변경 강제 `403 AUTH.PASSWORD_CHANGE_REQUIRED`, 토큰의 테넌트와 헤더가 다름 `400 TENANT.HOST_MISMATCH`다.

계정 없음, 비밀번호 불일치, 비활성, 잠금은 모두 `401 AUTH.INVALID_CREDENTIALS`다. 비밀번호 정책 위반은 `400 AUTH.PASSWORD_POLICY`와 `fieldErrors`다. 현재와 같으면 `400 AUTH.PASSWORD_SAME`, 최근 재사용은 `400 AUTH.PASSWORD_REUSED`다.

### FE 연동 시나리오

1. 로그인 화면은 토큰 없이 언어와 `common`, `error`, `auth` 번들을 받는다.
2. 로그인 성공 후 Access는 메모리에만 두고, refresh 쿠키는 브라우저가 보관한다.
3. Access가 만료되면 `X-MDM-Request: 1`과 쿠키로 refresh한다. 새 Access로 원래 요청을 다시 보낸다.
4. refresh가 `409 AUTH.REFRESH_IN_PROGRESS`이면 저장소의 새 쿠키로 한 번 더 refresh한다.
5. refresh가 401이면 로그인 화면으로 보낸다.
6. `passwordChangeRequired`가 true이면 비밀번호 변경 화면만 연다. 변경 후 새 Access를 쓴다.
7. 비밀번호 변경이 필요 없으면 `GET /api/mdm/v1/auth/me` 다음에 `GET /api/mdm/v1/menus/my`를 호출하고, 그 목록에 있는 `routePath`만 라우트로 연다.

프론트 구조는 정하지 않는다. 로컬은 개발 서버 프록시, 운영은 테넌트 Host 아래 같은 사이트를 전제로 한다.

## 메뉴 관리

권한 메뉴 코드는 `MENU`다. 조회 `READ`, 등록 `CREATE`, 수정·정렬·복구 `UPDATE`, 삭제 `DELETE`.

| 메서드 | 경로 | 권한 |
| --- | --- | --- |
| GET | `/api/mdm/v1/menus` | MENU READ |
| GET | `/api/mdm/v1/menus/tree` | MENU READ |
| GET | `/api/mdm/v1/menus/{menuPk}` | MENU READ |
| POST | `/api/mdm/v1/menus` | MENU CREATE |
| PUT | `/api/mdm/v1/menus/{menuPk}` | MENU UPDATE |
| PUT | `/api/mdm/v1/menus/sort-orders` | MENU UPDATE |
| DELETE | `/api/mdm/v1/menus/{menuPk}` | MENU DELETE |
| POST | `/api/mdm/v1/menus/{menuPk}/restore` | MENU UPDATE |

`{menuPk}`는 숫자만 받는다. `GET /menus/my`는 이 경로와 겹치지 않는다.

목록 필터는 `keyword`(코드, 기본 이름), `solutionCode`, `menuTypeCode`, `parentMenuCode`, `visibleYn`, `useYn`이다. `useYn`을 생략하면 사용 중지를 포함한다. 정렬은 `menuCode`, `menuName`, `sortOrder`, `levelNo`, `menuPk`다.

`GET /menus/tree?solutionCode=&includeInactive=`는 페이지 없는 배열이다. `sortOrder`, `menuCode` 순으로 자식을 앞에 둔다. 항목은 `depth`(levelNo), `childCount`, `permissionCount`(사용 중인 권한 수), `children`다. 메뉴와 권한 수를 각각 한 번 조회해 조립한다. 부모가 결과에 없는 노드는 넣지 않는다.

단건은 목록 항목에 `description`, `translations`, `permissions`(permissionCode, actionCode, useYn)를 더한다.

등록 예:

```json
{"solutionCode":"MDM","menuCode":"OPS_BOARD","menuName":"게시판","menuTypeCode":"PAGE","parentMenuCode":"SYSTEM","routePath":"/ops/board","sortOrder":1,"visibleYn":"Y","translations":[{"supportLanguageCode":"ko-KR","messageText":"게시판"}]}
```

`menuCode`는 `^[A-Z][A-Z0-9_]{0,99}$`(1~100자)다. S05 코드가 이 식에 맞는다. 등록 후 바꿀 수 없고, 논리 삭제된 코드도 다시 쓰지 못한다. `solutionCode`는 `^[A-Z][A-Z0-9_]{1,49}$`다. `PAGE`의 `routePath`는 `/`로 시작하고 `//`, `..`, `\`, `://`, 공백이 없다. 사용 중인 다른 PAGE와 같으면 `409 MENU.DUPLICATE_ROUTE`다. `iconName`은 `^[A-Za-z][A-Za-z0-9]*$`다. 상위는 같은 솔루션의 사용 중인 GROUP이다. 아니면 `400 MENU.PARENT_NOT_GROUP`다. `levelNo`와 `menuPath`는 서버가 계산한다. 시드와 같이 코드 경로(`/SYSTEM/I18N`)다. 깊이는 4를 넘기면 `400 MENU.TOO_DEEP`다.

PAGE를 등록하면 같은 트랜잭션에서 `READ`, `CREATE`, `UPDATE`, `DELETE`, `EXPORT` 권한을 만든다. 코드는 `메뉴코드:기능코드`다. 같은 메뉴·기능 행이 있으면 되살리지 않고 건너뛴다. GROUP에는 권한을 만들지 않는다.

수정은 `menuName`, `iconName`, `sortOrder`, `visibleYn`, `description`, `translations`, PAGE의 `routePath`, `parentMenuCode`만 된다. `menuCode`, `solutionCode`, `menuTypeCode`를 보내면 `400 COMMON.VALIDATION_FAILED`다. 상위가 바뀌지 않으면 상위의 사용 여부를 다시 보지 않는다. 자기 자신이나 자손 아래는 `409 MENU.PARENT_CYCLE`다. 이동하면 자손의 `levelNo`, `menuPath`를 한 문장으로 고친다.

정렬은 `[{"menuCode","sortOrder"}]` 최대 200건이다. 없는 코드는 `404 MENU.NOT_FOUND`이고 전부 롤백한다.

사용 중인 하위가 있으면 `409 MENU.HAS_CHILD`다. S05 메뉴 INSERT에 있는 코드는 `409 MENU.PROTECTED`다. 이름, 정렬, 표시, 번역은 고칠 수 있다. 삭제 가능한 PAGE를 지우면 그 메뉴의 permission과 role_permission을 `use_yn='N'`으로 회수한다. 복구는 상위가 사용 중이어야 하고(`409 MENU.PARENT_INACTIVE`) 권한 행은 되살리지 않는다.

메뉴 이름 번역은 `i18n_entity_text`가 아니라 네임스페이스 `menu`, 키=`menu_code`, 유형 `MENU` 메시지다. `menus/my`가 이미 이 키를 읽기 때문이다. 요청에 없는 언어는 유지한다. 단건만 `translations`를 준다. 네임스페이스가 없거나 키가 겹치면 기존 `I18N.*`를 쓴다.

등록을 뺀 변경은 커밋 후 권한 캐시와 `menu` 번들 캐시를 비운다. 그래서 권한 부여·회수·삭제 직후 `menus/my`가 바뀐다.

새 PAGE의 `routePath`는 프런트 `menuRoutes.ts`에 연결되기 전까지 이동해도 404 화면이다.

| 코드 | HTTP | 의미 |
| --- | --- | --- |
| MENU.DUPLICATE | 409 | 메뉴 코드 중복(삭제 포함) |
| MENU.DUPLICATE_ROUTE | 409 | 사용 중인 PAGE 경로 중복 |
| MENU.PARENT_NOT_GROUP | 400 | 상위가 같은 솔루션의 사용 중 GROUP이 아님 |
| MENU.TOO_DEEP | 400 | 깊이 4 초과 |
| MENU.PARENT_CYCLE | 409 | 자기 자신·자손 아래 이동 |
| MENU.NOT_FOUND | 404 | 메뉴 없음 |
| MENU.HAS_CHILD | 409 | 사용 중인 하위 |
| MENU.PROTECTED | 409 | 시드 메뉴 삭제 |
| MENU.PARENT_INACTIVE | 409 | 복구 시 상위가 사용 중이 아님 |

## 조직·사람

회사, 부서, 부서 이력, 사람, 사람 소속이다. 사용자 계정, 역할, 파일, 팀은 포함하지 않는다. 권한 메뉴는 `COMPANY`, `DEPT`, `PERSON`이고 기능은 `READ`, `CREATE`, `UPDATE`, `DELETE`다. 복구는 공통코드와 같이 `UPDATE`다.

이름 번역은 `i18n_entity_text`다. 대상은 `company.company_name`, `dept.dept_name`, `person.person_name`이다. 요청 `translations`는 `{supportLanguageCode, text}`이고, 요청에 있는 언어만 추가하거나 바꾼다. 요청에 없는 언어는 그대로 둔다. 상세 조회만 번역을 포함하고 목록은 포함하지 않는다. 알 수 없거나 사용 중이 아닌 언어는 `400 I18N.LANGUAGE_NOT_FOUND`다.

코드 값이 있을 때만 공통코드 서비스로 확인한다. 그룹은 `COMPANY_TYPE`, `DEPT_TYPE`, `EMPLOYMENT_TYPE`, `ASSIGNMENT_STATUS`, `POSITION`, `JOB_TITLE`이다. 사용 중이고 유효기간 안이 아니면 `400 ORG.INVALID_CODE_VALUE`와 `fieldErrors`다. 값을 생략하면 검사하지 않는다. `POSITION`과 `JOB_TITLE`은 그룹만 있고 시드 값이 없다.

목록의 `useYn`은 7단계와 같다. 생략하면 사용 중지를 포함하고, `Y` 또는 `N`만 거른다. `ALL`은 없다. 부서 트리만 예외다. `asOfDate`가 없으면 오늘 기준 사용 중인 부서만 준다. `includeClosed=true`이면 사용 중지 부서도 오늘 유효기간 안에 있으면 포함한다. `asOfDate`가 있으면 `use_yn`을 보지 않고 `valid_from <= 날짜`이고 `valid_to`가 없거나 `valid_to >= 날짜`인 부서를 준다.

오늘 날짜는 `TenantClock.today()`다. 생성·수정 사용자 PK는 검증된 테넌트 사용자 토큰의 `sub`다. 토큰이 없거나 플랫폼 토큰이면 null이다. 부서 이력의 `changeUserPk`도 같다.

회사 코드와 부서 코드는 만든 뒤 바꾸지 않는다. 코드 유니크는 논리 삭제 행도 포함하므로 같은 코드는 복구로만 다시 쓴다. 사업자등록번호는 앞뒤 공백만 제거하고 하이픈은 유지한다. 사용 중인 행끼리만 유일해서 논리 삭제 뒤 다른 회사가 같은 번호를 쓸 수 있다. 사람 이메일은 형식만 검사하고 중복을 허용한다. `profileFilePk`는 조회에만 나오고 저장 요청은 받지 않는다.

부서 경로 `deptPath`는 `/상위PK/.../자신PK`다. 레벨은 슬래시 개수다. 상위가 바뀌면 이동한 부서와 하위를 경로 접두사 한 번의 `UPDATE`로 다시 계산한다. 과거 시점의 조직도는 재구성하지 않는다.

소속의 주 소속은 한 사람의 현재 소속(종료일 없음, `use_yn='Y'`) 중 하나다. 첫 현재 소속은 요청과 관계없이 `primaryYn='Y'`다. 이후 요청이 `Y`이면 기존 주 소속을 같은 트랜잭션에서 `N`으로 바꾼다. 주 소속을 종료해도 다른 소속을 주 소속으로 올리지 않는다. 이동 API는 없고 종료 뒤 새 소속을 만든다. 사원번호는 같은 회사에서 `use_yn='Y'`이고 값이 있는 행만 유일하다. 종료일은 유니크 조건이 아니다. 다른 회사의 같은 사원번호는 가능하다.

### 회사

`GET/POST /api/mdm/v1/companies`

`GET/PUT/DELETE /api/mdm/v1/companies/{companyPk}`

`POST /api/mdm/v1/companies/{companyPk}/restore`

목록 필터는 `keyword`(코드, 이름, 약칭), `companyTypeCode`, `useYn`이다. 정렬은 `companyCode`, `companyName`, `sortOrder`, `companyPk`다.

```json
{
  "companyCode": "ACME",
  "companyName": "에이씨엠이",
  "companyTypeCode": "OWN",
  "businessRegistrationNo": "123-45-67890",
  "nationCode": "KR",
  "parentCompanyCode": null,
  "translations": [{ "supportLanguageCode": "ko-KR", "text": "에이씨엠이" }]
}
```

수정 본문에는 `companyCode`가 없다. 응답에는 `companyPk`, `parentCompanyPk`, `parentCompanyCode`, `nationPk`, `nationCode`를 함께 둔다.

상위 회사가 자신이거나 순환이면 `409 COMPANY.PARENT_CYCLE`이다. 상위가 없거나 사용 중이 아니면 `404 COMPANY.NOT_FOUND` 또는 `409 COMPANY.PARENT_INACTIVE`다. 국가가 없거나 사용 중이 아니면 `400 COMMON.VALIDATION_FAILED`와 `fieldErrors(nationCode)`다. `I18N.NATION_NOT_FOUND`는 쓰지 않는다. 회사 코드 중복은 `409 COMPANY.DUPLICATE`, 사업자번호 중복은 `409 COMPANY.DUPLICATE_BUSINESS_NO`다.

삭제는 논리 삭제다. 사용 중인 하위 회사는 `409 COMPANY.HAS_CHILD_COMPANY`, 사용 중인 부서는 `409 COMPANY.HAS_ACTIVE_DEPT`, 현재 소속은 `409 COMPANY.HAS_ACTIVE_ASSIGNMENT`다. 사용 중인 자사(`OWN`)가 하나뿐이면 `409 COMPANY.LAST_OWN`이다. 복구 때 상위 회사가 사용 중이 아니면 `409 COMPANY.PARENT_INACTIVE`다.

`companyCode`는 대문자, 숫자, 밑줄, 하이픈 1~50자다. ERD 설명에는 문자 규칙이 없고 길이 50과 유니크만 있다.

### 부서

`GET/POST /api/mdm/v1/departments`

`GET /api/mdm/v1/departments/tree?companyCode=&asOfDate=&includeClosed=`

`GET/PUT/DELETE /api/mdm/v1/departments/{deptPk}`

`POST /api/mdm/v1/departments/{deptPk}/restore`

`GET /api/mdm/v1/departments/{deptPk}/history`

목록 필터는 `companyCode`, `keyword`, `deptTypeCode`, `useYn`, `parentDeptPk`다. 정렬은 `deptCode`, `deptName`, `levelNo`, `sortOrder`, `deptPk`다. 트리는 페이지 없는 배열이고 `deptPath`, `sortOrder`, `deptCode` 순으로 같은 부모 안을 정렬한 전위 순회다.

등록은 같은 회사의 사용 중인 상위 부서, 회사 안에서 유일한 `deptCode`, 이름, 유형, 정렬, `validFromDate`(생략 시 오늘), `validToDate`가 시작일 이상이어야 한다. 서버가 `levelNo`와 `deptPath`를 같은 트랜잭션에서 계산하고 `CREATE` 이력을 남긴다. 수정은 코드와 회사를 바꾸지 않는다. 이름이 바뀌면 `RENAME`, 상위가 바뀌면 `MOVE`다. 둘 다 바뀌면 이력 두 줄이다. `effectiveDate` 생략은 오늘이고 `changeReason`은 선택이다.

상위가 자신 또는 하위이면 `409 DEPT.PARENT_CYCLE`이다. 다른 회사 부서이면 `409 DEPT.PARENT_COMPANY_MISMATCH`다. 사용 중이 아니면 `409 DEPT.PARENT_INACTIVE`다. 요청한 `parentDeptCode`가 현재 상위 코드와 같으면 그 검사를 다시 하지 않는다. 상위가 종료된 부서의 이름 변경은 성공한다. 상위를 바꾸거나 새로 지정할 때만 사용 중, 같은 회사, 순환을 검사한다. 날짜 순서가 아니면 `400 DEPT.INVALID_DATE`다. 코드 중복은 `409 DEPT.DUPLICATE`다.

삭제는 종료다. 사용 중인 하위 부서는 `409 DEPT.HAS_CHILD`, 현재 소속은 `409 DEPT.HAS_ACTIVE_ASSIGNMENT`다. 통과하면 `validToDate`를 종료일로 두고 `use_yn='N'`, `CLOSE` 이력을 남긴다. 복구는 상위가 사용 중이어야 하고 `validToDate`를 비우며 `use_yn='Y'`, `REOPEN` 이력을 남긴다.

이력은 최신순 페이지다. 변경 유형, 적용일, 변경 전후 이름, 변경 전후 상위 PK, 사유, 변경 시각, 변경 사용자 PK를 준다. 이력 수정과 삭제는 없다. 변경 유형은 `DEPT_CHANGE_TYPE`의 `CREATE`, `RENAME`, `MOVE`, `CLOSE`, `REOPEN`이다.

### 사람

`GET/POST /api/mdm/v1/people`

`GET/PUT/DELETE /api/mdm/v1/people/{personPk}`

`POST /api/mdm/v1/people/{personPk}/restore`

목록 필터는 `keyword`(이름, 이메일, 휴대전화, 현재 소속 사원번호), `companyCode`, `deptPk`, `employmentTypeCode`, `assignmentStatusCode`, `useYn`이다. 회사, 부서, 고용 유형, 재직 상태는 주 소속 기준이다. 정렬은 `personName`, `email`, `personPk`다. 각 항목에 주 소속 요약이 있다. 없으면 null이다. 요약은 페이지의 사람 PK를 `IN`으로 한 번 조회한다. 상세는 번역과 현재 소속 목록을 포함한다.

현재 소속이 있으면 `409 PERSON.HAS_ACTIVE_ASSIGNMENT`다. 사용 중인 계정이 있으면 `409 PERSON.HAS_ACCOUNT`다. 계정은 읽기만 하고 만들지 않는다. 복구는 가능하다.

### 소속

`GET/POST /api/mdm/v1/people/{personPk}/assignments`

`PUT/DELETE /api/mdm/v1/person-assignments/{assignmentPk}`

`POST /api/mdm/v1/person-assignments/{assignmentPk}/end`

`POST /api/mdm/v1/person-assignments/{assignmentPk}/restore`

목록은 과거 소속을 포함하고 기본 정렬은 `startDate` 내림차순이다. `useYn`으로 거른다. 회사는 필수이고 사용 중이어야 한다. 부서는 선택이며 있으면 같은 회사의 사용 중인 부서여야 한다. 시작일은 필수이고 종료일은 시작일 이상이다. `deptHeadYn='Y'`는 부서가 있을 때만 가능하다. `RESIGNED`는 종료일이 필요하다. 종료 API의 `endDate` 생략은 오늘이고 `assignmentStatusCode` 생략은 `RESIGNED`다. 이미 `end_date`가 있으면 `409 ASSIGNMENT.ALREADY_ENDED`다. `use_yn='N'`이면 `409 ASSIGNMENT.INACTIVE`이고, 복구한 뒤 종료한다.

같은 사람, 같은 회사, 같은 부서(둘 다 없음도 같음)의 현재 소속이 둘이면 `409 ASSIGNMENT.DUPLICATE`다. 부서와 회사가 다르면 `409 ASSIGNMENT.DEPT_COMPANY_MISMATCH`다. 사용 중이 아닌 부서는 `400 ASSIGNMENT.DEPT_INACTIVE`, 사용 중이 아닌 회사는 `400 ASSIGNMENT.COMPANY_INACTIVE`다. 사원번호 중복은 `409 ASSIGNMENT.EMPLOYEE_NO_DUPLICATE`다. 날짜 오류는 `400 ASSIGNMENT.INVALID_DATE`, 부서장 플래그 오류는 `400 ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT`, 퇴사일 누락은 `400 ASSIGNMENT.RESIGNED_REQUIRES_END`다.

### 오류

| 코드 | HTTP | 의미 |
| --- | --- | --- |
| `ORG.INVALID_CODE_VALUE` | 400 | 공통코드가 사용 중이 아니거나 유효기간 밖 |
| `I18N.LANGUAGE_NOT_FOUND` | 400 | 조직 이름 번역의 언어를 찾을 수 없음 |
| `COMPANY.DUPLICATE` | 409 | 회사 코드 중복. 논리 삭제 코드도 포함 |
| `COMPANY.DUPLICATE_BUSINESS_NO` | 409 | 사용 중인 사업자등록번호 중복 |
| `COMPANY.NOT_FOUND` | 404 | 회사 없음 |
| `COMPANY.PARENT_CYCLE` | 409 | 회사 계층 순환 |
| `COMPANY.PARENT_INACTIVE` | 409 | 상위 회사가 사용 중이 아님 |
| `COMPANY.HAS_CHILD_COMPANY` | 409 | 사용 중인 하위 회사 |
| `COMPANY.HAS_ACTIVE_DEPT` | 409 | 사용 중인 부서 |
| `COMPANY.HAS_ACTIVE_ASSIGNMENT` | 409 | 현재 소속 |
| `COMPANY.LAST_OWN` | 409 | 마지막 자사 |
| `DEPT.DUPLICATE` | 409 | 부서 코드 중복 |
| `DEPT.NOT_FOUND` | 404 | 부서 없음 |
| `DEPT.PARENT_CYCLE` | 409 | 부서 계층 순환 |
| `DEPT.PARENT_COMPANY_MISMATCH` | 409 | 다른 회사의 상위 부서 |
| `DEPT.PARENT_INACTIVE` | 409 | 상위 부서가 사용 중이 아님 |
| `DEPT.HAS_CHILD` | 409 | 사용 중인 하위 부서 |
| `DEPT.HAS_ACTIVE_ASSIGNMENT` | 409 | 현재 소속 |
| `DEPT.INVALID_DATE` | 400 | 부서 날짜 순서 |
| `PERSON.NOT_FOUND` | 404 | 사람 없음 |
| `PERSON.HAS_ACTIVE_ASSIGNMENT` | 409 | 현재 소속 |
| `PERSON.HAS_ACCOUNT` | 409 | 사용 중인 계정 |
| `ASSIGNMENT.NOT_FOUND` | 404 | 소속 없음 |
| `ASSIGNMENT.DUPLICATE` | 409 | 같은 회사·부서의 현재 소속 |
| `ASSIGNMENT.DEPT_COMPANY_MISMATCH` | 409 | 부서와 회사가 다름 |
| `ASSIGNMENT.DEPT_INACTIVE` | 400 | 부서가 사용 중이 아님 |
| `ASSIGNMENT.COMPANY_INACTIVE` | 400 | 회사가 사용 중이 아님 |
| `ASSIGNMENT.EMPLOYEE_NO_DUPLICATE` | 409 | 같은 회사의 사원번호 중복 |
| `ASSIGNMENT.INVALID_DATE` | 400 | 소속 날짜 순서 |
| `ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT` | 400 | 부서 없이 부서장 |
| `ASSIGNMENT.RESIGNED_REQUIRES_END` | 400 | 퇴사인데 종료일 없음 |
| `ASSIGNMENT.ALREADY_ENDED` | 409 | 이미 종료된 소속 |
| `ASSIGNMENT.INACTIVE` | 409 | 사용 중이 아닌 소속은 종료 불가. 복구 후 종료 |

### FE 연동 시나리오

1. `GET /api/mdm/v1/departments/tree?companyCode=`로 현재 조직도를 연다.
2. `POST /api/mdm/v1/people`로 사람을 만들고 `translations`에 필요한 언어만 보낸다.
3. `POST /api/mdm/v1/people/{personPk}/assignments`로 소속을 만든다. 그 사람의 첫 현재 소속은 주 소속이 된다.

## 사용자·역할·권한

계정, 사용자 역할, 역할, 역할 권한, 권한이다. 메뉴 등록과 수정은 포함하지 않는다. 응답에는 PK와 로그인 아이디를 함께 둔다. 다른 엔티티는 `roleCode`, `menuCode`, `actionCode`, `permissionCode`, `personPk`로 가리킨다.

| API | 메뉴 | 기능 |
| --- | --- | --- |
| 사용자 목록·상세·등록·수정·삭제·복구·상태·비밀번호 초기화 | `USER` | `READ` `CREATE` `UPDATE` `DELETE`. 상태, 초기화, 복구는 `UPDATE` |
| 사용자 역할, 역할, 역할 권한 | `ROLE` | `READ` `CREATE` `UPDATE` `DELETE`. 복구와 권한 교체는 `UPDATE` |
| 권한 목록·등록·수정·삭제·복구 | `PERMISSION` | `READ` `CREATE` `UPDATE` `DELETE`. 복구는 `UPDATE` |

시드 `S05`에 `USER`, `ROLE`, `PERMISSION`의 `READ` `CREATE` `UPDATE` `DELETE` `EXPORT`가 있고 `TENANT_ADMIN`에 부여되어 있다. 빠진 권한 행은 없다.

목록의 `useYn` 생략은 사용 중지를 포함한다. 페이지는 1부터다. 정렬 화이트리스트 밖은 `400 COMMON.INVALID_SORT`, 페이지 값이 아니면 `400 COMMON.INVALID_PAGE`다. 오늘은 `TenantClock.today()`다.

`role_name`과 `permission_name` 번역은 `i18n_entity_text`다. 상세만 번역을 포함하고 목록은 포함하지 않는다. 알 수 없는 언어는 `400 I18N.LANGUAGE_NOT_FOUND`다.

임시 비밀번호가 있는 응답만 `Cache-Control: no-store`다. 그 응답 객체의 `toString`은 비밀번호를 가린다. 비밀번호 해시, 평문, 토큰은 로그에 남기지 않는다. 역할 변경 로그는 사용자 PK, 역할 코드, `grant`/`revoke`/`period`만 남긴다. 역할 변경 이력 테이블은 없다. 권한 삭제 시 영향 역할 수는 로그에만 남긴다.

권한 캐시와 계정 상태 캐시는 변경이 커밋된 뒤 바로 비운다. 60초를 기다리지 않는다. 사용자 역할과 계정 상태는 그 사용자, 역할 권한은 그 역할의 사용자, 권한 삭제는 스키마 전체다.

### 사용자

`GET/POST /api/mdm/v1/users`

`GET/PUT/DELETE /api/mdm/v1/users/{userPk}`

`PUT /api/mdm/v1/users/{userPk}/status`

`POST /api/mdm/v1/users/{userPk}/password/reset`

`POST /api/mdm/v1/users/{userPk}/restore`

`GET/PUT /api/mdm/v1/users/{userPk}/roles`

목록 필터는 `keyword`(로그인 아이디, 연결된 사람 이름), `userStatusCode`, `accountTypeCode`, `roleCode`(현재 유효한 역할), `useYn`이다. 정렬은 `loginId`, `userStatusCode`, `lastLoginDatetime`, `createDatetime`, `userPk`다. 항목은 `userPk`, `loginId`, `accountTypeCode`, `userStatusCode`, `personPk`, `personName`(컬럼만), `lastLoginDatetime`, `mustChangePasswordYn`, `roleCodes`, `useYn`이다. `roleCodes`는 페이지 사용자 PK를 한 번의 `IN` 조회로 채운다. 상세는 `supportLanguageCode`, `timezoneId`, `description`, `loginFailCount`, `lockedDatetime`, `lockType`, `passwordChangedDatetime`, `passwordExpireDatetime`, 감사 컬럼, 역할 배열을 더한다. `lockType`은 `LOCKED`이면서 `lockedDatetime`이 없으면 `ADMIN`, `lockedDatetime`이 있으면 `AUTO`, 그 외에는 null이다. 상태 API로 건 잠금은 `lockedDatetime=null`이라 `LOCK_MINUTES`가 지나도 풀리지 않는다. `password_hash`는 주지 않는다.

```json
{
  "loginId": "staff.user@example.com",
  "accountTypeCode": "NORMAL",
  "personPk": 10,
  "supportLanguageCode": "ko-KR",
  "timezoneId": "Asia/Seoul",
  "roleCodes": ["USER"]
}
```

등록 응답 예:

```json
{
  "userPk": 20,
  "loginId": "staff.user@example.com",
  "temporaryPassword": "example-only",
  "passwordExpireDatetime": "2026-10-22T00:00:00",
  "mustChangePasswordYn": "Y"
}
```

`temporaryPassword` 예시는 형식만 보여 준다. 로그인 아이디는 앞뒤 공백을 자른 뒤 소문자다. 영문, 숫자, `._@-` 3~100자다. 계정 유형 생략은 `NORMAL`이고 `ACCOUNT_TYPE`이어야 한다. `personPk`는 선택이다. 있으면 사용 중인 사람이고 다른 사용 중 계정에 연결되지 않아야 한다. 시스템 계정도 사람을 비울 수 있다. 언어는 사용 중이어야 하고 시간대는 IANA다. `roleCodes`는 사용 중인 역할만, 기간 없이 부여한다. 서버가 임시 비밀번호를 만들어 해시하고 `must_change_password_yn='Y'`, 만료는 `TEMP_PASSWORD_VALID_DAYS`, 상태는 `ACTIVE`, 이력은 `INITIAL`이다.

수정은 `personPk`, `supportLanguageCode`, `timezoneId`, `description`만 바꾼다. `personPk`를 비우면 연결을 해제한다. 로그인 아이디, 계정 유형, 상태, 비밀번호는 바꾸지 않는다. 논리 삭제 계정 수정은 `409 USER.INACTIVE`다.

상태 본문은 `{userStatusCode}`이고 `ACTIVE`, `LOCKED`, `DORMANT`만 된다. `WITHDRAWN`은 삭제만 만든다. `LOCKED`에서 `ACTIVE`는 실패 횟수와 잠금 시각을 지운다. `ACTIVE`에서 `LOCKED`는 잠금 시각을 지금으로 둔다. `LOCKED`와 `DORMANT`는 유효한 refresh를 `ADMIN`으로 철회한다. 같은 상태는 `409 USER.INVALID_STATUS_TRANSITION`이다.

초기화는 등록과 같은 응답이다. 새 임시 비밀번호, 변경 필요, 만료 갱신, 실패 횟수 0, 잠금 시각 null, `LOCKED`면 `ACTIVE`, 이력 `RESET`, refresh `ADMIN` 철회, 계정 상태 캐시 삭제다. `password_hash`가 null이면 `409 USER.NO_PASSWORD_ACCOUNT`다.

삭제는 `use_yn='N'`, 상태 `WITHDRAWN`, refresh 철회다. 복구는 `use_yn='Y'`, `ACTIVE`, 실패 횟수 0, 변경 필요 `Y`다. 기존 해시는 유지한다. 복구 때 다른 사용 중 계정이 같은 사람이면 `409 USER.PERSON_ALREADY_LINKED`다.

로그인한 사용자의 잠금, 휴면, 삭제, 역할 변경, 초기화는 `409 USER.SELF_OPERATION_FORBIDDEN`이다. 권한 없음 `403`이 아니다. 본인 비밀번호 변경은 기존 비밀번호 변경 API다.

사용 중이고 `ACTIVE`이며 현재 유효한 `TENANT_ADMIN`이 그 계정뿐이면 삭제, `LOCKED`, `DORMANT`, `TENANT_ADMIN` 제거는 `409 USER.LAST_ADMIN`이다.

사람 삭제 조건 `PERSON.HAS_ACCOUNT`는 사용 중인 `user_account`와 같다.

역할 조회는 과거, 미래, 철회를 포함한다. `useYn`으로 거른다. 교체 본문은 `[{roleCode, validFromDate, validToDate}]`다. 요청에 있는 역할은 부여하거나 기간을 바꾸고, 빠진 사용 중 부여는 `use_yn='N'`이다. 같은 사용자·역할의 철회 행은 되살리고 새 행을 넣지 않는다. 요청 안 중복 코드는 `400`이다. 시작일이 종료일보다 늦으면 `400 ROLE.INVALID_DATE`다. 사용 중이 아닌 역할은 `400 ROLE.INACTIVE`와 `fieldErrors`다. 없는 역할은 `404 ROLE.NOT_FOUND`다.

로그인 아이디 중복은 `409 USER.DUPLICATE_LOGIN_ID`다. 논리 삭제한 아이디도 다시 쓰지 못한다. 계정 유형이 아니면 `400 ORG.INVALID_CODE_VALUE`다. 사람이 없으면 `404 PERSON.NOT_FOUND`다.

### 역할

`GET/POST /api/mdm/v1/roles`

`GET/PUT/DELETE /api/mdm/v1/roles/{rolePk}`

`POST /api/mdm/v1/roles/{rolePk}/restore`

`GET/PUT /api/mdm/v1/roles/{rolePk}/permissions`

목록 필터는 `keyword`(코드, 이름), `useYn`이다. 정렬은 `roleCode`, `roleName`, `sortOrder`, `rolePk`다. `userCount`는 현재 유효한 부여이면서 사용자 `use_yn='Y'`인 수를 페이지마다 한 번 집계한다.

`roleCode`는 `^[A-Z][A-Z0-9_]{1,49}$`다. 만든 뒤 바꾸지 않는다. 중복은 `409 ROLE.DUPLICATE`이고 논리 삭제 코드도 포함한다. 수정은 이름, 정렬, 설명, 번역이다. `TENANT_ADMIN` 삭제는 `409 ROLE.PROTECTED`다. 이름과 설명 수정은 된다. 사용 중인 부여가 날짜와 관계없이 있으면 `409 ROLE.IN_USE`다.

권한 조회는 사용 중인 권한을 메뉴 정렬, 권한 정렬 순으로 주고 `granted`를 붙인다. 필드 `permissionPk`, `permissionCode`, `menuCode`, `menuName`, `actionCode`, `permissionName`, `granted`다. 메뉴 이름은 `menus/my`와 같이 `lang`, `Accept-Language`, 테넌트 기본 언어 순으로 번들을 보고 없으면 `menu.menu_name`이다. `granted=true`면 부여된 것만 준다. 페이지는 없다.

교체 본문은 `{permissionCodes:[...]}`다. 철회된 `role_permission`은 되살린다. 없거나 사용 중이 아닌 코드는 `404 PERMISSION.NOT_FOUND`와 `fieldErrors`다. READ가 아닌 기능은 같은 메뉴 READ가 요청에 있어야 하고, 없으면 `400 ROLE.READ_REQUIRED`와 메뉴 코드를 `fieldErrors`에 둔다. `TENANT_ADMIN` 권한 교체는 `409 ROLE.PROTECTED`다.

### 권한

`GET/POST /api/mdm/v1/permissions`

`GET/PUT/DELETE /api/mdm/v1/permissions/{permissionPk}`

`POST /api/mdm/v1/permissions/{permissionPk}/restore`

목록 필터는 `menuCode`, `actionCode`, `keyword`, `useYn`이다. 정렬은 `permissionCode`, `menuCode`, `actionCode`, `sortOrder`, `permissionPk`다.

```json
{
  "menuCode": "COMPANY",
  "actionCode": "APPROVE",
  "permissionName": "회사승인",
  "sortOrder": 10
}
```

메뉴는 사용 중인 `PAGE`여야 한다. `GROUP`은 `400 PERMISSION.MENU_NOT_PAGE`다. `actionCode`는 사용 중인 `PERMISSION_ACTION`이다. 시드에는 `READ`, `CREATE`, `UPDATE`, `DELETE`, `EXPORT`, `APPROVE`가 있다. `permission_code`는 서버가 `menuCode:actionCode`로 만든다. 같은 메뉴·기능은 `409 PERMISSION.DUPLICATE`다. 논리 삭제 코드는 다시 만들지 않고 복구한다. 수정은 이름, 정렬, 설명, 번역만 된다.

`READ` 삭제는 `409 PERMISSION.PROTECTED`다. 그 외 삭제는 `use_yn='N'`이고 해당 `role_permission`도 `use_yn='N'`이다. 복구는 메뉴가 사용 중인 `PAGE`일 때만 되고 역할 부여는 되살리지 않는다.

### 오류

| 코드 | HTTP | 의미 |
| --- | --- | --- |
| `USER.DUPLICATE_LOGIN_ID` | 409 | 로그인 아이디 중복. 논리 삭제 포함 |
| `USER.PERSON_ALREADY_LINKED` | 409 | 사용 중인 다른 계정에 연결된 사람 |
| `USER.NOT_FOUND` | 404 | 사용자 없음 |
| `USER.INACTIVE` | 409 | 사용 중이 아니거나 탈퇴 |
| `USER.INVALID_STATUS_TRANSITION` | 409 | 허용되지 않거나 같은 상태 |
| `USER.SELF_OPERATION_FORBIDDEN` | 409 | 본인 잠금, 휴면, 삭제, 역할, 초기화 |
| `USER.LAST_ADMIN` | 409 | 마지막 관리자 |
| `USER.NO_PASSWORD_ACCOUNT` | 409 | 비밀번호 해시가 없음 |
| `ROLE.NOT_FOUND` | 404 | 역할 없음 |
| `ROLE.INACTIVE` | 400 | 사용 중이 아닌 역할 부여 |
| `ROLE.INVALID_DATE` | 400 | 역할 기간 순서 |
| `ROLE.DUPLICATE` | 409 | 역할 코드 중복 |
| `ROLE.PROTECTED` | 409 | `TENANT_ADMIN` 삭제 또는 권한 교체 |
| `ROLE.IN_USE` | 409 | 부여가 남아 있음 |
| `ROLE.READ_REQUIRED` | 400 | 같은 메뉴 READ 없음 |
| `PERMISSION.NOT_FOUND` | 404 | 권한 없음 |
| `PERMISSION.DUPLICATE` | 409 | 메뉴·기능 중복 |
| `PERMISSION.MENU_NOT_PAGE` | 400 | 페이지 메뉴가 아님 |
| `PERMISSION.PROTECTED` | 409 | READ 삭제 |
| `PERMISSION.INACTIVE` | 409 | 사용 중이 아닌 권한 수정 |

## 파일·팀

저장소, 파일, 첨부, 프로필 이미지, 팀이다. 응답에 저장 키, 기준 경로, 절대 경로를 넣지 않는다. 파일 본문 예:

```json
{"filePk":1,"originalFileName":"photo.png","fileSize":9,"mimeType":"image/png","fileExtension":"png","checksumSha256":"<64자>","fileStatusCode":"TEMP"}
```

### 권한

| 메뉴 | 기능 | 경로 |
| --- | --- | --- |
| `FILE_STORAGE` | READ CREATE UPDATE DELETE | `/api/mdm/v1/file-storages`. 복구는 UPDATE |
| `FILE` | READ CREATE UPDATE DELETE | `/api/mdm/v1/files`. 업로드는 CREATE, 정리는 DELETE, 복구는 UPDATE |
| 대상 메뉴 | READ UPDATE | 첨부 조회·다운로드는 대상 READ, 연결·해제는 대상 UPDATE |
| `PERSON` | UPDATE READ | 프로필 이미지 설정·해제는 UPDATE. 조회 권한이 없으면 404 |
| `TEAM` | READ CREATE UPDATE DELETE | `/api/mdm/v1/teams`, `/team-members`. 복구와 종료는 UPDATE |

`FILE_STORAGE`는 `TENANT_ADMIN`에만 준다. `FILE`와 `TEAM`은 기존 시드대로 `MANAGER`, `USER` 조회가 남아 있다. 다운로드는 `FILE:READ`이거나, 상태가 TEMP이고 올린 사람일 때 허용한다. Range는 받지 않고 전체를 준다. 다운로드는 파일 전체를 메모리에 올리며, 크기는 업로드 상한으로 제한된다.

### 저장소

목록 필터는 `keyword`(코드·이름), `storageTypeCode`, `useYn`이다. `useYn`을 빼면 사용 중지 행도 포함한다. 정렬은 `storageCode`, `storageName`, `createDatetime`, `storagePk`다.

코드는 만든 뒤 바꾸지 않는다. 논리 삭제한 코드도 다시 쓰지 못한다. `storage_type_code`는 `STORAGE_TYPE`이다. 어댑터는 LOCAL만 동작한다. 다른 유형은 등록할 수 있지만 업로드와 다운로드는 `501 FILE.STORAGE_TYPE_UNSUPPORTED`다.

`base_path`는 절대 경로이거나 `mdm.file.local-root` 아래 상대 경로다. `..`는 `400 FILE.INVALID_PATH`다. 디스크 경로는 `<base>/<테넌트 코드>/yyyy/MM/<uuid>`다. 사용자 파일명은 경로에 넣지 않는다.

사용 중인 기본 저장소는 하나다. 새로 지정하면 이전 기본을 같은 트랜잭션에서 끈다. 기본 저장소 삭제는 `409 FILE.DEFAULT_STORAGE`다. 삭제되지 않은 파일이 있으면 `409 FILE.STORAGE_IN_USE`다. 마지막 사용 중 저장소는 `409 FILE.LAST_STORAGE`다.

기본 저장소가 없고 `mdm.file.local-root`가 있으면 처음 업로드 때 코드 `DEFAULT`, 유형 LOCAL, `base_path` `.`를 만든다. root가 없으면 `409 FILE.NO_DEFAULT_STORAGE`다.

`connection_config`는 JSON 객체다. 키 이름에 password, secret, token, key가 있으면 `400 FILE.SECRET_IN_CONFIG`다. `secret_ref`는 `^[A-Za-z0-9_.-]{1,200}$`만 허용한다. 비밀 값은 저장하지 않는다.

### 업로드와 다운로드

필드는 multipart `file`이다. `storageCode`는 선택이다. 상한은 저장소 `max_file_size_mb`와 `mdm.file.max-size-mb`(기본 50) 중 작은 값이다. 넘으면 `413 FILE.TOO_LARGE`다. 스프링 multipart 한도도 같은 코드다.

확장자는 파일명 마지막 구간이다. 저장소 `allowed_extensions`가 있으면 그것만, 없으면 `mdm.file.allowed-extensions`다. `exe`, `bat`, `cmd`, `com`, `sh`, `js`, `jar`, `msi`, `dll`, `scr`, `ps1`, `vbs`는 항상 거절하고, 중간 구간에 있어도 `400 FILE.EXTENSION_NOT_ALLOWED`다.

매직 바이트로 png, jpg, gif, webp, pdf, zip 계열을 본다. 이 확장자는 판별에 실패해도 `400 FILE.MIME_MISMATCH`다. txt, csv처럼 판별할 수 없는 확장자는 허용하되, 앞 8KB에 NUL 바이트가 있으면 거부한다. 클라이언트의 Content-Type은 믿지 않고 서버가 정한 `mimeType`을 저장한다. 상태는 TEMP다. 체크섬은 SHA-256이다. 임시 파일에 쓴 뒤 옮긴다. DB 저장이 실패하거나 트랜잭션이 롤백되면 디스크 파일을 지운다.

다운로드는 저장된 MIME 또는 `application/octet-stream`이다. `Content-Disposition`은 attachment와 RFC 5987 파일명이다. `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store`다. 삭제됐거나 디스크가 없으면 `404 FILE.NOT_FOUND`다. 로그에는 파일 PK만 남긴다.

목록 필터는 `keyword`(원래 이름), `fileStatusCode`, `uploadUserPk`, `from`, `to`, `useYn`이다. 정렬은 `uploadDatetime`, `originalFileName`, `fileSize`, `filePk`다. 단건은 사용 중인 첨부 목록을 포함한다.

삭제는 사용 중인 첨부나 `person.profile_file_pk`가 있으면 `409 FILE.IN_USE`다. 아니면 `use_yn=N`, 상태 DELETED, `delete_scheduled_datetime`은 지금에 `mdm.file.delete-retention-days`(기본 7)를 더한 값이다.

복구는 디스크 파일이 있을 때만 된다. 없으면 `409 FILE.PURGED`다. 첨부 행이 하나라도 있거나 프로필로 참조되면 ACTIVE, 아니면 TEMP로 되돌린다. 예약 시각은 비운다.

`POST /files/cleanup`은 그대로 둔다. TEMP이면서 올린 시각이 `mdm.file.temp-ttl-hours`(기본 24)보다 오래되면 디스크를 지우고 DELETED, `use_yn=N`으로 둔다. DELETED이면서 예약 시각이 지났으면 디스크를 지우고 행은 남긴다. 디스크 삭제에 성공하면 `delete_scheduled_datetime`을 비워 다음 실행에서 다시 세지 않는다. 디스크 삭제 실패는 건너뛰고 `diskFailureCount`에 센다. 응답은 `expiredTempCount`, `purgedCount`, `diskFailureCount`다. 복구는 예약 시각이 비어 있어도 디스크가 없으면 `409 FILE.PURGED`다.

### 정리 스케줄러

기본은 꺼져 있다. `mdm.file.cleanup.enabled=true`일 때만 스케줄러 빈이 만들어지고, 매일 정해진 시각에 ACTIVE 테넌트 스키마를 차례로 정리한다. 수동 API와 같은 정리 로직이다. 스케줄러용 HTTP 엔드포인트는 없다. 감사 컬럼은 시스템 사용자가 없으면 비운다.

| 키 | 환경 변수 | 기본 |
| --- | --- | --- |
| `mdm.file.cleanup.enabled` | `MDM_FILE_CLEANUP_ENABLED` | `false` |
| `mdm.file.cleanup.cron` | `MDM_FILE_CLEANUP_CRON` | `0 0 3 * * *` |
| `mdm.file.cleanup.zone` | `MDM_FILE_CLEANUP_ZONE` | `Asia/Seoul` |

단일 인스턴스 전제다. 같은 프로세스에서 이전 실행이 끝나기 전에는 다음 실행을 시작하지 않는다. 서버가 여러 대일 때의 중복 방지는 없다.

### 첨부

`POST /api/mdm/v1/attachments` 본문은 `fileId`, `targetTableName`, `targetKey`, 선택 `attachmentCategoryCode`, `sortOrder`, `description`이다. 대상은 `person`, `company`, `dept`, `team`만이다. 그 외는 `400 FILE.TARGET_NOT_ALLOWED`다. 대상 행은 사용 중이어야 하고 없으면 각 도메인의 `NOT_FOUND`다. 키는 PK 문자열이다.

파일은 올린 사람의 TEMP이거나 이미 ACTIVE여야 한다. 다른 사람의 TEMP는 `403 FILE.NOT_OWNER`다. 성공하면 파일은 ACTIVE다. 같은 대상과 파일이 사용 중이면 `409 FILE.ATTACHMENT_DUPLICATE`다. 논리 삭제된 행은 같은 PK로 되살린다.

`GET /attachments?targetTableName=&targetKey=`는 배열이다. `GET /attachments/{attachmentPk}/download`는 대상 READ로 그 파일만 받는다. 첨부 해제는 대상 UPDATE다. 다른 첨부와 프로필 참조가 없어도 파일은 ACTIVE로 두고 삭제 예약은 걸지 않는다. 파일 화면에서 관리자가 지운다.

### 프로필 이미지

`PUT /api/mdm/v1/people/{personPk}/profile-image` 본문은 `{fileId}`다. png, jpg, jpeg, gif, webp만 된다. 그 외는 `400 FILE.NOT_IMAGE`다. TEMP는 올린 사람 것만, ACTIVE는 허용한다. `person.profile_file_pk`를 바꾸고 파일을 ACTIVE로 둔다. 이전 파일 상태는 바꾸지 않는다. DELETE는 참조만 지운다. GET은 이미지를 스트림한다. 권한이 없거나 사진이 없으면 `404 FILE.NOT_FOUND`다.

사람 목록과 단건에 `hasProfileImage`가 있다. 사람 행의 컬럼이라 건마다 추가 조회하지 않는다. 사람 등록 본문에는 `profile_file_pk`가 없다.

### 팀

목록 필터는 `keyword`(코드·이름), `teamTypeCode`, `active=true`(종료일이 없거나 오늘 이후), `useYn`이다. 정렬은 `teamCode`, `teamName`, `sortOrder`, `startDate`, `teamPk`다. `memberCount`는 현재 사용 중인 팀원 수이고 한 번의 집계다. 번역은 단건에만 있고 대상은 `team.team_name`이다.

팀 코드는 `^[A-Z0-9_-]{1,50}$`다. 중복은 `409 TEAM.DUPLICATE`다. 시작일이 종료일보다 늦으면 `400 TEAM.INVALID_DATE`다. 회사와 연결하지 않는다.

현재 팀원(`end_date`가 없고 `use_yn=Y`)이 있으면 삭제는 `409 TEAM.HAS_ACTIVE_MEMBER`다. 종료일을 오늘 이전으로 바꾸는 수정은 현재 팀원이 있어도 된다. 복구는 UPDATE다.

팀원 목록은 이력을 포함하고 기본 정렬은 `startDate` 내림차순이다. `useYn`, `current`로 거른다. 등록 본문은 `personPk`, `teamRoleCode`, `startDate`, 선택 `endDate`, `description`이다. 수정은 역할, 기간, 설명이다. `POST /team-members/{id}/end`의 `endDate`를 빼면 테넌트 오늘이다. 삭제와 복구가 있다.

사람은 사용 중이어야 한다. 없으면 `404 PERSON.NOT_FOUND`, 사용 중지가 `409 PERSON.INACTIVE`다. 같은 팀의 현재 소속은 하나라 중복은 `409 TEAM.MEMBER_DUPLICATE`다. 종료한 뒤 다시 넣으면 새 행이다. 이미 끝난 종료는 `409 TEAM.MEMBER_ALREADY_ENDED`다. 사용 중지가 아닌 팀원 종료는 `409 TEAM.MEMBER_INACTIVE`다. 응답의 사람 이름은 번역하지 않는다. 현재 회사·부서와 팀장 수 제한은 두지 않았다.

### 오류

| 코드 | HTTP | 의미 |
| --- | --- | --- |
| `FILE.STORAGE_DUPLICATE` | 409 | 저장소 코드 중복 |
| `FILE.STORAGE_NOT_FOUND` | 404 | 저장소 없음 |
| `FILE.STORAGE_INACTIVE` | 409 | 사용 중지 저장소 |
| `FILE.STORAGE_IN_USE` | 409 | 파일이 남아 있음 |
| `FILE.DEFAULT_STORAGE` | 409 | 기본 저장소 삭제 |
| `FILE.LAST_STORAGE` | 409 | 마지막 저장소 삭제 |
| `FILE.NO_DEFAULT_STORAGE` | 409 | 기본 저장소와 root 없음 |
| `FILE.STORAGE_TYPE_UNSUPPORTED` | 501 | LOCAL이 아님 |
| `FILE.INVALID_PATH` | 400 | 경로 |
| `FILE.SECRET_IN_CONFIG` | 400 | 연결 설정에 비밀 키 |
| `FILE.TOO_LARGE` | 413 | 크기 |
| `FILE.EXTENSION_NOT_ALLOWED` | 400 | 확장자 |
| `FILE.MIME_MISMATCH` | 400 | 내용과 확장자 |
| `FILE.NOT_FOUND` | 404 | 파일 없음 |
| `FILE.IN_USE` | 409 | 첨부 또는 프로필 |
| `FILE.PURGED` | 409 | 디스크가 이미 없음 |
| `FILE.NOT_DELETED` | 409 | 복구 대상이 아님 |
| `FILE.NOT_ATTACHABLE` | 409 | TEMP/ACTIVE가 아님 |
| `FILE.NOT_OWNER` | 403 | 다른 사람의 TEMP |
| `FILE.NOT_IMAGE` | 400 | 프로필 형식 |
| `FILE.TARGET_NOT_ALLOWED` | 400 | 첨부 대상 |
| `FILE.ATTACHMENT_DUPLICATE` | 409 | 같은 첨부 |
| `TEAM.DUPLICATE` | 409 | 팀 코드 |
| `TEAM.NOT_FOUND` | 404 | 팀 없음 |
| `TEAM.INACTIVE` | 409 | 사용 중지 팀 |
| `TEAM.INVALID_DATE` | 400 | 기간 |
| `TEAM.HAS_ACTIVE_MEMBER` | 409 | 현재 팀원 |
| `TEAM.MEMBER_NOT_FOUND` | 404 | 팀원 없음 |
| `TEAM.MEMBER_DUPLICATE` | 409 | 현재 소속 중복 |
| `TEAM.MEMBER_ALREADY_ENDED` | 409 | 이미 종료 |
| `TEAM.MEMBER_INACTIVE` | 409 | 사용 중지 팀원 |
| `PERSON.INACTIVE` | 409 | 사용 중지 사람 |

### 한계

Range는 지원하지 않는다. 다운로드는 파일 전체를 메모리에 올리며, 크기는 업로드 상한으로 제한된다. 정리 스케줄러는 기본이 꺼져 있고 단일 인스턴스 전제다. LOCAL 외 어댑터는 없다. 바이러스 검사는 없다. 업로드는 지금 `FILE:CREATE`가 필요하다.



