-- 이미 있는 국가는 수정하지 않는다. 기본 언어는 시드된 지원 언어만 참조한다.

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'KR', 'KOR', '410', '한국', 'ko-KR', '+82', 'KRW', 'Asia/Seoul', 1
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'KR');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'US', 'USA', '840', '미국', 'en-US', '+1', 'USD', 'America/New_York', 2
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'US');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'JP', 'JPN', '392', '일본', 'en-US', '+81', 'JPY', 'Asia/Tokyo', 3
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'JP');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'CN', 'CHN', '156', '중국', 'en-US', '+86', 'CNY', 'Asia/Shanghai', 4
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'CN');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'GB', 'GBR', '826', '영국', 'en-US', '+44', 'GBP', 'Europe/London', 5
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'GB');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'DE', 'DEU', '276', '독일', 'en-US', '+49', 'EUR', 'Europe/Berlin', 6
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'DE');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'FR', 'FRA', '250', '프랑스', 'en-US', '+33', 'EUR', 'Europe/Paris', 7
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'FR');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'CA', 'CAN', '124', '캐나다', 'en-US', '+1', 'CAD', 'America/Toronto', 8
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'CA');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'AU', 'AUS', '036', '호주', 'en-US', '+61', 'AUD', 'Australia/Sydney', 9
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'AU');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'SG', 'SGP', '702', '싱가포르', 'en-US', '+65', 'SGD', 'Asia/Singapore', 10
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'SG');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'IN', 'IND', '356', '인도', 'en-US', '+91', 'INR', 'Asia/Kolkata', 11
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'IN');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'VN', 'VNM', '704', '베트남', 'en-US', '+84', 'VND', 'Asia/Ho_Chi_Minh', 12
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'VN');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'TH', 'THA', '764', '태국', 'en-US', '+66', 'THB', 'Asia/Bangkok', 13
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'TH');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'ID', 'IDN', '360', '인도네시아', 'en-US', '+62', 'IDR', 'Asia/Jakarta', 14
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'ID');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'MY', 'MYS', '458', '말레이시아', 'en-US', '+60', 'MYR', 'Asia/Kuala_Lumpur', 15
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'MY');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'PH', 'PHL', '608', '필리핀', 'en-US', '+63', 'PHP', 'Asia/Manila', 16
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'PH');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'TW', 'TWN', '158', '대만', 'en-US', '+886', 'TWD', 'Asia/Taipei', 17
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'TW');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'HK', 'HKG', '344', '홍콩', 'en-US', '+852', 'HKD', 'Asia/Hong_Kong', 18
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'HK');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'AE', 'ARE', '784', '아랍에미리트', 'en-US', '+971', 'AED', 'Asia/Dubai', 19
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'AE');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'SA', 'SAU', '682', '사우디아라비아', 'en-US', '+966', 'SAR', 'Asia/Riyadh', 20
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'SA');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'BR', 'BRA', '076', '브라질', 'en-US', '+55', 'BRL', 'America/Sao_Paulo', 21
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'BR');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'MX', 'MEX', '484', '멕시코', 'en-US', '+52', 'MXN', 'America/Mexico_City', 22
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'MX');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'RU', 'RUS', '643', '러시아', 'en-US', '+7', 'RUB', 'Europe/Moscow', 23
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'RU');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'IT', 'ITA', '380', '이탈리아', 'en-US', '+39', 'EUR', 'Europe/Rome', 24
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'IT');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'ES', 'ESP', '724', '스페인', 'en-US', '+34', 'EUR', 'Europe/Madrid', 25
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'ES');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'NL', 'NLD', '528', '네덜란드', 'en-US', '+31', 'EUR', 'Europe/Amsterdam', 26
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'NL');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'CH', 'CHE', '756', '스위스', 'en-US', '+41', 'CHF', 'Europe/Zurich', 27
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'CH');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'SE', 'SWE', '752', '스웨덴', 'en-US', '+46', 'SEK', 'Europe/Stockholm', 28
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'SE');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'NZ', 'NZL', '554', '뉴질랜드', 'en-US', '+64', 'NZD', 'Pacific/Auckland', 29
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'NZ');

INSERT INTO nation (
    nation_code, iso_alpha3_code, iso_numeric_code, nation_name, default_language_code,
    phone_code, currency_code, timezone_id, sort_order)
SELECT 'TR', 'TUR', '792', '튀르키예', 'en-US', '+90', 'TRY', 'Europe/Istanbul', 30
WHERE NOT EXISTS (SELECT 1 FROM nation WHERE nation_code = 'TR');
