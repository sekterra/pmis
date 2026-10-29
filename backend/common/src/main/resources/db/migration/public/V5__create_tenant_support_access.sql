-- 공용 스키마용. 스키마명은 적지 않는다. 지원 접속 행은 삭제 API가 없다.

CREATE SEQUENCE tenant_support_access_seq INCREMENT BY 1;
CREATE SEQUENCE tenant_support_access_action_seq INCREMENT BY 1;

CREATE TABLE tenant_support_access (
    support_access_pk BIGINT NOT NULL DEFAULT nextval('tenant_support_access_seq'),
    tenant_pk BIGINT NOT NULL,
    platform_admin_pk BIGINT NOT NULL,
    platform_login_id VARCHAR(100) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    reason_default_yn CHAR(1) NOT NULL,
    status_code VARCHAR(30) NOT NULL,
    ticket_hash VARCHAR(64) NOT NULL,
    ticket_expire_datetime TIMESTAMP NOT NULL,
    issued_datetime TIMESTAMP NOT NULL,
    started_datetime TIMESTAMP,
    expire_datetime TIMESTAMP,
    ended_datetime TIMESTAMP,
    end_reason_code VARCHAR(30),
    issue_ip VARCHAR(45),
    start_ip VARCHAR(45),
    user_agent VARCHAR(500),
    CONSTRAINT pk_tenant_support_access PRIMARY KEY (support_access_pk),
    CONSTRAINT uk_tenant_support_access_ticket_hash UNIQUE (ticket_hash),
    CONSTRAINT fk_tenant_support_access_tenant FOREIGN KEY (tenant_pk) REFERENCES tenant (tenant_pk),
    CONSTRAINT fk_tenant_support_access_platform_admin FOREIGN KEY (platform_admin_pk) REFERENCES platform_admin (platform_admin_pk),
    CONSTRAINT ck_tenant_support_access_reason_default_yn CHECK (reason_default_yn IN ('Y', 'N')),
    CONSTRAINT ck_tenant_support_access_status_code CHECK (status_code IN ('ISSUED', 'ACTIVE', 'ENDED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX ix_tenant_support_access_tenant_issued
    ON tenant_support_access (tenant_pk, issued_datetime DESC);

CREATE INDEX ix_tenant_support_access_admin_issued
    ON tenant_support_access (platform_admin_pk, issued_datetime);

CREATE TABLE tenant_support_access_action (
    action_pk BIGINT NOT NULL DEFAULT nextval('tenant_support_access_action_seq'),
    support_access_pk BIGINT NOT NULL,
    occurred_datetime TIMESTAMP NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    status_code INTEGER,
    sensitive_yn CHAR(1) NOT NULL,
    CONSTRAINT pk_tenant_support_access_action PRIMARY KEY (action_pk),
    CONSTRAINT fk_tenant_support_access_action_access FOREIGN KEY (support_access_pk) REFERENCES tenant_support_access (support_access_pk),
    CONSTRAINT ck_tenant_support_access_action_sensitive_yn CHECK (sensitive_yn IN ('Y', 'N'))
);

CREATE INDEX ix_tenant_support_access_action_occurred
    ON tenant_support_access_action (support_access_pk, occurred_datetime);
