package com.tommy.common.access.dto;

import java.time.LocalDateTime;

public record IssuedCredential(Long userPk, String loginId, String temporaryPassword, LocalDateTime passwordExpireDatetime,
		String mustChangePasswordYn) {

	@Override
	public String toString() {
		return "IssuedCredential[userPk=" + userPk + ", loginId=" + loginId + ", temporaryPassword=masked]";
	}

}
