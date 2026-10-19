## [1단계] 개발 환경 구성 - 2026-10-09
- 변경/추가 파일:
  - 수정: `src/main/resources/application.yaml`, `src/main/resources/application-local.yaml`, `.gitignore`, `build.gradle`, `src/test/java/com/tommy/common/CommonApplicationTests.java`, `src/test/java/com/tommy/common/TestcontainersConfiguration.java`, `src/test/java/com/tommy/common/TestCommonApplication.java`
  - 추가: `config/application-local.yaml.example`, `doc/cursor-progress.md`
  - 유지: `src/main/java/com/tommy/common/global/config/JpaAuditingConfig.java`, `src/main/java/com/tommy/common/global/audit/BaseEntity.java`, 패키지 `package-info.java`, `src/main/resources/db/migration/public/.gitkeep`, `src/main/resources/db/migration/tenant/.gitkeep`
  - 삭제: `docker-compose.yml`, `.env`, `.env.example` (Docker 사용 불가, 접속 기본값을 커밋하지 않음)
- 검증 결과: `gradlew.bat build` 성공 (Docker/DB 접속 없음). `test`는 `integration` 태그를 제외한다. 단위 테스트 클래스는 아직 없다. `integrationTest`는 실행하지 않았다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요:
  - 접속 정보 방식과 Flyway 자동 실행의 추천안을 아래에 구현해 두었다. 다른 안을 원하면 알려 달라.
  - 기존 `CommonApplicationTests`는 아직 Testcontainers(Docker) 기반이다. 기본 `build`에서는 실행되지 않는다. `integrationTest`를 실행하면 Docker가 필요해 이 환경에서는 실패한다.
- 가정한 사항:
  - 기본 프로필은 `local` (`SPRING_PROFILES_ACTIVE`로 변경 가능).
  - 접속은 환경변수 `MDM_DB_URL`, `MDM_DB_USERNAME`, `MDM_DB_PASSWORD`를 우선 사용하고, 없으면 `config/application-local.yaml`(gitignore, optional import)의 같은 키를 사용한다.
  - Flyway는 `MDM_FLYWAY_ENABLED=true`일 때만 실행한다. 기본값은 끄기.
  - `spring.flyway.create-schemas=false`. 스키마 `pmis` 생성은 1.5단계 스크립트에서 한다.
  - `create_user_pk`, `update_user_pk`는 감사자가 null을 반환하므로 null을 허용한다.

## [1.5단계] 원격 DB 점검과 pmis 스키마 생성 - 2026-10-09
- 변경/추가 파일: `doc/db/00_inspect.sql`, `doc/cursor-progress.md`
- 검증 결과: 점검 SQL은 작성만 했다. 실행하지 않았다. `MDM_DB_URL` 환경변수와 `config/application-local.yaml`이 없어 대상 DB 이름을 확인할 수 없다. `psql`도 없다. `application.yaml`의 `spring.flyway.schemas=pmis`, `default-schema=pmis`, `create-schemas=false`는 이미 반영되어 있다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요:
  - 읽기 전용 점검 대상 DB 이름과 실행 승인. 접속 정보(주소, 계정, 비밀번호)는 채팅에 남기지 않는다.
  - 승인 전에는 collation 방안 확정, `doc/db/01_create_schema_pmis.sql` 작성, DB 반영을 하지 않는다.
- 가정한 사항: 없음

## [1.5단계] 전용 DB(pmis)와 공용 스키마(pmis) 생성 스크립트 - 2026-10-09
- 변경/추가 파일:
  - 추가: `doc/db/01_create_database_pmis.sql`, `doc/db/02_create_schema_pmis.sql`, `doc/db/03_verify_collation.sql`, `doc/db/README.md`
  - 수정: `config/application-local.yaml.example`, `doc/cursor-progress.md`
- 검증 결과: DB에 접속하지 않았다. 스크립트는 지시서의 PostgreSQL 16 문법을 따른다. `CREATE DATABASE`는 `TEMPLATE template0`, `ENCODING 'UTF8'`, `LOCALE_PROVIDER icu`, `ICU_LOCALE 'ko-KR'`, `LC_COLLATE 'en_US.utf8'`, `LC_CTYPE 'en_US.utf8'`이다. 검증 쿼리의 기대 정렬은 `1, 2, ㄱ, 가, ㄴ, 나, 다, 하, a, A, B`이다. `application.yaml`의 Flyway 스키마는 `pmis`, `create-schemas`는 `false`다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요:
  - `postgres`에 접속해 `01`을 실행한 뒤 `pmis` 데이터베이스가 생겼는지
  - `pmis`에 접속해 `02`를 실행한 뒤 스키마 `pmis`가 있는지
  - `03`에서 `datlocprovider = i`, `daticulocale = ko-KR`, `current_database() = pmis`인지
  - `03` 정렬 결과가 `1, 2, ㄱ, 가, ㄴ, 나, 다, 하, a, A, B`인지
- 가정한 사항: 없음

## [1.6단계] 초기화 스크립트 보강 (잘못된 DB에서 실행 방지) - 2026-10-09
- 변경/추가 파일: `doc/db/01_create_database_pmis.sql`, `doc/db/02_create_schema_pmis.sql`, `doc/db/03_verify_collation.sql`, `doc/db/README.md`, `doc/cursor-progress.md`
- 검증 결과: DB에 접속하지 않았다. `02`는 접속 DB가 `pmis`가 아니면 예외를 내는 DO 블록 한 문장이다. `03`은 앞에 접속 DB 확인 조회가 있다. `01` 맨 위 주석은 `postgres` 접속 전용임을 표시한다. README에 DBeaver 데이터베이스 전환, `SELECT current_database()` 확인, 잘못된 스키마 정리(`DROP SCHEMA pmis`, `CASCADE` 없음)를 적었다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요:
  - `pmis`에 접속해 `02`를 실행했을 때 스키마 `pmis`가 있는지
  - `postgres`에 접속한 채 `02`를 실행하면 오류가 나고 스키마가 생기지 않는지
  - `03` 첫 결과의 `connection_check`가 `OK`인지
  - `postgres`에 잘못 만든 스키마 `pmis`를 비어 있음 확인 후 `DROP SCHEMA pmis`로 정리했는지
- 가정한 사항: 없음

## [2단계] 공용 스키마 Flyway V1 (테넌트 관리) - 2026-10-10
- 변경/추가 파일:
  - 추가: `src/main/resources/db/migration/public/V1__create_tenant_tables.sql`, `src/test/java/com/tommy/common/PublicTenantMigrationTest.java`
  - 수정: `src/test/java/com/tommy/common/CommonApplicationTests.java`, `src/test/java/com/tommy/common/TestcontainersConfiguration.java`, `src/test/java/com/tommy/common/TestCommonApplication.java`, `doc/cursor-progress.md`
- 검증 결과: `gradlew.bat test` 성공. `integration` 태그는 제외되어 원격 DB에 접속하지 않았다. `integrationTest`는 실행하지 않았다.
- ERD 반영 내역: 기준 파일 `doc/erd/erd-model-tenant-20261009.json`

| 항목 | ERD | DDL |
|---|---|---|
| tenant.tenant_code | UK, 영문 소문자/숫자/하이픈 | `uk_tenant_tenant_code`, `ck_tenant_tenant_code` `^[a-z0-9-]+$` |
| tenant.schema_name | UK, 길이 63 | `uk_tenant_schema_name`, `VARCHAR(63)`. 형식 CHECK는 두지 않음 |
| tenant.tenant_status_code | PROVISIONING/ACTIVE/SUSPENDED/TERMINATED/FAILED | CHECK |
| tenant.use_yn | Y/N, 기본값 Y | `NOT NULL DEFAULT 'Y'` CHECK |
| tenant.create_datetime | 기본값 현재일시, nullable | `DEFAULT CURRENT_TIMESTAMP`, NULL 허용 |
| tenant.default_language_code | 테넌트 스키마 FK 없음 | FK 없음 |
| tenant_domain.domain_name | UK | `uk_tenant_domain_domain_name` |
| tenant_domain.domain_type_code | SUBDOMAIN/CUSTOM | CHECK. DEFAULT는 두지 않음 |
| tenant_domain.primary_yn | 테넌트당 1건만 Y, 기본값 N | `DEFAULT 'N'` CHECK, 부분 유니크 인덱스 `uk_tenant_domain_primary_yn` |
| tenant_domain.verified_yn | 기본값 N, Y/N | `DEFAULT 'N'` CHECK |
| tenant_domain.use_yn | Y/N, 기본값 Y | `NOT NULL DEFAULT 'Y'` CHECK |
| tenant_domain.tenant_pk | FK, NOT NULL | `fk_tenant_domain_tenant`, `ix_tenant_domain_tenant_pk` |
| tenant_status_history | Append-only, 감사 4컬럼 없음 | ERD 컬럼만. `from_status_code` NULL |
| tenant_status_history.change_user_pk | FK 없음 | FK 없음 |
| tenant_provisioning.current_step_code | REQUESTED/SCHEMA_CREATED/MIGRATED/SEEDED/ADMIN_CREATED/COMPLETED | CHECK |
| tenant_provisioning.execution_status_code | RUNNING/SUCCESS/FAILED | CHECK |
| tenant_provisioning.retry_count | 기본값 0 | `NOT NULL DEFAULT 0` |
| tenant_provisioning.error_message | CLOB | TEXT |
| tenant_schema_migration.migration_type_code | PROVISION/UPGRADE/REPAIR | CHECK |
| tenant_schema_migration.execution_status_code | RUNNING/SUCCESS/FAILED | CHECK |
| tenant_schema_migration.error_message | CLOB | TEXT |
| tenant_schema_migration.executed_user_pk | FK 없음 | FK 없음 |
| PK | BIGINT + 시퀀스 | `<테이블>_seq INCREMENT BY 1`, `DEFAULT nextval` |

- 질문/확인 필요:
  - `domain_type_code`의 "기본 제공"을 컬럼 DEFAULT `'SUBDOMAIN'`으로 둘지. 지금은 CHECK만 둠
  - `primary_yn='Y'` 1건 제한에 `use_yn='N'` 행을 제외할지. 지금은 `use_yn`과 무관함
  - `from_status_code`, `to_status_code`에 `tenant_status_code`와 같은 허용값 CHECK를 둘지. 지금은 두지 않음
  - `schema_name`에 `^t_[a-z0-9_]{1,61}$` CHECK를 둘지. 지금은 두지 않음
  - `tenant_code` 패턴 `^[a-z0-9-]+$`에서 앞뒤 하이픈, 연속 하이픈을 허용할지
  - `tenant_code`, `schema_name`의 생성 후 변경 불가를 DB 트리거로 강제할지. 지금은 두지 않음
  - 통합 테스트 실행 승인. 대상 DB 확인 후 `MDM_TEST_DB_URL`, `MDM_TEST_DB_USERNAME`, `MDM_TEST_DB_PASSWORD`를 설정하고 `gradlew.bat integrationTest`를 실행. 테스트는 `it_<랜덤>` 스키마만 만들고 삭제하며 스키마 `pmis`는 변경하지 않음
- 가정한 사항:
  - ERD 비고의 공용 스키마 표기 `public` 대신, SQL은 스키마명을 쓰지 않고 Flyway `default-schema=pmis`에 따른다
  - `create_user_pk`, `update_user_pk`, `change_user_pk`, `executed_user_pk`에는 FK를 두지 않는다
  - 컨텍스트 테스트는 컨텍스트 로딩 전에 skip 하려고 `@EnabledIfEnvironmentVariable`을 사용한다. 마이그레이션 테스트는 `Assumptions`로 skip 한다
  - 컨텍스트 테스트는 `spring.flyway.enabled=false`로 두어 공유 스키마를 변경하지 않는다

## [3단계] 테넌트 스키마 Flyway V1 (다국어, 공통코드) - 2026-10-10
- 변경/추가 파일:
  - 추가: `src/main/resources/db/migration/tenant/V1__create_i18n_and_code_tables.sql`, `src/test/java/com/tommy/common/TenantI18nCodeMigrationTest.java`
  - 수정: `doc/cursor-progress.md`
