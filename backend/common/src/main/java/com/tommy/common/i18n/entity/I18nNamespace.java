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
@Table(name = "i18n_namespace")
public class I18nNamespace {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "i18n_namespace_seq_gen")
	@SequenceGenerator(name = "i18n_namespace_seq_gen", sequenceName = "i18n_namespace_seq", allocationSize = 1)
	private Long namespacePk;

	@Column(nullable = false, length = 100, updatable = false)
	private String namespaceCode;

	@Column(nullable = false, length = 200)
	private String namespaceName;

	@Column(length = 50)
	private String solutionCode;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static I18nNamespace create(String namespaceCode, String namespaceName, String solutionCode, String description) {
		I18nNamespace namespace = new I18nNamespace();
		namespace.namespaceCode = namespaceCode;
		namespace.useYn = "Y";
		namespace.createDatetime = LocalDateTime.now();
		namespace.change(namespaceName, solutionCode, description);
		return namespace;
	}

	public void change(String namespaceName, String solutionCode, String description) {
		this.namespaceName = namespaceName;
		this.solutionCode = solutionCode;
		this.description = description;
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
