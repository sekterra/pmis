package com.tommy.common.org.entity;

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
@Table(name = "person")
public class Person {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_seq_gen")
	@SequenceGenerator(name = "person_seq_gen", sequenceName = "person_seq", allocationSize = 1)
	private Long personPk;

	@Column(nullable = false, length = 100)
	private String personName;

	@Column(length = 200)
	private String email;

	@Column(length = 30)
	private String mobilePhoneNo;

	@Column(length = 30)
	private String officePhoneNo;

	private Long profileFilePk;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static Person create(String personName, Long userPk) {
		Person person = new Person();
		person.personName = personName;
		person.useYn = "Y";
		person.createDatetime = LocalDateTime.now();
		person.createUserPk = userPk;
		person.updateDatetime = person.createDatetime;
		person.updateUserPk = userPk;
		return person;
	}

	public void change(String personName, String email, String mobilePhoneNo, String officePhoneNo, String description, Long userPk) {
		this.personName = personName;
		this.email = email;
		this.mobilePhoneNo = mobilePhoneNo;
		this.officePhoneNo = officePhoneNo;
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
