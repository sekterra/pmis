-- 이미 있는 문구는 수정하지 않는다. TENANT.* 오류는 넣지 않는다.

INSERT INTO i18n_namespace (namespace_code, namespace_name, solution_code)
SELECT 'common', '공통', 'MDM'
WHERE NOT EXISTS (SELECT 1 FROM i18n_namespace WHERE namespace_code = 'common');

INSERT INTO i18n_namespace (namespace_code, namespace_name, solution_code)
SELECT 'error', '오류', 'MDM'
WHERE NOT EXISTS (SELECT 1 FROM i18n_namespace WHERE namespace_code = 'error');

INSERT INTO i18n_namespace (namespace_code, namespace_name, solution_code)
SELECT 'menu', '메뉴', 'MDM'
WHERE NOT EXISTS (SELECT 1 FROM i18n_namespace WHERE namespace_code = 'menu');

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, 'COMMON.VALIDATION_FAILED', 'ERROR', '요청 값이 올바르지 않습니다.'
FROM i18n_namespace n
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = 'COMMON.VALIDATION_FAILED');

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, 'COMMON.INTERNAL', 'ERROR', '요청을 처리하지 못했습니다.'
FROM i18n_namespace n
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = 'COMMON.INTERNAL');

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, 'ko-KR', '요청 값이 올바르지 않습니다.', 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
WHERE n.namespace_code = 'error' AND m.message_key = 'COMMON.VALIDATION_FAILED'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = 'ko-KR');

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, 'en-US', 'The request values are invalid.', 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
WHERE n.namespace_code = 'error' AND m.message_key = 'COMMON.VALIDATION_FAILED'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = 'en-US');

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, 'ko-KR', '요청을 처리하지 못했습니다.', 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
WHERE n.namespace_code = 'error' AND m.message_key = 'COMMON.INTERNAL'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = 'ko-KR');

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, 'en-US', 'The request could not be processed.', 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
WHERE n.namespace_code = 'error' AND m.message_key = 'COMMON.INTERNAL'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = 'en-US');

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'MENU', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('SYSTEM', '시스템'),
    ('I18N', '다국어'),
    ('COMMON_CODE', '공통코드'),
    ('COMPANY', '회사'),
    ('DEPT', '부서'),
    ('PERSON', '사람'),
    ('USER', '사용자'),
    ('FILE', '파일'),
    ('TEAM', '팀'),
    ('ROLE', '역할'),
    ('MENU', '메뉴'),
    ('PERMISSION', '권한')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'menu'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('SYSTEM', 'ko-KR', '시스템'),
    ('SYSTEM', 'en-US', 'System'),
    ('I18N', 'ko-KR', '다국어'),
    ('I18N', 'en-US', 'Languages'),
    ('COMMON_CODE', 'ko-KR', '공통코드'),
    ('COMMON_CODE', 'en-US', 'Common codes'),
    ('COMPANY', 'ko-KR', '회사'),
    ('COMPANY', 'en-US', 'Companies'),
    ('DEPT', 'ko-KR', '부서'),
    ('DEPT', 'en-US', 'Departments'),
    ('PERSON', 'ko-KR', '사람'),
    ('PERSON', 'en-US', 'People'),
    ('USER', 'ko-KR', '사용자'),
    ('USER', 'en-US', 'Users'),
    ('FILE', 'ko-KR', '파일'),
    ('FILE', 'en-US', 'Files'),
    ('TEAM', 'ko-KR', '팀'),
    ('TEAM', 'en-US', 'Teams'),
    ('ROLE', 'ko-KR', '역할'),
    ('ROLE', 'en-US', 'Roles'),
    ('MENU', 'ko-KR', '메뉴'),
    ('MENU', 'en-US', 'Menus'),
    ('PERMISSION', 'ko-KR', '권한'),
    ('PERMISSION', 'en-US', 'Permissions')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'menu'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'ERROR', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('COMMON.INVALID_SORT', '정렬 필드가 올바르지 않습니다.'),
    ('COMMON.INVALID_PAGE', '페이지 값이 올바르지 않습니다.'),
    ('AUTH.FORBIDDEN', '이 기능을 사용할 권한이 없습니다.'),
    ('CODE.GROUP_NOT_FOUND', '코드 그룹을 찾을 수 없습니다.'),
    ('CODE.NOT_FOUND', '공통코드를 찾을 수 없습니다.'),
    ('CODE.DUPLICATE', '이미 있는 코드입니다.'),
    ('CODE.GROUP_IN_USE', '사용 중인 코드가 있어 그룹을 삭제할 수 없습니다.'),
    ('CODE.PARENT_NOT_ALLOWED', '계층형이 아닌 그룹에는 상위 코드를 둘 수 없습니다.'),
    ('CODE.CYCLE', '코드 계층이 순환합니다.'),
    ('CODE.VALUE_LENGTH', '코드 값이 허용 길이를 넘습니다.'),
    ('CODE.INVALID_DATE', '시작일은 종료일보다 늦을 수 없습니다.'),
    ('CODE.INVALID_POLICY_VALUE', '보안 정책 값은 양의 정수여야 합니다.'),
    ('CODE.PARENT_NOT_FOUND', '상위 코드를 찾을 수 없습니다.'),
    ('CODE.LANGUAGE_NOT_FOUND', '지원하지 않는 언어입니다.')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('COMMON.INVALID_SORT', 'ko-KR', '정렬 필드가 올바르지 않습니다.'),
    ('COMMON.INVALID_SORT', 'en-US', 'The sort field is invalid.'),
    ('COMMON.INVALID_PAGE', 'ko-KR', '페이지 값이 올바르지 않습니다.'),
    ('COMMON.INVALID_PAGE', 'en-US', 'The page values are invalid.'),
    ('AUTH.FORBIDDEN', 'ko-KR', '이 기능을 사용할 권한이 없습니다.'),
    ('AUTH.FORBIDDEN', 'en-US', 'You do not have permission for this action.'),
    ('CODE.GROUP_NOT_FOUND', 'ko-KR', '코드 그룹을 찾을 수 없습니다.'),
    ('CODE.GROUP_NOT_FOUND', 'en-US', 'The code group was not found.'),
    ('CODE.NOT_FOUND', 'ko-KR', '공통코드를 찾을 수 없습니다.'),
    ('CODE.NOT_FOUND', 'en-US', 'The code was not found.'),
    ('CODE.DUPLICATE', 'ko-KR', '이미 있는 코드입니다.'),
    ('CODE.DUPLICATE', 'en-US', 'The code already exists.'),
    ('CODE.GROUP_IN_USE', 'ko-KR', '사용 중인 코드가 있어 그룹을 삭제할 수 없습니다.'),
    ('CODE.GROUP_IN_USE', 'en-US', 'The group cannot be deleted while codes are in use.'),
    ('CODE.PARENT_NOT_ALLOWED', 'ko-KR', '계층형이 아닌 그룹에는 상위 코드를 둘 수 없습니다.'),
    ('CODE.PARENT_NOT_ALLOWED', 'en-US', 'A parent code is not allowed for a non-hierarchical group.'),
    ('CODE.CYCLE', 'ko-KR', '코드 계층이 순환합니다.'),
    ('CODE.CYCLE', 'en-US', 'The code hierarchy contains a cycle.'),
    ('CODE.VALUE_LENGTH', 'ko-KR', '코드 값이 허용 길이를 넘습니다.'),
    ('CODE.VALUE_LENGTH', 'en-US', 'The code value is longer than allowed.'),
    ('CODE.INVALID_DATE', 'ko-KR', '시작일은 종료일보다 늦을 수 없습니다.'),
    ('CODE.INVALID_DATE', 'en-US', 'The start date must not be after the end date.'),
    ('CODE.INVALID_POLICY_VALUE', 'ko-KR', '보안 정책 값은 양의 정수여야 합니다.'),
    ('CODE.INVALID_POLICY_VALUE', 'en-US', 'The security policy value must be a positive integer.'),
    ('CODE.PARENT_NOT_FOUND', 'ko-KR', '상위 코드를 찾을 수 없습니다.'),
    ('CODE.PARENT_NOT_FOUND', 'en-US', 'The parent code was not found.'),
    ('CODE.LANGUAGE_NOT_FOUND', 'ko-KR', '지원하지 않는 언어입니다.'),
    ('CODE.LANGUAGE_NOT_FOUND', 'en-US', 'The language is not supported.')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'ERROR', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('I18N.NAMESPACE_NOT_FOUND', '네임스페이스를 찾을 수 없습니다.'),
    ('I18N.LANGUAGE_IN_USE', '사용 중인 언어는 중지할 수 없습니다.'),
    ('I18N.FALLBACK_CYCLE', '대체 언어가 순환합니다.'),
    ('I18N.LANGUAGE_NOT_FOUND', '언어를 찾을 수 없습니다.'),
    ('I18N.DUPLICATE', '이미 있는 값입니다.'),
    ('I18N.MESSAGE_NOT_FOUND', '메시지를 찾을 수 없습니다.'),
    ('I18N.NATION_NOT_FOUND', '국가를 찾을 수 없습니다.'),
    ('I18N.DEFAULT_REQUIRED', '기본 언어는 하나만 있어야 합니다.')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('I18N.NAMESPACE_NOT_FOUND', 'ko-KR', '네임스페이스를 찾을 수 없습니다.'),
    ('I18N.NAMESPACE_NOT_FOUND', 'en-US', 'The namespace was not found.'),
    ('I18N.LANGUAGE_IN_USE', 'ko-KR', '사용 중인 언어는 중지할 수 없습니다.'),
    ('I18N.LANGUAGE_IN_USE', 'en-US', 'The language is in use and cannot be disabled.'),
    ('I18N.FALLBACK_CYCLE', 'ko-KR', '대체 언어가 순환합니다.'),
    ('I18N.FALLBACK_CYCLE', 'en-US', 'The fallback language chain contains a cycle.'),
    ('I18N.LANGUAGE_NOT_FOUND', 'ko-KR', '언어를 찾을 수 없습니다.'),
    ('I18N.LANGUAGE_NOT_FOUND', 'en-US', 'The language was not found.'),
    ('I18N.DUPLICATE', 'ko-KR', '이미 있는 값입니다.'),
    ('I18N.DUPLICATE', 'en-US', 'The value already exists.'),
    ('I18N.MESSAGE_NOT_FOUND', 'ko-KR', '메시지를 찾을 수 없습니다.'),
    ('I18N.MESSAGE_NOT_FOUND', 'en-US', 'The message was not found.'),
    ('I18N.NATION_NOT_FOUND', 'ko-KR', '국가를 찾을 수 없습니다.'),
    ('I18N.NATION_NOT_FOUND', 'en-US', 'The nation was not found.'),
    ('I18N.DEFAULT_REQUIRED', 'ko-KR', '기본 언어는 하나만 있어야 합니다.'),
    ('I18N.DEFAULT_REQUIRED', 'en-US', 'Exactly one default language is required.')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);

