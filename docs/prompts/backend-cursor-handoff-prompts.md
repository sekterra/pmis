# Cursor AI 작업 지시서 (backend/common)

이 파일은 Cursor AI가 읽고 수행하는 작업 지시서이다.
사용자가 "이 파일의 N단계를 수행해줘"라고 요청하면 **해당 단계 하나만** 수행한다. 다음 단계는 사용자가 요청할 때 진행한다.

## 공통 원칙 (모든 단계에 적용)
1. `.cursor/rules`의 규칙을 먼저 읽고 따른다. 특히 `00-project-overview.mdc`의 **개발 환경 제약**(Docker 사용 불가, 원격 개발 DB 공유)을 지킨다.
2. ERD(`doc/erd/*.json`)는 기준 문서이며 수정하지 않는다. ERD와 어긋나거나 모호한 부분은 임의로 결정하지 말고 질문 목록으로 정리한다.
3. 새 라이브러리나 새로운 환경 전제(설치 프로그램, 외부 서비스, 권한)가 필요하면 만들기 전에 사용자에게 먼저 알린다.
4. DB에 접속하거나 쓰는 명령은 실행 전에 대상 DB/스키마를 사용자에게 확인받는다. 접속 정보는 어디에도 기록하지 않는다.
5. 단계가 끝나면 `doc/cursor-progress.md`(없으면 생성)에 아래 형식으로 결과를 **추가**한다.

```
## [N단계] 단계 제목 - YYYY-MM-DD
- 변경/추가 파일: (목록)
- 검증 결과: (실행한 명령과 결과. 실행하지 못한 검증은 이유와 함께 명시)
- ERD 반영 내역: (해당 단계에서만. 표)
- 질문/확인 필요: (목록. 없으면 "없음")
- 가정한 사항: (목록. 없으면 "없음")
```

## 사전 상태 (사용자 준비 완료)
- ERD 파일이 `doc/erd/`에 있다: `erd-model-tenant-*.json`, `erd-model-i18n-common-code-*.json`, `erd-model-org-user-*.json`
- 개발 DB는 원격 PostgreSQL 서버이다(계정은 사용자가 환경변수로 제공). Docker는 사용할 수 없다.
- 개발 DB는 전용 데이터베이스 `pmis`(ICU ko-KR 기본 collation, 1.5단계에서 생성)이고, 그 안의 공용(테넌트 관리) 스키마 이름은 `pmis`이며, 테넌트 스키마는 `t_<테넌트코드>`이다. 계정에는 `CREATE SCHEMA` 권한이 있다.

---

## 1단계: 개발 환경 구성

```
도메인 코드는 만들지 말고 개발 환경만 구성한다.

1. DB 접속 설정
   - src/main/resources/application.yaml(공통)과 application-local.yaml을 구성한다. 접속 정보(URL, 계정, 비밀번호)는 코드/커밋되는 파일에 쓰지 않는다.
   - 접속 정보 전달 방식은 환경변수(예: MDM_DB_URL, MDM_DB_USERNAME, MDM_DB_PASSWORD)를 기본으로 하고, 원하면 .gitignore 처리된 로컬 설정 파일(예: config/application-local.yaml을 spring.config.import로 optional 로드)도 지원하는 안을 제시한다. 어떤 방식이 적절한지 장단점과 함께 추천안을 알려준다.
   - .gitignore에 로컬 비밀 설정 파일을 추가하고, 값 없이 항목만 있는 예시 파일(*.example)을 만든다.
2. Flyway 설정
   - spring.flyway.locations=classpath:db/migration/public
   - spring.flyway.schemas=pmis, spring.flyway.default-schema=pmis (공용 스키마 이름은 pmis. 스키마 생성은 1.5단계 스크립트로 하며 create-schemas는 사용하지 않는다)
   - spring.flyway.clean-disabled=true
   - 앱 기동 시 공유 개발 DB가 의도치 않게 변경되지 않도록, 기본 로컬 설정에서는 Flyway 자동 실행이 어떻게 동작하는지 확인하고 안전한 기본값을 제안한다(예: 환경변수로 명시적으로 켜야만 실행). 선택지와 추천안을 알려준다.
   - JPA: ddl-auto=none, hibernate.jdbc.time_zone=UTC, open-in-view=false
3. 폴더/패키지 뼈대
   - src/main/resources/db/migration/ 아래에 public/, tenant/ 폴더 생성(.gitkeep 사용, 기존 빈 루트 폴더 정리)
   - 패키지 뼈대: global(config, exception, response, audit, tenant), tenant, i18n, code. 빈 package-info.java 정도만 두고 클래스는 만들지 않는다.
4. 감사 기반 클래스
   - JPA Auditing 설정과 BaseEntity(@MappedSuperclass; create_datetime, create_user_pk, update_datetime, update_user_pk)
   - AuditorAware는 사용자 체계가 없으므로 null을 반환하는 임시 구현으로 하고 TODO로 표시
5. 테스트 구성 (Docker 없이)
   - 프로젝트 생성 시 포함된 TestcontainersConfiguration, TestCommonApplication, CommonApplicationTests는 Docker가 있어야 동작한다. 35-testing.mdc에 따라 기본 test 태스크가 Docker/DB 없이 통과하도록 조정하고(삭제하지 말고 통합 테스트로 분리하거나 비활성화), 어떻게 바꿨는지 보고한다.
   - build.gradle에 integrationTest 태스크를 추가하고(@Tag("integration")), test 태스크는 해당 태그를 제외한다.
6. 검증: Docker/DB 없이 ./gradlew build (Windows: gradlew.bat build)가 통과하는지 확인한다. DB에 접속하는 검증은 하지 않는다.

springdoc-openapi 등 새 라이브러리는 추가하지 말고 필요하면 Spring Boot 4 호환 여부를 조사해 사용자에게 먼저 묻는다.
공통 원칙 5번의 형식으로 doc/cursor-progress.md에 결과를 기록한다.
```

---

## 1.5단계: 전용 DB(pmis)와 공용 스키마(pmis) 생성 스크립트 (한글 정렬 포함)

사용자가 점검 SQL(00_inspect.sql)을 실행했고 결과를 바탕으로 아래가 **확정**되었다. 다른 방식을 제안하지 말고 이 결정대로 작업한다.
- 서버: PostgreSQL 16.11, UTF8. 기존 DB `postgres`는 en_US.utf8(libc) 기본 collation이라 한글 정렬이 맞지 않는다. ICU와 `ko-KR-x-icu`는 사용 가능하다.
- 결정 1: **전용 데이터베이스 `pmis`를 새로 만들고, ICU `ko-KR`을 DB 기본 collation으로 지정**한다. 기존 `postgres` DB는 건드리지 않는다. 이렇게 하면 모든 문자열 컬럼과 모든 테넌트 스키마가 한글 정렬을 자동으로 따르므로 컬럼별 COLLATE 지정이나 CREATE COLLATION 이 필요 없다.
- 결정 2: 정렬 순서는 ICU 기본(숫자, 한글(초성 포함), 영문). 점검 결과 `1, 2, ㄱ, 가, ㄴ, 나, 다, 하, a, A, B` 순이다.
- 결정 3: 새 DB 안에 공용 스키마 `pmis`를 만든다(DB 이름과 스키마 이름이 모두 pmis).
- 접속 계정은 슈퍼유저이며 DB 생성 권한이 있다. 이 계정은 개발 편의용이므로 코드에 슈퍼유저 전용 기능을 의존하지 않는다.

```
doc/db/ 에 아래 스크립트와 안내를 작성한다. 이 스크립트들은 Flyway 경로(db/migration)에 두지 않는다. 실행은 사용자가 DBeaver 등으로 직접 한다(Cursor 환경에서는 DB에 접속하지 않는다).

1. doc/db/01_create_database_pmis.sql  (기존 postgres DB에 접속한 상태에서 실행)
   - 먼저 실행할 확인 쿼리: pmis 데이터베이스가 이미 있는지 조회(pg_database). 있으면 실행하지 말라는 주석.
   - CREATE DATABASE pmis TEMPLATE template0 ENCODING 'UTF8' LOCALE_PROVIDER icu ICU_LOCALE 'ko-KR' LC_COLLATE 'en_US.utf8' LC_CTYPE 'en_US.utf8';
   - PostgreSQL 에는 CREATE DATABASE IF NOT EXISTS 가 없으므로 위처럼 존재 확인 쿼리 + 주석으로 안전장치를 둔다.
   - CREATE DATABASE 는 트랜잭션 블록 안에서 실행할 수 없다는 주의(DBeaver 자동 커밋 모드)를 주석으로 적는다.
2. doc/db/02_create_schema_pmis.sql  (새로 만든 pmis DB에 접속한 상태에서 실행)
   - CREATE SCHEMA IF NOT EXISTS pmis;
3. doc/db/03_verify_collation.sql  (pmis DB에 접속한 상태에서 실행, 읽기 전용)
   - pg_database 에서 datlocprovider='i', daticulocale='ko-KR' 인지 확인하는 쿼리 (PostgreSQL 16 의 컬럼명으로 작성)
   - current_database(), 스키마 pmis 존재 확인
   - COLLATE 절을 쓰지 않은 상태에서 ('가','나','다','하','ㄱ','ㄴ','A','a','B','1','2') 를 ORDER BY 했을 때 기대 결과를 주석으로 적는다: 1, 2, ㄱ, 가, ㄴ, 나, 다, 하, a, A, B
4. doc/db/README.md
   - 실행 순서(01 은 postgres DB 접속, 02/03 은 pmis DB 접속), 각 단계 확인 방법, 이미 실행했을 때의 동작, 롤백이 필요하면 사용자가 직접 DROP DATABASE 해야 한다는 안내(자동화하지 않음)
5. 설정 파일(config/application-local.yaml.example 등)에서 접속 URL 예시의 DB 이름이 pmis 가 되도록 수정한다. 값(주소, 계정, 비밀번호)은 적지 않는다.
6. 이 단계에서는 DB에 접속하거나 DB를 변경하지 않는다. 스크립트 문법과 주석, README 의 정확성을 점검한다.
7. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 기록한다. 질문 목록에는 사용자가 스크립트를 실행한 뒤 알려줘야 할 확인 항목을 적는다.
```

---

## 1.6단계: 초기화 스크립트 보강 (잘못된 DB에서 실행 방지)

배경: 사용자가 02, 03 스크립트를 `pmis` DB가 아닌 `postgres` DB에 접속한 상태에서 실행해, `postgres` DB에 스키마 pmis가 잘못 만들어졌다(사용자가 정리 중). 같은 실수를 막는다.

```
doc/db/ 의 스크립트를 보강한다. DB 에는 접속하지 않는다.

1. 02_create_schema_pmis.sql
   - 접속 DB가 pmis 가 아니면 오류를 내고 스키마를 만들지 않도록, 단일 DO 블록 하나로 작성한다.
     예) DO $$ BEGIN IF current_database() <> 'pmis' THEN RAISE EXCEPTION '접속 DB가 pmis 가 아닙니다: %', current_database(); END IF; EXECUTE 'CREATE SCHEMA IF NOT EXISTS pmis'; END $$;
   - DBeaver 스크립트 실행에서 한 문장씩 분리되어 가드가 우회되지 않도록 반드시 한 문장으로 작성한다.
2. 03_verify_collation.sql
   - 맨 앞에 현재 접속 DB 확인 쿼리를 둔다: current_database() 와 pmis 일치 여부를 'OK' 또는 '접속 DB가 pmis 가 아님'으로 보여준다.
   - 정렬 확인 쿼리 앞에도 "접속 DB가 pmis 일 때만 의미가 있다"는 주석을 적는다.
3. 01_create_database_pmis.sql 은 postgres DB 접속 상태에서 실행하는 스크립트임을 파일 맨 위 주석에 크게 표시한다(현재 주석 유지 + 강조).
4. README.md
   - "DBeaver 에서 DB 전환" 방법과, 실행 전에 SELECT current_database(); 로 접속 DB를 먼저 확인하라는 단계를 02/03 앞에 넣는다.
   - postgres DB 에 실수로 pmis 스키마를 만든 경우의 정리 방법을 "문제 해결"로 추가한다: 해당 DB 에서 스키마가 비어 있는지 information_schema.tables 로 확인한 뒤 DROP SCHEMA pmis; (CASCADE 사용 금지).
5. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 기록한다.
```

---

## 2단계: 공용 스키마 Flyway V1 (테넌트 관리)

```
doc/erd/erd-model-tenant-*.json 을 기준으로 db/migration/public/V1__create_tenant_tables.sql 을 작성한다.
- 40-erd-source-of-truth.mdc, 10-database-conventions.mdc 규칙을 따른다.
- 테이블 5개(tenant, tenant_domain, tenant_status_history, tenant_provisioning, tenant_schema_migration), 시퀀스(<테이블>_seq), PK/FK/UK/CHECK 제약, 인덱스를 포함한다.
- ERD description에 적힌 UK, 기본값, 허용값을 빠짐없이 반영하고 반영 내역을 표로 정리한다.
- ERD와 어긋나거나 모호한 부분은 임의로 결정하지 말고 질문 목록으로 정리한다.
- 먼저 기존 CommonApplicationTests / TestcontainersConfiguration / TestCommonApplication 이 Docker 없이 integrationTest 에서 실패하지 않도록, 원격 DB 환경변수(MDM_TEST_DB_*) 기반으로 전환하고 환경변수가 없으면 skip 되게 한다(35-testing.mdc). 변경 내용을 보고한다.
- 통합 테스트(@Tag("integration"))를 작성한다: 빈 스키마에 마이그레이션이 적용되고 ERD의 테이블/컬럼명과 일치하는지 검증. 원격 DB에 접속하는 테스트는 실행하지 말고, 작성만 한 뒤 실행 방법과 필요한 환경변수를 알려준다(실행은 사용자 확인 후).
- 공통 원칙 5번의 형식으로 doc/cursor-progress.md에 기록한다.
```

---

## 3단계: 테넌트 스키마 Flyway V1 (다국어, 공통코드)

```
doc/erd/erd-model-i18n-common-code-*.json 을 기준으로 db/migration/tenant/V1__create_i18n_and_code_tables.sql 을 작성한다.
- 테이블 12개(support_language, nation, i18n_namespace, i18n_message, i18n_message_text, i18n_entity_text, code_group, code_group_lang, common_code, common_code_lang, common_code_external_map, common_code_history).
- SQL에 스키마명을 쓰지 않는다(비한정 이름). 시퀀스도 같은 스크립트에서 생성한다.
- support_language는 support_language_code(VARCHAR)가 PK인 예외임에 주의한다.
- 자기참조 FK(support_language.fallback_language_code, common_code.parent_code_pk)를 포함한다.
- 2단계와 같은 방식으로 ERD 반영 내역 표, 질문 목록, 통합 테스트(작성만, 실행은 사용자 확인 후)를 제공한다.
- 이 단계에서는 Seed 데이터를 넣지 않는다.
- 공통 원칙 5번의 형식으로 doc/cursor-progress.md에 기록한다.
```

---

## 3.1단계: 2·3단계 질문 확정사항 반영 (DDL 보완)

```
doc/cursor-progress.md 의 [2단계], [3단계] "질문/확인 필요"에 대한 설계 담당의 결정을 반영한다. 아직 어떤 환경에도 적용되지 않은 V1 파일이므로 V1을 직접 수정한다(이미 원격 DB에 적용한 적이 있으면 수정하지 말고 알려준다).

[공용 스키마 V1 (tenant)]
- tenant_domain.domain_type_code: 컬럼 DEFAULT를 두지 않는다(현행 유지, 호출 측이 명시).
- tenant_domain.primary_yn 부분 유니크 인덱스: 조건을 WHERE primary_yn='Y' AND use_yn='Y' 로 바꾼다(논리삭제된 행이 기본 도메인 지정을 막지 않게).
- tenant_status_history.from_status_code(NULL 허용), to_status_code: tenant.tenant_status_code 와 같은 허용값 CHECK를 둔다.
- tenant.schema_name: CHECK ^t_[a-z0-9_]{1,61}$ 를 둔다.
- tenant.tenant_code: 패턴을 ^[a-z0-9]+(-[a-z0-9]+)*$ 로 바꾼다(앞뒤/연속 하이픈 불가).
- tenant_code, schema_name 생성 후 변경 불가: DB 트리거는 만들지 않는다. 엔티티에서 updatable=false 와 도메인 메서드 부재로 막고, 통합 테스트로 확인한다.

[테넌트 스키마 V1 (다국어/공통코드)]
- support_language.default_yn 부분 유니크 인덱스: WHERE default_yn='Y' AND use_yn='Y' 로 바꾼다. "기본 언어는 use_yn='Y'여야 한다"는 서비스 검증으로 둔다.
- message_type_code, translation_status_code: CHECK를 두지 않는다(공통코드 컬럼 원칙 유지, 애플리케이션 검증).
- code_group.code_group_code: CHECK ^[A-Z][A-Z0-9_]*$ 를 둔다.
- nation.nation_code: CHECK ^[A-Z]{2}$ (ISO 3166-1 alpha-2).
- i18n_entity_text UK 이름은 현행(uk_i18n_entity_text_target) 유지.
- common_code.parent_code_pk 와 code_group_pk 일치: DB 제약은 만들지 않고 서비스 검증 + 통합 테스트로 확인한다.
- valid_from_date <= valid_to_date CHECK를 둔다(둘 다 NULL 허용, 둘 다 있을 때만 비교). 5단계의 dept.valid_from_date/valid_to_date, person_assignment.start_date/end_date 에도 같은 CHECK를 적용한다.

[통합 테스트]
- 사용자가 터미널에 MDM_TEST_DB_URL(DB 이름 pmis), MDM_TEST_DB_USERNAME, MDM_TEST_DB_PASSWORD 를 직접 설정하고 gradlew.bat integrationTest 를 실행한다. 너는 접속 정보를 요구하거나 파일/채팅에 남기지 않는다. 실행 전 대상 DB가 pmis 인지(current_database()) 테스트 코드가 확인하게 하고, 테스트가 만든 it_ 스키마만 삭제하며 스키마 pmis 는 변경하지 않는다.
- 통합 테스트 결과는 사용자가 알려주면 progress에 기록한다.

공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [3.1단계] 로 기록한다. 완료 후 멈춘다(다음 단계 자동 진행 금지).
```

---

## 3.2단계: 통합 테스트 실행 (원격 DB pmis)

