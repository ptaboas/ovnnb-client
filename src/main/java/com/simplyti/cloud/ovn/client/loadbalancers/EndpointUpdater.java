package com.simplyti.cloud.ovn.client.loadbalancers;

import java.util.Collection;

import com.google.common.collect.Sets;
import com.simplyti.cloud.ovn.client.domain.Endpoint;

public class EndpointUpdater extends EndpointBuilder<EndpointUpdater>{
	
	private final LoadBalancerUpdater parent;
	
	private Collection<Endpoint> newTargets;
	private Collection<Endpoint> oldTargets;
	
	public EndpointUpdater(LoadBalancerUpdater parent){
		this.parent=parent;
	}

	public EndpointTargetUpdater target() {
		return new EndpointTargetUpdater(this);
	}
	
	public LoadBalancerUpdater remove() {
		parent.removeVip(endpoint());
		return parent;
	}
	
	public LoadBalancerUpdater uptate() {
		if(newTargets!=null){
			parent.addTargets(endpoint(),newTargets);
		}
		if(oldTargets!=null){
			parent.removeTargets(endpoint(),oldTargets);
		}
		return parent;
	}

	public void addTarget(Endpoint endpoint) {
		if(newTargets==null){
			this.newTargets=Sets.newHashSet();
		}
		this.newTargets.add(endpoint);
	}

	public void removeTarget(Endpoint endpoint) {
		if(oldTargets==null){
			this.oldTargets=Sets.newHashSet();
		}
		this.oldTargets.add(endpoint);
	}

	

}
