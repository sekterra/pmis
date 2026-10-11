# pmis 데이터베이스와 공용 스키마

한 번만 실행하는 초기화 스크립트다. Flyway 경로가 아니며, 애플리케이션이 기동할 때 실행되지 않는다. Cursor 환경에서는 DB에 접속하지 않는다. DBeaver 등에서 사용자가 직접 실행한다.

서버는 PostgreSQL 16이다. 새 데이터베이스 `pmis`의 기본 collation은 ICU `ko-KR`이다. 컬럼별 `COLLATE`나 `CREATE COLLATION`은 쓰지 않는다. 기존 `postgres` 데이터베이스는 변경하지 않는다.

## 실행 순서

1. `postgres` 데이터베이스에 접속한 뒤 `01_create_database_pmis.sql`을 실행한다. 이 파일은 `postgres`에 접속한 상태에서만 실행한다.
   - 먼저 `pg_database` 확인 쿼리를 실행한다. `pmis` 행이 있으면 `CREATE DATABASE`는 실행하지 않는다.
   - 행이 없을 때만 `CREATE DATABASE`를 실행한다.
   - `CREATE DATABASE`는 트랜잭션 안에서 실행할 수 없다. DBeaver는 자동 커밋으로 두고 이 문장만 단독 실행한다.
2. DBeaver에서 접속 데이터베이스를 `pmis`로 바꾼다.
   - 데이터베이스 네비게이터에서 `pmis`를 클릭하고 SQL 편집기를 연다. 또는 이미 연 편집기 툴바의 데이터베이스 선택 상자를 `pmis`로 바꾼다.
   - `02`를 실행하기 전에 `SELECT current_database();` 결과가 `pmis`인지 확인한다.
   - 그 다음 `02_create_schema_pmis.sql`을 실행한다. 스크립트는 한 문장이다. 접속 데이터베이스가 `pmis`가 아니면 오류를 내고 스키마를 만들지 않는다.
3. `03`을 실행하기 전에도 `SELECT current_database();` 결과가 `pmis`인지 확인한다. 그 다음 같은 `pmis` 데이터베이스에서 `03_verify_collation.sql`을 실행한다. 첫 결과의 `connection_check`가 `OK`여야 한다.

## 확인 방법

- 01 이후: `pg_database.datname`에 `pmis`가 있다.
- 02 이후: `pmis` 데이터베이스 안에 스키마 `pmis`가 있다. 스키마가 이미 있으면 유지한다. 다른 데이터베이스에 접속한 상태에서는 오류가 나고 스키마는 만들어지지 않는다.
- 03: `datlocprovider`가 `i`, `daticulocale`가 `ko-KR`이다. `current_database()`는 `pmis`다. 정렬 쿼리 결과는 `1, 2, ㄱ, 가, ㄴ, 나, 다, 하, a, A, B` 순이다.

## 이미 실행한 경우

- `CREATE DATABASE`를 다시 실행하면 데이터베이스가 이미 있다는 오류가 나고, 기존 `pmis` 데이터베이스는 바뀌지 않는다.
- `pmis`에 접속한 상태에서 스키마 스크립트를 다시 실행하면, 스키마가 있으면 통과한다.
- 검증 스크립트는 조회만 한다.

## 문제 해결

`postgres` 데이터베이스에 스키마 `pmis`를 잘못 만든 경우에만 아래를 따른다. 실제 `pmis` 데이터베이스에 접속한 상태에서는 실행하지 않는다.

1. `SELECT current_database();`로 접속 데이터베이스가 `postgres`인지 확인한다.
2. 스키마가 비어 있는지 확인한다.

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'pmis';
```

3. 결과가 없을 때만 `DROP SCHEMA pmis;`를 실행한다. `CASCADE`는 쓰지 않는다.

## 롤백

롤백 스크립트는 두지 않는다. `pmis` 데이터베이스를 없애야 하면 `postgres` 데이터베이스에 접속한 상태에서 사용자가 직접 `DROP DATABASE`를 실행한다. 이 명령은 `pmis` 데이터베이스 전체를 삭제한다.