- 검증 결과: `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았고 `integrationTest`는 실행하지 않았다.
- ERD 반영 내역: 기준 파일 `doc/erd/erd-model-i18n-common-code-20261009.json`. 테이블 12개. Seed 없음. SQL에 스키마명 없음.

| 항목 | ERD | DDL |
|---|---|---|
| support_language | VARCHAR PK, 시퀀스 없음 | `pk_support_language (support_language_code)` |
| support_language.fallback_language_code | 자기참조, NULL 허용 | `fk_support_language_support_language` |
| support_language.text_direction_code | LTR/RTL, 기본값 LTR | `DEFAULT 'LTR'` CHECK |
| support_language.default_yn | 1건만 Y, 기본값 N | `DEFAULT 'N'` CHECK, 부분 유니크 `uk_support_language_default_yn` |
| nation.nation_code | UK | `uk_nation_nation_code` |
| nation.default_language_code | FK, NOT NULL | `fk_nation_support_language` |
| i18n_namespace.namespace_code | UK | `uk_i18n_namespace_namespace_code` |
| i18n_message | UK(namespace_pk, message_key) | `uk_i18n_message_namespace_pk_message_key` |
| i18n_message.message_type_code | 공통코드 I18N_MESSAGE_TYPE | 문자열 저장. FK/CHECK 없음 |
| i18n_message_text | UK(message_pk, support_language_code) | 복합 UK. `message_text`는 TEXT |
| i18n_message_text.translation_status_code | 공통코드 I18N_TRANSLATION_STATUS | 문자열 저장. FK/CHECK 없음 |
| i18n_message_text.machine_translated_yn | 기본값 N | `DEFAULT 'N'` CHECK Y/N |
| i18n_entity_text | UK(대상 테이블, 컬럼, 키, 언어) | `uk_i18n_entity_text_target`. 대상 테이블 FK 없음 |
| code_group.code_group_code | UK | `uk_code_group_code_group_code` |
| code_group.hierarchy_yn | 기본값 N | `DEFAULT 'N'` CHECK Y/N |
| code_group_lang | UK(code_group_pk, support_language_code) | 복합 UK. `use_yn` 없음 |
| common_code | UK(code_group_pk, code_value) | 복합 UK |
| common_code.parent_code_pk | 자기참조, NULL 허용 | `fk_common_code_common_code` |
| common_code_lang | UK(code_pk, support_language_code) | 복합 UK. `use_yn` 없음 |
| common_code_external_map | UK(code_pk, external_system_code) | 복합 UK |
| common_code_history.code_pk | FK 미설정 | FK 없음. 조회용 `ix_common_code_history_code_pk` |
| common_code_history.change_type_code | C/U/D | CHECK |
| CLOB | error_message 등 | TEXT |
| use_yn, create_datetime | Y 기본, 현재일시 기본, nullable | `DEFAULT 'Y'`, `DEFAULT CURRENT_TIMESTAMP` |
| PK | BIGINT + 시퀀스. support_language 제외 | `<테이블>_seq INCREMENT BY 1` |

- 질문/확인 필요:
  - `default_yn='Y'` 1건 제한에 `use_yn='N'` 행을 제외할지. 지금은 `use_yn`과 무관함
  - `message_type_code`, `translation_status_code`에 허용값 CHECK를 둘지. 공통코드 컬럼이라 지금은 두지 않음
  - `code_group_code`의 대문자 SNAKE 형식을 CHECK로 둘지. 지금은 UK만 둠
  - `nation_code`를 `^[A-Z]{2}$`로 제한할지. 지금은 UK와 길이 2만 둠
  - `i18n_entity_text` UK 정식 이름이 63자를 넘는다. 지금은 `uk_i18n_entity_text_target`으로 둠
  - `parent_code_pk`가 같은 `code_group_pk`여야 하는지. 지금은 같은 테이블 자기참조만 둠
  - `valid_from_date <= valid_to_date` CHECK를 둘지. 지금은 두지 않음
  - 통합 테스트 실행 승인. 환경변수 `MDM_TEST_DB_URL`, `MDM_TEST_DB_USERNAME`, `MDM_TEST_DB_PASSWORD` 설정 후 `gradlew.bat integrationTest`. `it_<랜덤>` 스키마만 만들고 삭제함
- 가정한 사항:
  - 공통코드 값 컬럼은 FK를 만들지 않고, 허용값 CHECK도 애플리케이션 검증으로 둔다
  - 감사 사용자 PK와 `common_code_history.code_pk`, `change_user_pk`에는 FK를 두지 않는다
  - `common_code_history.code_pk`에는 조회 인덱스를 둔다
  - `i18n_entity_text`는 다형 참조라 대상 테이블 FK를 두지 않는다

## [3.1단계] 2·3단계 질문 확정사항 반영 (DDL 보완) - 2026-10-10
- 변경/추가 파일:
  - 수정: `src/main/resources/db/migration/public/V1__create_tenant_tables.sql`, `src/main/resources/db/migration/tenant/V1__create_i18n_and_code_tables.sql`, `src/test/java/com/tommy/common/PublicTenantMigrationTest.java`, `src/test/java/com/tommy/common/TenantI18nCodeMigrationTest.java`, `doc/cursor-progress.md`
  - 추가: `src/test/java/com/tommy/common/TenantCodeImmutabilityTest.java`
  - 유지: `Tenant`의 `tenant_code`/`schema_name` `updatable=false`, 변경 도메인 메서드 없음. `CommonCodeRules`, `SupportLanguageRules`
- 검증 결과: `gradlew.bat test` 성공(단위 테스트). `integrationTest`는 실행하지 않았다. V1은 원격 DB에 적용된 기록이 없어 파일을 직접 수정했다.
- ERD 반영 내역:

| 결정 | 반영 |
|---|---|
| `domain_type_code` DEFAULT 없음 | 유지 |
| `primary_yn` 부분 유니크 | `primary_yn='Y' AND use_yn='Y'` |
| 상태 이력 허용값 | `from_status_code`(NULL 허용), `to_status_code` CHECK |
| `schema_name` | `^t_[a-z0-9_]{1,61}$` |
| `tenant_code` | `^[a-z0-9]+(-[a-z0-9]+)*$` |
| 코드/스키마명 변경 불가 | DB 트리거 없음. 엔티티 `updatable=false`와 변경 메서드 부재 |
| `default_yn` 부분 유니크 | `default_yn='Y' AND use_yn='Y'` |
| 기본 언어는 사용 중 | `SupportLanguageRules` 서비스 검증. DB CHECK 없음 |
| `message_type_code`, `translation_status_code` | CHECK 없음 |
| `code_group_code` | `^[A-Z][A-Z0-9_]*$` |
| `nation_code` | `^[A-Z]{2}$` |
| `i18n_entity_text` UK 이름 | `uk_i18n_entity_text_target` 유지 |
| 부모 코드의 코드그룹 | DB 제약 없음. `CommonCodeRules` |
| `valid_from_date <= valid_to_date` | 둘 다 NULL 허용, 둘 다 있을 때만 비교 |

- 질문/확인 필요:
  - 사용자가 `MDM_TEST_DB_*`를 설정하고 `gradlew.bat integrationTest`를 실행한 뒤 결과를 알려주면 progress에 기록한다. 테스트는 `current_database()`가 `pmis`인지 확인하고, 만든 `it_` 스키마만 삭제한다
  - 5단계 DDL은 아직 없다. `dept.valid_from_date <= valid_to_date`, `person_assignment.start_date <= end_date`는 5단계에서 같은 방식(둘 다 NULL 허용, 둘 다 있을 때만 비교)으로 넣는다
- 가정한 사항:
  - V1은 아직 어떤 환경에도 적용되지 않았다. 이미 적용했다면 이 수정 대신 새 버전 파일이 필요하다

## [3.2단계] 통합 테스트 실행 (원격 DB pmis) - 2026-10-10
- 변경/추가 파일: `doc/cursor-progress.md`. 코드는 수정하지 않았다.
- 검증 결과: 실행하지 않았다. 프로세스, 사용자, 시스템 환경변수에 `MDM_TEST_DB_URL`, `MDM_TEST_DB_USERNAME`, `MDM_TEST_DB_PASSWORD`와 `MDM_DB_URL`, `MDM_DB_USERNAME`, `MDM_DB_PASSWORD`가 없다. URL의 DB 이름은 확인할 수 없다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요:
  - 채팅에 접속 정보를 붙이지 말고, 터미널 환경변수 `MDM_TEST_DB_*` 세 값을 설정한 뒤 3.2를 다시 요청한다
  - `CommonApplicationTests`는 `current_database()`가 `pmis`인지 확인하지 않는다. Flyway는 끄지만, 지시의 실행 전 검증과는 어긋난다. 수정은 승인 후 한다
- 가정한 사항:
  - `PublicTenantMigrationTest`, `TenantI18nCodeMigrationTest`, `TenantCodeImmutabilityTest`는 `current_database()`가 `pmis`일 때만 `it_` + 영숫자 16자 스키마를 만들고, 그 스키마만 삭제한다. `pmis`, `public`, `pg_`로 시작하는 이름은 거부한다

## [3.3단계] CommonApplicationTests DB 검증 보완 후 통합 테스트 - 2026-10-10
- 변경/추가 파일:
  - 수정: `src/test/java/com/tommy/common/CommonApplicationTests.java`, `doc/cursor-progress.md`
- 검증 결과:
  - `gradlew.bat test` 성공. `integration` 태그는 제외되어 원격 DB에 접속하지 않았다.
  - 실행 전 확인: 프로세스에 `MDM_TEST_DB_*` 세 값이 있었다. URL의 데이터베이스 이름은 `pmis`이다. 주소, 계정, 비밀번호는 기록하지 않는다.
  - 테스트 코드: `CommonApplicationTests`는 컨텍스트 로딩 전 읽기 전용 연결로 `current_database()`가 `pmis`인지 확인하고, 아니면 skip 한다. Flyway는 `false`다. 마이그레이션 테스트는 `current_database()`가 `pmis`일 때만 `it_` + 영숫자 16자 스키마를 만들고 그 스키마만 `DROP` 한다. `pmis`, `public`, `pg_`로 시작하는 이름은 거부한다. `t_it_` 스키마를 만드는 테스트는 없다.
  - `gradlew.bat integrationTest`: 4건 중 2건 통과, 2건 실패.
    - 통과: `CommonApplicationTests.contextLoads`, `TenantCodeImmutabilityTest.renamingTenantDoesNotChangeCodeOrSchemaName`
    - 실패: `PublicTenantMigrationTest.migratesEmptySchemaAndMatchesErd` — `tenant_status_history.tenant_pk=5`가 `tenant`에 없음 (`fk_tenant_status_history_tenant`)
    - 실패: `TenantI18nCodeMigrationTest.migratesEmptySchemaAndMatchesErd` — `common_code.code_group_pk=2`가 `code_group`에 없음 (`fk_common_code_code_group`)
  - 원인: DDL이 아니다. 거절된 `INSERT`도 `DEFAULT nextval()`이 먼저 실행되어 시퀀스가 증가한다. 성공한 행 뒤에 CHECK 거절이 4건 있으면 `currval('tenant_seq')`는 5가 되고, 거절이 1건 있으면 `currval('code_group_seq')`는 2가 된다. 그 값을 FK로 넣어 실패한다.
  - 실행 후 `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요:
  - 실패 테스트의 `currval()`을, CHECK 거절 `INSERT` 이전이 아니라 성공한 `INSERT` 직후의 PK로 바꿀지. 코드는 수정하지 않았다
- 가정한 사항:
  - JUnit `@EnabledIf`는 Spring 컨텍스트보다 먼저 평가된다. 접속 DB가 `pmis`가 아니거나 확인 쿼리가 실패하면 이 테스트는 skip 되고 컨텍스트는 뜨지 않는다
  - 확인 연결은 `setReadOnly(true)`와 `SELECT current_database()`만 사용한다

## [3.4단계] 통합 테스트 실패 수정 (currval 사용 제거) 후 재실행 - 2026-10-10
- 변경/추가 파일:
  - 수정: `src/test/java/com/tommy/common/PublicTenantMigrationTest.java`, `src/test/java/com/tommy/common/TenantI18nCodeMigrationTest.java`, `doc/cursor-progress.md`
  - 유지: 마이그레이션 SQL. `currval`을 쓰던 다른 테스트는 없다
- 검증 결과:
  - `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았다.
  - 실행 전 확인: `MDM_TEST_DB_*` 세 값이 있었다. URL의 데이터베이스 이름은 `pmis`이다. 주소, 계정, 비밀번호는 기록하지 않는다.
  - `gradlew.bat integrationTest`: 4건 모두 통과, 실패 0, skip 0.
    - `CommonApplicationTests.contextLoads`
    - `PublicTenantMigrationTest.migratesEmptySchemaAndMatchesErd`
    - `TenantCodeImmutabilityTest.renamingTenantDoesNotChangeCodeOrSchemaName`
    - `TenantI18nCodeMigrationTest.migratesEmptySchemaAndMatchesErd`
  - 실행 후 `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요: 없음
- 가정한 사항:
  - 성공한 `INSERT ... RETURNING`의 PK를 저장해 자식 행에 넣는다. 거절되는 `INSERT`는 시퀀스를 소모해도 그 PK를 쓰지 않는다
  - JDBC 기본값은 auto-commit이라 거절된 문장은 그 문장만 롤백된다. auto-commit이 꺼져 있으면 `SAVEPOINT`로 롤백한다

## [5단계] 테넌트 스키마 Flyway V2 (회사, 부서, 사람, 사용자) - 2026-10-10
- 변경/추가 파일:
  - 추가: `src/main/resources/db/migration/tenant/V2__create_company_dept_person_user_tables.sql`, `src/test/java/com/tommy/common/OrgUserMigrationTest.java`
  - 수정: `src/test/java/com/tommy/common/TenantI18nCodeMigrationTest.java`, `doc/cursor-progress.md`
- 검증 결과: `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았고 `integrationTest`는 실행하지 않았다. 통합 테스트는 작성만 했다. `TenantI18nCodeMigrationTest`는 Flyway `target("1")`로 V1만 적용한다. V2 테이블이 생겨도 다국어 ERD와 테이블 목록을 그대로 비교한다.
- ERD 반영 내역: 기준 파일 `doc/erd/erd-model-org-user-20261009.json`. 테이블 9개. Seed 없음. SQL에 스키마명 없음.

| 항목 | ERD | DDL |
|---|---|---|
| PK | BIGINT + 시퀀스 | `<테이블>_seq INCREMENT BY 1`, `DEFAULT nextval` |
| company.company_code | UK | `uk_company_company_code` |
| company.business_registration_no | NULL 제외 UK | 부분 유니크 `WHERE business_registration_no IS NOT NULL` |
| company.parent_company_pk | 자기참조, NULL 허용 | `fk_company_company` |
| company.nation_pk | 다른 ERD의 nation | `fk_company_nation`. NULL 허용 |
| company.company_type_code | 공통코드 COMPANY_TYPE | 문자열 저장. CHECK 없음 |
| dept (company_pk, dept_code) | UK | `uk_dept_company_pk_dept_code` |
| dept.parent_dept_pk | 자기참조, NULL 허용. 같은 회사·순환 금지는 애플리케이션 | `fk_dept_dept`. 회사 일치와 순환 금지는 DB 제약 없음 |
| dept.valid_from_date, valid_to_date | 시작 필수, 종료 NULL=유효 | `valid_from_date NOT NULL`. CHECK는 둘 다 NULL 허용 형태이고, 둘 다 있을 때만 비교 |
| dept_history | Append-only. 감사 4컬럼 없음 | ERD 컬럼만. `before_parent_dept_pk`, `after_parent_dept_pk`, `change_user_pk` FK 없음 |
| dept_history.change_type_code | 공통코드 DEPT_CHANGE_TYPE | CHECK 없음 |
| person.profile_file_pk | file FK는 나중 마이그레이션 | 컬럼만. FK 없음 |
| person_assignment.dept_pk | NULL 허용 | `fk_person_assignment_dept` |
| person_assignment (company_pk, employee_no) | NULL 제외 UK | `WHERE employee_no IS NOT NULL` |
| person_assignment.primary_yn | 사람당 유효 소속 중 1건만 Y. 기본값 N | `DEFAULT 'N'`. 부분 유니크 `WHERE primary_yn='Y' AND end_date IS NULL` |
| person_assignment.start_date, end_date | 시작 필수, 종료 NULL=현재 | `start_date NOT NULL`. CHECK는 둘 다 있을 때만 비교 |
| 소속 코드 컬럼 | 공통코드 | CHECK 없음. 부서와 회사 일치, 순환 참조는 DB 제약 없음 |
| user_account.login_id | 소문자 정규화 저장 전제 UK | 컬럼 UK. 소문자 CHECK와 함수 인덱스는 없음 |
| user_account.person_pk | NULL 제외 UK, NULL 허용 | `fk_user_account_person`. 부분 유니크 `WHERE person_pk IS NOT NULL` |
| user_account.support_language_code | support_language FK, NULL 허용 | `fk_user_account_support_language` |
| user_account.login_fail_count | 기본값 0 | `NOT NULL DEFAULT 0` |
| user_password_history | Append-only | `changed_user_pk` FK 없음 |
| user_login_history.user_pk | 없는 ID 시도는 NULL | NULL 허용 FK |
| user_login_history.login_datetime | 건수가 많음 | `ix_user_login_history_login_datetime`. 파티션 없음 |
| user_refresh_token.token_hash | UK | `uk_user_refresh_token_token_hash` |
| 감사 사용자 PK | FK 없음 | `create_user_pk`, `update_user_pk` FK 없음 |
| use_yn, *_yn, create_datetime | Y/N, 기본값, 현재일시 | `CHECK (IN ('Y','N'))`, `use_yn DEFAULT 'Y'`, `primary_yn`/`dept_head_yn`/`must_change_password_yn DEFAULT 'N'`, `create_datetime DEFAULT CURRENT_TIMESTAMP` |

- 질문/확인 필요:
  - `primary_yn` 부분 유니크에 `use_yn='Y'`를 넣을지. 지금은 `primary_yn='Y' AND end_date IS NULL`만이다. PostgreSQL 부분 유니크 인덱스로 사람당 미종료 주소속 Y 1건을 강제할 수 있다. 종료된 행의 Y와 `primary_yn='N'`은 막지 않는다. `use_yn='N'`이고 `end_date`가 NULL인 Y는 새 주소속을 막는다. 3.1의 비슷한 인덱스는 `use_yn='Y'`를 포함했다
  - `login_id` 소문자 저장을 DB에서 강제할지. 지금은 컬럼 UK만이다
  - `user_login_history` 파티션은 만들지 않았다. 보존 기간으로 오래된 구간을 버릴 때가 오면 월 단위 RANGE를 검토한다. PostgreSQL은 파티션 키를 PK에 넣어야 해서, 그때 `login_history_pk` 단독 PK를 바꿔야 한다. 그 전에는 `login_datetime` 인덱스로 둔다
  - 통합 테스트 실행 승인. `OrgUserMigrationTest`는 `current_database()`가 `pmis`인지 확인하고 `it_<랜덤>` 스키마만 만든 뒤 삭제한다
- 가정한 사항:
  - 공통코드 값 컬럼은 FK와 허용값 CHECK를 두지 않는다
  - 상위 부서가 같은 회사이어야 한다는 조건과 순환 참조 금지는 애플리케이션 검증이다
  - `dept_head_yn`은 부서당 1명 제한이 ERD에 없어 유니크를 두지 않는다
  - `person.profile_file_pk`의 `file` FK는 V2에 넣지 않는다
  - 이메일·전화번호 암호화는 이 DDL에서 하지 않는다

## [5.1단계] V2 질문 확정사항 반영 후 통합 테스트 실행 - 2026-10-10
- 변경/추가 파일:
  - 수정: `src/main/resources/db/migration/tenant/V2__create_company_dept_person_user_tables.sql`, `src/test/java/com/tommy/common/OrgUserMigrationTest.java`, `doc/cursor-progress.md`
- 검증 결과:
  - V2는 이전 단계에서 통합 테스트를 실행하지 않았고, 앱 Flyway 기본값도 꺼져 있어 어떤 환경에도 적용되지 않은 것으로 보고 파일을 직접 수정했다
  - `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*` 세 값이 있었다. URL의 데이터베이스 이름은 `pmis`이다. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest`: 5건 모두 통과, 실패 0, skip 0
    - `CommonApplicationTests.contextLoads`
    - `PublicTenantMigrationTest.migratesEmptySchemaAndMatchesErd`
    - `TenantCodeImmutabilityTest.renamingTenantDoesNotChangeCodeOrSchemaName`
    - `TenantI18nCodeMigrationTest.migratesEmptySchemaAndMatchesErd`
    - `OrgUserMigrationTest.migratesEmptySchemaAndMatchesErd`
  - 실행 후 `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역:

| 결정 | 반영 |
|---|---|
| `primary_yn` 부분 유니크 | `primary_yn='Y' AND end_date IS NULL AND use_yn='Y'` |
| `login_id` 소문자 | `ck_user_account_login_id` `login_id = lower(login_id)`. 컬럼 UK 유지. 저장 전 정규화는 7단계 이후 |
| `user_login_history` 파티션 | 하지 않음. `login_datetime` 인덱스 유지 |
| `dept_head_yn` 부서당 1명 | 유니크 없음 |

- 질문/확인 필요: 없음
- 가정한 사항:
  - V2는 아직 어떤 환경에도 적용되지 않았다. 이미 적용했다면 이 수정 대신 새 버전 파일이 필요하다

## [5.5단계] 테넌트 스키마 Flyway V3 (파일, 팀, 권한, 메뉴) - 2026-10-10
- 변경/추가 파일:
  - 추가: `src/main/resources/db/migration/tenant/V3__create_file_team_role_menu_tables.sql`, `src/main/java/com/tommy/common/auth/PermissionRules.java`, `src/test/java/com/tommy/common/FileTeamAuthMenuMigrationTest.java`
  - 수정: `src/test/java/com/tommy/common/OrgUserMigrationTest.java`, `src/test/java/com/tommy/common/CodeAndLanguageRuleTest.java`, `doc/cursor-progress.md`
  - 유지: V2 SQL. `person.profile_file_pk` FK는 V3의 `ALTER TABLE`로 추가
- 검증 결과: `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았고 `integrationTest`는 실행하지 않았다. 통합 테스트는 작성만 했다. `OrgUserMigrationTest`는 Flyway `target("2")`로 V3 테이블을 적용하지 않는다.
- ERD 반영 내역: 기준 파일 `doc/erd/erd-model-file-team-auth-menu-20261009.json`. 테이블 10개. Seed 없음. SQL에 스키마명 없음.