INSERT INTO i18n_namespace (namespace_code, namespace_name, solution_code)
SELECT 'auth', '인증', 'MDM'
WHERE NOT EXISTS (SELECT 1 FROM i18n_namespace WHERE namespace_code = 'auth');

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'ERROR', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('AUTH.UNAUTHORIZED', '인증이 필요합니다.'),
    ('AUTH.TOKEN_EXPIRED', '토큰이 만료되었습니다.'),
    ('AUTH.TOKEN_REJECTED', '토큰이 유효하지 않습니다.'),
    ('AUTH.ACCOUNT_DISABLED', '사용할 수 없는 계정입니다.')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('AUTH.UNAUTHORIZED', 'ko-KR', '인증이 필요합니다.'),
    ('AUTH.UNAUTHORIZED', 'en-US', 'Authentication is required.'),
    ('AUTH.TOKEN_EXPIRED', 'ko-KR', '토큰이 만료되었습니다.'),
    ('AUTH.TOKEN_EXPIRED', 'en-US', 'The token has expired.'),
    ('AUTH.TOKEN_REJECTED', 'ko-KR', '토큰이 유효하지 않습니다.'),
    ('AUTH.TOKEN_REJECTED', 'en-US', 'The token was rejected.'),
    ('AUTH.ACCOUNT_DISABLED', 'ko-KR', '사용할 수 없는 계정입니다.'),
    ('AUTH.ACCOUNT_DISABLED', 'en-US', 'The account cannot be used.')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'ERROR', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('AUTH.INVALID_CREDENTIALS', '로그인에 실패했습니다.'),
    ('AUTH.CSRF', '요청을 확인할 수 없습니다.'),
    ('AUTH.TOKEN_REUSED', '다시 사용된 토큰입니다.'),
    ('AUTH.REFRESH_IN_PROGRESS', '토큰 갱신이 진행 중입니다.'),
    ('AUTH.PASSWORD_CURRENT', '현재 비밀번호가 올바르지 않습니다.'),
    ('AUTH.PASSWORD_SAME', '새 비밀번호는 현재 비밀번호와 달라야 합니다.'),
    ('AUTH.PASSWORD_POLICY', '비밀번호 정책에 맞지 않습니다.'),
    ('AUTH.PASSWORD_REUSED', '최근에 사용한 비밀번호입니다.'),
    ('AUTH.PASSWORD_CHANGE_REQUIRED', '비밀번호를 변경해야 합니다.')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('AUTH.INVALID_CREDENTIALS', 'ko-KR', '로그인에 실패했습니다.'),
    ('AUTH.INVALID_CREDENTIALS', 'en-US', 'Sign-in failed.'),
    ('AUTH.CSRF', 'ko-KR', '요청을 확인할 수 없습니다.'),
    ('AUTH.CSRF', 'en-US', 'The request could not be verified.'),
    ('AUTH.TOKEN_REUSED', 'ko-KR', '다시 사용된 토큰입니다.'),
    ('AUTH.TOKEN_REUSED', 'en-US', 'The token was reused.'),
    ('AUTH.REFRESH_IN_PROGRESS', 'ko-KR', '토큰 갱신이 진행 중입니다.'),
    ('AUTH.REFRESH_IN_PROGRESS', 'en-US', 'Token refresh is already in progress.'),
    ('AUTH.PASSWORD_CURRENT', 'ko-KR', '현재 비밀번호가 올바르지 않습니다.'),
    ('AUTH.PASSWORD_CURRENT', 'en-US', 'The current password is incorrect.'),
    ('AUTH.PASSWORD_SAME', 'ko-KR', '새 비밀번호는 현재 비밀번호와 달라야 합니다.'),
    ('AUTH.PASSWORD_SAME', 'en-US', 'The new password must differ from the current password.'),
    ('AUTH.PASSWORD_POLICY', 'ko-KR', '비밀번호 정책에 맞지 않습니다.'),
    ('AUTH.PASSWORD_POLICY', 'en-US', 'The password does not meet the policy.'),
    ('AUTH.PASSWORD_REUSED', 'ko-KR', '최근에 사용한 비밀번호입니다.'),
    ('AUTH.PASSWORD_REUSED', 'en-US', 'That password was used recently.'),
    ('AUTH.PASSWORD_CHANGE_REQUIRED', 'ko-KR', '비밀번호를 변경해야 합니다.'),
    ('AUTH.PASSWORD_CHANGE_REQUIRED', 'en-US', 'The password must be changed.')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'LABEL', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('auth.login.title', '로그인'),
    ('auth.login.id', '아이디'),
    ('auth.login.password', '비밀번호'),
    ('auth.login.submit', '로그인'),
    ('auth.login.failed', '로그인에 실패했습니다.'),
    ('auth.password.title', '비밀번호 변경'),
    ('auth.password.current', '현재 비밀번호'),
    ('auth.password.new', '새 비밀번호'),
    ('auth.password.submit', '변경'),
    ('auth.password.required', '비밀번호를 변경해야 합니다.')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'auth'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('auth.login.title', 'ko-KR', '로그인'),
    ('auth.login.title', 'en-US', 'Sign in'),
    ('auth.login.id', 'ko-KR', '아이디'),
    ('auth.login.id', 'en-US', 'ID'),
    ('auth.login.password', 'ko-KR', '비밀번호'),
    ('auth.login.password', 'en-US', 'Password'),
    ('auth.login.submit', 'ko-KR', '로그인'),
    ('auth.login.submit', 'en-US', 'Sign in'),
    ('auth.login.failed', 'ko-KR', '로그인에 실패했습니다.'),
    ('auth.login.failed', 'en-US', 'Sign-in failed.'),
    ('auth.password.title', 'ko-KR', '비밀번호 변경'),
    ('auth.password.title', 'en-US', 'Change password'),
    ('auth.password.current', 'ko-KR', '현재 비밀번호'),
    ('auth.password.current', 'en-US', 'Current password'),
    ('auth.password.new', 'ko-KR', '새 비밀번호'),
    ('auth.password.new', 'en-US', 'New password'),
    ('auth.password.submit', 'ko-KR', '변경'),
    ('auth.password.submit', 'en-US', 'Change'),
    ('auth.password.required', 'ko-KR', '비밀번호를 변경해야 합니다.'),
    ('auth.password.required', 'en-US', 'You must change your password.')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'auth'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'ERROR', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('ORG.INVALID_CODE_VALUE', '사용할 수 없는 코드 값입니다.'),
    ('COMPANY.DUPLICATE', '이미 있는 회사 코드입니다.'),
    ('COMPANY.DUPLICATE_BUSINESS_NO', '이미 있는 사업자등록번호입니다.'),
    ('COMPANY.NOT_FOUND', '회사를 찾을 수 없습니다.'),
    ('COMPANY.PARENT_CYCLE', '회사 계층이 순환합니다.'),
    ('COMPANY.PARENT_INACTIVE', '상위 회사가 사용 중이 아닙니다.'),
    ('COMPANY.HAS_CHILD_COMPANY', '사용 중인 하위 회사가 있습니다.'),
    ('COMPANY.HAS_ACTIVE_DEPT', '사용 중인 부서가 있습니다.'),
    ('COMPANY.HAS_ACTIVE_ASSIGNMENT', '현재 소속이 있습니다.'),
    ('COMPANY.LAST_OWN', '마지막 자사는 삭제할 수 없습니다.'),
    ('DEPT.DUPLICATE', '이미 있는 부서 코드입니다.'),
    ('DEPT.NOT_FOUND', '부서를 찾을 수 없습니다.'),
    ('DEPT.PARENT_CYCLE', '부서 계층이 순환합니다.'),
    ('DEPT.PARENT_COMPANY_MISMATCH', '다른 회사의 부서를 상위로 둘 수 없습니다.'),
    ('DEPT.PARENT_INACTIVE', '상위 부서가 사용 중이 아닙니다.'),
    ('DEPT.HAS_CHILD', '사용 중인 하위 부서가 있습니다.'),
    ('DEPT.HAS_ACTIVE_ASSIGNMENT', '현재 소속이 있습니다.'),
    ('DEPT.INVALID_DATE', '시작일은 종료일보다 늦을 수 없습니다.'),
    ('PERSON.NOT_FOUND', '사람을 찾을 수 없습니다.'),
    ('PERSON.HAS_ACTIVE_ASSIGNMENT', '현재 소속이 있습니다.'),
    ('PERSON.HAS_ACCOUNT', '사용 중인 계정이 있습니다.'),
    ('ASSIGNMENT.NOT_FOUND', '소속을 찾을 수 없습니다.'),
    ('ASSIGNMENT.DEPT_COMPANY_MISMATCH', '부서가 회사와 맞지 않습니다.'),
    ('ASSIGNMENT.DEPT_INACTIVE', '사용 중인 부서가 아닙니다.'),
    ('ASSIGNMENT.COMPANY_INACTIVE', '사용 중인 회사가 아닙니다.'),
    ('ASSIGNMENT.DUPLICATE', '같은 회사와 부서의 현재 소속이 있습니다.'),
    ('ASSIGNMENT.EMPLOYEE_NO_DUPLICATE', '이미 있는 사원번호입니다.'),
    ('ASSIGNMENT.RESIGNED_REQUIRES_END', '퇴사 상태에는 종료일이 필요합니다.'),
    ('ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT', '부서장 여부는 부서가 있을 때만 쓸 수 있습니다.'),
    ('ASSIGNMENT.INVALID_DATE', '시작일은 종료일보다 늦을 수 없습니다.')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('ORG.INVALID_CODE_VALUE', 'ko-KR', '사용할 수 없는 코드 값입니다.'),
    ('ORG.INVALID_CODE_VALUE', 'en-US', 'The code value is not usable.'),
    ('COMPANY.DUPLICATE', 'ko-KR', '이미 있는 회사 코드입니다.'),
    ('COMPANY.DUPLICATE', 'en-US', 'The company code already exists.'),
    ('COMPANY.DUPLICATE_BUSINESS_NO', 'ko-KR', '이미 있는 사업자등록번호입니다.'),
    ('COMPANY.DUPLICATE_BUSINESS_NO', 'en-US', 'The business registration number already exists.'),
    ('COMPANY.NOT_FOUND', 'ko-KR', '회사를 찾을 수 없습니다.'),
    ('COMPANY.NOT_FOUND', 'en-US', 'The company was not found.'),
    ('COMPANY.PARENT_CYCLE', 'ko-KR', '회사 계층이 순환합니다.'),
    ('COMPANY.PARENT_CYCLE', 'en-US', 'The company hierarchy contains a cycle.'),
    ('COMPANY.PARENT_INACTIVE', 'ko-KR', '상위 회사가 사용 중이 아닙니다.'),
    ('COMPANY.PARENT_INACTIVE', 'en-US', 'The parent company is not active.'),
    ('COMPANY.HAS_CHILD_COMPANY', 'ko-KR', '사용 중인 하위 회사가 있습니다.'),
    ('COMPANY.HAS_CHILD_COMPANY', 'en-US', 'An active child company exists.'),
    ('COMPANY.HAS_ACTIVE_DEPT', 'ko-KR', '사용 중인 부서가 있습니다.'),
    ('COMPANY.HAS_ACTIVE_DEPT', 'en-US', 'An active department exists.'),
    ('COMPANY.HAS_ACTIVE_ASSIGNMENT', 'ko-KR', '현재 소속이 있습니다.'),
    ('COMPANY.HAS_ACTIVE_ASSIGNMENT', 'en-US', 'A current assignment exists.'),
    ('COMPANY.LAST_OWN', 'ko-KR', '마지막 자사는 삭제할 수 없습니다.'),
    ('COMPANY.LAST_OWN', 'en-US', 'The last own company cannot be deleted.'),
    ('DEPT.DUPLICATE', 'ko-KR', '이미 있는 부서 코드입니다.'),
    ('DEPT.DUPLICATE', 'en-US', 'The department code already exists.'),
    ('DEPT.NOT_FOUND', 'ko-KR', '부서를 찾을 수 없습니다.'),
    ('DEPT.NOT_FOUND', 'en-US', 'The department was not found.'),
    ('DEPT.PARENT_CYCLE', 'ko-KR', '부서 계층이 순환합니다.'),
    ('DEPT.PARENT_CYCLE', 'en-US', 'The department hierarchy contains a cycle.'),
    ('DEPT.PARENT_COMPANY_MISMATCH', 'ko-KR', '다른 회사의 부서를 상위로 둘 수 없습니다.'),
    ('DEPT.PARENT_COMPANY_MISMATCH', 'en-US', 'The parent department belongs to another company.'),
    ('DEPT.PARENT_INACTIVE', 'ko-KR', '상위 부서가 사용 중이 아닙니다.'),
    ('DEPT.PARENT_INACTIVE', 'en-US', 'The parent department is not active.'),
    ('DEPT.HAS_CHILD', 'ko-KR', '사용 중인 하위 부서가 있습니다.'),
    ('DEPT.HAS_CHILD', 'en-US', 'An active child department exists.'),
    ('DEPT.HAS_ACTIVE_ASSIGNMENT', 'ko-KR', '현재 소속이 있습니다.'),
    ('DEPT.HAS_ACTIVE_ASSIGNMENT', 'en-US', 'A current assignment exists.'),
    ('DEPT.INVALID_DATE', 'ko-KR', '시작일은 종료일보다 늦을 수 없습니다.'),
    ('DEPT.INVALID_DATE', 'en-US', 'The start date must not be after the end date.'),
    ('PERSON.NOT_FOUND', 'ko-KR', '사람을 찾을 수 없습니다.'),
    ('PERSON.NOT_FOUND', 'en-US', 'The person was not found.'),
    ('PERSON.HAS_ACTIVE_ASSIGNMENT', 'ko-KR', '현재 소속이 있습니다.'),
    ('PERSON.HAS_ACTIVE_ASSIGNMENT', 'en-US', 'A current assignment exists.'),
    ('PERSON.HAS_ACCOUNT', 'ko-KR', '사용 중인 계정이 있습니다.'),
    ('PERSON.HAS_ACCOUNT', 'en-US', 'An active account exists.'),
    ('ASSIGNMENT.NOT_FOUND', 'ko-KR', '소속을 찾을 수 없습니다.'),
    ('ASSIGNMENT.NOT_FOUND', 'en-US', 'The assignment was not found.'),
    ('ASSIGNMENT.DEPT_COMPANY_MISMATCH', 'ko-KR', '부서가 회사와 맞지 않습니다.'),
    ('ASSIGNMENT.DEPT_COMPANY_MISMATCH', 'en-US', 'The department does not belong to the company.'),
    ('ASSIGNMENT.DEPT_INACTIVE', 'ko-KR', '사용 중인 부서가 아닙니다.'),
    ('ASSIGNMENT.DEPT_INACTIVE', 'en-US', 'The department is not active.'),
    ('ASSIGNMENT.COMPANY_INACTIVE', 'ko-KR', '사용 중인 회사가 아닙니다.'),
    ('ASSIGNMENT.COMPANY_INACTIVE', 'en-US', 'The company is not active.'),
    ('ASSIGNMENT.DUPLICATE', 'ko-KR', '같은 회사와 부서의 현재 소속이 있습니다.'),
    ('ASSIGNMENT.DUPLICATE', 'en-US', 'A current assignment already exists for the same company and department.'),
    ('ASSIGNMENT.EMPLOYEE_NO_DUPLICATE', 'ko-KR', '이미 있는 사원번호입니다.'),
    ('ASSIGNMENT.EMPLOYEE_NO_DUPLICATE', 'en-US', 'The employee number already exists.'),
    ('ASSIGNMENT.RESIGNED_REQUIRES_END', 'ko-KR', '퇴사 상태에는 종료일이 필요합니다.'),
    ('ASSIGNMENT.RESIGNED_REQUIRES_END', 'en-US', 'A resigned assignment requires an end date.'),
    ('ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT', 'ko-KR', '부서장 여부는 부서가 있을 때만 쓸 수 있습니다.'),
    ('ASSIGNMENT.DEPT_HEAD_REQUIRES_DEPT', 'en-US', 'A department head flag requires a department.'),
    ('ASSIGNMENT.INVALID_DATE', 'ko-KR', '시작일은 종료일보다 늦을 수 없습니다.'),
    ('ASSIGNMENT.INVALID_DATE', 'en-US', 'The start date must not be after the end date.')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);

