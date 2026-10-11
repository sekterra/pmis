package com.tommy.common.tenant.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tommy.common.global.audit.BaseEntity;
import com.tommy.common.global.audit.YnConverter;
import com.tommy.common.tenant.TenantStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tenant", schema = "pmis")
public class Tenant extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_seq_gen")
	@SequenceGenerator(name = "tenant_seq_gen", sequenceName = "tenant_seq", allocationSize = 1)
	@Column(name = "tenant_pk", nullable = false)
	private Long tenantPk;

	@Column(name = "tenant_code", nullable = false, length = 50, updatable = false)
	private String tenantCode;

	@Column(name = "tenant_name", nullable = false, length = 200)
	private String tenantName;

	@Column(name = "schema_name", nullable = false, length = 63, updatable = false)
	private String schemaName;

	@Enumerated(EnumType.STRING)
	@Column(name = "tenant_status_code", nullable = false, length = 30)
	private TenantStatus tenantStatusCode;

	@Column(name = "default_language_code", nullable = false, length = 20)
	private String defaultLanguageCode;

	@Column(name = "timezone_id", length = 50)
	private String timezoneId;

	@Column(name = "schema_version", length = 50)
	private String schemaVersion;

	@Column(name = "contract_start_date")
	private LocalDate contractStartDate;

	@Column(name = "contract_end_date")
	private LocalDate contractEndDate;

	@Column(name = "activated_datetime")
	private LocalDateTime activatedDatetime;

	@Column(name = "description", length = 1000)
	private String description;

	@Convert(converter = YnConverter.class)
	@Column(name = "use_yn", nullable = false, length = 1)
	private Boolean useYn;

	public static Tenant create(String tenantCode, String tenantName, String schemaName, TenantStatus tenantStatusCode,
			String defaultLanguageCode) {
		Tenant tenant = new Tenant();
		tenant.tenantCode = tenantCode;
		tenant.tenantName = tenantName;
		tenant.schemaName = schemaName;
		tenant.tenantStatusCode = tenantStatusCode;
		tenant.defaultLanguageCode = defaultLanguageCode;
		tenant.useYn = true;
		return tenant;
	}

	public void rename(String tenantName) {
		this.tenantName = tenantName;
	}

}
