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
@Table(name = "person_assignment")
public class PersonAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_assignment_seq_gen")
	@SequenceGenerator(name = "person_assignment_seq_gen", sequenceName = "person_assignment_seq", allocationSize = 1)
	private Long assignmentPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "person_pk", nullable = false)
	private Person person;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_pk", nullable = false)
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "dept_pk")
	private Dept dept;

	@Column(length = 50)
	private String employeeNo;

	@Column(nullable = false, length = 30)
	private String employmentTypeCode;

	@Column(length = 30)
	private String positionCode;

	@Column(length = 30)
	private String jobTitleCode;

	@Column(nullable = false, length = 30)
	private String assignmentStatusCode;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String primaryYn;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String deptHeadYn;

	@Column(nullable = false)
	private LocalDate startDate;

	private LocalDate endDate;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static PersonAssignment create(Person person, Company company, Long userPk) {
		PersonAssignment assignment = new PersonAssignment();
		assignment.person = person;
		assignment.company = company;
		assignment.useYn = "Y";
		assignment.primaryYn = "N";
		assignment.deptHeadYn = "N";
		assignment.createDatetime = LocalDateTime.now();
		assignment.createUserPk = userPk;
		assignment.updateDatetime = assignment.createDatetime;
		assignment.updateUserPk = userPk;
		return assignment;
	}

	public void change(Company company, Dept dept, String employeeNo, String employmentTypeCode, String positionCode,
			String jobTitleCode, String assignmentStatusCode, String primaryYn, String deptHeadYn, LocalDate startDate,
			LocalDate endDate, String description, Long userPk) {
		this.company = company;
		this.dept = dept;
		this.employeeNo = employeeNo;
		this.employmentTypeCode = employmentTypeCode;
		this.positionCode = positionCode;
		this.jobTitleCode = jobTitleCode;
		this.assignmentStatusCode = assignmentStatusCode;
		this.primaryYn = primaryYn;
		this.deptHeadYn = deptHeadYn;
		this.startDate = startDate;
		this.endDate = endDate;
		this.description = description;
		touch(userPk);
	}

	public void clearPrimary(Long userPk) {
		this.primaryYn = "N";
		touch(userPk);
	}

	public void end(LocalDate endDate, String assignmentStatusCode, Long userPk) {
		this.endDate = endDate;
		this.assignmentStatusCode = assignmentStatusCode;
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

	public boolean current() {
		return "Y".equals(useYn) && endDate == null;
	}

	private void touch(Long userPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
