package nl.alperturkyilmaz.loadbalancer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.alperturkyilmaz.loadbalancer.LoadBalancer;
import nl.alperturkyilmaz.loadbalancer.LoadBalancer.Builder;
import nl.alperturkyilmaz.loadbalancer.heartbeat.policy.DefaultHeartbeatPolicy;
import nl.alperturkyilmaz.loadbalancer.heartbeat.policy.ImprovedHeartbeatPolicy;
import nl.alperturkyilmaz.loadbalancer.invocation.policy.RandomInvocationPolicy;
import nl.alperturkyilmaz.loadbalancer.invocation.policy.RoundRobinInvocationPolicy;
import nl.alperturkyilmaz.loadbalancer.provider.Provider;
import nl.alperturkyilmaz.loadbalancer.test.MockProvider.ProviderStatus;

public class AssessmentUnitTest {

	Builder builder;

	public static abstract class Task implements Runnable {
		int index = 0;

		public Task(int index) {
			this.index = index;
		}

		@Override
		public void run() {
			doRun();
		}

		public abstract void doRun();
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("####### LoadBalancer AssessmentUnitTest started #######");
		builder = new LoadBalancer.Builder();

	}

	private List<Provider> generateRandomSizeNonEmptyProviderList() {
		return generateProviders(new Random().nextInt(10) + 1);
	}

	private List<Provider> generateProviders(int size) {
		List<Provider> providers = new ArrayList<Provider>();
		for (int i = 0; i < size; i++) {
			providers.add(new Provider());
		}
		return providers;
	}

	@Test
	public void testRandomLoadBalancerWithOneProvider() {
		List<Provider> providers = generateProviders(1);

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RandomInvocationPolicy()).withProviders(providers).build();

		String response1 = loadBalancer.get();
		String response2 = loadBalancer.get();

		assertEquals(response1, response2);
	}

	@Test
	public void testRoundRobinLoadBalancerWithMaxProvider() {

		List<Provider> providers = generateProviders(10);

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withProviders(providers).build();

		int maximumNumberOfRequest = loadBalancer.getMaximumRegisteredProviders() + new Random().nextInt(loadBalancer.getMaximumRegisteredProviders());

		int counter = 0;
		while (counter < maximumNumberOfRequest) {
			Provider provider = providers.get(counter % providers.size());
			String response = loadBalancer.get();
			assertEquals(response, provider.get());
			counter++;
		}

	}

	@Test
	public void testRoundRobinLoadBalancerWithOneProvider() {
		List<Provider> providers = generateProviders(1);

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withProviders(providers).build();

		String response1 = loadBalancer.get();
		String response2 = loadBalancer.get();

		assertEquals(response1, response2);
	}

	@Test
	public void testRandomLoadBalancerWithNoProvider() {
		List<Provider> providers = generateProviders(0);

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RandomInvocationPolicy()).withProviders(providers).build();

		String response1 = loadBalancer.get();
		assertEquals(null, response1);

	}

	@Test
	public void testRoundRobinLoadBalancerWithNoProvider() {
		List<Provider> providers = generateProviders(0);

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withProviders(providers).build();

		String response1 = loadBalancer.get();
		assertEquals(null, response1);

	}

	@Test

	public void testDefaultHeartbeatPolicy() {
		List<Provider> providers = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			providers.add(new MockProvider(ProviderStatus.RANDOM_BEHAVIOR, false));
		}

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withProviders(providers).withHeartbeatPolicy(new DefaultHeartbeatPolicy()).build();
		try {
			Object lock = new Object();
			synchronized (lock) {
				lock.wait(20 * 1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadBalancer.shutdown();
	}

	@Test
	public void testDefaultHeartbeatPolicyWithNotWorkingProviders() {
		List<Provider> providers = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			providers.add(new MockProvider(ProviderStatus.NOT_WORKING, false));
		}

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withProviders(providers).withHeartbeatPolicy(new DefaultHeartbeatPolicy()).build();
		try {
			Object lock = new Object();
			synchronized (lock) {
				lock.wait(20 * 1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String response = loadBalancer.get();
		assertTrue(response == null);

		loadBalancer.shutdown();
	}

	@Test
	public void testImprovedHeartbeatPolicy() {
		List<Provider> providers = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			providers.add(new MockProvider(ProviderStatus.RANDOM_BEHAVIOR, false));
		}

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withProviders(providers).withHeartbeatPolicy(new ImprovedHeartbeatPolicy()).build();
		try {
			Object lock = new Object();
			synchronized (lock) {
				lock.wait(20 * 1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadBalancer.shutdown();
	}

	@Test
	public void testExcludeAllIncludeAllProviders() {
		List<Provider> providers = generateRandomSizeNonEmptyProviderList();

		LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withProviders(providers).build();

		for (Provider provider : providers) {
			assertTrue("Failed to exclude provider", loadBalancer.exclude(provider));
		}

		String response = loadBalancer.get();
		assertEquals("After excluding all the providers, null response expected", null, response);

		Collections.shuffle(providers);

		for (int i = 0; i < providers.size(); i++) {
			Provider provider = providers.get(i);
			loadBalancer.include(provider);
			response = loadBalancer.get();
			assertEquals("After including a provider, wrong response received", providers.get((i == 0) ? 0 : (i - 1)).getId(), response);
		}
	}

	@After
	public void clean() throws Exception {
		System.out.println("####### LoadBalancer AssessmentUnitTest finished #######");
	}

	@Test
	public void testClusterCapacityLimit() {
		List<Provider> providers = new ArrayList<>();
		int providerSize = 8;
		int requestPerProvider = 4;

		for (int i = 0; i < providerSize; i++) {
			MockProvider mockProvider = new MockProvider(ProviderStatus.ALIVE, true);
			mockProvider.setMaxDelayInSeconds(20);
			providers.add(mockProvider);
		}

		final LoadBalancer loadBalancer = builder.withInvocationPolicy(new RoundRobinInvocationPolicy()).withClusterCapacity(requestPerProvider).withProviders(providers).withHeartbeatPolicy(new ImprovedHeartbeatPolicy()).build();

		int requestSize = 50;
		final int capacity = providerSize * requestPerProvider;
		for (int i = 0; i < requestSize; i++) {
			new Thread(new Task(i) {
				@Override
				public void doRun() {
					String response = loadBalancer.get();
					if (response == null) {
						assertTrue("", index >= capacity);
					} else {
						assertTrue("", index < capacity);
					}
				}
			}).start();

		}
		try {
			Object lock = new Object();
			synchronized (lock) {
				lock.wait(40 * 1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadBalancer.shutdown();
	}
}
