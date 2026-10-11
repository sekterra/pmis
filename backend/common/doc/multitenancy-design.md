# 멀티테넌시 인프라 설계안

기준은 `20-flyway-multitenancy.mdc`이다. 아래 확정 사항에 따라 인프라를 구현한다. 프로비저닝 서비스와 테넌트 관리 API, JWT 발급은 이후 단계이다.

대상 버전은 현재 빌드의 Spring Boot 4.1.1, Hibernate 7.4, PostgreSQL이다. 테넌트 1개당 스키마 1개이다. 공용 스키마 이름은 `pmis`이고 테넌트 스키마는 `t_`로 시작한다. 기본 `public` 스키마는 쓰지 않는다.

## 확정 사항

1. 테넌트 식별은 Host와 검증된 JWT 클레임을 함께 쓴다. 로그인 요청은 토큰이 없으므로 Host로 식별한다. 로그인 성공 후 발급 토큰에 테넌트 클레임을 넣는다. 로그인 이후에는 JWT 클레임과 Host의 테넌트가 같아야 한다. 서버 간 호출은 JWT 클레임으로 식별한다. `X-Tenant-Code` 헤더 리졸버는 `dev` 프로필에서만 등록한다.
2. MDM이 JWT를 발급하고 PMIS 등은 JWKS로 검증한다. 발급과 클레임 이름, 라이브러리는 인증 설계 단계에서 정한다. 지금은 검증된 토큰에서 테넌트 코드를 읽는 인터페이스와 빈 스텁만 둔다.
3. 인프라 패키지는 `global.multitenancy`이다. `global.tenant`는 쓰지 않는다. 도메인 패키지 `tenant`는 유지한다. `global.multitenancy`는 `tenant`를 임포트하지 않는다.
4. SessionFactory는 하나이다. 공용 엔티티만 `@Table(schema = "pmis")`이다. 테넌트 엔티티는 스키마를 적지 않는다.
5. 일괄 업그레이드는 기동 시 실행하지 않는다.
6. 프로비저닝에 실패한 스키마는 자동으로 지우지 않는다. 사용자 승인 뒤에만 지운다.
7. PMIS 서버의 Flyway 이력 테이블 이름은 `flyway_schema_history_pmis`이다.
8. 테넌트 관리 API는 공용 스키마의 `platform_admin`으로 인증한다. 테이블은 공용 V2에 있다. 인증 구현은 인증 설계 단계이다.

## Hibernate SCHEMA 멀티테넌시

Hibernate 7은 테넌트마다 커넥션을 다르게 받는 `MultiTenantConnectionProvider`와, 지금 요청의 테넌트를 알려 주는 `CurrentTenantIdentifierResolver`로 SCHEMA 분리를 한다. Hibernate 6에서 빠졌던 `hibernate.multiTenancy=SCHEMA` 설정은 쓰지 않는다. 두 구현을 빈으로 등록하면 된다.

Spring Boot 4.1.1의 `HibernateJpaConfiguration`은 두 빈을 Hibernate에 넣지 않는다. `HibernatePropertiesCustomizer`로 `AvailableSettings` 상수를 등록한다. 문자열 키를 코드에 적지 않는다.

- `AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER` 값 `hibernate.multi_tenant_connection_provider`
- `AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER` 값 `hibernate.tenant_identifier_resolver`

식별자 리졸버의 실제 키는 `hibernate.multi_tenant_identifier_resolver`가 아니다.

`TenantSchemaMapper`와 `Connection.setSchema`는 쓰지 않는다. PostgreSQL에서는 `search_path`를 직접 바꾸는 편이 맞고, 매퍼는 커넥션을 풀에 되돌릴 때 초기화한다고 보장하지 않는다. `ddl-auto`는 `none`이다.

구성은 커넥션 풀 하나이다. 테넌트마다 DataSource를 만들지 않는다. 풀 크기가 테넌트 수와 같이 늘어나지 않는다.

| 방식 | 내용 | 판단 |
|---|---|---|
| 확정. SessionFactory 하나 | 테넌트 엔티티는 스키마를 적지 않는다. 공용 엔티티(`tenant` 등)만 `@Table(schema = "pmis")`이다. 업무 쿼리는 `search_path`의 테넌트 스키마를 본다. | 설정이 적고, 공용 테이블은 스키마가 고정된다. |
| SessionFactory 둘 | 공용은 멀티테넌시 없이 `pmis`만 보고, 테넌트 팩토리만 프로바이더를 쓴다. | 격리는 더 분명하다. 트랜잭션과 설정이 둘로 늘어난다. |

