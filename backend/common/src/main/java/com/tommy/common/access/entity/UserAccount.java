package com.tommy.common.access.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tommy.common.org.entity.Person;

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
@Table(name = "user_account")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_account_seq_gen")
	@SequenceGenerator(name = "user_account_seq_gen", sequenceName = "user_account_seq", allocationSize = 1)
	private Long userPk;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "person_pk")
	private Person person;

	@Column(nullable = false, length = 30)
	private String accountTypeCode;

	@Column(nullable = false, length = 100, updatable = false)
	private String loginId;

	@Column(length = 255)
	private String passwordHash;

	private LocalDateTime passwordChangedDatetime;

	private LocalDateTime passwordExpireDatetime;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String mustChangePasswordYn;

	@Column(nullable = false, length = 30)
	private String userStatusCode;

	@Column(nullable = false)
	private Integer loginFailCount;

	private LocalDateTime lockedDatetime;

	private LocalDateTime lastLoginDatetime;

	@Column(length = 20)
	private String supportLanguageCode;

	@Column(length = 50)
	private String timezoneId;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static UserAccount create(String loginId, String accountTypeCode, String passwordHash, LocalDateTime expire, Long userPk) {
		UserAccount account = new UserAccount();
		account.loginId = loginId;
		account.accountTypeCode = accountTypeCode;
		account.passwordHash = passwordHash;
		account.passwordChangedDatetime = LocalDateTime.now();
		account.passwordExpireDatetime = expire;
		account.mustChangePasswordYn = "Y";
		account.userStatusCode = "ACTIVE";
		account.loginFailCount = 0;
		account.useYn = "Y";
		account.createDatetime = LocalDateTime.now();
		account.createUserPk = userPk;
		account.updateDatetime = account.createDatetime;
		account.updateUserPk = userPk;
		return account;
	}

	public void changeProfile(Person person, String supportLanguageCode, String timezoneId, String description, Long userPk) {
		this.person = person;
		this.supportLanguageCode = supportLanguageCode;
		this.timezoneId = timezoneId;
		this.description = description;
		touch(userPk);
	}

	public void changeStatus(String userStatusCode, Integer loginFailCount, LocalDateTime lockedDatetime, Long userPk) {
		this.userStatusCode = userStatusCode;
		this.loginFailCount = loginFailCount;
		this.lockedDatetime = lockedDatetime;
		touch(userPk);
	}

	public void resetPassword(String passwordHash, LocalDateTime expire, boolean unlock, Long userPk) {
		this.passwordHash = passwordHash;
		this.passwordChangedDatetime = LocalDateTime.now();
		this.passwordExpireDatetime = expire;
		this.mustChangePasswordYn = "Y";
		this.loginFailCount = 0;
		this.lockedDatetime = null;
		if (unlock && "LOCKED".equals(userStatusCode)) {
			this.userStatusCode = "ACTIVE";
		}
		touch(userPk);
	}

	public void withdraw(Long userPk) {
		this.useYn = "N";
		this.userStatusCode = "WITHDRAWN";
		touch(userPk);
	}

	public void restore(Long userPk) {
		this.useYn = "Y";
		this.userStatusCode = "ACTIVE";
		this.loginFailCount = 0;
		this.lockedDatetime = null;
		this.mustChangePasswordYn = "Y";
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
