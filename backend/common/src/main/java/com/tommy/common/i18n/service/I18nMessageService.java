package com.tommy.common.i18n.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

import com.tommy.common.global.response.PageResponse;
import com.tommy.common.i18n.I18nExceptions;
import com.tommy.common.i18n.dto.MessageRequest;
import com.tommy.common.i18n.dto.MessageResponse;
import com.tommy.common.i18n.dto.MessageTextRequest;
import com.tommy.common.i18n.dto.MessageTextResponse;
import com.tommy.common.i18n.entity.I18nMessage;
import com.tommy.common.i18n.entity.I18nMessageText;
import com.tommy.common.i18n.entity.I18nNamespace;
import com.tommy.common.i18n.entity.SupportLanguage;
import com.tommy.common.i18n.repository.I18nMessageRepository;
import com.tommy.common.i18n.repository.I18nMessageTextRepository;
import com.tommy.common.i18n.repository.I18nNamespaceRepository;
import com.tommy.common.i18n.repository.MessageTextSlice;
import com.tommy.common.i18n.repository.SupportLanguageRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
@Transactional(readOnly = true)
public class I18nMessageService {

	private final I18nMessageRepository messageRepository;
	private final I18nMessageTextRepository textRepository;
	private final I18nNamespaceRepository namespaceRepository;
	private final SupportLanguageRepository languageRepository;
	private final BundleService bundles;

	public I18nMessageService(I18nMessageRepository messageRepository, I18nMessageTextRepository textRepository,
			I18nNamespaceRepository namespaceRepository, SupportLanguageRepository languageRepository, BundleService bundles) {
		this.messageRepository = messageRepository;
		this.textRepository = textRepository;
		this.namespaceRepository = namespaceRepository;
		this.languageRepository = languageRepository;
		this.bundles = bundles;
	}

