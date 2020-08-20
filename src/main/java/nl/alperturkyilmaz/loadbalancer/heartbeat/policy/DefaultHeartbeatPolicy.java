package nl.alperturkyilmaz.loadbalancer.heartbeat.policy;

import java.util.Collection;

import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public class DefaultHeartbeatPolicy implements HeartbeatPolicy {
	@Override
	public void apply(HeartbeatMonitorable monitorable) {
		Collection<Provider> providers = monitorable.getProviders();
		for (Provider provider : providers) {
			if (provider.check() == false) {
				monitorable.exclude(provider);
			}
		}
	}
}
