package com.tommy.common.global.security;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MdmSecurityGuard {

	private static final Logger log = LoggerFactory.getLogger(MdmSecurityGuard.class);

	public MdmSecurityGuard(MdmSecurityProperties properties, Environment environment) {
		check(properties, environment.getActiveProfiles());
	}

	static void check(MdmSecurityProperties properties, String[] profiles) {
		boolean dev = Arrays.stream(profiles).anyMatch("dev"::equals);
		boolean devOrTest = dev || Arrays.stream(profiles).anyMatch("test"::equals);
		if (!devOrTest && !properties.getCookie().isSecure()) {
			throw new IllegalStateException("dev/test 프로필이 아니면 쿠키 Secure 를 끌 수 없습니다.");
		}
		if (properties.isPermitAll() && !dev) {
			throw new IllegalStateException("mdm.security.permit-all 은 dev 프로필에서만 켤 수 있습니다.");
		}
		if (properties.isPermitAll()) {
			log.warn("mdm.security.permit-all 이 켜져 있습니다. 권한 검사를 건너뜁니다.");
		}
	}

}
