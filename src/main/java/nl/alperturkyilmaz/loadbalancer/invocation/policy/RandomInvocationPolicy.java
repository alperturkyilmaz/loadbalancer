package nl.alperturkyilmaz.loadbalancer.invocation.policy;

import java.util.List;
import java.util.Random;

import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public class RandomInvocationPolicy implements InvocationPolicy {
	Random random = new Random();

	public Provider apply(List<Provider> providers) {
		if (providers != null && providers.isEmpty()) {
			return null;
		}
		return providers.get(random.nextInt(providers.size()));
	}
}
