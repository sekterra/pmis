package com.tommy.common.i18n.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.i18n.BundleEtags;
import com.tommy.common.i18n.dto.BundleView;
import com.tommy.common.i18n.dto.LanguageItem;
import com.tommy.common.i18n.service.BundleService;
import com.tommy.common.i18n.service.SupportLanguageService;

@RestController
@RequestMapping("/api/mdm/v1/i18n")
public class I18nQueryController {

	private final SupportLanguageService languages;
	private final BundleService bundles;

	public I18nQueryController(SupportLanguageService languages, BundleService bundles) {
		this.languages = languages;
		this.bundles = bundles;
	}

	@GetMapping("/languages")
	public ApiResponse<List<LanguageItem>> languages() {
		return ApiResponse.ok(languages.listActive());
	}

	@GetMapping("/bundles/{namespaceCode}")
	public ResponseEntity<ApiResponse<BundleView>> bundle(
			@PathVariable String namespaceCode,
			@RequestParam(required = false) String lang,
			@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
			@RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
		BundleView view = bundles.bundle(namespaceCode, lang, acceptLanguage);
		String etag = BundleEtags.weak(view);
		if (BundleEtags.matches(ifNoneMatch, etag)) {
			return ResponseEntity.status(304).header("ETag", etag).header("Cache-Control", "no-cache").build();
		}
		return ResponseEntity.ok().header("ETag", etag).header("Cache-Control", "no-cache").body(ApiResponse.ok(view));
	}

	@GetMapping("/bundles")
	public ApiResponse<Map<String, BundleView>> bundles(
			@RequestParam String namespaces,
			@RequestParam(required = false) String lang,
			@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
		return ApiResponse.ok(bundles.bundles(namespaces, lang, acceptLanguage));
	}

}
