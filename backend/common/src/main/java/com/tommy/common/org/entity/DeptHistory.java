package com.tommy.common.org.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
@Table(name = "dept_history")
public class DeptHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dept_history_seq_gen")
	@SequenceGenerator(name = "dept_history_seq_gen", sequenceName = "dept_history_seq", allocationSize = 1)
	private Long historyPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dept_pk", nullable = false)
	private Dept dept;

	@Column(nullable = false, length = 30)
	private String changeTypeCode;

	@Column(nullable = false)
	private LocalDate effectiveDate;

	@Column(length = 200)
	private String beforeDeptName;

	@Column(length = 200)
	private String afterDeptName;

	private Long beforeParentDeptPk;

	private Long afterParentDeptPk;

	@Column(length = 500)
	private String changeReason;

	@Column(nullable = false)
	private LocalDateTime changeDatetime;

	private Long changeUserPk;

	public static DeptHistory record(Dept dept, String changeTypeCode, LocalDate effectiveDate, String beforeDeptName,
			String afterDeptName, Long beforeParentDeptPk, Long afterParentDeptPk, String changeReason, Long userPk) {
		DeptHistory history = new DeptHistory();
		history.dept = dept;
		history.changeTypeCode = changeTypeCode;
		history.effectiveDate = effectiveDate;
		history.beforeDeptName = beforeDeptName;
		history.afterDeptName = afterDeptName;
		history.beforeParentDeptPk = beforeParentDeptPk;
		history.afterParentDeptPk = afterParentDeptPk;
		history.changeReason = changeReason;
		history.changeDatetime = LocalDateTime.now();
		history.changeUserPk = userPk;
		return history;
	}

}
