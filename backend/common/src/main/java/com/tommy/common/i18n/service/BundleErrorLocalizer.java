package com.tommy.common.i18n.service;

import org.springframework.stereotype.Component;

import com.tommy.common.global.exception.ErrorMessageLocalizer;
import com.tommy.common.global.multitenancy.TenantContext;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class BundleErrorLocalizer implements ErrorMessageLocalizer {

	private final BundleService bundles;

	public BundleErrorLocalizer(BundleService bundles) {
		this.bundles = bundles;
	}

	@Override
	public String localize(String key, String fallback, HttpServletRequest request) {
		try {
			if (key == null || key.isBlank() || TenantContext.schema() == null) {
				return fallback;
			}
			String lang = request == null ? null : request.getParameter("lang");
			String accept = request == null ? null : request.getHeader("Accept-Language");
			String found = bundles.findQuietly("error", key, lang, accept);
			return found == null || found.isBlank() ? fallback : found;
		}
		catch (RuntimeException ex) {
			return fallback;
		}
	}

}
