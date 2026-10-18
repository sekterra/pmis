# 플랫폼 API

기본 경로 `/api/platform/v1`. 플랫폼 Access 토큰(`aud=mdm-platform`, `typ=platform`, `tenant` 클레임 없음)이 필요하다. 활성 운영자면 별도 메뉴 권한 없이 호출할 수 있다. 테넌트 토큰으로 이 경로를 호출하거나, 플랫폼 토큰으로 `/api/mdm/**` 업무 API를 호출하면 `401 AUTH.TOKEN_REJECTED`다.

플랫폼 요청은 테넌트 필터와 테넌트 문구 번들을 타지 않는다. 오류 메시지는 코드 안 한글이 기본이고, `Accept-Language`가 `en`으로 시작하면 코드 안 영문이다.

정책 숫자는 공통코드가 아니라 `mdm.security.platform.*`이다. 기본값은 access 15분, refresh 14일, 실패 5회, 잠금 30분, 비밀번호 길이 12, 문자 종류 3, 만료 90일, 임시 비밀번호 3일이다. 테넌트 정책과 숫자를 공유하지 않는다.

## 인증

| 메서드 | 경로 | 인증 |
| --- | --- | --- |
| POST | `/api/platform/v1/auth/login` | 공개 |
| POST | `/api/platform/v1/auth/refresh` | 공개. 헤더 `X-MDM-Request: 1` |
| POST | `/api/platform/v1/auth/logout` | 인증. 같은 CSRF 헤더 |
| POST | `/api/platform/v1/auth/password/change` | 인증 |
| GET | `/api/platform/v1/auth/me` | 인증 |

로그인 본문:

```json
{ "loginId": "ops@example.com", "password": "secret" }
```

성공 `data`:

```json
{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 900, "passwordChangeRequired": true }
```

Refresh 쿠키 이름은 `mdm_platform_refresh`, Path는 `/api/platform/v1/auth`, HttpOnly, SameSite=Lax, Secure는 `mdm.security.cookie.secure`다. 원문은 쿠키로만 가고 DB에는 SHA-256 해시만 저장한다. 회전, 10초 유예 `409 AUTH.REFRESH_IN_PROGRESS`, 유예가 지난 재사용 `401 AUTH.TOKEN_REUSED`는 테넌트와 같다. 로그아웃은 제시된 토큰만 `LOGOUT`으로 폐기하고 쿠키를 지운다. 쿠키가 없어도 CSRF 헤더가 있으면 200이다.

로그인 실패는 계정 없음, 비밀번호 불일치, 비활성, 잠금을 구분하지 않고 `401 AUTH.INVALID_CREDENTIALS`다. 계정이 없어도 더미 BCrypt 비교를 한다.

비밀번호 변경 순서는 현재 비밀번호, 현재와 같은 값, 길이/종류/로그인 아이디 포함이다. 이력 테이블이 없어 과거 해시 재사용 검사는 하지 않는다. `pwd_change_required=true`이면 password/change, logout, me, refresh 외 플랫폼 API는 `403 AUTH.PASSWORD_CHANGE_REQUIRED`다.

`me` 필드: `platformAdminPk`, `loginId`, `adminName`, `email`, `passwordChangeRequired`. 역할 배열은 없다.

