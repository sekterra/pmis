-- 공용 스키마용. 스키마명은 적지 않는다.

ALTER TABLE platform_admin ADD COLUMN password_expire_datetime TIMESTAMP NULL;

CREATE INDEX ix_platform_admin_refresh_token_token_family_id
    ON platform_admin_refresh_token (token_family_id);
