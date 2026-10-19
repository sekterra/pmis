package com.tommy.common.access.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "role")
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq_gen")
	@SequenceGenerator(name = "role_seq_gen", sequenceName = "role_seq", allocationSize = 1)
	private Long rolePk;

	@Column(nullable = false, length = 50, updatable = false)
	private String roleCode;

	@Column(nullable = false, length = 200)
	private String roleName;

	private Integer sortOrder;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static Role create(String roleCode, String roleName, Integer sortOrder, String description, Long userPk) {
		Role role = new Role();
		role.roleCode = roleCode;
		role.roleName = roleName;
		role.sortOrder = sortOrder;
		role.description = description;
		role.useYn = "Y";
		role.createDatetime = LocalDateTime.now();
		role.createUserPk = userPk;
		role.updateDatetime = role.createDatetime;
		role.updateUserPk = userPk;
		return role;
	}

	public void change(String roleName, Integer sortOrder, String description, Long userPk) {
		this.roleName = roleName;
		this.sortOrder = sortOrder;
		this.description = description;
		touch(userPk);
	}

	public void deactivate(Long userPk) {
		this.useYn = "N";
		touch(userPk);
	}

	public void activate(Long userPk) {
		this.useYn = "Y";
		touch(userPk);
	}

	public boolean inUse() {
		return "Y".equals(useYn);
	}

	private void touch(Long userPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
