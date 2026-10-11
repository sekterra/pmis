-- 공용 스키마(Flyway default schema)용. 스키마명은 적지 않는다.

CREATE SEQUENCE tenant_seq INCREMENT BY 1;
CREATE SEQUENCE tenant_domain_seq INCREMENT BY 1;
CREATE SEQUENCE tenant_status_history_seq INCREMENT BY 1;
CREATE SEQUENCE tenant_provisioning_seq INCREMENT BY 1;
CREATE SEQUENCE tenant_schema_migration_seq INCREMENT BY 1;

CREATE TABLE tenant (
    tenant_pk BIGINT NOT NULL DEFAULT nextval('tenant_seq'),
    tenant_code VARCHAR(50) NOT NULL,
    tenant_name VARCHAR(200) NOT NULL,
    schema_name VARCHAR(63) NOT NULL,
    tenant_status_code VARCHAR(30) NOT NULL,
    default_language_code VARCHAR(20) NOT NULL,
    timezone_id VARCHAR(50),
    schema_version VARCHAR(50),
    contract_start_date DATE,
    contract_end_date DATE,
    activated_datetime TIMESTAMP,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_tenant PRIMARY KEY (tenant_pk),
    CONSTRAINT uk_tenant_tenant_code UNIQUE (tenant_code),
    CONSTRAINT uk_tenant_schema_name UNIQUE (schema_name),
    CONSTRAINT ck_tenant_tenant_code CHECK (tenant_code ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_tenant_schema_name CHECK (schema_name ~ '^t_[a-z0-9_]{1,61}$'),
    CONSTRAINT ck_tenant_tenant_status_code CHECK (tenant_status_code IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'TERMINATED', 'FAILED')),
    CONSTRAINT ck_tenant_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE tenant_domain (
    tenant_domain_pk BIGINT NOT NULL DEFAULT nextval('tenant_domain_seq'),
    tenant_pk BIGINT NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    domain_type_code VARCHAR(30) NOT NULL,
    primary_yn CHAR(1) NOT NULL DEFAULT 'N',
    verified_yn CHAR(1) NOT NULL DEFAULT 'N',
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_tenant_domain PRIMARY KEY (tenant_domain_pk),
    CONSTRAINT uk_tenant_domain_domain_name UNIQUE (domain_name),
    CONSTRAINT fk_tenant_domain_tenant FOREIGN KEY (tenant_pk) REFERENCES tenant (tenant_pk),
    CONSTRAINT ck_tenant_domain_domain_type_code CHECK (domain_type_code IN ('SUBDOMAIN', 'CUSTOM')),
    CONSTRAINT ck_tenant_domain_primary_yn CHECK (primary_yn IN ('Y', 'N')),
    CONSTRAINT ck_tenant_domain_verified_yn CHECK (verified_yn IN ('Y', 'N')),
    CONSTRAINT ck_tenant_domain_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_tenant_domain_primary_yn
    ON tenant_domain (tenant_pk)
    WHERE primary_yn = 'Y' AND use_yn = 'Y';

CREATE TABLE tenant_status_history (
    history_pk BIGINT NOT NULL DEFAULT nextval('tenant_status_history_seq'),
    tenant_pk BIGINT NOT NULL,
    from_status_code VARCHAR(30),
    to_status_code VARCHAR(30) NOT NULL,
    change_reason VARCHAR(500),
    change_datetime TIMESTAMP NOT NULL,
    change_user_pk BIGINT,
    CONSTRAINT pk_tenant_status_history PRIMARY KEY (history_pk),
    CONSTRAINT fk_tenant_status_history_tenant FOREIGN KEY (tenant_pk) REFERENCES tenant (tenant_pk),
    CONSTRAINT ck_tenant_status_history_from_status_code CHECK (from_status_code IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'TERMINATED', 'FAILED')),
    CONSTRAINT ck_tenant_status_history_to_status_code CHECK (to_status_code IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'TERMINATED', 'FAILED'))
);

CREATE TABLE tenant_provisioning (
    provisioning_pk BIGINT NOT NULL DEFAULT nextval('tenant_provisioning_seq'),
    tenant_pk BIGINT NOT NULL,
    current_step_code VARCHAR(30) NOT NULL,
    execution_status_code VARCHAR(30) NOT NULL,
    admin_name VARCHAR(100) NOT NULL,
    admin_email VARCHAR(200) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    requested_datetime TIMESTAMP NOT NULL,
    completed_datetime TIMESTAMP,
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_tenant_provisioning PRIMARY KEY (provisioning_pk),
    CONSTRAINT fk_tenant_provisioning_tenant FOREIGN KEY (tenant_pk) REFERENCES tenant (tenant_pk),
    CONSTRAINT ck_tenant_provisioning_current_step_code CHECK (current_step_code IN ('REQUESTED', 'SCHEMA_CREATED', 'MIGRATED', 'SEEDED', 'ADMIN_CREATED', 'COMPLETED')),
    CONSTRAINT ck_tenant_provisioning_execution_status_code CHECK (execution_status_code IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE TABLE tenant_schema_migration (
    migration_pk BIGINT NOT NULL DEFAULT nextval('tenant_schema_migration_seq'),
    tenant_pk BIGINT NOT NULL,
    migration_type_code VARCHAR(30) NOT NULL,
    target_version VARCHAR(50) NOT NULL,
    execution_status_code VARCHAR(30) NOT NULL,
    started_datetime TIMESTAMP NOT NULL,
    finished_datetime TIMESTAMP,
    error_message TEXT,
    executed_user_pk BIGINT,
    CONSTRAINT pk_tenant_schema_migration PRIMARY KEY (migration_pk),
    CONSTRAINT fk_tenant_schema_migration_tenant FOREIGN KEY (tenant_pk) REFERENCES tenant (tenant_pk),
    CONSTRAINT ck_tenant_schema_migration_migration_type_code CHECK (migration_type_code IN ('PROVISION', 'UPGRADE', 'REPAIR')),
    CONSTRAINT ck_tenant_schema_migration_execution_status_code CHECK (execution_status_code IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

ALTER SEQUENCE tenant_seq OWNED BY tenant.tenant_pk;
ALTER SEQUENCE tenant_domain_seq OWNED BY tenant_domain.tenant_domain_pk;
ALTER SEQUENCE tenant_status_history_seq OWNED BY tenant_status_history.history_pk;
ALTER SEQUENCE tenant_provisioning_seq OWNED BY tenant_provisioning.provisioning_pk;
ALTER SEQUENCE tenant_schema_migration_seq OWNED BY tenant_schema_migration.migration_pk;

CREATE INDEX ix_tenant_domain_tenant_pk ON tenant_domain (tenant_pk);
CREATE INDEX ix_tenant_status_history_tenant_pk ON tenant_status_history (tenant_pk);
CREATE INDEX ix_tenant_provisioning_tenant_pk ON tenant_provisioning (tenant_pk);
CREATE INDEX ix_tenant_schema_migration_tenant_pk ON tenant_schema_migration (tenant_pk);
