package com.tommy.common.access.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "role_permission")
public class RolePermission {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_permission_seq_gen")
	@SequenceGenerator(name = "role_permission_seq_gen", sequenceName = "role_permission_seq", allocationSize = 1)
	private Long rolePermissionPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_pk", nullable = false)
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "permission_pk", nullable = false)
	private Permission permission;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static RolePermission grant(Role role, Permission permission, Long userPk) {
		RolePermission link = new RolePermission();
		link.role = role;
		link.permission = permission;
		link.useYn = "Y";
		link.createDatetime = LocalDateTime.now();
		link.createUserPk = userPk;
		link.updateDatetime = link.createDatetime;
		link.updateUserPk = userPk;
		return link;
	}

	public void activate(Long userPk) {
		this.useYn = "Y";
		touch(userPk);
	}

	public void deactivate(Long userPk) {
		this.useYn = "N";
		touch(userPk);
	}

	private void touch(Long userPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
