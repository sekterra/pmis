package com.tommy.common.global.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mdm.security")
public class MdmSecurityProperties {

	private String issuer = "http://localhost";

	private boolean permitAll;

	private boolean trustForwardedHeaders;

	private List<String> publicNamespaces = new ArrayList<>(List.of("common", "error", "auth"));

	private Cookie cookie = new Cookie();

	private Cors cors = new Cors();

	private String privateKeyPath = "";

	private String previousPublicKeyPath = "";

	private int refreshReuseGraceSeconds = 10;

	private Platform platform = new Platform();

	private SupportAccess supportAccess = new SupportAccess();

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public boolean isPermitAll() {
		return permitAll;
	}

	public void setPermitAll(boolean permitAll) {
		this.permitAll = permitAll;
	}

	public boolean isTrustForwardedHeaders() {
		return trustForwardedHeaders;
	}

	public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
		this.trustForwardedHeaders = trustForwardedHeaders;
	}

	public List<String> getPublicNamespaces() {
		return publicNamespaces;
	}

	public void setPublicNamespaces(List<String> publicNamespaces) {
		this.publicNamespaces = publicNamespaces;
	}

	public Cookie getCookie() {
		return cookie;
	}

	public void setCookie(Cookie cookie) {
		this.cookie = cookie;
	}

	public Cors getCors() {
		return cors;
	}

	public void setCors(Cors cors) {
		this.cors = cors;
	}

	public String getPrivateKeyPath() {
		return privateKeyPath;
	}

	public void setPrivateKeyPath(String privateKeyPath) {
		this.privateKeyPath = privateKeyPath;
	}

	public String getPreviousPublicKeyPath() {
		return previousPublicKeyPath;
	}

	public void setPreviousPublicKeyPath(String previousPublicKeyPath) {
		this.previousPublicKeyPath = previousPublicKeyPath;
	}

	public int getRefreshReuseGraceSeconds() {
		return refreshReuseGraceSeconds;
	}

	public void setRefreshReuseGraceSeconds(int refreshReuseGraceSeconds) {
		this.refreshReuseGraceSeconds = refreshReuseGraceSeconds;
	}

	public Platform getPlatform() {
		return platform;
	}

	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public SupportAccess getSupportAccess() {
		return supportAccess;
	}

	public void setSupportAccess(SupportAccess supportAccess) {
		this.supportAccess = supportAccess;
	}

	public static class Cookie {

		private boolean secure = true;

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

	}

	public static class Platform {

		private int accessTokenMinutes = 15;

		private int refreshTokenDays = 14;

		private int maxLoginFailCount = 5;

		private int lockMinutes = 30;

		private int passwordMinLength = 12;

		private int passwordMinClasses = 3;

		private int passwordExpireDays = 90;

		private int tempPasswordValidDays = 3;

		public int getAccessTokenMinutes() {
			return accessTokenMinutes;
		}

		public void setAccessTokenMinutes(int accessTokenMinutes) {
			this.accessTokenMinutes = accessTokenMinutes;
		}

		public int getRefreshTokenDays() {
			return refreshTokenDays;
		}

		public void setRefreshTokenDays(int refreshTokenDays) {
			this.refreshTokenDays = refreshTokenDays;
		}

		public int getMaxLoginFailCount() {
			return maxLoginFailCount;
		}

		public void setMaxLoginFailCount(int maxLoginFailCount) {
			this.maxLoginFailCount = maxLoginFailCount;
		}

		public int getLockMinutes() {
			return lockMinutes;
		}

		public void setLockMinutes(int lockMinutes) {
			this.lockMinutes = lockMinutes;
		}

		public int getPasswordMinLength() {
			return passwordMinLength;
		}

		public void setPasswordMinLength(int passwordMinLength) {
			this.passwordMinLength = passwordMinLength;
		}

		public int getPasswordMinClasses() {
			return passwordMinClasses;
		}

		public void setPasswordMinClasses(int passwordMinClasses) {
			this.passwordMinClasses = passwordMinClasses;
		}

		public int getPasswordExpireDays() {
			return passwordExpireDays;
		}

		public void setPasswordExpireDays(int passwordExpireDays) {
			this.passwordExpireDays = passwordExpireDays;
		}

		public int getTempPasswordValidDays() {
			return tempPasswordValidDays;
		}

		public void setTempPasswordValidDays(int tempPasswordValidDays) {
			this.tempPasswordValidDays = tempPasswordValidDays;
		}

	}

	public static class SupportAccess {

		private int ticketTtlSeconds = 60;

		private int ttlMinutes = 30;

		public int getTicketTtlSeconds() {
			return ticketTtlSeconds;
		}

		public void setTicketTtlSeconds(int ticketTtlSeconds) {
			this.ticketTtlSeconds = ticketTtlSeconds;
		}

		public int getTtlMinutes() {
			return ttlMinutes;
		}

		public void setTtlMinutes(int ttlMinutes) {
			this.ttlMinutes = ttlMinutes;
		}

	}

	public static class Cors {

		private List<String> allowedOrigins = new ArrayList<>();

		public List<String> getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(List<String> allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
		}

	}

}
