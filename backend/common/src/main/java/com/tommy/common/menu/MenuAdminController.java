package com.tommy.common.menu;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.global.response.ApiResponse;
import com.tommy.common.global.response.PageResponse;
import com.tommy.common.global.security.RequirePermission;
import com.tommy.common.menu.dto.MenuRequests.MenuCreateRequest;
import com.tommy.common.menu.dto.MenuRequests.MenuSortItem;
import com.tommy.common.menu.dto.MenuRequests.MenuUpdateRequest;
import com.tommy.common.menu.dto.MenuResponse;
import com.tommy.common.menu.dto.MenuTreeItem;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mdm/v1/menus")
public class MenuAdminController {

	private static final Set<String> SORTS = Set.of("menuCode", "menuName", "sortOrder", "levelNo", "menuPk");

	private static final List<Sort.Order> DEFAULT_SORT = List.of(Sort.Order.asc("sortOrder"), Sort.Order.asc("menuCode"),
			Sort.Order.asc("menuPk"));

	private final MenuAdminService menus;

	public MenuAdminController(MenuAdminService menus) {
		this.menus = menus;
	}

	@GetMapping
	@RequirePermission(menu = "MENU", action = "READ")
	public ApiResponse<PageResponse<MenuResponse>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String solutionCode,
			@RequestParam(required = false) String menuTypeCode,
			@RequestParam(required = false) String parentMenuCode,
			@RequestParam(required = false) String visibleYn,
			@RequestParam(required = false) String useYn,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) List<String> sort) {
		return ApiResponse.ok(menus.list(keyword, solutionCode, menuTypeCode, parentMenuCode, visibleYn, useYn,
				PageRequests.parse(page, size, sort, SORTS, DEFAULT_SORT, "menuPk")));
	}

	@GetMapping("/tree")
	@RequirePermission(menu = "MENU", action = "READ")
	public ApiResponse<List<MenuTreeItem>> tree(@RequestParam(required = false) String solutionCode,
			@RequestParam(required = false) Boolean includeInactive) {
		return ApiResponse.ok(menus.tree(solutionCode, Boolean.TRUE.equals(includeInactive)));
	}

	@PutMapping("/sort-orders")
	@RequirePermission(menu = "MENU", action = "UPDATE")
	public ApiResponse<Void> sort(@Valid @RequestBody List<MenuSortItem> items) {
		menus.sort(items);
		return ApiResponse.ok(null);
	}

	@GetMapping("/{menuPk:[0-9]+}")
	@RequirePermission(menu = "MENU", action = "READ")
	public ApiResponse<MenuResponse> get(@PathVariable long menuPk) {
		return ApiResponse.ok(menus.get(menuPk));
	}

	@PostMapping
	@RequirePermission(menu = "MENU", action = "CREATE")
	public ApiResponse<MenuResponse> create(@Valid @RequestBody MenuCreateRequest request) {
		return ApiResponse.ok(menus.create(request));
	}

	@PutMapping("/{menuPk:[0-9]+}")
	@RequirePermission(menu = "MENU", action = "UPDATE")
	public ApiResponse<MenuResponse> update(@PathVariable long menuPk, @Valid @RequestBody MenuUpdateRequest request) {
		return ApiResponse.ok(menus.update(menuPk, request));
	}

	@DeleteMapping("/{menuPk:[0-9]+}")
	@RequirePermission(menu = "MENU", action = "DELETE")
	public ApiResponse<Void> delete(@PathVariable long menuPk) {
		menus.delete(menuPk);
		return ApiResponse.ok(null);
	}

	@PostMapping("/{menuPk:[0-9]+}/restore")
	@RequirePermission(menu = "MENU", action = "UPDATE")
	public ApiResponse<MenuResponse> restore(@PathVariable long menuPk) {
		return ApiResponse.ok(menus.restore(menuPk));
	}

}
