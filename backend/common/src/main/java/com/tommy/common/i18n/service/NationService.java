package com.tommy.common.i18n.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.response.PageResponse;
import com.tommy.common.i18n.I18nExceptions;
import com.tommy.common.i18n.dto.NationItem;
import com.tommy.common.i18n.dto.NationRequest;
import com.tommy.common.i18n.dto.NationResponse;
import com.tommy.common.i18n.entity.Nation;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.i18n.repository.NationRepository;
import com.tommy.common.i18n.repository.SupportLanguageRepository;

@Service
@Transactional(readOnly = true)
public class NationService {

	private final NationRepository nationRepository;
	private final SupportLanguageRepository languageRepository;

	public NationService(NationRepository nationRepository, SupportLanguageRepository languageRepository) {
		this.nationRepository = nationRepository;
		this.languageRepository = languageRepository;
	}

	public List<NationItem> listActive() {
		return nationRepository.findByUseYnOrderBySortOrderAscNationCodeAsc("Y").stream()
				.map(nation -> new NationItem(nation.getNationCode(), nation.getNationName(),
						nation.getDefaultLanguage().getSupportLanguageCode(), nation.getPhoneCode(),
						nation.getCurrencyCode(), nation.getTimezoneId()))
				.toList();
	}

	public PageResponse<NationResponse> list(Pageable pageable) {
		Page<Nation> page = nationRepository.findAll(pageable);
		List<NationResponse> items = page.getContent().stream().map(this::response).toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	public NationResponse get(String nationCode) {
		return response(require(nationCode));
	}

	@Transactional
	public NationResponse create(NationRequest request) {
		if (nationRepository.existsByNationCode(request.nationCode())) {
			throw I18nExceptions.duplicate();
		}
		Nation nation = nationRepository.save(Nation.create(
				request.nationCode(),
				request.isoAlpha3Code(),
				request.isoNumericCode(),
				request.nationName(),
				activeLanguage(request.defaultLanguageCode()),
				request.phoneCode(),
				request.currencyCode(),
				request.timezoneId(),
				request.sortOrder()));
		flush();
		return response(nation);
	}

	@Transactional
	public NationResponse update(String nationCode, NationRequest request) {
		if (!nationCode.equals(request.nationCode())) {
			throw I18nExceptions.immutable("국가 코드는 바꿀 수 없습니다.");
		}
		Nation nation = require(nationCode);
		nation.change(request.isoAlpha3Code(), request.isoNumericCode(), request.nationName(),
				activeLanguage(request.defaultLanguageCode()), request.phoneCode(), request.currencyCode(),
				request.timezoneId(), request.sortOrder());
		flush();
		return response(nation);
	}

	@Transactional
	public void deactivate(String nationCode) {
		require(nationCode).deactivate();
	}

	@Transactional
	public NationResponse restore(String nationCode) {
		Nation nation = require(nationCode);
		nation.activate();
		flush();
		return response(nation);
	}

	private Nation require(String nationCode) {
		return nationRepository.findByNationCode(nationCode).orElseThrow(I18nExceptions::nationNotFound);
	}

	private SupportLanguage activeLanguage(String code) {
		return languageRepository.findById(code).filter(SupportLanguage::inUse).orElseThrow(I18nExceptions::languageNotFound);
	}

	private void flush() {
		try {
			nationRepository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw I18nExceptions.duplicate();
		}
	}

	private NationResponse response(Nation nation) {
		return new NationResponse(nation.getNationPk(), nation.getNationCode(), nation.getIsoAlpha3Code(),
				nation.getIsoNumericCode(), nation.getNationName(), nation.getDefaultLanguage().getSupportLanguageCode(),
				nation.getPhoneCode(), nation.getCurrencyCode(), nation.getTimezoneId(), nation.getSortOrder(),
				nation.getUseYn());
	}

}
