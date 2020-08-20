package nl.alperturkyilmaz.loadbalancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import nl.alperturkyilmaz.loadbalancer.heartbeat.policy.HeartbeatPolicy;
import nl.alperturkyilmaz.loadbalancer.heartbeat.policy.HeartbeatMonitorable;
import nl.alperturkyilmaz.loadbalancer.invocation.policy.InvocationPolicy;
import nl.alperturkyilmaz.loadbalancer.invocation.policy.RandomInvocationPolicy;
import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public class LoadBalancer implements HeartbeatMonitorable {

	public static int DEFAULT_HEARTBEAT_CONTROL_PERIOD = 10;
	public static int DEFAULT_MAXIMUM_REGISTERED_PROVIDERS = 10;
	public static int INFINITE_PARALLEL_REQUESTS = 0;

	/*
	 * Maximum number of providers that can register to the Load Balancer
	 */
	private int maximumRegisteredProviders;

	/*
	 * Time in seconds to check health of the Providers
	 */
	private int heartbeatControlPeriod;

	/*
	 * Maximum number of parallel request that a Provider can handle A non-positive
	 * value indicates infinite requests
	 */
	private int maximumNoOfParalelRequests;

	/*
	 * List of available providers (healthy & serving)
	 */
	private List<Provider> availableProviders = new ArrayList<>();

	/*
	 * List of unavailable providers (unhealthy & not serving)
	 */
	private List<Provider> unavailableProviders = new ArrayList<>();

	/*
	 * All registered providers
	 */
	private HashMap<String, Provider> allProviders = new HashMap<>();

	/*
	 * InvocationPolicy is used in determining the next Provider which will handle
	 * the request. An invocation policy can be of the type RandomInvocationPolicy
	 * or RoundRobinInvocationPolicy. Default policy is RandomInvocationPolicy
	 * 
	 */
	private InvocationPolicy invocationPolicy;

	/*
	 * HeartbeatPolicy is used in checking the healthiness of the Providers. A
	 * hearbeat policy can be of the type DefaultHeartbeatPolicy or
	 * ImprovedHeartbeatPolicy. Default policy is DefaultHeartbeatPolicy
	 * 
	 */
	private HeartbeatPolicy heartbeatPolicy;

	/*
	 * HeartbeatPolicy executor.
	 * 
	 */
	private ScheduledExecutorService scheduler;

	/*
	 * Statistical information. Number of active concurrent requests
	 */
	private AtomicInteger concurrentRequestCount = new AtomicInteger(0);

	public static class Builder {
		private int maxAllowedProviders = DEFAULT_MAXIMUM_REGISTERED_PROVIDERS;
		private int heartbeatControlPeriod = DEFAULT_HEARTBEAT_CONTROL_PERIOD;
		private int maximumNoOfParalelRequests = INFINITE_PARALLEL_REQUESTS;
		
		private List<Provider> providerList;
		private InvocationPolicy invocationPolicy = new RandomInvocationPolicy();
		private HeartbeatPolicy heartbeatPolicy;

		public Builder maximumAllowedProviders(int maxAllowedProviders) {
			if (maxAllowedProviders < 0) {
				throw new java.lang.IllegalArgumentException("Maximum allowed providers can not be negative");
			}
			this.maxAllowedProviders = maxAllowedProviders;
			return this;
		}

		public Builder withClusterCapacity(int maximumNoOfParalelRequestsPerProvider) {
			maximumNoOfParalelRequests = maximumNoOfParalelRequestsPerProvider <= 0 ? INFINITE_PARALLEL_REQUESTS : maximumNoOfParalelRequestsPerProvider;
			return this;
		}

		public Builder withProviders(List<Provider> providerList) {
			if (providerList != null) {
				this.providerList = providerList;
			}
			return this;
		}

		public Builder withInvocationPolicy(InvocationPolicy invocationPolicy) {
			if (invocationPolicy != null) {
				this.invocationPolicy = invocationPolicy;
			}
			return this;
		}

		public Builder withHeartbeatPolicy(HeartbeatPolicy heartbeatPolicy) {
			if (heartbeatPolicy != null) {
				this.heartbeatPolicy = heartbeatPolicy;
			}
			return this;
		}

		public Builder withHeartbeatFrequency(int inSeconds) {
			if (inSeconds <= 0) {
				throw new java.lang.IllegalArgumentException("Heartbeat frequency must be positive and must be in seconds");
			}
			this.heartbeatControlPeriod = inSeconds;
			return this;
		}

		public LoadBalancer build() {
			LoadBalancer loadBalancer = new LoadBalancer();
			loadBalancer.maximumRegisteredProviders = this.maxAllowedProviders;
			loadBalancer.invocationPolicy = this.invocationPolicy;
			loadBalancer.heartbeatPolicy = this.heartbeatPolicy;
			loadBalancer.heartbeatControlPeriod = this.heartbeatControlPeriod;
			loadBalancer.maximumNoOfParalelRequests = this.maximumNoOfParalelRequests;
			loadBalancer.register(providerList);
			loadBalancer.startHeartbeatScheduler();
			return loadBalancer;
		}
	}

	/*
	 * Starts a heartbeat scheduler to apply the heartbeatpolicy for every
	 * heartbeatControlPeriod
	 * 
	 */
	private void startHeartbeatScheduler() {
		if (heartbeatPolicy != null && scheduler == null) {
			scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleWithFixedDelay(() -> heartbeatPolicy.apply(this), 0, heartbeatControlPeriod, TimeUnit.SECONDS);
		}
	}

	/*
	 * Maximum number of registered providers
	 * 
	 */
	public int getMaximumRegisteredProviders() {
		return maximumRegisteredProviders;
	}

	/*
	 * Register a list of provider instances to the Load Balancer - the maximum
	 * number of providers accepted from the load balancer is
	 * maximumAllowedProviders. Providers can be also safely registered after the
	 * instantiation of the Load Balancer.
	 */
	public void register(List<Provider> providerList) {
		if (providerList == null) {
			return;
		}
		synchronized (availableProviders) {
			int limit = Math.min((allProviders.size() + providerList.size()), maximumRegisteredProviders);
			Iterator<Provider> iterator = providerList.iterator();
			while (allProviders.size() < limit && iterator.hasNext()) {
				Provider provider = iterator.next();
				availableProviders.add(provider);
				allProviders.put(provider.getId(), provider);
			}
		}
	}

	/*
	 * Returns the unmodifiable collection of all providers.
	 */
	public Collection<Provider> getProviders() {
		return Collections.unmodifiableCollection(allProviders.values());
	}

	/*
	 * Cluster capacity limit check If maximumNoOfParalelRequests is set to a
	 * positive value and number of active concurrent request count is above or
	 * equal to the Cluster Capacity, returns true o.w false.
	 * 
	 */
	private boolean isClusterCapacityLimitExceeded() {
		if (maximumNoOfParalelRequests <= INFINITE_PARALLEL_REQUESTS) {
			return false;
		}
		int currentClusterCapacity = 0;
		synchronized (availableProviders) {
			currentClusterCapacity = maximumNoOfParalelRequests * availableProviders.size();
		}
		return (concurrentRequestCount.get() >= currentClusterCapacity);
	}

	/*
	 * 1- Checks if the Cluster Capacity is exceeded or not, if exceeded returns
	 * null. 2- Gets an available Provider according to the invocationPolicy, 3- If
	 * there an available(alive) provider is found, invokes the provider and returns
	 * the response o.w. returns null.
	 */
	public String get() {
		String response = null;

		if (isClusterCapacityLimitExceeded()) {
			return response;
		}

		Provider nextProvider = null;

		synchronized (availableProviders) {
			nextProvider = invocationPolicy.apply(Collections.unmodifiableList(availableProviders));
		}

		if (nextProvider != null) {
			concurrentRequestCount.incrementAndGet();
			response = nextProvider.get();
			concurrentRequestCount.decrementAndGet();
		}
		return response;
	}

	/*
	 * Excludes the provider from available list
	 */
	public boolean exclude(Provider provider) {
		boolean excluded = false;
		synchronized (availableProviders) {
			excluded = availableProviders.remove(provider);
		}
		if (excluded) {
			synchronized (unavailableProviders) {
				unavailableProviders.add(provider);
			}
		}
		return excluded;
	}

	/*
	 * Includes the provider to the available list
	 */
	public boolean include(Provider provider) {
		boolean toBeIncluded = false;
		synchronized (unavailableProviders) {
			toBeIncluded = unavailableProviders.remove(provider);
		}
		if (toBeIncluded) {
			synchronized (availableProviders) {
				availableProviders.add(provider);
			}
		}
		return toBeIncluded;
	}

	private LoadBalancer() {

	}

	/*
	 * Stops the heartbeat scheduler
	 */
	public void shutdown() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}

}
