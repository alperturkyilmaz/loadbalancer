# Load Balancer (LB)

A Load Balancer for distributing incoming requests to a list of registered providers.

**Invocation Policies:**
* _RandomInvocationPolicy:_ The Provider that is going to process the message is being selected randomly.
* _RoundRobinInvocationPolicy:_ The Provider that is going to process the message is being selected via round-robin algorithm.

**Heartbeat Policies:**
* _DefaultHeartbeatPolicy:_ Unhealthy Providers are removed from the list.
* _ImprovedHeartbeatPolicy:_ Unhealthy Providers are removed from the list, after receiving  N successful heartbeat, Provider is being added to list.



Implemented using **Java 8**.
You can compile it via:

```bash
mvnw clean package -DskipTests
```
or if maven installed:

```bash
mvn clean package -DskipTests
```

REMARK! Unit tests include multithreaded testcases. If you enable tests, it may take a bit more time in packaging the application.

## Sample Usage
```bash
...
//instantiate the load balancer
LoadBalancer loadBalancer = new LoadBalancer.Builder()
                                  .withInvocationPolicy(new RoundRobinInvocationPolicy())
                                  .withHeartbeatPolicy(new ImprovedHeartbeatPolicy())
				  .withClusterCapacity(4)
				  .withHeartbeatFrequency(10)
                                  .build();
//with no Provider   
List<Provider> providerList = new ArrayList<>();
loadBalancer.register(providerList);

//Make request to the loadbalancer,since there is no provider to process the request, a null respose will be returned.
String response = loadBalancer.get();
...

```
		