| 항목 | ERD | DDL |
|---|---|---|
| PK | BIGINT + 시퀀스 | `<테이블>_seq INCREMENT BY 1` |
| file_storage.storage_code | UK | `uk_file_storage_storage_code` |
| file_storage.default_yn | 사용 중 1건만 Y. 기본값 N | `DEFAULT 'N'`. 부분 유니크 `WHERE default_yn='Y' AND use_yn='Y'` |
| file_storage.connection_config | CLOB | TEXT |
| file_storage.storage_type_code | 공통코드 STORAGE_TYPE | CHECK 없음 |
| file (storage_pk, storage_key) | UK | `uk_file_storage_pk_storage_key` |
| file.file_size | 0 이상 | `ck_file_file_size` `file_size >= 0` |
| file.upload_user_pk | FK 없음 | FK 없음 |
| file.file_status_code | 공통코드 FILE_STATUS | CHECK 없음 |
| file_attachment (대상 테이블, 키, file_pk) | UK. 다형 참조 | `uk_file_attachment_target`. 대상 테이블 FK 없음. 이 UK가 `(target_table_name, target_key)` 조회를 커버한다 |
| person.profile_file_pk | file FK | V3에서 `fk_person_file`. V2 파일은 수정하지 않음 |
| team.team_code | UK | `uk_team_team_code` |
| team.start_date, end_date | 종료일 >= 시작일. 둘 다 NULL 허용 | 둘 다 있을 때만 비교 |
| team.team_type_code | 공통코드 TEAM_TYPE | CHECK 없음 |
| team_member.person_pk | person FK | `fk_team_member_person` |
| team_member (team_pk, person_pk) | 현재 소속 1건 | `WHERE end_date IS NULL AND use_yn='Y'` |
| team_member 기간 | 종료일 >= 시작일. 시작 필수 | `start_date NOT NULL`. CHECK는 둘 다 있을 때만 비교 |
| team_member.team_role_code | 공통코드 TEAM_ROLE | CHECK 없음 |
| role.role_code | UK. 대문자 SNAKE | 컬럼 UK. 형식 CHECK는 없음 |
| user_role.user_pk | user_account FK | `fk_user_role_user`. `(user_pk, role_pk)` UK가 `user_pk` 조회를 커버한다 |
| user_role 기간 | valid_from_date <= valid_to_date. 둘 다 NULL 허용 | 둘 다 있을 때만 비교 |
| menu.parent_menu_pk | 자기참조, NULL 허용. 순환 금지 | `fk_menu_menu`. 순환 금지는 DB 제약 없음. `ix_menu_parent_menu_pk` |
| menu.menu_code | UK | `uk_menu_menu_code` |
| menu.menu_type_code | 공통코드 MENU_TYPE | CHECK 없음 |
| menu.visible_yn | Y/N. 기본값 Y | `DEFAULT 'Y'` CHECK |
| permission (menu_pk, action_code) | UK | `uk_permission_menu_pk_action_code`. `menu_pk` 조회를 커버한다 |
| permission.permission_code | UK | `uk_permission_permission_code` |
| permission.action_code | 공통코드 PERMISSION_ACTION | CHECK 없음 |
| role_permission (role_pk, permission_pk) | UK | `uk_role_permission_role_pk_permission_pk`. `ix_role_permission_permission_pk` |
| READ 이외 권한 | 같은 메뉴의 READ 필요 | DB 트리거 없음. `PermissionRules` |
| 감사 사용자 PK | FK 없음 | FK 없음 |
| use_yn, create_datetime | Y 기본, 현재일시 기본, nullable | `DEFAULT 'Y'`, `DEFAULT CURRENT_TIMESTAMP` |

- 질문/확인 필요:
  - `role.role_code`에 `^[A-Z][A-Z0-9_]*$` CHECK를 둘지. 지금은 UK만 둠
  - 통합 테스트 실행 승인. `FileTeamAuthMenuMigrationTest`는 `current_database()`가 `pmis`인지 확인하고 `it_<랜덤>` 스키마만 만든 뒤 삭제한다
- 가정한 사항:
  - 공통코드 값 컬럼은 FK와 허용값 CHECK를 두지 않는다
  - GROUP 메뉴에 권한을 만들지 않는 규칙과 메뉴 순환 금지는 애플리케이션 검증이다
  - `(target_table_name, target_key)`, `user_role.user_pk`, `permission.menu_pk`는 복합 UK의 앞쪽 컬럼이라 같은 이름의 단독 인덱스를 중복으로 만들지 않았다
  - `team_member.person_pk`와 `role_permission.permission_pk`는 복합 UK의 앞쪽이 아니라서 조회 인덱스를 두었다

## [5.6단계] V3 질문 확정사항 반영 후 통합 테스트 실행 - 2026-10-10
- 변경/추가 파일:
  - 수정: `src/main/resources/db/migration/tenant/V3__create_file_team_role_menu_tables.sql`, `src/test/java/com/tommy/common/FileTeamAuthMenuMigrationTest.java`, `doc/cursor-progress.md`
  - 추가: `src/test/java/com/tommy/common/AllMigrationTest.java`
- 검증 결과:
  - V3는 5.5단계에서 통합 테스트를 실행하지 않았고, 앱 Flyway 기본값도 꺼져 있어 어떤 환경에도 적용되지 않은 것으로 보고 파일을 직접 수정했다
  - `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*` 세 값이 있었다. URL의 데이터베이스 이름은 `pmis`이다. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest`: 7건 모두 통과, 실패 0, skip 0
    - `CommonApplicationTests.contextLoads`
    - `PublicTenantMigrationTest.migratesEmptySchemaAndMatchesErd`
    - `TenantCodeImmutabilityTest.renamingTenantDoesNotChangeCodeOrSchemaName`
    - `TenantI18nCodeMigrationTest.migratesEmptySchemaAndMatchesErd`
    - `OrgUserMigrationTest.migratesEmptySchemaAndMatchesErd`
    - `FileTeamAuthMenuMigrationTest.migratesEmptySchemaAndMatchesErd`
    - `AllMigrationTest.appliesPublicAndTenantMigrations`
  - 실행 후 `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역:

| 결정 | 반영 |
|---|---|
| `role.role_code` | `ck_role_role_code` `^[A-Z][A-Z0-9_]*$`. UK 유지 |
| 전체 마이그레이션 | 빈 `it_` 스키마 하나에 공용 V1 다음 테넌트 V1~V3. 업무 테이블은 공용 5개와 테넌트 31개 |

- 질문/확인 필요: 없음
- 가정한 사항:
  - V3는 아직 어떤 환경에도 적용되지 않았다. 이미 적용했다면 이 수정 대신 새 버전 파일이 필요하다
  - 공용과 테넌트 Flyway 버전 번호가 겹친다. 한 스키마에 둘 다 적용하는 테스트만 테넌트 이력 테이블을 `flyway_tenant_schema_history`로 두고, 이미 테이블이 있으면 baseline 0 다음 마이그레이션을 적용한다. 실제 테넌트 스키마는 이력 테이블을 나누지 않는다

## [4단계] 멀티테넌시 인프라 설계안 - 2026-10-10
- 변경/추가 파일:
  - 추가: `doc/multitenancy-design.md`
  - 수정: `doc/cursor-progress.md`
- 검증 결과: 코드를 쓰지 않았다. 빌드와 테스트는 실행하지 않았다. 설계안은 `doc/multitenancy-design.md`이다.
- ERD 반영 내역: 해당 없음
- 질문/확인 필요: `doc/multitenancy-design.md` 맨 위의 결정 필요 목록 1~7. 승인한 뒤에 구현한다.
  1. 테넌트 식별은 Host, JWT, 또는 둘 다인지
  2. PMIS가 MDM을 호출할 때의 인증은 공유 JWT, 서비스 토큰, 토큰 교환 중 무엇인지
  3. 인프라 패키지를 `global.multitenancy`로 바꿀지. 추천은 바꾼다
  4. SessionFactory를 하나로 둘지. 추천은 하나이고, 공용 엔티티만 `@Table(schema = "pmis")`로 둔다
  5. 기동 시 테넌트 일괄 업그레이드를 할지. 추천은 하지 않고 별도 실행으로 한다
  6. 실패한 스키마를 자동 삭제할지. 추천은 자동 삭제하지 않고 승인 뒤에만 지운다
  7. PMIS Flyway 이력 테이블 이름을 `flyway_schema_history_pmis`로 할지
- 가정한 사항:
  - Hibernate 7은 `MultiTenantConnectionProvider`와 `CurrentTenantIdentifierResolver` 등록으로 SCHEMA 분리를 한다. `hibernate.multiTenancy=SCHEMA`와 `TenantSchemaMapper`는 쓰지 않는다
  - 운영에서 공용 V1과 테넌트 V1은 스키마가 달라 버전 번호가 겹쳐도 이력 테이블이 충돌하지 않는다. `AllMigrationTest`의 이력 테이블 분리는 테스트 전용이다
  - 스키마명은 요청마다 테넌트 코드에서 조합하지 않고 `tenant.schema_name` 저장값을 쓴다
  - 규칙 문서의 패키지 문구는 이번 단계에서 고치지 않았다

## [4.2단계] 공용 스키마 Flyway V2 (플랫폼 운영자) - 2026-10-10
- 변경/추가 파일:
  - 추가: `src/main/resources/db/migration/public/V2__create_platform_admin_tables.sql`
  - 수정: `src/test/java/com/tommy/common/PublicTenantMigrationTest.java`, `doc/cursor-progress.md`
  - 유지: 공용 V1. 엔티티 클래스는 만들지 않았다
- 검증 결과:
  - `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*`가 있었다. URL의 데이터베이스 이름은 `pmis`이다. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest`: 7건 모두 통과, 실패 0, skip 0. `PublicTenantMigrationTest`가 공용 V1+V2를 ERD와 비교하고 `login_id` 대문자와 `token_hash` 중복을 거절했다
  - 실행 후 `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: 기준 파일 `doc/erd/erd-model-tenant-20261009.json`. 추가 테이블 3개. 스키마명 없음.

| 항목 | ERD | DDL |
|---|---|---|
| platform_admin.login_id | UK, 소문자 | `uk_platform_admin_login_id`, `ck_platform_admin_login_id` `login_id = lower(login_id)` |
| platform_admin.email | NOT NULL, UK 아님 | `NOT NULL`. UK 없음 |
| platform_admin.password_hash | NOT NULL | `NOT NULL` |
| must_change_password_yn | 기본값 N | `DEFAULT 'N'` CHECK Y/N |
| login_fail_count | 기본값 0 | `NOT NULL DEFAULT 0` |
| use_yn | 기본값 Y | `DEFAULT 'Y'` CHECK Y/N |
| admin_status_code, login_result_code, revoke_reason_code | 애플리케이션 Enum | CHECK 없음 |
| 감사 사용자 PK | FK 없음 | FK 없음 |
| platform_admin_login_history.platform_admin_pk | NULL 허용 FK | `fk_platform_admin_login_history_platform_admin` |
| login_datetime | 조회 인덱스 | `ix_platform_admin_login_history_login_datetime` |
| platform_admin_refresh_token.token_hash | UK | `uk_platform_admin_refresh_token_token_hash` |
| platform_admin_pk 인덱스 | 이력, 토큰 | `ix_platform_admin_login_history_platform_admin_pk`, `ix_platform_admin_refresh_token_platform_admin_pk` |

- 질문/확인 필요: 없음
- 가정한 사항:
  - `change_user_pk`, `executed_user_pk`의 운영자 FK는 V1 컬럼 설명이 갱신되어도 V1 파일은 수정하지 않는다. 이번 V2에도 그 FK를 추가하지 않았다
  - 이력과 토큰 테이블에는 감사 4컬럼을 두지 않는다

