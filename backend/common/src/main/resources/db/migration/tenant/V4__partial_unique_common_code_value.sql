-- 사용 중인 코드 값만 그룹 안에서 유일하다. 논리 삭제한 값은 다시 등록할 수 있다.

ALTER TABLE common_code DROP CONSTRAINT uk_common_code_code_group_pk_code_value;

CREATE UNIQUE INDEX uk_common_code_code_group_pk_code_value
    ON common_code (code_group_pk, code_value)
    WHERE use_yn = 'Y';
