package com.tommy.common.i18n.dto;

import java.util.Map;

public record BundleView(String namespaceCode, String language, Map<String, String> messages) {
}