## [4.1단계] 멀티테넌시 인프라 구현 (프로비저닝 제외) - 2026-10-10
- 변경/추가 파일:
  - 수정: `doc/multitenancy-design.md`, `src/main/java/com/tommy/common/tenant/entity/Tenant.java`, `src/test/java/com/tommy/common/TenantCodeImmutabilityTest.java`, `doc/cursor-progress.md`
  - 추가: `global.multitenancy` 패키지(컨텍스트, 스키마명 검증, Host/JWT/헤더 리졸버, 식별자 리졸버, 커넥션 프로바이더, DB 이름 검사, 요청 필터, Hibernate 등록), `global.exception.ApiException`, `global.exception.ApiExceptionHandler`, `global.response.ApiResponse`, `tenant.JdbcTenantDirectory`, `src/test/java/com/tommy/common/global/multitenancy/SchemaNamesTest.java`, `MultitenancyInfrastructureTest.java`, `src/test/java/com/tommy/common/TenantSchemaIsolationTest.java`
  - 만들지 않음: 프로비저닝/업그레이드 서비스, 테넌트 관리 API, JWT 라이브러리, 실제 토큰 파싱. `.cursor/rules`는 수정하지 않았다
- 검증 결과:
  - `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*`가 있었다. URL의 데이터베이스 이름은 `pmis`이다. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest`: 9건 중 8건 통과, 실패 0, skip 1
    - 통과: `AllMigrationTest`, `CommonApplicationTests.contextLoads`, `FileTeamAuthMenuMigrationTest`, `OrgUserMigrationTest`, `PublicTenantMigrationTest`, `TenantCodeImmutabilityTest`, `TenantI18nCodeMigrationTest`, `TenantSchemaIsolationTest.isolatesSchemas`
    - skip: `TenantSchemaIsolationTest.rejectsMissingAndSuspendedTenants`. 공유 스키마 `pmis`에 `tenant` 테이블이 없다. 공용 Flyway는 적용하지 않았다
  - 스키마 격리 통합 테스트는 `t_it_` 두 스키마의 데이터가 섞이지 않는지, 반납 뒤 `search_path`에 이전 테넌트가 남지 않는지를 확인했다
  - 없는 테넌트와 `SUSPENDED` 거절은 단위 테스트에서 확인했다. DB 조회 통합 테스트는 위 skip이다
  - 실행 후 `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: 해당 없음. DDL은 추가하지 않았다. 공용 엔티티 `Tenant`만 `@Table(schema = "pmis")`를 붙였다
- 질문/확인 필요:
  - 공유 개발 DB의 스키마 `pmis`에 공용 Flyway V1+V2를 적용해도 되는가. 적용 전에는 `JdbcTenantDirectory`가 `pmis.tenant`와 `pmis.tenant_domain`을 조회하는 통합 테스트를 실행할 수 없다
- 가정한 사항:
  - Spring Boot 4.1.1은 테넌트 빈을 자동 등록하지 않는다. `HibernatePropertiesCustomizer`와 `AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER`(`hibernate.multi_tenant_connection_provider`), `AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER`(`hibernate.tenant_identifier_resolver`)로 등록한다
  - 요청 스레드가 아니면 식별자는 `pmis`이다. SessionFactory 기동에 필요하다. 요청 중이고 컨텍스트가 비어 있으면 거절한다
  - `X-Tenant-Code` 리졸버는 `dev` 프로필에만 등록한다. 기본 프로필은 `local`이다
  - JWT 클레임 리더는 빈 값을 반환하는 스텁이다. 클레임 이름과 서명 검증은 인증 단계에서 정한다
  - 같은 테넌트 프로비저닝 중복은 `pg_advisory_lock`으로 막는다. 잠금 구현은 프로비저닝 서비스를 만들 때 한다
  - `ddl-auto`는 `none`이다. 엔티티와 테넌트 DDL 비교는 테넌트 스키마 통합 테스트에서 한다
  - 불변 컬럼 테스트는 엔티티의 스키마 `pmis`를 테스트 `it_` 스키마로 바꿔 실행한다. 공유 `pmis.tenant`에는 쓰지 않는다

## [4.3단계] 공유 개발 DB 스키마 pmis 공용 마이그레이션 적용 - 2026-10-10
- 변경/추가 파일:
  - 수정: `doc/cursor-progress.md`
  - 코드와 DDL 파일은 바꾸지 않았다. 운영자 초기 계정은 만들지 않았다
- 검증 결과:
  - 적용 전 읽기 전용 확인: URL의 데이터베이스 이름은 `pmis`이다. `current_database()`도 `pmis`이다. 스키마 `pmis`가 있고 테이블과 시퀀스는 0개였다. `flyway_schema_history`는 없었다. `public` 테이블도 0개였다. 주소, 계정, 비밀번호는 기록하지 않는다
  - 이 프로세스에서만 `MDM_FLYWAY_ENABLED=true`로 앱을 기동했다. 영구 설정과 파일에는 남기지 않았다. 앱이 읽는 `MDM_DB_*`는 이 프로세스에서만 테스트 접속 정보로 채웠다. 대상 데이터베이스 이름은 `pmis`이다
  - 2026-10-10 16:15:16에 V1 `create tenant tables` success, 16:15:17에 V2 `create platform admin tables` success. 스키마 `pmis`는 버전 v2이다
  - 첫 기동은 마이그레이션 뒤에 8080 포트가 이미 사용 중이라 웹 서버가 뜨지 못했다. 스키마는 지우거나 clean 하지 않았다. 이어서 18080 포트로 다시 기동했고, Flyway는 추가 마이그레이션 없이 최신 상태라고 한 뒤 앱이 시작되었다. 테넌트 마이그레이션 로그는 없었다. 시작을 확인한 뒤 앱을 종료했다
  - 기동 로그에 접속 URL과 개발용 생성 비밀번호가 찍혔다. 둘 다 이 문서에는 남기지 않는다
  - 적용 후 스키마 `pmis` 테이블 9개: `flyway_schema_history`, `platform_admin`, `platform_admin_login_history`, `platform_admin_refresh_token`, `tenant`, `tenant_domain`, `tenant_provisioning`, `tenant_schema_migration`, `tenant_status_history`. `public` 테이블은 0개로 그대로이다
  - `gradlew.bat integrationTest --rerun-tasks`: 9건 모두 통과, 실패 0, skip 0. `TenantSchemaIsolationTest.rejectsMissingAndSuspendedTenants`도 통과했다
  - 테스트 종료 후 `pmis`의 업무 테이블 8개는 모두 0행이다. `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: 해당 없음. 기존 공용 V1+V2를 스키마 `pmis`에 적용했다
- 질문/확인 필요: 없음
- 가정한 사항:
  - 사용자 환경에는 `MDM_DB_*`가 없고 `MDM_TEST_DB_*`만 있었다. 같은 데이터베이스 `pmis`이므로 이번 기동 프로세스에서만 앱 접속 정보로 사용했다

## [4.4단계] 테넌트 프로비저닝·일괄 업그레이드 (MIGRATED까지) - 2026-10-10
- 변경/추가 파일:
  - 수정: `doc/multitenancy-design.md`, `src/main/java/com/tommy/common/global/multitenancy/SchemaNames.java`, `src/test/java/com/tommy/common/global/multitenancy/SchemaNamesTest.java`, `doc/cursor-progress.md`
  - 추가: `tenant.provision` 패키지(`TenantProvisioningService`, `TenantUpgradeService`, `ProvisioningStep`과 `REQUESTED`/`SCHEMA_CREATED`/`MIGRATED` 구현, 저널, Flyway 적용, advisory lock, 임시 비밀번호 객체), `src/test/java/com/tommy/common/tenant/provision/ProvisioningBehaviorTest.java`, `src/test/java/com/tommy/common/TenantProvisioningIntegrationTest.java`
  - 만들지 않음: `SEEDED`/`ADMIN_CREATED` 구현체, 컨트롤러, 비밀번호 인코더, 프로덕션 `DROP SCHEMA`
- 검증 결과:
  - `gradlew.bat test` 성공. 원격 DB에는 접속하지 않았다. 끝난 단계 건너뛰기, 핸들러 없는 단계에서 멈추기, 락 실패 즉시 거절, 임시 비밀번호 `toString` 가림, 스키마명 검증을 확인했다
  - 실행 전 확인: `MDM_TEST_DB_*`가 있었다. URL의 데이터베이스 이름은 `pmis`이다. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest --rerun-tasks`: 14건 모두 통과, 실패 0, skip 0. 기존 9건과 프로비저닝 5건이다
    - 스키마 생성 후 테넌트 V1~V3로 업무 테이블 31개와 `flyway_schema_history`. 상태는 `PROVISIONING`, 단계는 `MIGRATED`, 실행은 `RUNNING`. 다시 실행해도 이어진다
    - 마이그레이션 실패 시 `FAILED`와 `SCHEMA_CREATED`, 재시도는 다음 단계부터 성공
    - 기록 없는 기존 스키마는 `TENANT.SCHEMA_NOT_OWNED`로 중단하고 Flyway를 적용하지 않음
    - 같은 테넌트 동시 실행은 `TENANT.PROVISIONING_IN_PROGRESS`
    - 일괄 업그레이드는 `ACTIVE`만 처리하고 `SUSPENDED`는 건너뜀. 한 테넌트 실패가 다른 테넌트를 막지 않음. 반납 뒤 `search_path`에 이전 테넌트 스키마가 남지 않음
  - 실행 후 `pmis` 업무 테이블 8개는 모두 0행이다. `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: 해당 없음. DDL은 추가하지 않았다
- 질문/확인 필요: 없음
- 가정한 사항:
  - 스키마명이 없으면 `t_`와 테넌트 코드의 하이픈을 밑줄로 바꾼 값을 한 곳(`SchemaNames.schemaNameForTenantCode`)에서 만든다
  - 락 키는 `pg_try_advisory_lock(hashtext('pmis.tenant'), hashtext(schema_name))`이고, 일괄 실행은 `pg_try_advisory_lock(hashtext('pmis.tenant.upgrade-all'), 0)`이다
  - `SEEDED`와 `ADMIN_CREATED` 구현체가 없어 성공한 프로비저닝은 `ACTIVE`로 바뀌지 않는다. 임시 비밀번호 객체는 만들어 두었고, 이번 결과에는 넣지 않는다. 해시는 인코더를 정한 뒤에 저장한다
  - 업그레이드 실패는 테넌트 상태를 `FAILED`로 바꾸지 않고 `tenant_schema_migration`만 `FAILED`로 남긴다

## [6단계] 테넌트 Seed와 SEEDED - 2026-10-10
- 변경/추가 파일:
  - 수정: `doc/multitenancy-design.md`, `src/main/java/com/tommy/common/tenant/provision/TenantUpgradeService.java`, `src/test/java/com/tommy/common/TenantProvisioningIntegrationTest.java`, `src/test/java/com/tommy/common/tenant/provision/ProvisioningBehaviorTest.java`, `doc/cursor-progress.md`
  - 추가: `src/main/java/com/tommy/common/tenant/provision/TenantSeedRunner.java`, `src/main/java/com/tommy/common/tenant/provision/SeededProvisioningStep.java`, `src/main/resources/db/seed/tenant/S01_support_language.sql`, `S02_nation.sql`, `S03_code.sql`, `S04_i18n.sql`, `S05_role_menu_permission.sql`
  - 만들지 않음: `ADMIN_CREATED` 구현체, 최초 관리자, 업무 데이터, `file_storage` 시드. 테넌트 DDL V1~V3는 수정하지 않음
- 검증 결과:
  - `gradlew.bat test`: 19건 통과, 실패 0. 파일명 정렬, 다음 핸들러가 없으면 `SEEDED`에서 멈춤을 확인했다. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*`가 있었다. URL의 데이터베이스 이름은 `pmis`이다. `MDM_FLYWAY_ENABLED=false`. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest --rerun-tasks`: 15건 모두 통과, 실패 0, skip 0. 기존 14건과 Seed 검증 1건이다
    - 프로비저닝은 `PROVISIONING` / `SEEDED` / `RUNNING`에서 멈춘다. `ADMIN_CREATED`는 실행하지 않는다
    - 같은 Seed를 다시 실행해도 행 수가 늘지 않는다. 테넌트가 고친 코드명과 역할명은 덮어쓰지 않는다
    - 기본 언어는 `ko-KR` 하나다. 공통코드와 i18n 메시지는 `ko-KR`과 `en-US` 이름이 있다
    - 페이지 메뉴는 READ/CREATE/UPDATE/DELETE/EXPORT, SYSTEM은 READ만. 다른 권한에는 READ가 함께 있다. `TENANT_ADMIN`은 시드 권한 전부, USER는 쓰기가 없다
  - 실행 후 `pmis` 업무 테이블 8개는 모두 0행이다. `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: DDL 변경 없음. 공통코드는 테넌트 스키마 Seed로만 넣었다. 공용 스키마 상태(테넌트 상태, 플랫폼 관리자 상태)는 애플리케이션 Enum이라 Seed하지 않았다
- 공통코드 그룹 (`solution_code=MDM`):

  | 그룹 | 값 |
  | --- | --- |
  | STORAGE_TYPE | LOCAL, NAS, S3, AZURE_BLOB, GCS |
  | FILE_STATUS | TEMP, ACTIVE, DELETED |
  | ATTACHMENT_CATEGORY | 그룹만. 값 없음 |
  | TEAM_TYPE | PROJECT, TASKFORCE, COMMITTEE, ETC |
  | TEAM_ROLE | LEADER, MEMBER, ADVISOR |
  | MENU_TYPE | GROUP, PAGE |
  | PERMISSION_ACTION | READ, CREATE, UPDATE, DELETE, EXPORT, APPROVE |
  | COMPANY_TYPE | OWN, AFFILIATE, CLIENT, CONTRACTOR, PARTNER, OTHER |
  | DEPT_TYPE | DIVISION, DEPARTMENT, TEAM, PROJECT, ETC |
  | DEPT_CHANGE_TYPE | CREATE, RENAME, MOVE, CLOSE, REOPEN |
  | EMPLOYMENT_TYPE | REGULAR, CONTRACT, DISPATCH, EXTERNAL |
  | POSITION | 그룹만. 값 없음 |
  | JOB_TITLE | 그룹만. 값 없음 |
  | ASSIGNMENT_STATUS | ACTIVE, LEAVE, RESIGNED |
  | ACCOUNT_TYPE | NORMAL, SYSTEM |
  | USER_STATUS | ACTIVE, LOCKED, DORMANT, WITHDRAWN |
  | PASSWORD_CHANGE_TYPE | INITIAL, CHANGE, RESET, EXPIRED_CHANGE |
  | LOGIN_RESULT | SUCCESS, FAIL_PASSWORD, FAIL_LOCKED, FAIL_INACTIVE, FAIL_NOT_FOUND, FAIL_EXPIRED |
  | TOKEN_REVOKE_REASON | LOGOUT, ROTATED, REUSE_DETECTED, ADMIN, PASSWORD_CHANGED |
  | I18N_MESSAGE_TYPE | LABEL, MESSAGE, ERROR, TOOLTIP, MENU, NOTICE, MAIL |
  | I18N_TRANSLATION_STATUS | DRAFT, TRANSLATED, REVIEWED, APPROVED |

- 질문/확인 필요:
  - `ATTACHMENT_CATEGORY`: ERD 예시는 PROFILE, CONTRACT, PHOTO이고 대상마다 정한다고 되어 있어 값을 만들지 않았다. 확정 목록이 있으면 알려 달라
  - `POSITION`: 사원/대리/과장/차장/부장 등 예시만 있고 코드가 없어 값을 만들지 않았다
  - `JOB_TITLE`: 팀장/실장/본부장/PM 등 예시만 있고 코드가 없어 값을 만들지 않았다
