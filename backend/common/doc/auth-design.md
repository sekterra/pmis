# 인증 설계

구현은 8.1 이후다. 이 문서는 코드, 의존성, DDL, Seed를 바꾸지 않는다. 확정된 전제 위에 현재 테이블과 필터를 기준으로 결정안을 적는다.

확정된 전제: Spring Security와 OAuth2 Resource Server(Nimbus), Access JWT는 RS256, Refresh는 HttpOnly 쿠키, 클레임은 최소, 권한은 토큰에 넣지 않음, Host와 검증된 토큰의 테넌트가 일치, `X-Tenant-Code`는 `dev`만.

## 토큰

Access 토큰 기본 수명은 15분이고 응답 본문으로만 준다. 프론트는 메모리에만 두고 `Authorization: Bearer`로 보낸다. 저장소에 넣지 않는다.

Refresh 토큰 기본 수명은 14일이다. JWT가 아니라 256비트 난수다. 응답 본문에 넣지 않고 쿠키로만 준다.

| 항목 | 테넌트 사용자 | 플랫폼 운영자 |
| --- | --- | --- |
| 쿠키 이름 | `mdm_refresh` | `mdm_platform_refresh` |
| Path | `/api/mdm/v1/auth` | `/api/platform/v1/auth` |
| HttpOnly | 예 | 예 |
| Secure | 운영은 예. `mdm.security.cookie.secure`가 false일 때만 끔 | 같음 |
| SameSite | Lax | Lax |
| 값 | 난수 원문 | 난수 원문 |

`mdm.security.cookie.secure` 기본값은 true다. 로컬 http에서는 false로 둔다. 운영 프로필에서 false면 기동을 거부한다.

DB에는 SHA-256 해시만 저장한다. 컬럼은 이미 있는 `token_hash VARCHAR(255)`다. BCrypt는 조회 키가 될 수 없어 Refresh 해시에는 쓰지 않는다. 비밀번호 해시만 기존 BCrypt(강도 기본 10)를 쓴다.

### CSRF

쿠키를 읽는 엔드포인트는 `refresh`와 `logout`이다. SameSite=Lax에 더해 요청 헤더 `X-MDM-Request: 1`이 없으면 `403 AUTH.CSRF`다. 브라우저의 단순 폼 전송은 이 헤더를 붙이지 못한다. Access만 쓰는 API는 Bearer라 쿠키 CSRF 대상이 아니다. 전역 CSRF 필터는 지금처럼 끄고, 이 헤더 검사를 쿠키 엔드포인트에만 둔다.

### 클레임

Access JWT만 다음 클레임을 가진다. 역할과 permission은 넣지 않는다.

| 클레임 | 테넌트 사용자 | 플랫폼 운영자 |
| --- | --- | --- |
| iss | 설정 `mdm.security.jwt.issuer` | 같은 발급자 |
| sub | `user_pk` 문자열 | `platform_admin_pk` 문자열 |
| aud | `mdm-api` | `mdm-platform` |
| iat, exp, jti | 예 | 예 |
| tenant | 테넌트 코드 | 없음 |
| typ | `tenant-user` | `platform` |
| pwd_change_required | 임시 또는 만료면 true | 같음 |

`kid`는 헤더에 둔다. 테넌트 토큰으로 `/api/platform/**`를 호출하거나, 플랫폼 토큰으로 `/api/mdm/**` 업무 API를 호출하면 `401 AUTH.TOKEN_REJECTED`다. `aud`와 `typ`이 둘 다 맞아야 한다.

## 서명 키와 JWKS

개인키는 PKCS#8 PEM이다. 경로만 환경변수 `MDM_JWT_PRIVATE_KEY_PATH`로 받는다. 키 내용은 저장소, 로그, 채팅, DB에 두지 않는다.

`dev`에서 경로가 없으면 기동 시 메모리에만 임시 RSA 키를 만든다. 재시작으로 폐기된다. 로그에는 `kid`만 남긴다. 그 외 프로필에서 키가 없으면 기동을 거부한다.

