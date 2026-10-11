-- 공용 스키마용. 스키마명은 적지 않는다.
-- 논리 삭제한 도메인 이름을 같은 테넌트나 다른 테넌트에 다시 등록할 수 있다.

ALTER TABLE tenant_domain DROP CONSTRAINT uk_tenant_domain_domain_name;

CREATE UNIQUE INDEX uk_tenant_domain_domain_name
    ON tenant_domain (domain_name)
    WHERE use_yn = 'Y';
