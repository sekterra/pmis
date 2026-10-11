package com.tommy.common.code.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.code.CodeRules;
import com.tommy.common.code.dto.CodeHistoryResponse;
import com.tommy.common.code.dto.CommonCodeRequest;
import com.tommy.common.code.dto.CommonCodeResponse;
import com.tommy.common.code.dto.TranslationRequest;
import com.tommy.common.code.dto.TranslationResponse;
import com.tommy.common.code.entity.CodeGroup;
import com.tommy.common.code.entity.CommonCode;
import com.tommy.common.code.entity.CommonCodeHistory;
import com.tommy.common.code.entity.CommonCodeLang;
import com.tommy.common.code.repository.CodeGroupRepository;
import com.tommy.common.code.repository.CommonCodeHistoryRepository;
import com.tommy.common.code.repository.CommonCodeLangRepository;
import com.tommy.common.code.repository.CommonCodeRepository;
import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.response.PageResponse;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class CommonCodeService {

	private static final String POLICY_GROUP = "SECURITY_POLICY";
	private static final java.util.Set<String> POLICY_CODES = java.util.Set.of(
			"TEMP_PASSWORD_VALID_DAYS", "PASSWORD_EXPIRE_DAYS", "PASSWORD_MIN_LENGTH", "PASSWORD_MIN_CLASSES",
			"PASSWORD_HISTORY_COUNT", "MAX_LOGIN_FAIL_COUNT", "LOCK_MINUTES", "ACCESS_TOKEN_MINUTES", "REFRESH_TOKEN_DAYS");

	private final CodeGroupRepository codeGroupRepository;
	private final CommonCodeRepository commonCodeRepository;
	private final CommonCodeLangRepository commonCodeLangRepository;
	private final CommonCodeHistoryRepository commonCodeHistoryRepository;
	private final CodeLanguageResolver languages;
	private final ObjectMapper objectMapper;

	public CommonCodeService(CodeGroupRepository codeGroupRepository, CommonCodeRepository commonCodeRepository,
			CommonCodeLangRepository commonCodeLangRepository, CommonCodeHistoryRepository commonCodeHistoryRepository,
			CodeLanguageResolver languages, ObjectMapper objectMapper) {
		this.codeGroupRepository = codeGroupRepository;
		this.commonCodeRepository = commonCodeRepository;
		this.commonCodeLangRepository = commonCodeLangRepository;
		this.commonCodeHistoryRepository = commonCodeHistoryRepository;
		this.languages = languages;
		this.objectMapper = objectMapper;
	}

	public PageResponse<CommonCodeResponse> list(String groupCode, String keyword, String useYn, Pageable pageable) {
		if (groupCode == null || groupCode.isBlank()) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "코드 그룹이 필요합니다.", 2);
		}
		requireGroup(groupCode);
		Page<CommonCode> page = commonCodeRepository.findAll(filter(groupCode, keyword, useYn), pageable);
		Map<Long, List<CommonCodeLang>> langs = langsOf(page.getContent());
		List<CommonCodeResponse> items = page.getContent().stream()
				.map(code -> toResponse(code, langs.getOrDefault(code.getCodePk(), List.of())))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	public CommonCodeResponse get(long codePk) {
		CommonCode code = require(codePk);
		return toResponse(code, commonCodeLangRepository.findByCommonCodeCodePkIn(List.of(codePk)));
	}

	public PageResponse<CodeHistoryResponse> history(long codePk, Pageable pageable) {
		require(codePk);
		Page<CommonCodeHistory> page = commonCodeHistoryRepository.findByCodePk(codePk, pageable);
		List<CodeHistoryResponse> items = page.getContent().stream().map(CommonCodeService::toHistory).toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional
	public CommonCodeResponse create(CommonCodeRequest request) {
		CodeGroup group = requireGroup(request.groupCode());
		prepare(group, null, request);
		if (commonCodeRepository.findByCodeGroupCodeGroupPkAndCodeValueAndUseYn(
				group.getCodeGroupPk(), request.codeValue(), "Y").isPresent()) {
			throw duplicate();
		}
		CommonCode code = commonCodeRepository.save(CommonCode.create(group, request.codeValue(), request.codeName()));
		apply(group, code, request);
		upsertTranslations(code, request.translations());
		record(code, "C", null, request.changeReason());
		return get(code.getCodePk());
	}

	@Transactional
	public CommonCodeResponse update(long codePk, CommonCodeRequest request) {
		CommonCode code = require(codePk);
		CodeGroup group = code.getCodeGroup();
		if (!group.getCodeGroupCode().equals(request.groupCode())) {
			throw new ApiException(400, "COMMON.VALIDATION_FAILED", "코드 그룹은 바꿀 수 없습니다.", 2);
		}
		String before = snapshot(code);
		prepare(group, code.getCodePk(), request);
		apply(group, code, request);
		upsertTranslations(code, request.translations());
		record(code, "U", before, request.changeReason());
		return get(codePk);
	}

	@Transactional
	public void delete(long codePk, String changeReason) {
		CommonCode code = require(codePk);
		if (policy(code.getCodeGroup().getCodeGroupCode(), code.getCodeValue())) {
			throw new ApiException(400, "CODE.INVALID_POLICY_VALUE", "보안 정책 값은 양의 정수여야 합니다.", 2);
		}
		String before = snapshot(code);
		code.deactivate();
		record(code, "D", before, changeReason);
	}

	@Transactional
	public CommonCodeResponse restore(long codePk, String changeReason) {
		CommonCode code = require(codePk);
		if (policy(code.getCodeGroup().getCodeGroupCode(), code.getCodeValue())) {
			CodeRules.requirePolicyValue(code.getCodeValue(), code.getExtAttr1());
		}
		if (commonCodeRepository.existsByCodeGroupCodeGroupPkAndCodeValueAndUseYnAndCodePkNot(
				code.getCodeGroup().getCodeGroupPk(), code.getCodeValue(), "Y", code.getCodePk())) {
			throw duplicate();
		}
		String before = snapshot(code);
		code.activate();
		record(code, "U", before, changeReason);
		return get(codePk);
	}

	private void prepare(CodeGroup group, Long selfPk, CommonCodeRequest request) {
		CodeRules.requireDateOrder(request.validFromDate(), request.validToDate());
		CodeRules.requireValueLength(group.getCodeValueLength(), request.codeValue());
		CodeRules.requireParentAllowed(group.hierarchical(), request.parentCodeValue());
		if (policy(group.getCodeGroupCode(), request.codeValue())) {
			CodeRules.requirePolicyValue(request.codeValue(), request.extAttr1());
		}
		if (selfPk != null && commonCodeRepository.existsByCodeGroupCodeGroupPkAndCodeValueAndUseYnAndCodePkNot(
				group.getCodeGroupPk(), request.codeValue(), "Y", selfPk)) {
			throw duplicate();
		}
	}

	private void apply(CodeGroup group, CommonCode code, CommonCodeRequest request) {
		code.changeValues(request.codeValue(), request.codeName(), request.sortOrder(), request.extAttr1(),
				request.extAttr2(), request.extAttr3(), request.extAttr4(), request.extAttr5(),
				request.validFromDate(), request.validToDate());
		place(group, code, request.parentCodeValue());
		try {
			commonCodeRepository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw duplicate();
		}
	}

	private void place(CodeGroup group, CommonCode code, String parentCodeValue) {
		List<CommonCode> family = commonCodeRepository.findByCodeGroupCodeGroupPk(group.getCodeGroupPk());
		Map<Long, CommonCode> byPk = new HashMap<>();
		for (CommonCode item : family) {
			byPk.put(item.getCodePk(), item);
		}
		byPk.put(code.getCodePk(), code);
		CommonCode parent = null;
		if (parentCodeValue != null && !parentCodeValue.isBlank()) {
			parent = family.stream()
					.filter(item -> parentCodeValue.equals(item.getCodeValue()) && item.inUse())
					.findFirst()
					.orElseThrow(() -> new ApiException(404, "CODE.PARENT_NOT_FOUND", "상위 코드를 찾을 수 없습니다.", 2));
			if (parent.getCodePk().equals(code.getCodePk())) {
				throw new ApiException(400, "CODE.CYCLE", "코드 계층이 순환합니다.", 2);
			}
		}
		CodeRules.requireNoCycle(code.getCodePk(), parent == null ? null : parent.getCodePk(),
				pk -> {
					CommonCode current = byPk.get(pk);
					return current == null || current.getParentCode() == null ? null : current.getParentCode().getCodePk();
				});
		code.place(parent, level(parent), path(parent, code.getCodeValue()));
		recomputeChildren(code, byPk);
	}

	private void recomputeChildren(CommonCode parent, Map<Long, CommonCode> byPk) {
		for (CommonCode child : byPk.values()) {
			if (child.getParentCode() != null && parent.getCodePk().equals(child.getParentCode().getCodePk())) {
				child.place(parent, level(parent), path(parent, child.getCodeValue()));
				recomputeChildren(child, byPk);
			}
		}
	}

	private static int level(CommonCode parent) {
		return parent == null || parent.getLevelNo() == null ? 1 : parent.getLevelNo() + 1;
	}

	private static String path(CommonCode parent, String codeValue) {
		String prefix = parent == null || parent.getCodePath() == null ? "" : parent.getCodePath();
		String path = prefix + "/" + codeValue;
		if (path.length() > 500) {
			throw new ApiException(400, "CODE.VALUE_LENGTH", "코드 값이 허용 길이를 넘습니다.", 2);
		}
		return path;
	}

	private void upsertTranslations(CommonCode code, List<TranslationRequest> translations) {
		if (translations == null) {
			return;
		}
		for (TranslationRequest translation : translations) {
			languages.requireSupported(translation.supportLanguageCode());
			commonCodeLangRepository.findByCommonCodeCodePkAndSupportLanguageCode(
					code.getCodePk(), translation.supportLanguageCode())
					.ifPresentOrElse(
							existing -> existing.change(translation.name(), translation.description()),
							() -> commonCodeLangRepository.save(CommonCodeLang.create(
									code, translation.supportLanguageCode(), translation.name(), translation.description())));
		}
	}

	private void record(CommonCode code, String changeType, String before, String changeReason) {
		commonCodeHistoryRepository.save(CommonCodeHistory.record(
				code.getCodePk(),
				code.getCodeGroup().getCodeGroupCode(),
				code.getCodeValue(),
				changeType,
				before,
				snapshot(code),
				changeReason));
	}

	private String snapshot(CommonCode code) {
		List<CommonCodeLang> langs = commonCodeLangRepository.findByCommonCodeCodePkIn(List.of(code.getCodePk()));
		Map<String, Object> body = new HashMap<>();
		body.put("codeValue", code.getCodeValue());
		body.put("codeName", code.getCodeName());
		body.put("parentCodeValue", code.getParentCode() == null ? null : code.getParentCode().getCodeValue());
		body.put("levelNo", code.getLevelNo());
		body.put("codePath", code.getCodePath());
		body.put("sortOrder", code.getSortOrder());
		body.put("extAttr1", code.getExtAttr1());
		body.put("extAttr2", code.getExtAttr2());
		body.put("extAttr3", code.getExtAttr3());
		body.put("extAttr4", code.getExtAttr4());
		body.put("extAttr5", code.getExtAttr5());
		body.put("validFromDate", code.getValidFromDate());
		body.put("validToDate", code.getValidToDate());
		body.put("useYn", code.inUse() ? "Y" : "N");
		body.put("translations", langs.stream()
				.sorted(Comparator.comparing(CommonCodeLang::getSupportLanguageCode))
				.map(lang -> Map.of(
						"supportLanguageCode", lang.getSupportLanguageCode(),
						"name", lang.getCodeName()))
				.toList());
		return objectMapper.writeValueAsString(body);
	}

	private CodeGroup requireGroup(String groupCode) {
		return codeGroupRepository.findByCodeGroupCode(groupCode)
				.orElseThrow(() -> new ApiException(404, "CODE.GROUP_NOT_FOUND", "코드 그룹을 찾을 수 없습니다.", 2));
	}

	private CommonCode require(long codePk) {
		return commonCodeRepository.findById(codePk)
				.orElseThrow(() -> new ApiException(404, "CODE.NOT_FOUND", "공통코드를 찾을 수 없습니다.", 2));
	}

	private Map<Long, List<CommonCodeLang>> langsOf(List<CommonCode> codes) {
		if (codes.isEmpty()) {
			return Map.of();
		}
		return commonCodeLangRepository.findByCommonCodeCodePkIn(codes.stream().map(CommonCode::getCodePk).toList())
				.stream()
				.collect(Collectors.groupingBy(lang -> lang.getCommonCode().getCodePk()));
	}

	private static CommonCodeResponse toResponse(CommonCode code, List<CommonCodeLang> langs) {
		return new CommonCodeResponse(
				code.getCodePk(),
				code.getCodeGroup().getCodeGroupCode(),
				code.getCodeValue(),
				code.getCodeName(),
				code.getParentCode() == null ? null : code.getParentCode().getCodeValue(),
				code.getLevelNo(),
				code.getCodePath(),
				code.getSortOrder(),
				code.getExtAttr1(),
				code.getExtAttr2(),
				code.getExtAttr3(),
				code.getExtAttr4(),
				code.getExtAttr5(),
				code.getValidFromDate(),
				code.getValidToDate(),
				code.inUse() ? "Y" : "N",
				langs.stream()
						.map(lang -> new TranslationResponse(lang.getSupportLanguageCode(), lang.getCodeName(),
								lang.getDescription()))
						.toList());
	}

	private static CodeHistoryResponse toHistory(CommonCodeHistory history) {
		return new CodeHistoryResponse(
				history.getHistoryPk(),
				history.getCodePk(),
				history.getCodeGroupCode(),
				history.getCodeValue(),
				history.getChangeTypeCode(),
				history.getBeforeData(),
				history.getAfterData(),
				history.getChangeReason(),
				history.getChangeDatetime(),
				history.getChangeUserPk());
	}

	private static Specification<CommonCode> filter(String groupCode, String keyword, String useYn) {
		return (root, query, builder) -> {
			Join<Object, Object> group = root.join("codeGroup");
			Predicate predicate = builder.equal(group.get("codeGroupCode"), groupCode);
			if (keyword != null && !keyword.isBlank()) {
				String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
				predicate = builder.and(predicate, builder.or(
						builder.like(builder.lower(root.get("codeValue")), pattern),
						builder.like(builder.lower(root.get("codeName")), pattern)));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicate = builder.and(predicate, builder.equal(root.get("useYn"), "Y".equalsIgnoreCase(useYn) ? "Y" : "N"));
			}
			return predicate;
		};
	}

	private static boolean policy(String groupCode, String codeValue) {
		return POLICY_GROUP.equals(groupCode) && POLICY_CODES.contains(codeValue);
	}

	private static ApiException duplicate() {
		return new ApiException(409, "CODE.DUPLICATE", "이미 있는 코드입니다.", 2);
	}

}
