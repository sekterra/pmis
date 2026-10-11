package com.tommy.common.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MyMenuRules {

	private MyMenuRules() {
	}

	public static List<MyMenuItem> visible(List<Row> rows, Map<String, List<String>> actionsByMenu) {
		Map<String, List<Row>> children = new LinkedHashMap<>();
		for (Row row : rows) {
			children.computeIfAbsent(row.parentMenuCode(), key -> new ArrayList<>()).add(row);
		}
		Set<String> included = new HashSet<>();
		for (Row row : rows) {
			include(row, children, actionsByMenu, included, new HashSet<>());
		}
		List<MyMenuItem> items = new ArrayList<>();
		for (Row row : rows) {
			if (!included.contains(row.menuCode())) {
				continue;
			}
			List<String> actions = "GROUP".equals(row.menuTypeCode()) ? List.of("READ")
					: actionsByMenu.getOrDefault(row.menuCode(), List.of());
			items.add(new MyMenuItem(row.menuCode(), row.parentMenuCode(), row.menuName(), row.menuTypeCode(), row.routePath(),
					row.iconName(), row.levelNo(), row.sortOrder(), actions));
		}
		items.sort(Comparator.comparing(MyMenuItem::levelNo, Comparator.nullsLast(Integer::compareTo))
				.thenComparing(MyMenuItem::sortOrder, Comparator.nullsLast(Integer::compareTo))
				.thenComparing(MyMenuItem::menuCode));
		return List.copyOf(items);
	}

	private static boolean include(Row row, Map<String, List<Row>> children, Map<String, List<String>> actionsByMenu,
			Set<String> included, Set<String> stack) {
		if (!row.useYn() || !row.visibleYn() || !stack.add(row.menuCode())) {
			return false;
		}
		try {
			if ("GROUP".equals(row.menuTypeCode())) {
				boolean any = false;
				for (Row child : children.getOrDefault(row.menuCode(), List.of())) {
					if (include(child, children, actionsByMenu, included, stack)) {
						any = true;
					}
				}
				if (any) {
					included.add(row.menuCode());
				}
				return any;
			}
			List<String> actions = actionsByMenu.get(row.menuCode());
			if (actions != null && actions.contains("READ")) {
				included.add(row.menuCode());
				return true;
			}
			return false;
		}
		finally {
			stack.remove(row.menuCode());
		}
	}

	public record Row(String menuCode, String parentMenuCode, String menuName, String menuTypeCode, String routePath,
			String iconName, Integer levelNo, Integer sortOrder, boolean useYn, boolean visibleYn) {
	}

}
