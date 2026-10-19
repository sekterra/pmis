package com.tommy.common.org.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record CompanyResponse(
		Long companyPk,
		Long parentCompanyPk,
		String parentCompanyCode,
		String companyCode,
		String companyName,
		String companyShortName,
		String companyTypeCode,
		String businessRegistrationNo,
		String corporateRegistrationNo,
		String representativeName,
		Long nationPk,
		String nationCode,
		String postalCode,
		String address,
		String addressDetail,
		String phoneNo,
		String faxNo,
		String email,
		String websiteUrl,
		Integer sortOrder,
		String description,
		String useYn,
		LocalDateTime createDatetime,
		Long createUserPk,
		LocalDateTime updateDatetime,
		Long updateUserPk,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<TextTranslation> translations) {
}
