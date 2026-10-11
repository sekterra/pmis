-- 논리 삭제 뒤 같은 값을 다시 넣을 수 있게, use_yn='Y' 인 행만 유일하다.
-- 변경 불가 자연키와 번역 행 식별자는 전체 유니크로 둔다.

ALTER TABLE i18n_message DROP CONSTRAINT uk_i18n_message_namespace_pk_message_key;
CREATE UNIQUE INDEX uk_i18n_message_namespace_pk_message_key
    ON i18n_message (namespace_pk, message_key)
    WHERE use_yn = 'Y';

ALTER TABLE user_role DROP CONSTRAINT uk_user_role_user_pk_role_pk;
CREATE UNIQUE INDEX uk_user_role_user_pk_role_pk
    ON user_role (user_pk, role_pk)
    WHERE use_yn = 'Y';

ALTER TABLE role_permission DROP CONSTRAINT uk_role_permission_role_pk_permission_pk;
CREATE UNIQUE INDEX uk_role_permission_role_pk_permission_pk
    ON role_permission (role_pk, permission_pk)
    WHERE use_yn = 'Y';

ALTER TABLE file_attachment DROP CONSTRAINT uk_file_attachment_target;
CREATE UNIQUE INDEX uk_file_attachment_target
    ON file_attachment (target_table_name, target_key, file_pk)
    WHERE use_yn = 'Y';

ALTER TABLE common_code_external_map DROP CONSTRAINT uk_common_code_external_map_code_pk_external_system_code;
CREATE UNIQUE INDEX uk_common_code_external_map_code_pk_external_system_code
    ON common_code_external_map (code_pk, external_system_code)
    WHERE use_yn = 'Y';

DROP INDEX uk_person_assignment_employee_no;
CREATE UNIQUE INDEX uk_person_assignment_employee_no
    ON person_assignment (company_pk, employee_no)
    WHERE employee_no IS NOT NULL AND use_yn = 'Y';

DROP INDEX uk_user_account_person_pk;
CREATE UNIQUE INDEX uk_user_account_person_pk
    ON user_account (person_pk)
    WHERE person_pk IS NOT NULL AND use_yn = 'Y';
