package com.simplyti.cloud.ovn.client.loadbalancers;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.simplyti.cloud.ovn.client.AbstractNamedApiBuilder;
import com.simplyti.cloud.ovn.client.NamedResourceApi;
import com.simplyti.cloud.ovn.client.domain.Endpoint;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;
import com.simplyti.cloud.ovn.client.routers.DefaultRoutersApi;
import com.simplyti.cloud.ovn.client.switches.DefaultSwitchesApi;

public class LoadBalancerBuilder extends AbstractNamedApiBuilder<LoadBalancer,LoadBalancerBuilder>{
	
	private final DefaultSwitchesApi switches; 
	private final DefaultRoutersApi routers;
	
	private Protocol protocol;
	private Map<Endpoint,Collection<Endpoint>> endpoints;

	public LoadBalancerBuilder(NamedResourceApi<LoadBalancer> namedApi,DefaultSwitchesApi switches,
			DefaultRoutersApi routers) {
		super(namedApi);
		this.switches=switches;
		this.routers=routers;
	}

	@Override
	protected LoadBalancer create(String name,Map<String,String> externalIds) {
		return new LoadBalancer(null,name, 
				protocol==null?Protocol.TCP:protocol,
				endpoints,externalIds);
	}

	public LoadBalancerEndpointBuilder withVirtualIp() {
		return new LoadBalancerEndpointBuilder(this);
	}

	public void withEndpoint(Endpoint endpoint, Collection<Endpoint> targets) {
		if(endpoints==null){
			endpoints=Maps.newHashMap();
		}
		endpoints.put(endpoint, targets);
	}

	public LoadBalancerBuilder withProtocol(Protocol protocol) {
		this.protocol=protocol;
		return this;
	}

	public LoadBalancerBuilder inLogicalSwitch(String name) {
		addOperation(switches.addLoadNamedUUIDBalancerOperation(name, "created"));
		return this;
	}

	public LoadBalancerBuilder inLogicalRouter(String name) {
		addOperation(routers.addLoadNamedUUIDBalancerOperation(name, "created"));
		return this;
	}

}