공용 엔티티와 테넌트 엔티티는 연관관계로 묶지 않는다. 이 규칙은 지금과 같다.

리졸버는 `ThreadLocal`만 읽는다. 비어 있으면 `pmis`로 떨어지지 않고 예외를 낸다. 공용 스키마를 써야 하는 요청은 필터가 컨텍스트에 `pmis`를 넣는다. 테넌트 업무 요청은 검증된 `t_*`만 넣는다. 식별에 실패하면 세션을 열기 전에 요청을 거절한다.

`validateExistingCurrentSessions`는 true로 둔다. 같은 스레드에 이미 열린 세션의 테넌트와 지금 값이 다르면 Hibernate가 막는다.

`getAnyConnection`과 `releaseAnyConnection`은 기동 시 메타데이터용이다. 이때 `search_path`는 `pmis`로 두고, 반납 때 초기화한다.

## TenantResolver

테넌트 식별은 이 인터페이스 뒤에만 둔다. 구현 이름을 업무 코드에 적지 않는다.

```text
TenantResolver
  Optional<String> resolveTenantCode(HttpServletRequest request)
```

반환값은 테넌트 코드이다. 스키마명이 아니다. 스키마명은 `tenant.schema_name`에서만 읽는다. 코드의 하이픈을 밑줄로 바꿔 스키마명을 즉석에서 만들지 않는다. `tenant_code`는 하이픈을 허용하고 `schema_name`은 `t_`와 영숫자, 밑줄만 허용하므로 생성 시점에 한 번 정해 `tenant` 행에 저장한다.

| 구현 | 읽는 값 | 맞는 경우 | 약한 점 |
|---|---|---|---|
| `HostTenantResolver` | Host 또는 신뢰된 `X-Forwarded-Host` | 테넌트마다 도메인이 있을 때 | 내부 호출은 도메인 매핑이 필요하다. 전달된 Host는 프록시를 신뢰할 때만 쓴다. |
| `JwtTenantResolver` | 검증된 JWT의 테넌트 클레임 | API와 서버 간 호출 | 클레임 이름과 서명 검증이 먼저 필요하다. 서명 전 클레임은 쓰지 않는다. |

둘은 `TenantResolver` 목록으로 등록하고, 설정으로 순서만 고른다. 첫 번째가 코드를 반환하면 그것을 쓴다. 코드가 정해지면 `TenantDirectory`가 `pmis.tenant`와 `tenant_domain`을 조회한다. 없거나 `SUSPENDED`, `TERMINATED`이면 요청을 거절한다. `ACTIVE`만 업무 요청을 받는다. `PROVISIONING`과 `FAILED`는 프로비저닝 재시도 경로만 허용한다.

조회 결과는 테넌트 코드와 도메인 기준으로 짧게 캐시한다. 상태 변경 시 그 항목을 지운다.

## search_path와 풀 오염

커넥션을 빌릴 때와 돌려줄 때 모두 처리한다. 돌려줄 때만 초기화하면, 초기화 전에 예외가 난 커넥션이 다음 테넌트로 넘어간다.

1. 스키마명을 검증한다. 검증 실패 시 `SET`을 실행하지 않는다.
2. `getConnection`에서 풀의 커넥션을 얻은 뒤 `SET search_path TO`에 검증된 이름만 넣는다. 값은 바인딩하지 못하므로, 정규식을 통과한 이름만 붙인다.
3. `releaseConnection`의 `finally`에서 `RESET search_path`를 실행한 다음 풀에 돌려준다. 초기화에 실패하면 커넥션을 풀에 넣지 않고 닫는다.
4. `supportsAggressiveRelease`는 false이다. 트랜잭션 중간에 커넥션을 풀에 되돌리지 않는다.
5. Hikari `connectionInitSql`로 테넌트 스키마를 넣지 않는다. 초기 SQL은 테넌트를 모른다.

요청이 끝나면 필터의 `finally`에서 `ThreadLocal`을 지운다. `@Async`는 호출자 스레드의 값을 복사하지 않는다. 비동기와 배치는 테넌트 코드를 인자로 받고, 작업 시작 때 컨텍스트를 넣은 뒤 끝에서 지운다.

## 스키마명 검증

테넌트 스키마는 모두 같은 검사를 통과해야 한다. `CREATE SCHEMA`, Flyway `schemas`, `search_path`가 같은 검사를 쓴다.

