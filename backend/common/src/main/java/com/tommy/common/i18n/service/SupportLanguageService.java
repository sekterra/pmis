package com.tommy.common.i18n.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.code.service.CodeLanguageResolver;
import com.tommy.common.i18n.I18nExceptions;
import com.tommy.common.i18n.SupportLanguageRules;
import com.tommy.common.i18n.dto.LanguageItem;
import com.tommy.common.i18n.dto.SupportLanguageRequest;
import com.tommy.common.i18n.dto.SupportLanguageResponse;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.i18n.repository.SupportLanguageRepository;

@Service
@Transactional(readOnly = true)
public class SupportLanguageService {

	private final SupportLanguageRepository repository;
	private final CodeLanguageResolver languages;
	private final BundleService bundles;

	public SupportLanguageService(SupportLanguageRepository repository, CodeLanguageResolver languages, BundleService bundles) {
		this.repository = repository;
		this.languages = languages;
		this.bundles = bundles;
	}

	public List<LanguageItem> listActive() {
		return repository.findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc("Y").stream()
				.map(this::item)
				.toList();
	}

	public List<SupportLanguageResponse> list(String useYn) {
		List<SupportLanguage> rows = useYn == null || useYn.isBlank()
				? repository.findAllByOrderBySortOrderAscSupportLanguageCodeAsc()
				: repository.findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc(useYn);
		return rows.stream()
				.sorted(Comparator.comparing(SupportLanguage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(SupportLanguage::getSupportLanguageCode))
				.map(this::response)
				.toList();
	}

	public SupportLanguageResponse get(String code) {
		return response(require(code));
	}

	@Transactional
	public SupportLanguageResponse create(SupportLanguageRequest request) {
		if (repository.existsById(request.supportLanguageCode())) {
			throw I18nExceptions.duplicate();
		}
		requireFallback(request.supportLanguageCode(), request.fallbackLanguageCode());
		applyDefault(request.supportLanguageCode(), Boolean.TRUE.equals(request.defaultLanguage()));
		SupportLanguage language = repository.save(SupportLanguage.create(
				request.supportLanguageCode(),
				request.languageName(),
				request.nativeName(),
				request.isoLanguageCode(),
				request.fallbackLanguageCode(),
				request.textDirectionCode(),
				request.dateFormat(),
				request.datetimeFormat(),
				request.numberFormat(),
				Boolean.TRUE.equals(request.defaultLanguage()),
				request.sortOrder()));
		flush();
		evict();
		return response(language);
	}

	@Transactional
	public SupportLanguageResponse update(String code, SupportLanguageRequest request) {
		if (!code.equals(request.supportLanguageCode())) {
			throw I18nExceptions.immutable("언어 코드는 바꿀 수 없습니다.");
		}
		SupportLanguage language = require(code);
		requireFallback(code, request.fallbackLanguageCode());
		applyDefault(code, Boolean.TRUE.equals(request.defaultLanguage()));
		language.change(request.languageName(), request.nativeName(), request.isoLanguageCode(),
				request.fallbackLanguageCode(), request.textDirectionCode(), request.dateFormat(),
				request.datetimeFormat(), request.numberFormat(), Boolean.TRUE.equals(request.defaultLanguage()),
				request.sortOrder());
		flush();
		evict();
		return response(language);
	}

	@Transactional
	public void deactivate(String code) {
		SupportLanguage language = require(code);
		boolean used = repository.existsByFallbackLanguageCodeAndUseYn(code, "Y");
		if (SupportLanguageRules.blocksDeactivate(language.defaultLanguage() && language.inUse(), used)) {
			throw I18nExceptions.languageInUse();
		}
		language.deactivate();
		evict();
	}

	@Transactional
	public SupportLanguageResponse restore(String code) {
		SupportLanguage language = require(code);
		if (language.defaultLanguage() && !repository.findByDefaultYnAndUseYn("Y", "Y").isEmpty()) {
			language.clearDefault();
		}
		language.activate();
		flush();
		evict();
		return response(language);
	}

	private void applyDefault(String code, boolean makeDefault) {
		List<String> current = repository.findByDefaultYnAndUseYn("Y", "Y").stream()
				.map(SupportLanguage::getSupportLanguageCode)
				.toList();
		if (!makeDefault && current.isEmpty()) {
			throw I18nExceptions.defaultRequired();
		}
		for (String replaced : SupportLanguageRules.replacedDefaults(code, makeDefault, current)) {
			repository.findById(replaced).ifPresent(SupportLanguage::clearDefault);
		}
		repository.flush();
	}

	private void requireFallback(String code, String fallback) {
		Map<String, String> graph = repository.findAll().stream()
				.filter(language -> language.getFallbackLanguageCode() != null)
				.collect(Collectors.toMap(SupportLanguage::getSupportLanguageCode, SupportLanguage::getFallbackLanguageCode));
		if (fallback != null && !fallback.isBlank()) {
			SupportLanguage target = repository.findById(fallback).filter(SupportLanguage::inUse)
					.orElseThrow(I18nExceptions::languageNotFound);
			if (target.getSupportLanguageCode().equals(code)) {
				SupportLanguageRules.requireNoCycle(code, fallback, graph);
				return;
			}
		}
		SupportLanguageRules.requireNoCycle(code, fallback, graph);
	}

	private SupportLanguage require(String code) {
		return repository.findById(code).orElseThrow(I18nExceptions::languageMissing);
	}

	private void flush() {
		try {
			repository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw I18nExceptions.duplicate();
		}
	}

	private void evict() {
		languages.evict();
		bundles.evictTenant();
	}

	private LanguageItem item(SupportLanguage language) {
		return new LanguageItem(language.getSupportLanguageCode(), language.getLanguageName(), language.getNativeName(),
				language.defaultLanguage(), language.getFallbackLanguageCode(), language.getTextDirectionCode(),
				language.getDateFormat(), language.getDatetimeFormat(), language.getNumberFormat());
	}

	private SupportLanguageResponse response(SupportLanguage language) {
		return new SupportLanguageResponse(language.getSupportLanguageCode(), language.getLanguageName(),
				language.getNativeName(), language.getIsoLanguageCode(), language.getFallbackLanguageCode(),
				language.getTextDirectionCode(), language.getDateFormat(), language.getDatetimeFormat(),
				language.getNumberFormat(), language.defaultLanguage(), language.getSortOrder(), language.getUseYn());
	}

}
