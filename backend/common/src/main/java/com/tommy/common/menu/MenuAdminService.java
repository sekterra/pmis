package com.tommy.common.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.query.LikeEscape;
import com.tommy.common.access.entity.Permission;
import com.tommy.common.access.entity.RolePermission;
import com.tommy.common.access.repository.PermissionRepository;
import com.tommy.common.access.repository.RolePermissionRepository;
import com.tommy.common.access.service.AccessSessions;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.i18n.service.BundleService;
import com.tommy.common.i18n.service.I18nMessageService;
import com.tommy.common.menu.dto.MenuRequests.MenuCreateRequest;
import com.tommy.common.menu.dto.MenuRequests.MenuSortItem;
import com.tommy.common.menu.dto.MenuRequests.MenuUpdateRequest;
import com.tommy.common.menu.dto.MenuResponse;
import com.tommy.common.menu.dto.MenuResponse.PermissionSummary;
import com.tommy.common.menu.dto.MenuTreeItem;
import com.tommy.common.menu.entity.Menu;
import com.tommy.common.menu.repository.MenuRepository;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

@Service
public class MenuAdminService {

	private static final Logger log = LoggerFactory.getLogger(MenuAdminService.class);

	private static final List<String[]> ACTIONS = List.of(
			new String[] { "READ", "조회", "1" },
			new String[] { "CREATE", "등록", "2" },
			new String[] { "UPDATE", "수정", "3" },
			new String[] { "DELETE", "삭제", "4" },
			new String[] { "EXPORT", "내보내기", "5" });

	private final MenuRepository menus;

	private final PermissionRepository permissions;

	private final RolePermissionRepository grants;

	private final I18nMessageService messages;

	private final BundleService bundles;

	private final AccessSessions sessions;

	private final CurrentTenantUser currentUser;

	private final EntityManager entityManager;

