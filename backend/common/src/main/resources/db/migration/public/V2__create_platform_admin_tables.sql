-- 공용 스키마용. 스키마명은 적지 않는다. 상태/결과/폐기사유 코드는 CHECK 를 두지 않는다.

CREATE SEQUENCE platform_admin_seq INCREMENT BY 1;
CREATE SEQUENCE platform_admin_login_history_seq INCREMENT BY 1;
CREATE SEQUENCE platform_admin_refresh_token_seq INCREMENT BY 1;

CREATE TABLE platform_admin (
    platform_admin_pk BIGINT NOT NULL DEFAULT nextval('platform_admin_seq'),
    login_id VARCHAR(100) NOT NULL,
    admin_name VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_datetime TIMESTAMP,
    must_change_password_yn CHAR(1) NOT NULL DEFAULT 'N',
    admin_status_code VARCHAR(30) NOT NULL,
    login_fail_count INTEGER NOT NULL DEFAULT 0,
    locked_datetime TIMESTAMP,
    last_login_datetime TIMESTAMP,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_platform_admin PRIMARY KEY (platform_admin_pk),
    CONSTRAINT uk_platform_admin_login_id UNIQUE (login_id),
    CONSTRAINT ck_platform_admin_login_id CHECK (login_id = lower(login_id)),
    CONSTRAINT ck_platform_admin_must_change_password_yn CHECK (must_change_password_yn IN ('Y', 'N')),
    CONSTRAINT ck_platform_admin_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE platform_admin_login_history (
    login_history_pk BIGINT NOT NULL DEFAULT nextval('platform_admin_login_history_seq'),
    platform_admin_pk BIGINT,
    login_id_input VARCHAR(100) NOT NULL,
    login_result_code VARCHAR(30) NOT NULL,
    fail_reason VARCHAR(200),
    login_datetime TIMESTAMP NOT NULL,
    logout_datetime TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    CONSTRAINT pk_platform_admin_login_history PRIMARY KEY (login_history_pk),
    CONSTRAINT fk_platform_admin_login_history_platform_admin FOREIGN KEY (platform_admin_pk) REFERENCES platform_admin (platform_admin_pk)
);

CREATE TABLE platform_admin_refresh_token (
    refresh_token_pk BIGINT NOT NULL DEFAULT nextval('platform_admin_refresh_token_seq'),
    platform_admin_pk BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    token_family_id VARCHAR(100) NOT NULL,
    issued_datetime TIMESTAMP NOT NULL,
    expire_datetime TIMESTAMP NOT NULL,
    revoked_datetime TIMESTAMP,
    revoke_reason_code VARCHAR(30),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    CONSTRAINT pk_platform_admin_refresh_token PRIMARY KEY (refresh_token_pk),
    CONSTRAINT uk_platform_admin_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_platform_admin_refresh_token_platform_admin FOREIGN KEY (platform_admin_pk) REFERENCES platform_admin (platform_admin_pk)
);

CREATE INDEX ix_platform_admin_login_history_platform_admin_pk ON platform_admin_login_history (platform_admin_pk);
CREATE INDEX ix_platform_admin_login_history_login_datetime ON platform_admin_login_history (login_datetime);
CREATE INDEX ix_platform_admin_refresh_token_platform_admin_pk ON platform_admin_refresh_token (platform_admin_pk);
