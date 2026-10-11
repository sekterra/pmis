-- 이미 있는 코드 그룹과 코드는 수정하지 않는다. INSERT ... WHERE NOT EXISTS.

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'STORAGE_TYPE', 'MDM', '저장소 유형', 'N', 1
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'STORAGE_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '저장소 유형'
FROM code_group g
WHERE g.code_group_code = 'STORAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Storage type'
FROM code_group g
WHERE g.code_group_code = 'STORAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'LOCAL', '서버 디스크', 1
FROM code_group g
WHERE g.code_group_code = 'STORAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'LOCAL');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '서버 디스크'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'LOCAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Local disk'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'LOCAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'NAS', 'NAS', 2
FROM code_group g
WHERE g.code_group_code = 'STORAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'NAS');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', 'NAS'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'NAS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'NAS'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'NAS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'S3', 'S3', 3
FROM code_group g
WHERE g.code_group_code = 'STORAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'S3');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', 'S3'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'S3'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'S3'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'S3'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'AZURE_BLOB', 'Azure Blob', 4
FROM code_group g
WHERE g.code_group_code = 'STORAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'AZURE_BLOB');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', 'Azure Blob'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'AZURE_BLOB'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Azure Blob'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'AZURE_BLOB'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'GCS', 'GCS', 5
FROM code_group g
WHERE g.code_group_code = 'STORAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'GCS');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', 'GCS'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'GCS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'GCS'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'STORAGE_TYPE' AND c.code_value = 'GCS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'FILE_STATUS', 'MDM', '파일 상태', 'N', 2
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'FILE_STATUS');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '파일 상태'
FROM code_group g
WHERE g.code_group_code = 'FILE_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'File status'
FROM code_group g
WHERE g.code_group_code = 'FILE_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'TEMP', '임시', 1
FROM code_group g
WHERE g.code_group_code = 'FILE_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'TEMP');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '임시'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'FILE_STATUS' AND c.code_value = 'TEMP'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Temporary'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'FILE_STATUS' AND c.code_value = 'TEMP'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ACTIVE', '사용 중', 2
FROM code_group g
WHERE g.code_group_code = 'FILE_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ACTIVE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '사용 중'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'FILE_STATUS' AND c.code_value = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Active'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'FILE_STATUS' AND c.code_value = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'DELETED', '삭제 대기', 3
FROM code_group g
WHERE g.code_group_code = 'FILE_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'DELETED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '삭제 대기'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'FILE_STATUS' AND c.code_value = 'DELETED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Pending deletion'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'FILE_STATUS' AND c.code_value = 'DELETED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'ATTACHMENT_CATEGORY', 'MDM', '첨부 구분', 'N', 3
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'ATTACHMENT_CATEGORY');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '첨부 구분'
FROM code_group g
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Attachment category'
FROM code_group g
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'GENERAL', '일반', 1
FROM code_group g
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'GENERAL');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'ko-KR', '일반', '일반 첨부'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'GENERAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'en-US', 'General', 'General attachment'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'GENERAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'PROFILE', '프로필', 2
FROM code_group g
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'PROFILE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'ko-KR', '프로필', '프로필 첨부'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'PROFILE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'en-US', 'Profile', 'Profile attachment'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'PROFILE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'PHOTO', '사진', 3
FROM code_group g
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'PHOTO');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'ko-KR', '사진', '사진 첨부'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'PHOTO'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'en-US', 'Photo', 'Photo attachment'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'PHOTO'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CONTRACT', '계약서', 4
FROM code_group g
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CONTRACT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'ko-KR', '계약서', '계약서 첨부'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'CONTRACT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'en-US', 'Contract', 'Contract attachment'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ATTACHMENT_CATEGORY' AND c.code_value = 'CONTRACT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'TEAM_TYPE', 'MDM', '팀 유형', 'N', 4
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'TEAM_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '팀 유형'
FROM code_group g
WHERE g.code_group_code = 'TEAM_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Team type'
FROM code_group g
WHERE g.code_group_code = 'TEAM_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'PROJECT', '프로젝트 팀', 1
FROM code_group g
WHERE g.code_group_code = 'TEAM_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'PROJECT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '프로젝트 팀'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'PROJECT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Project'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'PROJECT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'TASKFORCE', 'TF', 2
FROM code_group g
WHERE g.code_group_code = 'TEAM_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'TASKFORCE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', 'TF'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'TASKFORCE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Task force'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'TASKFORCE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'COMMITTEE', '위원회', 3
FROM code_group g
WHERE g.code_group_code = 'TEAM_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'COMMITTEE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '위원회'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'COMMITTEE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Committee'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'COMMITTEE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ETC', '기타', 4
FROM code_group g
WHERE g.code_group_code = 'TEAM_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ETC');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '기타'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'ETC'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Other'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_TYPE' AND c.code_value = 'ETC'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'TEAM_ROLE', 'MDM', '팀 역할', 'N', 5
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'TEAM_ROLE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '팀 역할'
FROM code_group g
WHERE g.code_group_code = 'TEAM_ROLE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Team role'
FROM code_group g
WHERE g.code_group_code = 'TEAM_ROLE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'LEADER', '팀장', 1
FROM code_group g
WHERE g.code_group_code = 'TEAM_ROLE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'LEADER');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '팀장'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_ROLE' AND c.code_value = 'LEADER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Leader'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_ROLE' AND c.code_value = 'LEADER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'MEMBER', '팀원', 2
FROM code_group g
WHERE g.code_group_code = 'TEAM_ROLE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'MEMBER');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '팀원'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_ROLE' AND c.code_value = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Member'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_ROLE' AND c.code_value = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ADVISOR', '자문', 3
FROM code_group g
WHERE g.code_group_code = 'TEAM_ROLE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ADVISOR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '자문'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_ROLE' AND c.code_value = 'ADVISOR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Advisor'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TEAM_ROLE' AND c.code_value = 'ADVISOR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'MENU_TYPE', 'MDM', '메뉴 유형', 'N', 6
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'MENU_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '메뉴 유형'
FROM code_group g
WHERE g.code_group_code = 'MENU_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Menu type'
FROM code_group g
WHERE g.code_group_code = 'MENU_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'GROUP', '그룹', 1
FROM code_group g
WHERE g.code_group_code = 'MENU_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'GROUP');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '그룹'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'MENU_TYPE' AND c.code_value = 'GROUP'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Group'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'MENU_TYPE' AND c.code_value = 'GROUP'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'PAGE', '화면', 2
FROM code_group g
WHERE g.code_group_code = 'MENU_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'PAGE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '화면'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'MENU_TYPE' AND c.code_value = 'PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Page'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'MENU_TYPE' AND c.code_value = 'PAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'PERMISSION_ACTION', 'MDM', '기능', 'N', 7
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'PERMISSION_ACTION');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '기능'
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Permission action'
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'READ', '조회', 1
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'READ');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '조회'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'READ'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Read'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'READ'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CREATE', '등록', 2
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CREATE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '등록'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'CREATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Create'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'CREATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'UPDATE', '수정', 3
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'UPDATE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '수정'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'UPDATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Update'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'UPDATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'DELETE', '삭제', 4
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'DELETE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '삭제'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'DELETE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Delete'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'DELETE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'EXPORT', '내보내기', 5
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'EXPORT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '내보내기'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Export'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'APPROVE', '승인', 6
FROM code_group g
WHERE g.code_group_code = 'PERMISSION_ACTION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'APPROVE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '승인'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'APPROVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Approve'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PERMISSION_ACTION' AND c.code_value = 'APPROVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'COMPANY_TYPE', 'MDM', '회사 유형', 'N', 8
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'COMPANY_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '회사 유형'
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Company type'
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'OWN', '자사', 1
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'OWN');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '자사'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'OWN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Own company'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'OWN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'AFFILIATE', '계열사', 2
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'AFFILIATE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '계열사'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'AFFILIATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Affiliate'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'AFFILIATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CLIENT', '발주사', 3
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CLIENT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '발주사'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'CLIENT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Client'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'CLIENT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CONTRACTOR', '시공사', 4
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CONTRACTOR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '시공사'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'CONTRACTOR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Contractor'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'CONTRACTOR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'PARTNER', '협력업체', 5
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'PARTNER');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '협력업체'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'PARTNER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Partner'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'PARTNER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'OTHER', '기타', 6
FROM code_group g
WHERE g.code_group_code = 'COMPANY_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'OTHER');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '기타'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'OTHER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Other'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'COMPANY_TYPE' AND c.code_value = 'OTHER'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'DEPT_TYPE', 'MDM', '부서 유형', 'N', 9
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'DEPT_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '부서 유형'
FROM code_group g
WHERE g.code_group_code = 'DEPT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Department type'
FROM code_group g
WHERE g.code_group_code = 'DEPT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'DIVISION', '본부', 1
FROM code_group g
WHERE g.code_group_code = 'DEPT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'DIVISION');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '본부'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'DIVISION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Division'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'DIVISION'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'DEPARTMENT', '부', 2
FROM code_group g
WHERE g.code_group_code = 'DEPT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'DEPARTMENT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '부'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'DEPARTMENT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Department'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'DEPARTMENT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'TEAM', '팀', 3
FROM code_group g
WHERE g.code_group_code = 'DEPT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'TEAM');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '팀'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'TEAM'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Team'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'TEAM'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'PROJECT', '프로젝트 조직', 4
FROM code_group g
WHERE g.code_group_code = 'DEPT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'PROJECT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '프로젝트 조직'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'PROJECT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Project organization'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'PROJECT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ETC', '기타', 5
FROM code_group g
WHERE g.code_group_code = 'DEPT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ETC');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '기타'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'ETC'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Other'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_TYPE' AND c.code_value = 'ETC'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'DEPT_CHANGE_TYPE', 'MDM', '부서 변경 유형', 'N', 10
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'DEPT_CHANGE_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '부서 변경 유형'
FROM code_group g
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Department change type'
FROM code_group g
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CREATE', '신설', 1
FROM code_group g
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CREATE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '신설'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'CREATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Create'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'CREATE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'RENAME', '명칭변경', 2
FROM code_group g
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'RENAME');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '명칭변경'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'RENAME'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Rename'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'RENAME'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'MOVE', '상위이동', 3
FROM code_group g
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'MOVE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '상위이동'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'MOVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Move'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'MOVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CLOSE', '폐지', 4
FROM code_group g
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CLOSE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '폐지'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'CLOSE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Close'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'CLOSE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'REOPEN', '재개', 5
FROM code_group g
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'REOPEN');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '재개'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'REOPEN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Reopen'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'DEPT_CHANGE_TYPE' AND c.code_value = 'REOPEN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'EMPLOYMENT_TYPE', 'MDM', '고용 유형', 'N', 11
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'EMPLOYMENT_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '고용 유형'
FROM code_group g
WHERE g.code_group_code = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Employment type'
FROM code_group g
WHERE g.code_group_code = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'REGULAR', '정규직', 1
FROM code_group g
WHERE g.code_group_code = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'REGULAR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '정규직'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'REGULAR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Regular'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'REGULAR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CONTRACT', '계약직', 2
FROM code_group g
WHERE g.code_group_code = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CONTRACT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '계약직'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'CONTRACT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Contract'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'CONTRACT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'DISPATCH', '파견', 3
FROM code_group g
WHERE g.code_group_code = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'DISPATCH');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '파견'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'DISPATCH'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Dispatch'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'DISPATCH'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'EXTERNAL', '외부 인력', 4
FROM code_group g
WHERE g.code_group_code = 'EMPLOYMENT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'EXTERNAL');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '외부 인력'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'EXTERNAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'External'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'EMPLOYMENT_TYPE' AND c.code_value = 'EXTERNAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'POSITION', 'MDM', '직위', 'N', 12
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'POSITION');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '직위'
FROM code_group g
WHERE g.code_group_code = 'POSITION'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Position'
FROM code_group g
WHERE g.code_group_code = 'POSITION'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'JOB_TITLE', 'MDM', '직책', 'N', 13
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'JOB_TITLE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '직책'
FROM code_group g
WHERE g.code_group_code = 'JOB_TITLE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Job title'
FROM code_group g
WHERE g.code_group_code = 'JOB_TITLE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'ASSIGNMENT_STATUS', 'MDM', '재직 상태', 'N', 14
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'ASSIGNMENT_STATUS');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '재직 상태'
FROM code_group g
WHERE g.code_group_code = 'ASSIGNMENT_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Assignment status'
FROM code_group g
WHERE g.code_group_code = 'ASSIGNMENT_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ACTIVE', '재직', 1
FROM code_group g
WHERE g.code_group_code = 'ASSIGNMENT_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ACTIVE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '재직'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ASSIGNMENT_STATUS' AND c.code_value = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Active'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ASSIGNMENT_STATUS' AND c.code_value = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'LEAVE', '휴직', 2
FROM code_group g
WHERE g.code_group_code = 'ASSIGNMENT_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'LEAVE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '휴직'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ASSIGNMENT_STATUS' AND c.code_value = 'LEAVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Leave'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ASSIGNMENT_STATUS' AND c.code_value = 'LEAVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'RESIGNED', '퇴사', 3
FROM code_group g
WHERE g.code_group_code = 'ASSIGNMENT_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'RESIGNED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '퇴사'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ASSIGNMENT_STATUS' AND c.code_value = 'RESIGNED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Resigned'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ASSIGNMENT_STATUS' AND c.code_value = 'RESIGNED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'ACCOUNT_TYPE', 'MDM', '계정 유형', 'N', 15
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'ACCOUNT_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '계정 유형'
FROM code_group g
WHERE g.code_group_code = 'ACCOUNT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Account type'
FROM code_group g
WHERE g.code_group_code = 'ACCOUNT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'NORMAL', '일반 사용자', 1
FROM code_group g
WHERE g.code_group_code = 'ACCOUNT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'NORMAL');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '일반 사용자'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ACCOUNT_TYPE' AND c.code_value = 'NORMAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Normal'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ACCOUNT_TYPE' AND c.code_value = 'NORMAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'SYSTEM', '시스템 계정', 2
FROM code_group g
WHERE g.code_group_code = 'ACCOUNT_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'SYSTEM');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '시스템 계정'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ACCOUNT_TYPE' AND c.code_value = 'SYSTEM'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'System'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'ACCOUNT_TYPE' AND c.code_value = 'SYSTEM'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'USER_STATUS', 'MDM', '사용자 상태', 'N', 16
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'USER_STATUS');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '사용자 상태'
FROM code_group g
WHERE g.code_group_code = 'USER_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'User status'
FROM code_group g
WHERE g.code_group_code = 'USER_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ACTIVE', '정상', 1
FROM code_group g
WHERE g.code_group_code = 'USER_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ACTIVE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '정상'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Active'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'LOCKED', '잠금', 2
FROM code_group g
WHERE g.code_group_code = 'USER_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'LOCKED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '잠금'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'LOCKED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Locked'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'LOCKED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'DORMANT', '휴면', 3
FROM code_group g
WHERE g.code_group_code = 'USER_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'DORMANT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '휴면'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'DORMANT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Dormant'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'DORMANT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'WITHDRAWN', '탈퇴', 4
FROM code_group g
WHERE g.code_group_code = 'USER_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'WITHDRAWN');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '탈퇴'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'WITHDRAWN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Withdrawn'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'USER_STATUS' AND c.code_value = 'WITHDRAWN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'PASSWORD_CHANGE_TYPE', 'MDM', '비밀번호 변경 유형', 'N', 17
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'PASSWORD_CHANGE_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '비밀번호 변경 유형'
FROM code_group g
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Password change type'
FROM code_group g
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'INITIAL', '최초', 1
FROM code_group g
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'INITIAL');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '최초'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'INITIAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Initial'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'INITIAL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'CHANGE', '본인 변경', 2
FROM code_group g
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'CHANGE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '본인 변경'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'CHANGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Change'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'CHANGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'RESET', '관리자 초기화', 3
FROM code_group g
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'RESET');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '관리자 초기화'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'RESET'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Reset'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'RESET'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'EXPIRED_CHANGE', '만료 후 변경', 4
FROM code_group g
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'EXPIRED_CHANGE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '만료 후 변경'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'EXPIRED_CHANGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Expired change'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'PASSWORD_CHANGE_TYPE' AND c.code_value = 'EXPIRED_CHANGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'LOGIN_RESULT', 'MDM', '로그인 결과', 'N', 18
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'LOGIN_RESULT');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '로그인 결과'
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Login result'
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'SUCCESS', '성공', 1
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'SUCCESS');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '성공'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'SUCCESS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Success'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'SUCCESS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'FAIL_PASSWORD', '비밀번호 오류', 2
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'FAIL_PASSWORD');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '비밀번호 오류'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_PASSWORD'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Wrong password'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_PASSWORD'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'FAIL_LOCKED', '잠금', 3
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'FAIL_LOCKED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '잠금'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_LOCKED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Locked'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_LOCKED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'FAIL_INACTIVE', '비활성', 4
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'FAIL_INACTIVE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '비활성'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_INACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Inactive'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_INACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'FAIL_NOT_FOUND', '계정 없음', 5
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'FAIL_NOT_FOUND');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '계정 없음'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_NOT_FOUND'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Not found'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_NOT_FOUND'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'FAIL_EXPIRED', '만료', 6
FROM code_group g
WHERE g.code_group_code = 'LOGIN_RESULT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'FAIL_EXPIRED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '만료'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_EXPIRED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Expired'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'LOGIN_RESULT' AND c.code_value = 'FAIL_EXPIRED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'TOKEN_REVOKE_REASON', 'MDM', '토큰 폐기 사유', 'N', 19
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'TOKEN_REVOKE_REASON');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '토큰 폐기 사유'
FROM code_group g
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Token revoke reason'
FROM code_group g
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'LOGOUT', '로그아웃', 1
FROM code_group g
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'LOGOUT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '로그아웃'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'LOGOUT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Logout'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'LOGOUT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ROTATED', '회전', 2
FROM code_group g
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ROTATED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '회전'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'ROTATED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Rotated'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'ROTATED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'REUSE_DETECTED', '재사용 감지', 3
FROM code_group g
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'REUSE_DETECTED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '재사용 감지'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'REUSE_DETECTED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Reuse detected'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'REUSE_DETECTED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ADMIN', '관리자', 4
FROM code_group g
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ADMIN');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '관리자'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Admin'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'PASSWORD_CHANGED', '비밀번호 변경', 5
FROM code_group g
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'PASSWORD_CHANGED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '비밀번호 변경'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'PASSWORD_CHANGED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Password changed'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'TOKEN_REVOKE_REASON' AND c.code_value = 'PASSWORD_CHANGED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'I18N_MESSAGE_TYPE', 'MDM', '메시지 유형', 'N', 20
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'I18N_MESSAGE_TYPE');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '메시지 유형'
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Message type'
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'LABEL', '라벨', 1
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'LABEL');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '라벨'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'LABEL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Label'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'LABEL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'MESSAGE', '메시지', 2
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'MESSAGE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '메시지'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'MESSAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Message'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'MESSAGE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'ERROR', '오류', 3
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'ERROR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '오류'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'ERROR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Error'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'ERROR'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'TOOLTIP', '툴팁', 4
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'TOOLTIP');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '툴팁'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'TOOLTIP'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Tooltip'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'TOOLTIP'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'MENU', '메뉴', 5
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'MENU');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '메뉴'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'MENU'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Menu'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'MENU'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'NOTICE', '공지', 6
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'NOTICE');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '공지'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'NOTICE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Notice'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'NOTICE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'MAIL', '메일', 7
FROM code_group g
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'MAIL');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '메일'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'MAIL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Mail'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_MESSAGE_TYPE' AND c.code_value = 'MAIL'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'I18N_TRANSLATION_STATUS', 'MDM', '번역 상태', 'N', 21
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'I18N_TRANSLATION_STATUS');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '번역 상태'
FROM code_group g
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Translation status'
FROM code_group g
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'DRAFT', '작성 중', 1
FROM code_group g
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'DRAFT');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '작성 중'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'DRAFT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Draft'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'DRAFT'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'TRANSLATED', '번역됨', 2
FROM code_group g
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'TRANSLATED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '번역됨'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'TRANSLATED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Translated'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'TRANSLATED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'REVIEWED', '검토됨', 3
FROM code_group g
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'REVIEWED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '검토됨'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'REVIEWED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Reviewed'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'REVIEWED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order)
SELECT g.code_group_pk, 'APPROVED', '승인됨', 4
FROM code_group g
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'APPROVED');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'ko-KR', '승인됨'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name)
SELECT c.code_pk, 'en-US', 'Approved'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'I18N_TRANSLATION_STATUS' AND c.code_value = 'APPROVED'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO code_group (code_group_code, solution_code, code_group_name, hierarchy_yn, sort_order)
SELECT 'SECURITY_POLICY', 'MDM', '보안 정책', 'N', 22
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE code_group_code = 'SECURITY_POLICY');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'ko-KR', '보안 정책'
FROM code_group g
WHERE g.code_group_code = 'SECURITY_POLICY'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'ko-KR');

