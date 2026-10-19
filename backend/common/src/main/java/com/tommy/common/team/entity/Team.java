package com.tommy.common.team.entity;

import java.time.LocalDate;
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
@Table(name = "team")
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_seq_gen")
	@SequenceGenerator(name = "team_seq_gen", sequenceName = "team_seq", allocationSize = 1)
	private Long teamPk;

	@Column(nullable = false, length = 50, updatable = false)
	private String teamCode;

	@Column(nullable = false, length = 200)
	private String teamName;

	@Column(nullable = false, length = 30)
	private String teamTypeCode;

	private LocalDate startDate;

	private LocalDate endDate;

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

	public static Team create(String teamCode, String teamName, String teamTypeCode, Long userPk) {
		Team team = new Team();
		team.teamCode = teamCode;
		team.teamName = teamName;
		team.teamTypeCode = teamTypeCode;
		team.useYn = "Y";
		team.createDatetime = LocalDateTime.now();
		team.createUserPk = userPk;
		team.updateDatetime = team.createDatetime;
		team.updateUserPk = userPk;
		return team;
	}

	public void change(String teamName, String teamTypeCode, LocalDate startDate, LocalDate endDate, Integer sortOrder,
			String description, Long userPk) {
		this.teamName = teamName;
		this.teamTypeCode = teamTypeCode;
		this.startDate = startDate;
		this.endDate = endDate;
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
