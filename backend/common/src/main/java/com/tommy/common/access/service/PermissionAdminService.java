package com.tommy.common.access.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.global.query.LikeEscape;
import com.tommy.common.access.dto.PermissionRequest;
import com.tommy.common.access.dto.PermissionResponse;
import com.tommy.common.access.dto.PermissionUpdateRequest;
import com.tommy.common.access.entity.Permission;
import com.tommy.common.access.entity.RolePermission;
import com.tommy.common.access.repository.PermissionRepository;
import com.tommy.common.access.repository.RolePermissionRepository;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.menu.entity.Menu;
import com.tommy.common.menu.repository.MenuRepository;
import com.tommy.common.org.service.EntityTextService;
import com.tommy.common.org.service.OrgCodes;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.criteria.Predicate;

@Service
public class PermissionAdminService {

	private static final Logger log = LoggerFactory.getLogger(PermissionAdminService.class);

	public static final String TABLE = "permission";

	public static final String NAME = "permission_name";

	private final PermissionRepository permissions;

	private final RolePermissionRepository grants;

	private final MenuRepository menus;

	private final OrgCodes codes;

	private final EntityTextService texts;

	private final AccessSessions sessions;

	private final CurrentTenantUser currentUser;

	public PermissionAdminService(PermissionRepository permissions, RolePermissionRepository grants, MenuRepository menus,
			OrgCodes codes, EntityTextService texts, AccessSessions sessions, CurrentTenantUser currentUser) {
		this.permissions = permissions;
		this.grants = grants;
		this.menus = menus;
		this.codes = codes;
		this.texts = texts;
		this.sessions = sessions;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public PageResponse<PermissionResponse> list(String menuCode, String actionCode, String keyword, String useYn,
			Pageable pageable) {
		Page<Permission> page = permissions.findAll(filter(menuCode, actionCode, keyword, useYn), menuSort(pageable));
		List<PermissionResponse> items = page.getContent().stream().map(permission -> response(permission, false)).toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public PermissionResponse get(long permissionPk) {
		return response(require(permissionPk), true);
	}

	@Transactional
	public PermissionResponse create(PermissionRequest request) {
		Menu menu = page(request.menuCode());
		codes.require("PERMISSION_ACTION", request.actionCode(), "actionCode");
		if (permissions.existsByMenu_MenuPkAndActionCode(menu.getMenuPk(), request.actionCode())) {
			throw OrgExceptions.of(409, "PERMISSION.DUPLICATE", "이미 있는 권한입니다.");
		}
		Permission permission = permissions.save(Permission.create(menu, request.actionCode(), request.permissionName().trim(),
				request.sortOrder(), OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull()));
		texts.upsert(TABLE, NAME, permission.getPermissionPk(), request.translations());
		return get(permission.getPermissionPk());
	}

	@Transactional
	public PermissionResponse update(long permissionPk, PermissionUpdateRequest request) {
		Permission permission = requireActive(permissionPk);
		permission.change(request.permissionName().trim(), request.sortOrder(), OrgExceptions.trimToNull(request.description()),
				currentUser.pkOrNull());
		texts.upsert(TABLE, NAME, permissionPk, request.translations());
		return get(permissionPk);
	}

	@Transactional
	public void delete(long permissionPk) {
		Permission permission = require(permissionPk);
		if ("READ".equals(permission.getActionCode())) {
			throw OrgExceptions.of(409, "PERMISSION.PROTECTED", "READ 권한은 삭제할 수 없습니다.");
		}
		List<RolePermission> affected = grants.findByPermission_PermissionPkAndUseYn(permissionPk, "Y");
		Long actor = currentUser.pkOrNull();
		for (RolePermission link : affected) {
			link.deactivate(actor);
		}
		permission.deactivate(actor);
		sessions.evictAll();
		log.info("permission deleted permissionPk={} affectedRoleCount={}", permissionPk, affected.size());
	}

	@Transactional
	public PermissionResponse restore(long permissionPk) {
		Permission permission = require(permissionPk);
		page(permission.getMenu().getMenuCode());
		permission.activate(currentUser.pkOrNull());
		return get(permissionPk);
	}

	private Menu page(String menuCode) {
		Menu menu = menus.findByMenuCode(menuCode)
				.orElseThrow(() -> OrgExceptions.field("COMMON.VALIDATION_FAILED", "메뉴를 찾을 수 없습니다.", "menuCode"));
		if (!"Y".equals(menu.getUseYn()) || !"PAGE".equals(menu.getMenuTypeCode())) {
			throw OrgExceptions.field("PERMISSION.MENU_NOT_PAGE", "페이지 메뉴만 권한을 가질 수 있습니다.", "menuCode");
		}
		return menu;
	}

	private Permission require(long permissionPk) {
		return permissions.findById(permissionPk)
				.orElseThrow(() -> OrgExceptions.of(404, "PERMISSION.NOT_FOUND", "권한을 찾을 수 없습니다."));
	}

	private Permission requireActive(long permissionPk) {
		Permission permission = require(permissionPk);
		if (!permission.inUse()) {
			throw OrgExceptions.of(409, "PERMISSION.INACTIVE", "사용 중이 아닌 권한입니다.");
		}
		return permission;
	}

	private PermissionResponse response(Permission permission, boolean detail) {
		return new PermissionResponse(permission.getPermissionPk(), permission.getPermissionCode(),
				permission.getMenu().getMenuCode(), permission.getActionCode(), permission.getPermissionName(),
				permission.getSortOrder(), permission.getDescription(), permission.getUseYn(), permission.getCreateDatetime(),
				permission.getCreateUserPk(), permission.getUpdateDatetime(), permission.getUpdateUserPk(),
				detail ? texts.list(TABLE, NAME, permission.getPermissionPk()) : null);
	}

	private static Pageable menuSort(Pageable pageable) {
		List<Sort.Order> orders = pageable.getSort().stream()
				.map(order -> "menuCode".equals(order.getProperty()) ? order.withProperty("menu.menuCode") : order)
				.toList();
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
	}

	private Specification<Permission> filter(String menuCode, String actionCode, String keyword, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (menuCode != null && !menuCode.isBlank()) {
				predicates.add(builder.equal(root.get("menu").get("menuCode"), menuCode));
			}
			if (actionCode != null && !actionCode.isBlank()) {
				predicates.add(builder.equal(root.get("actionCode"), actionCode));
			}
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				predicates.add(builder.or(LikeEscape.contains(builder, root.get("permissionCode"), text),
						LikeEscape.contains(builder, root.get("permissionName"), text)));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
