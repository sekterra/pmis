package com.tommy.common.access.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tommy.common.access.dto.PermissionCodesRequest;
import com.tommy.common.access.dto.PermissionGrantItem;
import com.tommy.common.access.dto.RoleRequest;
import com.tommy.common.access.dto.RoleResponse;
import com.tommy.common.access.dto.RoleUpdateRequest;
import com.tommy.common.access.entity.Permission;
import com.tommy.common.access.entity.Role;
import com.tommy.common.access.entity.RolePermission;
import com.tommy.common.access.repository.PermissionRepository;
import com.tommy.common.access.repository.RolePermissionRepository;
import com.tommy.common.access.repository.RoleRepository;
import com.tommy.common.access.repository.UserRoleRepository;
import com.tommy.common.auth.PermissionRules;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.CurrentTenantUser;
import com.tommy.common.global.time.TenantClock;
import com.tommy.common.i18n.service.BundleService;
import com.tommy.common.org.service.EntityTextService;
import com.tommy.common.org.support.OrgExceptions;

import jakarta.persistence.criteria.Predicate;

@Service
public class RoleAdminService {

	public static final String TABLE = "role";

	public static final String NAME = "role_name";

	private final RoleRepository roles;

	private final UserRoleRepository userRoles;

	private final PermissionRepository permissions;

	private final RolePermissionRepository grants;

	private final EntityTextService texts;

	private final BundleService bundles;

	private final AccessSessions sessions;

	private final CurrentTenantUser currentUser;

	private final TenantClock clock;

