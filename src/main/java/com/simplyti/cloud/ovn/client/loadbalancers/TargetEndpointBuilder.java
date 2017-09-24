package com.simplyti.cloud.ovn.client.loadbalancers;

public class TargetEndpointBuilder extends EndpointBuilder<TargetEndpointBuilder>{
	
	private LoadBalancerEndpointBuilder parentBuilder;

	public TargetEndpointBuilder(LoadBalancerEndpointBuilder parentBuilder){
		this.parentBuilder=parentBuilder;
	}

	public LoadBalancerEndpointBuilder add() {
		parentBuilder.addTarget(endpoint());
		return parentBuilder;
	}
	
}
