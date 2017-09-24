package com.simplyti.cloud.ovn.client;

import java.util.Map;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.domain.annotations.Column;

import lombok.Getter;

@Getter
public class NamedOvsResource extends OvsResource{

	private final String name;
	
	@Column("external_ids")
	private final Map<String,String> externalIds;
	
	public NamedOvsResource(UUID uuid, String name, Map<String,String> externalIds) {
		super(uuid);
		this.name=name;
		this.externalIds=externalIds;
	}

}
