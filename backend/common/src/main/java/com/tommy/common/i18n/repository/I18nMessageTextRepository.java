package com.tommy.common.i18n.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tommy.common.i18n.entity.I18nMessageText;

public interface I18nMessageTextRepository extends JpaRepository<I18nMessageText, Long> {

	List<I18nMessageText> findByMessageMessagePkIn(Collection<Long> messagePks);

	@Query("""
			select text.message.messagePk as messagePk, text.supportLanguageCode as supportLanguageCode,
					text.messageText as messageText, text.translationStatusCode as translationStatusCode
			from I18nMessageText text
			where text.message.messagePk in :messagePks
			""")
	List<MessageTextSlice> findSlicesByMessagePkIn(@Param("messagePks") Collection<Long> messagePks);

	List<I18nMessageText> findByMessageMessagePk(Long messagePk);

	Optional<I18nMessageText> findByMessageMessagePkAndSupportLanguageCode(Long messagePk, String supportLanguageCode);

}
