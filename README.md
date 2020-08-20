# Load Balancer (LB)

A Load Balancer for distributing incoming requests to a list of registered providers.

**Invocation Policies:**
* RandomInvocationPolicy: The Provider that is going to process the message is being selected randomly.
* RoundRobinInvocationPolicy: The Provider that is going to process the message is being selected via round-robin algorithm.

**Heartbeat Policies:**
* DefaultHeartbeatPolicy: Unhealthy Providers are removed from the list.
* ImprovedHeartbeatPolicy: Unhealthy Providers removed from the list, after receiving  N successful heartbeat, Provider is being added to list.



Implemented using **Java 8**.
You can compile it via:

```bash
mvnw clean package -DskipTests
```
REMARK! Unit tests include multithreaded testcases. If you enable tests, it may take a bit more time packaging the application.

## Sample Usage
```bash
...
//instantiate loadbalancer
LoadBalancer loadBalancer = new LoadBalancer.Builder()
                                  .withInvocationPolicy(new RoundRobinInvocationPolicy())
                                  .withHeartbeatPolicy(new ImprovedHeartbeatPolicy())
                                  .build();
//with no Provider   
List<Provider> providerList = new ArrayList<>();
loadBalancer.register(providerList);

//Make request to the loadbalancer
String response = loadBalancer.get();
...

```
		