- 정규식 `^t_[a-z0-9_]{1,61}$`
- 예약 이름 거부: `public`, `pmis`, `information_schema`, `pg_`로 시작하는 이름
- 업무 요청은 `tenant.schema_name`에 있고 상태가 허용된 행만 사용
- 사용자 입력을 그대로 SQL에 넣지 않음

테스트용 `it_<영숫자 16자>`는 테넌트 스키마가 아니다. 앱 리졸버와 프로바이더는 거부한다. 마이그레이션 테스트만 JDBC로 그 스키마를 만든다. 프로비저닝 통합 테스트의 스키마는 `t_it_<랜덤>`이며, 테스트가 `tenant` 행을 만든 뒤에만 프로바이더를 탄다.

`tenant_code` 검사 `^[a-z0-9]+(-[a-z0-9]+)*$`는 이미 공용 V1에 있다. 스키마명 생성 규칙은 결정 1과 별도로, 저장값을 조회하는 쪽으로 고정하는 것을 추천한다.

## 접속 DB 이름

Flyway와 프로비저닝 앞에 `SELECT current_database()`를 실행한다. 기대값은 설정 `pmis`이다. 다르면 예외를 내고 스키마 생성, Flyway, 프로비저닝을 하지 않는다.

기동 시에도 한 번 검사한다. DataSource가 없는 단위 테스트는 이 검사를 타지 않는다. 통합 테스트와 앱 기동은 틀린 DB면 바로 실패한다. `postgres` 데이터베이스에 스키마가 만들어졌던 경우를 막기 위한 검사이다.

## 패키지와 의존 방향

이름이 겹친다. `tenant`는 공용 스키마의 테넌트 관리 도메인이다. `global.tenant`는 해석과 커넥션이다. 규칙 문서의 패키지 예시는 `global.tenant`이지만, 구현 시에는 인프라 패키지를 `global.multitenancy`로 바꾸는 쪽을 추천한다. 도메인 패키지 `tenant`는 유지한다.

| 패키지 | 역할 | 의존 |
|---|---|---|
| `global.multitenancy` | 컨텍스트, 스키마명 검사, 리졸버 인터페이스, 커넥션 프로바이더, DB 이름 검사 | `tenant` 도메인을 임포트하지 않는다 |
| `tenant` | 테넌트, 도메인, 프로비저닝, 업그레이드 | `global.multitenancy`의 검사와 컨텍스트를 사용한다 |
| `TenantDirectory` | 코드와 도메인으로 스키마명과 상태를 조회하는 인터페이스 | 인터페이스는 인프라 쪽, 구현은 `tenant` |

인프라가 도메인 엔티티를 보면 순환이 생긴다. 조회는 인터페이스로 넘긴다. 도메인 서비스가 프로바이더 내부를 호출하지는 않는다.

이 이름을 승인하면 `00-project-overview.mdc`와 `20-flyway-multitenancy.mdc`의 패키지 문구도 그때 맞춘다. 이번 단계에서는 규칙을 고치지 않는다.

## 프로비저닝

순서는 아래와 같다. 각 단계의 결과는 `tenant_provisioning`에 남긴다. 상태 이력은 `tenant_status_history`에 남긴다.

