package com.tommy.common.global.multitenancy;

import java.util.Optional;

import com.tommy.common.global.exception.ApiException;

public final class TenantRequestIdentifier {

	private TenantRequestIdentifier() {
	}

	public static String identify(Optional<String> hostCode, Optional<String> jwtCode, Optional<String> headerCode) {
		boolean host = hostCode != null && hostCode.isPresent();
		boolean jwt = jwtCode != null && jwtCode.isPresent();
		boolean header = headerCode != null && headerCode.isPresent();
		if (jwt) {
			String tenant = jwtCode.get();
			if (host && !tenant.equals(hostCode.get())) {
				throw mismatch();
			}
			if (header && !tenant.equals(headerCode.get())) {
				throw mismatch();
			}
			return tenant;
		}
		if (header) {
			return headerCode.get();
		}
		if (host) {
			return hostCode.get();
		}
		throw new ApiException(400, "TENANT.UNRESOLVED", "테넌트를 식별할 수 없습니다.", 2);
	}

	private static ApiException mismatch() {
		return new ApiException(400, "TENANT.HOST_MISMATCH", "요청 도메인과 토큰의 테넌트가 다릅니다.", 2);
	}

}
