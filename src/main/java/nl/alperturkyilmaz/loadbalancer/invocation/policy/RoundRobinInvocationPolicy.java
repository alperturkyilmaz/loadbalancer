package nl.alperturkyilmaz.loadbalancer.invocation.policy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public class RoundRobinInvocationPolicy implements InvocationPolicy {
	private AtomicInteger position = new AtomicInteger(0);

	public Provider apply(List<Provider> providers) {
		if (providers != null && providers.isEmpty()) {
			return null;
		}
		return providers.get(position.getAndUpdate(p -> p = (p + 1) % providers.size()));
	}
}
