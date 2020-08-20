package nl.alperturkyilmaz.loadbalancer.heartbeat.policy;

import java.util.Collection;

import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public interface HeartbeatMonitorable {
	
	Collection<Provider> getProviders();

	boolean include(Provider provider);

	boolean exclude(Provider provider);

}
