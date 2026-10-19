package com.tommy.common.access.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
		Long userPk,
		String loginId,
		String accountTypeCode,
		String userStatusCode,
		Long personPk,
		String personName,
		LocalDateTime lastLoginDatetime,
		String mustChangePasswordYn,
		List<String> roleCodes,
		String useYn,
		String supportLanguageCode,
		String timezoneId,
		String description,
		Integer loginFailCount,
		LocalDateTime lockedDatetime,
		String lockType,
		LocalDateTime passwordChangedDatetime,
		LocalDateTime passwordExpireDatetime,
		LocalDateTime createDatetime,
		Long createUserPk,
		LocalDateTime updateDatetime,
		Long updateUserPk,
		List<UserRoleItem> roles) {
}
