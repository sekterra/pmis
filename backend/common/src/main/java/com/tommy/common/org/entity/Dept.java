package com.tommy.common.org.entity;

import java.time.LocalDate;
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
@Table(name = "dept")
public class Dept {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dept_seq_gen")
	@SequenceGenerator(name = "dept_seq_gen", sequenceName = "dept_seq", allocationSize = 1)
	private Long deptPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_pk", nullable = false, updatable = false)
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_dept_pk")
	private Dept parentDept;

	@Column(nullable = false, length = 50, updatable = false)
	private String deptCode;

	@Column(nullable = false, length = 200)
	private String deptName;

	@Column(length = 30)
	private String deptTypeCode;

	private Integer levelNo;

	@Column(length = 500)
	private String deptPath;

	private Integer sortOrder;

	@Column(nullable = false)
	private LocalDate validFromDate;

	private LocalDate validToDate;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static Dept create(Company company, String deptCode, String deptName, Long userPk) {
		Dept dept = new Dept();
		dept.company = company;
		dept.deptCode = deptCode;
		dept.deptName = deptName;
		dept.useYn = "Y";
		dept.createDatetime = LocalDateTime.now();
		dept.createUserPk = userPk;
		dept.updateDatetime = dept.createDatetime;
		dept.updateUserPk = userPk;
		return dept;
	}

	public void change(String deptName, String deptTypeCode, Integer sortOrder, LocalDate validFromDate, LocalDate validToDate,
			String description, Long userPk) {
		this.deptName = deptName;
		this.deptTypeCode = deptTypeCode;
		this.sortOrder = sortOrder;
		this.validFromDate = validFromDate;
		this.validToDate = validToDate;
		this.description = description;
		touch(userPk);
	}

	public void assignParent(Dept parent) {
		this.parentDept = parent;
	}

	public void locate(int levelNo, String deptPath) {
		this.levelNo = levelNo;
		this.deptPath = deptPath;
	}

	public void close(LocalDate effectiveDate, Long userPk) {
		this.validToDate = effectiveDate;
		this.useYn = "N";
		touch(userPk);
	}

	public void reopen(Long userPk) {
		this.validToDate = null;
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
