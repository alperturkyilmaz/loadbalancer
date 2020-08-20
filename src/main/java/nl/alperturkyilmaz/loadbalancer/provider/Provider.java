package nl.alperturkyilmaz.loadbalancer.provider;

import java.util.UUID;

/*
 * Step 1 - Generate Provider
 * 
 */
public class Provider {
	private final String id;
	
	public Provider() {
		id = UUID.randomUUID().toString();
	}

	/*
	 * returns the unique Id of the provider instance
	 */
	public String get() {
		return getId();
	}

	public String getId() {
		return id;
	}

	public boolean check() {
		return true;
	}

}
