package com.tommy.common.menu;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.security.DbPermissionChecker;
import com.tommy.common.i18n.service.BundleService;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true)
public class MyMenuService {

	private static final String MENU_NAMESPACE = "menu";

	private final EntityManager entityManager;

	private final DbPermissionChecker permissions;

	private final BundleService bundles;

	public MyMenuService(EntityManager entityManager, DbPermissionChecker permissions, BundleService bundles) {
		this.entityManager = entityManager;
		this.permissions = permissions;
		this.bundles = bundles;
	}

	public List<MyMenuItem> my(String lang, String acceptLanguage) {
		List<MyMenuItem> visible = MyMenuRules.visible(loadRows(), permissions.actionsByMenu());
		List<MyMenuItem> named = new ArrayList<>();
		for (MyMenuItem item : visible) {
			String translated = bundles.findQuietly(MENU_NAMESPACE, item.menuCode(), lang, acceptLanguage);
			String menuName = translated == null || translated.isBlank() ? item.menuName() : translated;
			named.add(new MyMenuItem(item.menuCode(), item.parentMenuCode(), menuName, item.menuTypeCode(), item.routePath(),
					item.iconName(), item.levelNo(), item.sortOrder(), item.actions()));
		}
		return List.copyOf(named);
	}

	@SuppressWarnings("unchecked")
	private List<MyMenuRules.Row> loadRows() {
		List<Object[]> rows = entityManager.createNativeQuery("""
				SELECT m.menu_code, parent.menu_code, m.menu_name, m.menu_type_code, m.route_path, m.icon_name,
				       m.level_no, m.sort_order, m.visible_yn
				FROM menu m
				LEFT JOIN menu parent ON parent.menu_pk = m.parent_menu_pk
				WHERE m.use_yn = 'Y'
				""").getResultList();
		List<MyMenuRules.Row> result = new ArrayList<>();
		for (Object[] row : rows) {
			result.add(new MyMenuRules.Row(text(row[0]), text(row[1]), text(row[2]), text(row[3]), text(row[4]), text(row[5]),
					number(row[6]), number(row[7]), true, "Y".equals(text(row[8]))));
		}
		return result;
	}

	private static String text(Object value) {
		if (value == null) {
			return null;
		}
		String text = value.toString().trim();
		return text.isEmpty() ? null : text;
	}

	private static Integer number(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.valueOf(value.toString().trim());
	}

}
