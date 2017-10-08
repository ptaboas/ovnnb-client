package com.simplyti.cloud.ovn.client.loadbalancers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.simplyti.cloud.ovn.client.InternalClient;
import com.simplyti.cloud.ovn.client.Updater;
import com.simplyti.cloud.ovn.client.domain.Endpoint;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.switches.DefaultSwitchesApi;

import io.netty.util.concurrent.Future;

public class LoadBalancerUpdater extends Updater<LoadBalancer> {

	private final DefaultSwitchesApi switches;
	private final DefaultLoadBalancersApi loadBalanders;
	
	private boolean clearBefore = false;
	
	private Map<Endpoint,Collection<Endpoint>> addEndpoints;
	private Map<Endpoint,Collection<Endpoint>> removeEndpoints;
	private Collection<Endpoint> removeVips;

	public LoadBalancerUpdater(InternalClient client,String db,String table,DefaultSwitchesApi switches,DefaultLoadBalancersApi loadBalanders, UUID uuid,
			Future<LoadBalancer> currentFuture) {
		super(client,db,table, uuid,currentFuture);
		this.loadBalanders=loadBalanders;
		this.switches=switches;
	}

	public LoadBalancerUpdater(InternalClient client,String db,String table,DefaultSwitchesApi switches,DefaultLoadBalancersApi loadBalanders, String name,
			Future<LoadBalancer> currentFuture) {
		super(client,db,table, name,currentFuture);
		this.loadBalanders=loadBalanders;
		this.switches=switches;
	}

	public LoadBalancerUpdater attachToSwitch(UUID uuid) {
		addOperation(switches.addLoadBalancerOperation(uuid,super.uuid));
		return this;
	}
	
	public EndpointUpdater virtualIp() {
		return new EndpointUpdater(this);
	}

	public void addTargets(Endpoint vip, Collection<Endpoint> targets) {
		if(addEndpoints==null){
			this.addEndpoints=Maps.newHashMap();
		}
		addEndpoints.put(vip, targets);
	}
	
	public void removeTargets(Endpoint vip, Collection<Endpoint> targets) {
		if(removeEndpoints==null){
			this.removeEndpoints=Maps.newHashMap();
		}
		removeEndpoints.put(vip, targets);
	}
	
	public void removeVip(Endpoint endpoint) {
		if(removeVips==null){
			this.removeVips=Sets.newHashSet();
		}
		removeVips.add(endpoint);
	}


	@Override
	protected void addMutation() {
		onCurrentResource(lb->setVip(lb));
	}

	private void setVip(LoadBalancer lb) {
		if(lb==null){
			return;
		}
		Stream<Entry<Endpoint,Collection<Endpoint>>> existingVips = lb.getVips().entrySet().stream()
			.map(vip->{
				if(addEndpoints!=null && addEndpoints.containsKey(vip.getKey())){
					Set<Endpoint> targets = Sets.newHashSet(clearBefore?Collections.emptySet():vip.getValue());
					targets.addAll(addEndpoints.get(vip.getKey()));
					if(removeEndpoints!=null && removeEndpoints.containsKey(vip.getKey())){
						targets.removeAll(removeEndpoints.get(vip.getKey()));
					}
					return Maps.immutableEntry(vip.getKey(), targets);
				}else if(removeEndpoints!=null && removeEndpoints.containsKey(vip.getKey())){
					Set<Endpoint> targets = Sets.newHashSet(vip.getValue());
					targets.removeAll(removeEndpoints.get(vip.getKey()));
					return Maps.immutableEntry(vip.getKey(), targets);
				}else{
					return vip;
				}
			});
		
		Stream<Entry<Endpoint,Collection<Endpoint>>> newVips;
		if(addEndpoints!=null){
			newVips = addEndpoints.entrySet().stream()
					.filter(entry->!lb.getVips().containsKey(entry.getKey()));
		}else{
			newVips = Stream.empty();
		}
		
		Map<Endpoint,Collection<Endpoint>> vips = Stream.concat(existingVips, newVips)
				.filter(entry->removeVips==null || !removeVips.contains(entry.getKey()))
				.filter(entry->!entry.getValue().isEmpty())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		
		addOperation(loadBalanders.updateVipsOperation(lb.getUuid(),vips));
	}

	public void setTargets(Endpoint endpoint, Collection<Endpoint> targets) {
		clearBefore=true;
		addTargets(endpoint,targets);
	}

}