| 단계 | 동작 | 다시 실행할 때 |
|---|---|---|
| REQUESTED | 코드와 스키마명을 검사하고 `tenant`를 `PROVISIONING`으로 넣는다. | 같은 코드가 `FAILED` 또는 `PROVISIONING`이면 그 행을 이어서 쓴다. `ACTIVE`면 새로 만들지 않는다. |
| SCHEMA_CREATED | `CREATE SCHEMA` | 스키마가 없고 이 단계 이전이면 만든다. 이미 있고 기록이 이 단계 이후이면 지나간다. 스키마가 있는데 우리 기록이 없으면 중단한다. `CREATE SCHEMA IF NOT EXISTS`로 남의 스키마에 붙지 않는다. |
| MIGRATED | 테넌트 Flyway | `migrate`는 적용된 버전을 다시 적용하지 않는다. |
| SEEDED | 지원언어, 국가, 공통코드, 기본 역할, 메뉴, 권한 | `src/main/resources/db/seed/tenant/`의 SQL을 파일 이름 순서로, 그 테넌트 스키마에만 실행한다. 멱등은 `INSERT ... WHERE NOT EXISTS`로 통일한다. 이미 있는 행은 수정하지 않는다. |
| ADMIN_CREATED | 최초 관리자 | 임시 비밀번호를 `SecureRandom`으로 만든다. 길이는 16이고 대문자, 소문자, 숫자, 기호가 각각 한 자 이상이며 `0/O`, `1/l/I`는 빼 둔다. DB에는 BCrypt 해시만 저장하고 `must_change_password_yn='Y'`로 둔다. 유효 일수는 공통코드 `SECURITY_POLICY` / `TEMP_PASSWORD_VALID_DAYS`의 `ext_attr1`을 그 테넌트 스키마에서 읽는다. 값이 없거나 양의 정수가 아니면 `TENANT.SECURITY_POLICY_MISSING`으로 `FAILED`가 되고 자사, 사람, 계정을 남기지 않는다. 평문은 프로비저닝 결과 객체로 1회만 돌려주고 `toString`에서는 가린다. 재조회하지 않는다. 메일 발송은 나중 기능이다. 같은 `login_id`가 있으면 새로 만들지 않고 임시 비밀번호도 만들지 않으며 결과에 이미 있음을 표시한다. |
| COMPLETED | 상태를 `ACTIVE`로 바꾼다. | `activated_datetime`, 상태 이력, `execution_status_code=SUCCESS`, `current_step_code=COMPLETED`, `completed_datetime`, `schema_version`, 디렉터리 캐시 무효화를 한다. 이미 `ACTIVE`이면 아무 것도 하지 않는다. |

구현된 단계는 `REQUESTED`, `SCHEMA_CREATED`, `MIGRATED`, `SEEDED`, `ADMIN_CREATED`, `COMPLETED`이다. 구현체가 없는 단계에 도달하면 테넌트는 `PROVISIONING`, `execution_status_code`는 `RUNNING`, `current_step_code`는 마지막 성공 단계로 두고 정상 종료한다. 전부 끝나면 테넌트는 `ACTIVE`, 현재 단계는 `COMPLETED`, 실행은 `SUCCESS`이다. Seed 실패 시 상태는 `FAILED`, `current_step_code`는 `MIGRATED`라서 재시도는 `SEEDED`부터 한다. 최초 관리자 생성 실패 시 `current_step_code`는 `SEEDED`라서 재시도는 `ADMIN_CREATED`부터 한다.

임시 비밀번호를 다시 발급할 때는 `TenantAdminPasswordService.reissue(tenantCode, executorPk)`를 쓴다. 대상은 `ACTIVE` 테넌트의 최초 관리자(`login_id`가 `tenant_provisioning.admin_email`의 소문자이고 `TENANT_ADMIN`인 계정)이다. 비밀번호를 바꾸고 `must_change_password_yn='Y'`, 만료를 정책 일수로 다시 계산하고, 상태를 `ACTIVE`로 두며 `login_fail_count`를 0으로, `locked_datetime`을 비운다. `user_password_history`에는 `RESET`을 남긴다. 평문은 결과 객체로 1회만 돌려준다. 컨트롤러는 운영자 인증 설계 뒤에 만든다.

실패하면 상태를 `FAILED`로 두고 `error_message`, `current_step_code`, `retry_count`를 남긴다. `error_message`에는 접속 정보와 비밀번호를 넣지 않는다. 만든 스키마는 지우지 않는다. 재시도는 `current_step_code` 다음부터 한다. 단계 기록과 실패 기록은 `REQUIRES_NEW` 트랜잭션으로 커밋한다.

스키마 삭제는 자동으로 하지 않는다. 사용자가 승인한 뒤에, 상태가 `FAILED` 또는 `TERMINATED`이고, 이름이 그 행의 `schema_name`과 같고, 테넌트 스키마 검사를 통과할 때만 `DROP SCHEMA`를 실행한다. `pmis`, `public`, `it_` 스키마는 이 경로로 지우지 않는다.

## 일괄 업그레이드

앱 기동 때는 하지 않는 것을 추천한다. 배포 직후 모든 테넌트에 마이그레이션이 돌면, 실패가 기동 실패로 번지고 공유 개발 DB도 예고 없이 바뀐다.

별도 관리 실행이 `ACTIVE` 테넌트만 순서대로 처리한다.

