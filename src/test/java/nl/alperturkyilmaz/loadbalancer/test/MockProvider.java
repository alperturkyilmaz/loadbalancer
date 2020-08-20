package nl.alperturkyilmaz.loadbalancer.test;

import java.util.Random;

import nl.alperturkyilmaz.loadbalancer.provider.Provider;

public class MockProvider extends Provider {
	enum ProviderStatus {
		ALIVE, NOT_WORKING, RANDOM_BEHAVIOR
	}

	private Random rnd = new Random();

	ProviderStatus status;
	boolean randomDelayInPProcessing;
	private int maxDelayInSeconds = 20;

	private Object lock = new Object();

	public MockProvider(ProviderStatus status, boolean randomDelayInPProcessing) {
		this.status = status;
		this.randomDelayInPProcessing = randomDelayInPProcessing;
	}

	public void setMaxDelayInSeconds(int maxDelayInSeconds) {
		this.maxDelayInSeconds = maxDelayInSeconds;
	}

	@Override
	public boolean check() {
		return (status == ProviderStatus.ALIVE) ? true : (status == ProviderStatus.NOT_WORKING) ? false : (rnd.nextDouble() < 0.7);
	}

	@Override
	public String get() {
		if (randomDelayInPProcessing) {
			synchronized (lock) {
				try {
					lock.wait((rnd.nextInt(maxDelayInSeconds) + 1) * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return super.get();
	}
}
