package com.tommy.common.code.dto;

import java.util.List;

public record CodeGroupResponse(
		Long codeGroupPk,
		String codeGroupCode,
		String solutionCode,
		String codeGroupName,
		String description,
		boolean hierarchy,
		Integer codeValueLength,
		String extAttrSchema,
		Integer sortOrder,
		String useYn,
		List<TranslationResponse> translations) {
}
