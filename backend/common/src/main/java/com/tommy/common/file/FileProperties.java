package com.tommy.common.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mdm.file")
public class FileProperties {

	private String localRoot = "";

	private int maxSizeMb = 50;

	private String allowedExtensions = "png,jpg,jpeg,gif,webp,pdf,zip,txt,csv";

	private int deleteRetentionDays = 7;

	private int tempTtlHours = 24;

	private Cleanup cleanup = new Cleanup();

	public String getLocalRoot() {
		return localRoot;
	}

	public void setLocalRoot(String localRoot) {
		this.localRoot = localRoot;
	}

	public int getMaxSizeMb() {
		return maxSizeMb;
	}

	public void setMaxSizeMb(int maxSizeMb) {
		this.maxSizeMb = maxSizeMb;
	}

	public String getAllowedExtensions() {
		return allowedExtensions;
	}

	public void setAllowedExtensions(String allowedExtensions) {
		this.allowedExtensions = allowedExtensions;
	}

	public int getDeleteRetentionDays() {
		return deleteRetentionDays;
	}

	public void setDeleteRetentionDays(int deleteRetentionDays) {
		this.deleteRetentionDays = deleteRetentionDays;
	}

	public int getTempTtlHours() {
		return tempTtlHours;
	}

	public void setTempTtlHours(int tempTtlHours) {
		this.tempTtlHours = tempTtlHours;
	}

	public Cleanup getCleanup() {
		return cleanup;
	}

	public void setCleanup(Cleanup cleanup) {
		this.cleanup = cleanup;
	}

	public static class Cleanup {

		private boolean enabled = false;

		private String cron = "0 0 3 * * *";

		private String zone = "Asia/Seoul";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getCron() {
			return cron;
		}

		public void setCron(String cron) {
			this.cron = cron;
		}

		public String getZone() {
			return zone;
		}

		public void setZone(String zone) {
			this.zone = zone;
		}

	}

}
