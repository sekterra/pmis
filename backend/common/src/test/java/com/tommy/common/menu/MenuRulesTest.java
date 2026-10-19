package com.tommy.common.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MenuRulesTest {

	@Test
	void acceptsSeedCodesAndRejectsBadRoutes() {
		for (String code : List.of("SYSTEM", "I18N", "COMMON_CODE", "MENU")) {
			assertTrue(MenuRules.menuCode(code));
			assertTrue(SeedMenuCodes.protects(code, "MDM"));
		}
		assertTrue(MenuRules.solution("MDM"));
		assertFalse(MenuRules.solution("mdm"));
		assertTrue(MenuRules.route("/mdm/codes"));
		assertFalse(MenuRules.route("mdm/codes"));
		assertFalse(MenuRules.route("/mdm//codes"));
		assertFalse(MenuRules.route("/mdm/../codes"));
		assertFalse(MenuRules.route("/mdm/codes here"));
		assertFalse(MenuRules.icon("1icon"));
		assertTrue(MenuRules.icon("SettingOutlined"));
		assertTrue(MenuRules.cycle("/SYSTEM/I18N", "/SYSTEM/I18N/CHILD"));
		assertFalse(MenuRules.cycle("/SYSTEM/I18N", "/SYSTEM"));
		assertEquals("/SYSTEM/I18N", MenuRules.path("/SYSTEM", "I18N"));
		assertTrue(MenuRules.tooDeep(5));
	}

	@Test
	void assemblesTreeWithoutOrphans() {
		List<MenuTree.Node> nodes = List.of(
				new MenuTree.Node(1, null, "SYSTEM", "시스템", "GROUP", 1, 10, 1),
				new MenuTree.Node(3, 1L, "MENU", "메뉴", "PAGE", 2, 20, 5),
				new MenuTree.Node(2, 1L, "I18N", "다국어", "PAGE", 2, 10, 5),
				new MenuTree.Node(9, 99L, "ORPHAN", "고아", "PAGE", 2, 1, 0));
		List<MenuTree.Branch> roots = MenuTree.assemble(nodes);
		assertEquals(1, roots.size());
		assertEquals(2, roots.get(0).childCount());
		assertEquals("I18N", roots.get(0).children().get(0).node().menuCode());
		assertEquals("MENU", roots.get(0).children().get(1).node().menuCode());
		assertTrue(roots.stream().noneMatch(branch -> "ORPHAN".equals(branch.node().menuCode())));
	}

}
