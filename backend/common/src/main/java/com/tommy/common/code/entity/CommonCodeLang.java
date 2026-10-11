package com.tommy.common.code.entity;

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
@Table(name = "common_code_lang")
public class CommonCodeLang {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "common_code_lang_seq_gen")
	@SequenceGenerator(name = "common_code_lang_seq_gen", sequenceName = "common_code_lang_seq", allocationSize = 1)
	private Long codeLangPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "code_pk", nullable = false)
	private CommonCode commonCode;

	@Column(nullable = false, length = 20)
	private String supportLanguageCode;

	@Column(nullable = false, length = 200)
	private String codeName;

	@Column(length = 100)
	private String shortName;

	@Column(length = 1000)
	private String description;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static CommonCodeLang create(CommonCode commonCode, String supportLanguageCode, String codeName,
			String description) {
		CommonCodeLang lang = new CommonCodeLang();
		lang.commonCode = commonCode;
		lang.supportLanguageCode = supportLanguageCode;
		lang.codeName = codeName;
		lang.description = description;
		lang.createDatetime = LocalDateTime.now();
		return lang;
	}

	public void change(String codeName, String description) {
		this.codeName = codeName;
		this.description = description;
		this.updateDatetime = LocalDateTime.now();
	}

}
