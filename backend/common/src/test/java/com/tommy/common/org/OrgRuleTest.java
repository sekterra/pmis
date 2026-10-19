package com.tommy.common.org;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tommy.common.global.exception.ApiException;
import com.tommy.common.global.paging.PageRequests;
import com.tommy.common.org.support.DeptPaths;
import com.tommy.common.org.support.DeptTreeOrder;
import com.tommy.common.org.support.OrgExceptions;
import com.tommy.common.org.support.PrimaryAssignmentPolicy;

class OrgRuleTest {

	@Test
	void rebuildsDescendantPathsWithoutCrossingSiblingPrefix() {
		assertEquals("/1/9", DeptPaths.replacePrefix("/1/5", "/1/5", "/1/9"));
		assertEquals("/1/9/12", DeptPaths.replacePrefix("/1/5/12", "/1/5", "/1/9"));
		assertEquals("/1/50", DeptPaths.replacePrefix("/1/50", "/1/5", "/1/9"));
		assertEquals("/1/5/12", DeptPaths.path("/1/5", 12));
		assertEquals(3, DeptPaths.level("/1/5/12"));
		assertTrue(DeptPaths.isSelfOrDescendant("/1/5/12", "/1/5"));
		assertFalse(DeptPaths.isSelfOrDescendant("/1/50", "/1/5"));
	}

	@Test
	void ordersSiblingsBySortThenCode() {
		List<DeptTreeOrder.Node> ordered = DeptTreeOrder.preorder(List.of(
				new DeptTreeOrder.Node(2, 1L, "B", 2),
				new DeptTreeOrder.Node(3, 1L, "A", 2),
				new DeptTreeOrder.Node(1, null, "ROOT", 1),
				new DeptTreeOrder.Node(4, 2L, "C", 1)));
		assertEquals(List.of(1L, 3L, 2L, 4L), ordered.stream().map(DeptTreeOrder.Node::deptPk).toList());
	}

	@Test
	void firstCurrentAssignmentIsPrimaryAndLaterPrimaryDemotes() {
		assertTrue(PrimaryAssignmentPolicy.becomesPrimary(false, true, 0));
		assertFalse(PrimaryAssignmentPolicy.demotesExisting(true, true, 0));
		assertTrue(PrimaryAssignmentPolicy.becomesPrimary(true, true, 1));
		assertTrue(PrimaryAssignmentPolicy.demotesExisting(true, true, 1));
		assertFalse(PrimaryAssignmentPolicy.becomesPrimary(false, true, 1));
		assertFalse(PrimaryAssignmentPolicy.demotesExisting(false, true, 1));
		assertTrue(PrimaryAssignmentPolicy.becomesPrimary(true, false, 1));
		assertFalse(PrimaryAssignmentPolicy.demotesExisting(true, false, 1));
	}

	@Test
	void rejectsReversedDatesAndUnknownSort() {
		ApiException dates = assertThrows(ApiException.class,
				() -> OrgExceptions.requireDateOrder(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1),
						"DEPT.INVALID_DATE", "시작일은 종료일보다 늦을 수 없습니다."));
		assertEquals("DEPT.INVALID_DATE", dates.errorCode());
		ApiException sort = assertThrows(ApiException.class,
				() -> PageRequests.parse(1, 20, List.of("unknown,asc"), java.util.Set.of("companyCode", "companyPk"),
						List.of(), "companyPk"));
		assertEquals("COMMON.INVALID_SORT", sort.errorCode());
	}

}
