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
@Table(name = "i18n_message")
public class I18nMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "i18n_message_seq_gen")
	@SequenceGenerator(name = "i18n_message_seq_gen", sequenceName = "i18n_message_seq", allocationSize = 1)
	private Long messagePk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "namespace_pk", nullable = false)
	private I18nNamespace namespace;

	@Column(nullable = false, length = 200, updatable = false)
	private String messageKey;

	@Column(nullable = false, length = 30)
	private String messageTypeCode;

	@Column(length = 1000)
	private String defaultMessage;

	@Column(length = 500)
	private String paramDescription;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static I18nMessage create(I18nNamespace namespace, String messageKey, String messageTypeCode,
			String defaultMessage, String paramDescription) {
		I18nMessage message = new I18nMessage();
		message.namespace = namespace;
		message.messageKey = messageKey;
		message.useYn = "Y";
		message.createDatetime = LocalDateTime.now();
		message.change(messageTypeCode, defaultMessage, paramDescription);
		return message;
	}

	public void change(String messageTypeCode, String defaultMessage, String paramDescription) {
		this.messageTypeCode = messageTypeCode;
		this.defaultMessage = defaultMessage;
		this.paramDescription = paramDescription;
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
