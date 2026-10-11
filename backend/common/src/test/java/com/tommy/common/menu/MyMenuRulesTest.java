package com.tommy.common.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MyMenuRulesTest {

	@Test
	void dropsAPageWithoutReadAndAGroupWhoseChildrenAreGone() {
		List<MyMenuItem> items = MyMenuRules.visible(List.of(
				row("SYSTEM", null, "GROUP", 1, 10, true),
				row("COMPANY", "SYSTEM", "PAGE", 2, 30, true),
				row("ROLE", "SYSTEM", "PAGE", 2, 90, true),
				row("HIDDEN", "SYSTEM", "PAGE", 2, 40, false)),
				Map.of("ROLE", List.of("CREATE"), "HIDDEN", List.of("READ")));

		assertEquals(List.of(), items);
	}

	@Test
	void keepsAGroupWhenAnyVisibleChildRemains() {
		List<MyMenuItem> items = MyMenuRules.visible(List.of(
				row("SYSTEM", null, "GROUP", 1, 10, true),
				row("I18N", "SYSTEM", "PAGE", 2, 10, true),
				row("ROLE", "SYSTEM", "PAGE", 2, 90, false)),
				Map.of("I18N", List.of("READ"), "ROLE", List.of("READ")));

		assertEquals(List.of("SYSTEM", "I18N"), items.stream().map(MyMenuItem::menuCode).toList());
		assertEquals(List.of("READ"), items.get(0).actions());
		assertEquals(List.of("READ"), items.get(1).actions());
		assertEquals("SYSTEM", items.get(1).parentMenuCode());
		assertNull(items.get(0).parentMenuCode());
	}

	@Test
	void dropsTheGroupWhenEveryChildIsHiddenOrUnused() {
		List<MyMenuItem> items = MyMenuRules.visible(List.of(
				row("SYSTEM", null, "GROUP", 1, 10, true),
				row("I18N", "SYSTEM", "PAGE", 2, 10, false),
				unused("ROLE", "SYSTEM")),
				Map.of("I18N", List.of("READ"), "ROLE", List.of("READ")));

		assertEquals(List.of(), items);
	}

	@Test
	void sortsByLevelThenSortOrderThenMenuCode() {
		List<MyMenuItem> items = MyMenuRules.visible(List.of(
				row("PAGE_B", "SYSTEM", "PAGE", 2, 10, true),
				row("PAGE_A", "SYSTEM", "PAGE", 2, 10, true),
				row("SYSTEM", null, "GROUP", 1, 10, true),
				row("LATE", null, "PAGE", null, null, true)),
				Map.of("PAGE_A", List.of("READ", "CREATE"), "PAGE_B", List.of("READ"), "LATE", List.of("READ")));

		assertEquals(List.of("SYSTEM", "PAGE_A", "PAGE_B", "LATE"), items.stream().map(MyMenuItem::menuCode).toList());
		assertEquals(List.of("READ", "CREATE"), items.get(1).actions());
	}

	private static MyMenuRules.Row row(String code, String parent, String type, Integer level, Integer sort, boolean visible) {
		return new MyMenuRules.Row(code, parent, code, type, "/" + code, null, level, sort, true, visible);
	}

	private static MyMenuRules.Row unused(String code, String parent) {
		return new MyMenuRules.Row(code, parent, code, "PAGE", "/" + code, null, 2, 90, false, true);
	}

}
