package com.tommy.common.team.entity;

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

import com.tommy.common.org.entity.Person;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "team_member")
public class TeamMember {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_member_seq_gen")
	@SequenceGenerator(name = "team_member_seq_gen", sequenceName = "team_member_seq", allocationSize = 1)
	private Long teamMemberPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "team_pk", nullable = false)
	private Team team;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "person_pk", nullable = false)
	private Person person;

	@Column(nullable = false, length = 30)
	private String teamRoleCode;

	@Column(nullable = false)
	private LocalDate startDate;

	private LocalDate endDate;

	@Column(length = 500)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static TeamMember create(Team team, Person person, String teamRoleCode, LocalDate startDate, Long userPk) {
		TeamMember member = new TeamMember();
		member.team = team;
		member.person = person;
		member.teamRoleCode = teamRoleCode;
		member.startDate = startDate;
		member.useYn = "Y";
		member.createDatetime = LocalDateTime.now();
		member.createUserPk = userPk;
		member.updateDatetime = member.createDatetime;
		member.updateUserPk = userPk;
		return member;
	}

	public void change(String teamRoleCode, LocalDate startDate, LocalDate endDate, String description, Long userPk) {
		this.teamRoleCode = teamRoleCode;
		this.startDate = startDate;
		this.endDate = endDate;
		this.description = description;
		touch(userPk);
	}

	public void end(LocalDate endDate, Long userPk) {
		this.endDate = endDate;
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

	public boolean current() {
		return inUse() && endDate == null;
	}

	private void touch(Long userPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
