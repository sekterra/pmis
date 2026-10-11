# Cursor AI 작업 지시문 (백엔드 common)

사용 방법: Cursor에서 `D:\_project\PMIS\backend\common`을 열고, 아래 지시문을 순서대로 하나씩 붙여 넣는다.
각 단계가 끝나면 사용자가 검토(diff 확인, 빌드/테스트 결과)한 뒤 다음 단계로 진행한다.
`.cursor/rules` 의 규칙은 자동 적용된다.

## 사전 준비 (사용자)
- ERD JSON 2개를 `backend/common/docs/erd/` 폴더에 복사한다.
  - `erd-model-tenant-20261009.json`
  - `erd-model-i18n-common-code-20261009.json`
- 로컬에 Docker가 설치되어 있어야 한다(PostgreSQL 컨테이너, Testcontainers).

---

## 1단계: 개발 환경 구성

```
.cursor/rules의 규칙을 먼저 읽고 따라줘. 이 단계에서는 도메인 코드를 만들지 말고 개발 환경만 구성해줘.

1. docker-compose.yml: PostgreSQL 로컬 개발용(포트 5432, DB명 pmis_common, 데이터 볼륨 유지). 접속 정보는 .env로 분리하고 .env.example을 제공해줘. .env는 .gitignore에 추가해줘.
2. src/main/resources/application.yaml (공통) + application-local.yaml (로컬):
   - datasource는 환경변수로 받고 local 기본값은 docker-compose 값과 맞춰줘.
   - spring.flyway.locations=classpath:db/migration/public
   - spring.jpa.hibernate.ddl-auto=none, hibernate.jdbc.time_zone=UTC
   - open-in-view=false
3. src/main/resources/db/migration/ 아래에 public/, tenant/ 폴더를 만들어줘(기존 루트의 빈 폴더 정리 포함, .gitkeep 사용).
4. 패키지 뼈대: global(config, exception, response, audit, tenant), tenant, i18n, code. 빈 package-info.java 정도만 두고 클래스는 아직 만들지 마.
5. JPA Auditing 설정과 BaseEntity(@MappedSuperclass; create_datetime, create_user_pk, update_datetime, update_user_pk)를 만들어줘. 감사자(AuditorAware)는 현재 사용자 체계가 없으므로 null을 반환하는 임시 구현으로 하고, TODO로 표시해줘.
6. ./gradlew build 가 통과하는지 확인하고, 결과와 변경 파일 목록을 알려줘.

새 라이브러리 추가는 하지 마. springdoc-openapi가 필요하면 Spring Boot 4 호환 버전을 조사해서 추가 여부를 나에게 먼저 물어봐줘.
```

## 2단계: 공용 스키마 Flyway V1 (테넌트 관리)

```
docs/erd/erd-model-tenant-20261009.json 을 기준으로 db/migration/public/V1__create_tenant_tables.sql 을 작성해줘.
- 40-erd-source-of-truth.mdc, 10-database-conventions.mdc 규칙을 따라줘.
- 테이블 5개(tenant, tenant_domain, tenant_status_history, tenant_provisioning, tenant_schema_migration), 시퀀스(<테이블>_seq), PK/FK/UK/CHECK 제약, 인덱스를 포함해줘.
- ERD description에 적힌 UK, 기본값, 허용값을 빠짐없이 반영하고, 반영 내역을 표로 정리해서 보여줘.
- ERD와 어긋나거나 모호한 부분은 임의로 결정하지 말고 질문 목록으로 정리해줘.
- Testcontainers 기반 테스트로 마이그레이션이 빈 DB에 적용되고 ERD 테이블/컬럼명과 일치하는지 검증해줘.
```

## 3단계: 테넌트 스키마 Flyway V1 (다국어, 공통코드)

```
docs/erd/erd-model-i18n-common-code-20261009.json 을 기준으로 db/migration/tenant/V1__create_i18n_and_code_tables.sql 을 작성해줘.
- 테이블 12개(support_language, nation, i18n_namespace, i18n_message, i18n_message_text, i18n_entity_text, code_group, code_group_lang, common_code, common_code_lang, common_code_external_map, common_code_history).
- 스키마명을 SQL에 쓰지 마(비한정 이름). 시퀀스도 같은 스크립트에서 생성.
- support_language는 support_language_code(VARCHAR)가 PK인 예외임에 주의해줘.
- 자기참조 FK(support_language.fallback_language_code, common_code.parent_code_pk)를 포함해줘.
- 2단계와 같은 방식으로 ERD 반영 내역 표, 질문 목록, 마이그레이션 테스트를 제공해줘.
- 이 단계에서는 Seed 데이터를 넣지 마(다음 단계에서 별도로 진행).
```

## 4단계: 멀티테넌시 인프라 (먼저 설계안 제시)

```
20-flyway-multitenancy.mdc 의 규칙을 기준으로 스키마 분리 멀티테넌시 인프라를 구현할 거야.
먼저 코드를 쓰지 말고 설계안만 작성해서 보여줘.
- Hibernate 멀티테넌시(SCHEMA)에서 CurrentTenantIdentifierResolver / MultiTenantConnectionProvider 구성 방식과 Spring Boot 4 / Hibernate 7 에서의 설정 방법
- TenantResolver 인터페이스 설계 (식별 방식 Host vs JWT는 미확정이므로 두 방식 모두 붙일 수 있게)
- 커넥션 반납 시 search_path 초기화 방법과 커넥션 풀 오염 방지
- 스키마명 검증 정책
- 테넌트 프로비저닝 서비스의 단계별 흐름과 실패 시 재시도 방법
- 모든 테넌트 일괄 업그레이드 방식
- 테스트 전략(테넌트 격리 테스트)
선택지가 있으면 트레이드오프를 비교하고 추천안을 표시해줘. 내가 승인하면 구현하자.
```

## 이후 단계 (예정)
- 5단계: 테넌트 스키마 Seed 데이터 (지원언어, 국가, 표준 코드그룹/코드, 기본 UI 메시지)
- 6단계: 다국어/공통코드 도메인 API (엔티티, 리포지토리, 서비스, 컨트롤러, 테스트) — API 응답 규약 확정 후
- 7단계: 프론트엔드 연동(`ko-KR` 로케일 추가, 공통코드 훅, 서버 번역 번들)