	public PageResponse<MessageResponse> list(String namespaceCode, String keyword, String messageTypeCode, String useYn,
			String translationStatusCode, Pageable pageable) {
		Page<I18nMessage> page = messageRepository.findAll(
				filter(namespaceCode, keyword, messageTypeCode, useYn, translationStatusCode), pageable);
		List<I18nMessage> messages = page.getContent();
		List<String> activeLanguages = languageRepository.findByUseYnOrderBySortOrderAscSupportLanguageCodeAsc("Y").stream()
				.map(SupportLanguage::getSupportLanguageCode)
				.toList();
		Map<Long, List<MessageTextSlice>> texts = textsFor(messages);
		List<MessageResponse> items = messages.stream()
				.map(message -> listItem(message, activeLanguages, texts.getOrDefault(message.getMessagePk(), List.of())))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	public MessageResponse get(long messagePk) {
		return response(require(messagePk), true);
	}

	@Transactional
	public MessageResponse create(MessageRequest request) {
		I18nNamespace namespace = requireNamespace(request.namespaceCode());
		if (messageRepository.existsByNamespaceNamespacePkAndMessageKeyAndUseYn(namespace.getNamespacePk(), request.messageKey(), "Y")) {
			throw I18nExceptions.duplicate();
		}
		I18nMessage message = messageRepository.save(I18nMessage.create(
				namespace, request.messageKey(), request.messageTypeCode(), request.defaultMessage(), request.paramDescription()));
		upsert(message, request.translations());
		flush();
		bundles.evictNamespace(namespace.getNamespaceCode());
		return response(message, true);
	}

	@Transactional
	public MessageResponse update(long messagePk, MessageRequest request) {
		I18nMessage message = require(messagePk);
		if (!message.getNamespace().getNamespaceCode().equals(request.namespaceCode())
				|| !message.getMessageKey().equals(request.messageKey())) {
			throw I18nExceptions.immutable("네임스페이스와 메시지 키는 바꿀 수 없습니다.");
		}
		message.change(request.messageTypeCode(), request.defaultMessage(), request.paramDescription());
		upsert(message, request.translations());
		flush();
		bundles.evictNamespace(message.getNamespace().getNamespaceCode());
		return response(message, true);
	}

	@Transactional
	public void deactivate(long messagePk) {
		I18nMessage message = require(messagePk);
		message.deactivate();
		bundles.evictNamespace(message.getNamespace().getNamespaceCode());
	}

	@Transactional
	public MessageResponse restore(long messagePk) {
		I18nMessage message = require(messagePk);
		if (messageRepository.existsByNamespaceNamespacePkAndMessageKeyAndUseYn(
				message.getNamespace().getNamespacePk(), message.getMessageKey(), "Y")) {
			throw I18nExceptions.duplicate();
		}
		message.activate();
		flush();
		bundles.evictNamespace(message.getNamespace().getNamespaceCode());
		return response(message, true);
	}

	private void upsert(I18nMessage message, List<MessageTextRequest> translations) {
		if (translations == null) {
			return;
		}
		for (MessageTextRequest translation : translations) {
			SupportLanguage language = languageRepository.findById(translation.supportLanguageCode())
					.filter(SupportLanguage::inUse)
					.orElseThrow(I18nExceptions::languageNotFound);
			textRepository.findByMessageMessagePkAndSupportLanguageCode(message.getMessagePk(), language.getSupportLanguageCode())
					.ifPresentOrElse(
							text -> text.apply(translation.messageText(), translation.translationStatusCode(), translation.machineTranslated()),
							() -> textRepository.save(I18nMessageText.create(
									message,
									language.getSupportLanguageCode(),
									translation.messageText(),
									statusOrDraft(translation.translationStatusCode()),
									Boolean.TRUE.equals(translation.machineTranslated()))));
		}
	}

	private static String statusOrDraft(String status) {
		return status == null || status.isBlank() ? "DRAFT" : status;
	}

	private I18nMessage require(long messagePk) {
		return messageRepository.findById(messagePk).orElseThrow(I18nExceptions::messageNotFound);
	}

	private I18nNamespace requireNamespace(String namespaceCode) {
		return namespaceRepository.findByNamespaceCode(namespaceCode).orElseThrow(I18nExceptions::namespaceNotFound);
	}

	private void flush() {
		try {
			messageRepository.flush();
		}
		catch (DataIntegrityViolationException ex) {
			throw I18nExceptions.duplicate();
		}
	}

	private MessageResponse response(I18nMessage message, boolean withTexts) {
		List<MessageTextResponse> texts = withTexts
				? textRepository.findByMessageMessagePk(message.getMessagePk()).stream()
						.map(text -> new MessageTextResponse(text.getSupportLanguageCode(), text.getMessageText(),
								text.getTranslationStatusCode(), text.machineTranslated(), text.getVersionNo()))
						.toList()
				: List.of();
		return new MessageResponse(message.getMessagePk(), message.getNamespace().getNamespaceCode(), message.getMessageKey(),
				message.getMessageTypeCode(), message.getDefaultMessage(), message.getParamDescription(), message.getUseYn(), texts,
				null, null, null);
	}

	private Map<Long, List<MessageTextSlice>> textsFor(List<I18nMessage> messages) {
		if (messages.isEmpty()) {
			return Map.of();
		}
		List<Long> messagePks = messages.stream().map(I18nMessage::getMessagePk).toList();
		return textRepository.findSlicesByMessagePkIn(messagePks).stream()
				.collect(Collectors.groupingBy(MessageTextSlice::getMessagePk, LinkedHashMap::new, Collectors.toList()));
	}

	private MessageResponse listItem(I18nMessage message, List<String> activeLanguages, List<MessageTextSlice> texts) {
		Map<String, MessageTextSlice> byLanguage = new LinkedHashMap<>();
		for (MessageTextSlice text : texts) {
			byLanguage.putIfAbsent(text.getSupportLanguageCode(), text);
		}
		List<String> translated = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		List<String> statuses = new ArrayList<>();
		for (String language : activeLanguages) {
			MessageTextSlice text = byLanguage.get(language);
			if (text == null || text.getMessageText() == null || text.getMessageText().isBlank()) {
				missing.add(language);
				continue;
			}
			translated.add(language);
			if (!statuses.contains(text.getTranslationStatusCode())) {
				statuses.add(text.getTranslationStatusCode());
			}
		}
		return new MessageResponse(message.getMessagePk(), message.getNamespace().getNamespaceCode(), message.getMessageKey(),
				message.getMessageTypeCode(), message.getDefaultMessage(), message.getParamDescription(), message.getUseYn(),
				List.of(), List.copyOf(translated), List.copyOf(missing), List.copyOf(statuses));
	}

	private static Specification<I18nMessage> filter(String namespaceCode, String keyword, String messageTypeCode, String useYn,
			String translationStatusCode) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (namespaceCode != null && !namespaceCode.isBlank()) {
				predicates.add(builder.equal(root.get("namespace").get("namespaceCode"), namespaceCode));
			}
			if (keyword != null && !keyword.isBlank()) {
				String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("messageKey")), like),
						builder.like(builder.lower(root.get("defaultMessage")), like)));
			}
			if (messageTypeCode != null && !messageTypeCode.isBlank()) {
				predicates.add(builder.equal(root.get("messageTypeCode"), messageTypeCode));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			if (translationStatusCode != null && !translationStatusCode.isBlank()) {
				Subquery<Long> texts = query.subquery(Long.class);
				Root<I18nMessageText> text = texts.from(I18nMessageText.class);
				texts.select(text.get("messageTextPk")).where(
						builder.equal(text.get("message"), root),
						builder.equal(text.get("translationStatusCode"), translationStatusCode));
				predicates.add(builder.exists(texts));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