INSERT INTO code_group_lang (code_group_pk, support_language_code, code_group_name)
SELECT g.code_group_pk, 'en-US', 'Security Policy'
FROM code_group g
WHERE g.code_group_code = 'SECURITY_POLICY'
  AND NOT EXISTS (
      SELECT 1 FROM code_group_lang l
      WHERE l.code_group_pk = g.code_group_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order, ext_attr1)
SELECT g.code_group_pk, 'TEMP_PASSWORD_VALID_DAYS', '임시 비밀번호 유효 일수', 1, '7'
FROM code_group g
WHERE g.code_group_code = 'SECURITY_POLICY'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = 'TEMP_PASSWORD_VALID_DAYS');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'ko-KR', '임시 비밀번호 유효 일수', '임시 비밀번호를 발급한 뒤 사용할 수 있는 일수'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'SECURITY_POLICY' AND c.code_value = 'TEMP_PASSWORD_VALID_DAYS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'ko-KR');

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, 'en-US', 'Temporary password validity days', 'Number of days a temporary password stays valid'
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
WHERE g.code_group_code = 'SECURITY_POLICY' AND c.code_value = 'TEMP_PASSWORD_VALID_DAYS'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = 'en-US');

INSERT INTO common_code (code_group_pk, code_value, code_name, sort_order, ext_attr1)
SELECT g.code_group_pk, v.code_value, v.code_name, v.sort_order, v.ext_attr1
FROM code_group g
JOIN (VALUES
    ('PASSWORD_EXPIRE_DAYS', '비밀번호 만료 일수', 2, '90'),
    ('PASSWORD_MIN_LENGTH', '비밀번호 최소 길이', 3, '10'),
    ('PASSWORD_MIN_CLASSES', '비밀번호 문자 종류', 4, '3'),
    ('PASSWORD_HISTORY_COUNT', '비밀번호 이력 개수', 5, '5'),
    ('MAX_LOGIN_FAIL_COUNT', '로그인 실패 허용 횟수', 6, '5'),
    ('LOCK_MINUTES', '계정 잠금 분', 7, '30'),
    ('ACCESS_TOKEN_MINUTES', '액세스 토큰 분', 8, '15'),
    ('REFRESH_TOKEN_DAYS', '리프레시 토큰 일수', 9, '14')
) AS v(code_value, code_name, sort_order, ext_attr1) ON TRUE
WHERE g.code_group_code = 'SECURITY_POLICY'
  AND NOT EXISTS (
      SELECT 1 FROM common_code c
      WHERE c.code_group_pk = g.code_group_pk AND c.code_value = v.code_value);

