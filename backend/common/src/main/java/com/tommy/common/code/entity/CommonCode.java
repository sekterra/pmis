package com.tommy.common.code.entity;

import java.time.LocalDate;
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
@Table(name = "common_code")
public class CommonCode {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "common_code_seq_gen")
	@SequenceGenerator(name = "common_code_seq_gen", sequenceName = "common_code_seq", allocationSize = 1)
	private Long codePk;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "code_group_pk", nullable = false)
	private CodeGroup codeGroup;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_code_pk")
	private CommonCode parentCode;

	@Column(nullable = false, length = 100)
	private String codeValue;

	@Column(nullable = false, length = 200)
	private String codeName;

	private Integer levelNo;

	@Column(length = 500)
	private String codePath;

	private Integer sortOrder;

	@Column(length = 200)
	private String extAttr1;

	@Column(length = 200)
	private String extAttr2;

	@Column(length = 200)
	private String extAttr3;

	@Column(length = 200)
	private String extAttr4;

	@Column(length = 200)
	private String extAttr5;

	private LocalDate validFromDate;

	private LocalDate validToDate;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static CommonCode create(CodeGroup codeGroup, String codeValue, String codeName) {
		CommonCode code = new CommonCode();
		code.codeGroup = codeGroup;
		code.codeValue = codeValue;
		code.codeName = codeName;
		code.useYn = "Y";
		code.createDatetime = LocalDateTime.now();
		return code;
	}

	public void changeValues(String codeValue, String codeName, Integer sortOrder, String extAttr1, String extAttr2,
			String extAttr3, String extAttr4, String extAttr5, LocalDate validFromDate, LocalDate validToDate) {
		this.codeValue = codeValue;
		this.codeName = codeName;
		this.sortOrder = sortOrder;
		this.extAttr1 = extAttr1;
		this.extAttr2 = extAttr2;
		this.extAttr3 = extAttr3;
		this.extAttr4 = extAttr4;
		this.extAttr5 = extAttr5;
		this.validFromDate = validFromDate;
		this.validToDate = validToDate;
		this.updateDatetime = LocalDateTime.now();
	}

	public void place(CommonCode parent, int levelNo, String codePath) {
		this.parentCode = parent;
		this.levelNo = levelNo;
		this.codePath = codePath;
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
