-- 테넌트 스키마용. 스키마명은 적지 않는다. Seed 데이터는 넣지 않는다.
-- person.profile_file_pk 의 file FK 는 file 테이블이 생긴 뒤 마이그레이션에서 추가한다.

CREATE SEQUENCE company_seq INCREMENT BY 1;
CREATE SEQUENCE dept_seq INCREMENT BY 1;
CREATE SEQUENCE dept_history_seq INCREMENT BY 1;
CREATE SEQUENCE person_seq INCREMENT BY 1;
CREATE SEQUENCE person_assignment_seq INCREMENT BY 1;
CREATE SEQUENCE user_account_seq INCREMENT BY 1;
CREATE SEQUENCE user_password_history_seq INCREMENT BY 1;
CREATE SEQUENCE user_login_history_seq INCREMENT BY 1;
CREATE SEQUENCE user_refresh_token_seq INCREMENT BY 1;

CREATE TABLE company (
    company_pk BIGINT NOT NULL DEFAULT nextval('company_seq'),
    parent_company_pk BIGINT,
    company_code VARCHAR(50) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    company_short_name VARCHAR(100),
    company_type_code VARCHAR(30) NOT NULL,
    business_registration_no VARCHAR(20),
    corporate_registration_no VARCHAR(20),
    representative_name VARCHAR(100),
    nation_pk BIGINT,
    postal_code VARCHAR(20),
    address VARCHAR(500),
    address_detail VARCHAR(500),
    phone_no VARCHAR(30),
    fax_no VARCHAR(30),
    email VARCHAR(200),
    website_url VARCHAR(500),
    sort_order INTEGER,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_company PRIMARY KEY (company_pk),
    CONSTRAINT uk_company_company_code UNIQUE (company_code),
    CONSTRAINT fk_company_company FOREIGN KEY (parent_company_pk) REFERENCES company (company_pk),
    CONSTRAINT fk_company_nation FOREIGN KEY (nation_pk) REFERENCES nation (nation_pk),
    CONSTRAINT ck_company_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_company_business_registration_no
    ON company (business_registration_no)
    WHERE business_registration_no IS NOT NULL;

CREATE TABLE dept (
    dept_pk BIGINT NOT NULL DEFAULT nextval('dept_seq'),
    company_pk BIGINT NOT NULL,
    parent_dept_pk BIGINT,
    dept_code VARCHAR(50) NOT NULL,
    dept_name VARCHAR(200) NOT NULL,
    dept_type_code VARCHAR(30),
    level_no INTEGER,
    dept_path VARCHAR(500),
    sort_order INTEGER,
    valid_from_date DATE NOT NULL,
    valid_to_date DATE,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_dept PRIMARY KEY (dept_pk),
    CONSTRAINT uk_dept_company_pk_dept_code UNIQUE (company_pk, dept_code),
    CONSTRAINT fk_dept_company FOREIGN KEY (company_pk) REFERENCES company (company_pk),
    CONSTRAINT fk_dept_dept FOREIGN KEY (parent_dept_pk) REFERENCES dept (dept_pk),
    CONSTRAINT ck_dept_valid_date CHECK (valid_from_date IS NULL OR valid_to_date IS NULL OR valid_from_date <= valid_to_date),
    CONSTRAINT ck_dept_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE dept_history (
    history_pk BIGINT NOT NULL DEFAULT nextval('dept_history_seq'),
    dept_pk BIGINT NOT NULL,
    change_type_code VARCHAR(30) NOT NULL,
    effective_date DATE NOT NULL,
    before_dept_name VARCHAR(200),
    after_dept_name VARCHAR(200),
    before_parent_dept_pk BIGINT,
    after_parent_dept_pk BIGINT,
    change_reason VARCHAR(500),
    change_datetime TIMESTAMP NOT NULL,
    change_user_pk BIGINT,
    CONSTRAINT pk_dept_history PRIMARY KEY (history_pk),
    CONSTRAINT fk_dept_history_dept FOREIGN KEY (dept_pk) REFERENCES dept (dept_pk)
);

CREATE TABLE person (
    person_pk BIGINT NOT NULL DEFAULT nextval('person_seq'),
    person_name VARCHAR(100) NOT NULL,
    email VARCHAR(200),
    mobile_phone_no VARCHAR(30),
    office_phone_no VARCHAR(30),
    profile_file_pk BIGINT,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_person PRIMARY KEY (person_pk),
    CONSTRAINT ck_person_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE person_assignment (
    assignment_pk BIGINT NOT NULL DEFAULT nextval('person_assignment_seq'),
    person_pk BIGINT NOT NULL,
    company_pk BIGINT NOT NULL,
    dept_pk BIGINT,
    employee_no VARCHAR(50),
    employment_type_code VARCHAR(30) NOT NULL,
    position_code VARCHAR(30),
    job_title_code VARCHAR(30),
    assignment_status_code VARCHAR(30) NOT NULL,
    primary_yn CHAR(1) NOT NULL DEFAULT 'N',
    dept_head_yn CHAR(1) NOT NULL DEFAULT 'N',
    start_date DATE NOT NULL,
    end_date DATE,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_person_assignment PRIMARY KEY (assignment_pk),
    CONSTRAINT fk_person_assignment_person FOREIGN KEY (person_pk) REFERENCES person (person_pk),
    CONSTRAINT fk_person_assignment_company FOREIGN KEY (company_pk) REFERENCES company (company_pk),
    CONSTRAINT fk_person_assignment_dept FOREIGN KEY (dept_pk) REFERENCES dept (dept_pk),
    CONSTRAINT ck_person_assignment_primary_yn CHECK (primary_yn IN ('Y', 'N')),
    CONSTRAINT ck_person_assignment_dept_head_yn CHECK (dept_head_yn IN ('Y', 'N')),
    CONSTRAINT ck_person_assignment_date CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ck_person_assignment_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_person_assignment_employee_no
    ON person_assignment (company_pk, employee_no)
    WHERE employee_no IS NOT NULL;

CREATE UNIQUE INDEX uk_person_assignment_primary_yn
    ON person_assignment (person_pk)
    WHERE primary_yn = 'Y' AND end_date IS NULL AND use_yn = 'Y';

CREATE TABLE user_account (
    user_pk BIGINT NOT NULL DEFAULT nextval('user_account_seq'),
    person_pk BIGINT,
    account_type_code VARCHAR(30) NOT NULL,
    login_id VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    password_changed_datetime TIMESTAMP,
    password_expire_datetime TIMESTAMP,
    must_change_password_yn CHAR(1) NOT NULL DEFAULT 'N',
    user_status_code VARCHAR(30) NOT NULL,
    login_fail_count INTEGER NOT NULL DEFAULT 0,
    locked_datetime TIMESTAMP,
    last_login_datetime TIMESTAMP,
    support_language_code VARCHAR(20),
    timezone_id VARCHAR(50),
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_user_account PRIMARY KEY (user_pk),
    CONSTRAINT uk_user_account_login_id UNIQUE (login_id),
    CONSTRAINT ck_user_account_login_id CHECK (login_id = lower(login_id)),
    CONSTRAINT fk_user_account_person FOREIGN KEY (person_pk) REFERENCES person (person_pk),
    CONSTRAINT fk_user_account_support_language FOREIGN KEY (support_language_code) REFERENCES support_language (support_language_code),
    CONSTRAINT ck_user_account_must_change_password_yn CHECK (must_change_password_yn IN ('Y', 'N')),
    CONSTRAINT ck_user_account_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_user_account_person_pk
    ON user_account (person_pk)
    WHERE person_pk IS NOT NULL;

CREATE TABLE user_password_history (
    password_history_pk BIGINT NOT NULL DEFAULT nextval('user_password_history_seq'),
    user_pk BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    change_type_code VARCHAR(30) NOT NULL,
    changed_datetime TIMESTAMP NOT NULL,
    changed_user_pk BIGINT,
    CONSTRAINT pk_user_password_history PRIMARY KEY (password_history_pk),
    CONSTRAINT fk_user_password_history_user FOREIGN KEY (user_pk) REFERENCES user_account (user_pk)
);

CREATE TABLE user_login_history (
    login_history_pk BIGINT NOT NULL DEFAULT nextval('user_login_history_seq'),
    user_pk BIGINT,
    login_id_input VARCHAR(100) NOT NULL,
    login_result_code VARCHAR(30) NOT NULL,
    fail_reason VARCHAR(200),
    login_datetime TIMESTAMP NOT NULL,
    logout_datetime TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    CONSTRAINT pk_user_login_history PRIMARY KEY (login_history_pk),
    CONSTRAINT fk_user_login_history_user FOREIGN KEY (user_pk) REFERENCES user_account (user_pk)
);

CREATE TABLE user_refresh_token (
    refresh_token_pk BIGINT NOT NULL DEFAULT nextval('user_refresh_token_seq'),
    user_pk BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    token_family_id VARCHAR(100) NOT NULL,
    issued_datetime TIMESTAMP NOT NULL,
    expire_datetime TIMESTAMP NOT NULL,
    revoked_datetime TIMESTAMP,
    revoke_reason_code VARCHAR(30),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    CONSTRAINT pk_user_refresh_token PRIMARY KEY (refresh_token_pk),
    CONSTRAINT uk_user_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_refresh_token_user FOREIGN KEY (user_pk) REFERENCES user_account (user_pk)
);

CREATE INDEX ix_company_parent_company_pk ON company (parent_company_pk);
CREATE INDEX ix_company_nation_pk ON company (nation_pk);
CREATE INDEX ix_dept_parent_dept_pk ON dept (parent_dept_pk);
CREATE INDEX ix_dept_history_dept_pk ON dept_history (dept_pk);
CREATE INDEX ix_person_assignment_person_pk ON person_assignment (person_pk);
CREATE INDEX ix_person_assignment_company_pk ON person_assignment (company_pk);
CREATE INDEX ix_person_assignment_dept_pk ON person_assignment (dept_pk);
CREATE INDEX ix_user_account_support_language_code ON user_account (support_language_code);
CREATE INDEX ix_user_password_history_user_pk ON user_password_history (user_pk);
CREATE INDEX ix_user_login_history_user_pk ON user_login_history (user_pk);
CREATE INDEX ix_user_login_history_login_datetime ON user_login_history (login_datetime);
CREATE INDEX ix_user_refresh_token_user_pk ON user_refresh_token (user_pk);