INSERT INTO common_code_lang (code_pk, support_language_code, code_name, description)
SELECT c.code_pk, v.support_language_code, v.code_name, v.description
FROM common_code c
JOIN code_group g ON g.code_group_pk = c.code_group_pk
JOIN (VALUES
    ('PASSWORD_EXPIRE_DAYS', 'ko-KR', '비밀번호 만료 일수', '비밀번호를 바꾼 뒤 만료되는 일수'),
    ('PASSWORD_EXPIRE_DAYS', 'en-US', 'Password expiry days', 'Days until a changed password expires'),
    ('PASSWORD_MIN_LENGTH', 'ko-KR', '비밀번호 최소 길이', '비밀번호 최소 글자 수'),
    ('PASSWORD_MIN_LENGTH', 'en-US', 'Minimum password length', 'Minimum number of characters'),
    ('PASSWORD_MIN_CLASSES', 'ko-KR', '비밀번호 문자 종류', '대문자, 소문자, 숫자, 특수문자 중 최소 종류'),
    ('PASSWORD_MIN_CLASSES', 'en-US', 'Password character classes', 'Minimum classes among upper, lower, digit, and symbol'),
    ('PASSWORD_HISTORY_COUNT', 'ko-KR', '비밀번호 이력 개수', '최근 비밀번호를 다시 쓰지 못하는 개수'),
    ('PASSWORD_HISTORY_COUNT', 'en-US', 'Password history count', 'How many recent passwords cannot be reused'),
    ('MAX_LOGIN_FAIL_COUNT', 'ko-KR', '로그인 실패 허용 횟수', '이 횟수에 도달하면 계정을 잠근다'),
    ('MAX_LOGIN_FAIL_COUNT', 'en-US', 'Maximum login failures', 'Lock the account when failures reach this count'),
    ('LOCK_MINUTES', 'ko-KR', '계정 잠금 분', '잠금 후 자동 해제까지 분. 0이면 자동 해제 없음'),
    ('LOCK_MINUTES', 'en-US', 'Lock minutes', 'Minutes before automatic unlock. Zero means no automatic unlock'),
    ('ACCESS_TOKEN_MINUTES', 'ko-KR', '액세스 토큰 분', '액세스 토큰 수명'),
    ('ACCESS_TOKEN_MINUTES', 'en-US', 'Access token minutes', 'Access token lifetime'),
    ('REFRESH_TOKEN_DAYS', 'ko-KR', '리프레시 토큰 일수', '리프레시 토큰 수명'),
    ('REFRESH_TOKEN_DAYS', 'en-US', 'Refresh token days', 'Refresh token lifetime')
) AS v(code_value, support_language_code, code_name, description) ON v.code_value = c.code_value
WHERE g.code_group_code = 'SECURITY_POLICY'
  AND NOT EXISTS (
      SELECT 1 FROM common_code_lang l
      WHERE l.code_pk = c.code_pk AND l.support_language_code = v.support_language_code);
