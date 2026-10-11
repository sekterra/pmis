package com.tommy.common.i18n.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NamespaceRequest(
		@NotBlank @Size(max = 100) @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]*$") String namespaceCode,
		@NotBlank @Size(max = 200) String namespaceName,
		@Size(max = 50) String solutionCode,
		@Size(max = 1000) String description) {
}