	public MenuAdminService(MenuRepository menus, PermissionRepository permissions, RolePermissionRepository grants,
			I18nMessageService messages, BundleService bundles, AccessSessions sessions, CurrentTenantUser currentUser,
			EntityManager entityManager) {
		this.menus = menus;
		this.permissions = permissions;
		this.grants = grants;
		this.messages = messages;
		this.bundles = bundles;
		this.sessions = sessions;
		this.currentUser = currentUser;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public PageResponse<MenuResponse> list(String keyword, String solutionCode, String menuTypeCode, String parentMenuCode,
			String visibleYn, String useYn, Pageable pageable) {
		Page<Menu> page = menus.findAll(filter(keyword, solutionCode, menuTypeCode, parentMenuCode, visibleYn, useYn), pageable);
		Map<Long, String> parents = parentCodes(page.getContent());
		List<MenuResponse> items = page.getContent().stream().map(menu -> response(menu, parents.get(menu.getParentMenuPk()), false))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public List<MenuTreeItem> tree(String solutionCode, boolean includeInactive) {
		List<Menu> rows = menus.findAll();
		Map<Long, Long> counts = permissionCounts();
		List<MenuTree.Node> nodes = new ArrayList<>();
		for (Menu menu : rows) {
			if (solutionCode != null && !solutionCode.isBlank() && !solutionCode.equals(menu.getSolutionCode())) {
				continue;
			}
			if (!includeInactive && !menu.inUse()) {
				continue;
			}
			nodes.add(new MenuTree.Node(menu.getMenuPk(), menu.getParentMenuPk(), menu.getMenuCode(), menu.getMenuName(),
					menu.getMenuTypeCode(), menu.getLevelNo() == null ? 1 : menu.getLevelNo(), menu.getSortOrder(),
					counts.getOrDefault(menu.getMenuPk(), 0L).intValue()));
		}
		return flatten(MenuTree.assemble(nodes));
	}

	@Transactional(readOnly = true)
	public MenuResponse get(long menuPk) {
		Menu menu = require(menuPk);
		return response(menu, parentCode(menu.getParentMenuPk()), true);
	}

	@Transactional
	public MenuResponse create(MenuCreateRequest request) {
		String solution = requiredCode(request.solutionCode(), "solutionCode", MenuRules::solution);
		String code = requiredCode(request.menuCode(), "menuCode", MenuRules::menuCode);
		if (menus.findByMenuCode(code).isPresent()) {
			throw OrgExceptions.of(409, "MENU.DUPLICATE", "이미 있는 메뉴 코드입니다.");
		}
		String type = type(request.menuTypeCode());
		Menu parent = parent(request.parentMenuCode(), null, solution);
		int level = parent == null ? 1 : parent.getLevelNo() + 1;
		if (MenuRules.tooDeep(level)) {
			throw OrgExceptions.of(400, "MENU.TOO_DEEP", "메뉴 깊이를 넘었습니다.");
		}
		String route = route(type, request.routePath(), null);
		String icon = icon(request.iconName());
		Long actor = currentUser.pkOrNull();
		Menu menu = menus.save(Menu.create(solution, code, request.menuName().trim(), type, parent == null ? null : parent.getMenuPk(),
				route, icon, level, MenuRules.path(parent == null ? null : parent.getMenuPath(), code),
				request.sortOrder() == null ? 0 : request.sortOrder(), yn(request.visibleYn(), "Y"),
				OrgExceptions.trimToNull(request.description()), actor));
		if ("PAGE".equals(type)) {
			seedPermissions(menu, actor);
		}
		if (request.translations() != null) {
			messages.upsertMenuName(code, menu.getMenuName(), request.translations());
		}
		return get(menu.getMenuPk());
	}

	@Transactional
	public MenuResponse update(long menuPk, MenuUpdateRequest request) {
		if (request.menuCode() != null || request.solutionCode() != null || request.menuTypeCode() != null) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "바꿀 수 없는 항목입니다.",
					request.menuCode() != null ? "menuCode" : request.solutionCode() != null ? "solutionCode" : "menuTypeCode");
		}
		Menu menu = requireActive(menuPk);
		String currentParent = parentCode(menu.getParentMenuPk());
		String requestedParent = OrgExceptions.trimToNull(request.parentMenuCode());
		boolean sameParent = java.util.Objects.equals(requestedParent, currentParent);
		Menu parent = sameParent ? parentOf(menu) : parent(request.parentMenuCode(), menu, menu.getSolutionCode());
		if (!sameParent && parent != null && MenuRules.cycle(menu.getMenuPath(), parent.getMenuPath())) {
			throw OrgExceptions.of(409, "MENU.PARENT_CYCLE", "자기 자신이나 자손 아래로는 옮길 수 없습니다.");
		}
		int level = parent == null ? 1 : parent.getLevelNo() + 1;
		String route = route(menu.getMenuTypeCode(), request.routePath(), menu.getMenuPk());
		String icon = icon(request.iconName());
		Long actor = currentUser.pkOrNull();
		String oldPath = menu.getMenuPath();
		int oldLevel = menu.getLevelNo() == null ? 1 : menu.getLevelNo();
		menu.change(request.menuName().trim(), route, icon, request.sortOrder() == null ? menu.getSortOrder() : request.sortOrder(),
				yn(request.visibleYn(), menu.getVisibleYn()), OrgExceptions.trimToNull(request.description()), actor);
		if (!sameParent) {
			String newPath = MenuRules.path(parent == null ? null : parent.getMenuPath(), menu.getMenuCode());
			int delta = level - oldLevel;
			assertDepth(menu.getMenuPk(), oldPath, delta);
			menu.place(parent == null ? null : parent.getMenuPk(), level, newPath, actor);
			moveDescendants(oldPath, newPath, delta, actor);
		}
		messages.upsertMenuName(menu.getMenuCode(), menu.getMenuName(), request.translations());
		evict();
		return get(menuPk);
	}

