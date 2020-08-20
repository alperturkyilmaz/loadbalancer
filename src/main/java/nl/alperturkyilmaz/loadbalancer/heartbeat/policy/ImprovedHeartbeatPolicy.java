package nl.alperturkyilmaz.loadbalancer.heartbeat.policy;

import java.util.Collection;
import java.util.HashMap;

import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public class ImprovedHeartbeatPolicy implements HeartbeatPolicy {

	public static int DEFAULT_CONSECUTIVE_HEARTBEAT_THRESHOLD = 2;

	int threshold = DEFAULT_CONSECUTIVE_HEARTBEAT_THRESHOLD;

	private HashMap<Provider, Integer> excludedProvidersMap = new HashMap<>();

	public ImprovedHeartbeatPolicy() {
	}

	public ImprovedHeartbeatPolicy(int threshold) {
		this.threshold = threshold;
	}

	/*
	 * This policy only re-includes a provider, if the policy excluded that provider before.
	 * Manually excluded Providers are not included even if they become healthy again.
	 */
	
	@Override
	public void apply(HeartbeatMonitorable monitorable) {
		Collection<Provider> providers = monitorable.getProviders();
		for (Provider provider : providers) {
			if (provider.check() == false) {
				// Hearbeat failed! Exclude provider from the alive list and store it
				// When provider is alive again for threshold times, it will be included to the
				// alive list.
				if (monitorable.exclude(provider) && !excludedProvidersMap.containsKey(provider)) {
					excludedProvidersMap.put(provider, 0);
				}
			} else {
				Integer successCount = excludedProvidersMap.get(provider);
				if (successCount != null) {
					// Heartbeatpolicy already excluded this provider before, it is allowed to check
					// for inclusion
					if (successCount == threshold) {
						excludedProvidersMap.remove(provider);
						monitorable.include(provider);
					} else {
						excludedProvidersMap.put(provider, ++successCount);
					}
				}
			}
		}
	}
}