- 가정한 사항:
  - 멱등은 `INSERT ... WHERE NOT EXISTS`이다. 있는 행은 고치지 않는다. 부분 유니크 인덱스와 맞지 않는 `ON CONFLICT`는 쓰지 않았다
  - `ko-KR`은 기본 언어, fallback 없음, LTR, `yyyy-MM-dd`, `yyyy-MM-dd HH:mm`, `#,##0.##`. `en-US`는 fallback `ko-KR`, LTR, `MM/dd/yyyy`, `MM/dd/yyyy HH:mm`, 같은 숫자 형식, 기본 아님
  - 국가 30개의 ISO, 전화 국가번호, 통화는 표준값으로 채웠다. `nation.default_language_code`가 NOT NULL이고 지원 언어가 두 개뿐이라 KR은 `ko-KR`, 나머지는 `en-US`이다. 여러 시간대가 있는 나라의 대표 시간대는 US `America/New_York`, CA `America/Toronto`, AU `Australia/Sydney`, ID `Asia/Jakarta`로 두었다
  - 메뉴 `route_path`는 `/system/<소문자 코드>`이다. 화면 라우트는 아직 없다. `menu_path`는 PK 경로 대신 코드 경로(`/SYSTEM`, `/SYSTEM/I18N`)이다. 다시 Seed할 때 경로를 고치지 않는다
  - `TENANT.*` 오류는 공용 스키마 오류라 테넌트 Seed에 넣지 않고 `default_message`를 쓴다. `error` 네임스페이스에는 `COMMON.VALIDATION_FAILED`, `COMMON.INTERNAL`만 있다. `AUTH.*`는 아직 없어 만들지 않았다. `common` 네임스페이스는 메시지가 없다
  - `ROLE`/`MENU`/`PERMISSION`/`USER`는 `TENANT_ADMIN`만 가진다. MANAGER와 USER에는 이 메뉴의 READ도 주지 않았다. 페이지 권한은 다섯 기능만 만들고 `APPROVE` 권한 행은 만들지 않았다
  - Seed 실패는 테넌트를 `FAILED`로 두고 `current_step_code=MIGRATED`라 재시도가 `SEEDED`부터 시작한다. 일괄 업그레이드는 Flyway 뒤에 같은 Seed를 실행하고, 실패해도 테넌트 상태는 바꾸지 않은 채 다음 테넌트로 넘어간다
  - `create_user_pk` / `update_user_pk`는 NULL이다

## [6.1단계] Seed 확정, 최초 관리자, 프로비저닝 완료 - 2026-10-10
- 변경/추가 파일:
  - 수정: `build.gradle`, `src/main/resources/application.yaml`, `src/main/resources/db/seed/tenant/S03_code.sql`, `doc/multitenancy-design.md`, `tenant.provision`의 프로비저닝 서비스·결과·저널·임시 비밀번호, `src/test/java/com/tommy/common/TenantProvisioningIntegrationTest.java`, `src/test/java/com/tommy/common/tenant/provision/ProvisioningBehaviorTest.java`, `doc/cursor-progress.md`
  - 추가: `global.security.PasswordHasher`, `BcryptPasswordHasher`, `AdminCreatedProvisioningStep`, `CompletedProvisioningStep`, `TenantAdminWriter`, `TenantAdminPasswordService`, `src/test/java/com/tommy/common/tenant/provision/AdminProvisioningRulesTest.java`
  - 만들지 않음: 재발급 컨트롤러, 메일 발송. 테넌트 DDL V1~V3는 수정하지 않음
- 검증 결과:
  - `gradlew.bat test`: 22건 통과, 실패 0. 임시 비밀번호 길이 16과 문자 종류, `toString` 가림, 정책값 누락·0·소수·문자에서 `TENANT.SECURITY_POLICY_MISSING`, 계정이 있으면 비밀번호를 만들지 않음, 그 예외가 프로비저닝을 `FAILED`로 남기는 것을 확인했다. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*`가 있었다. URL의 데이터베이스 이름은 `pmis`이다. `MDM_FLYWAY_ENABLED=false`. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest --rerun-tasks`: 17건 모두 통과, 실패 0, skip 0. 기존 15건과 관리자 생성 2건이다
    - 전체 프로비저닝은 `ACTIVE` / `COMPLETED` / `SUCCESS`까지 간다. 자사, 사람, 발령, 계정, `INITIAL` 이력, `TENANT_ADMIN` 역할이 각 1건이다
    - 반환된 임시 비밀번호는 BCrypt와 일치하고, 계정·이력·오류 메시지·로그·결과 `toString`에 평문이 없다. `must_change_password_yn='Y'`이고 유효 일수를 3으로 바꾸면 만료가 3일 뒤다
    - 정책 코드를 지우면 `FAILED`이고 현재 단계는 `SEEDED`이다. 회사·사람·계정은 없다. Seed로 정책을 되돌린 뒤 재시도하면 성공한다
    - 같은 `login_id`가 있으면 계정을 더 만들지 않고 새 임시 비밀번호가 없다. 이미 `ACTIVE`이면 `TENANT.ALREADY_ACTIVE`이다
    - 재발급은 비밀번호를 바꾸고 `RESET` 이력을 남기며 잠금을 푼다. 이전 비밀번호는 더 이상 일치하지 않는다. `ACTIVE`가 아니면 `TENANT.INACTIVE`이다
    - 일괄 업그레이드는 `SECURITY_POLICY`와 `ATTACHMENT_CATEGORY` 값을 넣고, 테넌트가 바꾼 이름과 `ext_attr1`은 유지한다
  - 실행 후 `pmis` 업무 테이블 8개는 모두 0행이다. `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: DDL 변경 없음. 확정된 공통코드만 Seed에 추가했다
  - `ATTACHMENT_CATEGORY`: GENERAL(일반), PROFILE(프로필), PHOTO(사진), CONTRACT(계약서)
  - `SECURITY_POLICY`: TEMP_PASSWORD_VALID_DAYS, `ext_attr1` 기본 `7`. 한글 이름 "임시 비밀번호 유효 일수", 영문 "Temporary password validity days"
  - `POSITION`, `JOB_TITLE`은 값이 없다
- 질문/확인 필요: 없음
- 가정한 사항:
  - 6단계에 승인된 가정은 유지한다
  - `company`에 대문자 코드 제약이 없어 `company_code`는 테넌트 코드를 그대로 쓴다
  - 재발급 때 `locked_datetime`도 비운다. 잠금 해제는 상태 `ACTIVE`와 `login_fail_count` 0과 함께 본다
  - 애플리케이션 BCrypt 강도 기본값은 10이다. 통합 테스트는 같은 구현을 강도 4로 호출한다
  - 관리자 생성 뒤에 `COMPLETED`가 실패하면 그 호출은 평문을 돌려주지 않는다. 재시도는 계정이 있으므로 새 비밀번호를 만들지 않고, `ACTIVE`가 된 뒤 재발급으로 다시 받는다
  - 이미 `ACTIVE`인 테넌트에 프로비저닝을 다시 호출하면 `TENANT.ALREADY_ACTIVE`이다. "이미 있는 계정" 분기는 그 전에 같은 `login_id`가 있을 때다

## [7단계] 공통 API 기반과 공통코드 API - 2026-10-10
- 변경/추가 파일:
  - 수정: `src/main/java/com/tommy/common/global/response/ApiExceptionHandler.java`, `src/main/resources/db/seed/tenant/S04_i18n.sql`, `src/test/java/com/tommy/common/TenantProvisioningIntegrationTest.java` (스키마 버전 기대값 4), `doc/cursor-progress.md`
  - 추가: `src/main/resources/db/migration/tenant/V4__partial_unique_common_code_value.sql`, `global.response.PageResponse`, `global.paging.PageRequests`, `global.security`의 `RequirePermission`·`PermissionChecker`·`DenyAllPermissionChecker`·`AllowAllPermissionChecker`·`PermissionInterceptor`·`ApiSecurityConfig`, `code` 패키지의 엔티티·리포지토리·서비스·DTO·컨트롤러, `doc/api/mdm-api.md`, `PageRequestsTest`, `CodeRulesTest`, `CodeApiSliceTest`, `CodeApiIntegrationTest`, `CodeApiForbiddenIntegrationTest`
  - 만들지 않음: i18n API, 재발급 컨트롤러. `spring-boot-starter-aop`는 추가하지 않았다. 테넌트 DDL V1~V3는 수정하지 않음
- 검증 결과:
  - `gradlew.bat test`: 26건 통과, 실패 0. 페이지·정렬 오류, 계층·길이·순환·정책 규칙, MockMvc 래퍼와 `fieldErrors`, `dev` 허용과 기본 프로필 `403 AUTH.FORBIDDEN`을 확인했다. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*`가 있었다. URL의 데이터베이스 이름은 `pmis`이다. `MDM_FLYWAY_ENABLED=false`. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest --rerun-tasks`: 19건 모두 통과, 실패 0, skip 0. 기존 17건과 코드 API 2건이다
    - 테넌트 스키마 V1~V4에 대해 Hibernate `ddl-auto=validate`가 통과했다. 애플리케이션 기동 설정은 `none`이다
    - 조회 이름은 요청 언어, `support_language.fallback_language_code` 체인, `code_name` 순이다. `en-US`는 `ko-KR`로, 지원 언어에 없는 `fr-FR`은 `code_name`으로 떨어진다
    - 두 테넌트의 같은 그룹 기본 `code_name`은 섞이지 않는다
    - 등록·수정·논리 삭제·복구가 `common_code_history`에 `C`/`U`/`D`와 복구 `U`를 남긴다. 논리 삭제한 값은 조회에서 빠지고 같은 `code_value`를 다시 등록할 수 있다
    - 정렬 화이트리스트와 `total`·`page`를 확인했다. `SECURITY_POLICY`/`TEMP_PASSWORD_VALID_DAYS`의 잘못된 값과 삭제는 `CODE.INVALID_POLICY_VALUE`이다
    - 기본 프로필에서 관리 API는 `403 AUTH.FORBIDDEN`이고 조회 API는 200이다
  - 실행 후 `pmis` 업무 테이블 8개는 모두 0행이다. `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: 테넌트 V4가 `common_code`의 전체 유니크 제약 `uk_common_code_code_group_pk_code_value`를 제거하고, 같은 이름의 부분 유니크 인덱스 `(code_group_pk, code_value) WHERE use_yn = 'Y'`를 만든다. 업무 테이블 수는 늘지 않는다. 공용 스키마 `pmis`에는 적용하지 않았다
- 질문/확인 필요: 없음
- 가정한 사항:
  - 6단계·6.1단계에 승인된 가정은 유지한다
  - 논리 삭제한 코드 값을 다시 등록하려면 부분 유니크가 필요하다. 이미 적용된 V1은 고치지 않고 V4로 제약을 인덱스에 바꿨다. 엔티티에는 `@UniqueConstraint`를 두지 않았다
  - `code_value_length`는 최대 길이로 본다. `code_path`는 값 경로이고 최상위는 `/{codeValue}`, 자식은 `{부모 경로}/{codeValue}`이다. 500자를 넘으면 `CODE.VALUE_LENGTH`이다
  - 유효기간의 오늘은 JVM 기본 시간대의 `LocalDate.now()`이다
  - 조회 이름 캐시는 테넌트별 fallback 언어 맵만 둔다. 코드명은 캐시하지 않는다
  - 관리 목록의 `useYn`을 생략하면 사용 중지 행을 포함한다. 여러 그룹 조회에서 하나라도 없거나 사용 중지가 아니면 `404 CODE.GROUP_NOT_FOUND`이다
  - 인증이 없어 HTTP는 모두 `permitAll`이다. 메서드 권한은 AspectJ가 아니라 `HandlerInterceptor`이다. 기본 프로필은 거부, `dev`만 허용한다
  - 오류 응답 문구는 예외의 한글 메시지다. Seed에는 `ko-KR`과 `en-US`를 모두 넣었다
  - `CHAR(1)`인 `use_yn`·`hierarchy_yn`은 문자열 `Y`/`N`과 `SqlTypes.CHAR`로 매핑했다
  - 변경 사용자 PK는 아직 없어 이력의 `change_user_pk`는 NULL이다

## [7.1단계] 다국어 API, 오류 메시지 연결, 유니크 점검 - 2026-10-10
- 변경/추가 파일:
  - 수정: `ApiExceptionHandler`, `CodeLanguageResolver`, `CodeQueryService`, `db/seed/tenant/S04_i18n.sql`, `TenantProvisioningIntegrationTest`(스키마 버전 5), `CodeApiForbiddenIntegrationTest`, `doc/api/mdm-api.md`, `doc/cursor-progress.md`
  - 추가: `db/migration/tenant/V5__partial_unique_fixes.sql`, `global.time.TenantClock`, `i18n` 패키지(엔티티·리포지토리·서비스·컨트롤러·번들/대체 규칙), `BundleErrorLocalizer`, `I18nApiIntegrationTest`, `BundleTextsTest`, `SupportLanguageRulesTest`, `I18nApiSliceTest`
  - 만들지 않음: 새 라이브러리. 테넌트 DDL V1~V4는 수정하지 않음. ERD 파일은 수정하지 않음
- 검증 결과:
  - `gradlew.bat test`: 31건 통과, 실패 0. 번들 우선순위와 순환 끊기, 네임스페이스 개수, 대체 언어 순환과 기본 언어 교체, 언어 배열과 ETag 304, 번들 조회 실패 시 기본 문구를 확인했다. 원격 DB에는 접속하지 않았다
  - 실행 전 확인: `MDM_TEST_DB_*`가 있었다. URL의 데이터베이스 이름은 `pmis`이다. `MDM_FLYWAY_ENABLED=false`. 주소, 계정, 비밀번호는 기록하지 않는다
  - `gradlew.bat integrationTest --rerun-tasks`: 20건 모두 통과, 실패 0, skip 0. 기존 19건과 다국어 API 1건이다
    - 테넌트 스키마 V1~V5에 대해 Hibernate `ddl-auto=validate`가 통과했다. 애플리케이션 기동 설정은 `none`이다
    - 번들 문구는 `ko-KR`, `en-US`, 지원하지 않는 언어의 테넌트 기본 언어, 번역이 없으면 `default_message` 순이다
    - 두 테넌트의 같은 키 문구는 섞이지 않는다. 수정 후 번들에 바로 반영되고 `version_no`는 문구가 바뀔 때만 오른다
    - 활성 기본 언어는 1개다. 대체 언어로 쓰이거나 기본인 언어 중지는 `I18N.LANGUAGE_IN_USE`다. 대체 순환은 `I18N.FALLBACK_CYCLE`이다
    - 논리 삭제한 `message_key`는 다시 등록되고, 사용 중 중복은 `I18N.DUPLICATE`다
    - `lang=en-US` 오류 응답은 영문 시드 문구다. 기본 프로필에서 관리 API는 403이고 언어 조회는 200이다
    - V5로 바꾼 7개 유니크는 사용 중지 뒤 같은 값 재등록이 되고, 사용 중 중복은 거절된다
  - 실행 후 `pmis` 업무 테이블 8개는 모두 0행이다. `it_` / `t_it_` 스키마는 남아 있지 않다. 확인 시점의 `current_database()`도 `pmis`이다
