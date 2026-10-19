package com.tommy.common.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MenuTree {

	public record Node(long menuPk, Long parentMenuPk, String menuCode, String menuName, String menuTypeCode, int levelNo,
			Integer sortOrder, int permissionCount) {
	}

	public record Branch(Node node, int childCount, List<Branch> children) {
	}

	private MenuTree() {
	}

	public static List<Branch> assemble(List<Node> nodes) {
		Map<Long, List<Node>> byParent = new HashMap<>();
		Map<Long, Node> byPk = new HashMap<>();
		for (Node node : nodes) {
			byPk.put(node.menuPk(), node);
			Long parent = node.parentMenuPk();
			if (parent != null && nodes.stream().noneMatch(item -> item.menuPk() == parent)) {
				continue;
			}
			byParent.computeIfAbsent(parent, key -> new ArrayList<>()).add(node);
		}
		for (List<Node> siblings : byParent.values()) {
			siblings.sort(Comparator.comparing(Node::sortOrder, Comparator.nullsLast(Integer::compareTo)).thenComparing(Node::menuCode));
		}
		List<Branch> roots = new ArrayList<>();
		for (Node node : byParent.getOrDefault(null, List.of())) {
			if (byPk.containsKey(node.menuPk())) {
				roots.add(branch(node, byParent));
			}
		}
		return List.copyOf(roots);
	}

	private static Branch branch(Node node, Map<Long, List<Node>> byParent) {
		List<Node> children = byParent.getOrDefault(node.menuPk(), List.of());
		List<Branch> nested = new ArrayList<>();
		for (Node child : children) {
			nested.add(branch(child, byParent));
		}
		return new Branch(node, nested.size(), List.copyOf(nested));
	}

}
