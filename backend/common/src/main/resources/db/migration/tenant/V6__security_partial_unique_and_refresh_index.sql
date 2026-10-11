-- 사업자번호는 사용 중인 행만 유일하다. Refresh 가족 폐기를 위해 인덱스를 둔다.

DROP INDEX uk_company_business_registration_no;
CREATE UNIQUE INDEX uk_company_business_registration_no
    ON company (business_registration_no)
    WHERE business_registration_no IS NOT NULL AND use_yn = 'Y';

CREATE INDEX ix_user_refresh_token_token_family_id
    ON user_refresh_token (token_family_id);
