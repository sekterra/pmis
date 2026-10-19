-- 이미 있는 역할, 메뉴, 권한, 부여 행은 수정하지 않는다.

INSERT INTO role (role_code, role_name, sort_order, description)
SELECT 'TENANT_ADMIN', '테넌트 관리자', 1, '모든 권한'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'TENANT_ADMIN');

INSERT INTO role (role_code, role_name, sort_order, description)
SELECT 'MANAGER', '매니저', 2, '조직과 파일의 등록, 수정, 내보내기'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'MANAGER');

INSERT INTO role (role_code, role_name, sort_order, description)
SELECT 'USER', '사용자', 3, '허용된 메뉴 조회'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code = 'USER');

INSERT INTO menu (
    parent_menu_pk, solution_code, menu_code, menu_name, menu_type_code,
    route_path, level_no, menu_path, sort_order)
SELECT NULL, 'MDM', 'SYSTEM', '시스템', 'GROUP', '/system', 1, '/SYSTEM', 10
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE menu_code = 'SYSTEM');

INSERT INTO menu (
    parent_menu_pk, solution_code, menu_code, menu_name, menu_type_code,
    route_path, level_no, menu_path, sort_order)
SELECT p.menu_pk, 'MDM', v.menu_code, v.menu_name, 'PAGE', v.route_path, 2, '/SYSTEM/' || v.menu_code, v.sort_order
FROM menu p
JOIN (VALUES
    ('I18N', '다국어', '/system/i18n', 10),
    ('COMMON_CODE', '공통코드', '/system/common_code', 20),
    ('COMPANY', '회사', '/system/company', 30),
    ('DEPT', '부서', '/system/dept', 40),
    ('PERSON', '사람', '/system/person', 50),
    ('USER', '사용자', '/system/user', 60),
    ('FILE_STORAGE', '파일 저장소', '/system/file-storage', 65),
    ('FILE', '파일', '/system/file', 70),
    ('TEAM', '팀', '/system/team', 80),
    ('ROLE', '역할', '/system/role', 90),
    ('MENU', '메뉴', '/system/menu', 100),
    ('PERMISSION', '권한', '/system/permission', 110)
) AS v(menu_code, menu_name, route_path, sort_order) ON TRUE
WHERE p.menu_code = 'SYSTEM'
  AND NOT EXISTS (SELECT 1 FROM menu m WHERE m.menu_code = v.menu_code);

INSERT INTO permission (menu_pk, action_code, permission_code, permission_name, sort_order)
SELECT m.menu_pk, a.action_code, m.menu_code || ':' || a.action_code, m.menu_name || ' ' || a.action_name, a.sort_order
FROM menu m
JOIN (VALUES
    ('READ', '조회', 1),
    ('CREATE', '등록', 2),
    ('UPDATE', '수정', 3),
    ('DELETE', '삭제', 4),
    ('EXPORT', '내보내기', 5)
) AS a(action_code, action_name, sort_order) ON TRUE
WHERE m.menu_code IN (
      'I18N', 'COMMON_CODE', 'COMPANY', 'DEPT', 'PERSON', 'USER',
      'FILE_STORAGE', 'FILE', 'TEAM', 'ROLE', 'MENU', 'PERMISSION')
  AND NOT EXISTS (
      SELECT 1 FROM permission p
      WHERE p.menu_pk = m.menu_pk AND p.action_code = a.action_code);

INSERT INTO permission (menu_pk, action_code, permission_code, permission_name, sort_order)
SELECT m.menu_pk, 'READ', 'SYSTEM:READ', '시스템 조회', 1
FROM menu m
WHERE m.menu_code = 'SYSTEM'
  AND NOT EXISTS (
      SELECT 1 FROM permission p
      WHERE p.menu_pk = m.menu_pk AND p.action_code = 'READ');

INSERT INTO role_permission (role_pk, permission_pk)
SELECT r.role_pk, p.permission_pk
FROM role r
JOIN permission p ON TRUE
JOIN menu m ON m.menu_pk = p.menu_pk
WHERE r.role_code = 'TENANT_ADMIN'
  AND m.solution_code = 'MDM'
  AND m.menu_code IN (
      'SYSTEM', 'I18N', 'COMMON_CODE', 'COMPANY', 'DEPT', 'PERSON', 'USER',
      'FILE_STORAGE', 'FILE', 'TEAM', 'ROLE', 'MENU', 'PERMISSION')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_pk = r.role_pk AND rp.permission_pk = p.permission_pk);

INSERT INTO role_permission (role_pk, permission_pk)
SELECT r.role_pk, p.permission_pk
FROM role r
JOIN permission p ON TRUE
JOIN menu m ON m.menu_pk = p.menu_pk
WHERE r.role_code = 'MANAGER'
  AND (
      (p.action_code = 'READ' AND m.menu_code IN ('SYSTEM', 'I18N', 'COMMON_CODE', 'COMPANY', 'DEPT', 'PERSON', 'FILE', 'TEAM'))
      OR (p.action_code IN ('CREATE', 'UPDATE', 'EXPORT') AND m.menu_code IN ('COMPANY', 'DEPT', 'PERSON', 'TEAM', 'FILE'))
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_pk = r.role_pk AND rp.permission_pk = p.permission_pk);

INSERT INTO role_permission (role_pk, permission_pk)
SELECT r.role_pk, p.permission_pk
FROM role r
JOIN permission p ON TRUE
JOIN menu m ON m.menu_pk = p.menu_pk
WHERE r.role_code = 'USER'
  AND p.action_code = 'READ'
  AND m.menu_code IN ('I18N', 'COMMON_CODE', 'COMPANY', 'DEPT', 'PERSON', 'FILE', 'TEAM')
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp
      WHERE rp.role_pk = r.role_pk AND rp.permission_pk = p.permission_pk);
