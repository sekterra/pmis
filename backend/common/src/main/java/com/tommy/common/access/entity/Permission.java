package com.tommy.common.access.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tommy.common.menu.entity.Menu;

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
@Table(name = "permission")
public class Permission {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permission_seq_gen")
	@SequenceGenerator(name = "permission_seq_gen", sequenceName = "permission_seq", allocationSize = 1)
	private Long permissionPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_pk", nullable = false, updatable = false)
	private Menu menu;

	@Column(nullable = false, length = 30, updatable = false)
	private String actionCode;

	@Column(nullable = false, length = 150, updatable = false)
	private String permissionCode;

	@Column(nullable = false, length = 200)
	private String permissionName;

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

	public static Permission create(Menu menu, String actionCode, String permissionName, Integer sortOrder, String description,
			Long userPk) {
		Permission permission = new Permission();
		permission.menu = menu;
		permission.actionCode = actionCode;
		permission.permissionCode = menu.getMenuCode() + ":" + actionCode;
		permission.permissionName = permissionName;
		permission.sortOrder = sortOrder;
		permission.description = description;
		permission.useYn = "Y";
		permission.createDatetime = LocalDateTime.now();
		permission.createUserPk = userPk;
		permission.updateDatetime = permission.createDatetime;
		permission.updateUserPk = userPk;
		return permission;
	}

	public void change(String permissionName, Integer sortOrder, String description, Long userPk) {
		this.permissionName = permissionName;
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
