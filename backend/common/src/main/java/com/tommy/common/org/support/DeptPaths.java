package com.tommy.common.org.support;

public final class DeptPaths {

	private DeptPaths() {
	}

	public static String path(String parentPath, long deptPk) {
		String prefix = parentPath == null || parentPath.isBlank() ? "" : parentPath;
		return prefix + "/" + deptPk;
	}

	public static int level(String path) {
		if (path == null || path.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (int index = 0; index < path.length(); index++) {
			if (path.charAt(index) == '/') {
				count++;
			}
		}
		return count;
	}

	public static boolean isSelfOrDescendant(String path, String ancestorPath) {
		return path != null && ancestorPath != null
				&& (path.equals(ancestorPath) || path.startsWith(ancestorPath + "/"));
	}

	public static String replacePrefix(String path, String oldPrefix, String newPrefix) {
		if (path.equals(oldPrefix)) {
			return newPrefix;
		}
		if (path.startsWith(oldPrefix + "/")) {
			return newPrefix + path.substring(oldPrefix.length());
		}
		return path;
	}

}
