package com.tommy.common.global.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LikeEscapeTest {

	@Test
	void escapesWildcardCharacters() {
		assertEquals("%a\\_b%", LikeEscape.contains("a_b"));
		assertEquals("%100\\%%", LikeEscape.contains("100%"));
		assertEquals("%a\\\\b%", LikeEscape.contains("a\\b"));
		assertEquals("%axb%", LikeEscape.contains("AxB"));
		assertEquals('\\', LikeEscape.ESCAPE);
	}

}
