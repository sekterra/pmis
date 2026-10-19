package com.tommy.common.org.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonRequest(
		@NotBlank @Size(max = 100) String personName,
		@Email @Size(max = 200) String email,
		@Size(max = 30) String mobilePhoneNo,
		@Size(max = 30) String officePhoneNo,
		@Size(max = 1000) String description,
		@Valid List<TextTranslation> translations) {
}
