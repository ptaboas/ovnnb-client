package com.simplyti.cloud.ovn.client.loadbalancers;

public class EndpointTargetUpdater extends EndpointBuilder<EndpointTargetUpdater>{
	
	private EndpointUpdater parent;
	
	public EndpointTargetUpdater(EndpointUpdater parent){
		this.parent=parent;
	}

	public EndpointUpdater add() {
		parent.addTarget(endpoint());
		return parent;
	}
	
	public EndpointUpdater remove() {
		parent.removeTarget(endpoint());
		return parent;
	}

}
