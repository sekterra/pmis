package com.tommy.common.supportaccess;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mdm.support-access")
public class SupportAccessLocationProperties {

	private String scheme = "https";

	private String devBaseUrl = "http://localhost:8000";

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getDevBaseUrl() {
		return devBaseUrl;
	}

	public void setDevBaseUrl(String devBaseUrl) {
		this.devBaseUrl = devBaseUrl;
	}

}
