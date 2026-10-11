-- 테넌트 스키마용. 스키마명은 적지 않는다. Seed 데이터는 넣지 않는다.
-- READ 이외 권한에 같은 메뉴의 READ 가 필요한 규칙은 DB 트리거로 두지 않는다.

CREATE SEQUENCE file_storage_seq INCREMENT BY 1;
CREATE SEQUENCE file_seq INCREMENT BY 1;
CREATE SEQUENCE file_attachment_seq INCREMENT BY 1;
CREATE SEQUENCE team_seq INCREMENT BY 1;
CREATE SEQUENCE team_member_seq INCREMENT BY 1;
CREATE SEQUENCE role_seq INCREMENT BY 1;
CREATE SEQUENCE user_role_seq INCREMENT BY 1;
CREATE SEQUENCE menu_seq INCREMENT BY 1;
CREATE SEQUENCE permission_seq INCREMENT BY 1;
CREATE SEQUENCE role_permission_seq INCREMENT BY 1;

CREATE TABLE file_storage (
    storage_pk BIGINT NOT NULL DEFAULT nextval('file_storage_seq'),
    storage_code VARCHAR(50) NOT NULL,
    storage_name VARCHAR(200) NOT NULL,
    storage_type_code VARCHAR(30) NOT NULL,
    base_path VARCHAR(500) NOT NULL,
    connection_config TEXT,
    secret_ref VARCHAR(200),
    default_yn CHAR(1) NOT NULL DEFAULT 'N',
    max_file_size_mb INTEGER,
    allowed_extensions VARCHAR(500),
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_file_storage PRIMARY KEY (storage_pk),
    CONSTRAINT uk_file_storage_storage_code UNIQUE (storage_code),
    CONSTRAINT ck_file_storage_default_yn CHECK (default_yn IN ('Y', 'N')),
    CONSTRAINT ck_file_storage_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_file_storage_default_yn
    ON file_storage ((1))
    WHERE default_yn = 'Y' AND use_yn = 'Y';

CREATE TABLE file (
    file_pk BIGINT NOT NULL DEFAULT nextval('file_seq'),
    storage_pk BIGINT NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    original_file_name VARCHAR(300) NOT NULL,
    file_extension VARCHAR(20),
    mime_type VARCHAR(150),
    file_size BIGINT NOT NULL,
    checksum_sha256 CHAR(64),
    file_status_code VARCHAR(30) NOT NULL,
    upload_user_pk BIGINT,
    upload_datetime TIMESTAMP NOT NULL,
    delete_scheduled_datetime TIMESTAMP,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_file PRIMARY KEY (file_pk),
    CONSTRAINT uk_file_storage_pk_storage_key UNIQUE (storage_pk, storage_key),
    CONSTRAINT fk_file_file_storage FOREIGN KEY (storage_pk) REFERENCES file_storage (storage_pk),
    CONSTRAINT ck_file_file_size CHECK (file_size >= 0),
    CONSTRAINT ck_file_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE file_attachment (
    attachment_pk BIGINT NOT NULL DEFAULT nextval('file_attachment_seq'),
    file_pk BIGINT NOT NULL,
    target_table_name VARCHAR(100) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    attachment_category_code VARCHAR(30),
    sort_order INTEGER,
    description VARCHAR(500),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_file_attachment PRIMARY KEY (attachment_pk),
    CONSTRAINT uk_file_attachment_target UNIQUE (target_table_name, target_key, file_pk),
    CONSTRAINT fk_file_attachment_file FOREIGN KEY (file_pk) REFERENCES file (file_pk),
    CONSTRAINT ck_file_attachment_use_yn CHECK (use_yn IN ('Y', 'N'))
);

ALTER TABLE person
    ADD CONSTRAINT fk_person_file FOREIGN KEY (profile_file_pk) REFERENCES file (file_pk);

CREATE TABLE team (
    team_pk BIGINT NOT NULL DEFAULT nextval('team_seq'),
    team_code VARCHAR(50) NOT NULL,
    team_name VARCHAR(200) NOT NULL,
    team_type_code VARCHAR(30) NOT NULL,
    start_date DATE,
    end_date DATE,
    sort_order INTEGER,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_team PRIMARY KEY (team_pk),
    CONSTRAINT uk_team_team_code UNIQUE (team_code),
    CONSTRAINT ck_team_date CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ck_team_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE team_member (
    team_member_pk BIGINT NOT NULL DEFAULT nextval('team_member_seq'),
    team_pk BIGINT NOT NULL,
    person_pk BIGINT NOT NULL,
    team_role_code VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    description VARCHAR(500),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_team_member PRIMARY KEY (team_member_pk),
    CONSTRAINT fk_team_member_team FOREIGN KEY (team_pk) REFERENCES team (team_pk),
    CONSTRAINT fk_team_member_person FOREIGN KEY (person_pk) REFERENCES person (person_pk),
    CONSTRAINT ck_team_member_date CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ck_team_member_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_team_member_team_pk_person_pk
    ON team_member (team_pk, person_pk)
    WHERE end_date IS NULL AND use_yn = 'Y';

CREATE TABLE role (
    role_pk BIGINT NOT NULL DEFAULT nextval('role_seq'),
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(200) NOT NULL,
    sort_order INTEGER,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_role PRIMARY KEY (role_pk),
    CONSTRAINT uk_role_role_code UNIQUE (role_code),
    CONSTRAINT ck_role_role_code CHECK (role_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_role_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE user_role (
    user_role_pk BIGINT NOT NULL DEFAULT nextval('user_role_seq'),
    user_pk BIGINT NOT NULL,
    role_pk BIGINT NOT NULL,
    valid_from_date DATE,
    valid_to_date DATE,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_user_role PRIMARY KEY (user_role_pk),
    CONSTRAINT uk_user_role_user_pk_role_pk UNIQUE (user_pk, role_pk),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_pk) REFERENCES user_account (user_pk),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_pk) REFERENCES role (role_pk),
    CONSTRAINT ck_user_role_valid_date CHECK (valid_from_date IS NULL OR valid_to_date IS NULL OR valid_from_date <= valid_to_date),
    CONSTRAINT ck_user_role_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE menu (
    menu_pk BIGINT NOT NULL DEFAULT nextval('menu_seq'),
    parent_menu_pk BIGINT,
    solution_code VARCHAR(50) NOT NULL,
    menu_code VARCHAR(100) NOT NULL,
    menu_name VARCHAR(200) NOT NULL,
    menu_type_code VARCHAR(30) NOT NULL,
    route_path VARCHAR(300),
    icon_name VARCHAR(100),
    level_no INTEGER,
    menu_path VARCHAR(500),
    sort_order INTEGER,
    visible_yn CHAR(1) NOT NULL DEFAULT 'Y',
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_menu PRIMARY KEY (menu_pk),
    CONSTRAINT uk_menu_menu_code UNIQUE (menu_code),
    CONSTRAINT fk_menu_menu FOREIGN KEY (parent_menu_pk) REFERENCES menu (menu_pk),
    CONSTRAINT ck_menu_visible_yn CHECK (visible_yn IN ('Y', 'N')),
    CONSTRAINT ck_menu_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE permission (
    permission_pk BIGINT NOT NULL DEFAULT nextval('permission_seq'),
    menu_pk BIGINT NOT NULL,
    action_code VARCHAR(30) NOT NULL,
    permission_code VARCHAR(150) NOT NULL,
    permission_name VARCHAR(200) NOT NULL,
    sort_order INTEGER,
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_permission PRIMARY KEY (permission_pk),
    CONSTRAINT uk_permission_menu_pk_action_code UNIQUE (menu_pk, action_code),
    CONSTRAINT uk_permission_permission_code UNIQUE (permission_code),
    CONSTRAINT fk_permission_menu FOREIGN KEY (menu_pk) REFERENCES menu (menu_pk),
    CONSTRAINT ck_permission_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE role_permission (
    role_permission_pk BIGINT NOT NULL DEFAULT nextval('role_permission_seq'),
    role_pk BIGINT NOT NULL,
    permission_pk BIGINT NOT NULL,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_role_permission PRIMARY KEY (role_permission_pk),
    CONSTRAINT uk_role_permission_role_pk_permission_pk UNIQUE (role_pk, permission_pk),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_pk) REFERENCES role (role_pk),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_pk) REFERENCES permission (permission_pk),
    CONSTRAINT ck_role_permission_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE INDEX ix_file_attachment_file_pk ON file_attachment (file_pk);
CREATE INDEX ix_team_member_team_pk ON team_member (team_pk);
CREATE INDEX ix_team_member_person_pk ON team_member (person_pk);
CREATE INDEX ix_user_role_role_pk ON user_role (role_pk);
CREATE INDEX ix_role_permission_permission_pk ON role_permission (permission_pk);
CREATE INDEX ix_menu_parent_menu_pk ON menu (parent_menu_pk);
