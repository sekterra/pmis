-- pmis 데이터베이스에 접속한 상태에서만 실행한다.
-- 아래는 세미콜론으로 나뉘지 않는 한 문장이다. 가드와 스키마 생성을 따로 실행하지 않는다.
DO $$ BEGIN IF current_database() <> 'pmis' THEN RAISE EXCEPTION '접속 DB가 pmis 가 아닙니다: %', current_database(); END IF; EXECUTE 'CREATE SCHEMA IF NOT EXISTS pmis'; END $$;