공개키는 `GET /api/mdm/v1/auth/jwks`로 준다. 본문은 JWKS JSON이고 `ApiResponse`로 감싸지 않는다. 개인키는 포함하지 않는다. PMIS는 이 엔드포인트로 검증한다.

키 교체: 서명에 쓰는 현재 `kid`와, 이전 공개키를 겹치는 동안 JWKS에 함께 둔다. Access는 15분이면 끝나므로 겹침은 1시간이면 된다. Refresh는 JWT가 아니라 키 교체와 무관하다. 이전 공개키 경로는 `MDM_JWT_PREVIOUS_PUBLIC_KEY_PATH`(선택)다. 검증은 JWKS의 `kid`로 고른다. 모르는 `kid`는 거절한다.

## 테넌트 식별과 필터

현재 `TenantResolutionFilter`는 최우선이다. `UnavailableTenantClaimReader`는 항상 빈 값이다. `TenantRequestIdentifier`는 `dev` 헤더가 있으면 Host/JWT 대조 없이 헤더를 쓴다. 헤더가 없으면 Host와 JWT가 둘 다 있을 때 같아야 하고, 다르면 `TENANT.HOST_MISMATCH`다. 로그인처럼 토큰이 없으면 Host만으로 식별한다.

8.1에서 `UnavailableTenantClaimReader`를 Nimbus 디코더를 쓰는 구현으로 바꾼다. Authorization이 없으면 빈 값이다. 있으면 서명을 검증한 뒤 `tenant` 클레임만 읽는다. 서명 전 페이로드는 쓰지 않는다. 디코더 빈은 Resource Server와 같은 것을 쓴다.

필터 순서:

1. Spring Security. 공개 경로는 인증 없이 통과하고, 그 외 Bearer를 검증한다. CSRF는 끈다.
2. `TenantResolutionFilter`. Security 다음으로 옮겨 Host, 검증된 토큰, `dev` 헤더를 합친다. 스키마를 `TenantContext`에 넣는다. `/actuator/**`, `/api/platform/**`, JWKS, `GET /api/mdm/v1/context`는 테넌트 필터를 타지 않는다. context는 로그인 전에 플랫폼 콘솔과 테넌트를 가른다. `dev`이고 `X-Tenant-Code`가 있으면 그 테넌트다. 아니면 요청 Host가 `mdm.platform.hosts`에 있으면 플랫폼이고, 아니면 `tenant_domain`으로 테넌트를 찾는다. `mdm.platform.hosts`가 비어 있지 않으면 `/api/platform/**`는 그 목록의 Host만 받는다. 목록 밖은 404다.
3. 비밀번호 변경 강제 필터. `pwd_change_required=true`이면 아래 허용 목록 외는 `403 AUTH.PASSWORD_CHANGE_REQUIRED`.
4. 기존 `PermissionInterceptor`. `@RequirePermission`만 검사한다.

`dev` 헤더는 Authorization이 없을 때만 쓴다. 토큰과 헤더가 둘 다 있고 값이 다르면 `TENANT.HOST_MISMATCH`다. 로그인 요청은 토큰이 없으므로 Host 또는 `dev` 헤더로 테넌트를 정한 뒤 그 스키마에서 계정을 찾는다.

## 테넌트 사용자 API

경로 `/api/mdm/v1/auth`. 래퍼는 기존 `ApiResponse`다. 성공 예:

```json
{ "success": true, "data": {} }
```

### 로그인

`POST /api/mdm/v1/auth/login`

```json
{ "loginId": "admin@example.com", "password": "..." }
```

`loginId`는 저장 전 소문자로 맞춘다. 계정 없음, 비밀번호 불일치, 비활성, 잠금을 응답에서 구분하지 않는다. 모두 `401 AUTH.INVALID_CREDENTIALS`, 문구는 "로그인에 실패했습니다."다. `showType`은 2다.

내부 `user_login_history.login_result_code`는 기존 `LOGIN_RESULT`를 쓴다.

