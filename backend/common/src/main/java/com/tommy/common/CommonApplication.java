package com.tommy.common;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import com.tommy.common.global.multitenancy.PlatformHostProperties;
import com.tommy.common.global.security.MdmSecurityProperties;

@SpringBootApplication
@EnableConfigurationProperties({ MdmSecurityProperties.class, PlatformHostProperties.class })
public class CommonApplication {

	public static void main(String[] args) {
		boolean bootstrap = false;
		for (String arg : args) {
			if ("--bootstrap-platform-admin".equals(arg)) {
				bootstrap = true;
			}
		}
		SpringApplication application = new SpringApplication(CommonApplication.class);
		if (bootstrap) {
			application.setWebApplicationType(WebApplicationType.NONE);
			application.setDefaultProperties(Map.of("mdm.bootstrap.platform-admin", "true"));
		}
		ConfigurableApplicationContext context = application.run(args);
		if (bootstrap) {
			System.exit(SpringApplication.exit(context));
		}
	}

}
