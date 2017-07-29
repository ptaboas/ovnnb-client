package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class LogicalRouter {
	
	public LogicalRouter(String name, Map<String, String> externalIds) {
		this(null,name,externalIds,null,null);
	}
	
	@JsonCreator
	public LogicalRouter(
			@JsonProperty("_uuid") UUID uuid,
			@JsonProperty("name") String name, 
			@JsonProperty("external_ids") Map<String,String> externalIds,
			@JsonProperty("load_balancer") List<UUID> loadBalancers,
			@JsonProperty("ports")List<UUID> ports){
		this.uuid=uuid;
		this.name=name;
		this.externalIds=externalIds;
		this.loadBalancers=loadBalancers;
		this.ports=ports;
	}
	
	



	private final UUID uuid;
	private final String name;
	@JsonProperty("external_ids")
	private final Map<String,String> externalIds;
	@JsonProperty("load_balancer")
	private final List<UUID> loadBalancers;
	private final List<UUID> ports;
	
}