```
2·3단계 마이그레이션 통합 테스트를 실행하고 결과를 기록한다. 코드는 수정하지 않는다(실패 원인 분석까지만 하고, 수정은 사용자 승인 후).

[접속 정보 처리 - 비밀 보호]
- 접속 정보(주소, 계정, 비밀번호)를 채팅, 로그, doc/cursor-progress.md, 커밋 어디에도 출력/기록하지 않는다. 환경변수 값을 echo/Write-Host 하지 않는다.
- 환경변수 MDM_TEST_DB_URL, MDM_TEST_DB_USERNAME, MDM_TEST_DB_PASSWORD 가 이미 있으면 그대로 사용한다.
- 없으면 MDM_DB_URL, MDM_DB_USERNAME, MDM_DB_PASSWORD 가 있는지 확인(존재 여부만 확인, 값은 출력 금지)하고, 있으면 테스트를 실행하는 같은 터미널 프로세스 안에서만 MDM_TEST_DB_* 로 복사해 사용한다(영구 설정 금지). 예(PowerShell): $env:MDM_TEST_DB_URL=$env:MDM_DB_URL 등.
- 둘 다 없으면 실행하지 말고, 사용자가 터미널에 환경변수를 설정해야 한다고 알려주고 멈춘다.

[실행 전 확인]
- 접속 URL의 데이터베이스 이름이 pmis 인지 URL 문자열에서 확인한다(값 전체를 출력하지 말고 DB 이름이 pmis 인지 여부만 보고한다). pmis 가 아니면 실행하지 않고 멈춘다.
- 테스트가 current_database() 가 pmis 인지 검증하고, 스키마 pmis 와 기존 스키마를 변경/삭제하지 않으며, 자신이 만든 it_ / t_it_ 스키마만 삭제하는지 테스트 코드를 읽어 확인한다. 어긋나면 실행하지 말고 알려준다.

[실행]
- gradlew.bat integrationTest 를 실행한다(사용자가 터미널 명령 실행을 승인해야 한다).
- 결과(통과/실패 테스트 수, 실패 시 오류 메시지와 원인 분석)를 doc/cursor-progress.md 에 [3.2단계] 로 기록한다. 오류 메시지에 접속 정보가 섞여 있으면 마스킹한다.
- 실행 후 DB 에 it_ / t_it_ 로 시작하는 스키마가 남지 않았는지 확인하고 결과를 기록한다. 남아 있으면 이름만 알려주고 삭제는 사용자 승인 후에 한다.
- 완료 후 멈춘다(다음 단계 자동 진행 금지).
```

---

## 3.3단계: CommonApplicationTests DB 검증 보완 후 통합 테스트 재실행

