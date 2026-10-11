package com.tommy.common.code.dto;

import java.time.LocalDate;
import java.util.List;

public record CommonCodeResponse(
		Long codePk,
		String groupCode,
		String codeValue,
		String codeName,
		String parentCodeValue,
		Integer levelNo,
		String codePath,
		Integer sortOrder,
		String extAttr1,
		String extAttr2,
		String extAttr3,
		String extAttr4,
		String extAttr5,
		LocalDate validFromDate,
		LocalDate validToDate,
		String useYn,
		List<TranslationResponse> translations) {
}
