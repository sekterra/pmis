package com.tommy.common.code.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.code.dto.CodeItemResponse;
import com.tommy.common.code.service.CodeQueryService;
import com.tommy.common.global.response.ApiResponse;

@RestController
@RequestMapping("/api/mdm/v1/codes")
public class CodeQueryController {

	private final CodeQueryService codeQueryService;

	public CodeQueryController(CodeQueryService codeQueryService) {
		this.codeQueryService = codeQueryService;
	}

	@GetMapping
	public ApiResponse<Map<String, List<CodeItemResponse>>> list(
			@RequestParam(required = false) String groups,
			@RequestParam(required = false) String lang,
			@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
		return ApiResponse.ok(codeQueryService.listGroups(groups, lang, acceptLanguage));
	}

	@GetMapping("/{groupCode}")
	public ApiResponse<List<CodeItemResponse>> listGroup(
			@PathVariable String groupCode,
			@RequestParam(required = false) String lang,
			@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
		return ApiResponse.ok(codeQueryService.list(groupCode, lang, acceptLanguage));
	}

}
