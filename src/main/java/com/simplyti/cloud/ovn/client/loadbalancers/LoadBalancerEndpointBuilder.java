package com.simplyti.cloud.ovn.client.loadbalancers;

import java.util.Collection;

import com.google.common.collect.Sets;
import com.simplyti.cloud.ovn.client.domain.Endpoint;

public class LoadBalancerEndpointBuilder extends EndpointBuilder<LoadBalancerEndpointBuilder>{
	
	private final LoadBalancerBuilder parentBuilder;
	
	private Collection<Endpoint> targets;
	
	public LoadBalancerEndpointBuilder(LoadBalancerBuilder parentBuilder){
		this.parentBuilder=parentBuilder;
	}
	
	public TargetEndpointBuilder addTarget() {
		return new TargetEndpointBuilder(this);
	}

	public LoadBalancerEndpointBuilder addTarget(Endpoint endpoint) {
		if(targets==null){
			targets=Sets.newHashSet();
		}
		this.targets.add(endpoint);
		return this;
	}

	public LoadBalancerBuilder create() {
		parentBuilder.withEndpoint(endpoint(),targets);
		return parentBuilder;
	}

	public LoadBalancerEndpointBuilder setTargets(Collection<Endpoint> targets) {
		this.targets=targets;
		return this;
	}

}
