-- 읽기 전용 점검. SELECT/SHOW 만 사용한다. 스키마, collation, 데이터를 만들지 않는다.
-- DBeaver 등에서는 문장마다 실행한다. 마지막 한글 collation 정렬은 해당 이름이 없으면 오류가 난다.
-- 3번 결과의 collname 이 다를 때는 5번의 COLLATE 이름을 그 값으로 바꿔 실행한다.

-- 1. 서버 버전, 인코딩
SELECT version();

SHOW server_encoding;

-- 2. 현재 DB collation / ctype. datlocprovider 는 PostgreSQL 15 미만이면 키가 없어 NULL 이다.
SELECT
    d.datname,
    d.datcollate,
    d.datctype,
    pg_catalog.pg_encoding_to_char(d.encoding) AS database_encoding,
    to_jsonb(d) -> 'datlocprovider' AS datlocprovider
FROM pg_catalog.pg_database AS d
WHERE d.datname = current_database();

-- 3. 한글 collation 과 ICU collation 존재 여부
SELECT
    n.nspname AS schema_name,
    c.collname,
    c.collprovider,
    c.collcollate,
    c.collctype,
    to_jsonb(c) ->> 'colliculocale' AS colliculocale
FROM pg_catalog.pg_collation AS c
JOIN pg_catalog.pg_namespace AS n ON n.oid = c.collnamespace
WHERE c.collname ILIKE '%ko%'
   OR c.collcollate ILIKE '%ko%'
   OR c.collctype ILIKE '%ko%'
   OR c.collname ILIKE '%korean%'
   OR COALESCE(to_jsonb(c) ->> 'colliculocale', '') ILIKE '%ko%'
ORDER BY n.nspname, c.collname;

SELECT EXISTS (
    SELECT 1
    FROM pg_catalog.pg_collation
    WHERE collprovider = 'i'
) AS icu_collation_present;

-- 4. 스키마 목록, 현재 계정의 CREATE 권한
SELECT
    current_user,
    current_database() AS database_name,
    has_database_privilege(current_user, current_database(), 'CREATE') AS can_create_schema,
    r.rolcreatedb AS can_create_database,
    r.rolsuper AS is_superuser
FROM pg_catalog.pg_roles AS r
WHERE r.rolname = current_user;

SELECT
    n.nspname AS schema_name,
    has_schema_privilege(current_user, n.nspname, 'USAGE') AS can_use,
    has_schema_privilege(current_user, n.nspname, 'CREATE') AS can_create
FROM pg_catalog.pg_namespace AS n
WHERE n.nspname NOT LIKE 'pg_toast%'
  AND n.nspname NOT LIKE 'pg_temp%'
ORDER BY n.nspname;

-- 5. 정렬 비교. 기본 collation 과 한글 collation.
SELECT 'default' AS collation_used, v AS value
FROM (
    SELECT v
    FROM (VALUES ('가'), ('나'), ('다'), ('하'), ('ㄱ'), ('ㄴ'), ('A'), ('a'), ('B'), ('1'), ('2')) AS t(v)
    ORDER BY v COLLATE "default"
) AS sorted;

-- ko-KR-x-icu 가 3번에 없을 때는 실행하지 말고, 있는 collname 으로 COLLATE 를 바꾼다.
SELECT 'ko-KR-x-icu' AS collation_used, v AS value
FROM (
    SELECT v
    FROM (VALUES ('가'), ('나'), ('다'), ('하'), ('ㄱ'), ('ㄴ'), ('A'), ('a'), ('B'), ('1'), ('2')) AS t(v)
    ORDER BY v COLLATE "ko-KR-x-icu"
) AS sorted;

-- ko_KR.utf8 이 3번에 없을 때는 실행하지 않는다.
SELECT 'ko_KR.utf8' AS collation_used, v AS value
FROM (
    SELECT v
    FROM (VALUES ('가'), ('나'), ('다'), ('하'), ('ㄱ'), ('ㄴ'), ('A'), ('a'), ('B'), ('1'), ('2')) AS t(v)
    ORDER BY v COLLATE "ko_KR.utf8"
) AS sorted;
