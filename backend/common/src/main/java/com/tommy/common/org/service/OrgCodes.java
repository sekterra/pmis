package com.tommy.common.org.service;

import org.springframework.stereotype.Component;

import com.tommy.common.code.service.CodeQueryService;
import com.tommy.common.org.support.OrgExceptions;

@Component
public class OrgCodes {

	private final CodeQueryService codes;

	public OrgCodes(CodeQueryService codes) {
		this.codes = codes;
	}

	public void require(String groupCode, String codeValue, String field) {
		if (codeValue == null || codeValue.isBlank()) {
			return;
		}
		if (!codes.isCurrentlyUsable(groupCode, codeValue)) {
			throw OrgExceptions.field("ORG.INVALID_CODE_VALUE", "사용할 수 없는 코드 값입니다.", field);
		}
	}

}
