package com.tommy.common.code.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.code.dto.CodeGroupRequest;
import com.tommy.common.code.dto.CodeGroupResponse;
import com.tommy.common.code.dto.TranslationRequest;
import com.tommy.common.code.dto.TranslationResponse;
import com.tommy.common.code.entity.CodeGroup;
import com.tommy.common.code.entity.CodeGroupLang;
import com.tommy.common.code.repository.CodeGroupLangRepository;
import com.tommy.common.code.repository.CodeGroupRepository;
import com.tommy.common.code.repository.CommonCodeRepository;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.PageResponse;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class CodeGroupService {

	private final CodeGroupRepository codeGroupRepository;
	private final CodeGroupLangRepository codeGroupLangRepository;
	private final CommonCodeRepository commonCodeRepository;
	private final CodeLanguageResolver languages;

	public CodeGroupService(CodeGroupRepository codeGroupRepository, CodeGroupLangRepository codeGroupLangRepository,
			CommonCodeRepository commonCodeRepository, CodeLanguageResolver languages) {
		this.codeGroupRepository = codeGroupRepository;
		this.codeGroupLangRepository = codeGroupLangRepository;
		this.commonCodeRepository = commonCodeRepository;
		this.languages = languages;
	}

	public PageResponse<CodeGroupResponse> list(String keyword, String solutionCode, String useYn, Pageable pageable) {
		Page<CodeGroup> page = codeGroupRepository.findAll(filter(keyword, solutionCode, useYn), pageable);
		Map<Long, List<CodeGroupLang>> langs = langsOf(page.getContent());
		List<CodeGroupResponse> items = page.getContent().stream()
				.map(group -> toResponse(group, langs.getOrDefault(group.getCodeGroupPk(), List.of())))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	public CodeGroupResponse get(String groupCode) {
		CodeGroup group = require(groupCode);
		return toResponse(group, codeGroupLangRepository.findByCodeGroupCodeGroupPkIn(List.of(group.getCodeGroupPk())));
	}

	@Transactional
	public CodeGroupResponse create(CodeGroupRequest request) {
		if (codeGroupRepository.existsByCodeGroupCode(request.codeGroupCode())) {
			throw duplicate();
		}
		CodeGroup group = codeGroupRepository.save(CodeGroup.create(
				request.codeGroupCode(),
				request.solutionCode(),
				request.codeGroupName(),
				request.description(),
				Boolean.TRUE.equals(request.hierarchy()),
				request.codeValueLength(),
				request.extAttrSchema(),
				request.sortOrder()));
		upsertTranslations(group, request.translations());
		return get(group.getCodeGroupCode());
	}

	@Transactional
	public CodeGroupResponse update(String groupCode, CodeGroupRequest request) {
		CodeGroup group = require(groupCode);
		group.change(request.solutionCode(), request.codeGroupName(), request.description(), request.hierarchy(),
				request.codeValueLength(), request.extAttrSchema(), request.sortOrder());
		upsertTranslations(group, request.translations());
		return get(groupCode);
	}

	@Transactional
	public void delete(String groupCode) {
		CodeGroup group = require(groupCode);
		if (commonCodeRepository.existsByCodeGroupCodeGroupPkAndUseYn(group.getCodeGroupPk(), "Y")) {
			throw new ApiException(409, "CODE.GROUP_IN_USE", "사용 중인 코드가 있어 그룹을 삭제할 수 없습니다.", 2);
		}
		group.deactivate();
	}

	@Transactional
	public CodeGroupResponse restore(String groupCode) {
		CodeGroup group = require(groupCode);
		group.activate();
		return get(groupCode);
	}

	private void upsertTranslations(CodeGroup group, List<TranslationRequest> translations) {
		if (translations == null) {
			return;
		}
		for (TranslationRequest translation : translations) {
			languages.requireSupported(translation.supportLanguageCode());
			codeGroupLangRepository.findByCodeGroupCodeGroupPkAndSupportLanguageCode(
					group.getCodeGroupPk(), translation.supportLanguageCode())
					.ifPresentOrElse(
							existing -> existing.change(translation.name(), translation.description()),
							() -> codeGroupLangRepository.save(CodeGroupLang.create(
									group, translation.supportLanguageCode(), translation.name(), translation.description())));
		}
	}

	private CodeGroup require(String groupCode) {
		return codeGroupRepository.findByCodeGroupCode(groupCode)
				.orElseThrow(() -> new ApiException(404, "CODE.GROUP_NOT_FOUND", "코드 그룹을 찾을 수 없습니다.", 2));
	}

	private Map<Long, List<CodeGroupLang>> langsOf(List<CodeGroup> groups) {
		if (groups.isEmpty()) {
			return Map.of();
		}
		return codeGroupLangRepository.findByCodeGroupCodeGroupPkIn(
				groups.stream().map(CodeGroup::getCodeGroupPk).toList()).stream()
				.collect(Collectors.groupingBy(lang -> lang.getCodeGroup().getCodeGroupPk()));
	}

	private static CodeGroupResponse toResponse(CodeGroup group, List<CodeGroupLang> langs) {
		return new CodeGroupResponse(
				group.getCodeGroupPk(),
				group.getCodeGroupCode(),
				group.getSolutionCode(),
				group.getCodeGroupName(),
				group.getDescription(),
				group.hierarchical(),
				group.getCodeValueLength(),
				group.getExtAttrSchema(),
				group.getSortOrder(),
				group.inUse() ? "Y" : "N",
				langs.stream()
						.map(lang -> new TranslationResponse(lang.getSupportLanguageCode(), lang.getCodeGroupName(),
								lang.getDescription()))
						.toList());
	}

	private static Specification<CodeGroup> filter(String keyword, String solutionCode, String useYn) {
		return (root, query, builder) -> {
			Predicate predicate = builder.conjunction();
			if (keyword != null && !keyword.isBlank()) {
				String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
				predicate = builder.and(predicate, builder.or(
						builder.like(builder.lower(root.get("codeGroupCode")), pattern),
						builder.like(builder.lower(root.get("codeGroupName")), pattern)));
			}
			if (solutionCode != null && !solutionCode.isBlank()) {
				predicate = builder.and(predicate, builder.equal(root.get("solutionCode"), solutionCode));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicate = builder.and(predicate, builder.equal(root.get("useYn"), "Y".equalsIgnoreCase(useYn) ? "Y" : "N"));
			}
			return predicate;
		};
	}

	private static ApiException duplicate() {
		return new ApiException(409, "CODE.DUPLICATE", "이미 있는 코드입니다.", 2);
	}

}