	@Transactional
	public void sort(List<MenuSortItem> items) {
		if (items == null || items.isEmpty() || items.size() > 200) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "정렬 항목은 1개 이상 200개 이하여야 합니다.", "items");
		}
		Long actor = currentUser.pkOrNull();
		List<Menu> found = new ArrayList<>();
		for (MenuSortItem item : items) {
			if (item.sortOrder() == null) {
				throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "정렬 순서가 필요합니다.", "sortOrder");
			}
			Menu menu = menus.findByMenuCode(item.menuCode())
					.orElseThrow(() -> OrgExceptions.of(404, "MENU.NOT_FOUND", "메뉴를 찾을 수 없습니다."));
			found.add(menu);
			menu.sort(item.sortOrder(), actor);
		}
		menus.saveAll(found);
		evict();
	}

	@Transactional
	public void delete(long menuPk) {
		Menu menu = requireActive(menuPk);
		if (menus.existsByParentMenuPkAndUseYn(menuPk, "Y")) {
			throw OrgExceptions.of(409, "MENU.HAS_CHILD", "사용 중인 하위 메뉴가 있습니다.");
		}
		if (SeedMenuCodes.protects(menu.getMenuCode(), menu.getSolutionCode())) {
			throw OrgExceptions.of(409, "MENU.PROTECTED", "보호된 메뉴입니다.");
		}
		Long actor = currentUser.pkOrNull();
		if ("PAGE".equals(menu.getMenuTypeCode())) {
			for (Permission permission : permissions.findByMenu_MenuPk(menuPk)) {
				for (RolePermission link : grants.findByPermission_PermissionPkAndUseYn(permission.getPermissionPk(), "Y")) {
					link.deactivate(actor);
				}
				if (permission.inUse()) {
					permission.deactivate(actor);
				}
			}
		}
		menu.deactivate(actor);
		evict();
	}

	@Transactional
	public MenuResponse restore(long menuPk) {
		Menu menu = require(menuPk);
		Menu parent = parentOf(menu);
		if (parent != null && !parent.inUse()) {
			throw OrgExceptions.of(409, "MENU.PARENT_INACTIVE", "상위 메뉴가 사용 중이 아닙니다.");
		}
		menu.activate(currentUser.pkOrNull());
		evict();
		return get(menuPk);
	}

	private void seedPermissions(Menu menu, Long actor) {
		for (String[] action : ACTIONS) {
			if (permissions.existsByMenu_MenuPkAndActionCode(menu.getMenuPk(), action[0])) {
				log.info("menu permission skipped menuCode={} action={}", menu.getMenuCode(), action[0]);
				continue;
			}
			permissions.save(Permission.create(menu, action[0], menu.getMenuName() + " " + action[1], Integer.valueOf(action[2]), null,
					actor));
		}
	}

	private void assertDepth(long menuPk, String oldPath, int delta) {
		String prefix = oldPath + "/";
		Number max = (Number) entityManager.createNativeQuery("""
				SELECT COALESCE(MAX(level_no), 1) FROM menu
				WHERE menu_pk = ? OR left(menu_path, char_length(?)) = ?
				""").setParameter(1, menuPk).setParameter(2, prefix).setParameter(3, prefix).getSingleResult();
		if (MenuRules.tooDeep(max.intValue() + delta)) {
			throw OrgExceptions.of(400, "MENU.TOO_DEEP", "메뉴 깊이를 넘었습니다.");
		}
	}

	private void moveDescendants(String oldPath, String newPath, int delta, Long actor) {
		entityManager.createNativeQuery("""
				UPDATE menu
				SET menu_path = ? || substring(menu_path from char_length(?) + 1),
				    level_no = level_no + ?,
				    update_datetime = CURRENT_TIMESTAMP,
				    update_user_pk = ?
				WHERE left(menu_path, char_length(?)) = ?
				""").setParameter(1, newPath).setParameter(2, oldPath).setParameter(3, delta).setParameter(4, actor)
				.setParameter(5, oldPath + "/").setParameter(6, oldPath + "/").executeUpdate();
		entityManager.flush();
		entityManager.clear();
	}

	private void evict() {
		sessions.evictAll();
		bundles.evictNamespace("menu");
	}

	private Menu parent(String parentMenuCode, Menu self, String solution) {
		String code = OrgExceptions.trimToNull(parentMenuCode);
		if (code == null) {
			return null;
		}
		Menu parent = menus.findByMenuCode(code)
				.orElseThrow(() -> OrgExceptions.field(400, "MENU.PARENT_NOT_GROUP", "상위 메뉴는 사용 중인 그룹이어야 합니다.", "parentMenuCode"));
		if (!parent.inUse() || !"GROUP".equals(parent.getMenuTypeCode()) || (self != null && parent.getMenuPk().equals(self.getMenuPk()))) {
			throw OrgExceptions.field(400, "MENU.PARENT_NOT_GROUP", "상위 메뉴는 사용 중인 그룹이어야 합니다.", "parentMenuCode");
		}
		if (!solution.equals(parent.getSolutionCode())) {
			throw OrgExceptions.field(400, "MENU.PARENT_NOT_GROUP", "상위 메뉴는 같은 솔루션의 그룹이어야 합니다.", "parentMenuCode");
		}
		return parent;
	}

	private Menu parentOf(Menu menu) {
		return menu.getParentMenuPk() == null ? null : menus.findById(menu.getParentMenuPk()).orElse(null);
	}

	private String route(String type, String raw, Long menuPk) {
		String route = OrgExceptions.trimToNull(raw);
		if ("PAGE".equals(type) && (route == null || !MenuRules.route(route))) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "페이지 경로 형식이 올바르지 않습니다.", "routePath");
		}
		if (route != null && !MenuRules.route(route)) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "페이지 경로 형식이 올바르지 않습니다.", "routePath");
		}
		if ("PAGE".equals(type) && menus.routeTaken(route, menuPk)) {
			throw OrgExceptions.of(409, "MENU.DUPLICATE_ROUTE", "이미 있는 화면 경로입니다.");
		}
		return route;
	}

	private static String icon(String raw) {
		String icon = OrgExceptions.trimToNull(raw);
		if (!MenuRules.icon(icon) || (icon != null && icon.length() > 100)) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "아이콘 이름 형식이 올바르지 않습니다.", "iconName");
		}
		return icon;
	}

	private static String type(String raw) {
		if (!"GROUP".equals(raw) && !"PAGE".equals(raw)) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "메뉴 유형은 GROUP 또는 PAGE입니다.", "menuTypeCode");
		}
		return raw;
	}

	private static String requiredCode(String raw, String field, java.util.function.Predicate<String> rule) {
		String value = raw == null ? null : raw.trim();
		if (!rule.test(value)) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "형식이 올바르지 않습니다.", field);
		}
		return value;
	}

	private static String yn(String raw, String fallback) {
		String value = raw == null || raw.isBlank() ? fallback : raw;
		if (!"Y".equals(value) && !"N".equals(value)) {
			throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "여부는 Y 또는 N입니다.", "visibleYn");
		}
		return value;
	}

	private Menu require(long menuPk) {
		return menus.findById(menuPk).orElseThrow(() -> OrgExceptions.of(404, "MENU.NOT_FOUND", "메뉴를 찾을 수 없습니다."));
	}

	private Menu requireActive(long menuPk) {
		Menu menu = require(menuPk);
		if (!menu.inUse()) {
			throw OrgExceptions.of(404, "MENU.NOT_FOUND", "메뉴를 찾을 수 없습니다.");
		}
		return menu;
	}

	private String parentCode(Long parentMenuPk) {
		if (parentMenuPk == null) {
			return null;
		}
		return menus.findById(parentMenuPk).map(Menu::getMenuCode).orElse(null);
	}

	private Map<Long, String> parentCodes(List<Menu> rows) {
		Map<Long, String> codes = new HashMap<>();
		for (Menu menu : rows) {
			if (menu.getParentMenuPk() != null) {
				codes.put(menu.getParentMenuPk(), parentCode(menu.getParentMenuPk()));
			}
		}
		return codes;
	}

	private Map<Long, Long> permissionCounts() {
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : permissions.countInUseByMenu()) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return counts;
	}

	private MenuResponse response(Menu menu, String parentCode, boolean detail) {
		List<PermissionSummary> permissionItems = detail
				? permissions.findByMenu_MenuPk(menu.getMenuPk()).stream()
						.map(permission -> new PermissionSummary(permission.getPermissionCode(), permission.getActionCode(),
								permission.getUseYn()))
						.toList()
				: null;
		return new MenuResponse(menu.getMenuPk(), menu.getParentMenuPk(), parentCode, menu.getSolutionCode(), menu.getMenuCode(),
				menu.getMenuName(), menu.getMenuTypeCode(), menu.getRoutePath(), menu.getIconName(), menu.getLevelNo(),
				menu.getMenuPath(), menu.getSortOrder(), menu.getVisibleYn(), menu.getUseYn(), detail ? menu.getDescription() : null,
				menu.getCreateDatetime(), menu.getCreateUserPk(), menu.getUpdateDatetime(), menu.getUpdateUserPk(),
				detail ? messages.menuTexts(menu.getMenuCode()) : null, permissionItems);
	}

	private List<MenuTreeItem> flatten(List<MenuTree.Branch> branches) {
		List<MenuTreeItem> items = new ArrayList<>();
		for (MenuTree.Branch branch : branches) {
			items.add(item(branch));
		}
		return items;
	}

	private MenuTreeItem item(MenuTree.Branch branch) {
		return new MenuTreeItem(branch.node().menuPk(), branch.node().menuCode(), branch.node().menuName(),
				branch.node().menuTypeCode(), branch.node().levelNo(), branch.node().sortOrder(), branch.childCount(),
				branch.node().permissionCount(), flatten(branch.children()));
	}

	private Specification<Menu> filter(String keyword, String solutionCode, String menuTypeCode, String parentMenuCode,
			String visibleYn, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				predicates.add(builder.or(LikeEscape.contains(builder, root.get("menuCode"), text),
						LikeEscape.contains(builder, root.get("menuName"), text)));
			}
			if (solutionCode != null && !solutionCode.isBlank()) {
				predicates.add(builder.equal(root.get("solutionCode"), solutionCode));
			}
			if (menuTypeCode != null && !menuTypeCode.isBlank()) {
				predicates.add(builder.equal(root.get("menuTypeCode"), menuTypeCode));
			}
			if (parentMenuCode != null && !parentMenuCode.isBlank()) {
				Menu parent = menus.findByMenuCode(parentMenuCode).orElse(null);
				predicates.add(builder.equal(root.get("parentMenuPk"), parent == null ? -1L : parent.getMenuPk()));
			}
			if (visibleYn != null && !visibleYn.isBlank()) {
				predicates.add(builder.equal(root.get("visibleYn"), visibleYn));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