- ERD 반영 내역: 테넌트 V5가 아래 유니크를 `use_yn='Y'` 부분 유니크로 바꾼다. 업무 테이블 수는 늘지 않는다. 공용 스키마 `pmis`에는 적용하지 않았다
- 유니크 점검 (`use_yn`이 있는 테이블의 전체 유니크, 또는 `use_yn`이 빠진 부분 유니크):

  | 테이블 | 이름 | 컬럼 | 판정 |
  | --- | --- | --- | --- |
  | i18n_message | uk_i18n_message_namespace_pk_message_key | namespace_pk, message_key | V5 부분 유니크. 논리 삭제 후 키 재등록 |
  | user_role | uk_user_role_user_pk_role_pk | user_pk, role_pk | V5. 역할 회수 후 다시 부여 |
  | role_permission | uk_role_permission_role_pk_permission_pk | role_pk, permission_pk | V5. 권한 회수 후 다시 부여 |
  | file_attachment | uk_file_attachment_target | target_table_name, target_key, file_pk | V5. 첨부 해제 후 다시 연결 |
  | common_code_external_map | uk_common_code_external_map_code_pk_external_system_code | code_pk, external_system_code | V5. 매핑 해제 후 다시 등록 |
  | person_assignment | uk_person_assignment_employee_no | company_pk, employee_no | V5. 기존 부분 인덱스에 `use_yn='Y'` 추가 |
  | user_account | uk_user_account_person_pk | person_pk | V5. 기존 부분 인덱스에 `use_yn='Y'` 추가 |
  | nation | uk_nation_nation_code | nation_code | 유지. 등록 후 바꿀 수 없는 자연키. 복구로 되돌린다 |
  | i18n_namespace | uk_i18n_namespace_namespace_code | namespace_code | 유지. 같은 이유 |
  | code_group | uk_code_group_code_group_code | code_group_code | 유지. 같은 이유 |
  | file_storage | uk_file_storage_storage_code | storage_code | 유지. 같은 이유 |
  | team | uk_team_team_code | team_code | 유지. 같은 이유 |
  | role | uk_role_role_code | role_code | 유지. 같은 이유 |
  | menu | uk_menu_menu_code | menu_code | 유지. 같은 이유 |
  | permission | uk_permission_menu_pk_action_code, uk_permission_permission_code | menu_pk+action_code, permission_code | 유지. 권한 코드는 역할 연결의 자연키라 재사용하지 않는다 |
  | company | uk_company_company_code | company_code | 유지. 자연키 |
  | dept | uk_dept_company_pk_dept_code | company_pk, dept_code | 유지. 자연키 |
  | user_account | uk_user_account_login_id | login_id | 유지. 논리 삭제 뒤에도 로그인 아이디를 재사용하지 않는다 |
  | i18n_message_text | uk_i18n_message_text_message_pk_support_language_code | message_pk, support_language_code | 유지. 번역은 같은 행을 upsert 한다 |
  | code_group_lang, common_code_lang | 각 언어 UK | 대상+언어 | 유지. `use_yn` 없는 번역 행 |
  | common_code | uk_common_code_code_group_pk_code_value | code_group_pk, code_value | 유지. V4에서 이미 부분 유니크 |
  | support_language, file_storage, team_member, person_assignment.primary | 각 부분 인덱스 | 기본/소속 | 유지. 이미 `use_yn='Y'` 조건이 있다 |
  | file | uk_file_storage_pk_storage_key | storage_pk, storage_key | 유지. 아래 질문 |
  | company | uk_company_business_registration_no | business_registration_no | 유지. `use_yn` 없는 부분 인덱스. 아래 질문 |
  | i18n_entity_text | uk_i18n_entity_text_target | 대상+언어 | 유지. 아래 질문 |
  | user_refresh_token | uk_user_refresh_token_token_hash | token_hash | 대상 아님. `use_yn` 없는 이력성 토큰 |
  | common_code_history 등 | 없음 | | 추가 전용 이력. 변경하지 않음 |

- 질문/확인 필요:
  - `file (storage_pk, storage_key)` 전체 유니크는 논리 삭제 뒤 같은 저장 키를 막는다. 객체가 섞일 수 있어 바꾸지 않았다
  - `company.business_registration_no` 부분 인덱스에 `use_yn='Y'`가 없다. 사업자번호 재사용 여부가 정해지지 않아 바꾸지 않았다
  - `i18n_entity_text`는 `use_yn`이 있는 번역 식별자다. 비활성 행과 활성 행이 같이 있으면 대상을 둘로 보게 되어 바꾸지 않았다
- 가정한 사항:
  - 7단계에 승인된 가정은 유지한다
  - 조회 `GET /api/mdm/v1/nations`는 배열이다. 관리 목록은 같은 경로에 `managed=true`이며 페이지다. 지원 언어 관리 목록은 페이지 없이 배열이다
  - 국가 관리 API도 메뉴 `I18N` 권한을 쓴다
  - 번들 캐시는 테넌트 메모리 맵이고 TTL은 60초다. 언어·네임스페이스·메시지 변경은 그 테넌트 캐시를 바로 지운다. 다른 인스턴스는 TTL 안에 맞춘다
  - 오류 문구는 `error` 네임스페이스에서 찾고, 없거나 테넌트가 없거나 조회가 실패하면 예외 기본 문구를 쓴다
  - 공통코드 유효기간의 오늘은 테넌트 `timezone_id`다. 없거나 올바르지 않으면 시스템 기본 시간대를 한 곳(`TenantClock`)에서 쓴다
  - `message_type_code`는 공통코드로 검사하지 않는다. 네임스페이스 목록의 `keyword`는 코드·이름 부분 일치이고 생략할 수 있다
  - 기본 언어를 바꿀 때는 이전 기본을 먼저 flush 한 뒤 새 기본을 저장한다

## [8단계] 인증 설계 - 2026-10-10
- 변경/추가 파일:
  - 추가: `doc/auth-design.md`
  - 수정: `doc/cursor-progress.md`
  - 바꾸지 않음: 코드, 의존성, DDL, Seed, `.cursor/rules`, `doc/api/mdm-api.md`. API 예시는 설계 문서 안에 두었고 8.1에서 `mdm-api.md`에 옮긴다
- 검증 결과: 설계 문서만 작성했다. 테스트와 DB 확인은 하지 않았다. `company.business_registration_no`의 `use_yn='Y'` 부분 유니크는 V6 안으로만 적었고 SQL 파일은 만들지 않았다
- ERD 반영 내역: 없음. Refresh 테이블은 컬럼 추가 없이 가족 인덱스만 V6 안에 포함했다
- 질문/확인 필요: `doc/auth-design.md`의 결정 필요 목록 9건. 추천안을 문서에 적었다
- 가정한 사항:
  - 7.1단계 유니크 판정은 승인된 대로 유지한다. 사업자번호만 V6에서 `use_yn='Y'`를 추가한다
  - Refresh는 JWT가 아니라 난수이고 SHA-256 해시만 저장한다. Access만 RS256 JWT다
  - 로그인 실패 응답은 원인을 구분하지 않는다. 만료된 비밀번호는 로그인을 막지 않고 `pwd_change_required`로 둔다
  - 플랫폼 운영자 비밀번호 정책은 테넌트 공통코드가 아니라 설정값이다

## [8.1단계] 보안 인프라 - 2026-10-10
- 변경/추가 파일:
  - 수정: `doc/auth-design.md`, `doc/api/mdm-api.md`, `build.gradle`, `src/main/resources/application.yaml`, `src/main/resources/db/seed/tenant/S04_i18n.sql`
  - 추가: `src/main/resources/db/migration/tenant/V6__security_partial_unique_and_refresh_index.sql`
  - 추가: `MdmSecurityProperties`, `MdmSecurityGuard`, `JwtSigningKeys`, `PemKeys`, `JwtTokenService`, `ApiAuthenticationEntryPoint`, `TypedJwtConverter`, `PublicNamespaceFilter`, `AccountStatusCache`, `AccountStatusFilter`, `DbPermissionChecker`, `JwksController`, `SecurityContextTenantClaimReader`
  - 수정: `ApiSecurityConfig`, `TenantResolutionFilter`, `TenantRequestIdentifier`, `TenantAdminWriter`
  - 삭제: `AllowAllPermissionChecker`, `DenyAllPermissionChecker`, `UnavailableTenantClaimReader`
  - 테스트: 기존 API 테스트는 관리자 Access 토큰을 붙인다. `dev`가 아닌 컨텍스트는 임시 PKCS#8로 기동한다. `SecurityInfrastructureTest`, `SecurityApiIntegrationTest` 추가
  - 의존성: `spring-boot-starter-oauth2-resource-server`만 추가. 로컬 캐시에 없어 한 번 받은 뒤 테스트는 `--offline`으로 돌렸다
- 검증 결과: 단위 34 통과, 통합 21 통과(`--rerun-tasks`). `jdbc_db=pmis`, `current_database=pmis`. 공용 `pmis` 업무 테이블 8개 모두 0. `it_`/`t_it_` 잔여 스키마 없음. 로그인·refresh·비밀번호 변경·플랫폼 부트스트랩·공용 V3는 하지 않았다
- ERD 반영 내역: 테넌트 V6. `uk_company_business_registration_no`를 `business_registration_no IS NOT NULL AND use_yn='Y'` 부분 유니크로 다시 만들었다. `ix_user_refresh_token_token_family_id` 추가. 플랫폼 refresh 가족 인덱스는 8.3으로 미뤘다. 스키마 버전 기대값은 `6`. 업무 테이블 수는 31
- 질문/확인 필요: 없음. 8단계 결정 9건은 이 단계 지시에 따라 확정했다
- 가정한 사항:
  - `X-Tenant-Code`는 Authorization이 없을 때만 테넌트가 된다. 토큰이 있으면 헤더나 Host가 다르면 `TENANT.HOST_MISMATCH`다
  - `permit-all`은 `dev`에서만 켤 수 있다. 테스트 프로필에서 켜면 기동을 거부한다. `dev` 밖에서는 키 경로가 없거나 쿠키 Secure가 꺼지면 기동을 거부한다
  - `/codes`와 `/nations`는 인증만 필요하고 권한은 없다. 공개 번들 기본 네임스페이스는 `common`, `error`, `auth`다
  - 계정 상태와 권한은 테넌트 스키마와 `user_pk` 기준 메모리 캐시 TTL 60초다. 비밀번호 재발급은 계정 상태 캐시를 트랜잭션 커밋 뒤에 지운다
  - READ가 없는 다른 기능은 `403 AUTH.FORBIDDEN`이다. 인증이 없으면 `401 AUTH.UNAUTHORIZED`다
  - 보안 정책 숫자 Seed, 로그인, refresh, 비밀번호 변경 강제 필터, 플랫폼 인증은 8.2 또는 8.3이다. `TENANT.*` 문구는 Seed에 넣지 않았다

## [8.2단계] 테넌트 사용자 인증 API와 보안 정책 Seed - 2026-10-10
- 변경/추가 파일:
  - 수정: `doc/auth-design.md`, `doc/api/mdm-api.md`, `src/main/resources/application.yaml`
  - Seed: `S03_code.sql`에 SECURITY_POLICY 8키, `S04_i18n.sql`에 AUTH 오류 문구와 `auth.*` LABEL. 8.1에 있던 AUTH 코드는 중복하지 않았다
  - 추가: `TenantAuthController`, `TenantAuthService`, `LoginLockRules`, `RefreshReuseRules`, `PasswordPolicy`, `RefreshCookies`, `RefreshTokenValues`, `ClientAddresses`, `PasswordChangeRequiredFilter`
  - 수정: `MdmSecurityProperties`(`refresh-reuse-grace-seconds` 기본 10), `CodeRules`, `CommonCodeService`, `ApiException`, `ApiExceptionHandler`, `TenantAdminWriter`(재발급 시 refresh 전체 `ADMIN` 폐기)
  - 테스트: `AuthRulesTest`(5), `AuthApiIntegrationTest`(2)
- 검증 결과: 단위 39 통과, 통합 23 통과(`test integrationTest --offline --no-daemon --rerun-tasks`). 기존 통합 21건 포함. `jdbc_db=pmis`, `current_database=pmis`. 공용 `pmis` 업무 테이블 8개 모두 0. `it_`/`t_it_` 잔여 스키마 없음. 임시 비밀번호 생성기는 길이 10·4종류를 만족한다. 플랫폼 인증, 부트스트랩, menus/my, 공용 V3는 하지 않았다
- ERD 반영 내역: 없음. 테넌트 V6와 업무 테이블 31, 스키마 버전 `6` 유지. 정책 키는 Seed INSERT … NOT EXISTS라 업그레이드가 없는 키만 넣고 바꾼 `ext_attr1`은 덮어쓰지 않는다
- 질문/확인 필요: 없음
- 가정한 사항:
  - refresh 행 잠금 뒤 ROTATED만 유예 대상이다. 유예 안이면 409 AUTH.REFRESH_IN_PROGRESS이고 가족은 유지한다. LOGOUT, PASSWORD_CHANGED, ADMIN은 유예 창 안이어도 REUSE_DETECTED다
  - 로그인·refresh·로그아웃·비밀번호 변경은 `ApiException`이 나도 실패 횟수, 로그인 이력, 가족 폐기를 커밋한다
  - `locked_datetime`과 refresh 시각은 세션 UTC의 `LocalDateTime`으로 읽고 UTC Instant로 비교한다. 잠금 만료 검증은 `locked_datetime`을 과거 절대 시각으로 넣었다
  - 계정이 없으면 더미 BCrypt 비교 후 같은 401 AUTH.INVALID_CREDENTIALS다. 로그인 이력의 `user_pk`는 없을 수 있다
  - 로그아웃은 CSRF가 있으면 쿠키가 없어도 200이다. refresh·로그아웃에 `X-MDM-Request: 1`이 없으면 403 AUTH.CSRF다
  - 비밀번호 변경 강제 필터는 보안 필터 기본 순서 +3이다. 허용은 POST password/change, logout, refresh와 GET me다
  - `X-Forwarded-For`는 `mdm.security.trust-forwarded-headers=true`일 때만 쓰고, 기본은 remoteAddr다. IP는 45자, User-Agent는 500자로 자른다

## [8.3단계] 플랫폼 운영자 인증, 부트스트랩, 테넌트 관리 API, 공용 V3 - 2026-10-11
- 변경/추가 파일:
  - 추가: `db/migration/public/V3__platform_admin_refresh_family_and_password_expire.sql`
  - 추가: `platform` 패키지(`PlatformAuthController`, `PlatformAuthService`, `PlatformTenantController`, `PlatformTenantService`, `PlatformAdmin`, `PlatformAdminBootstrapService`, `PlatformAdminBootstrapRunner`, `PlatformAccountStatus`, `PlatformMessages`, `DomainNames`, `TenantStatusTransitions`, `PlatformBootstrapRules`)
  - 추가: `doc/api/platform-api.md`, `doc/platform-admin-bootstrap.md`
  - 수정: `doc/auth-design.md`, `application.yaml`, `MdmSecurityProperties`, `ApiSecurityConfig`, `TypedJwtConverter`, `AccountStatusFilter`, `PasswordChangeRequiredFilter`, `CommonApplication`, `RefreshCookies`, `TemporaryPassword`, `TenantUpgradeService`
  - 테스트: `PlatformRulesTest`, `PlatformApiIntegrationTest`. `PublicTenantMigrationTest`에 가족 인덱스 확인
- 8.2 인증 테스트 커버리지 표:
  - (a) 최초 로그인 `passwordChangeRequired`와 허용 밖 403: `AuthApiIntegrationTest.signsInRotatesRefreshAndKeepsTenantsApart`
  - (b) 비밀번호 변경, 새 Access/refresh, 이전 refresh `PASSWORD_CHANGED`, 이력 `CHANGE`: 같은 메서드
  - (c) N회 실패 잠금, 잠금 중 올바른 비밀번호 401, 만료 후 `ACTIVE`: 같은 메서드
  - (d) 없는 계정과 틀린 비밀번호의 본문 일치: 같은 메서드
  - (e) refresh 회전, 유예 409, 유예 후 재사용으로 가족 폐기: 같은 메서드
  - (f) 계정 중지 후 refresh 거절: 같은 메서드
  - (g) CSRF 없음 403: 같은 메서드
  - (h) 로그아웃 후 refresh 실패와 쿠키 삭제: 같은 메서드
  - (i) 두 테넌트 분리와 `TENANT.HOST_MISMATCH`: 같은 메서드
  - (j) 재발급 후 이전 비밀번호·refresh 무효: 같은 메서드
  - (k) 로그에 비밀 없음: 같은 메서드
  - (l) 업그레이드가 정책 키를 추가하고 바꾼 값을 유지: `AuthApiIntegrationTest.upgradeAddsPolicyKeysWithoutOverwritingValues`
  - 단위 보강: `AuthRulesTest`가 정책, 잠금 계산, 쿠키, 더미 BCrypt, refresh 유예를 본다. 빠진 (a)~(l) 항목은 없다
