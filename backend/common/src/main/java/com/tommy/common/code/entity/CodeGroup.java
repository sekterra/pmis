package com.tommy.common.code.entity;

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
@Table(name = "code_group")
public class CodeGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "code_group_seq_gen")
	@SequenceGenerator(name = "code_group_seq_gen", sequenceName = "code_group_seq", allocationSize = 1)
	private Long codeGroupPk;

	@Column(nullable = false, length = 100, updatable = false)
	private String codeGroupCode;

	@Column(length = 50)
	private String solutionCode;

	@Column(nullable = false, length = 200)
	private String codeGroupName;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String hierarchyYn;

	private Integer codeValueLength;

	@Column(columnDefinition = "text")
	private String extAttrSchema;

	private Integer sortOrder;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static CodeGroup create(String codeGroupCode, String solutionCode, String codeGroupName, String description,
			boolean hierarchy, Integer codeValueLength, String extAttrSchema, Integer sortOrder) {
		CodeGroup group = new CodeGroup();
		group.codeGroupCode = codeGroupCode;
		group.solutionCode = solutionCode;
		group.codeGroupName = codeGroupName;
		group.description = description;
		group.hierarchyYn = hierarchy ? "Y" : "N";
		group.codeValueLength = codeValueLength;
		group.extAttrSchema = extAttrSchema;
		group.sortOrder = sortOrder;
		group.useYn = "Y";
		group.createDatetime = LocalDateTime.now();
		return group;
	}

	public void change(String solutionCode, String codeGroupName, String description, boolean hierarchy,
			Integer codeValueLength, String extAttrSchema, Integer sortOrder) {
		this.solutionCode = solutionCode;
		this.codeGroupName = codeGroupName;
		this.description = description;
		this.hierarchyYn = hierarchy ? "Y" : "N";
		this.codeValueLength = codeValueLength;
		this.extAttrSchema = extAttrSchema;
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

	public boolean hierarchical() {
		return "Y".equals(hierarchyYn);
	}

	public boolean inUse() {
		return "Y".equals(useYn);
	}

}
