package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class LoadBalancer {
	
	public LoadBalancer(String name, Protocol protocol, Map<String,String> vips, Map<String, String> externalIds) {
		this(null,name,protocol.getValue(),vips,externalIds);
	}
	
	@JsonCreator
	public LoadBalancer(
			@JsonProperty("_uuid") UUID uuid,
			@JsonProperty("name") String name, 
			@JsonProperty("protocol") String protocol, 
			@JsonProperty("vips")  Map<String,String> vips, 
		 @JsonProperty("external_ids") Map<String,String> externalIds){
		this.uuid=uuid;
		this.name=name;
		this.protocol=protocol;
		this.vips=vips;
		this.externalIds=externalIds;
	}
	

	private final UUID uuid;
	private final String name;
	private final String protocol;
	private final Map<String,String> vips;
	@JsonProperty("external_ids")
	private final Map<String,String> externalIds;
	

}
