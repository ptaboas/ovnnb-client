package com.simplyti.cloud.ovn.client.jsonrpc.domain;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class JsonRpcRequest extends JsonRpcMessage {
	
	private final String method;
	private final Collection<Object> params;

	@JsonCreator
	public JsonRpcRequest(@JsonProperty("id") Object id, 
			@JsonProperty("method") String method, 
			@JsonProperty("params") Collection<Object> params) {
		super(id);
		this.method=method;
		this.params=params;
	}

}