1. DB 이름이 `pmis`인지 확인한다.
2. 테넌트마다 `tenant_schema_migration`에 `RUNNING`을 넣는다.
3. 그 스키마에만 테넌트 Flyway `migrate`를 실행한 뒤, 같은 Seed SQL을 다시 실행해 없는 행만 보강한다. `SECURITY_POLICY`와 `ATTACHMENT_CATEGORY` 값도 이 경로로 들어간다. 이미 있는 값은 바꾸지 않는다.
4. Flyway 또는 Seed가 실패하면 그 테넌트의 `tenant_schema_migration`을 `FAILED`로 남기고 다음 테넌트로 간다. 둘 다 성공이면 `SUCCESS`이다.
5. `SUSPENDED`, `TERMINATED`, `PROVISIONING`, `FAILED` 테넌트는 건너뛴다. 업그레이드가 실패한 `ACTIVE` 테넌트는 다음 일괄 실행에서 다시 시도한다. 같은 실행 안에서 한 테넌트를 두 번 시도하지 않는다. 기동 시에는 실행하지 않는다.

## Flyway 이력 테이블

운영에서는 공용 스크립트와 테넌트 스크립트가 같은 스키마에 들어가지 않는다. 그래서 버전 번호가 둘 다 V1이어도 이력은 충돌하지 않는다. 5.6단계 `AllMigrationTest`는 시험을 위해 한 스키마에 둘을 넣었고, 그때만 이력 테이블을 나눴다. 그 방식을 운영에 복사하지 않는다.

| 위치 | 스크립트 | 이력 테이블 | 언제 실행하나 |
|---|---|---|---|
| 스키마 `pmis` | `db/migration/public` | `flyway_schema_history` | `MDM_FLYWAY_ENABLED=true`일 때만 기동 시 |
| 각 테넌트 스키마 | `db/migration/tenant` | `flyway_schema_history` | 프로비저닝과 일괄 업그레이드 |
| 각 테넌트 스키마, 이후 PMIS 서버 | PMIS 저장소의 스크립트 | `flyway_schema_history_pmis` | PMIS 서버가 자신의 실행에서 |

MDM은 PMIS 이력 테이블을 읽거나 쓰지 않는다. PMIS는 MDM 이력 테이블을 읽거나 쓰지 않는다. `clean`은 끄고, `create-schemas`는 쓰지 않는다. 스키마 `pmis` 생성은 `doc/db` 스크립트이다.

기동 기본값은 지금과 같다. `MDM_FLYWAY_ENABLED`의 기본은 false이다. true여도 공용 스키마만 마이그레이션한다. 테넌트 스키마는 기동만으로 바뀌지 않는다.

## 서버 간 호출

PMIS는 MDM을 REST로 호출한다. 라이브러리로 묶지 않는다. 같은 데이터베이스의 테넌트 스키마를 PMIS가 직접 쓸 수도 있다. 그 경우에도 스키마명 검사와 자신의 Flyway 이력 테이블 규칙을 따른다.

| 식별 | 동작 | 판단 |
|---|---|---|
| Host | PMIS가 테넌트 도메인을 Host로 보낸다. | 브라우저 요청과 같다. 내부 주소만 있으면 도메인 행이 필요하다. |
| JWT 클레임 | 서명이 검증된 토큰의 테넌트 클레임을 읽는다. | 서버 간 호출에 맞다. 클레임만 있고 서명이 없으면 거절한다. |
| `X-Tenant-Code` 헤더만 | 호출자가 코드를 적는다. | 위조가 쉽다. 단독 수단으로 쓰지 않는다. |

| 인증 | 동작 | 판단 |
|---|---|---|
| 공유 JWT | 한 발급자가 서명하고 MDM과 PMIS가 같은 JWKS로 검증한다. PMIS가 사용자 토큰을 전달한다. | 사용자 주체가 유지된다. `aud`에 MDM이 있어야 한다. |
| 서비스 토큰 | PMIS가 클라이언트 자격으로 토큰을 받고 테넌트와 사용자를 따로 보낸다. | 경계는 분명하다. 사용자 위장을 막으려면 서비스 인증이 강해야 한다. |
| 토큰 교환 | PMIS가 사용자 토큰을 MDM용 토큰으로 바꾼다. | `aud`가 분리된다. 발급 서버가 더 필요하다. |

확정된 식별은 위 1번이다. 헤더만으로 테넌트를 정하지 않는다. `X-Tenant-Code`는 `dev` 프로필 전용이다. JWT 라이브러리와 클레임 이름은 인증 설계 단계에서 정한다.

## 로그인

