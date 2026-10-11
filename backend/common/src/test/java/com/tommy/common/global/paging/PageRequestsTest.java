package com.tommy.common.global.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.tommy.common.global.exception.ApiException;

class PageRequestsTest {

	@Test
	void rejectsInvalidPageAndSortAndKeepsPrimaryKeyLast() {
		Set<String> allowed = Set.of("codeValue", "codePk");
		List<Sort.Order> defaults = List.of(Sort.Order.asc("codeValue"), Sort.Order.asc("codePk"));
		ApiException page = assertThrows(ApiException.class,
				() -> PageRequests.parse(0, 20, List.of(), allowed, defaults, "codePk"));
		assertEquals("COMMON.INVALID_PAGE", page.errorCode());
		ApiException size = assertThrows(ApiException.class,
				() -> PageRequests.parse(1, 201, List.of(), allowed, defaults, "codePk"));
		assertEquals("COMMON.INVALID_PAGE", size.errorCode());
		ApiException sort = assertThrows(ApiException.class,
				() -> PageRequests.parse(1, 20, List.of("unknown,asc"), allowed, defaults, "codePk"));
		assertEquals("COMMON.INVALID_SORT", sort.errorCode());
		Pageable pageable = PageRequests.parse(2, null, List.of("codeValue,desc"), allowed, defaults, "codePk");
		assertEquals(1, pageable.getPageNumber());
		assertEquals(20, pageable.getPageSize());
		assertEquals(List.of(Sort.Order.desc("codeValue"), Sort.Order.asc("codePk")), pageable.getSort().toList());
	}

}
