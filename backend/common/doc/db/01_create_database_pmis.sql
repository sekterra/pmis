-- ============================================================
-- 이 파일은 postgres 데이터베이스에 접속한 상태에서만 실행한다.
-- pmis 데이터베이스에 접속해서 실행하는 파일이 아니다.
-- ============================================================
-- 기존 postgres 데이터베이스에 접속한 상태에서 실행한다.
-- 기존 postgres 데이터베이스의 객체와 설정은 변경하지 않는다.
-- CREATE DATABASE 는 트랜잭션 블록 안에서 실행할 수 없다.
-- DBeaver 는 자동 커밋 모드로 두고, 확인 쿼리와 CREATE DATABASE 를 각각 단독 실행한다.

-- 확인: 1행이라도 나오면 아래 CREATE DATABASE 는 실행하지 않는다.
SELECT datname
FROM pg_database
WHERE datname = 'pmis';

-- 위 조회 결과가 없을 때만 실행한다.
CREATE DATABASE pmis TEMPLATE template0 ENCODING 'UTF8' LOCALE_PROVIDER icu ICU_LOCALE 'ko-KR' LC_COLLATE 'en_US.utf8' LC_CTYPE 'en_US.utf8';
