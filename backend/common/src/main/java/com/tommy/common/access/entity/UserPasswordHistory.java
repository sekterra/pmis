package com.tommy.common.access.entity;

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
@Table(name = "user_password_history")
public class UserPasswordHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_password_history_seq_gen")
	@SequenceGenerator(name = "user_password_history_seq_gen", sequenceName = "user_password_history_seq", allocationSize = 1)
	private Long passwordHistoryPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_pk", nullable = false)
	private UserAccount userAccount;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 30)
	private String changeTypeCode;

	@Column(nullable = false)
	private LocalDateTime changedDatetime;

	private Long changedUserPk;

	public static UserPasswordHistory record(UserAccount user, String passwordHash, String changeTypeCode, Long changedBy) {
		UserPasswordHistory history = new UserPasswordHistory();
		history.userAccount = user;
		history.passwordHash = passwordHash;
		history.changeTypeCode = changeTypeCode;
		history.changedDatetime = LocalDateTime.now();
		history.changedUserPk = changedBy;
		return history;
	}

	@Override
	public String toString() {
		return "UserPasswordHistory[passwordHistoryPk=" + passwordHistoryPk + ", changeTypeCode=" + changeTypeCode + "]";
	}

}
