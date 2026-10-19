package com.tommy.common.org.entity;

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
@Table(name = "company")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_seq_gen")
	@SequenceGenerator(name = "company_seq_gen", sequenceName = "company_seq", allocationSize = 1)
	private Long companyPk;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_company_pk")
	private Company parentCompany;

	@Column(nullable = false, length = 50, updatable = false)
	private String companyCode;

	@Column(nullable = false, length = 200)
	private String companyName;

	@Column(length = 100)
	private String companyShortName;

	@Column(nullable = false, length = 30)
	private String companyTypeCode;

	@Column(length = 20)
	private String businessRegistrationNo;

	@Column(length = 20)
	private String corporateRegistrationNo;

	@Column(length = 100)
	private String representativeName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "nation_pk")
	private com.tommy.common.i18n.entity.Nation nation;

	@Column(length = 20)
	private String postalCode;

	@Column(length = 500)
	private String address;

	@Column(length = 500)
	private String addressDetail;

	@Column(length = 30)
	private String phoneNo;

	@Column(length = 30)
	private String faxNo;

	@Column(length = 200)
	private String email;

	@Column(length = 500)
	private String websiteUrl;

	private Integer sortOrder;

	@Column(length = 1000)
	private String description;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 1)
	private String useYn;

	private LocalDateTime createDatetime;

	private Long createUserPk;

	private LocalDateTime updateDatetime;

	private Long updateUserPk;

	public static Company create(String companyCode, String companyName, Long userPk) {
		Company company = new Company();
		company.companyCode = companyCode;
		company.companyName = companyName;
		company.useYn = "Y";
		company.createDatetime = LocalDateTime.now();
		company.createUserPk = userPk;
		company.updateDatetime = company.createDatetime;
		company.updateUserPk = userPk;
		return company;
	}

	public void change(String companyName, String companyShortName, String companyTypeCode, String businessRegistrationNo,
			String corporateRegistrationNo, String representativeName, String postalCode, String address, String addressDetail,
			String phoneNo, String faxNo, String email, String websiteUrl, Integer sortOrder, String description, Long userPk) {
		this.companyName = companyName;
		this.companyShortName = companyShortName;
		this.companyTypeCode = companyTypeCode;
		this.businessRegistrationNo = businessRegistrationNo;
		this.corporateRegistrationNo = corporateRegistrationNo;
		this.representativeName = representativeName;
		this.postalCode = postalCode;
		this.address = address;
		this.addressDetail = addressDetail;
		this.phoneNo = phoneNo;
		this.faxNo = faxNo;
		this.email = email;
		this.websiteUrl = websiteUrl;
		this.sortOrder = sortOrder;
		this.description = description;
		touch(userPk);
	}

	public void assignParent(Company parent) {
		this.parentCompany = parent;
	}

	public void assignNation(com.tommy.common.i18n.entity.Nation nation) {
		this.nation = nation;
	}

	public void deactivate(Long userPk) {
		this.useYn = "N";
		touch(userPk);
	}

	public void activate(Long userPk) {
		this.useYn = "Y";
		touch(userPk);
	}

	public boolean inUse() {
		return "Y".equals(useYn);
	}

	private void touch(Long userPk) {
		this.updateDatetime = LocalDateTime.now();
		this.updateUserPk = userPk;
	}

}
