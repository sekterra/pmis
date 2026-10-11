-- 이미 있는 지원 언어는 수정하지 않는다. INSERT ... WHERE NOT EXISTS.

INSERT INTO support_language (
    support_language_code, language_name, native_name, iso_language_code,
    fallback_language_code, text_direction_code, date_format, datetime_format, number_format,
    default_yn, sort_order)
SELECT 'ko-KR', '한국어', '한국어', 'ko', NULL, 'LTR', 'yyyy-MM-dd', 'yyyy-MM-dd HH:mm', '#,##0.##', 'Y', 1
WHERE NOT EXISTS (
    SELECT 1 FROM support_language WHERE support_language_code = 'ko-KR');

INSERT INTO support_language (
    support_language_code, language_name, native_name, iso_language_code,
    fallback_language_code, text_direction_code, date_format, datetime_format, number_format,
    default_yn, sort_order)
SELECT 'en-US', '영어', 'English', 'en', 'ko-KR', 'LTR', 'MM/dd/yyyy', 'MM/dd/yyyy HH:mm', '#,##0.##', 'N', 2
WHERE NOT EXISTS (
    SELECT 1 FROM support_language WHERE support_language_code = 'en-US');
