-- pmis 데이터베이스에 접속한 상태에서 실행한다. 읽기 전용이다.

SELECT
    current_database() AS current_database,
    CASE
        WHEN current_database() = 'pmis' THEN 'OK'
        ELSE '접속 DB가 pmis 가 아님'
    END AS connection_check;

-- datlocprovider 는 'i'(ICU), daticulocale 은 'ko-KR' 이어야 한다.
SELECT
    datname,
    datlocprovider,
    daticulocale,
    datcollate,
    datctype,
    pg_catalog.pg_encoding_to_char(encoding) AS database_encoding
FROM pg_database
WHERE datname = 'pmis';

SELECT current_database();

SELECT nspname
FROM pg_namespace
WHERE nspname = 'pmis';

-- 접속 DB가 pmis 일 때만 의미가 있다.
-- COLLATE 를 붙이지 않는다. 데이터베이스 기본 collation 으로 정렬한다.
-- 기대 순서: 1, 2, ㄱ, 가, ㄴ, 나, 다, 하, a, A, B
SELECT v
FROM (VALUES ('가'), ('나'), ('다'), ('하'), ('ㄱ'), ('ㄴ'), ('A'), ('a'), ('B'), ('1'), ('2')) AS t(v)
ORDER BY v;