- 검증 결과: 단위 44 통과, 통합 26 통과(`test integrationTest --offline --no-daemon --rerun-tasks`). 기존 통합 23건 포함. 적용 전 읽기 전용 확인: `jdbc_db=pmis`, `current_database=pmis`, Flyway V1·V2 success, V3 없음, 업무 테이블 8개 0행, `password_expire_datetime`과 가족 인덱스 없음. 이 프로세스에서만 `MDM_FLYWAY_ENABLED=true`로 기동해 V3를 적용했다. 적용 후 Flyway V3 success, 컬럼과 `ix_platform_admin_refresh_token_token_family_id`가 있다. `dev` 프로필로 다시 기동해 애플리케이션이 시작되는 것을 확인한 뒤 종료했다. 주소, 계정, 비밀번호는 기록하지 않는다. 테스트 후 업무 테이블 8개 0행, `it_`/`t_it_` 잔여 스키마 없음. `current_database=pmis`. 내 메뉴 API는 하지 않았다
- ERD 반영 내역: 공용 V3. `platform_admin.password_expire_datetime` 컬럼과 `platform_admin_refresh_token(token_family_id)` 인덱스. ERD 파일은 수정하지 않았다. 엔티티 `PlatformAdmin`을 `pmis` 스키마에 매핑하고 통합 테스트에서 validate 했다
- 질문/확인 필요: 없음
- 가정한 사항:
  - 부트스트랩 이메일이 없으면 로그인 아이디를 email 컬럼에 넣는다. 이름이 없으면 로그인 아이디를 쓴다
  - 플랫폼 오류 문구는 번들을 조회하지 않고 코드 상수다. `Accept-Language`가 `en`으로 시작하면 영문이다
  - 도메인 논리 삭제는 이름을 지우지 않는다. `domain_name` 전체 유니크라 같은 이름을 다시 등록할 수 없다. primary를 지우면 남은 활성 도메인 중 가장 작은 PK를 primary로 올린다
  - 정렬 쿼리 `field,desc`가 두 값으로 나뉘어 들어오면 다시 합친 뒤 화이트리스트로 검사한다
  - 계정 상태 필터는 `EntityManager`가 준비되기 전에 만들어질 수 있어 의존성을 지연 조회한다. `local` 프로필은 JWT 키 경로가 없으면 기동을 거부한다
  - 일괄 업그레이드는 동기 실행이다. 비동기, 도메인 소유 검증, 테넌트 폐기는 후속 과제다

## [8.4단계] 내 메뉴(menus/my), 공용 V4(도메인 재등록), 공용 유니크 점검 - 2026-10-11
- 변경/추가 파일:
  - 추가: `db/migration/public/V4__tenant_domain_partial_unique.sql`
  - 추가: `menu` 패키지(`MyMenuController`, `MyMenuService`, `MyMenuRules`, `MyMenuItem`, `entity/Menu`)
  - 수정: `DbPermissionChecker`(`actionsByMenu`로 기존 캐시와 READ 규칙을 재사용), `PlatformTenantService`(활성 도메인만 중복으로 센다)
  - 수정: `doc/api/mdm-api.md`, `doc/api/platform-api.md`, `doc/platform-admin-bootstrap.md`, `doc/auth-design.md`
  - 테스트: `MyMenuRulesTest`, `MyMenuApiIntegrationTest`. `PublicTenantMigrationTest`에 부분 유니크와 재등록 확인. `PlatformApiIntegrationTest.reusesADeletedDomainName`
  - 8.2 `AuthApiIntegrationTest`는 한 메서드에 둔 채 기술 부채로만 기록했다. 분리하지 않았다
  - 만들지 않음: 새 라이브러리, 메뉴 관리 API, ETag. 운영자 부트스트랩은 실행하지 않았다. ERD 파일은 수정하지 않았다
- 검증 결과:
  - 적용 전 `gradlew.bat test --offline --no-daemon --rerun-tasks`: 단위 48 통과, 실패 0
  - 적용 전 `gradlew.bat integrationTest --offline --no-daemon --rerun-tasks`: 28건 중 27 통과, 1건은 부분 유니크가 없어 `reusesADeletedDomainName`만 건너뜀. 기존 26건과 내 메뉴 1건은 통과했다. `it_` 스키마의 공용 마이그레이션 테스트가 V4 부분 유니크와 삭제 이름 재등록을 확인했다
  - 적용 전 읽기 전용 확인: `jdbc_db=pmis`, `current_database=pmis`, Flyway V1·V2·V3 success, V4 없음, `domain_partial=false`, 업무 테이블 8개 0행, `it_`/`t_it_` 잔여 스키마 없음
  - 이 프로세스에서만 `MDM_FLYWAY_ENABLED=true`, `dev` 프로필로 기동해 V4를 적용했다. `Migrating schema "pmis" to version "4 - tenant domain partial unique"`, `Successfully applied 1 migration`, 애플리케이션 시작을 확인한 뒤 프로세스를 종료했다. 주소, 계정, 비밀번호는 기록하지 않는다
  - 적용 후 Flyway V1~V4 success, `domain_partial=true`, 업무 테이블 8개 0행, 잔여 스키마 없음
  - 적용 후 `gradlew.bat test integrationTest --offline --no-daemon --rerun-tasks`: 단위 48 통과, 통합 28 통과, 실패 0, skip 0. 도메인 재등록 테스트가 포함된다
  - 재실행 후 `jdbc_db=pmis`, `current_database=pmis`, 업무 테이블 8개 0행, `leftover=NONE`
- ERD 반영 내역: 공용 V4. `uk_tenant_domain_domain_name`을 `use_yn='Y'` 부분 유니크로 바꿨다. 컬럼과 업무 테이블 수는 그대로다. ERD 파일은 수정하지 않았다. `Menu` 엔티티를 테넌트 스키마에 validate 했다
- 공용 유니크 점검 (`use_yn`이 있는 테이블의 전체 유니크, 또는 이미 부분인 유니크):

  | 테이블 | 이름 | 컬럼 | 판정 |
  | --- | --- | --- | --- |
  | tenant | uk_tenant_tenant_code | tenant_code | 유지. 등록 후 바꿀 수 없는 자연키 |
  | tenant | uk_tenant_schema_name | schema_name | 유지. 같은 이유 |
  | tenant_domain | uk_tenant_domain_domain_name | domain_name | V4 부분 유니크 `WHERE use_yn='Y'`. 논리 삭제 후 같은 테넌트나 다른 테넌트에 재등록 |
  | tenant_domain | uk_tenant_domain_primary_yn | tenant_pk | 유지. 이미 `primary_yn='Y' AND use_yn='Y'` 부분 유니크 |
  | platform_admin | uk_platform_admin_login_id | login_id | 유지. 자연키. 논리 삭제 뒤에도 로그인 아이디를 재사용하지 않는다 |
  | platform_admin_refresh_token | uk_platform_admin_refresh_token_token_hash | token_hash | 유지. `use_yn`이 없는 해시라 재사용하지 않는다 |

- 질문/확인 필요: 없음
- 가정한 사항:
  - 8.3의 "도메인 이름은 전체 유니크라 다시 등록할 수 없다"는 이 단계의 부분 유니크로 대체한다. primary를 지우면 남은 활성 도메인 중 가장 작은 PK를 primary로 올리는 규칙은 유지한다
  - `GROUP`은 하위 `PAGE`가 하나라도 남을 때 포함하고, `actions`는 그룹 자체의 권한과 상관없이 `["READ"]`다. `USER` 역할은 `SYSTEM:READ`가 없어도 하위 화면이 있으면 `SYSTEM`이 나온다
  - 메뉴 이름 키는 네임스페이스 `menu`의 `message_key = menu_code`다. 키가 없거나 번들 결과가 비어 있으면 `menu.menu_name`을 쓴다. 키가 있으면 기존 번들 캐시의 대체 체인(번역, 대체 언어, 테넌트 기본 언어, `default_message`)을 쓴다
  - `levelNo`나 `sortOrder`가 없으면 정렬에서 뒤로 보낸다. Seed 값은 비어 있지 않다
  - 위 표 밖에는 애매한 전체 유니크가 없어 질문으로 남기지 않았다

## [8.4.1단계] non-web 부트스트랩에서 서블릿 보안 설정 제외 - 2026-10-11
- 변경/추가 파일:
  - 수정: `CommonApplication`(`MdmSecurityProperties` 등록을 웹 설정에서 분리), `ApiSecurityConfig`, `PasswordChangeRequiredFilter`, `AccountStatusFilter`, `TenantResolutionFilter`, `PermissionInterceptor`, `ApiAuthenticationEntryPoint`
  - 추가: `PlatformAdminBootstrapNonWebTest`
  - 공유 `pmis`에 `--bootstrap-platform-admin`은 실행하지 않았다
- 검증 결과:
  - 원인: `CommonApplication`이 `--bootstrap-platform-admin`이면 `WebApplicationType.NONE`으로 기동한다. Spring Boot `ServletWebSecurityAutoConfiguration`은 `@ConditionalOnWebApplication(type = SERVLET)`라 이 모드에서는 로드되지 않고, 그 설정이 만드는 `HttpSecurity` 빈도 없다. `ApiSecurityConfig.platformSecurityFilterChain(HttpSecurity)`는 컴포넌트 스캔으로 항상 등록되어 파라미터 0에 넣을 빈이 없어 기동이 실패했다
  - `@ConditionalOnWebApplication`으로 서블릿 보안 설정, 필터, 인터셉터, 인증 진입점만 웹 애플리케이션에서 등록한다. `MOCK` 웹 환경의 기존 테스트는 이 조건을 만족한다
  - `MdmSecurityProperties` 등록은 `ApiSecurityConfig`에만 있어서, 그 클래스를 빼면 부트스트랩 서비스가 설정을 받지 못한다. 등록을 `CommonApplication`으로 옮겼다
  - 단위 48 통과, 통합 29 통과(`test integrationTest --offline --no-daemon --rerun-tasks`), 실패 0, skip 0. 기존 통합 28건과 non-web 컨텍스트 1건이다
  - non-web 컨텍스트에서 `DataSource`, `EntityManagerFactory`, `PlatformAdminBootstrapService`, `PasswordHasher`가 있고 `ApiSecurityConfig`, `SecurityFilterChain`, 필터, 인터셉터, 부트스트랩 러너는 없다
  - 계정 생성까지 가진 않았다. `PlatformAdminBootstrapService.createIfEmpty`가 `current_database()`와 `pmis.platform_admin`을 고정하므로 `it_` 스키마로 테이블을 격리할 수 없다. 테스트 스키마에 넣으려면 그 SQL을 바꿔야 해서 컨텍스트 로드만 검증했다
  - `jdbc_db=pmis`, `current_database=pmis`. 업무 테이블 중 `platform_admin`만 1행이고 로그인 아이디는 테스트 접두 `itops`가 아니다. 이 테스트가 넣은 행이 아니라 지우지 않았다. 나머지 7개 업무 테이블은 0행이다. `it_`/`t_it_` 잔여 스키마는 없다. 주소, 계정, 비밀번호는 기록하지 않는다
- ERD 반영 내역: 없음
- 질문/확인 필요: 없음
- 가정한 사항:
  - `@ConditionalOnWebApplication` 기본값(서블릿 또는 리액티브)으로 충분하다. 이 애플리케이션의 웹 기동은 서블릿이다
  - 부트스트랩 SQL을 스키마 파라미터로 바꾸지 않는다. 공유 `pmis.platform_admin` 삽입 테스트는 하지 않는다

## [8.5단계] 로컬 접속 파일을 local 프로필에서만 읽기 - 2026-10-11
- 변경/추가 파일:
  - 수정: `src/main/resources/application.yaml` (`local`은 `dev`를 포함하고, `spring.datasource.*`는 `MDM_DB_*`가 `mdm.db.*`보다 우선). jar 기본 프로필은 두지 않는다
  - 수정: `src/main/resources/application-local.yaml` (`local`일 때만 `optional:file:./config/application-local.yaml`. Flyway 기본은 꺼짐)
  - 추가: `DbConnectionRequirements`, `DatasourceFailureAnalyzer`, `META-INF/spring.factories`
  - 수정: `build.gradle` (`bootRun`은 `SPRING_PROFILES_ACTIVE`가 비어 있을 때만 `local`)
  - 수정: `.gitignore` (`config/application-local.yaml`, `config/*.local.*`)
  - 수정: `config/application-local.yaml.example` (자리표시자만)
  - 추가: `doc/local-setup.md`
  - 수정: `doc/platform-admin-bootstrap.md`, `doc/api/mdm-api.md`
  - 테스트: `LocalConfigTest`. 통합 테스트 5곳은 `MDM_TEST_DB_*`로 데이터소스를 등록한다
  - 만들지 않음: 새 라이브러리, 시크릿 매니저, 값이 든 `config/application-local.yaml`. 통합 테스트 DB 설정은 파일로 옮기지 않았다
- 검증 결과:
  - `gradlew.bat test --offline`: 단위 53 통과 (기존 48, `LocalConfigTest` 5), 실패 0
  - `gradlew.bat integrationTest --offline --no-daemon`: 29 통과, 실패 0, skip 0. 값은 기록하지 않는다
  - 첫 통합 실행은 DB 연결 한도로 프로비저닝 테스트가 끊겼고, 이어서 `ACTIVE` 테넌트 2건이 남아 일괄 업그레이드 테스트가 거절됐다. 코드가 `it-` 16자이고 스키마가 `t_it_` 16자인 행과 스키마만 지운 뒤 다시 실행했다. 그 외 행은 없었다
  - 통과 후 `pmis.tenant` 0행, `it_`/`t_it_` 스키마 0개
  - `config/application-local.yaml`은 없다. 실제 DB로 `bootRun`은 하지 않았다. 비어 있는 설정으로 사용자가 기동한다
  - `git check-ignore`가 `config/application-local.yaml`을 제외한다. `config/application-local.yaml.example`은 제외되지 않는다
  - Flyway는 켜지 않았다. 부트스트랩은 실행하지 않았다
- ERD 반영 내역: 없음
- 질문/확인 필요:
  - `doc/api/platform-api.md`는 비밀번호 길이 기본값을 12로 적고, `application.yaml`과 `MdmSecurityProperties`는 10이다. 이 단계에서는 고치지 않았다
- 가정한 사항:
  - 로컬 파일 키는 `mdm.db.url`, `mdm.db.username`, `mdm.db.password`다. 환경변수 이름은 `MDM_DB_URL`, `MDM_DB_USERNAME`, `MDM_DB_PASSWORD`를 유지하고 `spring.datasource.*`에 묶는다
  - 파일 경로는 작업 디렉터리의 `config/application-local.yaml`이다. `bootRun`의 작업 디렉터리는 `backend/common`다. IDE도 그 디렉터리여야 파일을 찾는다. 저장소 루트에서 실행하면 파일을 못 찾고 환경변수만 쓴다
  - `bootRun`은 환경변수 `SPRING_PROFILES_ACTIVE`가 비어 있을 때만 시스템 프로퍼티로 `local`을 넣는다. `--args`의 프로필이 그 프로퍼티보다 우선한다
  - 비밀번호가 공백이면 접속 설정이 없는 것으로 본다. 오류 문구에는 값을 넣지 않는다
  - Hikari 설정 로그는 WARN이다. 분석기가 잡지 못한 예외의 원인 체인에는 URL이 남을 수 있다
  - Spring Boot 4.1은 `ApplicationContextInitializer`와 `FailureAnalyzer`를 `META-INF/spring.factories`로 읽는다

