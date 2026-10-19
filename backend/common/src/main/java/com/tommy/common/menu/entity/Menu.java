package com.tommy.common.menu.entity;

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
@Table(name = "menu")
public class Menu {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_seq_gen")
	@SequenceGenerator(name = "menu_seq_gen", sequenceName = "menu_seq", allocationSize = 1)
	private Long menuPk;

	private Long parentMenuPk;

	@Column(nullable = false, length = 50)
	private String solutionCode;

	@Column(nullable = false, length = 100, unique = true)
	private String menuCode;

	@Column(nullable = false, length = 200)
	private String menuName;

	@Column(nullable = false, length = 30)
	private String menuTypeCode;

	@Column(length = 300)
	private String routePath;

	@Column(length = 100)
	private String iconName;

	private Integer levelNo;

	@Column(length = 500)
	private String menuPath;

	private Integer sortOrder;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String visibleYn;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static Menu create(String solutionCode, String menuCode, String menuName, String menuTypeCode, Long parentMenuPk,
			String routePath, String iconName, int levelNo, String menuPath, Integer sortOrder, String visibleYn, String description,
			Long userPk) {
		Menu menu = new Menu();
		menu.solutionCode = solutionCode;
		menu.menuCode = menuCode;
		menu.menuName = menuName;
		menu.menuTypeCode = menuTypeCode;
		menu.parentMenuPk = parentMenuPk;
		menu.routePath = routePath;
		menu.iconName = iconName;
		menu.levelNo = levelNo;
		menu.menuPath = menuPath;
		menu.sortOrder = sortOrder;
		menu.visibleYn = visibleYn;
		menu.description = description;
		menu.useYn = "Y";
		menu.createDatetime = LocalDateTime.now();
		menu.createUserPk = userPk;
		menu.updateDatetime = menu.createDatetime;
		menu.updateUserPk = userPk;
		return menu;
	}

	public void change(String menuName, String routePath, String iconName, Integer sortOrder, String visibleYn, String description,
			Long userPk) {
		this.menuName = menuName;
		this.routePath = routePath;
		this.iconName = iconName;
		this.sortOrder = sortOrder;
		this.visibleYn = visibleYn;
		this.description = description;
		touch(userPk);
	}

	public void place(Long parentMenuPk, int levelNo, String menuPath, Long userPk) {
		this.parentMenuPk = parentMenuPk;
		this.levelNo = levelNo;
		this.menuPath = menuPath;
		touch(userPk);
	}

	public void sort(Integer sortOrder, Long userPk) {
		this.sortOrder = sortOrder;
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
