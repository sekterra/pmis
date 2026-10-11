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
@Table(name = "i18n_message_text")
public class I18nMessageText {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "i18n_message_text_seq_gen")
	@SequenceGenerator(name = "i18n_message_text_seq_gen", sequenceName = "i18n_message_text_seq", allocationSize = 1)
	private Long messageTextPk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "message_pk", nullable = false)
	private I18nMessage message;

	@Column(nullable = false, length = 20)
	private String supportLanguageCode;

	@Column(nullable = false, columnDefinition = "text")
	private String messageText;

	@Column(nullable = false, length = 30)
	private String translationStatusCode;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String machineTranslatedYn;

	private Integer versionNo;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static I18nMessageText create(I18nMessage message, String supportLanguageCode, String messageText,
			String translationStatusCode, boolean machineTranslated) {
		I18nMessageText text = new I18nMessageText();
		text.message = message;
		text.supportLanguageCode = supportLanguageCode;
		text.messageText = messageText;
		text.translationStatusCode = translationStatusCode;
		text.machineTranslatedYn = machineTranslated ? "Y" : "N";
		text.versionNo = 1;
		text.useYn = "Y";
		text.createDatetime = LocalDateTime.now();
		return text;
	}

	public boolean apply(String messageText, String translationStatusCode, Boolean machineTranslated) {
		boolean textChanged = !this.messageText.equals(messageText);
		this.messageText = messageText;
		if (translationStatusCode != null && !translationStatusCode.isBlank()) {
			this.translationStatusCode = translationStatusCode;
		}
		if (machineTranslated != null) {
			this.machineTranslatedYn = machineTranslated ? "Y" : "N";
		}
		if (textChanged) {
			this.versionNo = this.versionNo == null ? 1 : this.versionNo + 1;
		}
		this.updateDatetime = LocalDateTime.now();
		return textChanged;
	}

	public boolean machineTranslated() {
		return "Y".equals(machineTranslatedYn);
	}

}
