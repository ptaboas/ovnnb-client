package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.NamedOvsResource;
import com.simplyti.cloud.ovn.client.domain.annotations.Column;

import lombok.Getter;

@Getter
public class LogicalRouter extends NamedOvsResource{
	
	@Column("load_balancer")
	private final Collection<UUID> loadBalancers;
	
	public LogicalRouter(UUID uuid, String name, Collection<UUID> loadBalancers, Map<String,String> externalIds){
		super(uuid,name,externalIds);
		this.loadBalancers=loadBalancers;
	}
	
}
