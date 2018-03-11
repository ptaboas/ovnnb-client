package com.simplyti.cloud.ovn.client.jsonrpc.domain;

import java.util.List;

import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class JsonRpcRequest extends JsonRpcMessage {
	
	private final String method;
	private final List<Object> params;

	@JsonCreator
	public JsonRpcRequest(@JsonProperty("id") Object id, 
			@JsonProperty("method") String method, 
			@JsonProperty("params") List<Object> params) {
		super(id);
		this.method=method;
		this.params=params;
	}

}
