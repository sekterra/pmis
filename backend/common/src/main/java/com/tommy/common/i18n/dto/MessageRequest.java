package com.tommy.common.i18n.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
		@NotBlank @Size(max = 100) String namespaceCode,
		@NotBlank @Size(max = 200) String messageKey,
		@NotBlank @Size(max = 30) String messageTypeCode,
		@Size(max = 1000) String defaultMessage,
		@Size(max = 500) String paramDescription,
		@Valid List<MessageTextRequest> translations) {
}
