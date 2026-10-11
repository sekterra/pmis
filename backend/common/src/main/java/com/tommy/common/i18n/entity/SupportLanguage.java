package com.tommy.common.i18n.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "support_language")
public class SupportLanguage implements Persistable<String> {

	@Id
	@Column(nullable = false, length = 20)
	private String supportLanguageCode;

	@Column(nullable = false, length = 100)
	private String languageName;

	@Column(length = 100)
	private String nativeName;

	@Column(length = 10)
	private String isoLanguageCode;

	@Column(length = 20)
	private String fallbackLanguageCode;

	@Column(nullable = false, length = 3)
	private String textDirectionCode;

	@Column(length = 50)
	private String dateFormat;

	@Column(length = 50)
	private String datetimeFormat;

	@Column(length = 50)
	private String numberFormat;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String defaultYn;

	private Integer sortOrder;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	@Transient
	private boolean newly = true;

	@Override
	public String getId() {
		return supportLanguageCode;
	}

	@Override
	public boolean isNew() {
		return newly;
	}

	@PostLoad
	@PostPersist
	void markPersisted() {
		newly = false;
	}

	public static SupportLanguage create(String code, String languageName, String nativeName, String isoLanguageCode,
			String fallbackLanguageCode, String textDirectionCode, String dateFormat, String datetimeFormat,
			String numberFormat, boolean defaultLanguage, Integer sortOrder) {
		SupportLanguage language = new SupportLanguage();
		language.supportLanguageCode = code;
		language.useYn = "Y";
		language.createDatetime = LocalDateTime.now();
		language.change(languageName, nativeName, isoLanguageCode, fallbackLanguageCode, textDirectionCode, dateFormat,
				datetimeFormat, numberFormat, defaultLanguage, sortOrder);
		return language;
	}

	public void change(String languageName, String nativeName, String isoLanguageCode, String fallbackLanguageCode,
			String textDirectionCode, String dateFormat, String datetimeFormat, String numberFormat,
			boolean defaultLanguage, Integer sortOrder) {
		this.languageName = languageName;
		this.nativeName = nativeName;
		this.isoLanguageCode = isoLanguageCode;
		this.fallbackLanguageCode = blankToNull(fallbackLanguageCode);
		this.textDirectionCode = textDirectionCode;
		this.dateFormat = dateFormat;
		this.datetimeFormat = datetimeFormat;
		this.numberFormat = numberFormat;
		this.defaultYn = defaultLanguage ? "Y" : "N";
		this.sortOrder = sortOrder;
		this.updateDatetime = LocalDateTime.now();
	}

	public void clearDefault() {
		this.defaultYn = "N";
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

	public boolean defaultLanguage() {
		return "Y".equals(defaultYn);
	}

	public boolean inUse() {
		return "Y".equals(useYn);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

}
