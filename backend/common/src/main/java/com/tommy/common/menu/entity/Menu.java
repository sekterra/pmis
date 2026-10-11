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

}