| 상황 | 내부 코드 | user_pk |
| --- | --- | --- |
| 성공 | SUCCESS | 있음 |
| 비밀번호 불일치, 해시 없음 | FAIL_PASSWORD | 있으면 기록 |
| 잠금 | FAIL_LOCKED | 있음 |
| 중지, 휴면, 탈퇴, use_yn=N | FAIL_INACTIVE | 있으면 기록 |
| 계정 없음 | FAIL_NOT_FOUND | NULL |
| 비밀번호 만료만으로 로그인을 막지는 않음 | SUCCESS | 있음. 토큰에 pwd_change_required |

`FAIL_EXPIRED`는 쓰지 않는다. 만료는 로그인 성공 뒤 변경 강제다. IP는 `X-Forwarded-For`의 첫 값(프록시를 신뢰할 때만)이고, 아니면 `remoteAddr`다. User-Agent는 500자로 자른다. 비밀번호, 토큰, 쿠키 값은 이력과 로그에 남기지 않는다.

성공 시 `login_fail_count=0`, `last_login_datetime`을 갱신한다. `locked_datetime`이 있는 자동 잠금만 `LOCK_MINUTES`가 지나면 상태를 `ACTIVE`로 되돌리고 `locked_datetime`을 비운다. 관리자 잠금은 `locked_datetime`이 비어 있어 시간이 지나도 풀리지 않는다.

