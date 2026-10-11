package com.tommy.common.i18n.entity;

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
@Table(name = "nation")
public class Nation {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nation_seq_gen")
	@SequenceGenerator(name = "nation_seq_gen", sequenceName = "nation_seq", allocationSize = 1)
	private Long nationPk;

	@Column(nullable = false, length = 2, updatable = false)
	private String nationCode;

	@Column(length = 3)
	private String isoAlpha3Code;

	@Column(length = 3)
	private String isoNumericCode;

	@Column(nullable = false, length = 200)
	private String nationName;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "default_language_code", nullable = false)
	private SupportLanguage defaultLanguage;

	@Column(length = 10)
	private String phoneCode;

	@Column(length = 3)
	private String currencyCode;

	@Column(length = 50)
	private String timezoneId;

	private Integer sortOrder;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static Nation create(String nationCode, String isoAlpha3Code, String isoNumericCode, String nationName,
			SupportLanguage defaultLanguage, String phoneCode, String currencyCode, String timezoneId, Integer sortOrder) {
		Nation nation = new Nation();
		nation.nationCode = nationCode;
		nation.useYn = "Y";
		nation.createDatetime = LocalDateTime.now();
		nation.change(isoAlpha3Code, isoNumericCode, nationName, defaultLanguage, phoneCode, currencyCode, timezoneId, sortOrder);
		return nation;
	}

	public void change(String isoAlpha3Code, String isoNumericCode, String nationName, SupportLanguage defaultLanguage,
			String phoneCode, String currencyCode, String timezoneId, Integer sortOrder) {
		this.isoAlpha3Code = isoAlpha3Code;
		this.isoNumericCode = isoNumericCode;
		this.nationName = nationName;
		this.defaultLanguage = defaultLanguage;
		this.phoneCode = phoneCode;
		this.currencyCode = currencyCode;
		this.timezoneId = timezoneId;
		this.sortOrder = sortOrder;
		this.updateDatetime = LocalDateTime.now();
	}

	public void deactivate() {
		this.useYn = "N";
		this.updateDatetime = LocalDateTime.now();
	}

	public void activate() {
		this.useYn = "Y";
		this.updateDatetime = LocalDateTime.now();
	}

	public boolean inUse() {
		return "Y".equals(useYn);
	}

}