## 테넌트 관리

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/tenants` | 페이징. `status`, `keyword`, `page`, `size`, `sort` |
| GET | `/tenants/{tenantCode}` | 도메인, 프로비저닝 단계, 마지막 마이그레이션 |
| POST | `/tenants` | 생성 후 프로비저닝. 임시 비밀번호 1회. `Cache-Control: no-store` |
| POST | `/tenants/{tenantCode}/provisioning/retry` | `FAILED` 또는 `PROVISIONING`만 |
| POST | `/tenants/{tenantCode}/admin-password/reissue` | 임시 비밀번호 1회. `no-store` |
| POST | `/tenants/{tenantCode}/suspend` | `ACTIVE`에서 `SUSPENDED`. `changeReason` 필수 |
| POST | `/tenants/{tenantCode}/resume` | `SUSPENDED`에서 `ACTIVE` |
| GET | `/tenants/{tenantCode}/domains` | 활성 도메인 |
| POST | `/tenants/{tenantCode}/domains` | 추가. 첫 활성 도메인이 primary |
| DELETE | `/tenants/{tenantCode}/domains/{domainName}` | 논리 삭제(`use_yn='N'`, `primary_yn='N'`). 마지막 활성 도메인은 `409 TENANT.LAST_DOMAIN`. primary를 지우면 남은 활성 도메인 중 가장 작은 PK를 primary로 올린다. 삭제한 이름은 같은 테넌트나 다른 테넌트에 다시 등록할 수 있다 |
| POST | `/tenants/{tenantCode}/upgrade` | 해당 ACTIVE 테넌트 |
| POST | `/tenants/upgrade` | ACTIVE 전체. 그 외 상태는 건너뜀 |
| GET | `/tenants/{tenantCode}/migrations` | `tenant_schema_migration` 페이징 |

생성 본문 예:

```json
{
  "tenantCode": "acme",
  "tenantName": "Acme",
  "defaultLanguageCode": "ko-KR",
  "timezoneId": "Asia/Seoul",
  "contractStartDate": "2026-01-01",
  "contractEndDate": null,
  "adminName": "관리자",
  "adminEmail": "admin@example.com",
  "domains": ["acme.example.com"]
}
```

스키마명은 요청으로 받지 않는다. 도메인은 소문자 호스트명이다. 첫 도메인은 `primary_yn='Y'`, `verified_yn='Y'`다. 도메인 소유 검증은 하지 않는다. 사용 중인 `domain_name` 중복은 `409 TENANT.DUPLICATE`다. 논리 삭제한 이름은 `use_yn='Y'` 부분 유니크라 다시 등록된다. 정렬 허용 필드는 `tenantCode`, `tenantName`, `tenantStatusCode`, `createDatetime`이고 마이그레이션은 `startedDatetime`, `executionStatusCode`다. 페이지는 1부터다.

목록 항목은 기존 필드에 `provisioningStatus`(최신 `tenant_provisioning.current_step_code`, 없으면 null), `primaryDomain`(활성이고 primary인 도메인, 없으면 null), `createDatetime`(UTC ISO-8601)을 더한다. `createDatetime` 정렬은 `tenant.create_datetime`이다.

생성, 프로비저닝 재시도, 비밀번호 재발급 응답은 `adminLoginId`를 포함한다. 값은 `user_account.login_id`다. 생성 시 로그인 아이디는 관리자 이메일을 앞뒤 공백 제거 후 소문자로 저장한 값이고, 응답은 그 컬럼을 다시 읽는다. 계정이 이미 있어 임시 비밀번호가 null이어도 `adminLoginId`는 있다. 생성과 재발급은 `Cache-Control: no-store`다.

`POST /tenants/upgrade`는 ACTIVE 전체를 목록으로 만들어 업그레이드한다. 그 외 상태는 `skippedTenantCodes`다.

불가능한 상태 변경은 `409 TENANT.INVALID_STATUS_TRANSITION`이다. `TERMINATED`로 바꾸는 API는 없다. 변경 작업의 `update_user_pk`와 `tenant_status_history.change_user_pk`는 운영자 JWT `sub`다.

응답 본문, 로그, 마이그레이션 `error_message`에 임시 비밀번호, Access, Refresh, 쿠키를 남기지 않는다. 생성과 재발급 응답에만 임시 비밀번호가 한 번 들어간다.

테넌트 사용자 로그인은 등록한 도메인을 Host로 보낸다. dev 헤더 없이 Host만으로 테넌트를 찾는다.

## 오류 코드

`AUTH.INVALID_CREDENTIALS`, `AUTH.CSRF`, `AUTH.TOKEN_EXPIRED`, `AUTH.TOKEN_REUSED`, `AUTH.REFRESH_IN_PROGRESS`, `AUTH.ACCOUNT_DISABLED`, `AUTH.UNAUTHORIZED`, `AUTH.TOKEN_REJECTED`, `AUTH.PASSWORD_CURRENT`, `AUTH.PASSWORD_SAME`, `AUTH.PASSWORD_POLICY`, `AUTH.PASSWORD_CHANGE_REQUIRED`, `TENANT.NOT_FOUND`, `TENANT.DUPLICATE`, `TENANT.ALREADY_ACTIVE`, `TENANT.INACTIVE`, `TENANT.INVALID_STATUS_TRANSITION`, `TENANT.LAST_DOMAIN`, `COMMON.VALIDATION_FAILED`, `COMMON.INVALID_PAGE`, `COMMON.INVALID_SORT`.

## 후속 과제

- 일괄 업그레이드는 요청 안에서 동기 실행한다. 테넌트 수가 늘면 비동기로 바꾼다.
- 도메인 소유 검증은 범위 밖이다. 논리 삭제한 도메인 이름은 다시 등록할 수 있다. 사용 중인 이름만 `409 TENANT.DUPLICATE`다.
- 테넌트 폐기(`TERMINATED`)는 범위 밖이다.