	public RoleAdminService(RoleRepository roles, UserRoleRepository userRoles, PermissionRepository permissions,
			RolePermissionRepository grants, EntityTextService texts, BundleService bundles, AccessSessions sessions,
			CurrentTenantUser currentUser, TenantClock clock) {
		this.roles = roles;
		this.userRoles = userRoles;
		this.permissions = permissions;
		this.grants = grants;
		this.texts = texts;
		this.bundles = bundles;
		this.sessions = sessions;
		this.currentUser = currentUser;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PageResponse<RoleResponse> list(String keyword, String useYn, Pageable pageable) {
		Page<Role> page = roles.findAll(filter(keyword, useYn), pageable);
		Map<Long, Long> counts = counts(page.getContent());
		List<RoleResponse> items = page.getContent().stream()
				.map(role -> response(role, false, counts.getOrDefault(role.getRolePk(), 0L)))
				.toList();
		return new PageResponse<>(items, page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
	}

	@Transactional(readOnly = true)
	public RoleResponse get(long rolePk) {
		return response(require(rolePk), true, userCount(rolePk));
	}

	@Transactional
	public RoleResponse create(RoleRequest request) {
		if (roles.existsByRoleCode(request.roleCode())) {
			throw OrgExceptions.of(409, "ROLE.DUPLICATE", "이미 있는 역할 코드입니다.");
		}
		Role role = roles.save(Role.create(request.roleCode(), request.roleName().trim(), request.sortOrder(),
				OrgExceptions.trimToNull(request.description()), currentUser.pkOrNull()));
		texts.upsert(TABLE, NAME, role.getRolePk(), request.translations());
		return get(role.getRolePk());
	}

	@Transactional
	public RoleResponse update(long rolePk, RoleUpdateRequest request) {
		Role role = requireActive(rolePk);
		role.change(request.roleName().trim(), request.sortOrder(), OrgExceptions.trimToNull(request.description()),
				currentUser.pkOrNull());
		texts.upsert(TABLE, NAME, rolePk, request.translations());
		return get(rolePk);
	}

	@Transactional
	public void delete(long rolePk) {
		Role role = require(rolePk);
		if ("TENANT_ADMIN".equals(role.getRoleCode())) {
			throw OrgExceptions.of(409, "ROLE.PROTECTED", "보호된 역할입니다.");
		}
		if (userRoles.existsByRole_RolePkAndUseYn(rolePk, "Y")) {
			throw OrgExceptions.of(409, "ROLE.IN_USE", "부여 중인 역할입니다.");
		}
		role.deactivate(currentUser.pkOrNull());
	}

	@Transactional
	public RoleResponse restore(long rolePk) {
		Role role = require(rolePk);
		role.activate(currentUser.pkOrNull());
		return get(rolePk);
	}

	@Transactional(readOnly = true)
	public List<PermissionGrantItem> permissions(long rolePk, Boolean granted, String lang, String acceptLanguage) {
		Role role = require(rolePk);
		Set<Long> grantedPks = new HashSet<>();
		for (RolePermission link : grants.findByRole_RolePk(role.getRolePk())) {
			if ("Y".equals(link.getUseYn())) {
				grantedPks.add(link.getPermission().getPermissionPk());
			}
		}
		return permissions.findByUseYnOrderByMenu_SortOrderAscSortOrderAscPermissionPkAsc("Y").stream()
				.map(permission -> item(permission, grantedPks.contains(permission.getPermissionPk()), lang, acceptLanguage))
				.filter(item -> granted == null || !granted || item.granted())
				.toList();
	}

	@Transactional
	public List<PermissionGrantItem> replacePermissions(long rolePk, PermissionCodesRequest request, String lang,
			String acceptLanguage) {
		Role role = requireActive(rolePk);
		if ("TENANT_ADMIN".equals(role.getRoleCode())) {
			throw OrgExceptions.of(409, "ROLE.PROTECTED", "보호된 역할입니다.");
		}
		List<String> codes = request == null || request.permissionCodes() == null ? List.of() : request.permissionCodes();
		Set<String> seen = new HashSet<>();
		Map<String, Permission> selected = new HashMap<>();
		for (String code : codes) {
			if (!seen.add(code)) {
				throw OrgExceptions.field("COMMON.VALIDATION_FAILED", "같은 권한이 두 번 있습니다.", "permissionCodes");
			}
			Permission permission = permissions.findByPermissionCode(code)
					.filter(Permission::inUse)
					.orElseThrow(() -> OrgExceptions.field(404, "PERMISSION.NOT_FOUND", "권한을 찾을 수 없습니다.", "permissionCodes"));
			selected.put(code, permission);
		}
		Map<String, Boolean> readByMenu = new HashMap<>();
		for (Permission permission : selected.values()) {
			if ("READ".equals(permission.getActionCode())) {
				readByMenu.put(permission.getMenu().getMenuCode(), true);
			}
		}
		for (Permission permission : selected.values()) {
			try {
				PermissionRules.requireReadWhenGrantingOther(readByMenu.getOrDefault(permission.getMenu().getMenuCode(), false),
						permission.getActionCode());
			}
			catch (IllegalArgumentException ex) {
				throw OrgExceptions.field("ROLE.READ_REQUIRED", "같은 메뉴의 READ 권한이 필요합니다.", permission.getMenu().getMenuCode());
			}
		}
		Long actor = currentUser.pkOrNull();
		List<RolePermission> existing = grants.findByRole_RolePk(rolePk);
		Map<String, RolePermission> byCode = new HashMap<>();
		for (RolePermission link : existing) {
			byCode.put(link.getPermission().getPermissionCode(), link);
		}
		for (Permission permission : selected.values()) {
			RolePermission link = byCode.get(permission.getPermissionCode());
			if (link == null) {
				grants.save(RolePermission.grant(role, permission, actor));
			}
			else {
				link.activate(actor);
			}
		}
		for (RolePermission link : existing) {
			if ("Y".equals(link.getUseYn()) && !selected.containsKey(link.getPermission().getPermissionCode())) {
				link.deactivate(actor);
			}
		}
		sessions.evictUsers(userRoles.findActiveUserPks(rolePk));
		return permissions(rolePk, null, lang, acceptLanguage);
	}

	private PermissionGrantItem item(Permission permission, boolean granted, String lang, String acceptLanguage) {
		String menuCode = permission.getMenu().getMenuCode();
		String translated = bundles.findQuietly("menu", menuCode, lang, acceptLanguage);
		return new PermissionGrantItem(permission.getPermissionPk(), permission.getPermissionCode(), menuCode,
				translated == null || translated.isBlank() ? permission.getMenu().getMenuName() : translated,
				permission.getActionCode(), permission.getPermissionName(), granted);
	}

	private Map<Long, Long> counts(List<Role> page) {
		if (page.isEmpty()) {
			return Map.of();
		}
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : roles.countCurrentUsers(page.stream().map(Role::getRolePk).toList(), clock.today())) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return counts;
	}

	private long userCount(long rolePk) {
		return counts(List.of(require(rolePk))).getOrDefault(rolePk, 0L);
	}

	private Role require(long rolePk) {
		return roles.findById(rolePk).orElseThrow(() -> OrgExceptions.of(404, "ROLE.NOT_FOUND", "역할을 찾을 수 없습니다."));
	}

	private Role requireActive(long rolePk) {
		Role role = require(rolePk);
		if (!role.inUse()) {
			throw OrgExceptions.of(409, "ROLE.INACTIVE", "사용 중이 아닌 역할입니다.");
		}
		return role;
	}

	private RoleResponse response(Role role, boolean detail, long userCount) {
		return new RoleResponse(role.getRolePk(), role.getRoleCode(), role.getRoleName(), role.getSortOrder(), role.getDescription(),
				role.getUseYn(), userCount, role.getCreateDatetime(), role.getCreateUserPk(), role.getUpdateDatetime(),
				role.getUpdateUserPk(), detail ? texts.list(TABLE, NAME, role.getRolePk()) : null);
	}

	private Specification<Role> filter(String keyword, String useYn) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			String text = OrgExceptions.trimToNull(keyword);
			if (text != null) {
				String pattern = "%" + text.toLowerCase(Locale.ROOT) + "%";
				predicates.add(builder.or(builder.like(builder.lower(root.get("roleCode")), pattern),
						builder.like(builder.lower(root.get("roleName")), pattern)));
			}
			if (useYn != null && !useYn.isBlank()) {
				predicates.add(builder.equal(root.get("useYn"), useYn));
			}
			return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(Predicate[]::new));
		};
	}

}
