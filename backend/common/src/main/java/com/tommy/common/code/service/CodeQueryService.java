package com.tommy.common.code.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.code.dto.CodeItemResponse;
import com.tommy.common.code.entity.CodeGroup;
import com.tommy.common.code.entity.CommonCode;
import com.tommy.common.code.entity.CommonCodeLang;
import com.tommy.common.code.repository.CodeGroupRepository;
import com.tommy.common.code.repository.CommonCodeLangRepository;
import com.tommy.common.code.repository.CommonCodeRepository;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.time.TenantClock;

@Service
@Transactional(readOnly = true)
public class CodeQueryService {

	private static final int MAX_GROUPS = 30;

	private final CodeGroupRepository codeGroupRepository;
	private final CommonCodeRepository commonCodeRepository;
	private final CommonCodeLangRepository commonCodeLangRepository;
	private final CodeLanguageResolver languages;
	private final TenantClock tenantClock;

	public CodeQueryService(CodeGroupRepository codeGroupRepository, CommonCodeRepository commonCodeRepository,
			CommonCodeLangRepository commonCodeLangRepository, CodeLanguageResolver languages, TenantClock tenantClock) {
		this.codeGroupRepository = codeGroupRepository;
		this.commonCodeRepository = commonCodeRepository;
		this.commonCodeLangRepository = commonCodeLangRepository;
		this.languages = languages;
		this.tenantClock = tenantClock;
	}

	public List<CodeItemResponse> list(String groupCode, String lang, String acceptLanguage) {
		return items(requireActiveGroup(groupCode), languages.choose(lang, acceptLanguage));
	}

	public Map<String, List<CodeItemResponse>> listGroups(String groups, String lang, String acceptLanguage) {
		if (groups == null || groups.isBlank()) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "코드 그룹이 필요합니다.", 2);
		}
		List<String> codes = new ArrayList<>();
		for (String part : groups.split(",")) {
			if (!part.isBlank()) {
				codes.add(part.trim());
			}
		}
		if (codes.isEmpty() || codes.size() > MAX_GROUPS) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "그룹은 1개 이상 30개까지 조회할 수 있습니다.", 2);
		}
		String language = languages.choose(lang, acceptLanguage);
		Map<String, List<CodeItemResponse>> result = new LinkedHashMap<>();
		for (String code : codes) {
			result.put(code, items(requireActiveGroup(code), language));
		}
		return result;
	}

	private List<CodeItemResponse> items(CodeGroup group, String language) {
		LocalDate today = tenantClock.today();
		List<CommonCode> codes = commonCodeRepository.findByCodeGroupCodeGroupPk(group.getCodeGroupPk()).stream()
				.filter(CommonCode::inUse)
				.filter(code -> within(code, today))
				.sorted(Comparator.comparing(CommonCode::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(CommonCode::getCodeValue)
						.thenComparing(CommonCode::getCodePk))
				.toList();
		Map<Long, Map<String, String>> names = new java.util.HashMap<>();
		if (!codes.isEmpty()) {
			for (CommonCodeLang lang : commonCodeLangRepository.findByCommonCodeCodePkIn(
					codes.stream().map(CommonCode::getCodePk).toList())) {
				names.computeIfAbsent(lang.getCommonCode().getCodePk(), key -> new java.util.HashMap<>())
						.put(lang.getSupportLanguageCode(), lang.getCodeName());
			}
		}
		return codes.stream().map(code -> new CodeItemResponse(
				code.getCodeValue(),
				languages.name(language, names.getOrDefault(code.getCodePk(), Map.of()), code.getCodeName()),
				code.getParentCode() == null ? null : code.getParentCode().getCodeValue(),
				code.getSortOrder(),
				code.getExtAttr1(),
				code.getExtAttr2(),
				code.getExtAttr3(),
				code.getExtAttr4(),
				code.getExtAttr5())).toList();
	}

	private CodeGroup requireActiveGroup(String groupCode) {
		CodeGroup group = codeGroupRepository.findByCodeGroupCode(groupCode)
				.orElseThrow(() -> new ApiException(404, "CODE.GROUP_NOT_FOUND", "코드 그룹을 찾을 수 없습니다.", 2));
		if (!group.inUse()) {
			throw new ApiException(404, "CODE.GROUP_NOT_FOUND", "코드 그룹을 찾을 수 없습니다.", 2);
		}
		return group;
	}

	public boolean isCurrentlyUsable(String groupCode, String codeValue) {
		if (codeValue == null || codeValue.isBlank()) {
			return true;
		}
		Optional<CodeGroup> found = codeGroupRepository.findByCodeGroupCode(groupCode);
		if (found.isEmpty() || !found.get().inUse()) {
			return false;
		}
		LocalDate today = tenantClock.today();
		return commonCodeRepository
				.findByCodeGroupCodeGroupPkAndCodeValueAndUseYn(found.get().getCodeGroupPk(), codeValue, "Y")
				.filter(code -> within(code, today))
				.isPresent();
	}

	private static boolean within(CommonCode code, LocalDate today) {
		return (code.getValidFromDate() == null || !code.getValidFromDate().isAfter(today))
				&& (code.getValidToDate() == null || !code.getValidToDate().isBefore(today));
	}

}