응답 `data`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "passwordChangeRequired": false
}
```

Refresh는 `Set-Cookie`만 있다.

`pwd_change_required`는 `must_change_password_yn='Y'`이거나 `password_expire_datetime`이 지금 이전이면 true다. 지금은 테넌트 `timezone_id`가 아니라 UTC instant로 비교한다. 컬럼이 TIMESTAMP이기 때문이다.

이 토큰으로 허용하는 API는 `POST /auth/password/change`, `POST /auth/logout`, `GET /auth/me`뿐이다. 추천으로는 `POST /auth/refresh`도 허용한다. 변경 화면이 15분을 넘길 수 있어서다. 갱신된 Access도 `pwd_change_required=true`를 유지한다. 결정 목록에 둔다.

### Refresh

`POST /api/mdm/v1/auth/refresh`. 본문 없음. 쿠키와 `X-MDM-Request`가 필요하다.

해시를 찾고 그 사용자가 요청 테넌트 스키마 안에 있어야 한다. 행은 `SELECT ... FOR UPDATE`로 잠가 동시 refresh를 직렬화한다. 만료면 `401 AUTH.TOKEN_EXPIRED`이고 행은 그대로 둔다. `revoked_datetime`이 있으면 재사용이다. 다만 사유가 `ROTATED`이고 폐기 시각부터 `mdm.security.refresh-reuse-grace-seconds`(기본 10초) 이내이면 가족을 폐기하지 않고 `409 AUTH.REFRESH_IN_PROGRESS`를 준다. 유예가 지난 `ROTATED`만 `REUSE_DETECTED`로 가족 전체를 폐기하고 `401 AUTH.TOKEN_REUSED`다. `LOGOUT`, `PASSWORD_CHANGED`, `ADMIN`으로 폐기된 토큰은 유예 대상이 아니다. 정상이면 기존 행을 `ROTATED`로 폐기하고 같은 가족으로 새 해시를 넣은 뒤 쿠키와 Access를 다시 준다. 계정이 `ACTIVE`가 아니거나 `use_yn='N'`이면 `401 AUTH.ACCOUNT_DISABLED`이고 그 가족을 `ADMIN`으로 폐기한다.

### 로그아웃

`POST /api/mdm/v1/auth/logout`. 제시된 Refresh를 `LOGOUT`으로 폐기하고 쿠키를 Max-Age=0으로 지운다. 가족 전체가 아니라 그 토큰만 폐기한다. 쿠키가 없어도 200으로 쿠키를 지운다.

### 비밀번호 변경

`POST /api/mdm/v1/auth/password/change`

```json
{ "currentPassword": "...", "newPassword": "..." }
```

검사 순서는 현재 비밀번호 일치, 새 비밀번호가 현재와 같은지(`400 AUTH.PASSWORD_SAME`), 길이·문자 종류·로그인 아이디 포함(`400 AUTH.PASSWORD_POLICY`, `fieldErrors`), 최근 `PASSWORD_HISTORY_COUNT`개 재사용(`400 AUTH.PASSWORD_REUSED`, 현재 해시 포함)이다. 재사용은 `user_password_history`와 현재 `password_hash`에 `BCrypt.matches`로 본다. 해시는 salt가 달라 문자열 비교를 하지 않는다. 현재 비밀번호가 틀리면 `400 AUTH.PASSWORD_CURRENT`다.

성공 시 해시 교체, `must_change_password_yn=N`, `password_changed_datetime`과 `password_expire_datetime` 갱신, 이력 `change_type_code`는 만료였으면 `EXPIRED_CHANGE`, 임시였으면 `CHANGE`가 아니라 최초 변경은 `CHANGE`로 둔다. 프로비저닝이 이미 `INITIAL`을 썼으므로 사용자가 처음 바꾸는 것도 `CHANGE`다. 그 사용자의 Refresh는 모두 `PASSWORD_CHANGED`로 폐기하고 새 쿠키를 준다. Access도 새로 주고 `pwd_change_required=false`다.

### 내 정보

`GET /api/mdm/v1/auth/me`

```json
{
  "userPk": 1,
  "loginId": "admin@example.com",
  "displayName": "관리자",
  "supportLanguageCode": "ko-KR",
  "timezoneId": "Asia/Seoul",
  "roleCodes": ["TENANT_ADMIN"],
  "passwordChangeRequired": false
}
```

`displayName`은 `person`이 있으면 사람 이름, 없으면 로그인 아이디다. 권한 코드 목록은 넣지 않는다. 역할은 `user_role`이 유효하고 `use_yn='Y'`인 역할 코드다.

### 내 메뉴

`GET /api/mdm/v1/menus/my`. 로그인한 사용자면 권한 선언 없이 호출한다. `use_yn='Y'`이고 `visible_yn='Y'`인 메뉴만 평면 목록으로 준다. `PAGE`는 `READ`가 있을 때만 포함하고, `GROUP`은 노출되는 하위가 하나라도 있을 때만 포함한다. 하위가 모두 빠지면 그룹도 빠진다. 그룹 `actions`는 `["READ"]`다. 필드는 `menuCode`, `parentMenuCode`, `menuName`, `menuTypeCode`, `routePath`, `iconName`, `levelNo`, `sortOrder`, `actions`다. 이름은 네임스페이스 `menu`에서 `message_key = menu_code`인 번역이고, 없으면 `menu_name`이다. `Cache-Control: no-store`다. 트리 조립은 프론트다.

## 보안 정책 코드

그룹 `SECURITY_POLICY`. 코드 API는 이 키 전부를 검사한다. `LOCK_MINUTES`만 0을 허용하고 나머지는 양의 정수다. `PASSWORD_MIN_CLASSES`는 1~4, `PASSWORD_MIN_LENGTH`는 8 이상, `PASSWORD_HISTORY_COUNT`는 1 이상이다. 논리 삭제도 거절한다. 없거나 규칙에 맞지 않으면 로그인은 `500 TENANT.SECURITY_POLICY_MISSING`이다.

| code_value | 기본 ext_attr1 | 의미 |
| --- | --- | --- |
| TEMP_PASSWORD_VALID_DAYS | 7 | 이미 있음. 임시 비밀번호 일수 |
| PASSWORD_EXPIRE_DAYS | 90 | 변경 후 만료 일수 |
| PASSWORD_MIN_LENGTH | 10 | 최소 길이 |
| PASSWORD_MIN_CLASSES | 3 | 대문자, 소문자, 숫자, 특수문자 중 최소 종류 |
| PASSWORD_HISTORY_COUNT | 5 | 최근 N개 재사용 금지. 현재 해시 포함 |
| MAX_LOGIN_FAIL_COUNT | 5 | 이 횟수에 도달하면 LOCKED |
| LOCK_MINUTES | 30 | 잠금 후 자동 해제 분. 0이면 자동 해제 없음 |
| ACCESS_TOKEN_MINUTES | 15 | Access 수명 |
| REFRESH_TOKEN_DAYS | 14 | Refresh 수명 |

잠금: 실패마다 `login_fail_count`를 올린다. 횟수에 도달하면 `user_status_code=LOCKED`, `locked_datetime=지금`이다. `LOCK_MINUTES`가 지난 뒤의 성공한 로그인은 이 자동 잠금만 `ACTIVE`로 되돌린다. `LOCKED`이면서 `locked_datetime`이 NULL이면 관리자 잠금이라 시간이 지나도 `FAIL_LOCKED`로 거절한다. 관리자 해제는 상태 API의 `ACTIVE` 또는 비밀번호 재발급이다.

플랫폼 운영자는 테넌트 공통코드가 없다. 같은 기본값을 `mdm.security.platform.*` 설정으로 둔다. 테넌트 정책과 숫자를 공유하지 않는다.

## 플랫폼 운영자

경로 `/api/platform/v1/auth`. 테이블은 공용 스키마 `platform_admin`, `platform_admin_login_history`, `platform_admin_refresh_token`이다. 컬럼 구성은 테넌트 Refresh, 로그인 이력과 같다. `token_family_id`와 `revoke_reason_code`가 이미 있다.

로그인, refresh, logout, password change, me의 모양은 테넌트와 같다. me에는 역할 배열을 넣지 않는다. 플랫폼 권한 테이블이 없다. 활성 운영자면 플랫폼 API를 호출할 수 있다. 메뉴 권한 모델은 만들지 않는다.

테넌트 필터를 타지 않는다. 토큰에 `tenant`가 있으면 거절한다.

최초 계정 안:

| 안 | 내용 | 단점 |
| --- | --- | --- |
| A. 일회 부트스트랩 | 기동 인자 또는 `MDM_BOOTSTRAP_PLATFORM_ADMIN=true`이고 운영자 행이 0일 때만 생성. 로그인 아이디는 `MDM_BOOTSTRAP_ADMIN_LOGIN`. 임시 비밀번호는 생성해 표준 출력에 한 번만 찍고 로그 파일에는 남기지 않는다. 행이 있으면 거부 | 출력 캡처를 운영 절차로 막아야 한다 |
| B. 환경변수 비밀번호 | `MDM_BOOTSTRAP_ADMIN_PASSWORD`로 초기 해시 | 평문이 환경에 남는다 |
| C. Seed 해시 | 마이그레이션에 고정 해시 | 저장소에 알려진 비밀번호가 된다 |

추천은 A다. B와 C는 쓰지 않는다.

## 권한

`@RequirePermission(menu, action)`과 `PermissionChecker`는 유지한다. `DenyAllPermissionChecker`와 `AllowAllPermissionChecker`는 8.1에서 제거한다.

실제 검사는 테넌트 스키마에서 합집합을 구한다.

- `user_role.use_yn='Y'`이고 `valid_from_date`/`valid_to_date`가 테넌트 오늘을 포함한다. 오늘 계산은 `TenantClock`이다.
- `role.use_yn='Y'`, `role_permission.use_yn='Y'`, `permission.use_yn='Y'`, `menu.use_yn='Y'`.
- `permission_code`가 `메뉴:기능`과 같으면 허용한다.
- 기존 `PermissionRules`대로 READ가 아닌 기능은 같은 메뉴의 READ도 있어야 한다.

캐시는 테넌트 스키마와 `user_pk`를 키로 하는 메모리 맵, TTL 60초다. 새 라이브러리는 쓰지 않는다. 사용자 역할 변경과 계정 상태 변경은 그 사용자의 권한 캐시와 계정 상태 캐시를 커밋 뒤 바로 지운다. 역할의 권한 변경은 그 역할을 가진 사용자를 바로 지운다. 권한 삭제는 그 스키마의 권한 캐시를 전부 지운다. 다음 요청은 60초를 기다리지 않는다. PMIS는 같은 테넌트 스키마를 읽어 같은 규칙으로 계산한다. MDM이 권한 목록을 토큰이나 별도 응답으로 밀어 주지 않는다.

모두 허용 대안:

| 안 | 내용 | 판단 |
| --- | --- | --- |
| 1 | `dev`에서도 프로비저닝 관리자로 로그인 | 추천. 운영과 같다 |
| 2 | `mdm.security.permit-all=true`일 때만 검사 생략. 기본 false. `dev`가 아닌데 true면 기동 거부 | 테스트 보조. 기본은 끄고, 문서와 로그에 경고 |

확정은 1이 기본이고, 2는 `dev`에서만 켤 수 있다. 테스트 프로파일에서 켜는 것은 허용하지 않는다.

## 공개 경로

| 경로 | 인증 | 의견 |
| --- | --- | --- |
| POST `/api/mdm/v1/auth/login`, `/auth/refresh` | 공개 | 필요 |
| POST `/api/platform/v1/auth/login`, `/auth/refresh` | 공개 | 필요 |
| GET `/api/mdm/v1/auth/jwks` | 공개 | PMIS 검증 |
| GET `/api/mdm/v1/context` | 공개 | 로그인 전 접속 주소 판정. 테넌트 필터를 타지 않는다 |
| GET `/api/mdm/v1/i18n/languages` | 공개 | 로그인 화면 언어 |
| GET `/api/mdm/v1/i18n/bundles`, `/bundles/{code}` | 허용 네임스페이스만 공개 | 기본 `common`, `error`, `auth`. 그 외는 인증만 필요. 여러 개를 한 번에 받으면 전부 허용 목록일 때만 공개 |
| GET `/api/mdm/v1/codes`, `/codes/{group}` | 인증 필요, 권한 불필요 | 로그인 전 업무 데이터로 보지 않음 |
| GET `/api/mdm/v1/nations` (`managed` 없음) | 인증 필요, 권한 불필요 | 같음 |
| GET `/api/mdm/v1/auth/me`, 비밀번호 변경, 로그아웃 | 인증 | |
| `@RequirePermission`이 있는 관리 API | 인증과 권한 | 국가 `managed=true` 포함 |
| `/actuator/health` | 공개 | 기존처럼 테넌트 필터 제외 |
| 그 외 `/api/mdm/**`, `/api/platform/**` | 인증 | |

파일 저장소, 파일, 첨부, 프로필 이미지, 팀 API도 이 표의 인증 행에 든다. 공개 경로에 추가하지 않았다.

로그인 화면은 언어와 허용된 번들만 토큰 없이 받는다. 공통코드와 국가 목록은 인증된 사용자만 본다. 관리 API는 권한까지 필요하다.

## CORS

허용 출처는 `mdm.security.cors.allowed-origins` 목록이다. `allowCredentials=true`다. `*`와 credentials는 함께 쓰지 않는다.

| 방식 | 내용 | 쿠키 |
| --- | --- | --- |
| 개발 서버 프록시 | 브라우저는 프론트 출처만 보고, 프론트 서버가 MDM으로 전달 | 같은 출처라 SameSite=Lax와 로컬 http가 된다. 추천 |
| 브라우저가 MDM을 직접 호출 | 출처가 달라 CORS와 쿠키가 필요 | SameSite=None과 Secure가 필요하다. 로컬 http와 맞지 않는다 |

프론트 구조는 여기서 정하지 않는다. 로컬은 프록시를 추천한다.

## 속도 제한

이번 구현에는 넣지 않는 쪽을 추천한다. 계정 잠금이 비밀번호 추측의 1차 제한이다. IP 제한을 메모리 맵으로 만들 수는 있다. 인스턴스마다 따로 세고, 재시작에 사라지며, IP가 프록시 뒤에 있으면 부정확하다. 라이브러리 없이 맞는 운영 한계가 아니다. 8.1 뒤로 미룬다.

## 감사 기록

| 사건 | 기록 |
| --- | --- |
| 테넌트 로그인 성공, 실패 | `user_login_history` |
| 플랫폼 로그인 성공, 실패 | `platform_admin_login_history` |
| 비밀번호 변경 | `user_password_history`. 플랫폼은 이력 테이블이 없어 8.1에서 만들지 않고 `password_changed_datetime`만 갱신 |
| Refresh 폐기, 재사용 | 해당 refresh 행의 `revoked_datetime`, `revoke_reason_code` |
| 잠금 | 계정 상태와 `locked_datetime`. 실패 이력은 `FAIL_LOCKED` |
| 잠금 해제 | 재발급 또는 자동 해제 뒤 다음 `SUCCESS` 이력 |

애플리케이션 로그에는 결과 코드, 테넌트 코드, `user_pk`만 남긴다. 비밀번호, Access, Refresh, 쿠키, Authorization 헤더는 남기지 않는다.

## V6 안

테넌트 V6는 8.1에서 적용한다. 플랫폼 가족 인덱스는 8.3의 공용 스키마 마이그레이션으로 미룬다.

```sql
DROP INDEX uk_company_business_registration_no;
CREATE UNIQUE INDEX uk_company_business_registration_no
    ON company (business_registration_no)
    WHERE business_registration_no IS NOT NULL AND use_yn = 'Y';

CREATE INDEX ix_user_refresh_token_token_family_id
    ON user_refresh_token (token_family_id);
CREATE INDEX ix_platform_admin_refresh_token_token_family_id
    ON platform_admin_refresh_token (token_family_id);
```

Refresh 테이블 컬럼은 회전과 가족 폐기에 충분하다. 컬럼 추가는 없다. `token_hash` 전체 유니크는 유지한다. 해시는 재사용하지 않는다.

## 구현 분할

| 단계 | 범위 |
| --- | --- |
| 8.1 | 보안 인프라. Resource Server, JWKS, 키 로딩, 실제 권한 검사, V6, 공개 경로 |
| 8.2 | 테넌트 로그인, refresh, logout, 비밀번호 변경, me, 정책 Seed, 비밀번호 변경 강제 필터 |
| 8.3 | 플랫폼 로그인, 부트스트랩, 테넌트 관리 API, 공용 V3 |
| 8.4 | `GET /menus/my` |

메뉴 트리는 로그인에 필요 없어 8.4다. 플랫폼 API 상세는 `doc/api/platform-api.md`다.

## 플랫폼 부트스트랩 운영

웹 서버를 띄우지 않는다. 실행 인자는 `--bootstrap-platform-admin`이다. 환경변수 `MDM_BOOTSTRAP_ADMIN_LOGIN`이 필수이고, `MDM_BOOTSTRAP_ADMIN_NAME`, `MDM_BOOTSTRAP_ADMIN_EMAIL`은 선택이다. 이름이 없으면 로그인 아이디를 쓰고, 이메일이 없으면 로그인 아이디를 email 컬럼에 넣는다. `platform_admin`이 0건일 때만 20자 임시 비밀번호를 만들어 표준 출력에 한 줄로 찍고 해시만 저장한다. 행이 있으면 바꾸지 않고 0이 아닌 종료 코드로 끝난다. 데이터베이스 이름은 `pmis`여야 한다. `must_change_password_yn='Y'`이고 `password_expire_datetime`은 지금에 `mdm.security.platform.temp-password-valid-days`(기본 3일)를 더한 값이다.

표준 출력을 애플리케이션 로그 파일로 돌리지 않는다. 출력된 비밀번호는 그 자리에서만 기록하고 바로 로그인해서 바꾼다. 로그 파일과 채팅에 평문을 남기지 않는다.

플랫폼 API는 테넌트 스키마가 없어 오류 문구 번들을 조회하지 않는다. 한글 기본 메시지와 `Accept-Language`가 `en`으로 시작하면 영문 메시지를 코드 상수로 고른다.

## 확정 사항

1. 비밀번호 변경 강제 중 refresh는 허용한다. 새 Access도 `pwd_change_required=true`다. 구현은 8.2다.
2. `X-Tenant-Code`는 Authorization이 없을 때만 쓴다. 토큰의 tenant와 다르면 `TENANT.HOST_MISMATCH`다. 8.1에서 식별 규칙에 반영한다.
3. `mdm.security.permit-all` 기본은 false다. `dev`에서만 켤 수 있다. 다른 프로필에서 true면 기동을 거부한다. 켜지면 기동 시 경고 로그를 남긴다. 개발의 기본은 실제 로그인이다.
4. 공개는 login, refresh, JWKS, `i18n/languages`, 허용 네임스페이스 번들뿐이다. `/codes`와 `/nations`는 인증만 필요하고 권한은 없다. 번들 허용 목록 기본값은 `common`, `error`, `auth`다. 한 번에 여러 네임스페이스를 요청하면 전부 허용 목록일 때만 공개다.
5. 개발 CORS는 프론트 개발 서버 프록시가 전제다. `mdm.security.cors.allowed-origins`와 credentials만 구현한다. 기본 빈 목록은 CORS 헤더를 주지 않는다. 운영은 프론트와 API가 같은 사이트(테넌트 Host 아래)다.
6. IP 속도 제한은 넣지 않는다. 운영 배포 전 리버스 프록시나 게이트웨이 제한을 본다.
7. 플랫폼 최초 계정은 부트스트랩 A다. 서버를 계속 띄우는 모드가 아니라 `--bootstrap-platform-admin`처럼 한 번 실행하고 끝나는 모드다. 평문이 서버 로그에 섞이지 않게 한다. 구현은 8.3이다.
8. 플랫폼 비밀번호 이력 테이블은 만들지 않는다. 변경 시각 컬럼만 갱신한다.
9. 정책 숫자 Seed는 위 표 그대로다. Seed 추가는 8.2다.

## 8.2 이후에 구현할 보완

- 계정이 없어도 더미 BCrypt 비교를 해 응답 시간으로 계정 존재가 드러나지 않게 한다.
- 잠금 시간이 지난 뒤 첫 실패는 `login_fail_count`를 1부터 다시 센다.
- 새 비밀번호는 현재 비밀번호와 같으면 안 된다.
- `X-Forwarded-For` 신뢰는 `mdm.security.trust-forwarded-headers` 기본 false일 때만 끈다. true일 때만 첫 값을 쓴다.
- 플랫폼 refresh 가족 인덱스와 `platform_admin.password_expire_datetime`은 공용 스키마 public V3로 8.3에서 만든다.

## 후속 과제

- 테넌트 일괄 업그레이드는 지금 요청 안에서 동기 실행한다. 테넌트가 많아지면 비동기로 바꾼다.
- 도메인 소유 검증은 하지 않는다. 등록 시 `verified_yn='Y'`로 둔다.
- 테넌트 `TERMINATED` 폐기 전환은 이번 범위가 아니다.

8.1에서 구현하는 계정 상태: Access가 살아 있어도 계정이 `ACTIVE`가 아니거나 `use_yn='N'`이면 `401 AUTH.ACCOUNT_DISABLED`다. 사용자 단위 메모리 캐시 TTL 60초 안이며, 비밀번호 재발급과 계정 상태 변경이 그 캐시를 지운다.

## 배포 전 점검

- 리버스 프록시 또는 게이트웨이에서 로그인, refresh에 대한 IP 속도 제한이 있는지 확인한다. 애플리케이션에는 넣지 않았다.
- `MDM_JWT_PRIVATE_KEY_PATH`가 `dev` 밖에서는 필수이고, 쿠키 Secure가 켜져 있는지 확인한다.
- CORS 허용 출처가 비어 있거나 실제 프론트 출처만 포함하는지 확인한다. 운영은 같은 사이트를 전제로 한다.