## [8.6단계] 플랫폼 API 보완, 운영자 비밀번호 12, 통합 테스트 격리 - 2026-10-11
- 변경/추가 파일:
  - 수정: `TenantAdminWriter`, `TenantAdminPasswordService`, `ProvisioningWork`, `ProvisioningResult`, `ReissuedPassword`, `ProvisioningJournal`
  - 수정: `PlatformTenantService`(목록 열, `adminLoginId`), `TenantUpgradeService`(`upgradeTenants`)
  - 수정: `application.yaml`, `MdmSecurityProperties` 플랫폼 비밀번호 최소 길이 12
  - 수정: `doc/api/platform-api.md`
  - 테스트: `PlatformRulesTest`, `PlatformApiIntegrationTest`, `AuthApiIntegrationTest`, `TenantProvisioningIntegrationTest`, `ProvisioningBehaviorTest`, `AdminProvisioningRulesTest`, `TestcontainersConfiguration`
  - 테넌트 공통코드 `SECURITY_POLICY`의 길이 10은 바꾸지 않았다. 부트스트랩은 실행하지 않았다
- 관리자 로그인 아이디:
  - 생성 시 `TenantAdminWriter`가 관리자 이메일의 앞뒤 공백을 빼고 소문자로 만든 값을 `user_account.login_id`에 저장한다
  - 응답 `adminLoginId`는 그 행의 `login_id` 컬럼을 `user_pk`로 다시 읽는다. 계정이 이미 있으면 임시 비밀번호는 null이고, 같은 행의 `login_id`를 넣는다
  - 관리자 단계가 실행되지 않으면 활성 `TENANT_ADMIN` 중 가장 작은 `user_pk`의 `login_id`를 읽는다
- 검증 결과:
  - `gradlew.bat test integrationTest --offline --no-daemon`: 단위 53 통과, 통합 30 통과(기존 29와 비테스트 테넌트 격리 1), 실패 0, skip 0
  - 시작과 종료 모두 `pmis.tenant` 0행, 행 지문 SHA-256 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`(빈 목록). 다른 접두 행은 없었다
  - 연결 한도: 스프링 테스트 컨텍스트가 캐시에 남고 컨텍스트마다 Hikari 기본 10연결을 잡아, 이전 실행에서 `sorry, too many clients already`가 났다. 풀을 2로 줄이면 잠금 연결과 작업 연결이 겹쳐 30초 타임아웃이 났다. 통합 테스트 풀만 5로 두었다
- ERD 반영 내역: 없음
- 질문/확인 필요: 없음
- 가정한 사항:
  - `createDatetime`은 타임존 없는 `timestamp`를 UTC로 보고 ISO-8601로 낸다. `provisioningStatus`는 `provisioning_pk`가 가장 큰 행의 `current_step_code`다. `primaryDomain`은 활성이고 primary인 도메인 하나다
  - `upgradeTenants`는 받은 코드만 업그레이드하고 건너뛰지 않는다. `POST /tenants/upgrade`는 여전히 ACTIVE 전체를 모아 그 메서드의 잠금 안에서 실행하고, 그 외 상태는 `skippedTenantCodes`다
  - 통합 테스트는 `POST /tenants/upgrade`를 호출하지 않는다. 일괄 업그레이드 검증은 `it-` 코드 목록만 `upgradeTenants`에 넘긴다
  - 실제 테넌트처럼 넣는 행은 접두 `zz`와 스키마 `t_zz_`다. 정리는 그 테스트가 만든 행만 한다. `it-` / `t_it_` 잔여 정리도 그 패턴만 지운다
  - `findTenant`는 조회한 테넌트 코드를 `TenantState`에 넣는다. 이전 3인자 생성자는 코드를 null로 두었다

## [git baseline] 현재 상태 GitHub 반영 - 2026-10-18
- 브랜치: `baseline/f4-be8.6` (`origin/main` 기준, 원격 `origin/baseline/f4-be8.6`)
- 커밋 목록:
  - `b4aaba3` chore: 산출물 zip 및 Claude outputs 추적 해제
  - `4df826a` feat(backend): MDM 서버 8.6단계까지 현재 상태 반영
  - `6139616` feat(frontend): F4단계까지 현재 상태 반영
- 추적을 끊은 파일:
  - `Claude outputs/cursor-handoff-prompts.md`
  - `Claude outputs/erd-model-org-user-20261009.json`
  - `Claude outputs/erd-model-pmis-core-20261009.json`
  - `Claude outputs/fe-structure-proposal.md`
  - `common.zip`
  - `frontend.zip`
  - `frontend-backup-F0.zip`
  - `frontend-backup-F0.1.zip`
  - 디스크 파일은 그대로 두었다
- 점검에서 제외한 파일 종류:
  - `backend/common/config/application-local.yaml`
  - `frontend/.env.local`과 `.env.example`, `.env.local.example`을 뺀 `.env*`
  - `node_modules`, `dist`, `build`, `.gradle`, `.idea`, `backend/.idea`, `.umi`
  - 5MB 이상 파일, `*.zip`, `*.jar`(`gradle/wrapper/gradle-wrapper.jar`는 허용), `*.pem`, `*.key`
  - 위 종류는 추가 목록에 없었다. 루트에 남은 기타 파일이 없어 `chore: 기타 루트 파일` 커밋은 만들지 않았다
- 질문/확인 필요: 없음
- 가정한 사항:
  - 개발 스냅샷은 `feature/claude-review-test`의 `f93a1f1`에 이미 커밋되어 있었다. `origin/main`에서 브랜치를 만든 뒤 그 커밋과 공통 조상 사이의 파일 차이만 작업 트리에 적용했다. merge, rebase, reset은 하지 않았다
  - `origin/main`이 그 이후 추가한 파일(`.gitignore`, `AGENTS.md`, `docs`, `.github`)과 개발 커밋이 같은 경로를 고치지 않아 패치가 충돌 없이 적용되었다
  - `jdbc:postgresql://` 뒤가 `.invalid` 또는 `.example`인 테스트 주소, 마스킹 테스트의 `password=` 더미 문자열, JSX `password={...}` 속성은 실제 접속 정보가 아니라고 보았다

## [8.7단계] 메시지 목록 응답에 번역 요약 필드 추가 - 2026-10-19
- 변경/추가 파일:
  - `docs/prompts/be-8.7.md`
  - `MessageResponse`, `I18nMessageService`, `I18nMessageTextRepository`, `MessageTextSlice`
  - `I18nMessageListSummaryTest`, `I18nApiIntegrationTest`
  - `doc/api/mdm-api.md`
- 목록 `GET /api/mdm/v1/i18n-messages` 항목에 `translatedLanguageCodes`, `missingLanguageCodes`, `translationStatusCodes`를 추가했다. `translations`는 빈 배열이다. 단건 조회는 이 세 필드를 내려보내지 않는다
- 페이지의 메시지 PK를 한 번의 IN 조회로 읽고, 활성 언어는 페이지당 한 번만 읽는다. 비활성 언어와 빈 문구는 번역으로 세지 않는다. 상태 코드는 활성 언어 순서로 중복을 제거한다
- 검증 결과:
  - `gradlew.bat test --offline --no-daemon`: 55 통과, 실패 0, skip 0
  - `gradlew.bat integrationTest --offline --no-daemon`: 31 통과, 실패 0, skip 0. 대상 DB 이름은 `pmis`였다
  - 종료 후 `it_`/`t_it_` 스키마 0개, `pmis.tenant` 0행
- ERD 반영 내역: 없음
- 질문/확인 필요: 없음
- 가정한 사항:
  - 목록 쿼리 수가 페이지 크기와 무관함은 저장소 목으로 확인했다. 행마다 `findByMessageMessagePk`를 부르지 않고 슬라이스 조회는 한 번이다
  - 문구가 공백만 있으면 번역이 없는 언어로 본다. 그 행의 상태 코드는 요약에 넣지 않는다
  - `translationStatusCodes`의 순서는 활성 언어의 `sort_order`, `support_language_code` 순서로 처음 나온 상태다
  - 단건·등록·수정·복구 응답의 요약 필드는 null이라 JSON에서 빠진다. 번들과 ETag 코드는 수정하지 않았다

## [9.1단계] 조직·사람 관리 API - 2026-10-19
- 변경/추가 파일:
  - `doc/api/mdm-api.md`
  - `src/main/resources/db/seed/tenant/S04_i18n.sql`
  - `JpaAuditingConfig`, `CurrentTenantUser`, `CodeQueryService`
  - `org` 패키지의 회사·부서·사람·소속 API
  - `I18nEntityText`
  - `OrgRuleTest`, `OrgSliceTest`, `OrgApiIntegrationTest`
- 회사, 부서, 부서 이력, 사람, 사람 소속 API를 추가했다. 계정·역할·파일·팀은 만들지 않았다. 테넌트 DDL은 바꾸지 않았다
- 검증 결과:
  - `gradlew.bat test`: 62 통과, 실패 0, skip 0
  - `gradlew.bat integrationTest`: 36 통과, 실패 0, skip 0. 대상 DB 이름은 `pmis`였다
  - 조직 통합 테스트는 끝난 뒤 `it-`이 아닌 `pmis.tenant` 행 수가 시작과 같음을 확인한다. `t_it_` 스키마는 테스트가 삭제한다
- ERD 반영 내역: 없음. ERD는 수정하지 않았다
- 질문/확인 필요:
  - `company_code`는 대문자·숫자·밑줄·하이픈 1~50자로 받았다. ERD 설명은 길이 50과 유니크만 있고 문자 규칙은 없다
- 가정한 사항:
  - 목록의 `useYn` 생략은 7단계와 같이 사용 중지를 포함한다. `Y`/`N`만 거른다. 부서 트리만 생략 시 오늘 기준 사용 중 부서다
  - 국가가 없거나 사용 중이 아니면 `400 COMMON.VALIDATION_FAILED`와 `fieldErrors(nationCode)`다. `I18N.NATION_NOT_FOUND`는 쓰지 않는다
  - 사원번호 유니크는 V5 기준이다. `use_yn='Y'`이고 값이 있는 행만 유일하고, 종료일은 조건이 아니다
  - 사업자등록번호는 앞뒤 공백만 제거하고 하이픈은 유지한다. 사용 중인 행만 유일하다
  - 복구 권한은 공통코드와 같이 `UPDATE`다
  - 주 소속 요약이 페이지 크기와 무관함은 저장소 목으로 확인했다. SQL 통계는 남기지 않았다
  - 과거 조직도는 재구성하지 않는다

## [9.2단계] 사용자·역할·권한 관리 API - 2026-10-19
- 변경/추가 파일:
  - `doc/api/mdm-api.md`, `doc/auth-design.md`
  - `src/main/resources/db/seed/tenant/S04_i18n.sql`
  - `DbPermissionChecker`, `DeptService`, `PersonAssignmentService`, `OrgExceptions`
  - `access` 패키지의 사용자·역할·권한 API
  - `menu.repository.MenuRepository`
  - `AccessSliceTest`, `AccessApiIntegrationTest`
- 사용자 계정, 사용자 역할, 역할, 역할 권한, 권한 API를 추가했다. 메뉴 CRUD는 만들지 않았다. 테넌트 DDL은 바꾸지 않았다
- 9.1 수정: 같은 상위 부서 코드는 `DEPT.PARENT_INACTIVE`를 다시 보지 않는다. 이미 종료된 소속은 `ASSIGNMENT.ALREADY_ENDED`, 사용 중지가 아니면 `ASSIGNMENT.INACTIVE`다. 부서 트리는 `deptPk` 맵으로 순서를 유지한다
- 검증 결과:
  - `gradlew.bat test`: 71 통과, 실패 0, skip 0
  - `gradlew.bat integrationTest`: 39 통과, 실패 0, skip 0. 대상 DB 이름은 `pmis`였다
  - 접근 통합 테스트는 끝난 뒤 `it-`이 아닌 `pmis.tenant` 행 수가 시작과 같음을 확인한다. `t_it_` 스키마는 테스트가 삭제한다
- ERD 반영 내역: 없음. ERD는 수정하지 않았다
- 질문/확인 필요:
  - 관리자 잠금은 `locked_datetime=지금`이라 `LoginLockRules`가 `LOCK_MINUTES` 뒤에 맞는 비밀번호면 잠금을 푼다. 8.2 로그인 잠금은 바꾸지 않았다
  - `roleCode`는 `^[A-Z][A-Z0-9_]{1,49}$`(2~50자)로 받았다. DB 체크는 `^[A-Z][A-Z0-9_]*$`라 한 글자도 허용한다
- 가정한 사항:
  - `S05`에 `USER`, `ROLE`, `PERMISSION`의 READ/CREATE/UPDATE/DELETE/EXPORT가 있어 권한 행을 추가하지 않았다. 복구는 `UPDATE`다
  - 목록 `useYn` 생략은 사용 중지를 포함한다
  - 사용자 수정에서 `personPk`를 비우면 연결을 해제한다
  - `userCount`는 현재 유효한 부여이면서 사용자 `use_yn='Y'`인 수다
  - 역할·사용자 목록의 집계 횟수는 저장소 목으로 확인했다. SQL 통계는 없다
  - 401/403은 보안 필터가 있는 통합 테스트에서 확인했다. 슬라이스만으로는 필터를 올리지 않는다
  - 비밀번호 해시가 없는 계정은 프로비전에 없어 단위 테스트로 `USER.NO_PASSWORD_ACCOUNT`를 확인했다
  - 권한 캐시 테스트의 사용자 토큰은 `pwd_change_required=false`다. 비밀번호 변경 강제 필터는 토큰 클레임을 본다

## [9.2.1단계] 접속 주소 컨텍스트 조회 - 2026-10-19
- 변경/추가 파일:
  - `doc/api/mdm-api.md`, `doc/auth-design.md`, `doc/local-setup.md`
  - `src/main/resources/application.yaml`, `application-dev.yaml`
  - `AccessContextService`, `AccessContextController`, `PlatformHostFilter`, `PlatformHostProperties`
  - `TenantResolutionFilter`, `ApiSecurityConfig`, `TenantDirectory`, `JdbcTenantDirectory`
  - `AccessContextSliceTest`, `AccessContextIntegrationTest`
- 인증 없이 `GET /api/mdm/v1/context`로 플랫폼 콘솔과 테넌트를 가른다. 토큰 종류와 로그인 엔드포인트는 바꾸지 않았다
- 검증 결과:
  - `gradlew.bat test`: 74 통과, 실패 0, skip 0
  - `gradlew.bat integrationTest`: 40 통과, 실패 0, skip 0. 대상 DB 이름은 `pmis`였다
- ERD 반영 내역: 없음
- 질문/확인 필요:
  - 플랫폼 Host 거절은 새 코드 없이 `404 TENANT.NOT_FOUND`다. 응답 모양은 기존 `ApiResponse`다
- 가정한 사항:
  - `dev`에서 헤더가 가리키는 테넌트가 없으면 `404 TENANT.NOT_FOUND`다. 알 수 없는 Host는 기존 식별과 같이 `400 TENANT.UNRESOLVED`다. 사용 불가 테넌트는 `403 TENANT.INACTIVE`다
  - 설정 호스트는 앞뒤 공백을 제거하고 소문자로 맞추며 숫자 포트는 뺀다
  - `dev` 기본 호스트는 `localhost,127.0.0.1`이다. 비어 있으면 플랫폼 API 호스트 제한을 하지 않는다