로그인 요청에는 토큰이 없다. 필터는 Host로 `tenant_domain`을 찾아 테넌트 코드와 스키마를 정한 뒤 그 스키마에서 사용자를 조회한다. 로그인 성공 시 발급하는 JWT에 그 테넌트 클레임을 넣는다. 발급 구현은 인증 설계 단계이다.

로그인 이후 요청은 Host의 테넌트와 JWT 클레임의 테넌트가 같아야 한다. 하나만 있으면 그 값을 쓴다. 서버 간 호출은 테넌트 도메인이 아닌 주소로 올 수 있으므로 JWT만 있어도 된다. 둘 다 있는데 다르면 세션을 열기 전에 거절한다.

## 동시 실행

같은 테넌트의 프로비저닝과 업그레이드가 겹치지 않게 한다. 애플리케이션 잠금만으로는 인스턴스가 여러 개일 때 막지 못한다.

잠금은 `pg_try_advisory_lock`이다. 대기하는 `pg_advisory_lock`은 쓰지 않는다. 락은 전용 커넥션에서 잡고 `finally`에서 같은 커넥션으로 푼다.

락 키는 PostgreSQL `hashtext` 두 개다.

| 대상 | 호출 |
|---|---|
| 테넌트 프로비저닝·업그레이드 | `pg_try_advisory_lock(hashtext('pmis.tenant'), hashtext(schema_name))` |
| 일괄 업그레이드 전체 | `pg_try_advisory_lock(hashtext('pmis.tenant.upgrade-all'), 0)` |

`schema_name`은 검증을 통과한 값만 바인딩한다. 테넌트 락을 못 잡으면 대기하지 않고 `TENANT.PROVISIONING_IN_PROGRESS`를 바로 던진다. 일괄 실행 락을 못 잡으면 `TENANT.UPGRADE_IN_PROGRESS`를 바로 던진다. 일괄 실행 안에서 한 테넌트가 실패해도 다음 테넌트는 계속한다.

## 기동 시 DDL 검증

`ddl-auto`는 `none`이다. SessionFactory 기동은 공용 스키마 기준이라, 테넌트 테이블까지 `validate`하면 테넌트 스키마가 `search_path`에 없을 때 실패한다. 기동 시 validate는 하지 않는다.

엔티티와 DDL이 같은지는 테넌트 스키마에 마이그레이션을 적용한 통합 테스트에서 `hibernate.hbm2ddl.auto=validate`로 확인한다. 컬럼 이름은 기본 `CamelCaseToUnderscoresNamingStrategy`를 따른다. 공용 엔티티만 스키마 `pmis`를 적는다.

## 공유 개발 DB와 테스트

개발 DB는 원격 PostgreSQL이고 여러 사람이 쓴다. Docker와 Testcontainers는 쓰지 않는다.

- 계정에 `CREATE SCHEMA`가 필요하다. 데이터베이스 생성 권한은 앱에 넣지 않는다.
- 앱과 테스트는 `current_database()`가 `pmis`가 아니면 스키마를 만들지 않는다.
- 마이그레이션 통합 테스트는 `it_<영숫자 16자>`만 만들고 그 스키마만 지운다.
- 프로비저닝과 격리 통합 테스트는 `t_it_<랜덤>`만 만들고, 이름 검사 뒤에 그 스키마만 지운다.
- `pmis`와 다른 테넌트 스키마는 지우지 않는다.
- 일괄 업그레이드와 `MDM_FLYWAY_ENABLED=true` 기동은 대상 DB 이름을 확인한 뒤에만 실행한다.
- 접속 정보는 환경변수로만 받고 로그와 문서에 남기지 않는다.

테스트는 둘로 나눈다.

| 종류 | 확인 |
|---|---|
| 단위. DB 없음 | 스키마명 거부, 예약어 거부, 리졸버가 코드만 반환, 컨텍스트가 요청 끝에 비는지, 프로비저닝이 끝난 단계를 건너뛰는지, DB 이름 불일치 시 예외 |
| 통합. 원격 `pmis` | 테넌트 둘을 만들면 스키마와 테이블이 같고 데이터가 섞이지 않는지, 커넥션을 돌려준 뒤 다음 대여의 `search_path`가 이전 테넌트가 아닌지, 없는 테넌트와 `SUSPENDED`가 거절되는지, 공용 Flyway와 테넌트 Flyway가 서로의 이력 테이블을 쓰지 않는지 |

단위 테스트는 `gradlew test`로 DB 없이 통과해야 한다. 통합 테스트는 `integration` 태그와 `integrationTest`로 두고, 실행 전에 대상 DB를 확인한다.
