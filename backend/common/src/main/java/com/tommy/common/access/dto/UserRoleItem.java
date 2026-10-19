package com.tommy.common.access.dto;

import java.time.LocalDate;

public record UserRoleItem(
		Long userRolePk,
		String roleCode,
		String roleName,
		LocalDate validFromDate,
		LocalDate validToDate,
		boolean currentlyValid,
		String useYn) {
}