INSERT INTO i18n_message (namespace_pk, message_key, message_type_code, default_message)
SELECT n.namespace_pk, v.message_key, 'ERROR', v.default_message
FROM i18n_namespace n
JOIN (VALUES
    ('ASSIGNMENT.ALREADY_ENDED', '이미 종료된 소속입니다.'),
    ('ASSIGNMENT.INACTIVE', '사용 중이 아닌 소속은 종료할 수 없습니다.'),
    ('USER.DUPLICATE_LOGIN_ID', '이미 있는 로그인 아이디입니다.'),
    ('USER.PERSON_ALREADY_LINKED', '이미 계정이 연결된 사람입니다.'),
    ('USER.NOT_FOUND', '사용자를 찾을 수 없습니다.'),
    ('USER.INACTIVE', '사용 중이 아닌 계정입니다.'),
    ('USER.INVALID_STATUS_TRANSITION', '바꿀 수 없는 상태입니다.'),
    ('USER.SELF_OPERATION_FORBIDDEN', '본인 계정은 이렇게 바꿀 수 없습니다.'),
    ('USER.LAST_ADMIN', '마지막 관리자 계정입니다.'),
    ('USER.NO_PASSWORD_ACCOUNT', '비밀번호가 없는 계정입니다.'),
    ('ROLE.NOT_FOUND', '역할을 찾을 수 없습니다.'),
    ('ROLE.INACTIVE', '사용 중이 아닌 역할입니다.'),
    ('ROLE.INVALID_DATE', '시작일은 종료일보다 늦을 수 없습니다.'),
    ('ROLE.DUPLICATE', '이미 있는 역할 코드입니다.'),
    ('ROLE.PROTECTED', '보호된 역할입니다.'),
    ('ROLE.IN_USE', '부여 중인 역할입니다.'),
    ('ROLE.READ_REQUIRED', '같은 메뉴의 READ 권한이 필요합니다.'),
    ('PERMISSION.NOT_FOUND', '권한을 찾을 수 없습니다.'),
    ('PERMISSION.DUPLICATE', '이미 있는 권한입니다.'),
    ('PERMISSION.MENU_NOT_PAGE', '페이지 메뉴만 권한을 가질 수 있습니다.'),
    ('PERMISSION.PROTECTED', 'READ 권한은 삭제할 수 없습니다.'),
    ('PERMISSION.INACTIVE', '사용 중이 아닌 권한입니다.')
) AS v(message_key, default_message) ON TRUE
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message m
      WHERE m.namespace_pk = n.namespace_pk AND m.message_key = v.message_key);

