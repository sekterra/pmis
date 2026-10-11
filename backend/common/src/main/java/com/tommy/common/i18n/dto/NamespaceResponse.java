package com.tommy.common.i18n.dto;

public record NamespaceResponse(Long namespacePk, String namespaceCode, String namespaceName, String solutionCode,
		String description, String useYn) {
}
