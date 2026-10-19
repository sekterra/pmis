package com.tommy.common.i18n.entity;

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
@Table(name = "i18n_entity_text")
public class I18nEntityText {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "i18n_entity_text_seq_gen")
	@SequenceGenerator(name = "i18n_entity_text_seq_gen", sequenceName = "i18n_entity_text_seq", allocationSize = 1)
	private Long entityTextPk;

	@Column(nullable = false, length = 100)
	private String targetTableName;

	@Column(nullable = false, length = 100)
	private String targetColumnName;

	@Column(nullable = false, length = 100)
	private String targetKey;

	@Column(nullable = false, length = 20)
	private String supportLanguageCode;

	@Column(nullable = false, columnDefinition = "text")
	private String textValue;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static I18nEntityText create(String targetTableName, String targetColumnName, String targetKey,
			String supportLanguageCode, String textValue, Long userPk) {
		I18nEntityText text = new I18nEntityText();
		text.targetTableName = targetTableName;
		text.targetColumnName = targetColumnName;
		text.targetKey = targetKey;
		text.supportLanguageCode = supportLanguageCode;
		text.textValue = textValue;
		text.useYn = "Y";
		text.createDatetime = LocalDateTime.now();
		text.createUserPk = userPk;
		text.updateDatetime = text.createDatetime;
		text.updateUserPk = userPk;
		return text;
	}

	public void change(String textValue, Long userPk) {
		this.textValue = textValue;
		this.useYn = "Y";
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
