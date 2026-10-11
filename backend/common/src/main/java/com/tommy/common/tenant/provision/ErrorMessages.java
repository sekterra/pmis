package com.tommy.common.tenant.provision;

public final class ErrorMessages {

	private ErrorMessages() {
	}

	public static String mask(Throwable throwable) {
		String message = throwable.getMessage();
		if (message == null || message.isBlank()) {
			message = throwable.getClass().getSimpleName();
		}
		message = message.replaceAll("(?i)jdbc:[^\\s]+", "[masked-url]");
		message = message.replaceAll("(?i)(password\\s*[=:]\\s*)\\S+", "$1[masked]");
		if (message.length() > 500) {
			return message.substring(0, 500);
		}
		return message;
	}

}
