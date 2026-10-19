package com.tommy.common.org.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.i18n.entity.I18nEntityText;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.i18n.repository.I18nEntityTextRepository;
import com.tommy.common.i18n.repository.SupportLanguageRepository;
import com.tommy.common.org.dto.TextTranslation;
import com.tommy.common.org.support.OrgExceptions;

@Service
public class EntityTextService {

	public static final String COMPANY = "company";

	public static final String COMPANY_NAME = "company_name";

	public static final String DEPT = "dept";

	public static final String DEPT_NAME = "dept_name";

	public static final String PERSON = "person";

	public static final String PERSON_NAME = "person_name";

	public static final String TEAM = "team";

	public static final String TEAM_NAME = "team_name";

	private final I18nEntityTextRepository texts;

	private final SupportLanguageRepository languages;

	private final CurrentTenantUser currentUser;

	public EntityTextService(I18nEntityTextRepository texts, SupportLanguageRepository languages, CurrentTenantUser currentUser) {
		this.texts = texts;
		this.languages = languages;
		this.currentUser = currentUser;
	}

	public void upsert(String table, String column, Long targetPk, List<TextTranslation> translations) {
		if (translations == null || translations.isEmpty()) {
			return;
		}
		Set<String> seen = new HashSet<>();
		String key = Long.toString(targetPk);
		Long userPk = currentUser.pkOrNull();
		for (TextTranslation translation : translations) {
			if (!seen.add(translation.supportLanguageCode())) {
				throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "같은 언어가 두 번 있습니다.", "translations");
			}
			SupportLanguage language = languages.findById(translation.supportLanguageCode())
					.filter(SupportLanguage::inUse)
					.orElseThrow(() -> OrgExceptions.field("I18N.LANGUAGE_NOT_FOUND", "언어를 찾을 수 없습니다.", "translations"));
			I18nEntityText row = texts
					.findByTargetTableNameAndTargetColumnNameAndTargetKeyAndSupportLanguageCode(table, column, key,
							language.getSupportLanguageCode())
					.orElseGet(() -> I18nEntityText.create(table, column, key, language.getSupportLanguageCode(),
							translation.text(), userPk));
			row.change(translation.text(), userPk);
			texts.save(row);
		}
	}

	public List<TextTranslation> list(String table, String column, Long targetPk) {
		return texts.findByTargetTableNameAndTargetColumnNameAndTargetKeyAndUseYnOrderBySupportLanguageCodeAsc(
				table, column, Long.toString(targetPk), "Y").stream()
				.map(row -> new TextTranslation(row.getSupportLanguageCode(), row.getTextValue()))
				.toList();
	}

}
