package com.tommy.common.org.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
		@NotBlank @Size(max = 200) String companyName,
		@Size(max = 100) String companyShortName,
		@NotBlank @Size(max = 30) String companyTypeCode,
		@Size(max = 20) String businessRegistrationNo,
		@Size(max = 20) String corporateRegistrationNo,
		@Size(max = 100) String representativeName,
		@Size(max = 2) String nationCode,
		@Size(max = 50) String parentCompanyCode,
		@Size(max = 20) String postalCode,
		@Size(max = 500) String address,
		@Size(max = 500) String addressDetail,
		@Size(max = 30) String phoneNo,
		@Size(max = 30) String faxNo,
		@Email @Size(max = 200) String email,
		@Size(max = 500) String websiteUrl,
		Integer sortOrder,
		@Size(max = 1000) String description,
		@Valid List<TextTranslation> translations) {
}
