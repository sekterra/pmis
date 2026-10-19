package com.tommy.common.org.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeptTreeOrder {

	private DeptTreeOrder() {
	}

	public record Node(long deptPk, Long parentDeptPk, String deptCode, Integer sortOrder) {
	}

	public static List<Node> preorder(List<Node> nodes) {
		Map<Long, List<Node>> children = new HashMap<>();
		List<Node> roots = new ArrayList<>();
		List<Long> ids = nodes.stream().map(Node::deptPk).toList();
		for (Node node : nodes) {
			if (node.parentDeptPk() == null || !ids.contains(node.parentDeptPk())) {
				roots.add(node);
			}
			else {
				children.computeIfAbsent(node.parentDeptPk(), key -> new ArrayList<>()).add(node);
			}
		}
		Comparator<Node> comparator = Comparator
				.comparing(Node::sortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(Node::deptCode, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparingLong(Node::deptPk);
		roots.sort(comparator);
		children.values().forEach(group -> group.sort(comparator));
		List<Node> ordered = new ArrayList<>();
		for (Node root : roots) {
			walk(root, children, ordered);
		}
		return ordered;
	}

	private static void walk(Node node, Map<Long, List<Node>> children, List<Node> ordered) {
		ordered.add(node);
		for (Node child : children.getOrDefault(node.deptPk(), List.of())) {
			walk(child, children, ordered);
		}
	}

}
