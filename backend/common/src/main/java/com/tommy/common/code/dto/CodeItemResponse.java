package com.tommy.common.code.dto;

public record CodeItemResponse(
		String codeValue,
		String codeName,
		String parentCodeValue,
		Integer sortOrder,
		String extAttr1,
		String extAttr2,
		String extAttr3,
		String extAttr4,
		String extAttr5) {
}
