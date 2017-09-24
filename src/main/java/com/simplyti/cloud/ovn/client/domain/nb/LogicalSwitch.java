package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.NamedOvsResource;
import com.simplyti.cloud.ovn.client.domain.NetworkIp;
import com.simplyti.cloud.ovn.client.domain.annotations.Column;
import com.simplyti.cloud.ovn.client.domain.annotations.MapField;

import lombok.Getter;

@Getter
public class LogicalSwitch extends NamedOvsResource{
	
	@MapField("other_config")
	private final NetworkIp subnet;
	
	@Column("load_balancer")
	private final Collection<UUID> loadBalancers;
	
	public LogicalSwitch(UUID uuid, String name, NetworkIp subnet, Collection<UUID> loadBalancers ,Map<String,String> externalIds){
		super(uuid,name,externalIds);
		this.subnet=subnet;
		this.loadBalancers=loadBalancers;
	}
	
}
