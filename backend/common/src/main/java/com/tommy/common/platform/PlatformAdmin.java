package com.tommy.common.platform;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tommy.common.global.audit.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "platform_admin", schema = "pmis")
public class PlatformAdmin extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "platform_admin_seq_gen")
	@SequenceGenerator(name = "platform_admin_seq_gen", sequenceName = "platform_admin_seq", schema = "pmis", allocationSize = 1)
	@Column(name = "platform_admin_pk", nullable = false)
	private Long platformAdminPk;

	@Column(name = "login_id", nullable = false, length = 100, updatable = false)
	private String loginId;

	@Column(name = "admin_name", nullable = false, length = 100)
	private String adminName;

	@Column(name = "email", nullable = false, length = 200)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "password_changed_datetime")
	private LocalDateTime passwordChangedDatetime;

	@Column(name = "password_expire_datetime")
	private LocalDateTime passwordExpireDatetime;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "must_change_password_yn", nullable = false, length = 1)
	private String mustChangePasswordYn;

	@Column(name = "admin_status_code", nullable = false, length = 30)
	private String adminStatusCode;

	@Column(name = "login_fail_count", nullable = false)
	private Integer loginFailCount;

	@Column(name = "locked_datetime")
	private LocalDateTime lockedDatetime;

	@Column(name = "last_login_datetime")
	private LocalDateTime lastLoginDatetime;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "use_yn", nullable = false, length = 1)
	private String useYn;

}
