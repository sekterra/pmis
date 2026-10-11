-- 테넌트 스키마용. 스키마명은 적지 않는다. Seed 데이터는 넣지 않는다.

CREATE SEQUENCE nation_seq INCREMENT BY 1;
CREATE SEQUENCE i18n_namespace_seq INCREMENT BY 1;
CREATE SEQUENCE i18n_message_seq INCREMENT BY 1;
CREATE SEQUENCE i18n_message_text_seq INCREMENT BY 1;
CREATE SEQUENCE i18n_entity_text_seq INCREMENT BY 1;
CREATE SEQUENCE code_group_seq INCREMENT BY 1;
CREATE SEQUENCE code_group_lang_seq INCREMENT BY 1;
CREATE SEQUENCE common_code_seq INCREMENT BY 1;
CREATE SEQUENCE common_code_lang_seq INCREMENT BY 1;
CREATE SEQUENCE common_code_external_map_seq INCREMENT BY 1;
CREATE SEQUENCE common_code_history_seq INCREMENT BY 1;

CREATE TABLE support_language (
    support_language_code VARCHAR(20) NOT NULL,
    language_name VARCHAR(100) NOT NULL,
    native_name VARCHAR(100),
    iso_language_code VARCHAR(10),
    fallback_language_code VARCHAR(20),
    text_direction_code VARCHAR(3) NOT NULL DEFAULT 'LTR',
    date_format VARCHAR(50),
    datetime_format VARCHAR(50),
    number_format VARCHAR(50),
    default_yn CHAR(1) NOT NULL DEFAULT 'N',
    sort_order INTEGER,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_support_language PRIMARY KEY (support_language_code),
    CONSTRAINT fk_support_language_support_language FOREIGN KEY (fallback_language_code) REFERENCES support_language (support_language_code),
    CONSTRAINT ck_support_language_text_direction_code CHECK (text_direction_code IN ('LTR', 'RTL')),
    CONSTRAINT ck_support_language_default_yn CHECK (default_yn IN ('Y', 'N')),
    CONSTRAINT ck_support_language_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_support_language_default_yn
    ON support_language ((1))
    WHERE default_yn = 'Y' AND use_yn = 'Y';

CREATE TABLE nation (
    nation_pk BIGINT NOT NULL DEFAULT nextval('nation_seq'),
    nation_code VARCHAR(2) NOT NULL,
    iso_alpha3_code VARCHAR(3),
    iso_numeric_code VARCHAR(3),
    nation_name VARCHAR(200) NOT NULL,
    default_language_code VARCHAR(20) NOT NULL,
    phone_code VARCHAR(10),
    currency_code VARCHAR(3),
    timezone_id VARCHAR(50),
    sort_order INTEGER,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_nation PRIMARY KEY (nation_pk),
    CONSTRAINT uk_nation_nation_code UNIQUE (nation_code),
    CONSTRAINT ck_nation_nation_code CHECK (nation_code ~ '^[A-Z]{2}$'),
    CONSTRAINT fk_nation_support_language FOREIGN KEY (default_language_code) REFERENCES support_language (support_language_code),
    CONSTRAINT ck_nation_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE i18n_namespace (
    namespace_pk BIGINT NOT NULL DEFAULT nextval('i18n_namespace_seq'),
    namespace_code VARCHAR(100) NOT NULL,
    namespace_name VARCHAR(200) NOT NULL,
    solution_code VARCHAR(50),
    description VARCHAR(1000),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_i18n_namespace PRIMARY KEY (namespace_pk),
    CONSTRAINT uk_i18n_namespace_namespace_code UNIQUE (namespace_code),
    CONSTRAINT ck_i18n_namespace_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE i18n_message (
    message_pk BIGINT NOT NULL DEFAULT nextval('i18n_message_seq'),
    namespace_pk BIGINT NOT NULL,
    message_key VARCHAR(200) NOT NULL,
    message_type_code VARCHAR(30) NOT NULL,
    default_message VARCHAR(1000),
    param_description VARCHAR(500),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_i18n_message PRIMARY KEY (message_pk),
    CONSTRAINT uk_i18n_message_namespace_pk_message_key UNIQUE (namespace_pk, message_key),
    CONSTRAINT fk_i18n_message_i18n_namespace FOREIGN KEY (namespace_pk) REFERENCES i18n_namespace (namespace_pk),
    CONSTRAINT ck_i18n_message_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE i18n_message_text (
    message_text_pk BIGINT NOT NULL DEFAULT nextval('i18n_message_text_seq'),
    message_pk BIGINT NOT NULL,
    support_language_code VARCHAR(20) NOT NULL,
    message_text TEXT NOT NULL,
    translation_status_code VARCHAR(30) NOT NULL,
    machine_translated_yn CHAR(1) NOT NULL DEFAULT 'N',
    version_no INTEGER,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_i18n_message_text PRIMARY KEY (message_text_pk),
    CONSTRAINT uk_i18n_message_text_message_pk_support_language_code UNIQUE (message_pk, support_language_code),
    CONSTRAINT fk_i18n_message_text_i18n_message FOREIGN KEY (message_pk) REFERENCES i18n_message (message_pk),
    CONSTRAINT fk_i18n_message_text_support_language FOREIGN KEY (support_language_code) REFERENCES support_language (support_language_code),
    CONSTRAINT ck_i18n_message_text_machine_translated_yn CHECK (machine_translated_yn IN ('Y', 'N')),
    CONSTRAINT ck_i18n_message_text_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE i18n_entity_text (
    entity_text_pk BIGINT NOT NULL DEFAULT nextval('i18n_entity_text_seq'),
    target_table_name VARCHAR(100) NOT NULL,
    target_column_name VARCHAR(100) NOT NULL,
    target_key VARCHAR(100) NOT NULL,
    support_language_code VARCHAR(20) NOT NULL,
    text_value TEXT NOT NULL,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_i18n_entity_text PRIMARY KEY (entity_text_pk),
    CONSTRAINT uk_i18n_entity_text_target UNIQUE (target_table_name, target_column_name, target_key, support_language_code),
    CONSTRAINT fk_i18n_entity_text_support_language FOREIGN KEY (support_language_code) REFERENCES support_language (support_language_code),
    CONSTRAINT ck_i18n_entity_text_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE code_group (
    code_group_pk BIGINT NOT NULL DEFAULT nextval('code_group_seq'),
    code_group_code VARCHAR(100) NOT NULL,
    solution_code VARCHAR(50),
    code_group_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    hierarchy_yn CHAR(1) NOT NULL DEFAULT 'N',
    code_value_length INTEGER,
    ext_attr_schema TEXT,
    sort_order INTEGER,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_code_group PRIMARY KEY (code_group_pk),
    CONSTRAINT uk_code_group_code_group_code UNIQUE (code_group_code),
    CONSTRAINT ck_code_group_code_group_code CHECK (code_group_code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT ck_code_group_hierarchy_yn CHECK (hierarchy_yn IN ('Y', 'N')),
    CONSTRAINT ck_code_group_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE code_group_lang (
    code_group_lang_pk BIGINT NOT NULL DEFAULT nextval('code_group_lang_seq'),
    code_group_pk BIGINT NOT NULL,
    support_language_code VARCHAR(20) NOT NULL,
    code_group_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_code_group_lang PRIMARY KEY (code_group_lang_pk),
    CONSTRAINT uk_code_group_lang_code_group_pk_support_language_code UNIQUE (code_group_pk, support_language_code),
    CONSTRAINT fk_code_group_lang_code_group FOREIGN KEY (code_group_pk) REFERENCES code_group (code_group_pk),
    CONSTRAINT fk_code_group_lang_support_language FOREIGN KEY (support_language_code) REFERENCES support_language (support_language_code)
);

CREATE TABLE common_code (
    code_pk BIGINT NOT NULL DEFAULT nextval('common_code_seq'),
    code_group_pk BIGINT NOT NULL,
    parent_code_pk BIGINT,
    code_value VARCHAR(100) NOT NULL,
    code_name VARCHAR(200) NOT NULL,
    level_no INTEGER,
    code_path VARCHAR(500),
    sort_order INTEGER,
    ext_attr1 VARCHAR(200),
    ext_attr2 VARCHAR(200),
    ext_attr3 VARCHAR(200),
    ext_attr4 VARCHAR(200),
    ext_attr5 VARCHAR(200),
    valid_from_date DATE,
    valid_to_date DATE,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_common_code PRIMARY KEY (code_pk),
    CONSTRAINT uk_common_code_code_group_pk_code_value UNIQUE (code_group_pk, code_value),
    CONSTRAINT fk_common_code_code_group FOREIGN KEY (code_group_pk) REFERENCES code_group (code_group_pk),
    CONSTRAINT fk_common_code_common_code FOREIGN KEY (parent_code_pk) REFERENCES common_code (code_pk),
    CONSTRAINT ck_common_code_use_yn CHECK (use_yn IN ('Y', 'N')),
    CONSTRAINT ck_common_code_valid_date CHECK (valid_from_date IS NULL OR valid_to_date IS NULL OR valid_from_date <= valid_to_date)
);

CREATE TABLE common_code_lang (
    code_lang_pk BIGINT NOT NULL DEFAULT nextval('common_code_lang_seq'),
    code_pk BIGINT NOT NULL,
    support_language_code VARCHAR(20) NOT NULL,
    code_name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100),
    description VARCHAR(1000),
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_common_code_lang PRIMARY KEY (code_lang_pk),
    CONSTRAINT uk_common_code_lang_code_pk_support_language_code UNIQUE (code_pk, support_language_code),
    CONSTRAINT fk_common_code_lang_common_code FOREIGN KEY (code_pk) REFERENCES common_code (code_pk),
    CONSTRAINT fk_common_code_lang_support_language FOREIGN KEY (support_language_code) REFERENCES support_language (support_language_code)
);

CREATE TABLE common_code_external_map (
    map_pk BIGINT NOT NULL DEFAULT nextval('common_code_external_map_seq'),
    code_pk BIGINT NOT NULL,
    external_system_code VARCHAR(50) NOT NULL,
    external_code VARCHAR(100) NOT NULL,
    external_code_name VARCHAR(200),
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    create_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_user_pk BIGINT,
    update_datetime TIMESTAMP,
    update_user_pk BIGINT,
    CONSTRAINT pk_common_code_external_map PRIMARY KEY (map_pk),
    CONSTRAINT uk_common_code_external_map_code_pk_external_system_code UNIQUE (code_pk, external_system_code),
    CONSTRAINT fk_common_code_external_map_common_code FOREIGN KEY (code_pk) REFERENCES common_code (code_pk),
    CONSTRAINT ck_common_code_external_map_use_yn CHECK (use_yn IN ('Y', 'N'))
);

CREATE TABLE common_code_history (
    history_pk BIGINT NOT NULL DEFAULT nextval('common_code_history_seq'),
    code_pk BIGINT NOT NULL,
    code_group_code VARCHAR(100) NOT NULL,
    code_value VARCHAR(100) NOT NULL,
    change_type_code VARCHAR(1) NOT NULL,
    before_data TEXT,
    after_data TEXT,
    change_reason VARCHAR(500),
    change_datetime TIMESTAMP NOT NULL,
    change_user_pk BIGINT,
    CONSTRAINT pk_common_code_history PRIMARY KEY (history_pk),
    CONSTRAINT ck_common_code_history_change_type_code CHECK (change_type_code IN ('C', 'U', 'D'))
);

ALTER SEQUENCE nation_seq OWNED BY nation.nation_pk;
ALTER SEQUENCE i18n_namespace_seq OWNED BY i18n_namespace.namespace_pk;
ALTER SEQUENCE i18n_message_seq OWNED BY i18n_message.message_pk;
ALTER SEQUENCE i18n_message_text_seq OWNED BY i18n_message_text.message_text_pk;
ALTER SEQUENCE i18n_entity_text_seq OWNED BY i18n_entity_text.entity_text_pk;
ALTER SEQUENCE code_group_seq OWNED BY code_group.code_group_pk;
ALTER SEQUENCE code_group_lang_seq OWNED BY code_group_lang.code_group_lang_pk;
ALTER SEQUENCE common_code_seq OWNED BY common_code.code_pk;
ALTER SEQUENCE common_code_lang_seq OWNED BY common_code_lang.code_lang_pk;
ALTER SEQUENCE common_code_external_map_seq OWNED BY common_code_external_map.map_pk;
ALTER SEQUENCE common_code_history_seq OWNED BY common_code_history.history_pk;

CREATE INDEX ix_support_language_fallback_language_code ON support_language (fallback_language_code);
CREATE INDEX ix_nation_default_language_code ON nation (default_language_code);
CREATE INDEX ix_i18n_message_namespace_pk ON i18n_message (namespace_pk);
CREATE INDEX ix_i18n_message_text_support_language_code ON i18n_message_text (support_language_code);
CREATE INDEX ix_i18n_entity_text_support_language_code ON i18n_entity_text (support_language_code);
CREATE INDEX ix_code_group_lang_support_language_code ON code_group_lang (support_language_code);
CREATE INDEX ix_common_code_parent_code_pk ON common_code (parent_code_pk);
CREATE INDEX ix_common_code_lang_support_language_code ON common_code_lang (support_language_code);
CREATE INDEX ix_common_code_history_code_pk ON common_code_history (code_pk);
