package com.tommy.common.tenant.provision;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ProvisioningPlanner {

	private ProvisioningPlanner() {
	}

	public static Decision plan(ProvisioningStepCode lastSuccess, Set<ProvisioningStepCode> handlers) {
		List<ProvisioningStepCode> toRun = new ArrayList<>();
		ProvisioningStepCode[] order = ProvisioningStepCode.values();
		int start = lastSuccess == null ? 0 : lastSuccess.ordinal() + 1;
		for (int index = start; index < order.length; index++) {
			if (!handlers.contains(order[index])) {
				return new Decision(toRun, true);
			}
			toRun.add(order[index]);
		}
		return new Decision(toRun, false);
	}

	public record Decision(List<ProvisioningStepCode> toRun, boolean paused) {
	}

}
