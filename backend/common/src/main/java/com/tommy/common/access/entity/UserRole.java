package com.tommy.common.access.entity;

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
@Table(name = "user_role")
public class UserRole {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_role_seq_gen")
	@SequenceGenerator(name = "user_role_seq_gen", sequenceName = "user_role_seq", allocationSize = 1)
	private Long userRolePk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_pk", nullable = false)
	private UserAccount userAccount;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_pk", nullable = false)
	private Role role;

	private LocalDate validFromDate;

	private LocalDate validToDate;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static UserRole grant(UserAccount user, Role role, LocalDate from, LocalDate to, Long actorPk) {
		UserRole link = new UserRole();
		link.userAccount = user;
		link.role = role;
		link.useYn = "Y";
		link.createDatetime = LocalDateTime.now();
		link.createUserPk = actorPk;
		link.apply(from, to, actorPk);
		return link;
	}

	public void apply(LocalDate from, LocalDate to, Long actorPk) {
		this.validFromDate = from;
		this.validToDate = to;
		this.useYn = "Y";
		touch(actorPk);
	}

	public void revoke(Long actorPk) {
		this.useYn = "N";
		touch(actorPk);
	}

	public boolean current(LocalDate today) {
		return "Y".equals(useYn) && (validFromDate == null || !validFromDate.isAfter(today))
				&& (validToDate == null || !validToDate.isBefore(today));
	}

	private void touch(Long actorPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = actorPk;
	}

}
