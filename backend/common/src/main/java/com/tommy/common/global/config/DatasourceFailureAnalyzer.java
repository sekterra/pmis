package com.tommy.common.global.config;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

public class DatasourceFailureAnalyzer implements FailureAnalyzer {

	@Override
	public FailureAnalysis analyze(Throwable failure) {
		if (!datasourceRelated(failure)) {
			return null;
		}
		return new FailureAnalysis(DbConnectionRequirements.MISSING, "접속 정보는 출력하지 않습니다.",
				new IllegalStateException(DbConnectionRequirements.MISSING));
	}

	private static boolean datasourceRelated(Throwable failure) {
		Throwable current = failure;
		while (current != null) {
			String type = current.getClass().getName();
			if (type.contains("DataSource") || type.contains("Hikari") || type.contains("Jdbc")) {
				return true;
			}
			String message = current.getMessage();
			if (message != null && (message.contains("url must start with") || message.contains("jdbc:")
					|| message.equals(DbConnectionRequirements.MISSING))) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

}
