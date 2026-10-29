package com.tommy.common.devadmin;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class DevSetAdminRunner implements ApplicationRunner, ExitCodeGenerator {

	private final DevSetAdminService service;

	private final Environment environment;

	private int exitCode;

	public DevSetAdminRunner(DevSetAdminService service, Environment environment) {
		this.service = service;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!args.containsOption("dev-set-admin")) {
			return;
		}
		try {
			DevSetAdminRules.requireDevProfile(environment.acceptsProfiles(Profiles.of("dev")));
			DevSetAdminRules.Command command = DevSetAdminRules.parse(
					System.getenv("MDM_DEV_ADMIN_TARGET"),
					System.getenv("MDM_DEV_ADMIN_TENANT_CODE"),
					System.getenv("MDM_DEV_ADMIN_LOGIN"),
					System.getenv("MDM_DEV_ADMIN_PASSWORD"));
			System.out.println(service.apply(command));
		}
		catch (RuntimeException ex) {
			exitCode = 1;
			throw new IllegalStateException(ex.getMessage() == null ? "개발용 관리자 계정을 바꾸지 못했습니다." : ex.getMessage(), ex);
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
