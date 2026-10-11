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
@Table(name = "code_group_lang")
public class CodeGroupLang {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "code_group_lang_seq_gen")
	@SequenceGenerator(name = "code_group_lang_seq_gen", sequenceName = "code_group_lang_seq", allocationSize = 1)
	private Long codeGroupLangPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "code_group_pk", nullable = false)
	private CodeGroup codeGroup;

	@Column(nullable = false, length = 20)
	private String supportLanguageCode;

	@Column(nullable = false, length = 200)
	private String codeGroupName;

	@Column(length = 1000)
	private String description;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static CodeGroupLang create(CodeGroup codeGroup, String supportLanguageCode, String codeGroupName,
			String description) {
		CodeGroupLang lang = new CodeGroupLang();
		lang.codeGroup = codeGroup;
		lang.supportLanguageCode = supportLanguageCode;
		lang.codeGroupName = codeGroupName;
		lang.description = description;
		lang.createDatetime = LocalDateTime.now();
		return lang;
	}

	public void change(String codeGroupName, String description) {
		this.codeGroupName = codeGroupName;
		this.description = description;
		this.updateDatetime = LocalDateTime.now();
	}

}
