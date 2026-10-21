package com.tommy.common.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "mdm.file.cleanup", name = "enabled", havingValue = "true")
public class FileCleanupSchedulingConfig {
}
