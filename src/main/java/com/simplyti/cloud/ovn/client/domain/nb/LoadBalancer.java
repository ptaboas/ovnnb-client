package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.NamedOvsResource;
import com.simplyti.cloud.ovn.client.domain.Endpoint;

import lombok.Getter;

@Getter
public class LoadBalancer extends NamedOvsResource{
	
	private final Protocol protocol;
	private final Map<Endpoint,Collection<Endpoint>> vips;
	
	public LoadBalancer(UUID uuid, String name, Protocol protocol, Map<Endpoint,Collection<Endpoint>> vips, Map<String,String> externalIds){
		super(uuid,name,externalIds);
		this.protocol=protocol;
		this.vips=vips;
	}

}
