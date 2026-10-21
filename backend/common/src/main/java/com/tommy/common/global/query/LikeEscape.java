package com.tommy.common.global.query;

import java.util.Locale;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public final class LikeEscape {

	public static final char ESCAPE = '\\';

	private LikeEscape() {
	}

	public static String contains(String raw) {
		String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
		return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}

	public static Predicate contains(CriteriaBuilder builder, Expression<String> expression, String raw) {
		return builder.like(builder.lower(expression), contains(raw), ESCAPE);
	}

}
