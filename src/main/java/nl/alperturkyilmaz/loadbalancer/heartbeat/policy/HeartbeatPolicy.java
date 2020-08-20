package nl.alperturkyilmaz.loadbalancer.heartbeat.policy;

public interface HeartbeatPolicy {

	public void apply(HeartbeatMonitorable monitorable);

}