INSERT INTO i18n_message_text (message_pk, support_language_code, message_text, translation_status_code)
SELECT m.message_pk, v.support_language_code, v.message_text, 'APPROVED'
FROM i18n_message m
JOIN i18n_namespace n ON n.namespace_pk = m.namespace_pk
JOIN (VALUES
    ('ASSIGNMENT.ALREADY_ENDED', 'ko-KR', '이미 종료된 소속입니다.'),
    ('ASSIGNMENT.ALREADY_ENDED', 'en-US', 'The assignment has already ended.'),
    ('ASSIGNMENT.INACTIVE', 'ko-KR', '사용 중이 아닌 소속은 종료할 수 없습니다.'),
    ('ASSIGNMENT.INACTIVE', 'en-US', 'An inactive assignment cannot be ended.'),
    ('USER.DUPLICATE_LOGIN_ID', 'ko-KR', '이미 있는 로그인 아이디입니다.'),
    ('USER.DUPLICATE_LOGIN_ID', 'en-US', 'The login id already exists.'),
    ('USER.PERSON_ALREADY_LINKED', 'ko-KR', '이미 계정이 연결된 사람입니다.'),
    ('USER.PERSON_ALREADY_LINKED', 'en-US', 'The person is already linked to an account.'),
    ('USER.NOT_FOUND', 'ko-KR', '사용자를 찾을 수 없습니다.'),
    ('USER.NOT_FOUND', 'en-US', 'The user was not found.'),
    ('USER.INACTIVE', 'ko-KR', '사용 중이 아닌 계정입니다.'),
    ('USER.INACTIVE', 'en-US', 'The account is not active.'),
    ('USER.INVALID_STATUS_TRANSITION', 'ko-KR', '바꿀 수 없는 상태입니다.'),
    ('USER.INVALID_STATUS_TRANSITION', 'en-US', 'The status cannot be changed that way.'),
    ('USER.SELF_OPERATION_FORBIDDEN', 'ko-KR', '본인 계정은 이렇게 바꿀 수 없습니다.'),
    ('USER.SELF_OPERATION_FORBIDDEN', 'en-US', 'This operation is not allowed on the signed-in account.'),
    ('USER.LAST_ADMIN', 'ko-KR', '마지막 관리자 계정입니다.'),
    ('USER.LAST_ADMIN', 'en-US', 'The last administrator account cannot be changed this way.'),
    ('USER.NO_PASSWORD_ACCOUNT', 'ko-KR', '비밀번호가 없는 계정입니다.'),
    ('USER.NO_PASSWORD_ACCOUNT', 'en-US', 'The account has no password.'),
    ('ROLE.NOT_FOUND', 'ko-KR', '역할을 찾을 수 없습니다.'),
    ('ROLE.NOT_FOUND', 'en-US', 'The role was not found.'),
    ('ROLE.INACTIVE', 'ko-KR', '사용 중이 아닌 역할입니다.'),
    ('ROLE.INACTIVE', 'en-US', 'The role is not active.'),
    ('ROLE.INVALID_DATE', 'ko-KR', '시작일은 종료일보다 늦을 수 없습니다.'),
    ('ROLE.INVALID_DATE', 'en-US', 'The start date must not be after the end date.'),
    ('ROLE.DUPLICATE', 'ko-KR', '이미 있는 역할 코드입니다.'),
    ('ROLE.DUPLICATE', 'en-US', 'The role code already exists.'),
    ('ROLE.PROTECTED', 'ko-KR', '보호된 역할입니다.'),
    ('ROLE.PROTECTED', 'en-US', 'The role is protected.'),
    ('ROLE.IN_USE', 'ko-KR', '부여 중인 역할입니다.'),
    ('ROLE.IN_USE', 'en-US', 'The role is still granted.'),
    ('ROLE.READ_REQUIRED', 'ko-KR', '같은 메뉴의 READ 권한이 필요합니다.'),
    ('ROLE.READ_REQUIRED', 'en-US', 'The same menu requires READ.'),
    ('PERMISSION.NOT_FOUND', 'ko-KR', '권한을 찾을 수 없습니다.'),
    ('PERMISSION.NOT_FOUND', 'en-US', 'The permission was not found.'),
    ('PERMISSION.DUPLICATE', 'ko-KR', '이미 있는 권한입니다.'),
    ('PERMISSION.DUPLICATE', 'en-US', 'The permission already exists.'),
    ('PERMISSION.MENU_NOT_PAGE', 'ko-KR', '페이지 메뉴만 권한을 가질 수 있습니다.'),
    ('PERMISSION.MENU_NOT_PAGE', 'en-US', 'Only a page menu can have a permission.'),
    ('PERMISSION.PROTECTED', 'ko-KR', 'READ 권한은 삭제할 수 없습니다.'),
    ('PERMISSION.PROTECTED', 'en-US', 'A READ permission cannot be deleted.'),
    ('PERMISSION.INACTIVE', 'ko-KR', '사용 중이 아닌 권한입니다.'),
    ('PERMISSION.INACTIVE', 'en-US', 'The permission is not active.')
) AS v(message_key, support_language_code, message_text)
    ON v.message_key = m.message_key
WHERE n.namespace_code = 'error'
  AND NOT EXISTS (
      SELECT 1 FROM i18n_message_text t
      WHERE t.message_pk = m.message_pk AND t.support_language_code = v.support_language_code);
