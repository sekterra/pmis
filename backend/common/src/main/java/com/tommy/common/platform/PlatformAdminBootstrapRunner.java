package com.tommy.common.platform;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.tommy.common.tenant.provision.TemporaryPassword;

@Component
@Order(0)
@ConditionalOnProperty(name = "mdm.bootstrap.platform-admin", havingValue = "true")
public class PlatformAdminBootstrapRunner implements ApplicationRunner, ExitCodeGenerator {

	private final PlatformAdminBootstrapService bootstrapService;

	private int exitCode;

	public PlatformAdminBootstrapRunner(PlatformAdminBootstrapService bootstrapService) {
		this.bootstrapService = bootstrapService;
	}

	@Override
	public void run(ApplicationArguments args) {
		String loginId = System.getenv("MDM_BOOTSTRAP_ADMIN_LOGIN");
		if (loginId == null || loginId.isBlank()) {
			exitCode = 1;
			throw new IllegalStateException("MDM_BOOTSTRAP_ADMIN_LOGIN 이 없습니다.");
		}
		try {
			TemporaryPassword password = bootstrapService.createIfEmpty(loginId, System.getenv("MDM_BOOTSTRAP_ADMIN_NAME"),
					System.getenv("MDM_BOOTSTRAP_ADMIN_EMAIL"));
			System.out.println(password.reveal());
		}
		catch (RuntimeException ex) {
			exitCode = 1;
			throw ex;
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
