package com.tommy.common;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import com.tommy.common.file.FileProperties;
import com.tommy.common.global.multitenancy.PlatformHostProperties;
import com.tommy.common.global.security.MdmSecurityProperties;
import com.tommy.common.supportaccess.SupportAccessLocationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ MdmSecurityProperties.class, PlatformHostProperties.class, FileProperties.class,
		SupportAccessLocationProperties.class })
public class CommonApplication {

	public static void main(String[] args) {
		boolean bootstrap = false;
		boolean devSetAdmin = false;
		for (String arg : args) {
			if ("--bootstrap-platform-admin".equals(arg)) {
				bootstrap = true;
			}
			if ("--dev-set-admin".equals(arg)) {
				devSetAdmin = true;
			}
		}
		SpringApplication application = new SpringApplication(CommonApplication.class);
		if (bootstrap || devSetAdmin) {
			application.setWebApplicationType(WebApplicationType.NONE);
		}
		if (bootstrap) {
			application.setDefaultProperties(Map.of("mdm.bootstrap.platform-admin", "true"));
		}
		ConfigurableApplicationContext context = application.run(args);
		if (bootstrap || devSetAdmin) {
			System.exit(SpringApplication.exit(context));
		}
	}

}
