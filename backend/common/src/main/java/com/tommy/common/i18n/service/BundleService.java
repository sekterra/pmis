package com.tommy.common.i18n.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.code.service.CodeLanguageResolver;
import com.tommy.common.global.multitenancy.TenantContext;
import com.tommy.common.i18n.BundleTexts;
import com.tommy.common.i18n.I18nExceptions;
import com.tommy.common.i18n.dto.BundleView;
import com.tommy.common.i18n.entity.I18nMessage;
import com.tommy.common.i18n.entity.I18nMessageText;
import com.tommy.common.i18n.entity.I18nNamespace;
import com.tommy.common.i18n.repository.I18nMessageRepository;
import com.tommy.common.i18n.repository.I18nMessageTextRepository;
import com.tommy.common.i18n.repository.I18nNamespaceRepository;

@Service
@Transactional(readOnly = true)
public class BundleService {

	private static final long TTL_MILLIS = 60_000L;

	private final I18nNamespaceRepository namespaceRepository;
	private final I18nMessageRepository messageRepository;
	private final I18nMessageTextRepository textRepository;
	private final CodeLanguageResolver languages;
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, Cached>> cache = new ConcurrentHashMap<>();

	public BundleService(I18nNamespaceRepository namespaceRepository, I18nMessageRepository messageRepository,
			I18nMessageTextRepository textRepository, CodeLanguageResolver languages) {
		this.namespaceRepository = namespaceRepository;
		this.messageRepository = messageRepository;
		this.textRepository = textRepository;
		this.languages = languages;
	}

	public BundleView bundle(String namespaceCode, String lang, String acceptLanguage) {
		String language = languages.choose(lang, acceptLanguage);
		return cached(namespaceCode, language, () -> load(namespaceCode, language));
	}

	public Map<String, BundleView> bundles(String namespaces, String lang, String acceptLanguage) {
		Map<String, BundleView> result = new LinkedHashMap<>();
		for (String code : BundleTexts.namespaceCodes(namespaces)) {
			result.put(code, bundle(code, lang, acceptLanguage));
		}
		return result;
	}

	public String findQuietly(String namespaceCode, String key, String lang, String acceptLanguage) {
		try {
			if (TenantContext.schema() == null) {
				return null;
			}
			return bundle(namespaceCode, lang, acceptLanguage).messages().get(key);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	public void evictNamespace(String namespaceCode) {
		ConcurrentHashMap<String, Cached> tenant = cache.get(schemaKey());
		if (tenant == null) {
			return;
		}
		String prefix = namespaceCode + "\n";
		tenant.keySet().removeIf(key -> key.startsWith(prefix));
	}

	public void evictTenant() {
		cache.remove(schemaKey());
	}

	private BundleView cached(String namespaceCode, String language, java.util.function.Supplier<BundleView> loader) {
		ConcurrentHashMap<String, Cached> tenant = cache.computeIfAbsent(schemaKey(), key -> new ConcurrentHashMap<>());
		String key = namespaceCode + "\n" + language;
		long now = System.currentTimeMillis();
		Cached current = tenant.get(key);
		if (current != null && current.expiresAt() > now) {
			return current.view();
		}
		BundleView view = loader.get();
		tenant.put(key, new Cached(view, now + TTL_MILLIS));
		return view;
	}

	private BundleView load(String namespaceCode, String language) {
		I18nNamespace namespace = namespaceRepository.findByNamespaceCode(namespaceCode)
				.filter(I18nNamespace::inUse)
				.orElseThrow(I18nExceptions::namespaceNotFound);
		List<I18nMessage> messages = messageRepository.findByNamespaceNamespacePkAndUseYn(namespace.getNamespacePk(), "Y");
		Map<Long, Map<String, String>> texts = new LinkedHashMap<>();
		if (!messages.isEmpty()) {
			List<Long> ids = messages.stream().map(I18nMessage::getMessagePk).toList();
			for (I18nMessageText text : textRepository.findByMessageMessagePkIn(ids)) {
				if ("Y".equals(text.getUseYn())) {
					texts.computeIfAbsent(text.getMessage().getMessagePk(), key -> new LinkedHashMap<>())
							.put(text.getSupportLanguageCode(), text.getMessageText());
				}
			}
		}
		Map<String, String> fallbacks = languages.fallbackMap();
		String tenantDefault = languages.tenantDefaultLanguage();
		Map<String, String> resolved = new LinkedHashMap<>();
		for (I18nMessage message : messages) {
			resolved.put(message.getMessageKey(), BundleTexts.resolve(
					language,
					texts.getOrDefault(message.getMessagePk(), Map.of()),
					fallbacks,
					tenantDefault,
					message.getDefaultMessage()));
		}
		return new BundleView(namespace.getNamespaceCode(), language, resolved);
	}

	private static String schemaKey() {
		String schema = TenantContext.schema();
		return schema == null ? "" : schema;
	}

	private record Cached(BundleView view, long expiresAt) {
	}

}
