package com.tommy.common.global.paging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.tommy.common.global.exception.ApiException;

public final class PageRequests {

	public static final int DEFAULT_PAGE = 1;

	public static final int DEFAULT_SIZE = 20;

	public static final int MAX_SIZE = 200;

	private PageRequests() {
	}

	public static Pageable parse(Integer page, Integer size, List<String> sorts, Set<String> allowed,
			List<Sort.Order> defaults, String primaryKey) {
		int pageNumber = page == null ? DEFAULT_PAGE : page;
		int pageSize = size == null ? DEFAULT_SIZE : size;
		if (pageNumber < 1 || pageSize < 1 || pageSize > MAX_SIZE) {
			throw new ApiException(400, "COMMON.INVALID_PAGE", "페이지 값이 올바르지 않습니다.", 2);
		}
		List<Sort.Order> orders = new ArrayList<>();
		if (sorts == null || sorts.isEmpty()) {
			orders.addAll(defaults);
		}
		else {
			for (String raw : sorts) {
				orders.add(order(raw, allowed));
			}
		}
		if (orders.stream().noneMatch(order -> primaryKey.equals(order.getProperty()))) {
			orders.add(Sort.Order.asc(primaryKey));
		}
		return PageRequest.of(pageNumber - 1, pageSize, Sort.by(orders));
	}

	private static Sort.Order order(String raw, Set<String> allowed) {
		if (raw == null || raw.isBlank()) {
			throw invalidSort();
		}
		String[] parts = raw.split(",", -1);
		if (parts.length > 2 || parts[0].isBlank()) {
			throw invalidSort();
		}
		String field = parts[0].trim();
		String direction = parts.length == 2 ? parts[1].trim() : "asc";
		if (!allowed.contains(field) || (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction))) {
			throw invalidSort();
		}
		return new Sort.Order(Sort.Direction.fromString(direction), field);
	}

	private static ApiException invalidSort() {
		return new ApiException(400, "COMMON.INVALID_SORT", "정렬 필드가 올바르지 않습니다.", 2);
	}

}