```
3.2단계 [질문/확인 필요]에 대한 결정: CommonApplicationTests 수정을 승인한다.
- CommonApplicationTests 도 다른 통합 테스트와 같이, 컨텍스트 로딩 전에(또는 직접 연결로) current_database() 가 pmis 인지 확인하고 아니면 실행하지 않도록 보완한다. 읽기 전용 확인만 하고 스키마/데이터는 변경하지 않는다. Flyway 는 계속 끈다.
- 보완 후 gradlew.bat test 로 단위 테스트가 여전히 통과하는지 확인한다.
- 이어서 3.2단계 절차(접속 정보 비밀 보호, 실행 전 확인, gradlew.bat integrationTest 실행, 결과 기록, it_ / t_it_ 스키마 잔존 확인)를 그대로 수행한다. 환경변수가 없으면 실행하지 말고 사용자에게 설정을 요청하고 멈춘다.
- doc/cursor-progress.md 에 [3.3단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 3.4단계: 통합 테스트 실패 수정 (currval 사용 제거) 후 재실행

```
3.3단계 [질문/확인 필요]에 대한 결정: 승인한다. DDL 문제가 아니라 테스트 코드 문제이므로 마이그레이션 SQL 은 수정하지 않는다.
- PublicTenantMigrationTest, TenantI18nCodeMigrationTest(및 같은 패턴의 다른 테스트)에서 currval('..._seq') 로 PK 를 얻는 부분을 모두 바꾼다. 부모 행을 성공적으로 INSERT 할 때 INSERT ... RETURNING <pk> 로 PK 를 받아 변수에 저장하고, 이후 자식 INSERT 에 그 값을 쓴다. (거절되는 INSERT 도 시퀀스를 소모하므로 currval 은 신뢰할 수 없다.)
- 거절을 기대하는 INSERT 는 SAVEPOINT 또는 별도 트랜잭션으로 감싸 다음 검증에 영향이 없게 한다(이미 그렇게 되어 있으면 유지).
- 수정 후 gradlew.bat test, 이어서 gradlew.bat integrationTest 를 3.2단계 절차(접속 정보 비밀 보호, 실행 전 DB 이름 확인, it_ 스키마 잔존 확인)대로 실행한다. 환경변수가 없으면 사용자에게 설정을 요청하고 멈춘다.
- 4건 모두 통과해야 한다. 다른 실패가 나오면 원인 분석만 기록하고 멈춘다(DDL 수정은 승인 후).
- doc/cursor-progress.md 에 [3.4단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 4단계: 멀티테넌시 인프라 (먼저 설계안만 제시)

```
20-flyway-multitenancy.mdc 의 규칙을 기준으로 스키마 분리 멀티테넌시 인프라를 구현할 것이다.
먼저 코드를 쓰지 말고 설계안만 doc/multitenancy-design.md 에 작성한다.
- Hibernate 멀티테넌시(SCHEMA)에서 CurrentTenantIdentifierResolver / MultiTenantConnectionProvider 구성 방식과 Spring Boot 4 / Hibernate 7 에서의 설정 방법
- TenantResolver 인터페이스 설계 (식별 방식 Host vs JWT는 미확정이므로 두 방식 모두 붙일 수 있게)
- 커넥션 반납 시 search_path 초기화 방법과 커넥션 풀 오염 방지
- 스키마명 검증 정책
- 앱 기동 시 접속 DB 이름 검증(current_database() 가 설정한 기대 값 pmis 와 다르면 Flyway 실행과 프로비저닝을 거부)
- 테넌트 프로비저닝 서비스의 단계별 흐름과 실패 시 재시도 방법
- 모든 테넌트 일괄 업그레이드 방식
- 공유 원격 DB에서 안전하게 개발/테스트하는 방법(스키마 접두사, 권한 요구사항: CREATE SCHEMA 등)
- 테스트 전략(단위/통합 구분, 테넌트 격리 테스트)
- 패키지 구조: 테넌트 도메인(tenant: 테넌트 CRUD/프로비저닝)과 멀티테넌시 인프라(global.tenant: 해석, 커넥션 제공)의 패키지 이름이 겹쳐 혼동되므로, 이름과 의존 방향을 정리한 안을 제시한다.
- Flyway 이력 테이블: 공용 스키마(pmis)와 각 테넌트 스키마의 이력 테이블 이름/위치, 공용 V1 과 테넌트 V1 의 버전 번호가 겹치는 문제(5.6단계 AllMigrationTest 참고)를 실제 운영에서 어떻게 분리하는지. 같은 테넌트 스키마에 나중에 PMIS 서버(별도 저장소)가 자신의 테이블을 마이그레이션하므로, 서버별로 이력 테이블 이름을 분리하는 규칙(예: MDM 은 flyway_schema_history, PMIS 는 별도 이름)도 제안한다.
- 서버 간 호출: PMIS 서버가 MDM 서버를 호출하거나 같은 테넌트 스키마를 직접 사용할 때 테넌트를 어떻게 식별/전달하는지(Host, JWT 클레임, 헤더). 인증 토큰 공유 방식 선택지를 비교한다.
- 프로비저닝 단계: 스키마 생성 -> 마이그레이션 -> Seed(공통코드, 지원언어, 기본 역할/메뉴/권한은 6단계에서 정의) -> 최초 관리자 생성 -> 완료. 각 단계의 멱등성(재시도 시 중복 생성 방지)과 실패 후 정리(스키마 삭제 정책: 자동 삭제 금지, 사용자 승인 후)를 명시한다.
- 앱 기동 시 동작: 공용 스키마 마이그레이션 자동 실행 여부(MDM_FLYWAY_ENABLED)와 테넌트 스키마 마이그레이션 시점(프로비저닝 시, 업그레이드 배치 시)을 구분해 제안한다.
선택지가 있으면 트레이드오프를 비교하고 추천안을 표시한다. 사용자가 승인한 뒤에 구현한다. 결정이 필요한 항목은 doc/multitenancy-design.md 맨 위에 "결정 필요 목록"으로 모아 번호를 붙인다.
공통 원칙 5번의 형식으로 doc/cursor-progress.md에 기록한다. 완료 후 멈춘다.
```

---

## 4.1단계: 멀티테넌시 설계 확정 반영 및 인프라 구현 (프로비저닝 제외)

```
doc/multitenancy-design.md 의 [결정 필요 목록]에 대한 설계 담당(사용자 승인)의 결정:
1. 테넌트 식별: Host(도메인) + 검증된 JWT 클레임. 로그인 요청은 토큰이 없으므로 Host 로 식별한다. 로그인 후에는 JWT 의 테넌트 클레임과 Host 의 테넌트가 일치해야 한다. 서버 간 호출은 JWT 클레임. 로컬 개발용으로 dev 프로필에서만 켜지는 헤더 리졸버(X-Tenant-Code)를 둔다(운영 프로필에서는 등록되지 않게 한다).
2. 서버 간 인증 방향: MDM 이 JWT 를 발급하고 PMIS 등은 JWKS 로 검증한다. 구현은 별도 인증 설계 단계에서 하며, 지금은 JWT 리졸버를 "검증된 토큰에서 테넌트 클레임을 읽는 인터페이스 구현" 수준까지만 만든다(라이브러리/클레임 이름 확정 전이므로 실제 토큰 파싱은 만들지 않거나 인터페이스 뒤 스텁으로 둔다).
3. 인프라 패키지: global.multitenancy (global.tenant 사용 안 함). 도메인 패키지 tenant 는 유지한다. 의존 방향은 설계 문서 표대로 global.multitenancy 가 tenant 를 임포트하지 않는다.
4. SessionFactory 1개, 공용 엔티티만 @Table(schema = "pmis"). 5. 일괄 업그레이드는 기동 시 자동 실행하지 않는다. 6. 프로비저닝 실패 스키마를 자동 삭제하지 않는다. 7. PMIS Flyway 이력 테이블은 flyway_schema_history_pmis.
8. 테넌트 관리 API 는 공용 스키마의 플랫폼 운영자 계정(platform_admin)으로 인증한다(스키마는 4.2단계에서 추가, 인증 구현은 인증 설계 단계).

[설계 문서 보완 - 먼저 doc/multitenancy-design.md 를 수정한다]
- 결정 목록을 위 결정으로 갱신하고 "결정 필요 목록"을 "확정 사항"으로 바꾼다.
- Hibernate 설정 키 이름을 확인해 바로잡는다. CurrentTenantIdentifierResolver 의 설정 키는 문서의 hibernate.multi_tenant_identifier_resolver 가 아니라 AvailableSettings 상수(MULTI_TENANT_IDENTIFIER_RESOLVER, MULTI_TENANT_CONNECTION_PROVIDER)로 지정한다. 문자열 대신 상수를 쓴다. Spring Boot 4 가 두 빈을 자동 등록하는지 직접 확인하고, 아니면 HibernatePropertiesCustomizer 로 등록한다.
- 로그인 흐름을 추가한다: 로그인 요청의 테넌트는 Host 로 식별하고, 로그인 성공 시 발급 토큰에 테넌트 클레임을 넣는다(발급은 인증 설계 단계).
- 동시성 보완: 같은 테넌트의 프로비저닝/업그레이드가 동시에 실행되지 않게 하는 방법(tenant_provisioning 행 잠금 또는 pg_advisory_lock)과 여러 인스턴스 환경에서의 일괄 업그레이드 중복 실행 방지를 추가한다.
- ddl-auto 보완: 앱 기동 시 SessionFactory 는 공용 스키마(search_path=pmis) 기준이라 테넌트 테이블 validate 가 실패할 수 있다. 앱 기동 validate 는 끄고(ddl-auto=none), 엔티티-DDL 일치 검증은 테넌트 스키마를 적용한 통합 테스트에서 하는 방법을 정리한다(30-java-jpa-conventions.mdc 의 이름 매핑 규칙 참고).
- 규칙 파일(.cursor/rules)은 수정하지 않는다(설계 담당이 이미 갱신함).

[구현 범위 - 설계 문서 수정 후 구현]
- global.multitenancy: 테넌트 컨텍스트(ThreadLocal, 요청 종료 시 정리), 스키마명 검증기(정규식 + 예약어 거부), TenantResolver 인터페이스와 HostTenantResolver, 개발용 헤더 리졸버(dev 프로필만), JwtTenantResolver 는 인터페이스 뒤 스텁, TenantDirectory 인터페이스(구현은 tenant 도메인), CurrentTenantIdentifierResolver, MultiTenantConnectionProvider(빌릴 때 검증 후 SET search_path, 반납 finally 에서 RESET, 실패 시 커넥션 폐기), 접속 DB 이름 검사(current_database() != pmis 면 기동/Flyway/프로비저닝 거부), 요청 필터(테넌트 식별 실패/상태 불가 시 세션 열기 전에 거절하고 공통 응답 형식 오류 코드 사용).
- tenant 도메인: TenantDirectory 구현(pmis.tenant, tenant_domain 조회, 짧은 캐시, 상태 변경 시 무효화)은 만들되, 프로비저닝/업그레이드 서비스와 테넌트 관리 API 는 만들지 않는다(4.3단계 이후).
- 공통 오류 응답: global.response / global.exception 에 래퍼와 @RestControllerAdvice 를 30-java-jpa-conventions.mdc 의 확정 규약대로 구현한다(문자열 오류 코드, errorMessage 는 i18n 키 기반이며 i18n 조회 구현 전이므로 기본 메시지로 대체).
- 테스트: 단위 테스트(스키마명 거부, 예약어 거부, 리졸버, 컨텍스트 정리, DB 이름 불일치 예외, 커넥션 프로바이더가 반납 시 RESET 하는지 목 기반)와 통합 테스트(t_it_<랜덤> 스키마 2개로 데이터가 섞이지 않는지, 반납 후 다음 대여의 search_path 가 이전 테넌트가 아닌지, 없는 테넌트/SUSPENDED 거절). 통합 테스트는 3.2단계 절차(접속 정보 비밀 보호, DB 이름 pmis 확인, 만든 스키마만 삭제)로 실행하고 결과를 기록한다. t_it_ 스키마는 TenantDirectory 테스트용 tenant 행을 만든 뒤에만 사용하고, 테스트가 만든 tenant 행과 스키마를 모두 정리한다.
- 새 라이브러리는 추가하지 않는다.
- 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [4.1단계] 로 기록한다. 질문이 있으면 구현을 멈추고 묻는다. 완료 후 멈춘다.
```

---

## 4.2단계: 공용 스키마 Flyway V2 (플랫폼 운영자)

```
doc/erd/erd-model-tenant-*.json 이 갱신되었다(최신 파일을 기준으로 한다). 엔티티 3개가 추가되었다: platform_admin, platform_admin_login_history, platform_admin_refresh_token.
- db/migration/public/V2__create_platform_admin_tables.sql 을 작성한다(스키마명 없음, Flyway default-schema 사용). 공용 V1 파일은 수정하지 않는다.
- 규칙: PK BIGINT + 시퀀스(<table>_seq), login_id UK + CHECK (login_id = lower(login_id)), email 은 NOT NULL(UK 아님), use_yn/must_change_password_yn/login_fail_count 기본값, 감사컬럼 FK 없음, 이력/토큰 테이블 규칙은 tenant V2(user_login_history, user_refresh_token)와 같게(token_hash UK, login_datetime 인덱스, platform_admin_pk 인덱스).
- 상태/결과/폐기사유 코드 컬럼은 공용 스키마에 공통코드 테이블이 없으므로 애플리케이션 Enum 으로 관리한다(CHECK 는 두지 않는다. 기존 공용 V1 규칙과 같게).
- 엔티티 클래스는 만들지 않는다(인증 설계 단계에서 만든다). 통합 테스트(PublicTenantMigrationTest 확장 또는 새 테스트)로 V1+V2 가 ERD 와 일치하는지 확인하고 실행한다(3.2단계 절차).
- 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [4.2단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 4.3단계: 공유 개발 DB 스키마 pmis 에 공용 마이그레이션 적용

```
4.1단계 질문에 대한 결정: 공유 개발 DB 의 스키마 pmis 에 공용 Flyway(db/migration/public V1+V2)를 적용하는 것을 승인한다. 이것은 공유 DB 를 실제로 변경하는 첫 작업이므로 아래 순서와 확인을 지킨다.
1. 실행 전 확인(읽기 전용): 접속 URL 의 DB 이름이 pmis 인지(값 전체는 출력 금지), current_database() 가 pmis 인지, 스키마 pmis 가 존재하고 비어 있는지(테이블/시퀀스 0개, flyway_schema_history 없음)를 확인한다. 비어 있지 않거나 flyway_schema_history 가 이미 있으면 적용하지 말고 목록(이름만)을 보고하고 멈춘다.
2. 적용: 환경변수 MDM_FLYWAY_ENABLED=true 를 이 명령을 실행하는 프로세스에서만 설정하고(영구 설정/파일 기록 금지) 앱을 기동해 공용 마이그레이션만 실행한다(테넌트 마이그레이션은 기동 시 실행되지 않아야 한다). 기동 시 DB 이름 검사가 동작해야 한다. 마이그레이션이 끝나면 앱을 종료한다. 앱 기동 중 접속 URL 이나 비밀번호가 로그에 출력되면 마스킹해서 기록한다.
3. 적용 후 확인(읽기 전용): pmis 의 flyway_schema_history 에 V1, V2 가 success 로 있는지, 테이블 8개(tenant, tenant_domain, tenant_status_history, tenant_provisioning, tenant_schema_migration, platform_admin, platform_admin_login_history, platform_admin_refresh_token + flyway_schema_history 별도)가 있는지, 다른 스키마(public 등)가 바뀌지 않았는지 확인한다. 테이블 이름 목록만 기록한다(데이터 내용 출력 금지).
4. 4.1단계에서 skip 된 TenantSchemaIsolationTest.rejectsMissingAndSuspendedTenants 를 포함해 gradlew.bat integrationTest 를 3.2단계 절차대로 실행한다. 이 테스트가 pmis.tenant 에 넣은 행은 테스트가 정리해야 한다(테스트 종료 후 pmis.tenant 에 테스트 행이 남지 않았는지 확인). 실패하면 원인 분석만 기록하고 멈춘다.
5. 이 단계에서는 운영자 초기 계정을 만들지 않는다(인증 설계 단계에서 정한다). 실패해도 pmis 스키마를 삭제하거나 clean 하지 않는다. 문제가 생기면 상태(이력 테이블 포함)를 보고하고 멈춘다.
- doc/cursor-progress.md 에 [4.3단계] 로 기록한다(적용한 시각, 적용된 버전, 테이블 이름 목록). 완료 후 멈춘다.
```

---

## 4.4단계: 테넌트 프로비저닝·일괄 업그레이드 서비스 (MIGRATED 단계까지)

```
doc/multitenancy-design.md 의 "프로비저닝", "일괄 업그레이드" 절을 구현한다. 설계 담당(사용자 승인)의 결정:
1. 최초 테넌트 관리자는 "임시 비밀번호" 방식으로 만든다(초대 메일 아님). 임시 비밀번호는 SecureRandom 으로 생성하고, 평문은 DB·로그·예외 메시지·진행 문서 어디에도 남기지 않는다. 계정에는 해시만 저장하고 must_change_password_yn='Y' 로 둔다. 평문은 프로비저닝 결과 객체로 호출자에게 1회만 돌려준다(재조회 불가, toString 에서 마스킹). 메일 발송은 나중에 추가하는 별도 기능이다. tenant_provisioning.admin_email 은 관리자 로그인 ID/연락처 용도로만 쓴다.
2. 이번 단계는 REQUESTED → SCHEMA_CREATED → MIGRATED 까지만 구현한다. SEEDED(6단계 Seed 필요)와 ADMIN_CREATED(역할·메뉴 Seed 와 비밀번호 인코더 선정 필요)는 단계 핸들러 인터페이스(ProvisioningStep)만 만들고 구현체는 만들지 않는다. 핸들러가 없는 단계에 도달하면 tenant 를 PROVISIONING 으로, execution_status_code 를 RUNNING 으로, current_step_code 를 마지막 성공 단계로 두고 정상 종료한다(FAILED 아님). COMPLETED/ACTIVE 전환은 두 핸들러가 구현된 뒤에 한다. 비밀번호 인코더 라이브러리는 추가하지 않는다.
3. 진입점은 서비스 계층(TenantProvisioningService, TenantUpgradeService)만 만든다. 컨트롤러/API 는 만들지 않는다(운영자 인증 설계 이후). 호출자 식별용으로 실행자 user_pk(platform_admin_pk) 를 파라미터로 받되 null 이면 시스템 실행이다.
4. 실패한 스키마는 자동 삭제하지 않는다(설계 문서 그대로). 프로덕션 코드에는 DROP SCHEMA 를 만들지 않는다. 테스트 정리용 삭제는 테스트 코드에서만, 이름이 t_it_ 로 시작하는 본인이 만든 스키마에 한해 한다.

[구현 범위]
- 테넌트 입력 검증: tenant_code(소문자/숫자/하이픈), schema_name 은 코드에서 계산하지 않고 호출자 입력 또는 규칙 함수(t_ + 코드의 하이픈을 밑줄로)를 한 곳에 두고 SchemaNames 검증기를 반드시 통과시킨다. 중복 코드/스키마명은 거절한다.
- 동시 실행 방지: 같은 테넌트의 프로비저닝·업그레이드는 PostgreSQL pg_try_advisory_lock 으로 막는다. 락은 같은 커넥션에서 잡고 풀어야 하므로 전용 커넥션을 쓰고 finally 에서 해제한다. 락을 못 잡으면 대기하지 않고 즉시 오류(문자열 오류 코드 TENANT.PROVISIONING_IN_PROGRESS 형식)를 낸다. 락 키 산출 방식은 설계 문서에 쓴다.
- 단계 기록: 각 단계 결과를 tenant_provisioning, 상태 변경을 tenant_status_history 에 남긴다. 단계 기록과 실패 기록은 별도 트랜잭션(REQUIRES_NEW)으로 커밋해서 실패해도 남게 한다. 실패하면 tenant 를 FAILED 로, error_message/current_step_code/retry_count 를 기록한다(error_message 에 접속 정보·비밀번호가 들어가지 않게 마스킹). 재시도는 current_step_code 다음 단계부터 한다.
- SCHEMA_CREATED: CREATE SCHEMA IF NOT EXISTS 는 쓰지 않는다. 스키마가 이미 있는데 우리 기록(이전 단계 성공 기록)이 없으면 중단하고 FAILED 로 보고하며, 남의 스키마에 Flyway 를 적용하지 않는다. 실행 전 current_database()=pmis 검사를 통과해야 한다.
- MIGRATED: 프로그램 방식 Flyway 로 db/migration/tenant 를 해당 테넌트 스키마에만 적용한다(defaultSchema/schemas=해당 스키마, 이력 테이블 flyway_schema_history, createSchemas=false, cleanDisabled=true, 앱 기동 Flyway 설정과 분리). 성공 시 tenant.schema_version 에 적용된 최신 버전을 기록하고 tenant_schema_migration 에 PROVISION 행을 남긴다.
- 일괄 업그레이드(TenantUpgradeService): 기동 시 자동 실행하지 않는다. ACTIVE 테넌트만 순서대로 처리하고 SUSPENDED/TERMINATED/PROVISIONING/FAILED 는 건너뛴다. 테넌트마다 tenant_schema_migration 에 UPGRADE 행을 RUNNING 으로 넣고 SUCCESS/FAILED 로 갱신한다. 한 테넌트가 실패해도 다음으로 진행한다. 여러 인스턴스의 동시 일괄 실행은 배치 전체 advisory lock 으로 막는다. 이번에는 ACTIVE 테넌트가 아직 없으므로 통합 테스트에서 테스트용 tenant 행을 ACTIVE 로 직접 넣어 검증한다.
- doc/multitenancy-design.md 를 갱신한다: ADMIN_CREATED 행을 "임시 비밀번호, 해시만 저장, must_change_password_yn=Y, 평문은 결과 객체로 1회 반환"으로 바꾸고, 이번 단계에서 구현한 범위(MIGRATED 까지)와 락 키 방식, 단계 핸들러가 없을 때의 동작을 반영한다.
- 새 라이브러리는 추가하지 않는다. 패키지는 tenant 도메인(tenant.service 등)에 두고 global.multitenancy 가 tenant 를 임포트하지 않는 의존 방향을 지킨다.

[테스트]
- 단위(DB 없음): 이미 끝난 단계를 건너뛰는지, 핸들러 없는 단계에서 RUNNING 으로 멈추는지, 락 실패 시 즉시 거절, 임시 비밀번호 객체 toString 마스킹, 스키마명 검증기 통과 여부.
- 통합(@Tag("integration"), 3.2단계 절차: 접속 정보 비밀 보호, DB 이름 pmis 확인): 스키마명은 t_it_<랜덤> 만 사용. (a) 프로비저닝이 스키마를 만들고 tenant V1~V3 를 적용해 테이블 31개 + flyway_schema_history 가 생기는지, tenant 상태 PROVISIONING·단계 MIGRATED·RUNNING 인지 (b) 같은 테넌트를 다시 실행해도 오류 없이 이어지는지(멱등) (c) 마이그레이션 실패를 주입해 FAILED 와 재시도(current_step_code 다음부터)가 동작하는지 (d) 스키마는 있는데 기록이 없으면 중단하는지 (e) 같은 테넌트 동시 실행 시 한쪽이 거절되는지 (f) 일괄 업그레이드가 ACTIVE 만 처리하고 SUSPENDED 는 건너뛰는지, 한 테넌트 실패가 다른 테넌트를 막지 않는지 (g) 반납 후 search_path 가 이전 테넌트 스키마가 아닌지. 테스트가 만든 tenant, tenant_provisioning, tenant_status_history, tenant_schema_migration 행과 t_it_ 스키마를 모두 정리하고, 종료 후 pmis 업무 테이블이 0행인지 확인한다.
- 질문이 있으면 구현을 멈추고 묻는다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [4.4단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 6단계: 테넌트 Seed 데이터와 SEEDED 단계 구현

```
4.4단계 결과를 승인한다(질문 없음, 가정한 사항 모두 승인: 스키마명 규칙 함수, advisory lock 키, 업그레이드 실패 시 tenant 상태는 바꾸지 않음). 설계 담당(사용자 승인)의 결정:
1. Seed 는 Flyway 버전 마이그레이션에 넣지 않고 별도 SQL 파일(src/main/resources/db/seed/tenant/)로 둔다. 새 테넌트의 SEEDED 단계에서 실행한다. 여러 번 실행해도 안전해야 한다(멱등): 이미 있는 행은 건드리지 않고(수정·덮어쓰기 금지, 테넌트가 화면에서 바꾼 값을 보존) 없는 행만 넣는다. 부분 유니크 인덱스(use_yn='Y' 조건)에는 ON CONFLICT 대신 INSERT ... WHERE NOT EXISTS 를 쓰거나, ON CONFLICT 를 쓰려면 인덱스 조건을 정확히 맞춘다. 어느 쪽이든 한 가지로 통일하고 설계 문서에 쓴다.
2. 지원 언어는 ko-KR(기본, default_yn='Y'), en-US 두 개다. 대체 언어 체인과 날짜/숫자 포맷은 합리적인 값으로 정하고 가정으로 기록한다.
3. 국가(nation)는 주요 약 30개국만 넣는다(KR, US, JP, CN, GB, DE, FR, CA, AU, SG, IN, VN, TH, ID, MY, PH, TW, HK, AE, SA, BR, MX, RU, IT, ES, NL, CH, SE, NZ, TR). ISO alpha-3, 숫자 코드, 전화 국가번호, 통화 코드, 대표 시간대, 기본 언어를 채운다. 확신이 없는 값은 비워 두고 질문 목록에 적는다(추측으로 채우지 않는다). 나머지 국가는 화면에서 추가한다.
4. 공통코드: doc/erd/*.json 의 설명에서 "공통코드 <그룹코드>" 로 언급된 모든 컬럼(예: ACCOUNT_TYPE, USER_STATUS, PASSWORD_CHANGE_TYPE, 메뉴 유형, 기능(action) 코드, 저장소 유형, 번역 상태, 메시지 유형 등)을 빠짐없이 찾아 그룹/코드값/한글명·영문명(common_code_lang)으로 넣는다. 먼저 찾은 그룹 목록과 각 코드값을 진행 문서에 표로 보여 준다. ERD 설명에 값이 적혀 있으면 그것을 따르고, 적혀 있지 않은 그룹은 값을 만들지 말고 질문 목록에 올린다. 공용 스키마의 상태값(tenant 상태 등)은 애플리케이션 Enum 이므로 넣지 않는다. 그룹은 solution_code='MDM'.
5. i18n: 네임스페이스 common, error, menu 를 만들고(solution_code='MDM'), menu 네임스페이스에는 아래 메뉴 이름의 ko-KR/en-US 문구를, error 네임스페이스에는 코드에 이미 정의된 문자열 오류 코드(ApiException 사용처를 전부 찾는다)의 ko-KR/en-US 문구를 넣는다. 메시지 키는 오류 코드 그대로를 쓴다(예: AUTH.xxx). 공용 스키마의 TENANT.* 오류는 테넌트 스키마가 없는 요청에서 나오므로 이 Seed 에 넣지 않고, 기본 메시지(default_message)로 처리한다고 가정을 기록한다.
6. 역할 3종: TENANT_ADMIN(모든 권한), MANAGER(모든 메뉴 READ + 조직·사람·팀·파일 메뉴의 CREATE/UPDATE/EXPORT), USER(조직·사람·팀·파일·다국어·공통코드 메뉴 READ). 역할 코드는 대문자 SNAKE 이며 CHECK 를 통과해야 한다. READ 이외 권한을 가진 역할은 같은 메뉴의 READ 도 가져야 한다.
7. 메뉴(solution_code='MDM'): 최상위 그룹 SYSTEM 아래 I18N(다국어), COMMON_CODE(공통코드), COMPANY(회사), DEPT(부서), PERSON(사람), USER(사용자), FILE(파일), TEAM(팀), ROLE(역할), MENU(메뉴), PERMISSION(권한). 메뉴 유형은 4번에서 확인한 공통코드 값을 쓴다(그룹/화면). route_path 는 FE 라우트가 아직 없으므로 /system/<소문자 코드> 로 하고 가정으로 기록한다. 메뉴의 level_no/menu_path 를 일관되게 채운다. 화면 메뉴마다 기본 기능 READ/CREATE/UPDATE/DELETE/EXPORT 의 permission 행을 만든다(permission_code 형식은 ERD permission 설명을 따른다). 그룹 메뉴 SYSTEM 에는 READ 만 만든다. ROLE/MENU/PERMISSION/USER 메뉴는 TENANT_ADMIN 만 쓴다(MANAGER/USER 에 role_permission 을 주지 않는다).
8. file_storage 는 Seed 하지 않는다(저장 위치는 환경마다 다르다). 사용자·사람·회사·부서 등 업무 데이터와 최초 관리자도 이 단계에서 넣지 않는다(ADMIN_CREATED 는 다음 단계).
9. DDL(V1~V3)은 수정하지 않는다. Seed 에 필요한 컬럼/제약이 없거나 충돌하면 멈추고 질문한다. Seed 행의 create_user_pk/update_user_pk 는 NULL(시스템).

[구현 범위]
- db/seed/tenant/ 아래 번호 순서 SQL 파일(예: S01_support_language.sql, S02_nation.sql, S03_code.sql, S04_i18n.sql, S05_role_menu_permission.sql). PK 는 각 테이블의 시퀀스(nextval)를 쓴다. FK 대상은 PK 를 하드코딩하지 않고 자연키(코드)로 조회해서 연결한다.
- tenant.provision 에 SEEDED 단계 핸들러(ProvisioningStep 구현)를 추가한다. 해당 테넌트 스키마에만 SQL 을 실행한다(스키마명은 검증기를 통과한 tenant.schema_name, 접속 DB 이름 pmis 검사 포함). 파일 단위 트랜잭션으로 실행하고 실패하면 FAILED + current_step_code=MIGRATED 로 두어 재시도 시 SEEDED 부터 다시 한다. 테넌트 스키마 SQL 에 스키마명을 하드코딩하지 않는다(search_path 사용).
- ADMIN_CREATED 핸들러는 아직 만들지 않는다. SEEDED 이후 ADMIN_CREATED 핸들러가 없으므로 4.4와 같이 PROVISIONING/RUNNING, current_step_code=SEEDED 로 멈춘다.
- 일괄 업그레이드(TenantUpgradeService)는 Flyway 성공 뒤 같은 Seed 를 다시 실행해 새로 추가된 Seed 만 보강하도록 연결한다(멱등이므로 기존 값은 바뀌지 않아야 한다). Seed 실패는 해당 테넌트의 tenant_schema_migration 에 FAILED 로 남기고 다음 테넌트로 진행한다.
- doc/multitenancy-design.md 의 SEEDED 행과 일괄 업그레이드 절을 갱신한다(Seed 파일 위치, 멱등 방식, 덮어쓰지 않는다는 원칙).

[테스트]
- 단위: 파일 순서 정렬, 핸들러 없는 다음 단계에서 멈춤 유지.
- 통합(@Tag("integration"), 3.2단계 절차, t_it_<랜덤> 스키마만 사용): (a) 프로비저닝이 SEEDED 까지 진행되고 단계가 SEEDED/RUNNING 인지 (b) Seed 를 두 번 실행해도 모든 테이블 행 수가 같은지 (c) 테넌트가 바꾼 값(예: 코드명, 역할명)을 Seed 재실행이 덮어쓰지 않는지 (d) ERD 의 공통코드 그룹이 모두 존재하고 각 코드에 ko-KR/en-US 이름이 있는지 (e) 모든 화면 메뉴에 READ/CREATE/UPDATE/DELETE/EXPORT 권한이 있고 SYSTEM 에는 READ 만 있는지 (f) 모든 role_permission 이 "READ 이외 권한이면 같은 메뉴 READ 보유" 규칙을 지키는지, TENANT_ADMIN 이 전체 권한을 갖는지, USER 가 쓰기 권한이 없는지 (g) 기본 지원 언어 ko-KR 이 정확히 1개인지 (h) 모든 i18n_message 에 ko-KR/en-US 번역이 있는지. 테스트가 만든 tenant 관련 행과 스키마를 정리하고 pmis 업무 테이블 0행을 확인한다. 기존 14건 통합 테스트도 모두 통과해야 한다.
- 새 라이브러리는 추가하지 않는다. 질문(특히 4번의 코드값이 ERD 에 없는 그룹, 3번의 불확실한 국가 값)이 있으면 해당 부분만 비워 두고 나머지를 진행한 뒤 진행 문서의 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [6단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 6.1단계: Seed 질문 확정사항 반영, 최초 관리자(임시 비밀번호) 생성과 프로비저닝 완료

```
6단계 결과를 승인한다. 가정한 사항은 모두 승인한다(NOT EXISTS 멱등, 언어 포맷, 국가 값, route_path/menu_path, ROLE·MENU·PERMISSION·USER 메뉴는 TENANT_ADMIN 전용, APPROVE 권한 행 미생성, Seed 실패 시 current_step_code=MIGRATED 유지). 설계 담당(사용자 승인)의 결정:
1. 공통코드 ATTACHMENT_CATEGORY 에 GENERAL(일반), PROFILE(프로필), PHOTO(사진), CONTRACT(계약서)를 추가한다(한글/영문 이름 포함). POSITION, JOB_TITLE 은 회사마다 달라서 값을 넣지 않고 테넌트가 화면에서 등록한다(현행 유지).
2. 보안 정책값은 DB 공통코드로 관리한다. 그룹 SECURITY_POLICY(solution_code=MDM, 한글 "보안 정책"/영문 "Security Policy")를 만들고 코드 TEMP_PASSWORD_VALID_DAYS(임시 비밀번호 유효 일수, ext_attr1='7', 한글/영문 이름과 설명)를 Seed 한다. 코드값을 코드 상수에 박지 말고 ADMIN_CREATED 가 실행될 때 해당 테넌트 스키마에서 읽는다. 값이 없거나 양의 정수가 아니면 임의 기본값으로 계속하지 말고 FAILED 로 중단한다(오류 코드 문자열 예: TENANT.SECURITY_POLICY_MISSING). 이 Seed 는 기존 테넌트에도 일괄 업그레이드가 추가해 준다(멱등, 이미 바꾼 값은 보존).
3. 비밀번호 해시는 BCrypt 다. 새 의존성은 org.springframework.security:spring-security-crypto 하나만 추가한다(Spring Boot BOM 버전 사용, 전체 spring-security 스타터는 추가하지 않는다). global.security(또는 기존 패키지 구조에 맞는 위치)에 PasswordHasher 인터페이스와 BCrypt 구현을 두고 강도는 설정값(기본 10)으로 한다.
4. 임시 비밀번호: SecureRandom, 길이 16, 혼동되는 문자(0/O, 1/l/I)를 뺀 문자 집합, 대문자·소문자·숫자·기호가 각각 1자 이상 들어가게 한다. 평문은 4.4단계 규칙대로 DB·로그·예외 메시지·진행 문서에 남기지 않고 결과 객체로 1회만 반환한다(toString 마스킹 유지).
5. 최초 관리자 생성(ADMIN_CREATED 핸들러)은 해당 테넌트 스키마에서 한 트랜잭션으로 아래를 만든다. 하나라도 실패하면 전부 롤백한다.
   - company: company_code=tenant_code(스키마 대문자 규칙이 있으면 따른다), company_name=tenant.tenant_name, company_type_code='OWN'.
   - person: person_name=tenant_provisioning.admin_name, email=admin_email.
   - person_assignment: 위 회사, employment_type_code='REGULAR', assignment_status_code='ACTIVE', primary_yn='Y', dept_head_yn='N', start_date=오늘.
   - user_account: person_pk 연결, account_type_code='NORMAL', login_id=admin_email 소문자 정규화, password_hash, must_change_password_yn='Y', password_changed_datetime=지금, password_expire_datetime=지금+TEMP_PASSWORD_VALID_DAYS일, user_status_code='ACTIVE', support_language_code=tenant.default_language_code, timezone_id=tenant.timezone_id.
   - user_password_history: change_type_code='INITIAL'.
   - user_role: TENANT_ADMIN 역할 연결(6단계 Seed 의 역할을 코드로 조회. 없으면 FAILED 로 중단).
   - 감사 컬럼의 create_user_pk 는 NULL(시스템) 이다.
   - 같은 login_id 의 계정이 이미 있으면 새로 만들지 않고 임시 비밀번호도 만들지 않는다(결과에 "이미 존재" 표시). 재시도는 롤백 덕분에 중복 없이 처음부터 다시 만들어진다.
6. COMPLETED: ADMIN_CREATED 가 성공하면 tenant_status_code='ACTIVE', activated_datetime=지금, tenant_status_history 기록, tenant_provisioning 의 execution_status_code='SUCCESS'/current_step_code='COMPLETED'/completed_datetime, tenant.schema_version 갱신, TenantDirectory 캐시 무효화를 한다. 이미 ACTIVE 이면 아무것도 하지 않는다.
7. 임시 비밀번호는 한 번만 보여 주므로 분실에 대비해 재발급 서비스(TenantAdminPasswordService.reissue(tenantCode, executorPk))를 만든다: 해당 테넌트 관리자(TENANT_ADMIN 중 최초 관리자 login_id=tenant_provisioning.admin_email) 계정의 비밀번호를 새 임시 비밀번호로 바꾸고 must_change_password_yn='Y', 만료 갱신, 잠금 해제(user_status ACTIVE, login_fail_count 0), user_password_history 에 RESET 행을 남긴다. 평문은 결과 객체로 1회만 반환한다. 상태가 ACTIVE 인 테넌트만 허용한다. 컨트롤러는 만들지 않는다(운영자 인증 설계 이후).
8. doc/multitenancy-design.md 의 프로비저닝 표(ADMIN_CREATED, COMPLETED), 재발급, 정책값을 공통코드에서 읽는 방식을 갱신한다.

[테스트]
- 단위: 임시 비밀번호 생성 규칙(길이/문자 종류/혼동 문자 없음), 결과 객체 toString 마스킹, 정책값 누락/잘못된 값에서 FAILED 처리, 이미 존재하는 계정 처리.
- 통합(@Tag("integration"), 3.2단계 절차, t_it_<랜덤> 스키마만 사용): (a) 전체 프로비저닝이 ACTIVE/COMPLETED 까지 가는지 (b) company(OWN)·person·person_assignment·user_account·user_password_history(INITIAL)·user_role(TENANT_ADMIN)이 각 1건씩 생겼는지 (c) 반환된 임시 비밀번호를 BCrypt 로 검증하면 일치하고 DB 어디에도 평문이 없는지(user_account, user_password_history, tenant_provisioning.error_message 검사), 로그에 평문이 없는지(로그 어펜더 캡처) (d) must_change_password_yn='Y' 이고 만료일이 TEMP_PASSWORD_VALID_DAYS 를 3 으로 바꾸면 3일 뒤로 계산되는지(공통코드 값을 바꿔 검증) (e) 정책 코드를 지우면 FAILED 이고 정책을 복구한 뒤 재시도하면 성공하는지, 실패 시 부분 데이터가 남지 않는지 (f) 다시 프로비저닝/ADMIN_CREATED 를 실행해도 중복 계정이 없고 새 임시 비밀번호가 나오지 않는지 (g) 재발급이 비밀번호를 바꾸고 RESET 이력을 남기며 잠긴 계정을 풀고 이전 비밀번호가 더 이상 일치하지 않는지, ACTIVE 가 아니면 거절하는지 (h) 일괄 업그레이드가 기존 테넌트에 SECURITY_POLICY 와 ATTACHMENT_CATEGORY 값을 추가하되 바꾼 값은 보존하는지. 테스트가 만든 모든 행과 스키마를 정리하고 pmis 업무 테이블 0행을 확인한다. 기존 15건 통합 테스트도 통과해야 한다.
- 질문이 있으면 해당 부분만 멈추고 진행 문서 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [6.1단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 7단계: 공통 API 기반(페이징·권한 훅)과 공통코드 API

```
6.1단계 결과를 승인한다(질문 없음, 가정 모두 승인: company_code 는 테넌트 코드 그대로, 재발급 시 locked_datetime 초기화, 강도 10, COMPLETED 실패 시 평문 미반환 + 재시도 뒤 재발급으로 받음, 이미 ACTIVE 이면 TENANT.ALREADY_ACTIVE). 설계 담당(사용자 승인)의 결정:
1. API URL: /api/mdm/v1/<리소스>. 리소스 이름은 복수 kebab-case(예: /api/mdm/v1/code-groups, /api/mdm/v1/common-codes).
2. 페이징: 요청 쿼리 page(1부터, 기본 1), size(기본 20, 최대 200), sort(예: sort=codeValue,asc 여러 개 허용). 응답은 항상 ApiResponse 래퍼의 data = { items: [...], total, page, size }. 목록 응답용 PageResponse<T> record 를 global.response 에 둔다. sort 는 리소스별 허용 필드 목록(화이트리스트)에 있는 것만 받고, 없는 필드/방향이면 400 COMMON.INVALID_SORT. size 초과/0 이하/page 0 이하는 400 COMMON.INVALID_PAGE. 정렬이 없으면 리소스별 기본 정렬을 쓰고 항상 PK 를 마지막 정렬 키로 붙여 페이지 간 순서를 안정시킨다. Spring Data 의 Pageable 기본 바인딩(0부터)은 쓰지 않는다(변환 계층을 한 곳에 둔다).
3. 권한 훅: 인증은 아직 없다(별도 인증 설계 단계). 그래서 PermissionChecker 인터페이스(check(menuCode, actionCode))와 선언용 애너테이션(예: @RequirePermission(menu="COMMON_CODE", action="UPDATE"))만 만든다. 6단계 Seed 의 permission_code 형식을 그대로 써서 연결한다. 기본 구현(운영 포함 모든 프로필)은 "모두 거부"(403, 오류 코드 AUTH.FORBIDDEN)이고, 개발 프로필(dev)에서만 "모두 허용" 구현을 등록한다. 4.1단계의 X-Tenant-Code 개발용 리졸버와 같은 방식이다. 이 선언은 컨트롤러 메서드 단위로 둔다(AOP 는 30-java-jpa-conventions.mdc 에서 허용한 범위인 메서드 보안 용도). 조회 전용 공개 API(아래 읽기 API)는 권한 선언 없이 테넌트 식별만 필요하다.
4. 이번 단계 범위는 공통코드만이다(다국어 API 는 7.1단계). 패키지는 code(package-by-feature, 레이어: controller/service/repository/entity/dto).
5. 읽기 API(권한 선언 없음, 테넌트만 필요):
   - GET /api/mdm/v1/codes/{groupCode}: 해당 그룹의 사용 중(use_yn='Y', 유효기간 내) 코드 목록. 언어는 lang 쿼리 파라미터 > Accept-Language > 테넌트 기본 언어 순으로 정하고, 이름은 common_code_lang 에서 언어 → 대체 언어 체인(support_language.fallback_language_code) → common_code.code_name 순으로 찾는다. 계층형 그룹(hierarchy_yn='Y')은 parent_code_value 필드를 포함한 평면 목록으로 준다(트리 조립은 FE). 응답 필드: codeValue, codeName, parentCodeValue, sortOrder, extAttr1~5. 존재하지 않거나 사용 중지된 그룹은 404 CODE.GROUP_NOT_FOUND.
   - GET /api/mdm/v1/codes: 여러 그룹을 한 번에 받는 용도. groups=A,B,C 쿼리(최대 30개)로 그룹별 목록 맵을 돌려준다.
6. 관리 API(@RequirePermission, 메뉴 COMMON_CODE):
   - code-groups: 목록(페이징, 검색 keyword=그룹코드·이름 부분일치, solutionCode 필터, useYn 필터), 단건 조회, 등록, 수정, 삭제(= use_yn='N' 논리 삭제, 하위 코드가 사용 중이면 409 CODE.GROUP_IN_USE), 복구(use_yn='Y'). 그룹 이름 다국어(code_group_lang)는 요청 본문의 translations 배열로 받아 (그룹, 언어) 기준 upsert 한다. 요청에 없는 언어의 행은 건드리지 않는다(번역 행 물리 삭제는 하지 않는다).
   - common-codes: 목록(페이징, groupCode 필수 필터, keyword, useYn), 단건, 등록, 수정, 논리 삭제/복구. code_value 는 그룹 안에서 유일(부분 유니크 인덱스와 일치), code_value_length 가 있으면 길이 검증, 계층형이 아닌 그룹에 parent 를 주면 400, parent 는 같은 그룹이어야 하며 순환 금지, level_no/code_path 는 서버가 계산한다. 이름 다국어(common_code_lang)도 같은 upsert 방식(코드, 언어 기준, 요청에 없는 언어는 유지)으로 받는다. valid_from_date ≤ valid_to_date 검증.
   - 코드 변경(등록/수정/삭제/복구)마다 common_code_history 에 변경 유형(C/U/D, 복구는 U), 변경 전/후 JSON, 변경 사유(요청의 changeReason, 선택), 변경 시각, 변경 사용자(지금은 NULL)를 같은 트랜잭션으로 남는다. 이력 조회 API: GET /api/mdm/v1/common-codes/{codePk}/history (페이징).
   - 6단계 Seed 로 들어간 값도 같은 규칙으로 수정할 수 있다(system_yn 은 없다). 단, 이번 단계에서 6.1까지의 코드가 읽는 SECURITY_POLICY/TEMP_PASSWORD_VALID_DAYS 를 수정·삭제하는 경우 ext_attr1 이 양의 정수가 아니면 400 CODE.INVALID_POLICY_VALUE 로 거절한다.
7. 공통: ApiResponse 래퍼와 @RestControllerAdvice 를 그대로 쓴다. 요청 검증(Bean Validation)은 400 + fieldErrors, 중복은 409 CODE.DUPLICATE, 낙관적 동시성은 이번 범위가 아니다. 오류 코드는 문자열 <DOMAIN>.<NAME>(CODE.*, COMMON.*, AUTH.FORBIDDEN)이고 새로 만든 모든 오류 코드의 ko-KR/en-US 문구를 db/seed/tenant 의 error 네임스페이스 Seed(S04)에 추가한다(멱등 NOT EXISTS 방식 유지). spring-boot-starter-validation 이 아직 없으면 추가해도 된다(그 외 새 라이브러리는 금지).
8. 엔티티: 30-java-jpa-conventions.mdc 규칙대로(@Table 명시, 지연 로딩, @Data/@Setter 금지, 도메인 메서드로 변경). 엔티티-DDL 일치는 t_it_<랜덤> 스키마에 V1~V3 를 적용한 통합 테스트에서 Hibernate 스키마 검증(validate)으로 확인한다(앱 기동은 ddl-auto=none 유지).
9. FE 연동 계약 문서 doc/api/mdm-api.md 를 만든다: 공통 규약(래퍼, 페이징, 정렬, 오류 코드 규칙, 테넌트 식별/dev 헤더, 권한 훅 동작) + 이번 단계 엔드포인트별 요청/응답 예시와 오류 코드 표. 이후 단계가 같은 파일에 이어서 쓴다.

[테스트]
- 단위/슬라이스(DB 없음, MockMvc): 래퍼 형식, 페이징·정렬 검증 오류 코드, 권한 훅(dev 프로필 허용 / 기본 프로필 403), 검증 오류의 fieldErrors, 코드 계층·순환·길이 검증.
- 통합(@Tag("integration"), 3.2단계 절차, t_it_<랜덤> 스키마만 사용. 테넌트는 프로비저닝 서비스를 이용해 ACTIVE 로 만든다): (a) 엔티티 validate 통과 (b) 읽기 API 의 언어 대체 순서(요청 언어 → 대체 체인 → code_name) (c) 두 테넌트의 같은 그룹이 서로 다른 값을 가질 때 섞이지 않는지 (d) 코드 등록/수정/논리 삭제/복구와 common_code_history 행 (e) 논리 삭제된 코드가 읽기 API 와 유일성 검사에서 빠지는지 (f) 정렬 화이트리스트와 total/page 값 (g) 정책 코드 수정 거절 (h) 기본 프로필에서 관리 API 403. 테스트가 만든 모든 tenant 관련 행과 스키마를 정리하고 pmis 업무 테이블 0행을 확인한다. 기존 17건 통합 테스트도 통과해야 한다.
- 질문이 있으면 해당 부분만 멈추고 진행 문서 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [7단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 7.1단계: 다국어 API, 오류 메시지 다국어 연결, 유니크 제약 점검

```
7단계 결과를 승인한다. 가정 모두 승인(V4 부분 유니크, code_value_length=최대 길이, code_path 규칙, 인터셉터 방식 권한 훅, 모든 HTTP permitAll, 변경 사용자 NULL, useYn 생략 시 사용 중지 포함). V4 는 ERD 정본 쪽에 반영했다(doc/erd/erd-model-i18n-common-code-20261009.json 의 common_code.code_value 설명이 갱신됨. Cursor 는 ERD 파일을 수정하지 않는다). 설계 담당(사용자 승인)의 결정:
1. 조회성 목록(languages, nations 처럼 건수가 적은 코드성 목록)은 페이징하지 않고 data 에 배열을 바로 담는다. 관리용 검색 목록만 PageResponse 를 쓴다. 이 구분을 doc/api/mdm-api.md 에 명시한다.
2. 읽기 API(권한 선언 없음, 테넌트만 필요):
   - GET /api/mdm/v1/i18n/languages: 사용 중 지원 언어 전체(코드, 이름, 현지어 이름, 기본 여부, 대체 언어, 방향, 날짜/숫자 포맷).
   - GET /api/mdm/v1/i18n/bundles/{namespaceCode}?lang=: 해당 네임스페이스의 key→문구 맵. 언어 결정은 7단계 공통코드 읽기와 같다(lang > Accept-Language > 테넌트 기본 언어). 문구 우선순위는 요청 언어 → 대체 언어 체인 → 테넌트 기본 언어 → default_message. 순환이 있어도 무한 루프 없이 끊어야 한다. 응답 data = { namespaceCode, language, messages }. 약한 ETag(내용 해시)를 주고 If-None-Match 가 같으면 304(본문 없음, 래퍼 예외로 문서화)를 돌려준다. Cache-Control: no-cache.
   - GET /api/mdm/v1/i18n/bundles?namespaces=a,b(최대 20): 여러 네임스페이스를 맵으로 한 번에. 없는 네임스페이스는 404 I18N.NAMESPACE_NOT_FOUND.
   - GET /api/mdm/v1/nations: 사용 중 국가 전체(코드, 이름, 기본 언어, 전화 국가번호, 통화, 대표 시간대).
3. 관리 API(@RequirePermission, 메뉴 I18N. 국가 관리도 I18N 메뉴 권한을 쓴다. 이 결정을 문서에 적는다):
   - support-languages: 목록, 등록, 수정, 사용 중지/복구. 규칙: default_yn='Y' 는 정확히 1개(기본 언어를 바꾸면 한 트랜잭션 안에서 교체), 기본 언어와 다른 활성 언어의 대체 언어로 쓰이는 언어는 사용 중지 불가(409 I18N.LANGUAGE_IN_USE), 대체 언어 체인에 순환 금지(400 I18N.FALLBACK_CYCLE), 언어 코드는 BCP 47 형식 검증, 등록 후 코드 변경 불가.
   - nations: 목록(페이징), 등록, 수정, 사용 중지/복구. nation_code 는 ISO 3166-1 alpha-2 대문자 2자, 등록 후 변경 불가. default_language_code 는 활성 지원 언어여야 한다.
   - i18n-namespaces: 목록(페이징), 등록, 수정, 사용 중지/복구. namespace_code 는 등록 후 변경 불가. 메시지가 있는 네임스페이스를 사용 중지하면 번들 조회에서 빠진다(메시지 행은 유지).
   - i18n-messages: 목록(페이징, namespaceCode/keyword/messageTypeCode/useYn/번역 상태 필터), 단건(번역 포함), 등록, 수정, 논리 삭제/복구. message_key 는 네임스페이스 안에서 유일(use_yn='Y' 부분 유니크 기준, 7단계의 V4 방식과 일치해야 함). message_type_code 는 공통코드 I18N_MESSAGE_TYPE 값 검증은 하지 않고 문자열로 저장한다(공통코드는 FK/CHECK 없는 원칙). 번역(i18n_message_text)은 요청의 translations 배열로 (메시지, 언어) 기준 upsert 한다. 문구가 바뀔 때만 version_no 를 1 올리고, translation_status_code 는 요청값 또는 신규 DRAFT, machine_translated_yn 은 요청값 또는 N 이다. 요청에 없는 언어의 번역은 유지한다. 존재하지 않는 언어 코드는 400 I18N.LANGUAGE_NOT_FOUND.
   - 위 변경은 해당 네임스페이스/언어의 번들 캐시를 즉시 무효화한다. 번들 캐시는 새 라이브러리 없이 테넌트별 메모리 맵 + 짧은 TTL(60초)로 하고, 여러 인스턴스에서의 지연 반영은 TTL 이내라고 문서에 적는다.
4. 오류 메시지 다국어 연결: ApiExceptionHandler 가 만드는 errorMessage(와 fieldErrors 메시지)를 error 네임스페이스 번들에서 오류 코드 키로 찾아 요청 언어로 돌려준다. 못 찾거나 테넌트가 아직 식별되지 않은 요청(TENANT.* 등)은 예외의 기본 메시지를 쓴다. 번들 조회 실패가 오류 응답 자체를 깨뜨리면 안 된다(실패하면 기본 메시지). 이번 단계에서 새로 만든 모든 오류 코드의 ko-KR/en-US 문구를 S04 Seed 에 추가한다(NOT EXISTS 멱등 유지).
5. 7단계 가정 후속: 코드 유효기간의 "오늘"을 JVM 기본 시간대가 아니라 현재 테넌트의 timezone_id 기준으로 계산한다(테넌트 컨텍스트에서 읽고, 없으면 UTC 가 아니라 400/내부 오류가 아닌 안전한 폴백으로 시스템 기본 시간대를 쓰되 한 곳에서만 처리한다).
6. 유니크 제약 점검(코드 변경 전에 먼저 보고): 테넌트 V1~V4 전체에서 use_yn 컬럼이 있는 테이블의 "전체 UNIQUE 제약/전체 유니크 인덱스"를 모두 찾아 표로 진행 문서에 올린다(테이블, 제약/인덱스 이름, 컬럼, 판정). 10-database-conventions.mdc 의 "부분 유니크 인덱스는 use_yn='Y'(필요하면 end_date IS NULL) 조건 포함" 원칙에 어긋나 논리 삭제 후 재등록을 막는 것은 V5__partial_unique_fixes.sql 로 부분 유니크 인덱스로 바꾼다(V1~V4 파일은 수정하지 않는다). 이력(append-only), 번역 행처럼 use_yn 이 없는 테이블, 그리고 의도적으로 전체 유니크여야 하는 것(예: 변경 불가 자연키 중 재사용을 막아야 하는 것)은 바꾸지 말고 이유를 표에 적는다. 판단이 애매한 항목은 바꾸지 말고 질문 목록에 올린다. 바뀐 항목마다 "삭제 후 같은 값 재등록 가능 + 사용 중 중복은 거절" 통합 테스트를 추가한다. 엔티티 validate 가 다시 통과해야 한다.
7. doc/api/mdm-api.md 에 위 엔드포인트, 304/배열 응답 예외, 오류 코드 표를 이어서 쓴다.

[테스트]
- 단위/슬라이스: 번들 우선순위와 순환 끊기, ETag/304, 배열 응답 형식, 기본 언어 교체 규칙, 대체 체인 순환 거절, 오류 메시지 다국어 폴백(번들 실패 시 기본 메시지).
- 통합(@Tag("integration"), 3.2단계 절차, t_it_<랜덤> 스키마만, 테넌트는 프로비저닝 서비스로 ACTIVE 로 만든다): (a) 엔티티 validate (b) 번들 문구가 ko-KR/en-US/폴백 순서대로 나오는지 (c) 두 테넌트에서 같은 키의 문구가 섞이지 않는지 (d) 메시지 수정 후 번들에 즉시 반영(캐시 무효화)되고 version_no 가 변경 시에만 오르는지 (e) 기본 언어가 항상 1개인지, 대체 언어로 쓰이는 언어 중지 거절 (f) 논리 삭제 후 같은 message_key 재등록 가능 (g) 오류 응답이 요청 언어 문구로 오는지 (h) V5 가 있다면 각 항목의 재등록 테스트 (i) 기본 프로필 관리 API 403, 읽기 API 200. 테스트가 만든 모든 행과 스키마를 정리하고 pmis 업무 테이블 0행을 확인한다. 기존 19건 통합 테스트도 통과해야 한다.
- 새 라이브러리는 추가하지 않는다. 질문이 있으면 해당 부분만 멈추고 진행 문서 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [7.1단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 8단계: 인증 설계 (설계 문서만, 구현 금지)

```
7.1단계 결과를 승인한다(질문 없음). 유니크 점검 판정도 승인: 전체 유니크로 유지하는 항목(자연키·login_id·번역 행·file(storage_pk, storage_key)·i18n_entity_text)은 그대로 둔다. 결정이 필요했던 company.business_registration_no 는 "부분 유니크에 use_yn='Y' 추가"로 정했다(실제 수정은 8.1단계의 V6 에서 한다. 이번 단계에서는 DDL/Seed 를 바꾸지 않는다).
이번 단계는 doc/auth-design.md 를 쓰는 것이 전부다. 코드, 의존성, DDL, Seed 는 바꾸지 않는다(4단계와 같은 방식: 설계안 + 질문 → 설계 담당이 확정 → 구현). 아래는 설계 담당(사용자 승인)이 이미 정한 사항이고, 문서에는 이것을 전제로 구체화하되 세부는 현재 코드/DDL/ERD 를 읽고 결정안을 제시한다.

[확정 사항]
1. 라이브러리: Spring Security + OAuth2 Resource Server(Nimbus). JWT 서명 RS256. 새 의존성은 구현 단계(8.1)에서 추가한다.
2. 토큰 전달: Access 토큰(기본 15분)은 응답 본문으로 주고 FE 가 메모리에만 두며 Authorization: Bearer 헤더로 보낸다. Refresh 토큰(기본 14일)은 HttpOnly + Secure + SameSite 쿠키(경로를 인증 엔드포인트로 제한)로만 주고 응답 본문에 넣지 않는다. 로컬 개발(http)에서는 Secure 를 끄는 설정을 둔다. 쿠키를 쓰는 엔드포인트(refresh, logout)의 CSRF 대응(SameSite 와 사용자 지정 헤더 요구 등)을 문서에서 정한다.
3. 서명 키: RSA 개인키는 환경변수가 가리키는 PEM 파일 경로에서 읽는다(키 내용은 저장소·로그·채팅·DB 어디에도 두지 않는다). dev 프로필에서만 키가 없으면 기동 시 임시 키를 만든다. 그 외 프로필에서 키가 없으면 기동을 거부한다. kid 를 두고 JWKS 엔드포인트(공개키만)를 제공해 PMIS 서버가 검증하게 한다. 키 교체(다중 kid) 방법도 적는다.
4. 토큰 클레임은 최소로 한다: iss, sub(user_pk 또는 platform_admin_pk), aud, iat, exp, jti, tenant(테넌트 코드), typ(tenant-user 또는 platform), pwd_change_required(임시/만료 비밀번호 사용 중). 권한(역할/permission)은 토큰에 넣지 않고 요청마다 DB 에서 계산한다(짧은 TTL 캐시 60초, 역할·권한 변경 시 해당 캐시 무효화). PMIS 서버는 같은 테넌트 스키마를 읽어 권한을 계산하는 구조임을 문서에 적는다.
5. 테넌트 식별: 4.1단계 결정 그대로 Host + 검증된 토큰의 tenant 클레임이 일치해야 한다. 로그인 요청은 Host 로 식별(dev 헤더 리졸버 유지). JwtTenantResolver 스텁을 실제 구현으로 바꾸는 방법과 필터 순서를 설계한다.
6. 테넌트 사용자 인증 API(/api/mdm/v1/auth): login, refresh, logout, password/change, me(사용자 정보 + 역할 코드), 필요하면 별도 단계로 내 메뉴 트리(GET /api/mdm/v1/menus/my, READ 권한이 있는 메뉴만). 요청/응답 예시를 doc/api/mdm-api.md 형식으로 설계한다.
   - 로그인 실패는 계정 없음/비밀번호 틀림/비활성을 구분하지 않고 같은 오류(AUTH.INVALID_CREDENTIALS)로 돌려준다(계정 존재 노출 금지). 내부적으로는 user_login_history 의 LOGIN_RESULT 값(FAIL_PASSWORD 등)으로 구분해 기록한다. IP, User-Agent 도 기록한다.
   - 연속 실패 잠금: login_fail_count/locked_datetime/user_status(LOCKED). 잠금 횟수, 잠금 해제 방식(자동 해제 분 단위 또는 관리자 해제), 비밀번호 최소 길이/복잡도, 최근 N개 재사용 금지(user_password_history), Access/Refresh 유효시간은 코드에 박지 않고 SECURITY_POLICY 공통코드로 관리한다(이미 있는 TEMP_PASSWORD_VALID_DAYS 와 같은 방식). 필요한 코드 키 이름, 기본값, ext_attr 사용 방법을 표로 제안한다.
   - pwd_change_required 토큰(임시 비밀번호 로그인 또는 만료)은 password/change, logout, me 외의 API 를 403 AUTH.PASSWORD_CHANGE_REQUIRED 로 막는다.
   - Refresh 토큰은 user_refresh_token 에 해시로만 저장하고 회전(rotation)한다. 이미 쓴 토큰의 재사용을 감지하면 해당 가족(체인) 전체를 폐기한다(TOKEN_REVOKE_REASON: LOGOUT/ROTATED/REUSE_DETECTED/ADMIN/PASSWORD_CHANGED). 비밀번호 변경·재발급·계정 잠금 시 기존 refresh 토큰 전체 폐기. 현재 테이블 컬럼으로 부족한 것이 있으면 V6 변경안을 제시한다(적용은 하지 않는다).
7. 플랫폼 운영자 인증(/api/platform/v1/auth): 공용 스키마 platform_admin, platform_admin_login_history, platform_admin_refresh_token 사용. 토큰 typ=platform, tenant 클레임 없음, 별도 aud, 별도 refresh 쿠키 이름/경로. 테넌트 사용자 토큰으로 플랫폼 API 를, 플랫폼 토큰으로 테넌트 업무 API 를 호출할 수 없게 하는 규칙을 적는다. 플랫폼 운영자 최초 계정을 어떻게 만들지(예: 일회성 부트스트랩 실행 옵션과 환경변수, 평문 임시 비밀번호 출력/저장 금지 원칙과의 조화) 2~3개 안과 추천안을 제시한다.
8. 권한 검사: 7단계의 PermissionChecker/@RequirePermission 을 실제 구현으로 교체한다(role_permission 합집합, user_role 유효기간 내, use_yn='Y'). dev 프로필의 "모두 허용" 구현은 없애고 개발 중에도 실제 로그인(프로비저닝으로 만든 관리자)을 쓰게 하는 방안과, 불편할 때를 위한 안전한 대안(명시적 속성으로만 켜짐, 기본 꺼짐, dev 외 프로필에서 켜면 기동 거부)을 비교해 추천한다.
9. 공개 엔드포인트와 인증 필요 엔드포인트 목록(어떤 읽기 API 가 공개인지: languages, bundles, 공통코드 읽기, JWKS, auth/login, auth/refresh 등)을 표로 정리한다. 7·7.1단계의 읽기 API 를 계속 공개로 둘지 의견도 적는다(로그인 화면에서 다국어가 필요하므로 languages/bundles 는 공개가 필요하다는 점 고려).
10. CORS: 허용 출처를 설정으로 받고 credentials 를 허용한다. FE 개발 서버에서 프록시를 쓰는 방식과 직접 호출 방식을 비교해 추천한다(쿠키/SameSite 와의 관계 포함). FE 구조는 이후 사용자와 협의하므로 FE 쪽 결정은 하지 않는다.
11. 로그인 시도 제한(IP/계정 기준 속도 제한)을 이번에 구현할지, 다음 단계로 미룰지 의견을 적는다(새 라이브러리 없이 메모리 기반이면 가능한지 포함).
12. 보안 감사: 로그인 성공/실패, 비밀번호 변경, 토큰 재사용 감지, 잠금/해제, 운영자 로그인을 어디에 기록하는지(이미 있는 이력 테이블 우선) 정리하고, 로그에 토큰·비밀번호·쿠키 값이 남지 않게 하는 원칙을 적는다.
13. 구현 순서 제안: 예) 8.1 테넌트 사용자 로그인/refresh/logout/비밀번호 변경 + 실제 권한 검사 + SECURITY_POLICY Seed 추가 + V6(company 사업자번호 부분 유니크 포함), 8.2 플랫폼 운영자 인증 + 테넌트 관리 API(생성/프로비저닝/재발급/업그레이드), 8.3 me/내 메뉴 트리. 더 나은 분할이 있으면 제안한다.
- 문서 마지막에 "결정 필요 목록"(추천안 포함)을 둔다. 질문이 없어도 문서를 완성한다. 규칙 파일(.cursor/rules)은 수정하지 않는다(설계 담당이 갱신한다).
- 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [8단계] 로 기록한다(변경 파일: doc/auth-design.md, doc/cursor-progress.md). 완료 후 멈춘다.
```

---

## 8.1단계: 보안 인프라(Spring Security, JWT 검증·키·JWKS), 실제 권한 검사, V6

```
8단계 설계 문서를 승인한다(질문 없음). doc/auth-design.md 의 "결정 필요 목록"을 아래 결정으로 "확정 사항"으로 바꾸고, 아래 보완을 문서에 먼저 반영한 뒤 구현한다. 설계 담당(사용자 승인)의 결정:
1. 비밀번호 변경 강제 중 refresh 허용: 허용(새 Access 도 pwd_change_required=true 유지) — 8.2에서 구현.
2. dev 헤더(X-Tenant-Code)는 Authorization 이 없을 때만 쓴다. 토큰의 tenant 와 다르면 TENANT.HOST_MISMATCH.
3. 모두 허용 스위치: mdm.security.permit-all (기본 false). dev 프로필에서만 켤 수 있고 다른 프로필에서 true 면 기동 거부. 켜져 있으면 기동 시 경고 로그. 개발은 실제 로그인이 기본이다.
4. 공개 범위 수정(설계 문서 제안과 다름): 공개는 auth/login, auth/refresh, JWKS, i18n/languages, 번역 번들 중 허용 네임스페이스뿐이다. 공통코드 읽기(/codes)와 국가 목록(/nations)은 로그인 전에 필요하지 않고 테넌트 업무 데이터일 수 있으므로 "인증 필요(권한은 불필요, 로그인한 사용자 누구나)"로 바꾼다. 번들 공개는 허용 네임스페이스 목록(mdm.security.public-namespaces, 기본 common,error,auth)에 있는 네임스페이스만 공개이고, 그 밖의 네임스페이스(예: menu 등)는 인증이 필요하다. 여러 네임스페이스를 한 번에 요청할 때는 전부 공개 네임스페이스일 때만 공개이다. 이 때문에 6단계 Seed 의 네임스페이스에 auth(로그인 화면 문구용) 네임스페이스를 추가하는 Seed 변경(S04, 멱등)이 필요하면 포함한다(메시지는 비어 있어도 된다. 8.2에서 채움).
5. CORS: 개발은 FE 개발 서버 프록시를 쓰는 전제. mdm.security.cors.allowed-origins 설정과 credentials 허용만 구현한다(기본 빈 목록 = CORS 헤더 없음). 운영은 FE 와 API 가 같은 사이트(테넌트 Host 아래)라는 전제를 문서에 적는다.
6. IP 속도 제한은 이번에 넣지 않는다. 운영 배포 전에 리버스 프록시/게이트웨이 수준 제한을 점검한다는 항목을 문서의 "배포 전 점검"에 적는다.
7. 플랫폼 최초 계정: 부트스트랩 A 로 확정. 단 서버를 띄우는 모드가 아니라 "한 번 실행하고 종료하는" 실행 모드(예: --bootstrap-platform-admin 인자)로 만들어 평문이 서버 로그에 섞이지 않게 한다(8.3에서 구현). 문서에 이 형태를 반영한다.
8. 플랫폼 비밀번호 이력 테이블은 만들지 않는다(컬럼 갱신만).
9. 정책 숫자는 설계 문서 표 그대로 Seed 한다(8.2에서 Seed 추가).
[문서 보완 - 설계 문서에 추가]
- 로그인 타이밍: 계정이 없을 때도 더미 BCrypt 비교를 수행해 응답 시간 차이로 계정 존재가 드러나지 않게 한다(8.2 구현 사항으로 기록).
- 잠금 만료 뒤 첫 실패는 login_fail_count 를 1 부터 다시 센다(8.2).
- 계정 상태 반영: Access 토큰이 살아 있어도 계정이 ACTIVE 가 아니거나 use_yn='N' 이면 요청을 막는다(요청마다가 아니라 사용자 캐시 TTL 60초 이내). 오류 AUTH.ACCOUNT_DISABLED(401). 8.1에서 구현한다.
- 새 비밀번호는 현재 비밀번호와 같으면 안 된다(8.2).
- X-Forwarded-For 신뢰는 설정(mdm.security.trust-forwarded-headers, 기본 false)으로만 켠다(8.2).
- 플랫폼 운영자용 public V3(platform_admin_refresh_token 가족 인덱스)는 8.3에서 플랫폼 인증과 함께 만들고 공유 스키마 적용도 그때 승인한다. 이번 단계에서 공용 스키마(db/migration/public)를 건드리지 않는다.

[구현 범위 - 8.1]
- 의존성: spring-boot-starter-security, spring-boot-starter-oauth2-resource-server, 테스트용 spring-security-test (Spring Boot BOM 버전). 그 외 새 라이브러리는 금지.
- 설정 객체 MdmSecurityProperties(mdm.security.*): issuer, cookie.secure(기본 true), cors.allowed-origins, permit-all, public-namespaces, trust-forwarded-headers. 환경변수 MDM_JWT_PRIVATE_KEY_PATH, MDM_JWT_PREVIOUS_PUBLIC_KEY_PATH(선택). dev/test 프로필 외에서 cookie.secure=false 이면 기동 거부.
- 키 로딩(global.security): PKCS#8 PEM RSA 개인키를 경로에서 읽고 공개키는 개인키에서 유도한다(키 내용·경로 값 외의 비밀은 로그에 남기지 않는다. kid 는 공개키의 RFC 7638 지문). dev 에서 경로가 없으면 메모리 임시 RSA 2048 키(재시작 시 폐기), 그 외 프로필에서 경로가 없거나 읽기 실패면 기동 거부. 이전 공개키(X.509 PEM, 선택)가 있으면 JWKS 와 검증에 함께 쓴다.
- JWT 발급·검증 서비스(RS256, Nimbus): 발급 인터페이스(8.2/8.3에서 사용)와 검증 디코더. 검증은 서명, iss, exp, aud, typ 를 확인한다. 테넌트 체인(/api/mdm/**)은 aud=mdm-api, typ=tenant-user 만, 플랫폼 체인(/api/platform/**)은 aud=mdm-platform, typ=platform 만 받는다. 모르는 kid 는 거절. 오류 응답: 토큰 없음/형식 오류 401 AUTH.UNAUTHORIZED, 만료 401 AUTH.TOKEN_EXPIRED, aud/typ/서명 불일치 401 AUTH.TOKEN_REJECTED, 권한 부족 403 AUTH.FORBIDDEN. 모두 기존 ApiResponse 형식(traceId 포함). 토큰·쿠키 값은 로그에 남기지 않는다.
- JWKS: GET /api/mdm/v1/auth/jwks (래퍼 없음, 공개키만, Cache-Control public max-age 300). 개인키 필드(d, p, q, dp, dq, qi)가 응답에 없음을 테스트한다.
- 보안 필터 체인: 세션 없음(stateless), CSRF 끔, 위 공개 범위표 적용, 나머지 /api/** 인증 필요, /actuator/health 공개. /api/platform/** 체인은 aud/typ 규칙과 인증 필요만 만든다(엔드포인트는 8.3).
- 필터 순서: Security → TenantResolutionFilter(Security 뒤로 이동) → (8.2의 비밀번호 변경 강제 필터 자리) → PermissionInterceptor. TenantResolutionFilter 는 /actuator/**, /api/platform/**, JWKS 를 건너뛴다. UnavailableTenantClaimReader 는 SecurityContext 의 검증된 JWT(typ=tenant-user)에서 tenant 클레임을 읽는 구현으로 교체한다(서명 검증 전 페이로드를 읽지 않는다). 토큰이 있으면 Host 와 대조(불일치 TENANT.HOST_MISMATCH), 토큰이 없으면 Host 또는 dev 헤더.
- 계정 상태 확인: 테넌트 사용자 토큰이 인증된 요청에서 사용자 행(user_status_code, use_yn)을 사용자 단위 메모리 캐시(TTL 60초)로 확인하고 ACTIVE 가 아니면 401 AUTH.ACCOUNT_DISABLED. 이 캐시는 비밀번호 재발급 서비스 등 계정을 바꾸는 쪽에서 무효화할 수 있게 한 곳에 둔다(재발급 서비스가 호출하도록 연결).
- 권한 검사 실제 구현: DenyAllPermissionChecker/AllowAllPermissionChecker 를 제거하고 DB 기반 PermissionChecker 로 교체한다. 규칙은 auth-design.md "권한" 절 그대로(user_role 유효기간은 TenantClock 의 오늘, use_yn='Y' 전부, permission_code 일치, READ 이외는 같은 메뉴 READ 도 필요). 캐시는 (테넌트 스키마, user_pk) 키, TTL 60초, 무효화 메서드 제공. permit-all 속성이 true(dev 한정)면 검사를 건너뛰고 경고를 남긴다. 현재 사용자는 SecurityContext 의 sub 이다. 인증이 없는 호출은 AUTH.UNAUTHORIZED.
- 기존 테스트 전환: dev 프로필의 "모두 허용"에 기대던 통합 테스트(CodeApi*, I18nApi*, 프로비저닝 관련 API 호출 등)는 테스트 전용 토큰 발급 헬퍼(JwtService 로 aud/typ/tenant/sub 를 넣어 발급)와 테스트에서 만든 사용자·역할·권한 행을 쓰도록 바꾼다. 로그인 API 는 아직 없다. permit-all 은 꼭 필요한 곳에만 쓰고 이유를 주석으로 적는다.
- V6(테넌트): company.business_registration_no 부분 유니크를 WHERE business_registration_no IS NOT NULL AND use_yn='Y' 로 교체, user_refresh_token(token_family_id) 인덱스 추가. 엔티티-DDL validate 와 재등록 통합 테스트 포함. 공유 DB 스키마 pmis 에는 아무것도 적용하지 않는다(테넌트 SQL 은 테넌트 스키마용).
- doc/api/mdm-api.md 에 공개/인증 필요 표, 보안 오류 코드, JWKS 를 추가한다. 오류 코드 문구(ko-KR/en-US)를 S04 Seed 에 추가한다(멱등).

[테스트]
- 단위/슬라이스: 키 로딩 성공·실패·dev 임시 키·비 dev 거부, kid 지문 결정성, 발급-검증 왕복, 만료/aud/typ/서명 변조/모르는 kid 거절 코드, 이전 공개키 병행 검증, JWKS 내용, 공개 범위표의 각 행(토큰 없이 200/401), 공개 네임스페이스 외 번들 401, 시작 거부 조합(비 dev 에서 permit-all=true, 비 dev 에서 cookie.secure=false).
- 통합(@Tag("integration"), 3.2단계 절차, t_it_<랜덤>, 테넌트는 프로비저닝 서비스로 ACTIVE): (a) 유효 토큰 + 올바른 권한으로 관리 API 200 (b) 권한 없음 403 AUTH.FORBIDDEN (c) 역할 유효기간 밖, 역할/권한/메뉴 use_yn='N', READ 없이 쓰기 권한만 있는 경우 모두 거부 (d) 계정을 LOCKED/use_yn='N' 으로 바꾸면 캐시 TTL 안에서 401 AUTH.ACCOUNT_DISABLED (테스트에서는 캐시 무효화 메서드 호출 또는 짧은 TTL 설정) (e) 토큰 tenant 와 Host/요청 테넌트 불일치 TENANT.HOST_MISMATCH (f) 두 테넌트에서 같은 user_pk 값이라도 다른 테넌트 권한이 섞이지 않는지 (g) V6 로 삭제한 회사의 사업자번호 재등록 가능 + 사용 중 중복은 거절 (h) 엔티티 validate (V1~V6). 테스트가 만든 행과 스키마를 정리하고 pmis 업무 테이블 0행 확인. 기존 20건 통합 테스트도(전환 후) 통과해야 한다.
- 질문이 있으면 해당 부분만 멈추고 진행 문서 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [8.1단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 8.2단계: 테넌트 사용자 인증 API와 보안 정책 Seed

```
8.1단계 결과를 승인한다(질문 없음, 가정 모두 승인: dev 밖 기동 거부 조합, /codes·/nations 인증만, 공개 번들 기본 common·error·auth, 캐시 60초, 재발급 시 계정 캐시를 커밋 뒤 무효화, READ 없는 다른 기능 403). doc/auth-design.md 를 기준으로 구현한다. 아래는 문서에 없거나 문서와 다르게 정하는 사항이다. 문서에 먼저 반영한다.
1. 동시 refresh 처리: 브라우저 탭 두 개가 같은 refresh 토큰으로 동시에 요청하면 두 번째가 "재사용"으로 보이는 문제가 있다. 회전할 행을 행 잠금(SELECT ... FOR UPDATE)으로 직렬화하고, 이미 ROTATED 로 폐기된 토큰이 폐기 후 mdm.security.refresh-reuse-grace-seconds(기본 10초) 이내에 다시 오면 가족을 폐기하지 말고 409 AUTH.REFRESH_IN_PROGRESS 로 돌려준다(브라우저에는 이미 새 쿠키가 있으므로 FE 가 한 번 더 시도하면 된다). 유예 시간이 지난 재사용만 REUSE_DETECTED 로 가족 전체를 폐기한다. 유예 후의 로그아웃/비밀번호 변경으로 폐기된 토큰은 유예 대상이 아니다.
2. refresh 시 사용자 상태 확인: 계정이 ACTIVE 가 아니거나 use_yn='N' 이면 401 AUTH.ACCOUNT_DISABLED 이고 그 가족을 폐기한다(reason ADMIN).
3. 정책 값 검사 확대: 7단계의 SECURITY_POLICY 값 검사(CODE.INVALID_POLICY_VALUE)를 아래 모든 키로 넓힌다. LOCK_MINUTES 만 0 을 허용하고(0 = 자동 해제 없음) 나머지는 양의 정수다. PASSWORD_MIN_CLASSES 는 1~4, PASSWORD_MIN_LENGTH 는 8 이상, PASSWORD_HISTORY_COUNT 는 1 이상이다. 삭제(논리 삭제)도 같은 규칙으로 거절한다.
4. 임시 비밀번호가 비밀번호 정책(길이 10, 4종류)을 이미 만족하는지 확인하고, 만족하지 못하면 질문으로 올린다(생성기는 16자 4종류이므로 만족해야 한다).
5. 비밀번호 정책 검사 순서: 현재 비밀번호 일치 → 새 비밀번호가 현재 비밀번호와 같은지(같으면 400 AUTH.PASSWORD_SAME) → 길이/문자 종류(400 AUTH.PASSWORD_POLICY, fieldErrors 에 사유) → 최근 PASSWORD_HISTORY_COUNT 개(현재 해시 포함) 재사용(400 AUTH.PASSWORD_REUSED). 로그인 ID 를 그대로 포함하는 비밀번호는 거절한다(AUTH.PASSWORD_POLICY).

[구현 범위 - auth-design.md 의 8.2 범위]
- Seed(S03/S04, 멱등 NOT EXISTS): SECURITY_POLICY 그룹에 PASSWORD_EXPIRE_DAYS(90), PASSWORD_MIN_LENGTH(10), PASSWORD_MIN_CLASSES(3), PASSWORD_HISTORY_COUNT(5), MAX_LOGIN_FAIL_COUNT(5), LOCK_MINUTES(30), ACCESS_TOKEN_MINUTES(15), REFRESH_TOKEN_DAYS(14) 를 ko-KR/en-US 이름·설명과 함께 추가(TEMP_PASSWORD_VALID_DAYS 는 이미 있음). 일괄 업그레이드가 기존 테넌트에도 추가한다. auth 네임스페이스 메시지(로그인 화면 문구: 제목, 아이디, 비밀번호, 로그인 버튼, 비밀번호 변경 화면 문구, 흔한 안내 문구)와 새 오류 코드 문구(ko-KR/en-US)를 넣는다. 문구 키는 auth.login.title 처럼 점 표기다.
- 엔드포인트(/api/mdm/v1/auth): login, refresh, logout, password/change, me. 요청·응답·쿠키·CSRF 헤더(X-MDM-Request: 1, 없으면 403 AUTH.CSRF)·토큰 클레임(pwd_change_required 포함)은 auth-design.md 그대로. 토큰 유효시간은 ACCESS_TOKEN_MINUTES/REFRESH_TOKEN_DAYS 값을 해당 테넌트 스키마에서 읽는다. 정책이 없으면 로그인은 500 TENANT.SECURITY_POLICY_MISSING.
- 로그인 보안: 계정 없음/불일치/비활성/잠금을 같은 401 AUTH.INVALID_CREDENTIALS 로 응답하고 내부 LOGIN_RESULT 값으로 user_login_history 에 기록한다(IP, User-Agent 500자). 계정이 없을 때도 더미 BCrypt 비교를 해서 응답 시간 차이를 줄인다. 연속 실패 잠금(MAX_LOGIN_FAIL_COUNT), 잠금 만료(LOCK_MINUTES) 후 첫 성공 로그인은 ACTIVE 로 복귀, 잠금 만료 뒤 첫 실패는 카운트를 1 부터 다시 센다. 성공 시 login_fail_count=0, last_login_datetime 갱신. 동시 로그인 실패 카운트는 UPDATE ... SET login_fail_count = login_fail_count + 1 같은 원자적 갱신으로 한다. X-Forwarded-For 는 mdm.security.trust-forwarded-headers=true 일 때만 신뢰한다.
- Refresh 토큰: 256비트 난수 원문은 쿠키로만, DB 에는 SHA-256 해시만 저장(user_refresh_token.token_hash). 회전, 가족(token_family_id), 폐기 사유(LOGOUT/ROTATED/REUSE_DETECTED/ADMIN/PASSWORD_CHANGED), 비밀번호 변경·재발급·계정 잠금 시 해당 사용자 refresh 전체 폐기(재발급 서비스에 연결). 쿠키 속성은 설계 문서 표대로(이름 mdm_refresh, Path /api/mdm/v1/auth, HttpOnly, Secure 설정값, SameSite=Lax).
- 비밀번호 변경 강제 필터(필터 순서 자리): pwd_change_required=true 토큰은 password/change, logout, me, refresh 외 /api/mdm/** 를 403 AUTH.PASSWORD_CHANGE_REQUIRED 로 막는다.
- 오류 코드와 ko-KR/en-US 문구를 Seed 에 추가(AUTH.INVALID_CREDENTIALS, AUTH.CSRF, AUTH.TOKEN_EXPIRED, AUTH.TOKEN_REUSED, AUTH.REFRESH_IN_PROGRESS, AUTH.ACCOUNT_DISABLED, AUTH.PASSWORD_*, AUTH.PASSWORD_CHANGE_REQUIRED 등 문서에 정의된 것 전부). 8.1에서 이미 있는 것은 중복하지 않는다.
- me: userPk, loginId, displayName, supportLanguageCode, timezoneId, roleCodes, passwordChangeRequired.
- 로그에 비밀번호, Access, Refresh, 쿠키, Authorization 헤더 값이 남지 않는지 로그 어펜더 캡처 테스트로 확인한다.
- doc/api/mdm-api.md 에 인증 API 요청/응답 예시, 쿠키, CSRF 헤더, 오류 코드를 추가하고 FE 가 알아야 할 흐름(로그인 → 만료 시 refresh → 실패 시 로그인 화면, 409 AUTH.REFRESH_IN_PROGRESS 재시도)을 "FE 연동 시나리오"로 정리한다. FE 구조 결정은 하지 않는다.
- 새 라이브러리는 추가하지 않는다. 8.1의 JwtTokenService/DbPermissionChecker/AccountStatusCache 를 재사용한다.

[테스트]
- 단위: 정책 검사(길이, 종류, 같은 비밀번호, 이력 재사용, 로그인 ID 포함), 잠금 계산(만료 전/후, 카운트 재시작), 쿠키 속성, 더미 BCrypt 경로 호출 여부, refresh 유예 판정(유예 내/외).
- 통합(@Tag("integration"), 3.2단계 절차, t_it_<랜덤>, 테넌트는 프로비저닝 서비스로 ACTIVE, 프로비저닝 결과의 임시 비밀번호로 로그인): (a) 최초 로그인은 passwordChangeRequired=true 이고 허용 목록 밖 API 는 403 AUTH.PASSWORD_CHANGE_REQUIRED (b) 비밀번호 변경 후 새 Access(pwd_change_required=false)와 새 refresh 쿠키, 이전 refresh 전부 PASSWORD_CHANGED 폐기, 이력 CHANGE (c) 틀린 비밀번호 N회 → LOCKED, 올바른 비밀번호도 잠금 중에는 같은 401, LOCK_MINUTES 경과(정책값을 작게 바꾸거나 시간 조작)후 성공 로그인으로 ACTIVE 복귀 (d) 존재하지 않는 계정과 틀린 비밀번호의 응답 본문이 같음 (e) refresh 회전, 재사용 감지로 가족 폐기, 유예 내 재사용은 409 이고 가족 유지 (f) 계정을 중지하면 refresh 거절 (g) CSRF 헤더 없으면 403 (h) 로그아웃 후 refresh 불가, 쿠키 삭제 (i) 두 테넌트에서 같은 login_id 가 서로 섞이지 않고 다른 테넌트 Host 로 토큰 사용 시 TENANT.HOST_MISMATCH (j) 임시 비밀번호 재발급 후 이전 비밀번호·refresh 가 무효 (k) 로그에 비밀 값이 없음 (l) 일괄 업그레이드가 기존 테넌트에 정책 키를 추가하되 바뀐 값은 유지. 테스트가 만든 모든 행과 스키마를 정리하고 pmis 업무 테이블 0행을 확인한다. 기존 21건 통합 테스트도 통과해야 한다.
- 질문이 있으면 해당 부분만 멈추고 진행 문서 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [8.2단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 8.3단계: 플랫폼 운영자 인증, 최초 운영자 부트스트랩, 테넌트 관리 API, 공용 V3

```
8.2단계 결과를 승인한다(질문 없음, 가정 모두 승인). 단, 8.2 인증 통합 테스트가 2건뿐이므로 이번 단계 진행 문서에 "8.2 인증 테스트 커버리지 표"를 포함한다: 8.2 지시문 테스트 (a)~(l) 각 항목이 어느 테스트 클래스·메서드에서 검증되는지 대응표를 쓰고, 빠진 항목이 있으면 이번 단계에서 테스트를 보강한다(보안 핵심 기능이므로 건너뛰지 않는다).
설계 담당(사용자 승인)의 결정:
1. 공용 스키마 V3(db/migration/public/V3__platform_admin_refresh_family_and_password_expire.sql): ① platform_admin_refresh_token(token_family_id) 인덱스 ② platform_admin.password_expire_datetime TIMESTAMP NULL 컬럼 추가(ERD 정본 doc/erd/erd-model-tenant-20261009.json 에 반영 완료. Cursor 는 ERD 파일을 수정하지 않는다. 임시/초기화 비밀번호에 유효 기간을 두기 위한 것). 엔티티 매핑/validate 포함.
2. 이 단계의 끝에서 공유 DB 스키마 pmis 에 공용 V3 적용을 승인한다. 4.3단계와 같은 절차를 그대로 지킨다: 적용 전 읽기 전용 확인(DB 이름 pmis, current_database(), flyway_schema_history 가 V1·V2 success 이고 8개 업무 테이블이 있고 0행인지, V3 가 아직 없는지), 이 프로세스에서만 MDM_FLYWAY_ENABLED=true 로 기동(영구 설정 금지, 접속 정보 기록 금지), 적용 후 이력 V3 success·컬럼/인덱스 존재 확인, 다른 스키마 불변, 전체 통합 테스트 재실행, 테스트 행 잔여 없음 확인. 적용은 모든 테스트가 통과한 뒤 마지막에 한다. clean/drop 금지. 문제가 생기면 상태를 보고하고 멈춘다.
3. 플랫폼 운영자 인증(/api/platform/v1/auth): login, refresh, logout, password/change, me. 8.2 테넌트 인증과 같은 동작(통일된 로그인 오류 AUTH.INVALID_CREDENTIALS, 더미 BCrypt, 잠금, 쿠키 회전과 유예 409, CSRF 헤더, 비밀번호 변경 강제 필터)이다. 차이: 공용 스키마의 platform_admin 계열 테이블, 토큰 typ=platform / aud=mdm-platform / tenant 클레임 없음, 쿠키 mdm_platform_refresh(Path /api/platform/v1/auth), 정책값은 공통코드가 아니라 설정 mdm.security.platform.*(access-token-minutes 15, refresh-token-days 14, max-login-fail-count 5, lock-minutes 30, password-min-length 12, password-min-classes 3, password-expire-days 90, temp-password-valid-days 3). 테넌트 필터를 타지 않는다. me 에는 역할 없음. 비밀번호 이력 테이블이 없으므로 이전 비밀번호와 같은 값만 거절한다. 8.2와 중복되는 로직(잠금 계산, 쿠키, 회전 규칙, 정책 검사)은 가능한 한 공유 컴포넌트로 합치되, 과한 추상화로 테넌트 쪽 동작을 바꾸지 않는다(기존 테스트가 그대로 통과해야 한다).
4. 부트스트랩: 서버를 띄우지 않는 일회성 실행 모드(--bootstrap-platform-admin 실행 인자 또는 동등한 방식, 웹 서버 비활성)로 구현한다. 환경변수 MDM_BOOTSTRAP_ADMIN_LOGIN(필수), MDM_BOOTSTRAP_ADMIN_NAME, MDM_BOOTSTRAP_ADMIN_EMAIL(선택). platform_admin 에 행이 0건일 때만 생성하고, 있으면 아무것도 바꾸지 않고 오류 종료(0이 아닌 종료 코드). DB 이름 pmis 검사를 통과해야 한다. 임시 비밀번호(20자, 4종류, SecureRandom)를 표준 출력에 한 번만 출력하고(로거 사용 금지, 로그 파일에 남기지 않음) 해시만 저장한다. must_change_password_yn='Y', password_expire_datetime=지금+temp-password-valid-days. 운영 절차(출력 캡처 주의, 즉시 변경)를 doc/auth-design.md 와 README 성격의 운영 문서에 적는다. 이 모드의 자동 테스트는 실제 공유 DB 가 아닌 테스트용 스키마/트랜잭션 격리 방식으로 하고, 공유 pmis 에 운영자 행을 만들지 않는다(테스트가 만들었다면 정리).
5. 테넌트 관리 API(/api/platform/v1, 플랫폼 토큰 필수, 활성 운영자면 모두 허용): 
   - GET /tenants(페이징 7단계 규약, 필터 status/keyword), GET /tenants/{tenantCode}(도메인, 프로비저닝 단계, 마지막 마이그레이션 요약 포함).
   - POST /tenants: tenantCode, tenantName, defaultLanguageCode, timezoneId, contractStartDate, contractEndDate(선택), adminName, adminEmail, domains(1개 이상, 호스트명). 스키마명은 규칙 함수로 만든다(요청으로 받지 않는다). tenant_domain 을 만들고(첫 도메인 primary_yn='Y', verified_yn='Y' 로 시작하되 도메인 검증은 범위 밖) 프로비저닝 서비스를 동기 실행한다. 응답에 프로비저닝 결과와 관리자 임시 비밀번호를 한 번만 담는다(Cache-Control: no-store, 응답 로깅·감사 기록에 평문 제외). 도메인은 형식 검증(소문자 호스트명, 길이, 중복 도메인 409).
   - POST /tenants/{tenantCode}/provisioning/retry: FAILED 또는 RUNNING 정지 상태의 프로비저닝을 이어서 실행.
   - POST /tenants/{tenantCode}/admin-password/reissue: 8.2의 재발급 서비스를 호출, 임시 비밀번호 1회 응답(no-store).
   - POST /tenants/{tenantCode}/suspend, /resume: 상태 전환(ACTIVE↔SUSPENDED, 사유 필수 changeReason), tenant_status_history 기록(change_user_pk=운영자 PK), TenantDirectory 캐시 즉시 무효화. 불가능한 전환은 409 TENANT.INVALID_STATUS_TRANSITION. TERMINATED 전환(폐기)은 이번 범위가 아니다.
   - POST /tenants/{tenantCode}/domains, DELETE /tenants/{tenantCode}/domains/{domainName}(논리 삭제, 마지막 활성 도메인은 삭제 불가 409 TENANT.LAST_DOMAIN, primary 도메인 변경 규칙 포함), GET 도메인 목록. 캐시 무효화.
   - POST /tenants/{tenantCode}/upgrade, POST /tenants/upgrade(ACTIVE 전체): 4.4의 일괄 업그레이드 서비스 호출, 요청 시간 안에 동기 실행하고 결과 요약(테넌트별 SUCCESS/FAILED/건너뜀)을 돌려준다. 오래 걸릴 수 있다는 점과 비동기 전환 여부를 문서의 "후속 과제"에 적는다.
   - GET /tenants/{tenantCode}/migrations(페이징): tenant_schema_migration 이력.
   - 모든 변경 작업의 실행 운영자 PK 를 변경 사용자 컬럼에 남긴다(JWT sub). 응답 본문, 로그, 이력의 error_message 에 임시 비밀번호·토큰·쿠키가 없어야 한다.
   - 오류 문구: 플랫폼 API 는 테넌트 스키마가 없어 i18n 번들을 쓸 수 없다. 오류 코드별 한글 기본 메시지(코드에 정의)를 쓰고, Accept-Language 로 en 이면 영문 메시지도 코드 안 상수로 제공한다(둘 다 코드 내 정의, 번들 조회 없음). 이 방식을 문서에 적는다.
6. 도메인 → 테넌트 식별 통합 검증: POST /tenants 로 만든 테넌트의 도메인을 Host 헤더로 지정해 테넌트 사용자 로그인(8.2)이 성공하는 흐름을 통합 테스트로 확인한다(dev 헤더 없이 Host 만 사용).
7. doc/api/ 에 플랫폼 API 문서(doc/api/platform-api.md)를 만든다. 요청/응답 예시, 오류 코드, 플랫폼 인증 흐름, 임시 비밀번호 1회 응답 규칙, 후속 과제(비동기 업그레이드, 도메인 검증, 테넌트 폐기)를 쓴다.

[테스트]
- 단위: 플랫폼 정책 검사, 부트스트랩 조건(0건일 때만), 상태 전환 규칙표(허용/불허 모든 조합), 도메인 형식 검증, 임시 비밀번호 마스킹 유지.
- 통합(@Tag("integration"), 3.2단계 절차, 테넌트는 t_it_<랜덤> 스키마. 플랫폼 운영자 행은 테스트가 만들고 정리한다): (a) 플랫폼 로그인/refresh/로그아웃/비밀번호 변경/강제 필터/잠금/CSRF/회전·유예 (b) 테넌트 토큰으로 /api/platform/** 호출, 플랫폼 토큰으로 /api/mdm/** 업무 API 호출 모두 401 AUTH.TOKEN_REJECTED (c) POST /tenants → ACTIVE, 임시 비밀번호 1회 응답, 응답 헤더 no-store, 그 임시 비밀번호로 Host 만 이용한 테넌트 로그인 성공 (d) 같은 tenantCode/도메인 중복 409 (e) 프로비저닝 실패 주입 후 retry 로 완료 (f) suspend 직후 테넌트 요청 거절, resume 후 복구(캐시 무효화 확인) (g) 도메인 추가/삭제/마지막 도메인 삭제 거절 (h) reissue 로 이전 비밀번호 무효 (i) 일괄 업그레이드 요약 (j) 페이징·정렬 화이트리스트 (k) 로그/응답/이력에 평문 비밀번호·토큰 없음(로그 캡처). 테스트가 만든 모든 tenant 관련 행·운영자 행·스키마를 정리하고 공용 pmis 업무 테이블 0행(적용 후 platform_admin 포함)을 확인한다. 기존 23건 통합 테스트도 통과해야 한다.
- 새 라이브러리는 추가하지 않는다. 질문이 있으면 해당 부분만 멈추고 진행 문서 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [8.3단계] 로 기록한다(공용 V3 적용 결과 포함, 접속 정보 기록 금지). 완료 후 멈춘다.
```

---

## 8.4단계: 내 메뉴(menus/my), 공용 V4(도메인 재등록), 공용 유니크 점검

```
8.3단계 결과를 승인한다(질문 없음, 가정 모두 승인). 8.2 인증 테스트가 한 메서드에 몰려 있다는 점은 기술 부채로 기록만 하고 이번에는 바꾸지 않는다(실패 시 원인 파악이 어려워지면 그때 분리). 설계 담당(사용자 승인)의 결정:
1. GET /api/mdm/v1/menus/my (인증 필요, 권한 선언 없음 = 로그인한 사용자 누구나). 비밀번호 변경 강제 토큰은 기존 규칙대로 403.
   - 응답 data = 배열(건수가 적은 코드성 목록 규칙). 항목: menuCode, parentMenuCode(없으면 null), menuName(현재 언어), menuTypeCode, routePath, iconName, levelNo, sortOrder, actions(이 사용자가 가진 기능 코드 배열. 예: ["READ","CREATE","UPDATE"]).
   - 노출 규칙: use_yn='Y' 이고 visible_yn='Y' 인 메뉴. 화면(PAGE) 메뉴는 사용자가 READ 권한을 가진 경우만 포함. 그룹(GROUP) 메뉴는 노출 가능한 하위 메뉴가 하나라도 있을 때만 포함(하위가 모두 빠지면 그룹도 제외). 권한 계산은 DbPermissionChecker 와 같은 규칙과 캐시(user_role 유효기간, role_permission 합집합, "READ 이외는 같은 메뉴 READ 필요")를 재사용하고 규칙을 복제하지 않는다. actions 에는 READ 가 없는 메뉴가 나오지 않으므로 항상 READ 가 포함된다. 그룹 메뉴의 actions 는 ["READ"] 로 둔다.
   - 정렬: levelNo, sortOrder, menuCode 순(안정 정렬). FE 가 트리를 조립한다.
   - 메뉴 이름: 6단계 Seed 의 menu 네임스페이스 번역을 쓰고(키 규칙은 6단계에서 만든 것을 따르고 문서에 명시), 번역이 없으면 menu.menu_name 으로 대체한다. 언어 결정은 lang > Accept-Language > 테넌트 기본 언어 이며 대체 언어 체인을 따른다. 번들 캐시를 재사용한다.
   - ETag(내용 해시)/304 는 지원하지 않는다(사용자별 응답, Cache-Control: no-store).
   - 메뉴를 바꾸는 관리 API 는 이번 범위가 아니다. 단 메뉴/역할 변경 시 권한 캐시 무효화 지점은 기존대로 둔다.
2. 공용 V4(db/migration/public/V4__tenant_domain_partial_unique.sql): 공용 스키마의 전체 유니크 중 7.1단계와 같은 원칙(논리 삭제 후 재등록이 막히는지)으로 점검한다. 확정: tenant_domain.domain_name 은 use_yn='Y' 조건의 부분 유니크로 바꾼다(도메인 이관/재사용 허용). 같은 원칙으로 점검해 바꿀지 판단하되 tenant_code, schema_name, platform_admin.login_id 같은 변경 불가 자연키는 전체 유니크를 유지한다. 점검 결과를 7.1단계와 같은 표로 진행 문서에 올린다(판단이 애매하면 바꾸지 말고 질문). 8.3의 도메인 삭제/재등록 로직은 부분 유니크에 맞게 조정하고 테스트를 갱신한다(삭제한 도메인을 같은 테넌트 또는 다른 테넌트에 다시 등록 가능, 사용 중 중복은 409).
3. 공용 V4 의 공유 스키마 pmis 적용을 승인한다. 8.3의 V3 적용과 같은 절차를 그대로 지킨다(적용 전 읽기 전용 확인: 현재 V1~V3 success 이고 V4 없음, 업무 테이블 0행/또는 기존 데이터 건수를 기록, 테스트용 행 정리; 이 프로세스에서만 MDM_FLYWAY_ENABLED=true; 적용 후 확인; 전체 테스트 재실행; clean/drop 금지; 접속 정보 기록 금지). 적용은 모든 테스트가 통과한 뒤 마지막에 한다.
4. doc/api/mdm-api.md 에 menus/my 요청/응답 예시, 노출 규칙, 오류 코드를 추가하고 doc/api/platform-api.md 의 도메인 규칙을 갱신한다. FE 구조 협의 결과를 반영해 "FE 연동 시나리오"에 메뉴 로딩 순서(로그인 → me → menus/my → 라우트 필터)를 추가한다.
5. 운영자 부트스트랩의 실제 실행은 하지 않는다(사용자가 직접 한다). doc/platform-admin-bootstrap.md 에 "개발 DB 에서 첫 운영자를 만드는 순서"를 따라 하기 쉬운 단계별 명령 형태(환경변수 이름만, 값 없음)로 보강한다.

[테스트]
- 단위: 메뉴 노출 규칙(그룹 제외 규칙, READ 없는 메뉴 제외, 비노출 메뉴), 정렬 안정성.
- 통합(@Tag("integration"), 3.2단계 절차, t_it_<랜덤>, 프로비저닝 서비스로 만든 테넌트): (a) TENANT_ADMIN 이 Seed 의 전체 메뉴와 각 actions 를 받는다 (b) 6단계 Seed 의 MANAGER/USER 역할 사용자가 각자 허용된 메뉴만 받고 ROLE/MENU/PERMISSION/USER 메뉴가 없다 (c) READ 만 있는 메뉴의 actions 는 ["READ"] (d) 메뉴 use_yn='N' 또는 visible_yn='N' 이면 빠지고 하위가 모두 빠지면 부모 그룹도 빠진다 (e) 역할 유효기간 밖이면 빠진다 (f) ko-KR/en-US 이름과 번역 없을 때 menu_name 대체 (g) 비밀번호 변경 강제 토큰은 403 (h) 두 테넌트 분리 (i) 공용 V4 로 삭제된 도메인 재등록 가능 + 사용 중 중복 409 (j) 엔티티 validate. 정리 및 pmis 업무 테이블 0행 확인. 기존 26건 통합 테스트도 통과해야 한다.
- 새 라이브러리는 추가하지 않는다. 질문이 있으면 해당 부분만 멈추고 진행 문서 질문 목록에 올린다. 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 [8.4단계] 로 기록한다(공용 V4 적용 결과 포함). 완료 후 멈춘다.
```

---

## 8.5단계: 로컬 개발 설정 파일(application-local.yaml)과 local 프로필

사용자 결정(2026-10-11): 로컬 개발 속도를 위해 DB 접속 정보를 환경변수 대신 로컬 파일로 줄 수 있게 한다. 개발서버·운영은 계속 환경변수만 쓴다. local 프로필은 사용자가 직접 관리하는 서버이므로 접속 정보 취급을 완화하되, 값이 문서·커밋·채팅에 남지 않게 하고 파괴적 동작 금지는 유지한다.

```
목표: 로컬에서는 `config/application-local.yaml`(저장소에 올리지 않는 파일)에 사용자가 직접 적은 값으로 기동하고, 개발서버/운영은 환경변수로만 기동한다. 코드 변경은 최소로 한다.

보안 규칙 (사용자 결정 2026-10-11: 로컬 프로필은 사용자가 직접 관리하는 서버이므로 완화한다):
- `config/application-local.yaml`은 로컬 개발 전용이며, 너는 이 파일을 읽고 수정할 수 있다(값 확인, 기동 확인, 값 변경 포함). 사용자가 값을 채우지 않았거나 필요한 키가 빠져 있으면 사용자에게 값을 요청하지 말고 키 이름만 알려 준다. 값은 사용자가 입력한다.
- 단, 이 파일의 값(접속 URL, 계정, 비밀번호)을 진행 기록(doc/*.md), 커밋, 예시 파일, 테스트 출력, 채팅 응답에 복사하지 않는다. 문서에는 키 이름과 `<여기에 입력>` 같은 자리표시자만 쓴다. 이 파일을 읽은 사실과 결과(접속 성공/실패)만 보고한다.
- 이 파일은 .gitignore에 반드시 포함한다(경로 `config/application-local.yaml`과 `config/*.local.*`). git이 없더라도 .gitignore 항목은 추가한다.
- `local` 프로필로 기동하는 것(`gradlew.bat bootRun`)과 읽기 전용 확인은 사용자에게 매번 대상 DB를 묻지 않고 수행해도 된다. 그래도 접속 URL, 계정, 비밀번호가 기동 로그나 예외 메시지에 출력되지 않게 마스킹한다.
- 파괴적 동작 금지는 그대로 유지한다: Flyway 자동 실행은 기본 꺼짐(`MDM_FLYWAY_ENABLED` 또는 local 파일의 명시적 설정이 켜야 실행하며, 켜는 경우 보고한다), clean/drop 금지, 테스트는 `it_`/`t_it_` 스키마만, 부트스트랩의 current_database()='pmis' 검사, 기존 데이터 삭제·변경은 사용자 승인 후.

작업:
1. 설정 로딩
   - `spring.config.import: optional:file:./config/application-local.yaml`을 local 프로필에서만 쓰도록 구성한다(파일 없으면 오류 없이 넘어가고 환경변수로 동작). 작업 디렉터리가 `backend/common`일 때와 IDE에서 실행할 때 모두 파일을 찾는지 확인하고, 못 찾는 경우의 해결(경로 기준)을 보고한다.
   - 우선순위는 환경변수 > 로컬 파일 > 기본값이다. 로컬 파일은 같은 키(`mdm.db.url`, `mdm.db.username`, `mdm.db.password` 등 현재 사용하는 설정 키 이름 그대로)를 쓴다. 현재 환경변수 이름(MDM_DB_URL 등)이 어떤 설정 키로 매핑되는지 확인하고 로컬 파일이 같은 키를 덮어쓸 수 있게 한다. 새로운 환경변수 이름은 만들지 않는다.
   - 값이 없거나 URL이 jdbc: 로 시작하지 않으면, 접속 정보를 출력하지 않고 "DB 접속 설정이 없습니다. config/application-local.yaml 또는 환경변수 MDM_DB_URL/MDM_DB_USERNAME/MDM_DB_PASSWORD를 설정하세요" 같은 명확한 기동 오류 한 줄을 낸다(지금의 'url must start with jdbc' 같은 불친절한 스택 대신). 값 자체는 오류 메시지에 넣지 않는다.
2. local 프로필
   - `local` 프로필은 `dev` 프로필 설정(임시 서명 키 생성, dev 전용 X-Tenant-Code 허용 등)을 모두 포함한다(프로필 그룹 `spring.profiles.group.local: dev` 등 가장 단순한 방법). 중복 설정을 만들지 않는다.
   - Gradle `bootRun` 작업만 기본 프로필을 `local`로 한다(`bootRun` 설정에서 spring.profiles.active 지정). 애플리케이션 jar의 기본 프로필은 바꾸지 않는다. 즉 운영에서 프로필을 지정하지 않아도 local/dev 동작이 켜지지 않아야 한다. 명시적으로 `--spring.profiles.active=...`를 주면 그것이 우선한다.
   - 포트: local 프로필에서도 8080 기본이며 `--server.port`나 로컬 파일의 `server.port`로 바꿀 수 있어야 한다.
3. 예시 파일과 문서
   - `config/application-local.yaml.example`: 값 없이 키와 주석만(자리표시자는 `<여기에 입력>` 같은 명백한 문구). 접속 URL 형식 예시는 `jdbc:postgresql://<host>:<port>/pmis`처럼 자리표시자만 쓴다.
   - `doc/local-setup.md`(없으면 생성): 1) 예시 파일을 복사해 application-local.yaml을 만들고 값을 채우는 방법 2) `gradlew.bat bootRun`으로 실행 3) 환경변수가 있으면 파일보다 우선 4) 파일을 공유·클라우드 동기화 폴더에 두지 말 것 5) Windows PowerShell 실행 예시 6) 플랫폼 관리자 부트스트랩 시에도 같은 파일을 쓴다는 설명(`bootRun --args="--bootstrap-platform-admin"`; 이 모드는 DB 이름 pmis 검사 유지) 7) 통합 테스트는 계속 환경변수 MDM_TEST_DB_*를 쓴다. 값은 문서 어디에도 적지 않는다.
   - doc/platform-admin-bootstrap.md와 doc/api 문서의 해당 안내를 local 프로필 기준으로 짧게 갱신한다.
4. 테스트
   - 단위 테스트(DB 없이): (a) 로컬 파일이 없을 때 환경변수만으로 설정이 해결되는 것 (b) 환경변수가 로컬 파일 값보다 우선하는 것(테스트용 임시 파일은 테스트 리소스/임시 디렉터리에 만들고 값은 더미) (c) 필수 접속 설정이 비었거나 jdbc: 로 시작하지 않을 때 값이 노출되지 않는 명확한 오류 (d) local 프로필이 dev 설정을 포함 (e) 프로필을 지정하지 않은 기본 기동에서 dev/local 전용 동작(X-Tenant-Code 허용, 임시 키 생성)이 꺼져 있음.
   - 기존 단위 48개와 통합 29개가 계속 통과해야 한다. 통합 테스트는 기존대로 MDM_TEST_DB_* 환경변수를 쓴다.
5. 검증
   - gradlew.bat test, integrationTest(통합 테스트는 기존대로 MDM_TEST_DB_* 환경변수를 쓴다. 값은 기록하지 않는다). local 파일이 채워져 있으면 `bootRun`으로 실제 기동해 DB 연결과 `Started` 로그, 기본 프로필 기동(local 아님)에서 접속 설정 누락 시 안내 오류가 나는 것을 확인하고 결과만 보고한 뒤 서버를 종료한다. local 파일이 비어 있으면 이 확인은 건너뛰고 사용자가 한다고 적는다.
   - .gitignore가 application-local.yaml을 실제로 제외하는지(git이 없으면 패턴만 확인) 보고한다.

만들지 않는 것: 새 라이브러리, 시크릿 관리 도구 연동, 접속 정보를 담은 커밋 대상 파일, 테스트용 DB 설정의 파일화(이번엔 환경변수 유지).
진행 기록은 [8.5단계] 형식으로 추가하고 멈춘다. 질문/가정을 반드시 적는다.
```

---

## 8.6단계: 플랫폼 API 보완(관리자 로그인 아이디, 목록 열), 운영자 비밀번호 길이 12, 통합 테스트 격리

배경: F2 플랫폼 콘솔이 (1) 생성·재발급 응답에 관리자 로그인 아이디가 없어 입력한 이메일로 추정해 보여 주고, (2) 목록 응답에 프로비저닝 단계, 기본 도메인, 생성일이 없어 표에서 뺐다. 또 8.5 진행 중 (3) 플랫폼 운영자 비밀번호 길이가 문서는 12인데 코드 기본값은 10으로 어긋난 것이 드러났고, (4) 일괄 업그레이드 통합 테스트가 공유 DB의 모든 ACTIVE 테넌트를 대상으로 해서 사용자가 실제 테넌트를 등록하면 그 테넌트를 건드리거나 거절되는 위험이 확인됐다.

```
작업 범위는 아래 네 가지뿐이다. 기존 API 동작은 호환을 유지한다(필드 추가만, 삭제·이름 변경 금지).

1. 관리자 로그인 아이디 응답
   - 테넌트 생성(POST /tenants), 프로비저닝 재시도(생성 응답과 같은 형식), 관리자 비밀번호 재발급(POST /tenants/{code}/admin-password/reissue) 응답에 `adminLoginId`(문자열, 소문자 정규화된 실제 로그인 아이디)를 추가한다. 값은 프로비저닝이 실제로 만든(또는 기존) 관리자 계정의 login_id여야 하며 입력 이메일을 다시 가공해 추정하지 않는다. 먼저 현재 코드에서 관리자 login_id가 어떻게 정해지는지 확인해 보고서에 적는다.
   - 임시 비밀번호가 없는 응답(이미 계정이 있어 새 비밀번호를 만들지 않은 경우)에도 adminLoginId는 포함한다.
   - 응답 본문, 로그에 임시 비밀번호 외 비밀을 추가하지 않는다. Cache-Control: no-store 유지.
2. 테넌트 목록 응답 보완
   - GET /tenants 항목에 `provisioningStatus`(현재 프로비저닝 단계 코드, 없으면 null), `primaryDomain`(활성이고 primary인 도메인 이름, 없으면 null), `createDatetime`(UTC ISO)을 추가한다. 기존 필드는 그대로 둔다.
   - 정렬 허용 필드 `createDatetime`이 이제 값과 일치하는지, N+1 쿼리 없이(조인 또는 서브쿼리) 구현했는지 확인한다.
3. 플랫폼 운영자 비밀번호 길이 12
   - `mdm.security.platform.*`의 최소 길이 기본값을 12로 하고(application.yaml, MdmSecurityProperties 기본값, 문서 platform-api.md를 서로 일치시킨다), 테넌트 사용자 정책(공통코드 SECURITY_POLICY, 10)은 바꾸지 않는다. 기존 플랫폼 운영자 계정에는 영향이 없다(변경 시점부터 적용). 관련 테스트의 기대값을 갱신한다.
4. 통합 테스트 격리
   - 일괄 업그레이드(POST /tenants/upgrade)의 본체를 "대상 테넌트 목록을 받는 서비스 메서드"로 분리하고, API는 ACTIVE 전체를 목록으로 만들어 호출한다. 동작과 응답은 그대로다.
   - 통합 테스트는 자기가 만든 `it-` 접두 테넌트 목록만 넘겨 검증한다. 공유 `pmis`에 실제 테넌트가 있어도 테스트가 그 테넌트를 업그레이드/수정/삭제/거절하지 않아야 한다. 테스트가 `ACTIVE 테넌트가 남아 있어서 실패`하는 가드는 제거하고 위 방식으로 대체한다.
   - 테스트 시작 시 `it-` 접두 잔여 테넌트/`t_it_` 스키마를 정리하는 데 그치고, 접두가 다른 행은 절대 지우지 않는다. 실제 테넌트 1건이 있는 상태를 흉내 내는 테스트(접두가 다른 ACTIVE 테넌트를 하나 넣고 통합 테스트 시나리오를 돌려도 그 행이 변하지 않음)를 추가한다. 이 테스트용 가짜 테넌트도 정리 대상에 포함하고, 정리는 그 테스트가 만든 행만 한다.
   - 통합 테스트가 DB 연결 한도(연결 수)로 끊긴 적이 있다. 원인(풀 크기, 컨텍스트 중복 등)을 조사해 가능한 최소 수정으로 줄이고, 못 고치면 현상과 원인을 보고한다.
5. 문서: doc/api/platform-api.md(응답 필드, 비밀번호 길이), 필요하면 mdm-api.md. 

검증: gradlew.bat test, integrationTest. 공유 DB에서 pmis.tenant가 시작·종료 시 같은 행 수인지(테스트가 만든 행만 정리되는지) 읽기 전용으로 확인해 보고한다. 실제 테넌트가 있으면 그 행을 건드리지 않았다는 증거(행 수와 내용 불변)를 남긴다. 값(주소, 계정, 비밀번호, 임시 비밀번호)은 기록하지 않는다. 부트스트랩은 실행하지 않는다.
진행 기록은 [8.6단계] 형식으로 추가하고 멈춘다.
```

---

## 8.7단계: 메시지 목록 응답에 번역 요약 필드 추가

배경: F4 다국어 화면의 메시지 표에 "번역 상태" 열이 있는데, 목록 응답의 `translations`가 비어 있어 어떤 언어가 번역됐는지 알 수 없다. 행마다 단건 조회를 부르면 요청이 과도하므로 목록 응답에 가벼운 요약 필드만 추가한다.

```
작업 범위는 아래뿐이다. 기존 응답 필드는 삭제·이름 변경하지 않는다(필드 추가만).

1. GET /api/mdm/v1/i18n-messages (관리 목록) 항목에 아래 필드를 추가한다.
   - `translatedLanguageCodes`: 번역 문구가 비어 있지 않은 활성 언어 코드 배열(언어 정렬 순서). 없으면 빈 배열.
   - `missingLanguageCodes`: 활성 지원 언어 중 번역이 없는 언어 코드 배열.
   - `translationStatusCodes`: 이 메시지 번역들의 translation_status_code 중복 제거 배열(예: ["DRAFT","APPROVED"]).
   - 기존 `translations` 필드는 그대로 비워 두고(본문은 단건 조회에만), 요약 필드만 채운다.
2. 구현: 페이지에 포함된 메시지 PK 목록으로 번역을 한 번의 쿼리(IN 조회 또는 집계)로 가져와 조립한다. 행마다 쿼리를 내는 N+1은 금지한다. 비활성(use_yn='N') 언어는 활성 언어 집계에서 제외한다.
3. 필터: 기존 번역 상태 필터가 있으면 동작을 바꾸지 않는다. 새 필터(예: 누락 언어만 보기)는 이번에 추가하지 않는다.
4. 번들 조회(/i18n/bundles)와 단건 조회 응답은 바꾸지 않는다. ETag 동작에도 영향이 없어야 한다.
5. 테스트: 번역이 일부 언어에만 있는 메시지, 모든 언어에 있는 메시지, 번역이 전혀 없는 메시지, 비활성 언어가 섞인 경우를 검증하는 통합 테스트를 추가한다. 목록 쿼리 수가 페이지 크기와 무관함을 확인하는 테스트(또는 쿼리 로그 확인)를 하나 포함한다.
6. 문서: doc/api/mdm-api.md 의 i18n-messages 목록 응답 예시와 필드 설명을 갱신한다.

검증: gradlew.bat test, integrationTest(접속 정보 비밀 보호, 실행 전 DB 이름 pmis 확인, it_ 접두 스키마 잔존 확인, 실제 테넌트 행 불변은 8.6 방식 유지). 값(주소, 계정, 비밀번호)은 기록하지 않는다. 새 라이브러리는 추가하지 않는다. git commit/branch/push 는 하지 않는다.
진행 기록은 [8.7단계] 형식으로 doc/cursor-progress.md 에 추가하고 멈춘다.
```

---

## 5단계: 테넌트 스키마 Flyway V2 (회사, 부서, 사람, 사용자)

```
doc/erd/erd-model-org-user-*.json 을 기준으로 db/migration/tenant/V2__create_company_dept_person_user_tables.sql 을 작성한다.
- 테이블 9개(company, dept, dept_history, person, person_assignment, user_account, user_password_history, user_login_history, user_refresh_token).
- 40-erd-source-of-truth.mdc 의 "파일 간 참조" 규칙을 따른다: company.nation_pk -> nation(nation_pk), user_account.support_language_code -> support_language(support_language_code) 는 실제 FK로 만든다. 감사컬럼(create_user_pk 등)에는 FK를 만들지 않는다.
- 자기참조 FK(company.parent_company_pk, dept.parent_dept_pk), 선택 관계(person_assignment.dept_pk, user_account.person_pk, user_login_history.user_pk)는 nullable FK로 만든다.
- ERD description 의 UK를 제약으로 반영한다. NULL 제외 UK는 부분 유니크 인덱스(WHERE ... IS NOT NULL)로 만든다: business_registration_no, (company_pk, employee_no), user_account.person_pk. user_account.login_id 는 소문자 정규화 저장을 전제로 한 UK.
- person_assignment.primary_yn 의 "사람당 유효 소속 중 1건만 Y" 를 부분 유니크 인덱스(WHERE primary_yn='Y' AND end_date IS NULL)로 만들 수 있는지 검토하고 안을 알려준다.
- user_login_history 는 데이터가 많이 쌓이므로 login_datetime 인덱스를 고려하고, 파티셔닝은 하지 말고 필요 여부만 의견으로 알려준다.
- 이전 단계와 같은 방식으로 ERD 반영 내역 표, 질문 목록, 통합 테스트(작성만)를 제공한다.
- 공통 원칙 5번의 형식으로 doc/cursor-progress.md에 기록한다.
```

---

## 5.1단계: V2 질문 확정사항 반영 후 통합 테스트 실행

```
doc/cursor-progress.md 의 [5단계] "질문/확인 필요"에 대한 설계 담당의 결정을 반영한다. V2 는 어떤 환경에도 적용되지 않았으므로 V2 파일을 직접 수정한다(이미 적용했다면 수정하지 말고 알려준다).
- person_assignment.primary_yn 부분 유니크 조건을 WHERE primary_yn='Y' AND end_date IS NULL AND use_yn='Y' 로 바꾼다(3.1의 다른 부분 유니크와 같은 원칙: 논리삭제된 행은 제약에서 제외).
- user_account.login_id: DB 에서도 소문자 저장을 강제한다. CHECK (login_id = lower(login_id)) 를 추가한다. 애플리케이션은 저장 전에 소문자로 정규화한다(서비스/엔티티 처리는 7단계 이후 구현).
- user_login_history 파티셔닝: 하지 않는다(현행 유지). dept_head_yn 부서당 1명 제한: 두지 않는다(현행 유지).
- 위 변경에 맞춰 OrgUserMigrationTest 를 갱신한다(use_yn='N' 행이 있어도 새 주소속 Y 가 가능한지, login_id 대문자 INSERT 가 거절되는지 확인). INSERT 후 PK 는 RETURNING 으로 받는다(currval 금지).
- gradlew.bat test 후 3.2단계 절차(접속 정보 비밀 보호, 실행 전 DB 이름 pmis 확인, it_ 스키마 잔존 확인)대로 gradlew.bat integrationTest 를 실행한다. 모든 통합 테스트(기존 4건 + OrgUserMigrationTest)가 통과해야 한다. 실패하면 원인 분석만 기록하고 멈춘다.
- doc/cursor-progress.md 에 [5.1단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 5.5단계: 테넌트 스키마 Flyway V3 (파일, 팀, 권한, 메뉴)

```
doc/erd/erd-model-file-team-auth-menu-*.json 을 기준으로 db/migration/tenant/V3__create_file_team_role_menu_tables.sql 을 작성한다.
- 테이블 10개(file_storage, file, file_attachment, team, team_member, role, user_role, menu, permission, role_permission).
- 40-erd-source-of-truth.mdc 의 "파일 간 참조" 규칙: team_member.person_pk -> person(person_pk), user_role.user_pk -> user_account(user_pk) 는 실제 FK. 추가로 V2 의 person.profile_file_pk -> file(file_pk) FK 를 이 파일에서 ALTER TABLE 로 추가한다(V2 파일은 수정하지 않는다). 감사컬럼과 file.upload_user_pk 에는 FK 를 만들지 않는다.
- UK/부분 유니크 인덱스: file_storage.default_yn(WHERE default_yn='Y' AND use_yn='Y'), file(storage_pk, storage_key), file_attachment(target_table_name, target_key, file_pk), team.team_code, team_member(team_pk, person_pk) WHERE end_date IS NULL AND use_yn='Y', role.role_code, user_role(user_pk, role_pk), menu.menu_code, permission(menu_pk, action_code), permission.permission_code, role_permission(role_pk, permission_pk).
- CHECK: valid_from_date<=valid_to_date, team/team_member 종료일>=시작일, menu.visible_yn Y/N, menu_type_code/action_code 는 공통코드 컬럼이므로 CHECK 없음, file.file_size >= 0.
- "READ 이외 권한을 부여하려면 같은 메뉴의 READ 권한도 필요"는 DB 제약으로 만들기 어려우므로 서비스 검증 + 통합 테스트로 둔다(DB 트리거는 만들지 않는다).
- 조회 인덱스: file_attachment(target_table_name, target_key), user_role.user_pk, permission.menu_pk, role_permission.permission_pk, team_member.person_pk, menu.parent_menu_pk.
- 이전 단계와 같은 방식으로 ERD 반영 내역 표, 질문 목록, 통합 테스트(작성만)를 제공한다.
- 공통 원칙 5번의 형식으로 doc/cursor-progress.md 에 기록한다. 완료 후 멈춘다.
```

---

## 5.6단계: V3 질문 확정사항 반영 후 통합 테스트 실행

```
doc/cursor-progress.md 의 [5.5단계] "질문/확인 필요"에 대한 설계 담당의 결정을 반영한다. V3 는 어떤 환경에도 적용되지 않았으므로 V3 파일을 직접 수정한다(이미 적용했다면 수정하지 말고 알려준다).
- role.role_code: CHECK ^[A-Z][A-Z0-9_]*$ 를 추가한다(ERD 의 대문자 SNAKE 규칙). 제약 이름은 ck_role_role_code.
- 테스트(FileTeamAuthMenuMigrationTest)에 role_code 소문자 INSERT 거절을 확인하는 검증을 추가한다. INSERT 후 PK 는 RETURNING 으로 받는다(currval 금지).
- gradlew.bat test 후 3.2단계 절차(접속 정보 비밀 보호, 실행 전 DB 이름 pmis 확인, it_ 스키마 잔존 확인)대로 gradlew.bat integrationTest 를 실행한다. 모든 통합 테스트(기존 5건 + FileTeamAuthMenuMigrationTest)가 통과해야 한다. 실패하면 원인 분석만 기록하고 멈춘다.
- 추가로, 지금까지의 마이그레이션 전체(public V1, tenant V1~V3)를 빈 it_ 스키마 하나에 순서대로 적용해 최종 테이블 수(tenant 스키마 31개 = V1 12 + V2 9 + V3 10)와 FK/인덱스가 오류 없이 만들어지는지 확인하는 통합 테스트가 없으면 추가하고 실행한다(스키마명은 코드에 쓰지 않고 Flyway default-schema/search_path 방식 유지).
- doc/cursor-progress.md 에 [5.6단계] 로 기록한다. 완료 후 멈춘다.
```

---

## 이후 단계 (예정, 지시문은 해당 시점에 추가됨)
- 이후: FE는 frontend/doc 의 별도 지시서(F0~)로 진행. 백엔드는 조직·사용자·파일·팀·역할·권한·메뉴 관리 API
- 7단계: 다국어/공통코드 도메인 API (엔티티, 리포지토리, 서비스, 컨트롤러, 테스트) — API 응답 규약 확정 후
- 8단계: 프론트엔드 연동(`ko-KR` 로케일 추가, 공통코드 훅, 서버 번역 번들)
