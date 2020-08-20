package nl.alperturkyilmaz.loadbalancer.invocation.policy;

import java.util.List;

import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public interface InvocationPolicy {

	/*
	 * returns next suitable Provider
	 */

	Provider apply(List<Provider> providers);

}
